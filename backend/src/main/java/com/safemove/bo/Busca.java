package com.safemove.bo;

import com.safemove.exception.ApiException;
import com.safemove.geo.Coordenada;

/** Validações comuns das buscas por raio (lat, lng e raio em metros). */
final class Busca {

    static final double RAIO_MAXIMO_METROS = 50_000;

    private Busca() {}

    static Coordenada centro(double lat, double lng) {
        try {
            return new Coordenada(lat, lng);
        } catch (IllegalArgumentException e) {
            throw ApiException.requisicaoInvalida(e.getMessage());
        }
    }

    static double raio(Double raio, double padrao) {
        double raioMetros = raio == null ? padrao : raio;
        if (!Double.isFinite(raioMetros) || raioMetros <= 0 || raioMetros > RAIO_MAXIMO_METROS) {
            throw ApiException.requisicaoInvalida("O raio deve estar entre 0 e 50000 metros.");
        }
        return raioMetros;
    }
}
