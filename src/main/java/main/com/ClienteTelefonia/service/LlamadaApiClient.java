package main.com.ClienteTelefonia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LlamadaApiClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI baseUri;
    private final String token;

    public record LlamadaVista(
            long idLlamada,
            String destino,
            String estado,
            String estadoSolicitudTelnyx,
            int duracionSegundos,
            BigDecimal costo,
            String moneda,
            Instant fechaSolicitud,
            String motivoFinalizacion
    ) {
        @Override
        public String toString() {
            String estadoVisible = "PENDIENTE".equals(estadoSolicitudTelnyx)
                    || "INCIERTA".equals(estadoSolicitudTelnyx)
                    ? estado + " (" + estadoSolicitudTelnyx + ")"
                    : estado;
            return "#" + idLlamada + " | " + destino + " | "
                    + estadoVisible + " | S/ " + costo.toPlainString();
        }
    }

    public LlamadaApiClient(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "No hay una sesión autenticada para llamadas."
            );
        }
        String configured = System.getProperty(
                "llamadas.api.url", "http://localhost:8081"
        );
        if (!configured.matches("^https?://[^\\s]+$")) {
            throw new IllegalArgumentException(
                    "llamadas.api.url debe ser una URL HTTP(S) válida."
            );
        }
        this.baseUri = URI.create(configured.replaceAll("/+$", ""));
        this.token = token;
    }

    public long iniciar(String destino, UUID claveIdempotencia)
            throws IOException, InterruptedException {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("destino", destino);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUri.resolve("/api/llamadas"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header(
                        "Idempotency-Key",
                        claveIdempotencia.toString()
                )
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        payload.toString(), StandardCharsets.UTF_8
                ))
                .build();
        JsonNode response = enviar(request, 202);
        if (response == null || !response.isObject()) {
            throw new IOException(
                    "El servidor devolvió una respuesta de llamada inválida."
            );
        }
        long idLlamada = response.path("idLlamada").asLong();
        if (idLlamada <= 0) {
            throw new IOException(
                    "El servidor no devolvió un identificador de llamada."
            );
        }
        return idLlamada;
    }

    public List<LlamadaVista> listar()
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUri.resolve("/api/llamadas"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        JsonNode response = enviar(request, 200);
        if (!response.isArray()) {
            throw new IOException(
                    "La respuesta del servidor de llamadas no es válida."
            );
        }
        List<LlamadaVista> llamadas = new ArrayList<>();
        for (JsonNode item : response) {
            if (!item.isObject()) {
                throw new IOException(
                        "El historial contiene una llamada inválida."
                );
            }
            long idLlamada = enteroLargo(item, "idLlamada");
            String destino = textoObligatorio(item, "destino");
            String estado = textoObligatorio(item, "estado");
            String estadoSolicitud =
                    textoObligatorio(item, "estadoSolicitudTelnyx");
            int duracion = entero(item, "duracionSegundos");
            BigDecimal costo = decimal(item, "costo");
            String moneda = textoObligatorio(item, "moneda");
            Instant fecha = parsearFecha(
                    textoObligatorio(item, "fechaSolicitud")
            );
            llamadas.add(new LlamadaVista(
                    idLlamada,
                    destino,
                    estado,
                    estadoSolicitud,
                    duracion,
                    costo,
                    moneda,
                    fecha,
                    item.path("motivoFinalizacion").asText(null)
            ));
        }
        return llamadas;
    }

    public void finalizar(long idLlamada)
            throws IOException, InterruptedException {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("idLlamada", idLlamada);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUri.resolve("/api/llamadas/finalizar"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        payload.toString(), StandardCharsets.UTF_8
                ))
                .build();
        enviar(request, 202);
    }

    private JsonNode enviar(HttpRequest request, int expectedStatus)
            throws IOException, InterruptedException {
        HttpResponse<byte[]> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );
        JsonNode body;
        try {
            body = mapper.readTree(response.body());
        } catch (IOException e) {
            throw new IOException(
                    "El servidor de llamadas devolvió una respuesta inválida.",
                    e
            );
        }
        if (response.statusCode() != expectedStatus) {
            String message = body != null
                    ? body.path("error").asText(
                        "HTTP " + response.statusCode()
                    )
                    : "HTTP " + response.statusCode();
            throw new IOException(message);
        }
        return body;
    }

    private Instant parsearFecha(String valor) throws IOException {
        if (valor == null || valor.isBlank()) {
            throw new IOException("La fecha de solicitud es inválida.");
        }
        try {
            return Instant.parse(valor);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IOException(
                    "La fecha de solicitud es inválida.",
                    e
            );
        }
    }

    private long enteroLargo(JsonNode objeto, String campo)
            throws IOException {
        JsonNode valor = objeto.get(campo);
        if (valor == null || !valor.isIntegralNumber()
                || !valor.canConvertToLong() || valor.asLong() <= 0) {
            throw new IOException(
                    "El campo " + campo + " es inválido."
            );
        }
        return valor.asLong();
    }

    private int entero(JsonNode objeto, String campo) throws IOException {
        JsonNode valor = objeto.get(campo);
        if (valor == null || !valor.isIntegralNumber()
                || !valor.canConvertToInt() || valor.asInt() < 0) {
            throw new IOException(
                    "El campo " + campo + " es inválido."
            );
        }
        return valor.asInt();
    }

    private BigDecimal decimal(JsonNode objeto, String campo)
            throws IOException {
        JsonNode valor = objeto.get(campo);
        if (valor == null || valor.isNull()
                || !(valor.isNumber() || valor.isTextual())) {
            throw new IOException(
                    "El campo " + campo + " es inválido."
            );
        }
        try {
            return new BigDecimal(valor.asText());
        } catch (NumberFormatException e) {
            throw new IOException(
                    "El campo " + campo + " es inválido.",
                    e
            );
        }
    }

    private String textoObligatorio(JsonNode objeto, String campo)
            throws IOException {
        JsonNode valor = objeto.get(campo);
        if (valor == null || !valor.isTextual()
                || valor.asText().isBlank()) {
            throw new IOException(
                    "El campo " + campo + " es inválido."
            );
        }
        return valor.asText();
    }
}
