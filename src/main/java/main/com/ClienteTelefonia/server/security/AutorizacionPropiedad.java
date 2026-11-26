
package main.com.ClienteTelefonia.server.security;

import java.util.Objects;

import main.com.ClienteTelefonia.dao.SesionDAO;

/**
 * Centraliza las comprobaciones de propiedad de los recursos.
 *
 * RBAC determina qué operaciones puede realizar un rol.
 * Esta clase determina sobre qué cliente puede realizarlas.
 */
public final class AutorizacionPropiedad {

    private AutorizacionPropiedad() {
        throw new UnsupportedOperationException(
                "Esta clase no puede instanciarse."
        );
    }

    /**
     * Comprueba si el cliente solicitado pertenece
     * al usuario autenticado.
     *
     * ADMIN también puede ser propietario de su propia cuenta.
     */
    public static boolean esPropietario(
            SesionDAO.SesionActiva sesion,
            long idClienteSolicitado
    ) {

        if (sesion == null || idClienteSolicitado <= 0) {
            return false;
        }

        return sesion.idCliente() == idClienteSolicitado;
    }

    /**
     * Determina si el usuario puede acceder a un cliente.
     *
     * ADMIN puede acceder a cualquier cliente.
     * CLIENTE solamente puede acceder a su propio cliente.
     *
     * Esta comprobación no sustituye la validación RBAC.
     */
    public static boolean puedeAccederCliente(
            SesionDAO.SesionActiva sesion,
            long idClienteSolicitado
    ) {

        if (sesion == null || idClienteSolicitado <= 0) {
            return false;
        }

        Rol rol;

        try {
            rol = Rol.desde(sesion.rol());
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (rol == Rol.ADMIN) {
            return true;
        }

        if (rol == Rol.CLIENTE) {
            return esPropietario(sesion, idClienteSolicitado);
        }

        return false;
    }

    /**
     * Comprueba que exista una sesión válida.
     */
    public static boolean tieneIdentidad(
            SesionDAO.SesionActiva sesion
    ) {

        return sesion != null
                && sesion.idUsuario() > 0
                && sesion.idCliente() > 0
                && sesion.username() != null
                && !sesion.username().isBlank()
                && esRolValido(sesion.rol());
    }

    private static boolean esRolValido(String valor) {
        try {
            Rol.desde(valor);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
