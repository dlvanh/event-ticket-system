package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.Entity.OrderTicket;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi email xác thực với nội dung xác thực được truyền vào.
     */
    public void sendVerificationEmail(String toUserEmail, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("dlvietanh2k4@gmail.com");
        message.setTo(toUserEmail);
        message.setSubject("Mã xác thực Email");
        message.setText("Mã xác thực email của bạn là: " + verificationCode + "\nMã này có hiệu lực trong 5 phút.");
        mailSender.send(message);
    }

    /**
     * Gửi email với nhiều QR codes (một cho mỗi loại vé)
     */
    public void sendMultipleQRCodeEmail(String to, String subject, String text,
                                        List<byte[]> qrCodes, List<OrderTicket> orderTickets) throws MessagingException {

        if (orderTickets == null || orderTickets.isEmpty()) {
            throw new IllegalArgumentException("OrderTickets list cannot be null or empty");
        }

        if (qrCodes == null || qrCodes.size() != orderTickets.size()) {
            throw new IllegalArgumentException("QR codes count must match OrderTickets count");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("dlvietanh2k4@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);

        // Tạo nội dung email chi tiết và đẹp hơn
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("<html><body>");
        emailContent.append("<h2>🎫 Vé Sự Kiện Của Bạn</h2>");
        emailContent.append("<p>").append(text).append("</p>");
        emailContent.append("<br>");
        emailContent.append("<h3>📋 Chi tiết vé:</h3>");
        emailContent.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        emailContent.append("<tr style='background-color: #f2f2f2;'>");
        emailContent.append("<th style='padding: 8px;'>Sự kiện</th>");
        emailContent.append("<th style='padding: 8px;'>Loại vé</th>");
        emailContent.append("<th style='padding: 8px;'>Số lượng</th>");
        emailContent.append("<th style='padding: 8px;'>Giá (VND)</th>");
        emailContent.append("</tr>");

        double totalAmount = 0;
        for (OrderTicket orderTicket : orderTickets) {
            double itemTotal = orderTicket.getUnitPrice() * orderTicket.getQuantity();
            totalAmount += itemTotal;

            emailContent.append("<tr>");
            emailContent.append("<td style='padding: 8px;'>").append(orderTicket.getTicket().getEvent().getEventName()).append("</td>");
            emailContent.append("<td style='padding: 8px;'>").append(orderTicket.getTicket().getTicketType()).append("</td>");
            emailContent.append("<td style='padding: 8px;'>").append(orderTicket.getQuantity()).append("</td>");
            emailContent.append("<td style='padding: 8px;'>").append(String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,d", (long) itemTotal)).append("</td>");
            emailContent.append("</tr>");
        }

        emailContent.append("<tr style='background-color: #f9f9f9; font-weight: bold;'>");
        emailContent.append("<td colspan='3' style='padding: 8px;'>Tổng cộng:</td>");
        emailContent.append("<td style='padding: 8px;'>").append(String.format("%.0f", totalAmount)).append(" VND</td>");
        emailContent.append("</tr>");
        emailContent.append("</table>");
        emailContent.append("<br>");
        emailContent.append("<p><strong>🔍 Lưu ý:</strong> Mỗi loại vé có một mã QR riêng được đính kèm. Vui lòng tải xuống và lưu trữ cẩn thận.</p>");
        emailContent.append("<p><em>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</em></p>");
        emailContent.append("</body></html>");

        helper.setText(emailContent.toString(), true); // true để enable HTML

        // Attach từng QR code với tên riêng biệt và an toàn
        for (int i = 0; i < qrCodes.size(); i++) {
            OrderTicket orderTicket = orderTickets.get(i);
            byte[] qrCode = qrCodes.get(i);

            if (qrCode != null && qrCode.length > 0) {
                // Tạo tên file an toàn
                String ticketType = orderTicket.getTicket().getTicketType()
                        .replaceAll("[^a-zA-Z0-9._-]", "_") // Loại bỏ ký tự đặc biệt
                        .replaceAll("_{2,}", "_"); // Thay nhiều underscore thành một

                String fileName = String.format("QR_%d_%s.png",
                        orderTicket.getOrderTicketId(),
                        ticketType);

                helper.addAttachment(fileName, new ByteArrayResource(qrCode));
                System.out.println("Attached QR code: " + fileName + " (" + qrCode.length + " bytes)");
            }
        }

        System.out.println("Sending email to: " + to + " with " + qrCodes.size() + " QR codes");
        mailSender.send(message);
        System.out.println("Email sent successfully!");
    }
}