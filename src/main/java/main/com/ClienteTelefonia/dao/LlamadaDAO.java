package main.com.ClienteTelefonia.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import main.com.ClienteTelefonia.dto.DatabaseDTO;
import main.com.ClienteTelefonia.model.LlamadaRegistro;
import main.com.ClienteTelefonia.service.LlamadaService;

public class LlamadaDAO {

    private final LlamadaService calculadora = new LlamadaService();

    public record SolicitudCreada(
            long idLlamada,
            String destino,
            String estado,
            String estadoSolicitudTelnyx,
            boolean nueva
    ) {
    }

    public record EventoTelnyx(
            String idEvento,
            Long idLlamada,
            String callControlId,
            String callSessionId,
            String tipo,
            LocalDateTime ocurridoEn,
            String hangupCause,
            String hangupSource,
            Integer duracionSegundos
    ) {
    }

    public record LlamadaParaFinalizar(long idLlamada, String callControlId) {
    }

    public SolicitudCreada crearSolicitud(
            long idCliente,
            String destino,
            String idempotencyKey
    ) throws SQLException {
        validarSolicitud(idCliente, destino, idempotencyKey);
        idempotencyKey = idempotencyKey.toLowerCase(Locale.ROOT);

        try (Connection cn = DatabaseDTO.getConnection()) {
            cn.setAutoCommit(false);
            try {
                SolicitudCreada existente =
                        buscarSolicitudPorClave(
                                cn,
                                idCliente,
                                destino,
                                idempotencyKey
                        );
                if (existente != null) {
                    cn.commit();
                    return existente;
                }

                Cuenta cuenta = bloquearCuenta(cn, idCliente);
                Tarifa tarifa = obtenerTarifaActiva(cn);
                int duracionMaxima = obtenerDuracionMaxima(cn);

                BigDecimal costoMaximo = calculadora.calcularCosto(
                        tarifa.precioMinuto(),
                        duracionMaxima,
                        tarifa.segundosMinimos(),
                        duracionMaxima
                );
                if (cuenta.saldo().compareTo(costoMaximo) < 0) {
                    throw new IllegalStateException(
                            "El saldo no cubre el costo máximo posible "
                            + "de esta llamada."
                    );
                }

                long idLlamada = insertarLlamada(
                        cn,
                        cuenta.idCuenta(),
                        destino,
                        idempotencyKey,
                        tarifa,
                        duracionMaxima
                );
                cn.commit();
                return new SolicitudCreada(
                        idLlamada,
                        destino,
                        "SOLICITADA",
                        "PENDIENTE",
                        true
                );
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    public List<LlamadaRegistro> listarPorCliente(long idCliente)
            throws SQLException {
        String sql = """
            SELECT TOP (100)
                l.id_llamada,
                l.destino,
                l.estado,
                l.solicitud_telnyx_estado,
                l.duracion_facturable_segundos,
                l.costo,
                l.moneda,
                l.fecha_solicitud,
                l.fecha_contestacion,
                l.fecha_finalizacion,
                l.motivo_finalizacion
            FROM dbo.Llamada l
            INNER JOIN dbo.Cuenta c ON c.id_cuenta = l.id_cuenta
            WHERE c.id_cliente = ?
              AND l.id_llamada > 0
            ORDER BY l.id_llamada DESC
            """;

        List<LlamadaRegistro> llamadas = new ArrayList<>();
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    llamadas.add(mapearLlamada(rs));
                }
            }
        }
        return llamadas;
    }

