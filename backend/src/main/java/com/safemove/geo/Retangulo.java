package com.safemove.geo;

/** Retângulo alinhado aos eixos de latitude/longitude, útil como pré-filtro de busca no banco. */
public record Retangulo(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {

    public boolean contem(Coordenada c) {
        return c.latitude() >= minLatitude && c.latitude() <= maxLatitude
                && c.longitude() >= minLongitude && c.longitude() <= maxLongitude;
    }
}
