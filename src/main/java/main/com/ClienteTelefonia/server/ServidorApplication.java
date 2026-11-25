
package main.com.ClienteTelefonia.server;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ServidorApplication {

    private static final int PUERTO = 8081;

    public static void main(String[] args) {

        try {
            HttpServer servidor = HttpServer.create(
                new InetSocketAddress(PUERTO),
                0
            );

            servidor.createContext("/api/health", exchange -> {

                String respuesta = """
                    {
                        "status": "OK",
                        "message": "Servidor disponible"
                    }
                    """;

                byte[] contenido = respuesta.getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
                );

                exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                );

                exchange.sendResponseHeaders(200, contenido.length);

                try (var salida = exchange.getResponseBody()) {
                    salida.write(contenido);
                }
            });

            servidor.setExecutor(
                Executors.newFixedThreadPool(10)
            );

            servidor.start();

            System.out.println("--------------------------------");
            System.out.println(" SERVIDOR GESTIONLLAMADAS");
            System.out.println("--------------------------------");
            System.out.println("Puerto: " + PUERTO);
            System.out.println("Estado: ACTIVO");
            System.out.println(
                "Health: http://localhost:8081/api/health"
            );

        } catch (IOException e) {
            System.err.println(
                "No se pudo iniciar el servidor: " + e.getMessage()
            );
        }
    }
}
