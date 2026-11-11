package com.safemove.arearisco;

import java.time.YearMonth;

/** Roubos e furtos registrados pela SSP-SP num distrito, num mês (soma das delegacias do distrito). */
public record EstatisticaOficial(String distrito, YearMonth mes, int roubos, int furtos) {

    /** Peso de cada crime no índice que compara os distritos: roubo (com violência) pesa o dobro do furto. */
    public double indice() {
        return roubos + furtos / 2.0;
    }
}
