package com.satellite.api.dto;

import com.satellite.api.model.OrbitType;
import com.satellite.api.model.Satellite;
import com.satellite.api.model.SatelliteParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for creating and updating satellites.
 * Validation annotations ensure the client sends valid data.
 */
public class SatelliteRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Launch date is required")
    private LocalDateTime launchDate;

    @NotNull(message = "Orbit type is required")
    private OrbitType orbit;

    @Valid
    @NotNull(message = "Parameters are required")
    private SatelliteParametersRequest parameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(LocalDateTime launchDate) {
        this.launchDate = launchDate;
    }

    public OrbitType getOrbit() {
        return orbit;
    }

    public void setOrbit(OrbitType orbit) {
        this.orbit = orbit;
    }

    public SatelliteParametersRequest getParameters() {
        return parameters;
    }

    public void setParameters(SatelliteParametersRequest parameters) {
        this.parameters = parameters;
    }

    /**
     * Converts this request DTO to a Satellite entity.
     */
    public Satellite toEntity() {
        SatelliteParameters params = new SatelliteParameters(
                parameters.getAlt(),
                parameters.getLat(),
                parameters.getLon()
        );
        return new Satellite(name, launchDate, orbit, params);
    }
}
