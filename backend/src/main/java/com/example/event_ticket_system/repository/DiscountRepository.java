package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.Discount;
import com.example.event_ticket_system.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Integer> {
    Optional<Discount> findByCode(String code);

    @Query("SELECT d FROM Discount d JOIN FETCH d.event WHERE d.code = :code")
    Optional<Discount> findByCodeWithEvent(@Param("code") String code);

    List<Discount> findByEvent(Event event);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE Discount d SET d.maxUsage = d.maxUsage - 1 " +
           "WHERE d.discountId = :discountId AND d.maxUsage > 0")
    int decrementMaxUsage(@Param("discountId") Integer discountId);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE Discount d SET d.maxUsage = d.maxUsage + 1 " +
           "WHERE d.discountId = :discountId")
    int incrementMaxUsage(@Param("discountId") Integer discountId);
}
