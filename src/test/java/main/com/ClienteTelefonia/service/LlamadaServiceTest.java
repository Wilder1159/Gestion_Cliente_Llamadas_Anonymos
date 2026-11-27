package main.com.ClienteTelefonia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

class LlamadaServiceTest {

    private final LlamadaService service = new LlamadaService();
    private final BigDecimal tarifa = new BigDecimal("0.2000");

    @Test
    void cobraElMinimoYRedondeaCadaMinutoIniciado() {
        assertEquals(
                new BigDecimal("0.20"),
                service.calcularCosto(tarifa, 1, 60, 900)
        );
        assertEquals(
                new BigDecimal("0.20"),
                service.calcularCosto(tarifa, 60, 60, 900)
        );
        assertEquals(
                new BigDecimal("0.40"),
                service.calcularCosto(tarifa, 61, 60, 900)
        );
    }

    @Test
    void limitaAlMaximoConfiguradoYAplicaHalfUp() {
        BigDecimal precio = new BigDecimal("0.2050");
        BigDecimal costo = service.calcularCosto(precio, 1200, 60, 900);

        assertEquals(
                new BigDecimal("3.08"),
                costo.setScale(2, RoundingMode.HALF_UP)
        );
    }

    @Test
    void cobraElCostoRealHastaElSaldoDisponible() {
        assertEquals(
                new BigDecimal("0.70"),
                service.calcularMontoCobrado(
                        new BigDecimal("1.00"),
                        new BigDecimal("0.70")
                )
        );
        assertEquals(
                new BigDecimal("0.40"),
                service.calcularMontoCobrado(
                        new BigDecimal("0.40"),
                        new BigDecimal("0.70")
                )
        );
    }

    @Test
    void rechazaDuracionesYTarifasIncoherentes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcularCosto(tarifa, -1, 60, 900)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcularCosto(tarifa, 60, 61, 60)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcularCosto(
                        new BigDecimal("-0.01"), 60, 60, 900
                )
        );
    }
}
