package com.safemove.arearisco;

import java.util.Collection;

/**
 * Regras do nível de risco de um distrito (1 = muito seguro … 5 = risco muito alto).
 *
 * <ul>
 *   <li><b>Assaltos reportados no app</b> nos últimos {@value #DIAS} dias: 0 → 1, 1–2 → 2, 3–5 → 3,
 *       6–9 → 4, 10 ou mais → 5.</li>
 *   <li><b>SSP (último mês)</b>: o índice roubos + furtos/2 do distrito é comparado com o dos outros
 *       distritos e vira um quintil: os 20% mais baixos → 1, …, os 20% mais altos → 5.</li>
 * </ul>
 * O nível final é o maior dos dois. Até o nível 2 a região é considerada segura.
 */
public final class CalculadoraRisco {

    /** Janela, em dias, dos assaltos reportados no app ("último mês"). */
    public static final int DIAS = 30;
    public static final int NIVEL_MAXIMO_SEGURO = 2;

    private CalculadoraRisco() {}

    public static int nivelPorAssaltos(int assaltos) {
        if (assaltos >= 10) return 5;
        if (assaltos >= 6) return 4;
        if (assaltos >= 3) return 3;
        if (assaltos >= 1) return 2;
        return 1;
    }

    /** Quintil do {@code indice} entre os índices de todos os distritos com dados (que incluem o próprio). */
    public static int nivelOficial(double indice, Collection<Double> indices) {
        if (indices.isEmpty()) return 1;
        long abaixo = indices.stream().filter(i -> i < indice).count();
        int nivel = 1 + (int) (5 * abaixo / indices.size());
        return Math.max(1, Math.min(5, nivel));
    }

    public static int combinar(int nivelAssaltos, Integer nivelOficial) {
        return nivelOficial == null ? nivelAssaltos : Math.max(nivelAssaltos, nivelOficial);
    }

    public static boolean segura(int nivel) {
        return nivel <= NIVEL_MAXIMO_SEGURO;
    }
}
