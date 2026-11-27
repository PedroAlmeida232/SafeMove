package com.safemove.geo;

import java.util.List;

/** Cálculos geográficos sobre uma Terra esférica (raio médio de 6.371 km). */
public final class Geo {

    /** Raio médio da Terra em metros (mesmo valor usado no app). */
    public static final double RAIO_TERRA_METROS = 6_371_000;

    /** Grande São Paulo, a área atendida pelo SafeMove (~3.300 km²). */
    public static final Retangulo GRANDE_SAO_PAULO = new Retangulo(-24.01, -23.36, -46.83, -46.36);

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

    /**
     * Menor distância em metros de um ponto até uma linha (rota), segmento a segmento.
     * Usa projeção plana local, precisa o bastante para distâncias de poucos quilômetros.
     */
    public static double distanciaAteRota(Coordenada p, List<Coordenada> rota) {
        if (rota.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        if (rota.size() == 1) {
            return distanciaMetros(p, rota.get(0));
        }
        double cosLat = Math.cos(Math.toRadians(p.latitude()));
        double menor = Double.POSITIVE_INFINITY;
        for (int i = 0; i < rota.size() - 1; i++) {
            Coordenada a = rota.get(i);
            Coordenada b = rota.get(i + 1);
            // Coordenadas em metros relativas a p.
            double ax = Math.toRadians(a.longitude() - p.longitude()) * cosLat * RAIO_TERRA_METROS;
            double ay = Math.toRadians(a.latitude() - p.latitude()) * RAIO_TERRA_METROS;
            double bx = Math.toRadians(b.longitude() - p.longitude()) * cosLat * RAIO_TERRA_METROS;
            double by = Math.toRadians(b.latitude() - p.latitude()) * RAIO_TERRA_METROS;
            double dx = bx - ax;
            double dy = by - ay;
            double comprimento2 = dx * dx + dy * dy;
            double t = comprimento2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / comprimento2));
            double x = ax + t * dx;
            double y = ay + t * dy;
            menor = Math.min(menor, Math.sqrt(x * x + y * y));
        }
        return menor;
    }
}
