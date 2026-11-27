package com.safemove.integracao;

import com.safemove.geo.Coordenada;
import java.util.List;

/** Rota calculada pela TomTom: índice (0 = principal), resumo e o traçado completo. */
public record RotaTomTom(int indice, int distanciaMetros, int duracaoSegundos, int atrasoSegundos,
                         List<Coordenada> pontos) {}
