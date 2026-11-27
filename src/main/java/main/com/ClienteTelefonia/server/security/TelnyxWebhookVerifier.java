package main.com.ClienteTelefonia.server.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public final class TelnyxWebhookVerifier {

    private static final byte[] ED25519_X509_PREFIX = new byte[] {
        0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65,
        0x70, 0x03, 0x21, 0x00
    };

    private final PublicKey publicKey;
    private final Clock clock;

    public TelnyxWebhookVerifier(String configuredPublicKey) {
        this(configuredPublicKey, Clock.systemUTC());
    }

    TelnyxWebhookVerifier(String configuredPublicKey, Clock clock) {
        if (configuredPublicKey == null || configuredPublicKey.isBlank()) {
            throw new IllegalArgumentException(
                    "TELNYX_PUBLIC_KEY no está configurada."
            );
        }
        try {
            this.publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(
                            decodePublicKey(configuredPublicKey)
                    ));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "La clave pública Ed25519 de Telnyx es inválida.",
                    e
            );
        }
        this.clock = clock;
    }

    public boolean verificar(
            byte[] body,
            String signatureHeader,
            String timestampHeader
    ) {
        if (body == null
                || signatureHeader == null
                || timestampHeader == null) {
            return false;
        }

        final long timestamp;
        final byte[] signatureBytes;
        try {
            timestamp = Long.parseLong(timestampHeader);
            signatureBytes = Base64.getDecoder().decode(signatureHeader);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (signatureBytes.length != 64) {
            return false;
        }
        Instant signedAt;
        try {
            signedAt = Instant.ofEpochSecond(timestamp);
        } catch (RuntimeException e) {
            return false;
        }
        if (Duration.between(signedAt, clock.instant()).abs()
                .compareTo(Duration.ofMinutes(5)) > 0) {
            return false;
        }

        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(timestampHeader.getBytes(StandardCharsets.UTF_8));
            verifier.update((byte) '|');
            verifier.update(body);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo verificar la firma Ed25519 de Telnyx.",
                    e
            );
        }
    }

    private byte[] decodePublicKey(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("-----BEGIN PUBLIC KEY-----")) {
            normalized = normalized
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
        }

        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "TELNYX_PUBLIC_KEY debe estar codificada en Base64.",
                    e
            );
        }

        if (decoded.length == 32) {
            byte[] encoded = new byte[
                    ED25519_X509_PREFIX.length + decoded.length
            ];
            System.arraycopy(
                    ED25519_X509_PREFIX, 0,
                    encoded, 0,
                    ED25519_X509_PREFIX.length
            );
            System.arraycopy(
                    decoded, 0,
                    encoded, ED25519_X509_PREFIX.length,
                    decoded.length
            );
            return encoded;
        }
        if (decoded.length >= 44 && decoded[0] == 0x30) {
            return decoded;
        }
        throw new IllegalArgumentException(
                "La clave pública Ed25519 de Telnyx tiene un formato inválido."
        );
    }
}
