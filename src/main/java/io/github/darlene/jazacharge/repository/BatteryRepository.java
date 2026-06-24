package io.github.darlene.jazacharge.repository;

import io.github.darlene.jazacharge.entity.Battery;
import io.github.darlene.jazacharge.entity.BatteryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatteryRepository extends JpaRepository<Battery, Long> {

    List<Battery> findByBatteryTypeAndIsAvailableTrue(BatteryType batteryType);

    @Query("SELECT b FROM Battery b WHERE b.station.id = :stationId AND b.batteryType = :type AND b.isAvailable = true")
    Optional<Battery> findFirstAvailableAtStation(@Param("stationId") Long stationId, @Param("type") BatteryType type);

    @Query("SELECT COUNT(b) FROM Battery b WHERE b.station.id = :stationId AND b.isAvailable = true")
    Integer countAvailableAtStation(@Param("stationId") Long stationId);
}