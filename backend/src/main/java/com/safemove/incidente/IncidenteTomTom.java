package com.safemove.incidente;

/** Incidente já convertido para o vocabulário do SafeMove, como veio da TomTom. */
public record IncidenteTomTom(String idTomtom, CategoriaIncidente categoria, int gravidade, String via,
                              String descricao, Integer atrasoSegundos, double latitude, double longitude) {}
