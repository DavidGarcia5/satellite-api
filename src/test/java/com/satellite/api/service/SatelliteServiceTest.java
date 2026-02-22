package com.satellite.api.service;

import com.satellite.api.exception.SatelliteNotFoundException;
import com.satellite.api.model.OrbitType;
import com.satellite.api.model.Satellite;
import com.satellite.api.model.SatelliteParameters;
import com.satellite.api.repository.SatelliteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SatelliteService.
 * The repository is mocked — these tests verify business logic only.
 */
@ExtendWith(MockitoExtension.class)
class SatelliteServiceTest {

    @Mock
    private SatelliteRepository satelliteRepository;

    @InjectMocks
    private DefaultSatelliteService satelliteService;

    private Satellite satellite;

    @BeforeEach
    void setUp() {
        SatelliteParameters params = new SatelliteParameters(408.0, 51.6, -0.12);
        satellite = new Satellite("ISS", LocalDateTime.of(1998, 11, 20, 6, 40), OrbitType.LEO, params);
        satellite.setId(1L);
    }

    // --- getAllSatellites ---

    @Test
    void getAllSatellites_returnsList() {
        when(satelliteRepository.findAll()).thenReturn(List.of(satellite));

        List<Satellite> result = satelliteService.getAllSatellites();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("ISS");
        verify(satelliteRepository).findAll();
    }

    @Test
    void getAllSatellites_emptyList() {
        when(satelliteRepository.findAll()).thenReturn(List.of());

        List<Satellite> result = satelliteService.getAllSatellites();

        assertThat(result).isEmpty();
    }

    // --- getSatelliteById ---

    @Test
    void getSatelliteById_found() {
        when(satelliteRepository.findById(1L)).thenReturn(Optional.of(satellite));

        Satellite result = satelliteService.getSatelliteById(1L);

        assertThat(result.getName()).isEqualTo("ISS");
        assertThat(result.getOrbit()).isEqualTo(OrbitType.LEO);
    }

    @Test
    void getSatelliteById_notFound_throwsException() {
        when(satelliteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> satelliteService.getSatelliteById(99L))
                .isInstanceOf(SatelliteNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- createSatellite ---

    @Test
    void createSatellite_savesAndReturns() {
        when(satelliteRepository.save(any(Satellite.class))).thenReturn(satellite);

        Satellite result = satelliteService.createSatellite(satellite);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("ISS");
        verify(satelliteRepository).save(satellite);
    }

    // --- updateSatellite ---

    @Test
    void updateSatellite_updatesExisting() {
        Satellite updated = new Satellite("ISS Updated", LocalDateTime.now(), OrbitType.MEO,
                new SatelliteParameters(500.0, 40.0, 10.0));

        when(satelliteRepository.findById(1L)).thenReturn(Optional.of(satellite));
        when(satelliteRepository.save(any(Satellite.class))).thenReturn(satellite);

        Satellite result = satelliteService.updateSatellite(1L, updated);

        assertThat(result).isNotNull();
        verify(satelliteRepository).findById(1L);
        verify(satelliteRepository).save(any(Satellite.class));
    }

    @Test
    void updateSatellite_notFound_throwsException() {
        when(satelliteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> satelliteService.updateSatellite(99L, satellite))
                .isInstanceOf(SatelliteNotFoundException.class)
                .hasMessageContaining("99");

        verify(satelliteRepository, never()).save(any());
    }

    // --- deleteSatellite ---

    @Test
    void deleteSatellite_deletesExisting() {
        when(satelliteRepository.existsById(1L)).thenReturn(true);

        satelliteService.deleteSatellite(1L);

        verify(satelliteRepository).deleteById(1L);
    }

    @Test
    void deleteSatellite_notFound_throwsException() {
        when(satelliteRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> satelliteService.deleteSatellite(99L))
                .isInstanceOf(SatelliteNotFoundException.class)
                .hasMessageContaining("99");

        verify(satelliteRepository, never()).deleteById(any());
    }

    // --- getSatellitePosition ---

    @Test
    void getSatellitePosition_returnsParameters() {
        when(satelliteRepository.findById(1L)).thenReturn(Optional.of(satellite));

        SatelliteParameters position = satelliteService.getSatellitePosition(1L);

        assertThat(position.getAlt()).isEqualTo(408.0);
        assertThat(position.getLat()).isEqualTo(51.6);
        assertThat(position.getLon()).isEqualTo(-0.12);
    }

    @Test
    void getSatellitePosition_notFound_throwsException() {
        when(satelliteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> satelliteService.getSatellitePosition(99L))
                .isInstanceOf(SatelliteNotFoundException.class)
                .hasMessageContaining("99");
    }
}
