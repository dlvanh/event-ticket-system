package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    List<Ticket> findByEvent(Event event);
    List<Ticket> findByEventEventId(Integer eventId);
    List<Ticket> findByEventEventIdIn(Collection<Integer> eventIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t JOIN FETCH t.event WHERE t.ticketId = :ticketId")
    Optional<Ticket> findByIdForUpdate(@Param("ticketId") Integer ticketId);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.quantitySold = t.quantitySold + :qty " +
           "WHERE t.ticketId = :ticketId AND (t.quantityTotal - t.quantitySold) >= :qty")
    int reserveTickets(@Param("ticketId") Integer ticketId, @Param("qty") int qty);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.quantitySold = CASE WHEN t.quantitySold >= :qty THEN t.quantitySold - :qty ELSE 0 END " +
           "WHERE t.ticketId = :ticketId")
    int releaseTickets(@Param("ticketId") Integer ticketId, @Param("qty") int qty);
}
