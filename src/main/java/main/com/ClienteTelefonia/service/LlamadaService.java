package main.com.ClienteTelefonia.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LlamadaService {

    public static final int DURACION_SEGUNDOS = 180;
    public static final double TARIFA_MINUTO = 0.20;
    public double calcularCosto(int segundos) {
        return calcularCosto(
                BigDecimal.valueOf(TARIFA_MINUTO),
                segundos,
                60,
                DURACION_SEGUNDOS
        ).doubleValue();
    }

    public double CostoLlamadaCompleta() {
        return calcularCosto(DURACION_SEGUNDOS);
    }

    public BigDecimal calcularCosto(
            BigDecimal precioMinuto,
            int segundos,
            int segundosMinimos,
            int duracionMaxima
    ) {
        validarTarifa(precioMinuto, segundosMinimos, duracionMaxima);
        if (segundos < 0) {
            throw new IllegalArgumentException(
                    "Duración de llamada inválida."
            );
        }
        if (segundos == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        int segundosFacturables = Math.min(
                Math.max(segundos, segundosMinimos),
                duracionMaxima
        );
        long minutosFacturables =
                (segundosFacturables + 59L) / 60L;

        return precioMinuto.multiply(BigDecimal.valueOf(minutosFacturables))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularMontoCobrado(
            BigDecimal saldoDisponible,
            BigDecimal costo
    ) {
        if (saldoDisponible == null || saldoDisponible.signum() < 0
                || costo == null || costo.signum() < 0) {
            throw new IllegalArgumentException(
                    "El saldo y el costo deben ser válidos."
            );
        }
        return saldoDisponible.min(costo);
    }

    private void validarTarifa(
            BigDecimal precioMinuto,
            int segundosMinimos,
            int duracionMaxima
    ) {
        if (precioMinuto == null || precioMinuto.signum() < 0) {
            throw new IllegalArgumentException(
                    "La tarifa no puede ser negativa."
            );
        }
        if (segundosMinimos <= 0
                || duracionMaxima <= 0
                || duracionMaxima < segundosMinimos) {
            throw new IllegalArgumentException(
                    "La duración máxima debe cubrir el mínimo facturable."
            );
        }
    }
}
