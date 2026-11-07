package com.safemove.geo;

/** Ponto na Terra em graus decimais (WGS84). */
public record Coordenada(double latitude, double longitude) {

    public Coordenada {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude deve estar entre -90 e 90: " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude deve estar entre -180 e 180: " + longitude);
        }
    }
}
