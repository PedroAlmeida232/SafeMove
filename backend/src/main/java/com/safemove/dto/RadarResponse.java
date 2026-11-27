package com.safemove.dto;

import com.safemove.model.Radar;

/** distanciaMetros só vem preenchida quando a busca foi feita com lat/lng. */
public record RadarResponse(Long id, double latitude, double longitude, Integer velocidadeMax, Double distanciaMetros) {

    public static RadarResponse de(Radar r, Double distanciaMetros) {
        return new RadarResponse(r.getId(), r.getLatitude(), r.getLongitude(), r.getVelocidadeMax(), distanciaMetros);
    }
}
