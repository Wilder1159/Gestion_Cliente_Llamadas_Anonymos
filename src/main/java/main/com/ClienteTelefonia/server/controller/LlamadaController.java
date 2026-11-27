package main.com.ClienteTelefonia.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import main.com.ClienteTelefonia.dao.LlamadaDAO;
import main.com.ClienteTelefonia.model.LlamadaRegistro;
import main.com.ClienteTelefonia.server.security.AuthGuard;
import main.com.ClienteTelefonia.server.security.Permiso;
import main.com.ClienteTelefonia.service.LlamadaManager;

public final class LlamadaController {

    private static final int MAX_BODY_BYTES = 8192;
    private final ObjectMapper mapper = new ObjectMapper();
    private final LlamadaManager manager;

    public LlamadaController(LlamadaManager manager) {
        this.manager = manager;
    }

    public void manejarColeccion(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "POST" -> iniciar(exchange);
            case "GET" -> listar(exchange);
            default -> {
                exchange.getResponseHeaders().set("Allow", "GET, POST");
                responder(
                        exchange,
                        405,
                        Map.of("error", "Método no permitido")
                );
            }
        }
    }

    public void iniciar(HttpExchange exchange) throws IOException {
        if (!rutaYMetodo(exchange, "/api/llamadas", "POST")) {
            return;
        }
        var sesion = AuthGuard.obtenerUsuario(exchange);
        if (sesion == null) {
            responder(exchange, 401, Map.of(
                    "error", "Usuario no autenticado"
            ));
            return;
        }

        JsonNode json = leerJson(exchange);
        if (json == null) {
            return;
        }
        if (!json.hasNonNull("destino")
                || !json.get("destino").isTextual()) {
            responder(exchange, 400, Map.of(
                    "error", "El destino es obligatorio"
            ));
            return;
        }

        String destino = json.get("destino").asText().trim();
        String idempotencyKey = exchange.getRequestHeaders()
                .getFirst("Idempotency-Key");
        try {
            LlamadaDAO.SolicitudCreada solicitud = manager.iniciar(
                    sesion.idCliente(),
                    destino,
                    idempotencyKey
            );
            responder(exchange, 202, Map.of(
                    "idLlamada", solicitud.idLlamada(),
                    "destino", solicitud.destino(),
                    "estado", solicitud.estado(),
                    "estadoSolicitudTelnyx",
                    solicitud.estadoSolicitudTelnyx(),
                    "reutilizada", !solicitud.nueva()
            ));
        } catch (LlamadaDAO.IdempotencyConflictException e) {
            responder(exchange, 409, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            responder(exchange, 400, Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            responder(exchange, 409, Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            System.err.println(
                    "Error al crear solicitud de llamada: " + e.getMessage()
            );
            responder(exchange, 500, Map.of(
                    "error", "No se pudo crear la solicitud de llamada"
            ));
        }
    }

    public void listar(HttpExchange exchange) throws IOException {
        if (!rutaYMetodo(exchange, "/api/llamadas", "GET")) {
            return;
        }
        var sesion = AuthGuard.obtenerUsuario(exchange);
        if (sesion == null) {
            responder(exchange, 401, Map.of(
                    "error", "Usuario no autenticado"
            ));
            return;
        }
        try {
            List<LlamadaRegistro> llamadas =
                    manager.listar(sesion.idCliente());
            ArrayNode resultado = mapper.createArrayNode();
            for (LlamadaRegistro llamada : llamadas) {
                ObjectNode item = resultado.addObject();
                item.put("idLlamada", llamada.idLlamada());
                item.put("destino", llamada.destino());
                item.put("estado", llamada.estado());
                item.put(
                        "estadoSolicitudTelnyx",
                        llamada.estadoSolicitudTelnyx()
                );
                item.put("duracionSegundos", llamada.duracionSegundos());
                item.put("costo", llamada.costo());
                item.put("moneda", llamada.moneda());
                agregarFecha(item, "fechaSolicitud", llamada.fechaSolicitud());
                agregarFecha(
                        item, "fechaContestacion",
                        llamada.fechaContestacion()
                );
                agregarFecha(
                        item, "fechaFinalizacion",
                        llamada.fechaFinalizacion()
                );
                if (llamada.motivoFinalizacion() == null) {
                    item.putNull("motivoFinalizacion");
                } else {
                    item.put(
                            "motivoFinalizacion",
                            llamada.motivoFinalizacion()
                    );
                }
            }
            responder(exchange, 200, resultado);
        } catch (SQLException e) {
            System.err.println(
                    "Error al consultar llamadas propias: " + e.getMessage()
            );
            responder(exchange, 500, Map.of(
                    "error", "No se pudo consultar el historial"
            ));
        }
    }

    public void finalizar(HttpExchange exchange) throws IOException {
        if (!rutaYMetodo(exchange, "/api/llamadas/finalizar", "POST")) {
            return;
        }
        var sesion = AuthGuard.obtenerUsuario(exchange);
        if (sesion == null) {
            responder(exchange, 401, Map.of(
                    "error", "Usuario no autenticado"
            ));
            return;
        }
        JsonNode json = leerJson(exchange);
        if (json == null) {
            return;
        }
        if (!json.hasNonNull("idLlamada")
                || !json.get("idLlamada").isIntegralNumber()
                || !json.get("idLlamada").canConvertToLong()
                || json.get("idLlamada").asLong() <= 0) {
            responder(exchange, 400, Map.of(
                    "error", "idLlamada debe ser un entero positivo"
            ));
            return;
        }
        try {
            manager.finalizar(
                    sesion.idCliente(),
                    json.get("idLlamada").asLong()
            );
            responder(exchange, 202, Map.of(
                    "solicitada", true,
                    "mensaje", "La finalización se solicitó a Telnyx"
            ));
        } catch (IllegalArgumentException e) {
            responder(exchange, 404, Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            responder(exchange, 409, Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            System.err.println(
                    "Error al finalizar llamada: " + e.getMessage()
            );
            responder(exchange, 500, Map.of(
                    "error", "No se pudo solicitar la finalización"
            ));
        }
    }

    private JsonNode leerJson(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody()
                .readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            responder(exchange, 413, Map.of(
                    "error", "Solicitud demasiado grande"
            ));
            return null;
        }
        try {
            JsonNode json = mapper.readTree(body);
            if (json == null || !json.isObject()) {
                responder(exchange, 400, Map.of("error", "JSON inválido"));
                return null;
            }
            return json;
        } catch (IOException e) {
            responder(exchange, 400, Map.of("error", "JSON inválido"));
            return null;
        }
    }

    private boolean rutaYMetodo(
            HttpExchange exchange,
            String ruta,
            String metodo
    ) throws IOException {
        if (!ruta.equals(exchange.getRequestURI().getPath())) {
            responder(exchange, 404, Map.of(
                    "error", "Ruta no encontrada"
            ));
            return false;
        }
        if (!metodo.equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", metodo);
            responder(exchange, 405, Map.of(
                    "error", "Método no permitido"
            ));
            return false;
        }
        return true;
    }

    private void responder(
            HttpExchange exchange,
            int status,
            Object body
    ) throws IOException {
        byte[] respuesta = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, respuesta.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(respuesta);
        }
    }

    private void agregarFecha(
            ObjectNode destino,
            String campo,
            java.time.LocalDateTime fecha
    ) {
        if (fecha == null) {
            destino.putNull(campo);
        } else {
            destino.put(
                    campo,
                    fecha.toInstant(ZoneOffset.UTC).toString()
            );
        }
    }
}
