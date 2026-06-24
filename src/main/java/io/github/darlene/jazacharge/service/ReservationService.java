package io.github.darlene.jazacharge.service;

import io.github.darlene.jazacharge.entity.Reservations;
import io.github.darlene.jazacharge.entity.ReservationStatus;
import io.github.darlene.jazacharge.exception.ReservationNotFoundException;
import io.github.darlene.jazacharge.repository.ReservationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationsRepository reservationsRepository;

    public List<Reservations> getAllReservations() {
        return reservationsRepository.findAll();
    }

    public Reservations getByCode(String code) {
        return reservationsRepository.findByReservationCode(code)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + code));
    }

    @Transactional
    public Reservations cancelReservation(String code) {
        Reservations reservation = getByCode(code);
        reservation.setStatus(ReservationStatus.CANCELLED);
        // Free up the battery
        reservation.getBattery().setIsAvailable(true);
        return reservationsRepository.save(reservation);
    }

    @Transactional
    public Reservations completeReservation(String code) {
        Reservations reservation = getByCode(code);
        reservation.setStatus(ReservationStatus.COMPLETED);
        return reservationsRepository.save(reservation);
    }
}
