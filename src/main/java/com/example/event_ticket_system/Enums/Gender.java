package com.example.event_ticket_system.Enums;

public enum Gender {
    Male,
    Female,
    Other;

    public boolean isEmpty() {
        return this.name().isEmpty();
    }
}