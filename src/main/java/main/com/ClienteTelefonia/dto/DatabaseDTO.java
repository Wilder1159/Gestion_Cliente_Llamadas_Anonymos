
package main.com.ClienteTelefonia.dto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseDTO {

    private DatabaseDTO() {
    }

    public static Connection getConnection() throws SQLException {

        try {
            String url = EnvDTO.obtener("DB_URL");
            String usuario = EnvDTO.obtener("DB_USER");
            String password = EnvDTO.obtener("DB_PASSWORD");

            if (url == null || url.isBlank()) {
                throw new SQLException(
                    "DB_URL está vacío en el archivo .env."
                );
            }

            return DriverManager.getConnection(url, usuario, password);

        } catch (IllegalStateException e) {
            throw new SQLException(
                "No se pudo leer la configuración de SQL Server. "
                + "Verifica DB_URL, DB_USER y DB_PASSWORD en .env.",
                e
            );
        }
    }
}
