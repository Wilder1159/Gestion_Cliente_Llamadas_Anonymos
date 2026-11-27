package main.com.ClienteTelefonia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CalculoRecargaSimuladaTest {

    @Test
    void yapeAcreditaElImporteCompleto() {
        var importes = CalculoRecargaSimulada.yape(
                new BigDecimal("10.00")
        );

        assertEquals(new BigDecimal("10.00"), importes.montoBruto());
        assertEquals(new BigDecimal("0.00"), importes.comision());
        assertEquals(new BigDecimal("10.00"), importes.montoAcreditado());
    }

    @Test
    void tarjetaCalculaComisionDelDosPorCiento() {
        var importes = CalculoRecargaSimulada.tarjeta(
                new BigDecimal("10.00")
        );

        assertEquals(new BigDecimal("10.00"), importes.montoBruto());
        assertEquals(new BigDecimal("0.20"), importes.comision());
        assertEquals(new BigDecimal("9.80"), importes.montoAcreditado());
    }

    @Test
    void redondeaLaComisionConHalfUp() {
        var importes = CalculoRecargaSimulada.tarjeta(
                new BigDecimal("0.25")
        );

        assertEquals(new BigDecimal("0.01"), importes.comision());
        assertEquals(new BigDecimal("0.24"), importes.montoAcreditado());
    }

    @Test
    void rechazaImportesNoPositivosYMontosQueRedondeanACero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CalculoRecargaSimulada.yape(new BigDecimal("0"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> CalculoRecargaSimulada.tarjeta(new BigDecimal("-1"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> CalculoRecargaSimulada.yape(new BigDecimal("0.004"))
        );
    }
}
