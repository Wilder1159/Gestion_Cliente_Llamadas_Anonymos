
package main.com.ClienteTelefonia.server.security;

/**
 * Define los roles disponibles dentro del sistema.
 *
 * ADMIN: administrador del sistema.
 * CLIENTE: usuario que opera exclusivamente sobre su propia cuenta.
 */
public enum Rol {

    ADMIN,
    CLIENTE;

    /**
     * Convierte el rol almacenado en la base de datos
     * enum permite representar un conjunto limitado de valores válidos.
     *
     * @param valor rol recibido
     * @return rol correspondiente
     * @throws IllegalArgumentException si el rol no existe
     */
    public static Rol desde(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                "El rol no puede ser nulo ni estar vacío."
            );
        }

        try {
            return Rol.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Rol no reconocido: " + valor
            );
        }
    }
}
