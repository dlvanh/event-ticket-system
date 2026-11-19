package com.example.event_ticket_system.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QRCodeUtilTest {

    private static final String VALID_SECRET = "test-qr-signing-secret-must-be-at-least-32-bytes-long";

    @Test
    void constructor_ShouldRejectShortSecret() {
        assertThatThrownBy(() -> new QRCodeUtil("too-short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void generateAndVerifySignedToken_ShouldRoundTripAndRejectTampering() {
        QRCodeUtil qrCodeUtil = new QRCodeUtil(VALID_SECRET);

        String token = qrCodeUtil.generateSignedToken(101, 202, 303, 404, 505, 2);

        assertThat(token).startsWith("v1:101:202:303:404:505:2.");
        assertThat(qrCodeUtil.verifySignedToken(token)).isTrue();

        // Tamper payload (change quantity from 2 to 9)
        String tamperedPayload = token.replace(":2.", ":9.");
        assertThat(qrCodeUtil.verifySignedToken(tamperedPayload)).isFalse();

        // Tamper 1 character in signature
        char lastChar = token.charAt(token.length() - 1);
        char replacement = lastChar == 'A' ? 'B' : 'A';
        String tamperedSignature = token.substring(0, token.length() - 1) + replacement;
        assertThat(qrCodeUtil.verifySignedToken(tamperedSignature)).isFalse();

        // Different secret rejects token
        QRCodeUtil otherSecretUtil = new QRCodeUtil("another-completely-different-secret-key-at-least-32-bytes");
        assertThat(otherSecretUtil.verifySignedToken(token)).isFalse();

        // Malformed inputs
        assertThat(qrCodeUtil.verifySignedToken(null)).isFalse();
        assertThat(qrCodeUtil.verifySignedToken("")).isFalse();
        assertThat(qrCodeUtil.verifySignedToken("nodot")).isFalse();
        assertThat(qrCodeUtil.verifySignedToken(".leadingdot")).isFalse();
        assertThat(qrCodeUtil.verifySignedToken("trailingdot.")).isFalse();
    }

    @Test
    void generateQRCodeImage_ShouldProduceValidPngBytes() throws Exception {
        QRCodeUtil qrCodeUtil = new QRCodeUtil(VALID_SECRET);
        String token = qrCodeUtil.generateSignedToken(1, 2, 3, 4, 5, 1);

        byte[] png = qrCodeUtil.generateQRCodeImage(token);

        assertThat(png).isNotNull();
        assertThat(png.length).isGreaterThan(100);
        // PNG magic header: 89 50 4E 47 0D 0A 1A 0A
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50);
        assertThat(png[2]).isEqualTo((byte) 0x4E);
        assertThat(png[3]).isEqualTo((byte) 0x47);
    }
}
