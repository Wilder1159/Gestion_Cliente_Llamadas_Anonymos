package main.com.ClienteTelefonia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.StringJoiner;

import main.com.ClienteTelefonia.dto.EnvDTO;

public class TelnyxLlamadaService {

    private static final String API_URL = "https://api.telnyx.com/v2/calls";

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String connectionId;
    private final String numeroOrigen;

    public record LlamadaAceptada(
            String callControlId,
            String callSessionId
    ) {
    }

    public static final class SolicitudTelnyxException
            extends Exception {
        private final boolean resultadoIncierto;

        public SolicitudTelnyxException(
                String mensaje,
                boolean resultadoIncierto,
                Throwable causa
        ) {
            super(mensaje, causa);
            this.resultadoIncierto = resultadoIncierto;
        }

        public boolean resultadoIncierto() {
            return resultadoIncierto;
        }
    }

    public TelnyxLlamadaService() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        apiKey = EnvDTO.obtener("TELNYX_API_KEY");
        connectionId = EnvDTO.obtener("TELNYX_CONNECTION_ID");
        numeroOrigen = EnvDTO.obtener("TELNYX_FROM_NUMBER");
    }

    public LlamadaAceptada realizarLlamada(
            String numeroDestino,
            long idLlamada
    ) throws SolicitudTelnyxException {
        if (numeroDestino == null
                || !numeroDestino.matches("^\\+[1-9]\\d{7,14}$")
                || idLlamada <= 0) {
            throw new IllegalArgumentException(
                    "Destino o identificador de llamada inválido."
            );
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("connection_id", connectionId);
        payload.put("to", numeroDestino);
        payload.put("from", numeroOrigen);
        payload.put("timeout_secs", 30);
        payload.put("client_state", codificarClientState(idLlamada));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        payload.toString(), StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> response = enviar(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            boolean incierta = response.statusCode() >= 500
                    || response.statusCode() == 408
                    || response.statusCode() == 429;
            throw new SolicitudTelnyxException(
                    "Telnyx respondió HTTP " + response.statusCode()
                    + ". Detalle: "
                    + detalleRespuestaError(response.body()),
                    incierta,
                    null
            );
        }

        try {
            JsonNode data = mapper.readTree(response.body()).path("data");
            String controlId = texto(data, "call_control_id");
            String sessionId = texto(data, "call_session_id");
            if (controlId == null) {
                throw new SolicitudTelnyxException(
                        "Telnyx aceptó la solicitud sin devolver "
                        + "call_control_id.",
                        true,
                        null
                );
            }
            return new LlamadaAceptada(controlId, sessionId);
        } catch (IOException e) {
            throw new SolicitudTelnyxException(
                    "La respuesta de Telnyx no pudo interpretarse.",
                    true,
                    e
            );
        }
    }

    public void finalizarLlamada(String callControlId)
            throws SolicitudTelnyxException {
        String encoded = codificarCallControlId(callControlId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + encoded
                        + "/actions/hangup"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = enviar(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SolicitudTelnyxException(
                    "Telnyx respondió HTTP " + response.statusCode()
                    + " al finalizar la llamada. Detalle: "
                    + detalleRespuestaError(response.body()),
                    response.statusCode() >= 500
                            || response.statusCode() == 408
                            || response.statusCode() == 429,
                    null
            );
        }
    }

    private HttpResponse<String> enviar(HttpRequest request)
            throws SolicitudTelnyxException {
        try {
            return httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SolicitudTelnyxException(
                    "Se interrumpió la comunicación con Telnyx.",
                    true,
                    e
            );
        } catch (IOException e) {
            throw new SolicitudTelnyxException(
                    "No se pudo confirmar la respuesta de Telnyx.",
                    true,
                    e
            );
        }
    }

    static String codificarCallControlId(String callControlId) {
        if (callControlId == null
                || !callControlId.matches(
                        "[A-Za-z0-9._~!$&'()*+,;=:@-]{1,150}"
                )) {
            throw new IllegalArgumentException(
                    "Identificador Telnyx inválido."
            );
        }
        return URLEncoder.encode(callControlId, StandardCharsets.UTF_8);
    }

    static String codificarClientState(long idLlamada) {
        return java.util.Base64.getEncoder().encodeToString(
                Long.toString(idLlamada).getBytes(StandardCharsets.UTF_8)
        );
    }

    static String detalleRespuestaError(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return "Telnyx no proporcionó detalles.";
        }

        try {
            JsonNode errores = new ObjectMapper()
                    .readTree(cuerpo)
                    .path("errors");
            if (errores.isArray()) {
                StringJoiner detalles = new StringJoiner(" | ");
                for (int i = 0; i < Math.min(errores.size(), 5); i++) {
                    JsonNode error = errores.get(i);
                    StringJoiner campos = new StringJoiner(", ");
                    agregarCampo(campos, "code", error.path("code"));
                    agregarCampo(campos, "title", error.path("title"));
                    agregarCampo(campos, "detail", error.path("detail"));
                    agregarCampo(
                            campos,
                            "source",
                            error.path("source").path("pointer")
                    );
                    if (campos.length() > 0) {
                        detalles.add(campos.toString());
                    }
                }
                if (detalles.length() > 0) {
                    return limitar(detalles.toString(), 1000);
                }
            }
        } catch (IOException e) {
            return "Respuesta no JSON: " + limitar(cuerpo, 500);
        }

        return "Respuesta: " + limitar(cuerpo, 500);
    }

    private static void agregarCampo(
            StringJoiner campos,
            String nombre,
            JsonNode valor
    ) {
        if (valor.isTextual() && !valor.asText().isBlank()) {
            campos.add(nombre + "=" + valor.asText());
        }
    }

    private static String limitar(String texto, int maximo) {
        return texto.length() <= maximo
                ? texto
                : texto.substring(0, maximo) + "...";
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || !valor.isTextual()
                || valor.asText().isBlank()
                ? null
                : valor.asText();
    }
}
