
package main.com.ClienteTelefonia.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.UUID;

import main.com.ClienteTelefonia.dto.DatabaseDTO;
import main.com.ClienteTelefonia.model.ClienteTelefonia;
import main.com.ClienteTelefonia.service.CalculoRecargaSimulada;
import main.com.ClienteTelefonia.service.CalculoRecargaSimulada.Importes;

public class ClienteDAO {

    public long registrar(ClienteTelefonia cliente)
            throws SQLException {
        // Implementación del método para registrar un cliente en la base de datos
      String sqlCliente = """
        INSERT INTO dbo.Cliente (nombre, apellido, telefono, correo)
        VALUES (?, ?, ?, ?)
        """;
      
        // Aquí iría la implementación para insertar el cliente en la base de datos
    String sqlCuenta = """
        INSERT INTO dbo.Cuenta (id_cliente, saldo, estado)
        VALUES (?, 0.00, 'ACTIVA')
        """;

    try (Connection conexion = DatabaseDTO.getConnection()) {

        conexion.setAutoCommit(false);

        try {
            long idCliente;

            // 1. Registrar el cliente.
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    sqlCliente, Statement.RETURN_GENERATED_KEYS)) {

                sentencia.setString(1, cliente.getNombre());
                sentencia.setString(2, cliente.getApellido());
                sentencia.setString(3, cliente.getTelefono());

                if (cliente.getCorreo() == null
                        || cliente.getCorreo().isBlank()) {
                    sentencia.setNull(4, java.sql.Types.NVARCHAR);
                } else {
                    sentencia.setString(4, cliente.getCorreo().trim());
                }

                int filas = sentencia.executeUpdate();

                if (filas != 1) {
                    throw new SQLException(
                        "No se pudo insertar el cliente."
                    );
                }

                try (ResultSet claves = sentencia.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException(
                            "No se pudo recuperar el ID del cliente."
                        );
                    }

                    idCliente = claves.getLong(1);
                }
            }

            // 2. Crear la cuenta asociada.
            try (PreparedStatement sentenciaCuenta =
                    conexion.prepareStatement(sqlCuenta)) {

                sentenciaCuenta.setLong(1, idCliente);

                int filasCuenta = sentenciaCuenta.executeUpdate();

                if (filasCuenta != 1) {
                    throw new SQLException(
                        "No se pudo crear la cuenta del cliente."
                    );
                }
            }

            // 3. Confirmar ambas operaciones.
            conexion.commit();

            cliente.setIdCliente(idCliente);

