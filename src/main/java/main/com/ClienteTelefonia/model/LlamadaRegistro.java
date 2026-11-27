package main.com.ClienteTelefonia.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LlamadaRegistro(
        long idLlamada,
        String destino,
        String estado,
        String estadoSolicitudTelnyx,
        int duracionSegundos,
        BigDecimal costo,
        String moneda,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaContestacion,
        LocalDateTime fechaFinalizacion,
        String motivoFinalizacion
) {
}
