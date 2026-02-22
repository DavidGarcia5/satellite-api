package com.satellite.api.service;

import com.satellite.api.exception.SatelliteNotFoundException;
import com.satellite.api.model.Satellite;
import com.satellite.api.model.SatelliteParameters;
import com.satellite.api.repository.SatelliteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link SatelliteService}.
 * Handles all satellite business logic using the JPA repository.
 */
@Service
public class DefaultSatelliteService implements SatelliteService {

    private static final Logger log = LoggerFactory.getLogger(DefaultSatelliteService.class);

    private final SatelliteRepository satelliteRepository;

    public DefaultSatelliteService(SatelliteRepository satelliteRepository) {
        this.satelliteRepository = satelliteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Satellite> getAllSatellites() {
        log.info("Retrieving all satellites");
        return satelliteRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Satellite getSatelliteById(Long id) {
        log.info("Retrieving satellite with id: {}", id);
        return satelliteRepository.findById(id)
                .orElseThrow(() -> new SatelliteNotFoundException(id));
    }

    @Override
    @Transactional
    public Satellite createSatellite(Satellite satellite) {
        log.info("Creating satellite: {}", satellite.getName());
        return satelliteRepository.save(satellite);
    }

    @Override
    @Transactional
    public Satellite updateSatellite(Long id, Satellite updatedSatellite) {
        log.info("Updating satellite with id: {}", id);
        Satellite existing = satelliteRepository.findById(id)
                .orElseThrow(() -> new SatelliteNotFoundException(id));

        existing.setName(updatedSatellite.getName());
        existing.setLaunchDate(updatedSatellite.getLaunchDate());
        existing.setOrbit(updatedSatellite.getOrbit());
        existing.setParameters(updatedSatellite.getParameters());

        return satelliteRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteSatellite(Long id) {
        log.info("Deleting satellite with id: {}", id);
        if (!satelliteRepository.existsById(id)) {
            throw new SatelliteNotFoundException(id);
        }
        satelliteRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SatelliteParameters getSatellitePosition(Long id) {
        log.info("Retrieving position for satellite with id: {}", id);
        Satellite satellite = satelliteRepository.findById(id)
                .orElseThrow(() -> new SatelliteNotFoundException(id));
        return satellite.getParameters();
    }
}
