package com.safemove.arearisco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraRiscoTest {

    @Test void nivelPorAssaltosSegueAsFaixas() {
        assertEquals(1, CalculadoraRisco.nivelPorAssaltos(0));
        assertEquals(2, CalculadoraRisco.nivelPorAssaltos(1));
        assertEquals(2, CalculadoraRisco.nivelPorAssaltos(2));
        assertEquals(3, CalculadoraRisco.nivelPorAssaltos(3));
        assertEquals(3, CalculadoraRisco.nivelPorAssaltos(5));
        assertEquals(4, CalculadoraRisco.nivelPorAssaltos(6));
        assertEquals(4, CalculadoraRisco.nivelPorAssaltos(9));
        assertEquals(5, CalculadoraRisco.nivelPorAssaltos(10));
        assertEquals(5, CalculadoraRisco.nivelPorAssaltos(100));
    }

    @Test void nivelOficialEhOQuintil() {
        List<Double> indices = List.of(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0);
        assertEquals(1, CalculadoraRisco.nivelOficial(10, indices));
        assertEquals(1, CalculadoraRisco.nivelOficial(20, indices));
        assertEquals(2, CalculadoraRisco.nivelOficial(30, indices));
        assertEquals(3, CalculadoraRisco.nivelOficial(50, indices));
        assertEquals(5, CalculadoraRisco.nivelOficial(90, indices));
        assertEquals(5, CalculadoraRisco.nivelOficial(100, indices));
    }

    @Test void empatesFicamNoMesmoNivel() {
        assertEquals(1, CalculadoraRisco.nivelOficial(7, List.of(7.0, 7.0, 7.0)));
        assertEquals(1, CalculadoraRisco.nivelOficial(7, List.of()));
    }

    @Test void combinaPeloMaiorESeguraAteDois() {
        assertEquals(4, CalculadoraRisco.combinar(2, 4));
        assertEquals(3, CalculadoraRisco.combinar(3, 1));
        assertEquals(3, CalculadoraRisco.combinar(3, null));
        assertTrue(CalculadoraRisco.segura(2));
        assertFalse(CalculadoraRisco.segura(3));
    }
}
