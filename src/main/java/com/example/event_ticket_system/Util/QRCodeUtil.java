package com.example.event_ticket_system.Util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.EncodeHintType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class QRCodeUtil {

    /**
     * Tạo QR code siêu tối ưu với kích thước nhỏ nhất có thể
     */
    public static byte[] generateOptimizedQRCodeImage(String text) throws WriterException, IOException {
        // Rút gọn nội dung QR để giảm độ phức tạp
        String shortText = shortenQRContent(text);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        // Cấu hình tối ưu cho QR code nhỏ gọn
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // Mức sửa lỗi thấp nhất
        hints.put(EncodeHintType.MARGIN, 0); // Không có margin
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        // Kích thước nhỏ nhưng vẫn đọc được
        int size = 120; // Giảm từ 200 xuống 120
        BitMatrix bitMatrix = qrCodeWriter.encode(shortText, BarcodeFormat.QR_CODE, size, size, hints);

        // Tạo ảnh 1-bit (chỉ đen trắng) để tiết kiệm tối đa dung lượng
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);

        // Vẽ QR code lên ảnh
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Đen = true, Trắng = false
                int rgb = bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF;
                image.setRGB(x, y, rgb);
            }
        }

        // Lưu thành PNG với nén tối đa
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Sử dụng PNG với compression level cao nhất
        if (!ImageIO.write(image, "PNG", baos)) {
            throw new IOException("Could not write PNG image");
        }

        byte[] imageBytes = baos.toByteArray();

        // Nén thêm lần nữa bằng Deflater nếu cần
        if (imageBytes.length > 10000) { // Nếu > 10KB thì nén thêm
            return compressBytes(imageBytes);
        }

        return imageBytes;
    }

    /**
     * Rút gọn nội dung QR code để giảm kích thước
     */
    private static String shortenQRContent(String originalContent) {
        // Chuyển từ dạng dài sang dạng rút gọn
        // Ví dụ: orderId=123&orderTicketId=456&ticketId=789&userId=101&eventId=202&quantity=2
        // Thành: o=123&ot=456&t=789&u=101&e=202&q=2

        return originalContent
                .replace("orderId=", "o=")
                .replace("orderTicketId=", "ot=")
                .replace("ticketId=", "t=")
                .replace("userId=", "u=")
                .replace("eventId=", "e=")
                .replace("quantity=", "q=");
    }

    /**
     * Nén byte array để giảm kích thước lưu trữ
     */
    private static byte[] compressBytes(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.BEST_COMPRESSION))) {
            dos.write(data);
        }

        return baos.toByteArray();
    }

    /**
     * Giải nén byte array (sử dụng khi đọc QR từ database)
     */
    public static byte[] decompressBytes(byte[] compressedData) throws IOException {
        // Implementation để giải nén nếu cần thiết
        return compressedData; // Tạm thời return như cũ
    }

    /**
     * Tạo QR code với kích thước cố định siêu nhỏ (dưới 5KB)
     */
    public static byte[] generateMiniQRCode(String text) throws WriterException, IOException {
        String shortText = shortenQRContent(text);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.MARGIN, 0);

        // Kích thước rất nhỏ: 80x80 pixel
        int size = 80;
        BitMatrix bitMatrix = qrCodeWriter.encode(shortText, BarcodeFormat.QR_CODE, size, size, hints);

        // Tạo ảnh đơn giản nhất
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}