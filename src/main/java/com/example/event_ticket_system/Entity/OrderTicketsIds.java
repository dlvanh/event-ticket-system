package com.example.event_ticket_system.Entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class OrderTicketsIds implements Serializable {
    private Integer orderId;
    private Integer ticketId;

    public OrderTicketsIds() {
    }
    public OrderTicketsIds(Integer orderId, Integer ticketId) {
        this.orderId = orderId;
        this.ticketId = ticketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderTicketsIds that)) return false;
        return orderId.equals(that.orderId) && ticketId.equals(that.ticketId);
    }
    @Override
    public int hashCode() {
        int result = orderId.hashCode();
        result = 31 * result + ticketId.hashCode();
        return result;
    }
}
