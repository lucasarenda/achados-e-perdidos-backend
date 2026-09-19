package com.lostandfound.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GeocodingService {

    private final String baseUrl;
    private final String userAgent;
    private final RestTemplate restTemplate;

    public GeocodingService(
            @Value("${geocoding.api.base-url}") String baseUrl,
            @Value("${geocoding.api.user-agent}") String userAgent) {
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.restTemplate = new RestTemplate();

        log.info("GeocodingService inicializado - Base URL: '{}' | User-Agent: '{}'", this.baseUrl, this.userAgent);
    }

    public record Coordinates(double latitude, double longitude) {}

    public Optional<Coordinates> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("q", address)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, userAgent);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            List<Map<String, Object>> body = response.getBody();
            if (body == null || body.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> first = body.get(0);
            double lat = Double.parseDouble(first.get("lat").toString());
            double lon = Double.parseDouble(first.get("lon").toString());

            return Optional.of(new Coordinates(lat, lon));
        } catch (Exception e) {
            log.warn("Failed to geocode address '{}': {}", address, e.getMessage());
            return Optional.empty();
        }
    }

    public static double distanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }
}
