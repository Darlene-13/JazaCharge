package io.github.darlene.jazacharge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "riders")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class Rider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "last_location")
    private String lastLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_battery")
    private BatteryType preferredBattery;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}