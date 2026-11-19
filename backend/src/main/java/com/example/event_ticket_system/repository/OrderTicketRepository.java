package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.dto.response.DailyTicketSalesProjection;
import com.example.event_ticket_system.dto.response.DailyTicketTypeSalesProjection;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.Order;
import com.example.event_ticket_system.entity.OrderTicket;
import com.example.event_ticket_system.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderTicketRepository extends JpaRepository<OrderTicket, Integer> {

    List<OrderTicket> findByOrder(Order order);

    @Query("SELECT ot FROM OrderTicket ot JOIN FETCH ot.ticket t JOIN FETCH t.event e WHERE ot.order = :order")
    List<OrderTicket> findByOrderWithTicketAndEvent(@Param("order") Order order);

    @Query("SELECT ot FROM OrderTicket ot JOIN FETCH ot.ticket t JOIN FETCH t.event e JOIN FETCH ot.order o WHERE o.orderId IN :orderIds")
    List<OrderTicket> findByOrderIdsWithDetails(@Param("orderIds") Collection<Integer> orderIds);

    @Query("SELECT ot.ticket.event.eventId, COALESCE(SUM(ot.quantity), 0) FROM OrderTicket ot " +
           "WHERE ot.ticket.event.eventId IN :eventIds " +
           "AND ot.order.status = com.example.event_ticket_system.enums.OrderStatus.paid " +
           "GROUP BY ot.ticket.event.eventId")
    List<Object[]> sumPaidQuantityByEventIds(@Param("eventIds") Collection<Integer> eventIds);

    @Query("SELECT COALESCE(SUM(ot.quantity), 0) FROM OrderTicket ot " +
           "WHERE ot.ticket = :ticket AND ot.order.status = com.example.event_ticket_system.enums.OrderStatus.paid")
    int sumQuantityByTicket(@Param("ticket") Ticket ticket);

    @Query("SELECT ot FROM OrderTicket ot " +
           "JOIN FETCH ot.order o " +
           "JOIN FETCH o.user " +
           "JOIN FETCH ot.ticket t " +
           "WHERE t.event = :event AND o.status = com.example.event_ticket_system.enums.OrderStatus.paid")
    List<OrderTicket> findAllByTicketEvent(@Param("event") Event event);

    boolean existsByTicket(Ticket ticket);

    Optional<OrderTicket> findByCheckinToken(String checkinToken);

    @org.springframework.data.jpa.repository.Query("SELECT ot FROM OrderTicket ot " +
           "JOIN FETCH ot.order o " +
           "JOIN FETCH ot.ticket t " +
           "JOIN FETCH t.event e " +
           "JOIN FETCH e.organizer " +
           "WHERE ot.checkinToken = :token")
    Optional<OrderTicket> findByCheckinTokenWithDetails(@Param("token") String token);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE OrderTicket ot SET ot.checkedInAt = :now " +
           "WHERE ot.checkinToken = :token AND ot.order.status = com.example.event_ticket_system.enums.OrderStatus.paid " +
           "AND ot.checkedInAt IS NULL")
    int markCheckedIn(@Param("token") String token, @Param("now") java.time.LocalDateTime now);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(ot) > 0 FROM OrderTicket ot " +
           "WHERE ot.order.user.id = :userId AND ot.ticket.event.eventId = :eventId " +
           "AND ot.order.status = com.example.event_ticket_system.enums.OrderStatus.paid")
    boolean existsPaidTicketByUserAndEvent(@Param("userId") Integer userId, @Param("eventId") Integer eventId);

    @Query("SELECT CAST(ot.order.orderDate AS LocalDate) AS date, " +
           "COALESCE(SUM(ot.quantity), 0) AS totalTicketSold " +
           "FROM OrderTicket ot " +
           "WHERE ot.ticket.event.eventId = :eventId " +
           "AND ot.order.status = com.example.event_ticket_system.enums.OrderStatus.paid " +
           "GROUP BY CAST(ot.order.orderDate AS LocalDate) " +
           "ORDER BY CAST(ot.order.orderDate AS LocalDate)")
    List<DailyTicketSalesProjection> findTicketSoldPerDayByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT CAST(ot.order.orderDate AS LocalDate) AS date, " +
           "ot.ticket.ticketType AS ticketType, " +
           "COALESCE(SUM(ot.quantity), 0) AS totalQuantity " +
           "FROM OrderTicket ot " +
           "WHERE ot.ticket.event.eventId = :eventId " +
           "AND ot.order.status = com.example.event_ticket_system.enums.OrderStatus.paid " +
           "GROUP BY CAST(ot.order.orderDate AS LocalDate), ot.ticket.ticketType " +
           "ORDER BY CAST(ot.order.orderDate AS LocalDate), ot.ticket.ticketType")
    List<DailyTicketTypeSalesProjection> findTicketSoldPerTicketTypeByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT e.category " +
           "FROM OrderTicket ot " +
           "JOIN ot.order o " +
           "JOIN o.user u " +
           "JOIN ot.ticket t " +
           "JOIN t.event e " +
           "WHERE u.id = :userId AND o.status = com.example.event_ticket_system.enums.OrderStatus.paid " +
           "GROUP BY e.category " +
           "ORDER BY SUM(ot.quantity) DESC")
    List<String> findFavouriteGenresByUserId(@Param("userId") Integer userId);
}
