package com.satellite.api.model;

import jakarta.persistence.Embeddable;

/**
 * Represents the current positional parameters of a satellite.
 * Embedded within the Satellite entity (stored in the same table).
 */
@Embeddable
public class SatelliteParameters {

    private double alt;  // Altitude in km
    private double lat;  // Latitude in degrees (-90 to 90)
    private double lon;  // Longitude in degrees (-180 to 180)

    public SatelliteParameters() {
    }

    public SatelliteParameters(double alt, double lat, double lon) {
        this.alt = alt;
        this.lat = lat;
        this.lon = lon;
    }

    public double getAlt() {
        return alt;
    }

    public void setAlt(double alt) {
        this.alt = alt;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
