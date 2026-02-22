package com.satellite.api.repository;

import com.satellite.api.model.Satellite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Satellite entity.
 * Spring Data JPA automatically provides the implementation at runtime.
 */
@Repository
public interface SatelliteRepository extends JpaRepository<Satellite, Long> {
}
