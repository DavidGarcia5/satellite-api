package com.satellite.api.config;

import com.satellite.api.model.OrbitType;
import com.satellite.api.model.Satellite;
import com.satellite.api.model.SatelliteParameters;
import com.satellite.api.repository.SatelliteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds the H2 database with sample satellite data on application startup.
 * This allows the API to be explored immediately via Swagger UI or Postman
 * without manually creating entries first.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SatelliteRepository satelliteRepository;

    public DataSeeder(SatelliteRepository satelliteRepository) {
        this.satelliteRepository = satelliteRepository;
    }

    @Override
    public void run(String... args) {
        if (satelliteRepository.count() > 0) {
            log.info("Database already seeded, skipping");
            return;
        }

        satelliteRepository.save(new Satellite(
                "ISS",
                LocalDateTime.of(1998, 11, 20, 6, 40),
                OrbitType.LEO,
                new SatelliteParameters(408.0, 51.6, -0.12)
        ));

        satelliteRepository.save(new Satellite(
                "Hubble Space Telescope",
                LocalDateTime.of(1990, 4, 24, 12, 33),
                OrbitType.LEO,
                new SatelliteParameters(547.0, 28.5, -80.6)
        ));

        satelliteRepository.save(new Satellite(
                "GPS IIF-1",
                LocalDateTime.of(2010, 5, 28, 3, 0),
                OrbitType.MEO,
                new SatelliteParameters(20200.0, 55.0, -105.0)
        ));

        log.info("Seeded database with 3 sample satellites");
    }
}
