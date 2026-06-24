package io.github.darlene.jazacharge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JazaChargeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JazaChargeApplication.class, args);
    }

}
