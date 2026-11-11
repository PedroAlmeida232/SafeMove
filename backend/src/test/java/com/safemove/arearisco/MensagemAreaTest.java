package com.safemove.arearisco;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MensagemAreaTest {

    @Test void soComAssaltoReportado() {
        var a = new AreaRiscoResponse("Brás", 0, 0, 3, false, 1, null, null, null);
        assertEquals("Brás: risco moderado. 1 assalto reportado no app nos últimos 30 dias."
                + " Evite exibir o celular e prefira vias movimentadas e iluminadas.", AreaRiscoService.mensagem(a));
        assertEquals("Atenção nesta região", AreaRiscoService.titulo(a));
    }

    @Test void soComSsp() {
        var a = new AreaRiscoResponse("Sé", 0, 0, 5, false, 0, 10, 20, "2026-10");
        assertEquals("Sé: risco muito alto. 10 roubos e 20 furtos registrados pela SSP em 10/2026."
                + " Evite exibir o celular e prefira vias movimentadas e iluminadas.", AreaRiscoService.mensagem(a));
    }

    @Test void semRegistros() {
        var a = new AreaRiscoResponse("Moema", 0, 0, 1, true, 0, null, null, null);
        assertEquals("Moema: região segura. Nenhum assalto reportado nos últimos 30 dias.", AreaRiscoService.mensagem(a));
    }
}
