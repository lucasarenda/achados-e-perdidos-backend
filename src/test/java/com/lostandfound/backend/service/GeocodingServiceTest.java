package com.lostandfound.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("GeocodingService.distanceInMeters")
class GeocodingServiceTest {

    @Test
    @DisplayName("returns 0 for identical coordinates")
    void returnsZeroForSameCoordinates() {
        double distance = GeocodingService.distanceInMeters(-23.5489, -46.6388, -23.5489, -46.6388);

        assertThat(distance).isEqualTo(0.0, within(0.001));
    }

    @Test
    @DisplayName("calculates the approximate distance between two nearby points in São Paulo")
    void calculatesApproximateDistance() {
        // Praça da Sé and Mosteiro de São Bento, São Paulo - roughly 850m apart
        double distance = GeocodingService.distanceInMeters(
                -23.5505, -46.6333,
                -23.5438, -46.6339);

        assertThat(distance).isBetween(600.0, 900.0);
    }

    @Test
    @DisplayName("calculates a much larger distance between distant cities")
    void calculatesLargeDistance() {
        // São Paulo to Rio de Janeiro - roughly 360km
        double distance = GeocodingService.distanceInMeters(
                -23.5505, -46.6333,
                -22.9068, -43.1729);

        assertThat(distance).isBetween(350_000.0, 370_000.0);
    }
}
