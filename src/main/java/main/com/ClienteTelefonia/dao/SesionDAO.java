
package main.com.ClienteTelefonia.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import main.com.ClienteTelefonia.dto.DatabaseDTO;

public class SesionDAO {

    /**
     * Información de una sesión válida.
     *
     * idUsuario: identifica la cuenta de acceso.
     * idCliente: identifica al propietario de la cuenta telefónica.
     * username: nombre de usuario.
     * rol: ADMIN o CLIENTE.
     */
    public record SesionActiva(
            long idUsuario,
            long idCliente,
            String username,
            String rol
    ) {
    }

    /**
     * Registra una nueva sesión.
     * Las fechas recibidas deben estar expresadas en UTC.
     */
    public void registrar(
            long idUsuario,
            byte[] tokenHash,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaExpiracion
    ) throws SQLException {

        String sql = """
            INSERT INTO dbo.SesionUsuario (
                id_usuario,
                token_hash,
                fecha_creacion,
                fecha_expiracion,
                revocada
            )
            VALUES (?, ?, ?, ?, 0)
        """;

        try (Connection conexion = DatabaseDTO.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            ps.setBytes(2, tokenHash);
            ps.setTimestamp(3, Timestamp.valueOf(fechaCreacion));
            ps.setTimestamp(4, Timestamp.valueOf(fechaExpiracion));

            ps.executeUpdate();
        }
    }

    /**
     * Busca una sesión vigente y recupera la información
     * del usuario y del cliente asociado.
     */
    public SesionActiva buscarSesionValida(
            byte[] tokenHash
    ) throws SQLException {

        String sql = """
            SELECT
                s.id_usuario,
                u.id_cliente,
                u.username,
                u.rol
            FROM dbo.SesionUsuario s
            INNER JOIN dbo.Usuario u
                ON s.id_usuario = u.id_usuario
            WHERE s.token_hash = ?
              AND s.revocada = 0
              AND s.fecha_expiracion > SYSUTCDATETIME()
              AND u.activo = 1
              AND u.rol IN ('ADMIN', 'CLIENTE')
        """;

        try (Connection conexion = DatabaseDTO.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setBytes(1, tokenHash);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                return new SesionActiva(
                        rs.getLong("id_usuario"),
                        rs.getLong("id_cliente"),
                        rs.getString("username"),
                        rs.getString("rol")
                );
            }
        }
    }

    /**
     * Revoca una sesión mediante su hash.
     */
    public void revocar(byte[] tokenHash) throws SQLException {

        String sql = """
            UPDATE dbo.SesionUsuario
            SET revocada = 1
            WHERE token_hash = ?
              AND revocada = 0
        """;

        try (Connection conexion = DatabaseDTO.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setBytes(1, tokenHash);
            ps.executeUpdate();
        }
    }

    /**
     * Revoca todas las sesiones activas de un usuario.
     */
    public void revocarTodas(long idUsuario) throws SQLException {

        String sql = """
            UPDATE dbo.SesionUsuario
            SET revocada = 1
            WHERE id_usuario = ?
              AND revocada = 0
        """;

        try (Connection conexion = DatabaseDTO.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            ps.executeUpdate();
        }
    }
}
