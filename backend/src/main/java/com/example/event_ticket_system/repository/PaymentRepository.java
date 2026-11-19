package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.Payment;
import com.example.event_ticket_system.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    boolean existsByOrderIdAndPaymentStatus(Integer orderId, PaymentStatus status);
    Optional<Payment> findByOrderId(Integer orderId);
}
