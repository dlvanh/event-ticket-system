package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.DTO.response.DailyTicketSalesProjection;
import com.example.event_ticket_system.DTO.response.DailyTicketTypeSalesProjection;
import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Entity.OrderTicket;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTicketRepository extends JpaRepository<OrderTicket, Integer> {
    List<OrderTicket> findByOrder(Order order);

    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM OrderTicket o WHERE o.ticket.event = :event")
    long sumQuantityByEvent(@Param("event") Event event);

    @Query("SELECT COALESCE(SUM(ot.quantity), 0) FROM OrderTicket ot WHERE ot.ticket = :ticket")
    int sumQuantityByTicket(@Param("ticket") Ticket ticket);

    @Query("SELECT ot FROM OrderTicket ot WHERE ot.ticket.event = :event")
    List<OrderTicket> findAllByTicketEvent(@Param("event") Event event);

    List<OrderTicket> findByOrderOrderId(Integer orderId);

    boolean existsByTicket(Ticket ticket);

    // Sử dụng interface projection
    @Query("SELECT DATE(ot.order.orderDate) AS date, " +
            "COUNT(ot) AS totalTicketSold " +
            "FROM OrderTicket ot " +
            "WHERE ot.ticket.event.eventId = :eventId " +
            "AND ot.order.status = 'PAID' " +
            "GROUP BY DATE(ot.order.orderDate) " +
            "ORDER BY DATE(ot.order.orderDate)")
    List<DailyTicketSalesProjection> findTicketSoldPerDayByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT DATE(ot.order.orderDate) AS date, " +
            "ot.ticket.ticketType AS ticketType, " +
            "SUM(ot.quantity) AS totalQuantity " +
            "FROM OrderTicket ot " +
            "WHERE ot.ticket.event.eventId = :eventId " +
            "AND ot.order.status = 'PAID' " +
            "GROUP BY DATE(ot.order.orderDate), ot.ticket.ticketType " +
            "ORDER BY DATE(ot.order.orderDate), ot.ticket.ticketType")
    List<DailyTicketTypeSalesProjection> findTicketSoldPerTicketTypeByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT e.category " +
            "FROM OrderTicket ot " +
            "JOIN ot.order o " +
            "JOIN o.user u " +
            "JOIN ot.ticket t " +
            "JOIN t.event e " +
            "WHERE u.id = :userId " +
            "GROUP BY e.category " +
            "ORDER BY COUNT(e.category) DESC")
    List<String> findFavouriteGenresByUserId(@Param("userId") Integer userId);
}
