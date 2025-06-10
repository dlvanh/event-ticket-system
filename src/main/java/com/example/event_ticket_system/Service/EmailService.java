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
     * @param toUserEmail      địa chỉ email người nhận
     * @param verificationCode nội dung email (ví dụ: "Mã xác thực của bạn là: 123456")
     */
    public void sendVerificationEmail(String toUserEmail, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("dlvietanh2k4@gmail.com");
        message.setTo(toUserEmail);
        message.setSubject("Mã xác thực Email");
        message.setText("Mã xác thực email của bạn là: " + verificationCode + "\nMã này có hiệu lực trong 5 phút.");
        mailSender.send(message);
    }
}
