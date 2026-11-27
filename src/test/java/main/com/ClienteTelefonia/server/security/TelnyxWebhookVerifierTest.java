package main.com.ClienteTelefonia.server.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelnyxWebhookVerifierTest {

    private static final Instant NOW =
            Instant.parse("2026-11-25T18:00:00Z");

    private KeyPair keys;
    private TelnyxWebhookVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        keys = generator.generateKeyPair();
        verifier = new TelnyxWebhookVerifier(
                Base64.getEncoder().encodeToString(
                        keys.getPublic().getEncoded()
                ),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void aceptaFirmaEd25519ValidaSobreTimestampYCuerpoOriginal()
            throws Exception {
        byte[] body = "{\"data\":{\"event_type\":\"call.answered\"}}"
                .getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());
        String signature = firmar(timestamp, body);

        assertTrue(verifier.verificar(body, signature, timestamp));
    }

    @Test
    void rechazaCuerpoModificadoTimestampAntiguoYFirmaInvalida()
            throws Exception {
        byte[] body = "{\"event\":\"call.hangup\"}"
                .getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());
        String signature = firmar(timestamp, body);

        assertFalse(verifier.verificar(
                "{\"event\":\"call.answered\"}"
                        .getBytes(StandardCharsets.UTF_8),
                signature,
                timestamp
        ));
        assertFalse(verifier.verificar(
                body,
                signature,
                Long.toString(NOW.minusSeconds(301).getEpochSecond())
        ));
        assertFalse(verifier.verificar(body, "invalid", timestamp));
    }

    private String firmar(String timestamp, byte[] body) throws Exception {
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keys.getPrivate());
        signer.update(timestamp.getBytes(StandardCharsets.UTF_8));
        signer.update((byte) '|');
        signer.update(body);
        return Base64.getEncoder().encodeToString(signer.sign());
    }
}
