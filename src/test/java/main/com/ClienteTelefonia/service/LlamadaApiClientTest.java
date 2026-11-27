package main.com.ClienteTelefonia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LlamadaApiClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ObjectNode> llamadas = new CopyOnWriteArrayList<>();
    private final Set<Long> finalizacionesSolicitadas =
            ConcurrentHashMap.newKeySet();
    private final AtomicBoolean rechazarAutenticacion =
            new AtomicBoolean();
    private final AtomicBoolean respuestaHistorialInvalida =
            new AtomicBoolean();
    private HttpServer server;
    private String previousApiUrl;

    @BeforeEach
    void iniciarServidorDeContrato() throws IOException {
        previousApiUrl = System.getProperty("llamadas.api.url");
        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0
        );
        server.createContext("/api/llamadas/finalizar", this::finalizar);
        server.createContext("/api/llamadas", this::coleccion);
        server.start();
        System.setProperty(
                "llamadas.api.url",
                "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    @AfterEach
    void detenerServidorDeContrato() {
        if (server != null) {
            server.stop(0);
        }
        if (previousApiUrl == null) {
            System.clearProperty("llamadas.api.url");
        } else {
            System.setProperty("llamadas.api.url", previousApiUrl);
        }
    }

    @Test
    void iniciaConsultaYFinalizaMultiplesLlamadasConSesion()
            throws Exception {
        LlamadaApiClient client = new LlamadaApiClient("session-token");
        UUID primeraClave = UUID.randomUUID();

        long primera = client.iniciar("+51911111111", primeraClave);
        assertEquals(primera, client.iniciar(
                "+51911111111", primeraClave
        ));
        long segunda = client.iniciar(
                "+51922222222", UUID.randomUUID()
        );

        List<LlamadaApiClient.LlamadaVista> historial = client.listar();
        assertEquals(2, historial.size());
        assertEquals(Set.of(primera, segunda), Set.of(
                historial.get(0).idLlamada(),
                historial.get(1).idLlamada()
        ));

        client.finalizar(primera);

        assertTrue(finalizacionesSolicitadas.contains(primera));
        assertEquals(2, client.listar().size());
    }

    @Test
    void rechazaCredencialesInvalidasDelServidor() {
        rechazarAutenticacion.set(true);
        LlamadaApiClient client = new LlamadaApiClient("invalid-token");

        IOException error = assertThrows(
                IOException.class,
                client::listar
        );

        assertTrue(error.getMessage().contains("Sesión inválida"));
    }

    @Test
    void rechazaHistorialConCamposFinancierosInvalidos()
            throws Exception {
        LlamadaApiClient client = new LlamadaApiClient("session-token");
        client.iniciar("+51911111111", UUID.randomUUID());
        respuestaHistorialInvalida.set(true);

        IOException error = assertThrows(
                IOException.class,
                client::listar
        );

        assertTrue(error.getMessage().contains("costo"));
    }

    private void coleccion(HttpExchange exchange) throws IOException {
        if (!autenticado(exchange)) {
            responder(exchange, 401, "{\"error\":\"Sesión inválida\"}");
            return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            String clave = exchange.getRequestHeaders()
                    .getFirst("Idempotency-Key");
            JsonNode body = mapper.readTree(exchange.getRequestBody());
            if (clave == null || !body.hasNonNull("destino")) {
                responder(exchange, 400, "{\"error\":\"Datos inválidos\"}");
                return;
            }
            ObjectNode llamada = llamadas.stream()
                    .filter(item -> clave.equals(item.path("clave").asText()))
                    .findFirst()
                    .orElse(null);
            if (llamada == null) {
                llamada = mapper.createObjectNode();
                llamada.put("idLlamada", llamadas.size() + 1);
                llamada.put("clave", clave);
                llamada.put("destino", body.path("destino").asText());
                llamada.put("estado", "SOLICITADA");
                llamada.put("estadoSolicitudTelnyx", "PENDIENTE");
                llamada.put("duracionSegundos", 0);
                llamada.put("costo", "0.00");
                llamada.put("moneda", "PEN");
                llamada.put("fechaSolicitud", "2026-11-25T18:00:00Z");
                llamada.putNull("motivoFinalizacion");
                llamadas.add(llamada);
            }
            responder(exchange, 202, """
                    {"idLlamada":%d,"estado":"SOLICITADA"}
                    """.formatted(llamada.path("idLlamada").asLong()));
            return;
        }
        if ("GET".equals(exchange.getRequestMethod())) {
            ArrayNode resultado = mapper.createArrayNode();
            llamadas.forEach(item -> {
                ObjectNode vista = item.deepCopy();
                vista.remove("clave");
                if (respuestaHistorialInvalida.get()) {
                    vista.putNull("costo");
                }
                resultado.add(vista);
            });
            responder(exchange, 200, mapper.writeValueAsString(resultado));
            return;
        }
        responder(exchange, 405, "{\"error\":\"Método no permitido\"}");
    }

    private void finalizar(HttpExchange exchange) throws IOException {
        if (!autenticado(exchange)) {
            responder(exchange, 401, "{\"error\":\"Sesión inválida\"}");
            return;
        }
        JsonNode body = mapper.readTree(exchange.getRequestBody());
        finalizacionesSolicitadas.add(body.path("idLlamada").asLong());
        responder(exchange, 202, "{\"solicitada\":true}");
    }

    private boolean autenticado(HttpExchange exchange) {
        return !rechazarAutenticacion.get()
                && "Bearer session-token".equals(
                        exchange.getRequestHeaders()
                                .getFirst("Authorization")
                );
    }

    private void responder(
            HttpExchange exchange,
            int status,
            String json
    ) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
