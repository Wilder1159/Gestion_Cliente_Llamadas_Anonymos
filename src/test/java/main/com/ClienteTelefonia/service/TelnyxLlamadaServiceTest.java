package main.com.ClienteTelefonia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TelnyxLlamadaServiceTest {

    @Test
    void admitePrefijoVersionadoEnCallControlId() {
        assertEquals(
                "v3%3Aabc-123",
                TelnyxLlamadaService.codificarCallControlId("v3:abc-123")
        );
    }

    @Test
    void rechazaSeparadoresDeRutaEnCallControlId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TelnyxLlamadaService.codificarCallControlId("a/b")
        );
    }

    @Test
    void codificaClientStateComoBase64EstandarConPadding() {
        assertEquals(
                "MQ==",
                TelnyxLlamadaService.codificarClientState(1)
        );
    }

    @Test
    void extraeCodigoYDetalleDeErrorTelnyx() {
        String detalle = TelnyxLlamadaService.detalleRespuestaError("""
                {
                  "errors": [{
                    "code": "10015",
                    "title": "Unprocessable Entity",
                    "detail": "The caller ID is not authorized.",
                    "source": {"pointer": "/from"}
                  }]
                }
                """);

        assertEquals(
                "code=10015, title=Unprocessable Entity, "
                + "detail=The caller ID is not authorized., "
                + "source=/from",
                detalle
        );
    }

    @Test
    void conservaRespuestaCuandoTelnyxNoDevuelveErrorsEstructurados() {
        assertEquals(
                "Respuesta no JSON: temporalmente no disponible",
                TelnyxLlamadaService.detalleRespuestaError(
                        "temporalmente no disponible"
                )
        );
    }
}
