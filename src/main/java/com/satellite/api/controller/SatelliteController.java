package com.satellite.api.controller;

import com.satellite.api.dto.SatelliteRequest;
import com.satellite.api.dto.SatelliteResponse;
import com.satellite.api.dto.ErrorResponse;
import com.satellite.api.model.Satellite;
import com.satellite.api.model.SatelliteParameters;
import com.satellite.api.service.SatelliteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for satellite CRUD operations.
 */
@RestController
@RequestMapping("/api/satellites")
@Tag(name = "Satellites", description = "Satellite management and tracking API")
public class SatelliteController {

    private final SatelliteService satelliteService;

    public SatelliteController(SatelliteService satelliteService) {
        this.satelliteService = satelliteService;
    }

    @GetMapping
    @Operation(summary = "Get all satellites", description = "Retrieves a list of all tracked satellites")
    @ApiResponse(responseCode = "200", description = "Satellite list retrieved successfully")
    public ResponseEntity<List<SatelliteResponse>> getAllSatellites() {
        List<SatelliteResponse> responses = satelliteService.getAllSatellites()
                .stream()
                .map(SatelliteResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get satellite by ID", description = "Retrieves details of a specific satellite")
    @ApiResponse(responseCode = "200", description = "Satellite found")
    @ApiResponse(responseCode = "404", description = "Satellite not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<SatelliteResponse> getSatelliteById(@PathVariable Long id) {
        return ResponseEntity.ok(SatelliteResponse.fromEntity(satelliteService.getSatelliteById(id)));
    }

    @PostMapping
    @Operation(summary = "Add a new satellite", description = "Creates a new satellite entry in the system")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponse(responseCode = "201", description = "Satellite created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required")
    public ResponseEntity<SatelliteResponse> createSatellite(@Valid @RequestBody SatelliteRequest request) {
        Satellite created = satelliteService.createSatellite(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(SatelliteResponse.fromEntity(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a satellite", description = "Updates an existing satellite by ID")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponse(responseCode = "200", description = "Satellite updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Satellite not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<SatelliteResponse> updateSatellite(@PathVariable Long id,
                                                             @Valid @RequestBody SatelliteRequest request) {
        Satellite updated = satelliteService.updateSatellite(id, request.toEntity());
        return ResponseEntity.ok(SatelliteResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a satellite", description = "Removes a satellite from the system by ID")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponse(responseCode = "204", description = "Satellite deleted successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Satellite not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteSatellite(@PathVariable Long id) {
        satelliteService.deleteSatellite(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/position")
    @Operation(summary = "Get satellite position", description = "Retrieves the current position (lat, lon, alt) of a satellite")
    @ApiResponse(responseCode = "200", description = "Position retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Satellite not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<SatelliteParameters> getSatellitePosition(@PathVariable Long id) {
        return ResponseEntity.ok(satelliteService.getSatellitePosition(id));
    }
}
