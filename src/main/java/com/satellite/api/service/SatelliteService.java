package com.satellite.api.service;

import com.satellite.api.model.Satellite;
import com.satellite.api.model.SatelliteParameters;

import java.util.List;

/**
 * Contract for satellite business operations.
 * The controller depends on this interface (not the implementation),
 * following the Dependency Inversion Principle.
 */
public interface SatelliteService {

    List<Satellite> getAllSatellites();

    Satellite getSatelliteById(Long id);

    Satellite createSatellite(Satellite satellite);

    Satellite updateSatellite(Long id, Satellite updatedSatellite);

    void deleteSatellite(Long id);

    SatelliteParameters getSatellitePosition(Long id);
}
