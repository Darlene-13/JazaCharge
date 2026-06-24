package io.github.darlene.jazacharge.service;

import io.github.darlene.jazacharge.dto.request.ParsedRiderIntent;
import io.github.darlene.jazacharge.dto.response.ReservationResponse;
import io.github.darlene.jazacharge.entity.*;
import io.github.darlene.jazacharge.exception.*;
import io.github.darlene.jazacharge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentService {

    private final BatteryStationRepository stationRepository;
    private final BatteryRepository batteryRepository;
    private final ReservationsRepository reservationsRepository;
    private final RiderRepository riderRepository;
    private final SmsService smsService;

    @Transactional
    public ReservationResponse processRiderRequest(String phoneNumber, ParsedRiderIntent intent) {

        log.info("Agent processing: phone={}, location={}, battery={}",
            phoneNumber, intent.getLocation(), intent.getBatteryType());

        // Step 1: Find or create rider
        Rider rider = riderRepository.findByPhoneNumber(phoneNumber)
            .orElseGet(() -> {
                Rider newRider = Rider.builder()
                    .phoneNumber(phoneNumber)
                    .preferredBattery(intent.getBatteryType())
                    .lastLocation(intent.getLocation())
                    .build();
                return riderRepository.save(newRider);
            });

        // Update rider's last known location
        rider.setLastLocation(intent.getLocation());
        riderRepository.save(rider);

        // Step 2: Find stations near requested location with available battery
        List<BatteryStation> candidates = stationRepository
            .findStationsWithAvailableBattery(intent.getBatteryType());

        if (candidates.isEmpty()) {
            throw new NoBatteryAvailableException(
                "No " + intent.getBatteryType().getDescription() + " batteries available near " + intent.getLocation()
            );
        }

        // Step 3: Pick best station — prioritize location match, then most available
        BatteryStation bestStation = candidates.stream()
            .filter(s -> s.getLocation().toLowerCase()
                .contains(intent.getLocation().toLowerCase()))
            .findFirst()
            .orElse(candidates.get(0)); // fallback to first available

        // Step 4: Lock a specific battery at that station
        Battery battery = batteryRepository
            .findFirstAvailableAtStation(bestStation.getId(), intent.getBatteryType())
            .orElseThrow(() -> new NoBatteryAvailableException("Battery just taken, try again"));

        // Step 5: Mark battery as unavailable
        battery.setIsAvailable(false);
        batteryRepository.save(battery);

        // Step 6: Create reservation — 15 minute hold
        String reservationCode = "UPP-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        Reservations reservation = Reservations.builder()
            .reservationCode(reservationCode)
            .rider(rider)
            .station(bestStation)
            .battery(battery)
            .status(ReservationStatus.ACTIVE)
            .expiresAt(expiresAt)
            .build();

        reservationsRepository.save(reservation);

        // Step 7: Check if station is now low — trigger bulk reroute SMS
        int remaining = batteryRepository.countAvailableAtStation(bestStation.getId());
        if (remaining <= 1) {
            log.warn("Station {} is running low — triggering reroute alerts", bestStation.getName());
            triggerLowStockAlert(bestStation, candidates);
        }

        // Step 8: Build response
        String expiryString = expiresAt.format(DateTimeFormatter.ofPattern("HH:mm"));
        String smsMessage = smsService.buildReservationConfirmation(
            reservationCode, bestStation.getName(), bestStation.getLocation(), expiryString
        );

        log.info("Reservation created: {} for rider {} at {}", reservationCode, phoneNumber, bestStation.getName());

        return ReservationResponse.builder()
            .reservationCode(reservationCode)
            .stationName(bestStation.getName())
            .stationLocation(bestStation.getLocation())
            .expiresAt(expiryString)
            .smsMessage(smsMessage)
            .build();
    }

    // Auto-expire reservations every 2 minutes
    @Scheduled(fixedRate = 120000)
    @Transactional
    public void expireOldReservations() {
        int expired = reservationsRepository.expireOldReservations(LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} stale reservations", expired);
            // Free up batteries for expired reservations
            // In production you'd also release the battery.isAvailable back to true
        }
    }

    private void triggerLowStockAlert(BatteryStation lowStation, List<BatteryStation> alternatives) {
        if (alternatives.size() <= 1) return;

        BatteryStation alternative = alternatives.stream()
            .filter(s -> !s.getId().equals(lowStation.getId()))
            .findFirst()
            .orElse(null);

        if (alternative == null) return;

        // Get riders who have recently requested at this station area
        List<Reservations> activeAtStation = reservationsRepository
            .findByStationIdAndStatus(lowStation.getId(), ReservationStatus.ACTIVE);

        List<String> phones = activeAtStation.stream()
            .map(r -> r.getRider().getPhoneNumber())
            .toList();

        if (!phones.isEmpty()) {
            String alert = smsService.buildRerouteAlert(
                lowStation.getName(), alternative.getName(), 10
            );
            smsService.sendBulkSms(phones, alert);
        }
    }
}
