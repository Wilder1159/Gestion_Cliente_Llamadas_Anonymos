
package main.com.ClienteTelefonia.server.security;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Set;
import java.util.Objects;
import java.util.function.Predicate;

import main.com.ClienteTelefonia.dao.SesionDAO;

public class AuthGuard {

    private static final String ATRIBUTO_AUTH =
            "usuario.autenticado";

    private static final String ATRIBUTO_TOKEN =
            "usuario.token";

    private final AuthSessionService sessionService;

    public AuthGuard(AuthSessionService sessionService) {

        if (sessionService == null) {
            throw new IllegalArgumentException(
                    "AuthSessionService no puede ser null."
            );
        }

        this.sessionService = sessionService;
    }

    /**
     * NUEVA PROTECCIÓN RBAC.
     *
     * Protege una ruta HTTP y comprueba el permiso
     * solicitado mediante PoliticaAutorizacion.
     */
    public HttpHandler proteger(
            String rutaEsperada,
            String metodoEsperado,
            Permiso permisoRequerido,
            HttpHandler siguiente
    ) {

        Objects.requireNonNull(
                permisoRequerido,
                "El permiso requerido no puede ser null."
        );

        return protegerInterno(
                rutaEsperada,
                metodoEsperado,
                sesion -> PoliticaAutorizacion.tienePermiso(
                        sesion.rol(),
                        permisoRequerido
                ),
                siguiente
        );
    }

    /**
     * COMPATIBILIDAD TEMPORAL.
     *
     * Conserva el mecanismo anterior basado en roles.
     * Se eliminará cuando todas las rutas migren a RBAC.
     */
    @Deprecated
    public HttpHandler proteger(
            String rutaEsperada,
            String metodoEsperado,
            Set<String> rolesPermitidos,
            HttpHandler siguiente
    ) {

        return protegerInterno(
                rutaEsperada,
                metodoEsperado,
                sesion -> rolesPermitidos == null
                        || rolesPermitidos.isEmpty()
                        || rolesPermitidos.contains(sesion.rol()),
                siguiente
        );
    }

    /**
     * Implementación central de autenticación y autorización.
     */
    private HttpHandler protegerInterno(
            String rutaEsperada,
            String metodoEsperado,
            Predicate<SesionDAO.SesionActiva> autorizacion,
            HttpHandler siguiente
    ) {

        Objects.requireNonNull(rutaEsperada);
        Objects.requireNonNull(metodoEsperado);
        Objects.requireNonNull(autorizacion);
        Objects.requireNonNull(siguiente);

        return exchange -> {

            // 1. Validar ruta exacta.
            String rutaActual =
                    exchange.getRequestURI().getPath();

            if (!rutaEsperada.equals(rutaActual)) {
                responder(
                        exchange,
                        404,
                        "{\"error\":\"Ruta no encontrada\"}"
                );
                return;
            }

            // 2. Validar método HTTP.
            String metodoActual =
                    exchange.getRequestMethod();

            if (!metodoEsperado.equalsIgnoreCase(metodoActual)) {

                exchange.getResponseHeaders().set(
                        "Allow",
                        metodoEsperado.toUpperCase()
                );

                responder(
                        exchange,
                        405,
                        "{\"error\":\"Método HTTP no permitido\"}"
                );
                return;
            }

            // 3. Obtener token Bearer.
            String token = obtenerToken(exchange);

            if (token == null) {
                responder(
                        exchange,
                        401,
                        "{\"error\":\"Token de autenticación requerido\"}"
                );
                return;
            }

            try {

                // 4. Validar sesión contra SQL Server.
                SesionDAO.SesionActiva sesion =
                        sessionService.obtenerSesion(token);

                if (sesion == null) {
                    responder(
                            exchange,
                            401,
                            "{\"error\":\"Sesión inválida o expirada\"}"
                    );
                    return;
                }

                // 5. Comprobar autorización.
                if (!autorizacion.test(sesion)) {
                    responder(
                            exchange,
                            403,
                            "{\"error\":\"No tienes permisos para realizar esta operación\"}"
                    );
                    return;
                }

                // 6. Compartir contexto autenticado.
                exchange.setAttribute(
                        ATRIBUTO_AUTH,
                        sesion
                );

                exchange.setAttribute(
                        ATRIBUTO_TOKEN,
                        token
                );

                // 7. Continuar con el controlador.
                siguiente.handle(exchange);

            } catch (SQLException e) {

                responder(
                        exchange,
                        500,
                        "{\"error\":\"Error al validar la sesión\"}"
                );

                e.printStackTrace();
            }
        };
    }

    /**
     * Obtiene el usuario autenticado desde HttpExchange.
     */
    public static SesionDAO.SesionActiva obtenerUsuario(
            HttpExchange exchange
    ) {

        Object usuario =
                exchange.getAttribute(ATRIBUTO_AUTH);

        if (usuario instanceof SesionDAO.SesionActiva sesion) {
            return sesion;
        }

        return null;
    }

    /**
     * Obtiene el token asociado a la solicitud autenticada.
     */
    public static String obtenerTokenAutenticado(
            HttpExchange exchange
    ) {

        Object token =
                exchange.getAttribute(ATRIBUTO_TOKEN);

        if (token instanceof String valor) {
            return valor;
        }

        return null;
    }

    /**
     * Extrae el token del encabezado Authorization.
     */
    private String obtenerToken(HttpExchange exchange) {

        String authorization = exchange.getRequestHeaders()
                .getFirst("Authorization");

        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        String prefijo = "Bearer ";

        if (!authorization.regionMatches(
                true, 0, prefijo, 0, prefijo.length())) {
            return null;
        }

        String token =
                authorization.substring(prefijo.length()).trim();

        return token.isEmpty() ? null : token;
    }

    /**
     * Envía una respuesta JSON.
     */
    private void responder(
            HttpExchange exchange,
            int codigo,
            String respuesta
    ) throws IOException {

        byte[] cuerpo =
                respuesta.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "Cache-Control",
                "no-store"
        );

        exchange.sendResponseHeaders(codigo, cuerpo.length);

        try (var output = exchange.getResponseBody()) {
            output.write(cuerpo);
        }
    }
}
