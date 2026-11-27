package main.com.ClienteTelefonia.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ClienteTelefoniaControllerTest {

    @Test
    void clienteNoPuedeSeleccionarLaCuentaDeOtroCliente() {
        ClienteTelefoniaController controller =
                new ClienteTelefoniaController(15L, "CLIENTE");

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.seleccionarCliente(20L)
        );
    }
}
