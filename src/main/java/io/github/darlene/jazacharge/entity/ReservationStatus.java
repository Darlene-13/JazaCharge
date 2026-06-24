package io.github.darlene.jazacharge.entity;

import lombok.Getter;

@Getter
public enum ReservationStatus {
    ACTIVE("ACTIVE"),
    EXPIRED("EXPIRED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String description;
    ReservationStatus(String description) { this.description = description; }
}