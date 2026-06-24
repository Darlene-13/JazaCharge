package io.github.darlene.jazacharge.service;

import io.github.darlene.jazacharge.entity.*;
import io.github.darlene.jazacharge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final BatteryStationRepository stationRepository;
    private final BatteryRepository batteryRepository;

    @Override
    public void run(String... args) {
        if (stationRepository.count() > 0) return; // don't seed twice

        log.info("Seeding Nairobi swap stations...");

        // Create stations
        BatteryStation cbd = createStation("JazaCharge CBD Hub", "CBD", -1.2833, 36.8172);
        BatteryStation westlands = createStation("JazaCharge Westlands", "Westlands", -1.2636, 36.8030);
        BatteryStation ngong = createStation("JazaCharge Ngong Road", "Ngong Road", -1.3031, 36.7737);
        BatteryStation kasarani = createStation("JazaCharge Kasarani", "Kasarani", -1.2202, 36.8975);
        BatteryStation eastleigh = createStation("JazaCharge Eastleigh", "Eastleigh", -1.2748, 36.8450);

        List<BatteryStation> stations = stationRepository.saveAll(
            List.of(cbd, westlands, ngong, kasarani, eastleigh)
        );

        // Seed batteries for each station
        for (BatteryStation station : stations) {
            seedBatteries(station, BatteryType.LITHIUM_48V, 3);
            seedBatteries(station, BatteryType.LITHIUM_60V, 2);
            seedBatteries(station, BatteryType.LITHIUM_72V, 2);
        }

        log.info("Seeded {} stations with batteries", stations.size());
    }

    private BatteryStation createStation(String name, String location, double lat, double lng) {
        return BatteryStation.builder()
                .name(name)
                .location(location)
                .latitude(lat)
                .longitude(lng)
                .isActive(true)
                .build();
    }

    private void seedBatteries(BatteryStation station, BatteryType type, int count) {
        for (int i = 0; i < count; i++) {
            Battery battery = Battery.builder()
                    .batteryType(type)
                    .isAvailable(true)
                    .station(station)
                    .build();
            batteryRepository.save(battery);
        }
    }
}
