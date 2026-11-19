package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.Order;
import com.example.event_ticket_system.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByPayosOrderCode(Long payosOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.discount WHERE o.payosOrderCode = :payosOrderCode")
    Optional<Order> findByPayosOrderCodeForUpdate(@Param("payosOrderCode") Long payosOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.discount WHERE o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Integer orderId);

    List<Order> findByStatusAndOrderDateBefore(OrderStatus orderStatus, LocalDateTime cutoff);

    List<Order> findByUserIdOrderByOrderDateDesc(Integer userId);
}
