package io.github.darlene.jazacharge.controller;

import io.github.darlene.jazacharge.dto.response.StationStatusDto;
import io.github.darlene.jazacharge.entity.BatteryStation;
import io.github.darlene.jazacharge.entity.ReservationStatus;
import io.github.darlene.jazacharge.repository.BatteryStationRepository;
import io.github.darlene.jazacharge.repository.ReservationsRepository;
import io.github.darlene.jazacharge.repository.BatteryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StationController {

    private final BatteryStationRepository stationRepository;
    private final ReservationsRepository reservationsRepository;
    private final BatteryRepository batteryRepository;

    // Dashboard — all stations with live battery count
    @GetMapping("/stations")
    public ResponseEntity<List<StationStatusDto>> getStations() {
        List<BatteryStation> stations = stationRepository.findByIsActiveTrue();

        List<StationStatusDto> result = stations.stream().map(s -> StationStatusDto.builder()
            .id(s.getId())
            .name(s.getName())
            .location(s.getLocation())
            .availableBatteries(batteryRepository.countAvailableAtStation(s.getId()))
            .isActive(s.getIsActive())
            .activeReservations(
                reservationsRepository.findByStationIdAndStatus(s.getId(), ReservationStatus.ACTIVE).size()
            )
            .build()
        ).toList();

        return ResponseEntity.ok(result);
    }

    // Dashboard — all reservations
    @GetMapping("/reservations")
    public ResponseEntity<?> getReservations() {
        return ResponseEntity.ok(reservationsRepository.findAll());
    }

    // Dashboard — all riders count
    @GetMapping("/riders")
    public ResponseEntity<?> getRiders(Pageable pageable) {
        return ResponseEntity.ok(reservationsRepository.findAll(pageable));
    }
    // Health check
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("JazaCharge is live");
    }
}
