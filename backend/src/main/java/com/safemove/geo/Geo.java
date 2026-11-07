package com.safemove.geo;

/** Cálculos geográficos sobre uma Terra esférica (raio médio de 6.371 km). */
public final class Geo {

    /** Raio médio da Terra em metros (mesmo valor usado no app). */
    public static final double RAIO_TERRA_METROS = 6_371_000;

    private static final double METROS_POR_GRAU_LATITUDE = Math.PI * RAIO_TERRA_METROS / 180;

    private Geo() {}

    /** Distância em metros pela fórmula de Haversine (círculo máximo). */
    public static double distanciaMetros(Coordenada a, Coordenada b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.longitude() - a.longitude());

        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
        // min(1, ..) protege contra h = 1.0000000000000002 por arredondamento (pontos antípodas)
        return 2 * RAIO_TERRA_METROS * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }

    /**
     * Menor retângulo que contém o círculo de {@code raioMetros} ao redor do centro.
     * Todo ponto a até {@code raioMetros} do centro está dentro do retângulo (o inverso não vale:
     * os cantos ficam mais longe). Use-o para filtrar no banco e depois refine com
     * {@link #distanciaMetros}.
     *
     * <p>Limites: latitude é cortada em ±90 e longitude em ±180. Perto dos polos a longitude cobre
     * tudo. Não trata a linha de data (±180), irrelevante para São Paulo.
     */
    public static Retangulo retanguloEmVolta(Coordenada centro, double raioMetros) {
        if (!Double.isFinite(raioMetros) || raioMetros < 0) {
            throw new IllegalArgumentException("O raio deve ser um número >= 0: " + raioMetros);
        }

        double dLat = raioMetros / METROS_POR_GRAU_LATITUDE;
        double minLat = Math.max(-90, centro.latitude() - dLat);
        double maxLat = Math.min(90, centro.latitude() + dLat);

        // Um grau de longitude encolhe com cos(lat). Usa a latitude mais próxima do polo
        // do retângulo, onde o grau é mais curto, para não deixar pontos de fora.
        double latMaisExtrema = Math.max(Math.abs(minLat), Math.abs(maxLat));
        if (latMaisExtrema >= 90) {
            return new Retangulo(minLat, maxLat, -180, 180);
        }
        double dLng = Math.toDegrees(raioMetros / (RAIO_TERRA_METROS * Math.cos(Math.toRadians(latMaisExtrema))));
        if (dLng >= 180) {
            return new Retangulo(minLat, maxLat, -180, 180);
        }
        double minLng = Math.max(-180, centro.longitude() - dLng);
        double maxLng = Math.min(180, centro.longitude() + dLng);
        return new Retangulo(minLat, maxLat, minLng, maxLng);
    }
}
