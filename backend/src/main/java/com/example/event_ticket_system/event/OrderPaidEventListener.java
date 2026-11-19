package com.example.event_ticket_system.event;

import com.example.event_ticket_system.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidEventListener {

    private final EmailService emailService;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderPaidEvent event) {
        try {
            emailService.sendOrderConfirmationEmail(
                    event.recipientEmail(),
                    "Mã QR Vé Sự Kiện Của Bạn",
                    "Cảm ơn bạn đã mua vé! Vui lòng xuất trình các mã QR này tại lối vào sự kiện.",
                    event.totalAmount(),
                    event.items()
            );
            log.info("Dispatched QR confirmation email for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to send QR confirmation email for order {}: {}", event.orderId(), e.getMessage(), e);
        }
    }
}
