package com.example.event_ticket_system.service;

import com.example.event_ticket_system.entity.OrderTicket;
import com.example.event_ticket_system.event.OrderPaidEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:noreply@eventa.local}}")
    private String fromAddress;

    public void sendVerificationEmail(String toUserEmail, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toUserEmail);
        message.setSubject("Mã xác thực Email");
        message.setText("Mã xác thực email của bạn là: " + verificationCode + "\nMã này có hiệu lực trong 5 phút.");
        mailSender.send(message);
    }

    public void sendOrderConfirmationEmail(String to,
                                           String subject,
                                           String introText,
                                           List<OrderPaidEvent.TicketEmailItem> items) throws MessagingException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Ticket items cannot be null or empty");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);

        Locale viLocale = Locale.forLanguageTag("vi-VN");
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("<html><body>");
        emailContent.append("<h2>Vé Sự Kiện Của Bạn</h2>");
        emailContent.append("<p>").append(introText).append("</p><br>");
        emailContent.append("<h3>Chi tiết vé:</h3>");
        emailContent.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        emailContent.append("<tr style='background-color: #f2f2f2;'>");
        emailContent.append("<th style='padding: 8px;'>Sự kiện</th>");
        emailContent.append("<th style='padding: 8px;'>Loại vé</th>");
        emailContent.append("<th style='padding: 8px;'>Số lượng</th>");
        emailContent.append("<th style='padding: 8px;'>Giá (VND)</th>");
        emailContent.append("</tr>");

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderPaidEvent.TicketEmailItem item : items) {
            BigDecimal itemTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            totalAmount = totalAmount.add(itemTotal);

            emailContent.append("<tr>");
            emailContent.append("<td style='padding: 8px;'>").append(item.eventName()).append("</td>");
            emailContent.append("<td style='padding: 8px;'>").append(item.ticketType()).append("</td>");
            emailContent.append("<td style='padding: 8px;'>").append(item.quantity()).append("</td>");
            emailContent.append("<td style='padding: 8px;'>")
                    .append(String.format(viLocale, "%,d", itemTotal.setScale(0, RoundingMode.HALF_UP).longValue()))
                    .append("</td>");
            emailContent.append("</tr>");
        }

        emailContent.append("<tr style='background-color: #f9f9f9; font-weight: bold;'>");
        emailContent.append("<td colspan='3' style='padding: 8px;'>Tổng cộng:</td>");
        emailContent.append("<td style='padding: 8px;'>")
                .append(String.format(viLocale, "%,d", totalAmount.setScale(0, RoundingMode.HALF_UP).longValue()))
                .append(" VND</td>");
        emailContent.append("</tr></table><br>");
        emailContent.append("<p><strong>Lưu ý:</strong> Mỗi loại vé có một mã QR riêng được đính kèm. Vui lòng tải xuống và lưu trữ cẩn thận.</p>");
        emailContent.append("<p><em>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</em></p>");
        emailContent.append("</body></html>");

        helper.setText(emailContent.toString(), true);

        for (OrderPaidEvent.TicketEmailItem item : items) {
            byte[] qrCode = item.qrCode();
            if (qrCode != null && qrCode.length > 0) {
                String safeType = item.ticketType()
                        .replaceAll("[^a-zA-Z0-9._-]", "_")
                        .replaceAll("_{2,}", "_");
                String fileName = String.format("QR_%d_%s.png", item.orderTicketId(), safeType);
                helper.addAttachment(fileName, new ByteArrayResource(qrCode));
            }
        }

        log.info("Sending ticket QR email to {} with {} attachment(s)", to, items.size());
        mailSender.send(message);
    }

    public void sendMultipleQRCodeEmail(String to, String subject, String text,
                                        List<byte[]> qrCodes, List<OrderTicket> orderTickets) throws MessagingException {
        if (orderTickets == null || orderTickets.isEmpty()) {
            throw new IllegalArgumentException("OrderTickets list cannot be null or empty");
        }
        if (qrCodes == null || qrCodes.size() != orderTickets.size()) {
            throw new IllegalArgumentException("QR codes count must match OrderTickets count");
        }
        List<OrderPaidEvent.TicketEmailItem> items = new java.util.ArrayList<>(orderTickets.size());
        for (int i = 0; i < orderTickets.size(); i++) {
            OrderTicket ot = orderTickets.get(i);
            items.add(new OrderPaidEvent.TicketEmailItem(
                    ot.getOrderTicketId(),
                    ot.getTicket().getEvent().getEventName(),
                    ot.getTicket().getTicketType(),
                    ot.getQuantity(),
                    ot.getUnitPrice(),
                    qrCodes.get(i)
            ));
        }
        sendOrderConfirmationEmail(to, subject, text, items);
    }
}