    public List<SolicitudCreada> listarSolicitudesPendientes()
            throws SQLException {
        String sql = """
            SELECT id_llamada, destino, estado, solicitud_telnyx_estado
            FROM dbo.Llamada
            WHERE estado = 'SOLICITADA'
              AND solicitud_telnyx_estado = 'PENDIENTE'
            ORDER BY id_llamada
            """;
        List<SolicitudCreada> resultado = new ArrayList<>();
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(new SolicitudCreada(
                        rs.getLong("id_llamada"),
                        rs.getString("destino"),
                        rs.getString("estado"),
                        rs.getString("solicitud_telnyx_estado"),
                        false
                ));
            }
        }
        return resultado;
    }

    public boolean marcarSolicitudEnviando(long idLlamada)
            throws SQLException {
        return ejecutarActualizacion("""
            UPDATE dbo.Llamada
            SET solicitud_telnyx_estado = 'ENVIANDO'
            WHERE id_llamada = ?
              AND estado = 'SOLICITADA'
              AND solicitud_telnyx_estado = 'PENDIENTE'
            """, idLlamada) == 1;
    }

    public void marcarSolicitudAceptada(
            long idLlamada,
            String callControlId,
            String callSessionId
    ) throws SQLException {
        String sql = """
            UPDATE dbo.Llamada
            SET solicitud_telnyx_estado = 'ACEPTADA',
                call_control_id = COALESCE(call_control_id, ?),
                call_session_id = COALESCE(call_session_id, ?)
            WHERE id_llamada = ?
              AND estado NOT IN ('FINALIZADA', 'FALLIDA', 'CANCELADA')
            """;
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, callControlId);
            ps.setString(2, callSessionId);
            ps.setLong(3, idLlamada);
            ps.executeUpdate();
        }
    }

    public void marcarSolicitudIncierta(long idLlamada)
            throws SQLException {
        ejecutarActualizacion("""
            UPDATE dbo.Llamada
            SET solicitud_telnyx_estado = 'INCIERTA'
            WHERE id_llamada = ?
              AND solicitud_telnyx_estado = 'ENVIANDO'
            """, idLlamada);
    }

    public void recuperarSolicitudesEnviando() throws SQLException {
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement("""
                     UPDATE dbo.Llamada
                     SET solicitud_telnyx_estado = 'INCIERTA'
                     WHERE estado = 'SOLICITADA'
                       AND solicitud_telnyx_estado = 'ENVIANDO'
                     """)) {
            ps.executeUpdate();
        }
    }

    public void marcarSolicitudFallida(
            long idLlamada,
            String motivo
    ) throws SQLException {
        try (Connection cn = DatabaseDTO.getConnection()) {
            cn.setAutoCommit(false);
            try {
                bloquearCuentaDeLlamada(cn, idLlamada);
                try (PreparedStatement ps = cn.prepareStatement("""
                        UPDATE dbo.ReservaSaldo
                        SET estado = 'LIBERADA',
                            fecha_liberacion = SYSUTCDATETIME()
                        WHERE id_llamada = ? AND estado = 'ACTIVA'
                        """)) {
                    ps.setLong(1, idLlamada);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = cn.prepareStatement("""
                        UPDATE dbo.Llamada
                        SET estado = 'FALLIDA',
                            solicitud_telnyx_estado = 'FALLIDA',
                            motivo_finalizacion = ?,
                            fecha_finalizacion = SYSUTCDATETIME()
                        WHERE id_llamada = ?
                          AND estado = 'SOLICITADA'
                        """)) {
                    ps.setString(1, limitar(motivo, 120));
                    ps.setLong(2, idLlamada);
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    public boolean solicitarCancelacion(
            long idCliente,
            long idLlamada
    ) throws SQLException {
        String sql = """
            UPDATE l
            SET cancelacion_solicitada = 1
            FROM dbo.Llamada l
            INNER JOIN dbo.Cuenta c ON c.id_cuenta = l.id_cuenta
            WHERE l.id_llamada = ?
              AND c.id_cliente = ?
              AND l.estado IN ('SOLICITADA', 'TIMBRANDO', 'CONTESTADA')
              AND l.call_control_id IS NOT NULL
            """;
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idLlamada);
            ps.setLong(2, idCliente);
            return ps.executeUpdate() == 1;
        }
    }

    public void deshacerSolicitudCancelacion(long idLlamada)
            throws SQLException {
        ejecutarActualizacion("""
            UPDATE dbo.Llamada
            SET cancelacion_solicitada = 0
            WHERE id_llamada = ?
              AND estado IN ('SOLICITADA', 'TIMBRANDO', 'CONTESTADA')
            """, idLlamada);
    }

    public boolean marcarCancelacionAutomatica(long idLlamada)
            throws SQLException {
        String sql = """
            UPDATE dbo.Llamada
            SET cancelacion_solicitada = 1
            WHERE id_llamada = ?
              AND cancelacion_solicitada = 0
              AND call_control_id IS NOT NULL
              AND estado IN ('SOLICITADA', 'TIMBRANDO', 'CONTESTADA')
            """;
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idLlamada);
            return ps.executeUpdate() == 1;
        }
    }

    public List<LlamadaParaFinalizar> listarVencidas()
            throws SQLException {
        String sql = """
            SELECT id_llamada, call_control_id
            FROM dbo.Llamada
            WHERE call_control_id IS NOT NULL
              AND cancelacion_solicitada = 0
              AND (
                    (estado = 'CONTESTADA'
                     AND duracion_maxima_aplicada_segundos IS NOT NULL
                     AND fecha_contestacion <= DATEADD(
                            second, -duracion_maxima_aplicada_segundos,
                            SYSUTCDATETIME()
                         ))
                    OR
                    (estado IN ('SOLICITADA', 'TIMBRANDO')
                     AND solicitud_telnyx_estado = 'ACEPTADA'
                     AND fecha_solicitud <= DATEADD(
                            second, -45, SYSUTCDATETIME()
                         ))
                  )
            """;
        List<LlamadaParaFinalizar> llamadas = new ArrayList<>();
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                llamadas.add(new LlamadaParaFinalizar(
                        rs.getLong("id_llamada"),
                        rs.getString("call_control_id")
                ));
            }
        }
        return llamadas;
    }

    public String buscarCallControlId(
            long idCliente,
            long idLlamada
    ) throws SQLException {
        String sql = """
            SELECT l.call_control_id
            FROM dbo.Llamada l
            INNER JOIN dbo.Cuenta c ON c.id_cuenta = l.id_cuenta
            WHERE l.id_llamada = ? AND c.id_cliente = ?
            """;
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idLlamada);
            ps.setLong(2, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public boolean guardarEvento(EventoTelnyx evento)
            throws SQLException {
        String sql = """
            INSERT INTO dbo.TelnyxWebhookEvento (
                id_evento_telnyx,
                id_llamada,
                call_control_id,
                call_session_id,
                tipo_evento,
                ocurrido_en,
                hangup_cause,
                hangup_source,
                duracion_segundos
            )
            SELECT ?,
                   CASE
                       WHEN ? IS NOT NULL
                            AND EXISTS (
                                SELECT 1 FROM dbo.Llamada
                                WHERE id_llamada = ?
                            )
                       THEN ? ELSE NULL
                   END,
                   ?, ?, ?, ?, ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1
                FROM dbo.TelnyxWebhookEvento WITH (UPDLOCK, HOLDLOCK)
                WHERE id_evento_telnyx = ?
            )
            """;
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, evento.idEvento());
            if (evento.idLlamada() == null) {
                ps.setNull(i++, java.sql.Types.BIGINT);
                ps.setNull(i++, java.sql.Types.BIGINT);
                ps.setNull(i++, java.sql.Types.BIGINT);
            } else {
                ps.setLong(i++, evento.idLlamada());
                ps.setLong(i++, evento.idLlamada());
                ps.setLong(i++, evento.idLlamada());
            }
            ps.setString(i++, evento.callControlId());
            ps.setString(i++, evento.callSessionId());
            ps.setString(i++, evento.tipo());
            ps.setTimestamp(i++, Timestamp.valueOf(evento.ocurridoEn()));
            ps.setString(i++, limitar(evento.hangupCause(), 120));
            ps.setString(i++, limitar(evento.hangupSource(), 120));
            if (evento.duracionSegundos() == null) {
                ps.setNull(i++, java.sql.Types.INTEGER);
            } else {
                ps.setInt(i++, evento.duracionSegundos());
            }
            ps.setString(i, evento.idEvento());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            if ((e.getErrorCode() == 2601 || e.getErrorCode() == 2627)
                    && eventoExiste(evento.idEvento())) {
                return false;
            }
            throw e;
        }
    }

    public EventoTelnyx reclamarEventoPendiente() throws SQLException {
        String sql = """
            ;WITH siguiente AS (
                SELECT TOP (1) *
                FROM dbo.TelnyxWebhookEvento
                    WITH (UPDLOCK, READPAST, ROWLOCK)
                WHERE estado_procesamiento = 'PENDIENTE'
                   OR (estado_procesamiento = 'ERROR'
                       AND intentos < 5
                       AND reintentar_en <= SYSUTCDATETIME())
                   OR (estado_procesamiento = 'PROCESANDO'
                       AND intentos < 5
                       AND procesando_en <
                           DATEADD(minute, -2, SYSUTCDATETIME()))
                ORDER BY recibido_en
            )
            UPDATE siguiente
            SET estado_procesamiento = 'PROCESANDO',
                procesando_en = SYSUTCDATETIME(),
                reintentar_en = NULL,
                intentos = intentos + 1
            OUTPUT
                inserted.id_evento_telnyx,
                inserted.id_llamada,
                inserted.call_control_id,
                inserted.call_session_id,
                inserted.tipo_evento,
                inserted.ocurrido_en,
                inserted.hangup_cause,
                inserted.hangup_source,
                inserted.duracion_segundos
            """;
        try (Connection cn = DatabaseDTO.getConnection()) {
            try (PreparedStatement agotados = cn.prepareStatement("""
                    UPDATE dbo.TelnyxWebhookEvento
                    SET estado_procesamiento = 'ERROR',
                        ultimo_error =
                            N'Límite de reintentos de procesamiento agotado.',
                        procesando_en = NULL,
                        reintentar_en = NULL
                    WHERE estado_procesamiento = 'PROCESANDO'
                      AND intentos >= 5
                      AND procesando_en <
                          DATEADD(minute, -2, SYSUTCDATETIME())
                    """)) {
                agotados.executeUpdate();
            }
            try (PreparedStatement ps = cn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long id = rs.getLong("id_llamada");
                Long idLlamada = rs.wasNull() ? null : id;
                int duracion = rs.getInt("duracion_segundos");
                Integer duracionSegundos = rs.wasNull()
                        ? null
                        : duracion;
                return new EventoTelnyx(
                        rs.getString("id_evento_telnyx"),
                        idLlamada,
                        rs.getString("call_control_id"),
                        rs.getString("call_session_id"),
                        rs.getString("tipo_evento"),
                        rs.getTimestamp("ocurrido_en").toLocalDateTime(),
                        rs.getString("hangup_cause"),
                        rs.getString("hangup_source"),
                        duracionSegundos
                );
            }
        }
    }

    public void procesarEvento(EventoTelnyx evento) throws SQLException {
        try (Connection cn = DatabaseDTO.getConnection()) {
            cn.setAutoCommit(false);
            try {
                Long idLlamada = buscarIdLlamadaEvento(cn, evento);
                if (idLlamada != null) {
                    aplicarEvento(cn, idLlamada, evento);
                }
                completarEvento(cn, evento.idEvento(), null);
                cn.commit();
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                try {
                    marcarEventoConError(evento.idEvento(), e.getMessage());
                } catch (SQLException errorEvento) {
                    e.addSuppressed(errorEvento);
                }
                throw e;
            }
        }
    }

    public void marcarEventoConError(String idEvento, String mensaje)
            throws SQLException {
        String sql = """
            UPDATE dbo.TelnyxWebhookEvento
            SET estado_procesamiento = 'ERROR',
                ultimo_error = ?,
                procesando_en = NULL,
                reintentar_en = CASE
                    WHEN intentos < 5 THEN DATEADD(
                        second,
                        CASE WHEN intentos = 1 THEN 2
                             WHEN intentos = 2 THEN 10
                             WHEN intentos = 3 THEN 30
                             ELSE 60 END,
                        SYSUTCDATETIME()
                    )
                    ELSE NULL
                END
            WHERE id_evento_telnyx = ?
            """;
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, limitar(mensaje, 500));
            ps.setString(2, idEvento);
            ps.executeUpdate();
        }
    }

    private void aplicarEvento(
            Connection cn,
            long idLlamada,
            EventoTelnyx evento
    ) throws SQLException {
        LlamadaDatos llamada = cargarLlamada(cn, idLlamada, false);
        if (llamada == null || eventoObsoleto(llamada, evento)) {
            return;
        }

        String tipo = evento.tipo();
        if ("call.initiated".equals(tipo)
                || "call.ringing".equals(tipo)
                || "call.answered".equals(tipo)) {
            llamada = cargarLlamada(cn, idLlamada, true);
            if (llamada == null || eventoObsoleto(llamada, evento)) {
                return;
            }
            if ("call.answered".equals(tipo)) {
                actualizarContestada(cn, idLlamada, evento);
            } else {
                actualizarEstadoNoTerminal(
                        cn, idLlamada, "TIMBRANDO", evento
                );
            }
            return;
        }
        if (!"call.hangup".equals(tipo)
                && !"call.failed".equals(tipo)) {
            return;
        }

        bloquearCuentaPorId(cn, llamada.idCuenta());
        llamada = cargarLlamada(cn, idLlamada, true);
        if (llamada == null || eventoObsoleto(llamada, evento)) {
            return;
        }

        Timestamp contestacion = llamada.fechaContestacion();
        boolean contestada = contestacion != null
                || "CONTESTADA".equals(llamada.estado())
                || (evento.duracionSegundos() != null
                    && evento.duracionSegundos() > 0);
        if (contestada) {
            if (contestacion == null && evento.duracionSegundos() != null) {
                contestacion = Timestamp.valueOf(
                        evento.ocurridoEn().minusSeconds(
                                evento.duracionSegundos()
                        )
                );
            }
            finalizarContestada(
                    cn,
                    idLlamada,
                    llamada.idCuenta(),
                    contestacion,
                    evento,
                    llamada.precioMinuto(),
                    llamada.segundosMinimos(),
                    llamada.duracionMaxima()
            );
        } else {
            finalizarSinRespuesta(
                    cn,
                    idLlamada,
                    llamada.cancelacionSolicitada()
                            ? "CANCELADA" : "FALLIDA",
                    evento
            );
        }
    }

    private LlamadaDatos cargarLlamada(
            Connection cn,
            long idLlamada,
            boolean bloquear
    ) throws SQLException {
        String sql = """
            SELECT id_cuenta, estado, fecha_contestacion,
                   fecha_ultimo_evento_telnyx, cancelacion_solicitada,
                   duracion_maxima_aplicada_segundos,
                   precio_minuto_aplicado, segundos_minimos_aplicados
            FROM dbo.Llamada %s
            WHERE id_llamada = ?
            """.formatted(bloquear ? "WITH (UPDLOCK, HOLDLOCK)" : "");
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idLlamada);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Timestamp ultimoEvento =
                        rs.getTimestamp("fecha_ultimo_evento_telnyx");
                return new LlamadaDatos(
                        rs.getLong("id_cuenta"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_contestacion"),
                        ultimoEvento == null
                                ? null : ultimoEvento.toLocalDateTime(),
                        rs.getBoolean("cancelacion_solicitada"),
                        rs.getInt("duracion_maxima_aplicada_segundos"),
                        rs.getBigDecimal("precio_minuto_aplicado"),
                        rs.getInt("segundos_minimos_aplicados")
                );
            }
        }
    }

    private boolean eventoObsoleto(
            LlamadaDatos llamada,
            EventoTelnyx evento
    ) {
        return esTerminal(llamada.estado())
                || (llamada.fechaUltimoEvento() != null
                    && !llamada.fechaUltimoEvento()
                            .isBefore(evento.ocurridoEn()));
    }

    private void finalizarContestada(
            Connection cn,
            long idLlamada,
            long idCuenta,
            Timestamp contestacion,
            EventoTelnyx evento,
            BigDecimal precioMinuto,
            int segundosMinimos,
            int duracionMaxima
    ) throws SQLException {
        int segundos = evento.duracionSegundos() == null
                ? (int) Math.max(
                    0,
                    java.time.Duration.between(
                            contestacion.toLocalDateTime(),
                            evento.ocurridoEn()
                    ).getSeconds()
                )
                : evento.duracionSegundos();
        int segundosFacturables =
                Math.min(Math.max(1, segundos), duracionMaxima);
        BigDecimal costo = calculadora.calcularCosto(
                precioMinuto,
                segundosFacturables,
                segundosMinimos,
                duracionMaxima
        );

        BigDecimal saldo = bloquearSaldoCuenta(cn, idCuenta);
        BigDecimal montoCobrado =
                calculadora.calcularMontoCobrado(saldo, costo);
        BigDecimal saldoNuevo = saldo.subtract(montoCobrado);

        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE dbo.Cuenta
                SET saldo = ?
                WHERE id_cuenta = ?
                """)) {
            ps.setBigDecimal(1, saldoNuevo);
            ps.setLong(2, idCuenta);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("La cuenta está bloqueada.");
            }
        }

        if (montoCobrado.signum() > 0) {
            try (PreparedStatement ps = cn.prepareStatement("""
                    INSERT INTO dbo.MovimientoSaldo (
                        id_cuenta, id_llamada, tipo, monto,
                        id_movimiento_origen, saldo_anterior,
                        saldo_posterior, descripcion
                    )
                    VALUES (?, ?, 'CONSUMO', ?, NULL, ?, ?, ?)
                    """)) {
                ps.setLong(1, idCuenta);
                ps.setLong(2, idLlamada);
                ps.setBigDecimal(3, montoCobrado);
                ps.setBigDecimal(4, saldo);
                ps.setBigDecimal(5, saldoNuevo);
                ps.setString(6, descripcionConsumo(costo, montoCobrado));
                ps.executeUpdate();
            }
        }

        cerrarReserva(cn, idLlamada, "LIBERADA");
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE dbo.Llamada
                SET estado = 'FINALIZADA',
                    solicitud_telnyx_estado = 'ACEPTADA',
                    duracion_facturable_segundos = ?,
                    costo = ?,
                    fecha_finalizacion = ?,
                    fecha_ultimo_evento_telnyx = ?,
                    motivo_finalizacion = ?
                WHERE id_llamada = ?
                  AND estado NOT IN ('FINALIZADA', 'FALLIDA', 'CANCELADA')
                """)) {
            ps.setInt(1, segundosFacturables);
            ps.setBigDecimal(2, costo);
            ps.setTimestamp(3, Timestamp.valueOf(evento.ocurridoEn()));
            ps.setTimestamp(4, Timestamp.valueOf(evento.ocurridoEn()));
            ps.setString(5, limitar(evento.hangupCause(), 120));
            ps.setLong(6, idLlamada);
            ps.executeUpdate();
        }
    }

    private void finalizarSinRespuesta(
            Connection cn,
            long idLlamada,
            String estado,
            EventoTelnyx evento
    ) throws SQLException {
        cerrarReserva(cn, idLlamada, "LIBERADA");
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE dbo.Llamada
                SET estado = ?,
                    solicitud_telnyx_estado = 'ACEPTADA',
                    duracion_facturable_segundos = 0,
                    costo = 0,
                    fecha_finalizacion = ?,
                    fecha_ultimo_evento_telnyx = ?,
                    motivo_finalizacion = ?
                WHERE id_llamada = ?
                  AND estado NOT IN ('FINALIZADA', 'FALLIDA', 'CANCELADA')
                """)) {
            ps.setString(1, estado);
            ps.setTimestamp(2, Timestamp.valueOf(evento.ocurridoEn()));
            ps.setTimestamp(3, Timestamp.valueOf(evento.ocurridoEn()));
            ps.setString(4, limitar(evento.hangupCause(), 120));
            ps.setLong(5, idLlamada);
            ps.executeUpdate();
        }
    }

    private void actualizarEstadoNoTerminal(
            Connection cn,
            long idLlamada,
            String estado,
            EventoTelnyx evento
    ) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE dbo.Llamada
                SET estado = ?,
                    solicitud_telnyx_estado = 'ACEPTADA',
                    call_control_id = COALESCE(call_control_id, ?),
                    call_session_id = COALESCE(call_session_id, ?),
                    fecha_ultimo_evento_telnyx = ?
                WHERE id_llamada = ?
                  AND estado NOT IN ('FINALIZADA', 'FALLIDA', 'CANCELADA')
                """)) {
            ps.setString(1, estado);
            ps.setString(2, evento.callControlId());
            ps.setString(3, evento.callSessionId());
            ps.setTimestamp(4, Timestamp.valueOf(evento.ocurridoEn()));
            ps.setLong(5, idLlamada);
            ps.executeUpdate();
        }
    }

    private void actualizarContestada(
            Connection cn,
            long idLlamada,
            EventoTelnyx evento
    ) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE dbo.Llamada
                SET estado = 'CONTESTADA',
                    solicitud_telnyx_estado = 'ACEPTADA',
                    call_control_id = COALESCE(call_control_id, ?),
                    call_session_id = COALESCE(call_session_id, ?),
                    fecha_contestacion = ?,
                    fecha_ultimo_evento_telnyx = ?
                WHERE id_llamada = ?
                  AND estado NOT IN ('FINALIZADA', 'FALLIDA', 'CANCELADA')
                """)) {
            ps.setString(1, evento.callControlId());
            ps.setString(2, evento.callSessionId());
            ps.setTimestamp(3, Timestamp.valueOf(evento.ocurridoEn()));
            ps.setTimestamp(4, Timestamp.valueOf(evento.ocurridoEn()));
            ps.setLong(5, idLlamada);
            ps.executeUpdate();
        }
    }

    private void cerrarReserva(
            Connection cn,
            long idLlamada,
            String estado
    ) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE dbo.ReservaSaldo
                SET estado = ?, fecha_liberacion = SYSUTCDATETIME()
                WHERE id_llamada = ? AND estado = 'ACTIVA'
                """)) {
            ps.setString(1, estado);
            ps.setLong(2, idLlamada);
            ps.executeUpdate();
        }
    }

    private Long buscarIdLlamadaEvento(
            Connection cn,
            EventoTelnyx evento
    ) throws SQLException {
        if (evento.idLlamada() != null) {
            return evento.idLlamada();
        }
        if (evento.callControlId() == null) {
            return null;
        }
        try (PreparedStatement ps = cn.prepareStatement("""
                SELECT id_llamada
                FROM dbo.Llamada
                WHERE call_control_id = ?
                """)) {
            ps.setString(1, evento.callControlId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private void completarEvento(
            Connection cn,
            String idEvento,
            String error
    ) throws SQLException {
        String estado = error == null ? "PROCESADO" : "ERROR";
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE dbo.TelnyxWebhookEvento
                SET estado_procesamiento = ?,
                    procesado_en = CASE WHEN ? = 'PROCESADO'
                        THEN SYSUTCDATETIME() ELSE procesado_en END,
                    ultimo_error = ?,
                    procesando_en = NULL
                WHERE id_evento_telnyx = ?
                """)) {
            ps.setString(1, estado);
            ps.setString(2, estado);
            ps.setString(3, limitar(error, 500));
            ps.setString(4, idEvento);
            ps.executeUpdate();
        }
    }

    private SolicitudCreada buscarSolicitudPorClave(
            Connection cn,
            long idCliente,
            String destino,
            String idempotencyKey
    ) throws SQLException {
        String sql = """
            SELECT l.id_llamada, l.destino, l.estado,
                   l.solicitud_telnyx_estado, c.id_cliente
            FROM dbo.Llamada l WITH
                (UPDLOCK, HOLDLOCK, INDEX(UX_Llamada_IdempotencyKey))
            INNER JOIN dbo.Cuenta c ON c.id_cuenta = l.id_cuenta
            WHERE l.idempotency_key = ?
            """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, idempotencyKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                if (rs.getLong("id_cliente") != idCliente
                        || !destino.equals(rs.getString("destino"))) {
                    throw new IdempotencyConflictException(
                            "La clave de idempotencia ya se usó "
                            + "para otra solicitud."
                    );
                }
                return new SolicitudCreada(
                        rs.getLong("id_llamada"),
                        rs.getString("destino"),
                        rs.getString("estado"),
                        rs.getString("solicitud_telnyx_estado"),
                        false
                );
            }
        }
    }

    private boolean eventoExiste(String idEvento) throws SQLException {
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement("""
                     SELECT 1
                     FROM dbo.TelnyxWebhookEvento
                     WHERE id_evento_telnyx = ?
                     """)) {
            ps.setString(1, idEvento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Cuenta bloquearCuenta(Connection cn, long idCliente)
            throws SQLException {
        String sql = """
            SELECT c.id_cuenta, c.saldo
            FROM dbo.Cuenta c WITH (UPDLOCK, HOLDLOCK)
            INNER JOIN dbo.Cliente cl ON cl.id_cliente = c.id_cliente
            WHERE c.id_cliente = ?
              AND c.estado = 'ACTIVA'
              AND cl.estado = 'ACTIVO'
            """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException(
                            "La cuenta no existe, está bloqueada o "
                            + "el cliente está inactivo."
                    );
                }
                return new Cuenta(
                        rs.getLong("id_cuenta"),
                        rs.getBigDecimal("saldo")
                );
            }
        }
    }

    private BigDecimal bloquearSaldoCuenta(Connection cn, long idCuenta)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                SELECT saldo
                FROM dbo.Cuenta WITH (UPDLOCK, HOLDLOCK)
                WHERE id_cuenta = ?
                """)) {
            ps.setLong(1, idCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("La cuenta está bloqueada.");
                }
                return rs.getBigDecimal(1);
            }
        }
    }

    private void bloquearCuentaPorId(Connection cn, long idCuenta)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                SELECT id_cuenta
                FROM dbo.Cuenta WITH (UPDLOCK, HOLDLOCK)
                WHERE id_cuenta = ?
                """)) {
            ps.setLong(1, idCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe la cuenta.");
                }
            }
        }
    }

    private void bloquearCuentaDeLlamada(
            Connection cn,
            long idLlamada
    ) throws SQLException {
        long idCuenta;
        try (PreparedStatement ps = cn.prepareStatement("""
                SELECT id_cuenta
                FROM dbo.Llamada
                WHERE id_llamada = ?
                """)) {
            ps.setLong(1, idLlamada);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe la llamada.");
                }
                idCuenta = rs.getLong(1);
            }
        }
        bloquearCuentaPorId(cn, idCuenta);
    }

    private Tarifa obtenerTarifaActiva(Connection cn)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                SELECT TOP (1) id_tarifa, precio_minuto,
                       moneda, segundos_minimos
                FROM dbo.Tarifa WITH (UPDLOCK, HOLDLOCK)
                WHERE activa = 1
                ORDER BY fecha_inicio DESC, id_tarifa DESC
                """);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException(
                        "No hay una tarifa activa configurada."
                );
            }
            String moneda = rs.getString("moneda");
            if (!"PEN".equals(moneda)) {
                throw new IllegalStateException(
                        "La tarifa activa no está expresada en PEN."
                );
            }
            return new Tarifa(
                    rs.getLong("id_tarifa"),
                    rs.getBigDecimal("precio_minuto"),
                    moneda,
                    rs.getInt("segundos_minimos")
            );
        }
    }

    private int obtenerDuracionMaxima(Connection cn)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                SELECT duracion_maxima_llamada_segundos
                FROM dbo.ConfiguracionSistema WITH (HOLDLOCK)
                WHERE id_configuracion = 1
                """);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException(
                        "No está configurada la duración máxima de llamadas."
                );
            }
            return rs.getInt("duracion_maxima_llamada_segundos");
        }
    }

    private long insertarLlamada(
            Connection cn,
            long idCuenta,
            String destino,
            String idempotencyKey,
            Tarifa tarifa,
            int duracionMaxima
    ) throws SQLException {
        String sql = """
            INSERT INTO dbo.Llamada (
                id_cuenta, id_tarifa, destino, estado,
                fecha_solicitud, duracion_facturable_segundos, costo,
                moneda, idempotency_key, solicitud_telnyx_estado,
                precio_minuto_aplicado, segundos_minimos_aplicados,
                duracion_maxima_aplicada_segundos
            )
            OUTPUT INSERTED.id_llamada
            VALUES (?, ?, ?, 'SOLICITADA', SYSUTCDATETIME(),
                    0, 0, ?, ?, 'PENDIENTE', ?, ?, ?)
            """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idCuenta);
            ps.setLong(2, tarifa.idTarifa());
            ps.setString(3, destino);
            ps.setString(4, tarifa.moneda());
            ps.setString(5, idempotencyKey);
            ps.setBigDecimal(6, tarifa.precioMinuto());
            ps.setInt(7, tarifa.segundosMinimos());
            ps.setInt(8, duracionMaxima);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "SQL Server no devolvió el id de la llamada."
                    );
                }
                return rs.getLong(1);
            }
        }
    }

    private int ejecutarActualizacion(String sql, long id)
            throws SQLException {
        try (Connection cn = DatabaseDTO.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    private LlamadaRegistro mapearLlamada(ResultSet rs)
            throws SQLException {
        return new LlamadaRegistro(
                rs.getLong("id_llamada"),
                rs.getString("destino"),
                rs.getString("estado"),
                rs.getString("solicitud_telnyx_estado"),
                rs.getInt("duracion_facturable_segundos"),
                rs.getBigDecimal("costo"),
                rs.getString("moneda"),
                fecha(rs, "fecha_solicitud"),
                fecha(rs, "fecha_contestacion"),
                fecha(rs, "fecha_finalizacion"),
                rs.getString("motivo_finalizacion")
        );
    }

    private LocalDateTime fecha(ResultSet rs, String columna)
            throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columna);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private boolean esTerminal(String estado) {
        return "FINALIZADA".equals(estado)
                || "FALLIDA".equals(estado)
                || "CANCELADA".equals(estado);
    }

    private void validarSolicitud(
            long idCliente,
            String destino,
            String idempotencyKey
    ) {
        if (idCliente <= 0) {
            throw new IllegalArgumentException("Cliente inválido.");
        }
        if (destino == null || !destino.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new IllegalArgumentException(
                    "El destino debe estar en formato internacional E.164."
            );
        }
        if (idempotencyKey == null
                || !idempotencyKey.matches(
                    "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-"
                    + "[89ab][0-9a-f]{3}-[0-9a-f]{12}"
                )) {
            throw new IllegalArgumentException(
                    "La clave de idempotencia debe ser un UUID."
            );
        }
    }

    private String descripcionConsumo(
            BigDecimal costoCalculado,
            BigDecimal montoCobrado
    ) {
        if (montoCobrado.compareTo(costoCalculado) == 0) {
            return "Consumo de llamada Telnyx";
        }
        BigDecimal faltante = costoCalculado.subtract(montoCobrado);
        return "Consumo Telnyx calculado: PEN "
                + costoCalculado.toPlainString()
                + "; cobrado: PEN " + montoCobrado.toPlainString()
                + "; faltante no cobrado por saldo insuficiente: PEN "
                + faltante.toPlainString();
    }

    private String limitar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private record Cuenta(long idCuenta, BigDecimal saldo) {
    }

    private record Tarifa(
            long idTarifa,
            BigDecimal precioMinuto,
            String moneda,
            int segundosMinimos
    ) {
    }

    private record LlamadaDatos(
            long idCuenta,
            String estado,
            Timestamp fechaContestacion,
            LocalDateTime fechaUltimoEvento,
            boolean cancelacionSolicitada,
            int duracionMaxima,
            BigDecimal precioMinuto,
            int segundosMinimos
    ) {
    }

    public static final class IdempotencyConflictException
            extends IllegalStateException {
        public IdempotencyConflictException(String mensaje) {
            super(mensaje);
        }
    }
}
