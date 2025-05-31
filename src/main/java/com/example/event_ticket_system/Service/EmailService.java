package com.example.event_ticket_system.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi email xác thực với nội dung xác thực được truyền vào.
     *
     * @param to      địa chỉ email người nhận
     * @param content nội dung email (ví dụ: "Mã xác thực của bạn là: 123456")
     */
    public void sendVerificationEmail(String to, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("thuongmaidientu.022025@gmail.com");
        message.setTo(to);
        message.setSubject("Xác thực Email của bạn");
        message.setText(content);
        mailSender.send(message);
    }
}
