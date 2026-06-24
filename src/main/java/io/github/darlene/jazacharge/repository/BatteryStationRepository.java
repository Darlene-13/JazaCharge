package io.github.darlene.jazacharge.repository;

import io.github.darlene.jazacharge.entity.BatteryStation;
import io.github.darlene.jazacharge.entity.BatteryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BatteryStationRepository extends JpaRepository<BatteryStation, Long> {

    List<BatteryStation> findByIsActiveTrue();

    @Query("SELECT s FROM BatteryStation s JOIN s.batteries b WHERE b.batteryType = :type AND b.isAvailable = true AND s.isActive = true")
    List<BatteryStation> findStationsWithAvailableBattery(@Param("type") BatteryType type);

    @Query("SELECT s FROM BatteryStation s WHERE LOWER(s.location) LIKE LOWER(CONCAT('%', :location, '%')) AND s.isActive = true")
    List<BatteryStation> findByLocationContaining(@Param("location") String location);
}