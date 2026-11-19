package com.example.event_ticket_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import vn.payos.PayOS;

@SpringBootTest
@ActiveProfiles("test")
class EventTicketSystemApplicationTests {

    @MockitoBean
    private PayOS payOS;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }
}
