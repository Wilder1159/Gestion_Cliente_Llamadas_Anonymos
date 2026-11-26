package main.com.ClienteTelefonia.server.security;

import main.com.ClienteTelefonia.dao.SesionDAO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.*;

class AutorizacionPropiedadTest {

    // ADMIN: puede acceder a su cliente y a otros clientes.
    @Test
    void adminPuedeAccederASuPropioCliente() {
        var sesion = new SesionDAO.SesionActiva(
                1L, 10L, "admin", "ADMIN"
        );

        assertTrue(
                AutorizacionPropiedad.puedeAccederCliente(sesion, 10L)
        );
    }

    @Test
    void adminPuedeAccederAOtroCliente() {
        var sesion = new SesionDAO.SesionActiva(
                1L, 10L, "admin", "ADMIN"
        );

        assertTrue(
                AutorizacionPropiedad.puedeAccederCliente(sesion, 20L)
        );
    }

    // CLIENTE: solamente puede acceder a su propio cliente.
    @Test
    void clientePuedeAccederASuPropioCliente() {
        var sesion = new SesionDAO.SesionActiva(
                2L, 15L, "cliente01", "CLIENTE"
        );

        assertTrue(
                AutorizacionPropiedad.puedeAccederCliente(sesion, 15L)
        );
    }

    @Test
    void clienteNoPuedeAccederAOtroCliente() {
        var sesion = new SesionDAO.SesionActiva(
                2L, 15L, "cliente01", "CLIENTE"
        );

        assertFalse(
                AutorizacionPropiedad.puedeAccederCliente(sesion, 20L)
        );
    }

    // Validaciones de entradas.
    @Test
    void sesionNulaDebeDenegarAcceso() {
        assertFalse(
                AutorizacionPropiedad.puedeAccederCliente(null, 10L)
        );
    }

    @Test
    void idClienteCeroDebeDenegarAcceso() {
        var sesion = new SesionDAO.SesionActiva(
                2L, 15L, "cliente01", "CLIENTE"
        );

        assertFalse(
                AutorizacionPropiedad.puedeAccederCliente(sesion, 0L)
        );
    }

    @Test
    void idClienteNegativoDebeDenegarAcceso() {
        var sesion = new SesionDAO.SesionActiva(
                2L, 15L, "cliente01", "CLIENTE"
        );

        assertFalse(
                AutorizacionPropiedad.puedeAccederCliente(sesion, -1L)
        );
    }

    @Test
    void rolDesconocidoDebeDenegarAcceso() {
        var sesion = new SesionDAO.SesionActiva(
                2L, 15L, "usuario01", "SUPERVISOR"
        );

        assertFalse(
                AutorizacionPropiedad.puedeAccederCliente(sesion, 15L)
        );
    }

    // Comprobación de propiedad del recurso.
    @Test
    void esPropietarioDebeCompararIdDelCliente() {
        var sesion = new SesionDAO.SesionActiva(
                2L, 15L, "cliente01", "CLIENTE"
        );

        assertTrue(
                AutorizacionPropiedad.esPropietario(sesion, 15L)
        );

        assertFalse(
                AutorizacionPropiedad.esPropietario(sesion, 20L)
        );
    }

    @Test
    void adminTambienDebeComprobarPropiedadCuandoSeUsaEsteMetodo() {
        var sesion = new SesionDAO.SesionActiva(
                1L, 10L, "admin", "ADMIN"
        );

        assertTrue(
                AutorizacionPropiedad.esPropietario(sesion, 10L)
        );

        assertFalse(
                AutorizacionPropiedad.esPropietario(sesion, 20L)
        );
    }

    // Validación de identidad completa.
    @Test
    void identidadValidaDebeSerAceptada() {
        var sesion = new SesionDAO.SesionActiva(
                1L, 10L, "admin", "ADMIN"
        );

        assertTrue(
                AutorizacionPropiedad.tieneIdentidad(sesion)
        );
    }

    @Test
    void identidadNulaDebeSerRechazada() {
        assertFalse(
                AutorizacionPropiedad.tieneIdentidad(null)
        );
    }

    @Test
    void identidadConIdUsuarioInvalidoDebeSerRechazada() {
        var sesion = new SesionDAO.SesionActiva(
                0L, 10L, "admin", "ADMIN"
        );

        assertFalse(
                AutorizacionPropiedad.tieneIdentidad(sesion)
        );
    }

    @Test
    void identidadConIdClienteInvalidoDebeSerRechazada() {
        var sesion = new SesionDAO.SesionActiva(
                1L, 0L, "admin", "ADMIN"
        );

        assertFalse(
                AutorizacionPropiedad.tieneIdentidad(sesion)
        );
    }

    @Test
    void identidadConUsernameNuloDebeSerRechazada() {
        var sesion = new SesionDAO.SesionActiva(
                1L, 10L, null, "ADMIN"
        );

        assertFalse(
                AutorizacionPropiedad.tieneIdentidad(sesion)
        );
    }

    @Test
    void identidadConUsernameVacioDebeSerRechazada() {
        var sesion = new SesionDAO.SesionActiva(
                1L, 10L, "   ", "ADMIN"
        );

        assertFalse(
                AutorizacionPropiedad.tieneIdentidad(sesion)
        );
    }

    @Test
    void identidadConRolInvalidoDebeSerRechazada() {
        var sesion = new SesionDAO.SesionActiva(
                1L, 10L, "usuario01", "SUPERVISOR"
        );

        assertFalse(
                AutorizacionPropiedad.tieneIdentidad(sesion)
        );
    }
}
