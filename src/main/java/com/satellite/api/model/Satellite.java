package com.satellite.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a satellite.
 */
@Entity
@Table(name = "satellites")
public class Satellite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime launchDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrbitType orbit;

    @Embedded
    private SatelliteParameters parameters;

    public Satellite() {
    }

    public Satellite(String name, LocalDateTime launchDate, OrbitType orbit, SatelliteParameters parameters) {
        this.name = name;
        this.launchDate = launchDate;
        this.orbit = orbit;
        this.parameters = parameters;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public SatelliteParameters getParameters() {
        return parameters;
    }

    public void setParameters(SatelliteParameters parameters) {
        this.parameters = parameters;
    }
}
