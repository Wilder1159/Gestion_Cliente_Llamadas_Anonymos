package main.com.ClienteTelefonia.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import main.com.ClienteTelefonia.dao.LlamadaDAO;
import main.com.ClienteTelefonia.server.security.TelnyxWebhookVerifier;

public final class TelnyxWebhookController {

    private static final int MAX_BODY_BYTES = 64 * 1024;
    private final ObjectMapper mapper = new ObjectMapper();
    private final LlamadaDAO llamadaDAO;
    private final TelnyxWebhookVerifier verifier;

    public TelnyxWebhookController(
            LlamadaDAO llamadaDAO,
            TelnyxWebhookVerifier verifier
    ) {
        this.llamadaDAO = llamadaDAO;
        this.verifier = verifier;
    }

    public void recibir(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "POST");
            responder(exchange, 405, "{\"error\":\"Método no permitido\"}");
            return;
        }

        byte[] body = exchange.getRequestBody()
                .readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            responder(exchange, 413, "{\"error\":\"Solicitud demasiado grande\"}");
            return;
        }

        boolean firmaValida = verifier.verificar(
                body,
                exchange.getRequestHeaders().getFirst(
                        "Telnyx-Signature-Ed25519"
                ),
                exchange.getRequestHeaders().getFirst("Telnyx-Timestamp")
        );
        if (!firmaValida) {
            responder(exchange, 401, "{\"error\":\"Firma inválida\"}");
            return;
        }

        final LlamadaDAO.EventoTelnyx evento;
        try {
            evento = parsearEvento(mapper.readTree(body));
        } catch (IllegalArgumentException | IOException e) {
            responder(exchange, 400, "{\"error\":\"Evento inválido\"}");
            return;
        }

        try {
            boolean nuevo = llamadaDAO.guardarEvento(evento);
            responder(
                    exchange,
                    202,
                    nuevo
                            ? "{\"recibido\":true}"
                            : "{\"recibido\":true,\"duplicado\":true}"
            );
        } catch (SQLException e) {
            System.err.println(
                    "No se pudo persistir webhook Telnyx: "
                    + e.getMessage()
            );
            responder(
                    exchange,
                    503,
                    "{\"error\":\"No se pudo registrar el evento\"}"
            );
        }
    }

    private LlamadaDAO.EventoTelnyx parsearEvento(JsonNode root) {
        JsonNode data = root == null ? null : root.get("data");
        JsonNode payload = data == null ? null : data.get("payload");
        if (data == null || !data.isObject()
                || payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("Estructura de evento inválida.");
        }

        String id = texto(data, "id");
        String tipo = texto(data, "event_type");
        String ocurrido = texto(data, "occurred_at");
        String callControlId = texto(payload, "call_control_id");
        String callSessionId = texto(payload, "call_session_id");
        if (id == null || id.length() > 128
                || tipo == null || tipo.length() > 80
                || ocurrido == null
                || (callControlId != null && callControlId.length() > 150)
                || (callSessionId != null && callSessionId.length() > 150)) {
            throw new IllegalArgumentException("Campos de evento inválidos.");
        }

        LocalDateTime fecha = parsearFecha(ocurrido);
        Integer duracion = enteroOpcional(payload, "call_duration_secs");
        if (duracion != null && (duracion < 0 || duracion > 86400)) {
            throw new IllegalArgumentException("Duración inválida.");
        }

        return new LlamadaDAO.EventoTelnyx(
                id,
                parsearClientState(texto(payload, "client_state")),
                callControlId,
                callSessionId,
                tipo,
                fecha,
                texto(payload, "hangup_cause"),
                texto(payload, "hangup_source"),
                duracion
        );
    }

    private LocalDateTime parsearFecha(String valor) {
        try {
            Instant instant = Instant.parse(valor);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (RuntimeException e) {
            try {
                return OffsetDateTime.parse(valor)
                        .withOffsetSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();
            } catch (RuntimeException invalid) {
                throw new IllegalArgumentException(
                        "Fecha del evento inválida.",
                        invalid
                );
            }
        }
    }

    private Long parsearClientState(String valor) {
        if (valor == null || valor.length() > 128) {
            return null;
        }
        try {
            byte[] decoded;
            try {
                decoded = Base64.getUrlDecoder().decode(valor);
            } catch (IllegalArgumentException e) {
                decoded = Base64.getDecoder().decode(valor);
            }
            String id = new String(decoded, StandardCharsets.UTF_8);
            if (!id.matches("[1-9]\\d{0,18}")) {
                return null;
            }
            return Long.parseLong(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer enteroOpcional(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        if (!valor.isIntegralNumber() || !valor.canConvertToInt()) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " debe ser entero."
            );
        }
        return valor.intValue();
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        if (!valor.isTextual()) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " debe ser texto."
            );
        }
        String texto = valor.asText();
        return texto.isBlank() ? null : texto;
    }

    private void responder(
            HttpExchange exchange,
            int status,
            String json
    ) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(body);
        }
    }
}
