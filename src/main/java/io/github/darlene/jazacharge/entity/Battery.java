package io.github.darlene.jazacharge.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "battery")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class Battery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "battery_type", nullable = false)
    private BatteryType batteryType;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private BatteryStation station;
}