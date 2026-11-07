package com.safemove.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class GeoTest {

    private static final Coordenada SE = new Coordenada(-23.5503, -46.6339);
    private static final Coordenada RIO = new Coordenada(-22.9068, -43.1729);

    @Test void distanciaDeUmPontoParaSiMesmoEZero() {
        assertEquals(0, Geo.distanciaMetros(SE, SE), 1e-9);
    }

    @Test void distanciaEntreSaoPauloERio() {
        // Conferido com a lei esférica dos cossenos: 360.805 m entre os dois centros.
        assertEquals(360_805, Geo.distanciaMetros(SE, RIO), 1);
    }

    @Test void distanciaESimetrica() {
        assertEquals(Geo.distanciaMetros(SE, RIO), Geo.distanciaMetros(RIO, SE), 1e-6);
    }

    @Test void umGrauDeLatitudeVale111km() {
        double d = Geo.distanciaMetros(new Coordenada(0, 0), new Coordenada(1, 0));
        assertEquals(111_195, d, 1);
    }

    @Test void antipodasNaoGeramNaN() {
        double d = Geo.distanciaMetros(new Coordenada(0, 0), new Coordenada(0, 180));
        assertEquals(Math.PI * Geo.RAIO_TERRA_METROS, d, 1e-3);
    }

    @Test void retanguloTemAsBordasNoRaio() {
        Retangulo r = Geo.retanguloEmVolta(SE, 1_000);
        double norte = Geo.distanciaMetros(SE, new Coordenada(r.maxLatitude(), SE.longitude()));
        double sul = Geo.distanciaMetros(SE, new Coordenada(r.minLatitude(), SE.longitude()));
        double leste = Geo.distanciaMetros(SE, new Coordenada(SE.latitude(), r.maxLongitude()));
        assertEquals(1_000, norte, 1);
        assertEquals(1_000, sul, 1);
        // Leste/oeste é medido ao longo do paralelo da borda, então fica um pouco maior que o raio.
        assertTrue(leste >= 999 && leste < 1_001, "leste=" + leste);
        assertTrue(r.contem(SE));
    }

    @Test void todoPontoDentroDoRaioCaiNoRetangulo() {
        Random rnd = new Random(42);
        for (double lat : new double[] {-23.55, 0, 45, 80}) {
            Coordenada centro = new Coordenada(lat, -46.63);
            double raio = 5_000;
            Retangulo r = Geo.retanguloEmVolta(centro, raio);
            for (int i = 0; i < 2_000; i++) {
                double ang = rnd.nextDouble() * 2 * Math.PI;
                double dist = rnd.nextDouble() * raio;
                double lat2 = lat + Math.toDegrees(dist * Math.cos(ang) / Geo.RAIO_TERRA_METROS);
                double lng2 = -46.63 + Math.toDegrees(dist * Math.sin(ang) / (Geo.RAIO_TERRA_METROS * Math.cos(Math.toRadians(lat))));
                Coordenada p = new Coordenada(lat2, lng2);
                if (Geo.distanciaMetros(centro, p) <= raio) {
                    assertTrue(r.contem(p), "ponto fora do retângulo: " + p + " lat=" + lat);
                }
            }
        }
    }

    @Test void pontoLongeNaoCaiNoRetangulo() {
        assertFalse(Geo.retanguloEmVolta(SE, 1_000).contem(RIO));
    }

    @Test void raioZeroViraUmPonto() {
        Retangulo r = Geo.retanguloEmVolta(SE, 0);
        assertEquals(SE.latitude(), r.minLatitude(), 1e-12);
        assertEquals(SE.latitude(), r.maxLatitude(), 1e-12);
        assertEquals(SE.longitude(), r.minLongitude(), 1e-12);
        assertEquals(SE.longitude(), r.maxLongitude(), 1e-12);
    }

    @Test void pertoDoPoloCobreTodasAsLongitudes() {
        Retangulo r = Geo.retanguloEmVolta(new Coordenada(89.9, 10), 50_000);
        assertEquals(90, r.maxLatitude(), 0);
        assertEquals(-180, r.minLongitude(), 0);
        assertEquals(180, r.maxLongitude(), 0);
    }

    @Test void entradasInvalidasSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class, () -> new Coordenada(91, 0));
        assertThrows(IllegalArgumentException.class, () -> new Coordenada(0, -181));
        assertThrows(IllegalArgumentException.class, () -> new Coordenada(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> Geo.retanguloEmVolta(SE, -1));
        assertThrows(IllegalArgumentException.class, () -> Geo.retanguloEmVolta(SE, Double.NaN));
    }
}
