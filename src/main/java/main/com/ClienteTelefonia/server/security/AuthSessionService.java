
package main.com.ClienteTelefonia.server.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;

import main.com.ClienteTelefonia.dao.SesionDAO;

public final class AuthSessionService {

    private static final long DURACION_SEGUNDOS = 3600;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SesionDAO sesionDAO;

    //constructor
    public AuthSessionService() {
        this.sesionDAO = new SesionDAO();
    }

    /**
     * Crea una sesión persistente y devuelve el token original.
     */
    public String crearSesion(long idUsuario) throws SQLException {

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        byte[] tokenHash = calcularHash(token);

        LocalDateTime fechaCreacion =
                LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime fechaExpiracion =
                fechaCreacion.plusSeconds(DURACION_SEGUNDOS);

        try {
            sesionDAO.registrar(
                    idUsuario,
                    tokenHash,
                    fechaCreacion,
                    fechaExpiracion
            );
        } finally {
            Arrays.fill(bytes, (byte) 0);
            Arrays.fill(tokenHash, (byte) 0);
        }

        return token;
    }

    /**
     * Comprueba si el token pertenece a una sesión vigente.
     */
    public boolean esValida(String token) throws SQLException {

        if (token == null || token.isBlank()) {
            return false;
        }

        byte[] tokenHash = calcularHash(token);

        try {
            return sesionDAO.buscarSesionValida(tokenHash) != null;
        } finally {
            Arrays.fill(tokenHash, (byte) 0);
        }
    }

    /**
     * Obtiene los datos del usuario asociado a la sesión.
     */
    public SesionDAO.SesionActiva obtenerSesion(
            String token
    ) throws SQLException {

        if (token == null || token.isBlank()) {
            return null;
        }

        byte[] tokenHash = calcularHash(token);

        try {
            return sesionDAO.buscarSesionValida(tokenHash);
        } finally {
            Arrays.fill(tokenHash, (byte) 0);
        }
    }

    /**
     * Revoca una sesión.
     */
    public void cerrarSesion(String token) throws SQLException {

        if (token == null || token.isBlank()) {
            return;
        }

        byte[] tokenHash = calcularHash(token);

        try {
            sesionDAO.revocar(tokenHash);
        } finally {
            Arrays.fill(tokenHash, (byte) 0);
        }
    }

    /**
     * Revoca todas las sesiones de un usuario.
     */
    public void cerrarTodasLasSesiones(
            long idUsuario
    ) throws SQLException {

        sesionDAO.revocarTodas(idUsuario);
    }

    /**
     * Calcula SHA-256 del token.
     */
    private byte[] calcularHash(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return digest.digest(
                    token.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 no está disponible en este entorno Java.",
                    e
            );
        }
    }
}
