package io.github.darlene.jazacharge.dto.response;

import lombok.Builder;
import lombok.Data;

// For the vanilla JS dashboard /api/stations endpoint
@Data
@Builder
public class StationStatusDto {
    private Long id;
    private String name;
    private String location;
    private Integer availableBatteries;
    private Boolean isActive;
    private Integer activeReservations;
}