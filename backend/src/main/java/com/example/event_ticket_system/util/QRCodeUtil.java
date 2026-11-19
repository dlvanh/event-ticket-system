package com.example.event_ticket_system.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

@Component
public class QRCodeUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int DEFAULT_QR_SIZE = 200;

    private final byte[] signingSecretBytes;

    public QRCodeUtil(@Value("${app.qr.signing-secret}") String signingSecret) {
        if (signingSecret == null || signingSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("QR signing secret must be at least 32 bytes");
        }
        this.signingSecretBytes = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generates a tamper-resistant HMAC-SHA256 signed check-in token for a ticket line item.
     */
    public String generateSignedToken(Integer orderId,
                                      Integer orderTicketId,
                                      Integer ticketId,
                                      Integer userId,
                                      Integer eventId,
                                      int quantity) {
        String payload = String.format("v1:%d:%d:%d:%d:%d:%d",
                orderId, orderTicketId, ticketId, userId, eventId, quantity);
        String signature = computeHmac(payload);
        return payload + "." + signature;
    }

    /**
     * Verifies the HMAC-SHA256 signature of a check-in token using constant-time comparison.
     */
    public boolean verifySignedToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        int dotIndex = token.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex >= token.length() - 1) {
            return false;
        }
        String payload = token.substring(0, dotIndex);
        String providedSig = token.substring(dotIndex + 1);
        String expectedSig = computeHmac(payload);
        return MessageDigest.isEqual(
                providedSig.getBytes(StandardCharsets.UTF_8),
                expectedSig.getBytes(StandardCharsets.UTF_8)
        );
    }

    public byte[] generateQRCodeImage(String content) throws WriterException, IOException {
        return renderQrPng(content, DEFAULT_QR_SIZE);
    }

    public static byte[] renderQrPng(String content, int size) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());

        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "PNG", baos)) {
            throw new IOException("Could not encode QR PNG image");
        }
        return baos.toByteArray();
    }

    private String computeHmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecretBytes, HMAC_ALGORITHM));
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign QR token", e);
        }
    }
}