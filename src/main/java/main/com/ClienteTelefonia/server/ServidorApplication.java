
package main.com.ClienteTelefonia.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import main.com.ClienteTelefonia.server.controller.AuthController;
import main.com.ClienteTelefonia.server.security.AuthGuard;
import main.com.ClienteTelefonia.server.security.AuthSessionService;
import main.com.ClienteTelefonia.server.security.Permiso;

public class ServidorApplication {

    private static final int PUERTO = 8081;

    public static void main(String[] args) {

        try {
            // 1. Crear servidor HTTP.
            HttpServer servidor = HttpServer.create(
                    new InetSocketAddress(PUERTO),
                    0
            );

            // 2. Inicializar servicios de autenticación.
            AuthSessionService sessionService =
                    new AuthSessionService();

            AuthController authController =
                    new AuthController(sessionService);

            // 3. Inicializar protección RBAC. ROLE-BASAD ACCESS CONTROL.
            AuthGuard authGuard =
                    new AuthGuard(sessionService);

            // 4. Endpoint público: Health.
            servidor.createContext("/api/health", exchange -> {

                if (!"GET".equalsIgnoreCase(
                        exchange.getRequestMethod())) {

                    exchange.getResponseHeaders().set(
                            "Allow", "GET"
                    );

                    byte[] error =
                            "{\"error\":\"Método no permitido\"}"
                                    .getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set(
                            "Content-Type",
                            "application/json; charset=UTF-8"
                    );

                    exchange.getResponseHeaders().set(
                            "Cache-Control", "no-store"
                    );

                    exchange.sendResponseHeaders(
                            405, error.length
                    );

                    try (var salida = exchange.getResponseBody()) {
                        salida.write(error);
                    }

                    return;
                }

                if (!"/api/health".equals(
                        exchange.getRequestURI().getPath())) {

                    byte[] error =
                            "{\"error\":\"Ruta no encontrada\"}"
                                    .getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set(
                            "Content-Type",
                            "application/json; charset=UTF-8"
                    );

                    exchange.getResponseHeaders().set(
                            "Cache-Control", "no-store"
                    );

                    exchange.sendResponseHeaders(
                            404, error.length
                    );

                    try (var salida = exchange.getResponseBody()) {
                        salida.write(error);
                    }

                    return;
                }

                String respuesta = """
                    {
                        "status": "OK",
                        "message": "Servidor disponible"
                    }
                    """;

                byte[] contenido =
                        respuesta.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                exchange.getResponseHeaders().set(
                        "Cache-Control", "no-store"
                );

                exchange.sendResponseHeaders(
                        200, contenido.length
                );

                try (var salida = exchange.getResponseBody()) {
                    salida.write(contenido);
                }
            });

            // 5. Endpoint público: Login.
            servidor.createContext(
                    "/api/auth/login",
                    authController::login
            );

            // 6. Endpoint protegido: Consultar sesión.
            servidor.createContext(
                    "/api/auth/me",
                    authGuard.proteger(
                            "/api/auth/me",
                            "GET",
                            Permiso.SESION_VER_PROPIA,
                            authController::me
                    )
            );

            // 7. Endpoint protegido: Cerrar sesión.
            servidor.createContext(
                    "/api/auth/logout",
                    authGuard.proteger(
                            "/api/auth/logout",
                            "POST",
                            Permiso.SESION_CERRAR_PROPIA,
                            authController::logout
                    )
            );

            // 8. Configurar la ejecución.
            servidor.setExecutor(
                    Executors.newFixedThreadPool(10)
            );

            // 9. Iniciar servidor.
            servidor.start();

            System.out.println("--------------------------------");
            System.out.println(" SERVIDOR GESTIONLLAMADAS");
            System.out.println("--------------------------------");
            System.out.println("Puerto: " + PUERTO);
            System.out.println("Estado: ACTIVO");
            System.out.println(
                    "Health: http://localhost:8081/api/health"
            );
            System.out.println(
                    "Login: POST http://localhost:8081/api/auth/login"
            );
            System.out.println(
                    "Me: GET http://localhost:8081/api/auth/me"
            );
            System.out.println(
                    "Logout: POST http://localhost:8081/api/auth/logout"
            );
            System.out.println("--------------------------------");

        } catch (IOException e) {

            System.err.println(
                    "No se pudo iniciar el servidor: "
                    + e.getMessage()
            );
        }
    }
}
