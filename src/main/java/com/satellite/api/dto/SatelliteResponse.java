package com.satellite.api.dto;

import com.satellite.api.model.OrbitType;
import com.satellite.api.model.Satellite;
import com.satellite.api.model.SatelliteParameters;

import java.time.LocalDateTime;

/**
 * Response DTO for satellite data returned to clients.
 * Decouples the API response shape from the JPA entity,
 * following the Single Responsibility Principle.
 */
public class SatelliteResponse {

    private Long id;
    private String name;
    private LocalDateTime launchDate;
    private OrbitType orbit;
    private SatelliteParameters parameters;

    public SatelliteResponse() {
    }

    public SatelliteResponse(Long id, String name, LocalDateTime launchDate,
                             OrbitType orbit, SatelliteParameters parameters) {
        this.id = id;
        this.name = name;
        this.launchDate = launchDate;
        this.orbit = orbit;
        this.parameters = parameters;
    }

    /**
     * Factory method to convert a Satellite entity to a response DTO.
     */
    public static SatelliteResponse fromEntity(Satellite satellite) {
        return new SatelliteResponse(
                satellite.getId(),
                satellite.getName(),
                satellite.getLaunchDate(),
                satellite.getOrbit(),
                satellite.getParameters()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getLaunchDate() {
        return launchDate;
    }

    public OrbitType getOrbit() {
        return orbit;
    }

    public SatelliteParameters getParameters() {
        return parameters;
    }
}
