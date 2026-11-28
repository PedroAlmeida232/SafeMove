package com.safemove.model;

/** Resultado da avaliação de uma rota pela nota (0 a 100). */
public enum Classificacao {
    SEGURA, ATENCAO, EVITE;

    static final int NOTA_SEGURA = 70;
    static final int NOTA_ATENCAO = 45;

    public static Classificacao daNota(int nota) {
        return nota >= NOTA_SEGURA ? SEGURA : nota >= NOTA_ATENCAO ? ATENCAO : EVITE;
    }
}
