package main.com.ClienteTelefonia.service;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import main.com.ClienteTelefonia.dao.LlamadaDAO;
import main.com.ClienteTelefonia.model.LlamadaRegistro;

public final class LlamadaManager implements AutoCloseable {

    private final LlamadaDAO llamadaDAO;
    private final TelnyxLlamadaService telnyx;
    private final ThreadPoolExecutor operacionesTelnyx;
    private final ScheduledExecutorService tareas;
    private final ConcurrentHashMap<String, Boolean> enCola =
            new ConcurrentHashMap<>();

    public LlamadaManager() throws SQLException {
        this(new LlamadaDAO(), new TelnyxLlamadaService());
    }

    public LlamadaManager(
            LlamadaDAO llamadaDAO,
            TelnyxLlamadaService telnyx
    ) throws SQLException {
        this.llamadaDAO = llamadaDAO;
        this.telnyx = telnyx;
        operacionesTelnyx = new ThreadPoolExecutor(
                4,
                4,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                runnable -> {
                    Thread thread = new Thread(
                            runnable, "telnyx-call-operation"
                    );
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
        tareas = java.util.concurrent.Executors
                .newScheduledThreadPool(3, runnable -> {
                    Thread thread = new Thread(
                            runnable, "llamada-background-worker"
                    );
                    thread.setDaemon(true);
                    return thread;
                });

        llamadaDAO.recuperarSolicitudesEnviando();
        tareas.scheduleWithFixedDelay(
                this::recuperarSolicitudes, 0, 3, TimeUnit.SECONDS
        );
        tareas.scheduleWithFixedDelay(
                this::procesarEvento, 0, 1, TimeUnit.SECONDS
        );
        tareas.scheduleWithFixedDelay(
                this::finalizarVencidas, 5, 5, TimeUnit.SECONDS
        );
    }

    public LlamadaDAO.SolicitudCreada iniciar(
            long idCliente,
            String destino,
            String idempotencyKey
    ) throws SQLException {
        LlamadaDAO.SolicitudCreada solicitud =
                llamadaDAO.crearSolicitud(
                        idCliente, destino, idempotencyKey
                );
        if ("PENDIENTE".equals(solicitud.estadoSolicitudTelnyx())) {
            encolar(solicitud);
        }
        return solicitud;
    }

    public List<LlamadaRegistro> listar(long idCliente)
            throws SQLException {
        return llamadaDAO.listarPorCliente(idCliente);
    }

    public void finalizar(long idCliente, long idLlamada)
            throws SQLException {
        if (!llamadaDAO.solicitarCancelacion(idCliente, idLlamada)) {
            throw new IllegalArgumentException(
                    "La llamada no existe, no te pertenece o ya finalizó."
            );
        }
        String callControlId = llamadaDAO.buscarCallControlId(
                idCliente, idLlamada
        );
        if (callControlId == null) {
            llamadaDAO.deshacerSolicitudCancelacion(idLlamada);
            throw new IllegalStateException(
                    "Todavía no se recibió el identificador de Telnyx; "
                    + "la llamada no se puede finalizar en este momento."
            );
        }
        String claveCola = claveCola("finalizar", idLlamada);
        if (enCola.putIfAbsent(claveCola, Boolean.TRUE) != null) {
            return;
        }
        try {
            operacionesTelnyx.execute(() ->
                    solicitarFinalizacion(
                            idLlamada, callControlId, claveCola
                    )
            );
        } catch (RejectedExecutionException e) {
            enCola.remove(claveCola);
            llamadaDAO.deshacerSolicitudCancelacion(idLlamada);
            throw new IllegalStateException(
                    "El servidor está ocupado; intenta de nuevo.",
                    e
            );
        }
    }

    private void encolar(LlamadaDAO.SolicitudCreada solicitud)
            throws SQLException {
        String claveCola = claveCola("crear", solicitud.idLlamada());
        if (enCola.putIfAbsent(claveCola, Boolean.TRUE) != null) {
            return;
        }
        try {
            operacionesTelnyx.execute(
                    () -> enviarSolicitud(solicitud, claveCola)
            );
        } catch (RejectedExecutionException e) {
            enCola.remove(claveCola);
            llamadaDAO.marcarSolicitudFallida(
                    solicitud.idLlamada(),
                    "Servidor ocupado; no se envió a Telnyx"
            );
            throw new IllegalStateException(
                    "No se pudo encolar la llamada porque el servidor "
                    + "está ocupado.",
                    e
            );
        }
    }

    private void enviarSolicitud(
            LlamadaDAO.SolicitudCreada solicitud,
            String claveCola
    ) {
        try {
            if (!llamadaDAO.marcarSolicitudEnviando(
                    solicitud.idLlamada()
            )) {
                return;
            }
            try {
                TelnyxLlamadaService.LlamadaAceptada aceptada =
                        telnyx.realizarLlamada(
                                solicitud.destino(),
                                solicitud.idLlamada()
                        );
                llamadaDAO.marcarSolicitudAceptada(
                        solicitud.idLlamada(),
                        aceptada.callControlId(),
                        aceptada.callSessionId()
                );
            } catch (TelnyxLlamadaService.SolicitudTelnyxException e) {
                if (e.resultadoIncierto()) {
                    llamadaDAO.marcarSolicitudIncierta(
                            solicitud.idLlamada()
                    );
                } else {
                    llamadaDAO.marcarSolicitudFallida(
                            solicitud.idLlamada(), e.getMessage()
                    );
                }
                System.err.println(
                        "Resultado Telnyx para llamada "
                        + solicitud.idLlamada() + ": " + e.getMessage()
                );
            }
        } catch (SQLException | RuntimeException e) {
            System.err.println(
                    "Error al procesar solicitud de llamada "
                    + solicitud.idLlamada() + ": " + e.getMessage()
            );
            try {
                llamadaDAO.marcarSolicitudIncierta(
                        solicitud.idLlamada()
                );
            } catch (SQLException persistenciaError) {
                e.addSuppressed(persistenciaError);
                System.err.println(
                        "No se pudo persistir estado incierto para llamada "
                        + solicitud.idLlamada() + ": "
                        + persistenciaError.getMessage()
                );
            }
        } finally {
            enCola.remove(claveCola);
        }
    }

    private void recuperarSolicitudes() {
        try {
            for (LlamadaDAO.SolicitudCreada solicitud
                    : llamadaDAO.listarSolicitudesPendientes()) {
                encolar(solicitud);
            }
        } catch (SQLException | RuntimeException e) {
            System.err.println(
                    "Error al recuperar llamadas pendientes: "
                    + e.getMessage()
            );
        }
    }

    private void procesarEvento() {
        try {
            LlamadaDAO.EventoTelnyx evento =
                    llamadaDAO.reclamarEventoPendiente();
            if (evento != null) {
                llamadaDAO.procesarEvento(evento);
            }
        } catch (SQLException | RuntimeException e) {
            System.err.println(
                    "Error al procesar evento Telnyx: " + e.getMessage()
            );
        }
    }

    private void finalizarVencidas() {
        try {
            for (LlamadaDAO.LlamadaParaFinalizar llamada
                    : llamadaDAO.listarVencidas()) {
                if (!llamadaDAO.marcarCancelacionAutomatica(
                        llamada.idLlamada()
                )) {
                    continue;
                }
                String claveCola = claveCola(
                        "finalizar", llamada.idLlamada()
                );
                if (enCola.putIfAbsent(claveCola, Boolean.TRUE) != null) {
                    continue;
                }
                try {
                    operacionesTelnyx.execute(() ->
                            solicitarFinalizacion(
                                    llamada.idLlamada(),
                                    llamada.callControlId(),
                                    claveCola
                            )
                    );
                } catch (RejectedExecutionException e) {
                    enCola.remove(claveCola);
                    llamadaDAO.deshacerSolicitudCancelacion(
                            llamada.idLlamada()
                    );
                    System.err.println(
                            "No se pudo encolar la finalización automática "
                            + "de la llamada " + llamada.idLlamada()
                    );
                }
            }
        } catch (SQLException | RuntimeException e) {
            System.err.println(
                    "Error al buscar llamadas vencidas: " + e.getMessage()
            );
        }
    }

    private void solicitarFinalizacion(
            long idLlamada,
            String callControlId,
            String claveCola
    ) {
        try {
            telnyx.finalizarLlamada(callControlId);
        } catch (TelnyxLlamadaService.SolicitudTelnyxException e) {
            System.err.println(
                    "No se pudo confirmar la finalización Telnyx de "
                    + idLlamada + ": " + e.getMessage()
            );
        } finally {
            enCola.remove(claveCola);
        }
    }

    private String claveCola(String operacion, long idLlamada) {
        return operacion + ":" + idLlamada;
    }

    @Override
    public void close() {
        tareas.shutdown();
        operacionesTelnyx.shutdown();
    }
}