            return idCliente;

        } catch (SQLException | RuntimeException e) {

            try {
                conexion.rollback();
            } catch (SQLException errorRollback) {
                e.addSuppressed(errorRollback);
            }

            throw e;
        }
       }
    }

    public ClienteTelefonia buscarPorTelefono(String telefono)
            throws SQLException {

            String sql = """
                SELECT
                    c.id_cliente,
                    c.nombre,
                    c.apellido,
                    c.telefono,
                    c.correo,
                    c.estado AS estado_cliente,
                    cu.saldo,
                    cu.estado AS estado_cuenta
                FROM dbo.Cliente AS c
                INNER JOIN dbo.Cuenta AS cu
                    ON cu.id_cliente = c.id_cliente
                WHERE c.id_cliente = ?
                """;

        try (Connection conexion = DatabaseDTO.getConnection();
             PreparedStatement sentencia =
                 conexion.prepareStatement(sql)) {

            sentencia.setString(1, telefono);

            try (ResultSet resultado = sentencia.executeQuery()) {

                if (!resultado.next()) {
                    return null;
                }

                ClienteTelefonia cliente = new ClienteTelefonia();

                cliente.setIdCliente(
                    resultado.getLong("id_cliente")
                );
                cliente.setNombre(
                    resultado.getString("nombre")
                );
                cliente.setApellido(
                    resultado.getString("apellido")
                );
                cliente.setTelefono(
                    resultado.getString("telefono")
                );
                cliente.setCorreo(
                    resultado.getString("correo")
                );
                cliente.setEstado(
                    resultado.getString("estado_cliente")
                );

                // Recuperar el saldo persistido en SQL Server.
                    double saldo = resultado.getDouble("saldo");
                    cliente.setSaldoInicial(saldo);
                    cliente.setSaldoFinal(saldo);

                return cliente;
            }
        }
    }

    //metodo para eliminar un cliente de la base de datos
    public boolean eliminar(long idCliente) throws SQLException {

    String sqlVerificar = """
        SELECT COUNT(*)
        FROM dbo.Recarga r
        INNER JOIN dbo.Cuenta c ON c.id_cuenta = r.id_cuenta
        WHERE c.id_cliente = ?
        """;

    String sqlVerificarLlamadas = """
        SELECT COUNT(*)
        FROM dbo.Llamada l
        INNER JOIN dbo.Cuenta c ON c.id_cuenta = l.id_cuenta
        WHERE c.id_cliente = ?
        """;

    String sqlVerificarMovimientos = """
        SELECT COUNT(*)
        FROM dbo.MovimientoSaldo m
        INNER JOIN dbo.Cuenta c ON c.id_cuenta = m.id_cuenta
        WHERE c.id_cliente = ?
        """;

    try (Connection conexion = DatabaseDTO.getConnection()) {
        conexion.setAutoCommit(false);

        try {
            // No eliminar clientes con historial financiero.
            for (String sql : new String[]{
                    sqlVerificar,
                    sqlVerificarLlamadas,
                    sqlVerificarMovimientos
            }) {
                try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                    ps.setLong(1, idCliente);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getLong(1) > 0) {
                            throw new SQLException(
                                "No se puede eliminar el cliente porque "
                                + "tiene recargas, llamadas o movimientos."
                            );
                        }
                    }
                }
            }

            // Eliminar primero la cuenta.
            try (PreparedStatement ps = conexion.prepareStatement("""
                    DELETE FROM dbo.Cuenta
                    WHERE id_cliente = ?
                    """)) {
                ps.setLong(1, idCliente);
                ps.executeUpdate();
            }

            // Eliminar después el cliente.
            int filas;
            try (PreparedStatement ps = conexion.prepareStatement("""
                    DELETE FROM dbo.Cliente
                    WHERE id_cliente = ?
                    """)) {
                ps.setLong(1, idCliente);
                filas = ps.executeUpdate();
            }

            conexion.commit();
            return filas == 1;

        } catch (SQLException | RuntimeException e) {
            try {
                conexion.rollback();
            } catch (SQLException errorRollback) {
                e.addSuppressed(errorRollback);
            }
            throw e;
        }
    }
  }


  //metodo de verificar las consultas de cuentas antes de eliminar
  public java.util.List<ClienteTelefonia> listar()
        throws SQLException {

    String sql = """
        SELECT id_cliente, nombre, apellido,
               telefono, correo, estado
        FROM dbo.Cliente
        ORDER BY id_cliente DESC
        """;

    java.util.List<ClienteTelefonia> clientes =
            new java.util.ArrayList<>();

    try (Connection conexion = DatabaseDTO.getConnection();
         PreparedStatement sentencia =
                 conexion.prepareStatement(sql);
         ResultSet resultado = sentencia.executeQuery()) {

        while (resultado.next()) {
            ClienteTelefonia cliente = new ClienteTelefonia();

            cliente.setIdCliente(
                resultado.getLong("id_cliente")
            );
            cliente.setNombre(
                resultado.getString("nombre")
            );
            cliente.setApellido(
                resultado.getString("apellido")
            );
            cliente.setTelefono(
                resultado.getString("telefono")
            );
            cliente.setCorreo(
                resultado.getString("correo")
            );
            cliente.setEstado(
                resultado.getString("estado")
            );

            clientes.add(cliente);
        }
    }

        return clientes;
    }





    // Conserva la firma utilizada por las ventanas y llamadas existentes.
    public double registrarRecargaYape(long idCliente, double monto)
            throws SQLException {
        return registrarRecargaYape(
                idCliente,
                BigDecimal.valueOf(monto),
                UUID.randomUUID()
        ).doubleValue();
    }

    public BigDecimal registrarRecargaYape(
            long idCliente,
            BigDecimal monto,
            UUID claveIdempotencia
    ) throws SQLException {
        return registrarRecargaSimulada(
                idCliente,
                CalculoRecargaSimulada.yape(monto),
                "YAPE",
                claveIdempotencia
        );
    }

    // Conserva la firma utilizada por las ventanas y llamadas existentes.
    public double registrarRecargaTarjeta(long idCliente, double monto)
            throws SQLException {
        return registrarRecargaTarjeta(
                idCliente,
                BigDecimal.valueOf(monto),
                UUID.randomUUID()
        ).doubleValue();
    }

    public BigDecimal registrarRecargaTarjeta(
            long idCliente,
            BigDecimal monto,
            UUID claveIdempotencia
    ) throws SQLException {
        return registrarRecargaSimulada(
                idCliente,
                CalculoRecargaSimulada.tarjeta(monto),
                "TARJETA",
                claveIdempotencia
        );
    }

    private BigDecimal registrarRecargaSimulada(
            long idCliente,
            Importes importes,
            String metodo,
            UUID claveIdempotencia
    ) throws SQLException {
        if (idCliente <= 0) {
            throw new IllegalArgumentException("El cliente no es válido.");
        }
        if (claveIdempotencia == null) {
            throw new IllegalArgumentException(
                    "La clave de idempotencia es obligatoria."
            );
        }

        String referencia = "SIMULADA:" + claveIdempotencia;

        try (Connection conexion = DatabaseDTO.getConnection()) {
            conexion.setAutoCommit(false);

            try {
                BigDecimal saldoExistente = buscarRecargaExistente(
                        conexion,
                        idCliente,
                        metodo,
                        importes,
                        referencia
                );
                if (saldoExistente != null) {
                    conexion.commit();
                    return saldoExistente;
                }

                long idCuenta;
                BigDecimal saldoAnterior;
                String estadoCuenta;

                String sqlCuenta = """
                    SELECT c.id_cuenta, c.saldo, c.estado
                    FROM dbo.Cuenta AS c WITH (UPDLOCK, ROWLOCK)
                    INNER JOIN dbo.Cliente AS cl
                        ON cl.id_cliente = c.id_cliente
                    WHERE c.id_cliente = ? AND cl.estado = 'ACTIVO'
                    """;

                try (PreparedStatement ps =
                             conexion.prepareStatement(sqlCuenta)) {
                    ps.setLong(1, idCliente);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "No se encontró una cuenta para el "
                                    + "cliente activo."
                            );
                        }
                        idCuenta = rs.getLong("id_cuenta");
                        saldoAnterior = rs.getBigDecimal("saldo");
                        estadoCuenta = rs.getString("estado");
                    }
                }

                if (!"ACTIVA".equalsIgnoreCase(estadoCuenta)) {
                    throw new SQLException("La cuenta no está activa.");
                }

                BigDecimal saldoNuevo = saldoAnterior
                        .add(importes.montoAcreditado())
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                if (saldoNuevo.compareTo(
                        new BigDecimal("9999999999.99")) > 0) {
                    throw new IllegalArgumentException(
                            "El saldo supera el máximo permitido."
                    );
                }

                long idRecarga;
                String sqlRecarga = """
                    INSERT INTO dbo.Recarga
                        (id_cuenta, monto, monto_comision,
                         monto_acreditado, es_simulada, metodo, estado,
                         referencia, fecha_confirmacion)
                    OUTPUT INSERTED.id_recarga
                    VALUES (?, ?, ?, ?, 1, ?, 'CONFIRMADA', ?,
                            SYSDATETIME())
                    """;

                try (PreparedStatement ps =
                             conexion.prepareStatement(sqlRecarga)) {
                    ps.setLong(1, idCuenta);
                    ps.setBigDecimal(2, importes.montoBruto());
                    ps.setBigDecimal(3, importes.comision());
                    ps.setBigDecimal(4, importes.montoAcreditado());
                    ps.setString(5, metodo);
                    ps.setString(6, referencia);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "No se pudo obtener el ID de la recarga."
                            );
                        }
                        idRecarga = rs.getLong(1);
                    }
                }

                String sqlActualizar = """
                    UPDATE dbo.Cuenta
                    SET saldo = ?
                    WHERE id_cuenta = ? AND estado = 'ACTIVA'
                    """;
                try (PreparedStatement ps =
                             conexion.prepareStatement(sqlActualizar)) {
                    ps.setBigDecimal(1, saldoNuevo);
                    ps.setLong(2, idCuenta);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo actualizar el saldo de la cuenta."
                        );
                    }
                }

                String descripcion =
                        "Recarga SIMULADA académica " + metodo
                        + ". Bruto PEN " + importes.montoBruto().toPlainString()
                        + ", comisión PEN " + importes.comision().toPlainString()
                        + ", acreditado PEN "
                        + importes.montoAcreditado().toPlainString() + ".";
                String sqlMovimiento = """
                    INSERT INTO dbo.MovimientoSaldo
                        (id_cuenta, id_recarga, tipo, monto,
                         saldo_anterior, saldo_posterior, descripcion)
                    VALUES (?, ?, 'RECARGA', ?, ?, ?, ?)
                    """;
                try (PreparedStatement ps =
                             conexion.prepareStatement(sqlMovimiento)) {
                    ps.setLong(1, idCuenta);
                    ps.setLong(2, idRecarga);
                    ps.setBigDecimal(3, importes.montoAcreditado());
                    ps.setBigDecimal(4, saldoAnterior);
                    ps.setBigDecimal(5, saldoNuevo);
                    ps.setString(6, descripcion);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo registrar el movimiento de saldo."
                        );
                    }
                }

                conexion.commit();
                return saldoNuevo;
            } catch (SQLException | RuntimeException ex) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    ex.addSuppressed(errorRollback);
                }
                throw ex;
            }
        }
    }

    private BigDecimal buscarRecargaExistente(
            Connection conexion,
            long idCliente,
            String metodo,
            Importes importes,
            String referencia
    ) throws SQLException {
        String sql = """
            SELECT r.id_cuenta, c.id_cliente, r.metodo, r.monto,
                   r.monto_comision, r.monto_acreditado, r.es_simulada,
                   r.estado, c.saldo, m.id_movimiento
            FROM dbo.Recarga AS r
                WITH (UPDLOCK, HOLDLOCK, INDEX(UX_Recarga_Referencia))
            INNER JOIN dbo.Cuenta AS c ON c.id_cuenta = r.id_cuenta
            LEFT JOIN dbo.MovimientoSaldo AS m
                ON m.id_recarga = r.id_recarga
            WHERE r.referencia = ?
            """;

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, referencia);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                boolean mismaOperacion =
                        rs.getLong("id_cliente") == idCliente
                        && metodo.equalsIgnoreCase(rs.getString("metodo"))
                        && importes.montoBruto().compareTo(
                                rs.getBigDecimal("monto")) == 0
                        && importes.comision().compareTo(
                                rs.getBigDecimal("monto_comision")) == 0
                        && importes.montoAcreditado().compareTo(
                                rs.getBigDecimal("monto_acreditado")) == 0
                        && rs.getBoolean("es_simulada")
                        && "CONFIRMADA".equalsIgnoreCase(
                                rs.getString("estado"));
                long idMovimiento = rs.getLong("id_movimiento");
                boolean tieneMovimiento = !rs.wasNull();
                BigDecimal saldoActual = rs.getBigDecimal("saldo");

                if (rs.next()) {
                    throw new SQLException(
                            "La clave de idempotencia tiene registros "
                            + "financieros inconsistentes."
                    );
                }
                if (!mismaOperacion || !tieneMovimiento) {
                    throw new SQLException(
                            "La clave de idempotencia ya se utilizó para "
                            + "otra operación o tiene un registro incompleto."
                    );
                }
                if (idMovimiento <= 0 || saldoActual == null) {
                    throw new SQLException(
                            "La recarga repetida no tiene un saldo válido."
                    );
                }

                return saldoActual;
            }
        }
    }




    //metodo de buscar por ID
        public ClienteTelefonia buscarPorId(long idCliente) throws SQLException {
            String sql =
                "SELECT " +
                "cl.id_cliente, cl.nombre, cl.apellido, " +
                "cl.telefono, cl.correo, cl.estado, " +
                "c.saldo, c.estado AS estado_cuenta " +
                "FROM dbo.Cliente cl " +
                "INNER JOIN dbo.Cuenta c " +
                "ON c.id_cliente = cl.id_cliente " +
                "WHERE cl.id_cliente = ?";

            try (Connection cn = DatabaseDTO.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {

                ps.setLong(1, idCliente);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }

                    ClienteTelefonia cliente = new ClienteTelefonia();

                    cliente.setIdCliente(rs.getLong("id_cliente"));
                    cliente.setNombre(rs.getString("nombre"));
                    cliente.setApellido(rs.getString("apellido"));
                    cliente.setTelefono(rs.getString("telefono"));
                    cliente.setCorreo(rs.getString("correo"));
                    cliente.setEstado(rs.getString("estado"));

                    // Cargar el saldo que realmente está en SQL Server.
                    double saldoActual = rs.getBigDecimal("saldo").doubleValue();
                    cliente.setSaldoFinal(saldoActual);

                    return cliente;
                }
            }
        }

}
