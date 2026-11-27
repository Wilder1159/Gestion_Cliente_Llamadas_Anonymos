package main.com.ClienteTelefonia.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalculoRecargaSimulada {

    private static final BigDecimal COMISION_TARJETA =
            new BigDecimal("0.02");
    private static final BigDecimal MAXIMO_DECIMAL_12_2 =
            new BigDecimal("9999999999.99");

    private CalculoRecargaSimulada() {
    }

    public static Importes yape(BigDecimal monto) {
        return calcular(monto, false);
    }

    public static Importes tarjeta(BigDecimal monto) {
        return calcular(monto, true);
    }

    private static Importes calcular(
            BigDecimal monto,
            boolean tarjeta
    ) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor que cero."
            );
        }

        BigDecimal bruto = monto.setScale(2, RoundingMode.HALF_UP);

        if (bruto.signum() <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser al menos S/ 0.01."
            );
        }
        if (bruto.compareTo(MAXIMO_DECIMAL_12_2) > 0) {
            throw new IllegalArgumentException(
                    "El monto supera el máximo permitido."
            );
        }

        BigDecimal comision = tarjeta
                ? bruto.multiply(COMISION_TARJETA)
                        .setScale(2, RoundingMode.HALF_UP)
                : new BigDecimal("0.00");
        BigDecimal acreditado = bruto.subtract(comision)
                .setScale(2, RoundingMode.HALF_UP);

        return new Importes(bruto, comision, acreditado);
    }

    public record Importes(
            BigDecimal montoBruto,
            BigDecimal comision,
            BigDecimal montoAcreditado
    ) {
    }
}
