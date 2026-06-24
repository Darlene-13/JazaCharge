package io.github.darlene.jazacharge.entity;

import lombok.Getter;

@Getter
public enum BatteryType {
    LITHIUM_48V("LITHIUM_48V"),
    LITHIUM_60V("LITHIUM_60V"),
    LITHIUM_72V("LITHIUM_72V");

    private final String description;
    BatteryType(String description) { this.description = description; }
}