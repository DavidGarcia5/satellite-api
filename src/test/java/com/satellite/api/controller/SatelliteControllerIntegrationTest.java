package com.satellite.api.controller;

import com.satellite.api.model.OrbitType;
import com.satellite.api.model.Satellite;
import com.satellite.api.model.SatelliteParameters;
import com.satellite.api.repository.SatelliteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Satellite REST API.
 * Uses a real Spring context with an H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SatelliteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SatelliteRepository satelliteRepository;

    private Satellite savedSatellite;

    @BeforeEach
    void setUp() {
        satelliteRepository.deleteAll();

        Satellite satellite = new Satellite(
                "ISS",
                LocalDateTime.of(1998, 11, 20, 6, 40),
                OrbitType.LEO,
                new SatelliteParameters(408.0, 51.6, -0.12)
        );
        savedSatellite = satelliteRepository.save(satellite);
    }

    // --- GET /api/satellites ---

    @Test
    void getAllSatellites_returns200WithList() throws Exception {
        mockMvc.perform(get("/api/satellites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("ISS")));
    }

    // --- GET /api/satellites/{id} ---

    @Test
    void getSatelliteById_returns200() throws Exception {
        mockMvc.perform(get("/api/satellites/{id}", savedSatellite.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("ISS")))
                .andExpect(jsonPath("$.orbit", is("LEO")))
                .andExpect(jsonPath("$.parameters.alt", is(408.0)));
    }

    @Test
    void getSatelliteById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/satellites/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("9999")));
    }

    // --- POST /api/satellites ---

    @Test
    void createSatellite_returns201() throws Exception {
        String json = """
                {
                    "name": "Hubble",
                    "launchDate": "1990-04-24T12:33:00",
                    "orbit": "LEO",
                    "parameters": {
                        "alt": 547.0,
                        "lat": 28.5,
                        "lon": -80.6
                    }
                }
                """;

        mockMvc.perform(post("/api/satellites")
                .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Hubble")))
                .andExpect(jsonPath("$.orbit", is("LEO")));
    }

    @Test
    void createSatellite_invalidInput_returns400() throws Exception {
        String json = """
                {
                    "name": "",
                    "launchDate": null,
                    "orbit": null,
                    "parameters": null
                }
                """;

        mockMvc.perform(post("/api/satellites")
                .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasSize(greaterThan(0))));
    }

    @Test
    void createSatellite_invalidParameters_returns400() throws Exception {
        String json = """
                {
                    "name": "BadSat",
                    "launchDate": "2020-01-01T00:00:00",
                    "orbit": "LEO",
                    "parameters": {
                        "alt": -100,
                        "lat": 999,
                        "lon": -999
                    }
                }
                """;

        mockMvc.perform(post("/api/satellites")
                .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasSize(greaterThan(0))));
    }

    // --- PUT /api/satellites/{id} ---

    @Test
    void updateSatellite_returns200() throws Exception {
        String json = """
                {
                    "name": "ISS Updated",
                    "launchDate": "1998-11-20T06:40:00",
                    "orbit": "LEO",
                    "parameters": {
                        "alt": 420.0,
                        "lat": 45.0,
                        "lon": 10.0
                    }
                }
                """;

        mockMvc.perform(put("/api/satellites/{id}", savedSatellite.getId())
                .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("ISS Updated")))
                .andExpect(jsonPath("$.parameters.alt", is(420.0)));
    }

    @Test
    void updateSatellite_notFound_returns404() throws Exception {
        String json = """
                {
                    "name": "Ghost",
                    "launchDate": "2020-01-01T00:00:00",
                    "orbit": "GEO",
                    "parameters": {
                        "alt": 35786.0,
                        "lat": 0.0,
                        "lon": 0.0
                    }
                }
                """;

        mockMvc.perform(put("/api/satellites/{id}", 9999)
                .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/satellites/{id} ---

    @Test
    void deleteSatellite_returns204() throws Exception {
        mockMvc.perform(delete("/api/satellites/{id}", savedSatellite.getId())
                .with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/satellites/{id}", savedSatellite.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSatellite_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/satellites/{id}", 9999)
                .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    // --- Security Tests ---

    @Test
    void createSatellite_unauthenticated_returns401() throws Exception {
        String json = """
                {
                    "name": "Unauthorized Sat",
                    "launchDate": "2020-01-01T00:00:00",
                    "orbit": "LEO",
                    "parameters": {
                        "alt": 500.0,
                        "lat": 0.0,
                        "lon": 0.0
                    }
                }
                """;

        mockMvc.perform(post("/api/satellites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/satellites/{id}/position ---

    @Test
    void getSatellitePosition_returns200() throws Exception {
        mockMvc.perform(get("/api/satellites/{id}/position", savedSatellite.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alt", is(408.0)))
                .andExpect(jsonPath("$.lat", is(51.6)))
                .andExpect(jsonPath("$.lon", is(-0.12)));
    }

    @Test
    void getSatellitePosition_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/satellites/{id}/position", 9999))
                .andExpect(status().isNotFound());
    }
}
