
package main.com.ClienteTelefonia.server.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Define los permisos asignados a cada rol del sistema.
 *
 * Esta clase centraliza las reglas RBAC.(Role-Based Access Control o Control de Acceso Basado en Roles)
 */
public final class PoliticaAutorizacion {

    private static final Map<Rol, Set<Permiso>> PERMISOS_POR_ROL;

    static {

        EnumMap<Rol, Set<Permiso>> permisos =
                new EnumMap<>(Rol.class);

        // ==================================
        // PERMISOS DEL ADMINISTRADOR
        permisos.put(Rol.ADMIN, Set.of(

            // Sesiones propias
            Permiso.SESION_VER_PROPIA,
            Permiso.SESION_CERRAR_PROPIA,

            // Clientes
            Permiso.CLIENTE_VER_PROPIO,
            Permiso.CLIENTE_LISTAR_TODOS,
            Permiso.CLIENTE_CREAR,
            Permiso.CLIENTE_ACTUALIZAR_PROPIO,
            Permiso.CLIENTE_ACTUALIZAR_TODOS,
            Permiso.CLIENTE_DESACTIVAR,

            // Saldos
            Permiso.SALDO_VER_PROPIO,
            Permiso.SALDO_VER_TODOS,

            // Recargas
            Permiso.RECARGA_CREAR_PROPIA,
            Permiso.RECARGA_VER_HISTORIAL_PROPIO,
            Permiso.RECARGA_VER_HISTORIAL_TODOS,

            // Llamadas
            Permiso.LLAMADA_INICIAR_PROPIA,
            Permiso.LLAMADA_VER_REGISTROS_PROPIOS,
            Permiso.LLAMADA_VER_TODAS,

            // Reportes
            Permiso.REPORTE_VER_GENERAL
        ));

        // ==================================
        // PERMISOS DEL CLIENTE
        permisos.put(Rol.CLIENTE, Set.of(

            // Sesiones propias
            Permiso.SESION_VER_PROPIA,
            Permiso.SESION_CERRAR_PROPIA,

            // Perfil propio
            Permiso.CLIENTE_VER_PROPIO,
            Permiso.CLIENTE_ACTUALIZAR_PROPIO,

            // Saldo propio
            Permiso.SALDO_VER_PROPIO,

            // Recargas propias
            Permiso.RECARGA_CREAR_PROPIA,
            Permiso.RECARGA_VER_HISTORIAL_PROPIO,

            // Llamadas propias
            Permiso.LLAMADA_INICIAR_PROPIA,
            Permiso.LLAMADA_VER_REGISTROS_PROPIOS
        ));

        PERMISOS_POR_ROL = Collections.unmodifiableMap(permisos);
    }


    //Constructor privado para impedir instancias.
    private PoliticaAutorizacion() {
        throw new UnsupportedOperationException(
            "Esta clase no puede instanciarse."
        );
    }

    /**
     * Comprueba si un rol posee un permiso.
     *
     * @param rol rol recibido desde la sesión autenticada
     * @param permiso operación que se desea autorizar
     * @return true si tiene autorización
     */
    public static boolean tienePermiso(
            Rol rol,
            Permiso permiso) {

        if (rol == null || permiso == null) {
            return false;
        }

        Set<Permiso> permisos =
                PERMISOS_POR_ROL.get(rol);

        return permisos != null && permisos.contains(permiso);
    }

    /**
     * Comprueba permisos usando el rol almacenado
     * como texto en SQL Server.
     *
     * @param rolBD rol recibido desde la base de datos
     * @param permiso permiso solicitado
     * @return true si está autorizado
     */
    public static boolean tienePermiso(
            String rolBD,
            Permiso permiso) {

        try {
            Rol rol = Rol.desde(rolBD);

            return tienePermiso(rol, permiso);

        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
