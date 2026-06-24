package io.github.darlene.jazacharge.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservationResponse {
    private String reservationCode;
    private String stationName;
    private String stationLocation;
    private String expiresAt;
    private String smsMessage; // pre-built SMS text to send to rider
}