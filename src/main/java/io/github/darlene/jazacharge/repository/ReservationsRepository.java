package io.github.darlene.jazacharge.repository;

import io.github.darlene.jazacharge.entity.Reservations;
import io.github.darlene.jazacharge.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationsRepository extends JpaRepository<Reservations, Long> {

    Optional<Reservations> findByReservationCode(String reservationCode);

    List<Reservations> findByRiderPhoneNumberAndStatus(String phoneNumber, ReservationStatus status);

    @Modifying
    @Query("UPDATE Reservations r SET r.status = 'EXPIRED' WHERE r.expiresAt < :now AND r.status = 'ACTIVE'")
    int expireOldReservations(@Param("now") LocalDateTime now);

    List<Reservations> findByStationIdAndStatus(Long stationId, ReservationStatus status);
}