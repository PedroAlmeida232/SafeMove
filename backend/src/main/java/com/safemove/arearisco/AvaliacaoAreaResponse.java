package com.safemove.arearisco;

/** Resposta de GET /areas-risco/avaliar: a área onde o ponto está e o texto do alerta para o usuário. */
public record AvaliacaoAreaResponse(AreaRiscoResponse area, double distanciaMetros, String titulo,
                                    String mensagem) {}
