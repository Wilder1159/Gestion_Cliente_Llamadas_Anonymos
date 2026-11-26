
package main.com.ClienteTelefonia.server.security;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaAutorizacionTest {

    /**
     * ADMIN debe tener todos los permisos definidos
     * en el enum Permiso.
     */
    @Test
    void adminDebeTenerTodosLosPermisos() {

        for (Permiso permiso : Permiso.values()) {

            assertTrue(
                PoliticaAutorizacion.tienePermiso(Rol.ADMIN, permiso),
                "ADMIN debería tener el permiso: " + permiso
            );
        }
    }

    /**
     * CLIENTE solamente debe tener los permisos
     * relacionados con su propia cuenta.
     */
    @Test
    void clienteDebeTenerSoloPermisosPropios() {

        Set<Permiso> permisosEsperados = EnumSet.of(
            Permiso.SESION_VER_PROPIA,
            Permiso.SESION_CERRAR_PROPIA,
            Permiso.CLIENTE_VER_PROPIO,
            Permiso.CLIENTE_ACTUALIZAR_PROPIO,
            Permiso.SALDO_VER_PROPIO,
            Permiso.RECARGA_CREAR_PROPIA,
            Permiso.RECARGA_VER_HISTORIAL_PROPIO,
            Permiso.LLAMADA_INICIAR_PROPIA,
            Permiso.LLAMADA_VER_REGISTROS_PROPIOS
        );

        Set<Permiso> permisosActuales = EnumSet.noneOf(Permiso.class);

        for (Permiso permiso : Permiso.values()) {

            if (PoliticaAutorizacion.tienePermiso(Rol.CLIENTE, permiso)) {
                permisosActuales.add(permiso);
            }
        }

        assertEquals(
            permisosEsperados,
            permisosActuales,
            "CLIENTE tiene permisos incorrectos o permisos adicionales."
        );
    }

    /**
     * CLIENTE no debe tener permisos administrativos
     * ni permisos de supervisión global.
     */
    @Test
    void clienteNoDebeTenerPermisosGlobales() {

        Permiso[] permisosRestringidos = {
            Permiso.CLIENTE_LISTAR_TODOS,
            Permiso.CLIENTE_CREAR,
            Permiso.CLIENTE_ACTUALIZAR_TODOS,
            Permiso.CLIENTE_DESACTIVAR,
            Permiso.SALDO_VER_TODOS,
            Permiso.RECARGA_VER_HISTORIAL_TODOS,
            Permiso.LLAMADA_VER_TODAS,
            Permiso.REPORTE_VER_GENERAL
        };

        for (Permiso permiso : permisosRestringidos) {

            assertFalse(
                PoliticaAutorizacion.tienePermiso(Rol.CLIENTE, permiso),
                "CLIENTE no debería tener el permiso: " + permiso
            );
        }
    }

    /**
     * Comprueba la consulta de permisos mediante String.
     */
    @Test
    void debeReconocerRolesMedianteString() {

        for (Permiso permiso : Permiso.values()) {

            assertEquals(
                PoliticaAutorizacion.tienePermiso(Rol.ADMIN, permiso),
                PoliticaAutorizacion.tienePermiso("ADMIN", permiso)
            );

            assertEquals(
                PoliticaAutorizacion.tienePermiso(Rol.CLIENTE, permiso),
                PoliticaAutorizacion.tienePermiso("CLIENTE", permiso)
            );
        }
    }

    /**
     * Un rol nulo o desconocido nunca debe obtener permisos.
     */
    @Test
    void rolesInvalidosNoDebenObtenerPermisos() {

        for (Permiso permiso : Permiso.values()) {

            assertFalse(
                PoliticaAutorizacion.tienePermiso((Rol) null, permiso)
            );

            assertFalse(
                PoliticaAutorizacion.tienePermiso("DESCONOCIDO", permiso)
            );

            assertFalse(
                PoliticaAutorizacion.tienePermiso("", permiso)
            );

            assertFalse(
                PoliticaAutorizacion.tienePermiso((String) null, permiso)
            );
        }
    }
}
