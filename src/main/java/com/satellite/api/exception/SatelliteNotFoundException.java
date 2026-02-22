package com.satellite.api.exception;

/**
 * Thrown when a satellite with the requested ID does not exist.
 */
public class SatelliteNotFoundException extends RuntimeException {

    public SatelliteNotFoundException(Long id) {
        super("Satellite not found with id: " + id);
    }
}
