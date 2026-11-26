
package main.com.ClienteTelefonia.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Map;

import main.com.ClienteTelefonia.controller.LoginController;
import main.com.ClienteTelefonia.model.Usuario;
import main.com.ClienteTelefonia.server.security.AuthGuard;
import main.com.ClienteTelefonia.server.security.AuthSessionService;
import main.com.ClienteTelefonia.dao.SesionDAO;

public class AuthController {

    private static final int MAX_BODY_BYTES = 8192;
    private static final int DURACION_SESION_SEGUNDOS = 3600;

    private final ObjectMapper mapper = new ObjectMapper();
    private final LoginController loginController = new LoginController();
    private final AuthSessionService sessionService;

    public AuthController(AuthSessionService sessionService) {
        if (sessionService == null) {
            throw new IllegalArgumentException(
                    "AuthSessionService no puede ser null."
            );
        }

        this.sessionService = sessionService;
    }

    // =====================================================
    // LOGIN
    // =====================================================

    public void login(HttpExchange exchange) throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "POST");

            responder(exchange, 405, Map.of(
                    "error", "Método no permitido"
            ));
            return;
        }

        byte[] body = exchange.getRequestBody()
                .readNBytes(MAX_BODY_BYTES + 1);

        if (body.length > MAX_BODY_BYTES) {
            responder(exchange, 413, Map.of(
                    "error", "Solicitud demasiado grande"
            ));
            return;
        }

        JsonNode json;

        try {
            json = mapper.readTree(body);
        } catch (IOException e) {
            responder(exchange, 400, Map.of(
                    "error", "JSON inválido"
            ));
            return;
        }

        if (json == null
                || !json.hasNonNull("username")
                || !json.hasNonNull("password")
                || !json.get("username").isTextual()
                || !json.get("password").isTextual()) {

            responder(exchange, 400, Map.of(
                    "error", "Usuario y contraseña son obligatorios"
            ));
            return;
        }

        String username = json.get("username").asText();
        char[] password = json.get("password").asText().toCharArray();

        try {

            Usuario usuario = loginController.autenticar(
                    username,
                    password
            );

            String token = sessionService.crearSesion(
                    usuario.getIdUsuario()
            );

            responder(exchange, 200, Map.of(
                    "autenticado", true,
                    "username", usuario.getUsername(),
                    "rol", usuario.getRol(),
                    "token", token,
                    "expiraEn", DURACION_SESION_SEGUNDOS
            ));

        } catch (IllegalArgumentException e) {

            responder(exchange, 401, Map.of(
                    "error", "Credenciales incorrectas"
            ));

        } catch (GeneralSecurityException | SQLException e) {

            System.err.println(
                    "Error interno de autenticación: " + e.getMessage()
            );

            responder(exchange, 500, Map.of(
                    "error", "No se pudo procesar la autenticación"
            ));
        }
    }

    // =====================================================
    // CONSULTAR SESIÓN ACTUAL
    // =====================================================

    public void me(HttpExchange exchange) throws IOException {

        /*
         * AuthGuard ya validó el token y consultó la sesión.
         * Recuperamos los datos del usuario autenticado.
         */
        SesionDAO.SesionActiva sesion =
                AuthGuard.obtenerUsuario(exchange);

        if (sesion == null) {
            responder(exchange, 401, Map.of(
                    "error", "Usuario no autenticado"
            ));
            return;
        }

        responder(exchange, 200, Map.of(
                "autenticado", true,
                "idUsuario", sesion.idUsuario(),
                "idCliente", sesion.idCliente(),
                "username", sesion.username(),
                "rol", sesion.rol()
        ));
    }

    // =====================================================
    // CERRAR SESIÓN
    // =====================================================

    public void logout(HttpExchange exchange) throws IOException {

        /*
         * El AuthGuard ya comprobó que el token es válido.
         * Recuperamos el token almacenado en el contexto.
         */
        String token = AuthGuard.obtenerTokenAutenticado(exchange);

        if (token == null) {
            responder(exchange, 401, Map.of(
                    "error", "Usuario no autenticado"
            ));
            return;
        }

        try {

            sessionService.cerrarSesion(token);

            responder(exchange, 200, Map.of(
                    "cerrada", true,
                    "mensaje", "Sesión cerrada correctamente"
            ));

        } catch (SQLException e) {

            System.err.println(
                    "Error al cerrar sesión: " + e.getMessage()
            );

            responder(exchange, 500, Map.of(
                    "error", "No se pudo cerrar la sesión"
            ));
        }
    }

    // =====================================================
    // RESPUESTA JSON
    // =====================================================

    private void responder(
            HttpExchange exchange,
            int status,
            Object contenido
    ) throws IOException {

        byte[] respuesta = mapper.writeValueAsBytes(contenido);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "Cache-Control",
                "no-store"
        );

        exchange.sendResponseHeaders(status, respuesta.length);

        try (var salida = exchange.getResponseBody()) {
            salida.write(respuesta);
        }
    }
}
