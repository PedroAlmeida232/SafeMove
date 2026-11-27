package com.safemove.bo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safemove.dto.AreaRiscoResponse;
import com.safemove.geo.Coordenada;
import com.safemove.geo.Geo;
import com.safemove.integracao.RotaTomTom;
import com.safemove.model.CategoriaIncidente;
import com.safemove.model.Classificacao;
import com.safemove.model.Incidente;
import com.safemove.model.Ocorrencia;
import com.safemove.model.Radar;
import com.safemove.model.TipoOcorrencia;
import com.safemove.model.Usuario;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regras da nota de segurança (sem Spring: só a lógica de RotaBO e AreaRiscoBO). */
class RotaBOTest {

    private static final Usuario ANA = new Usuario("u1", "Ana", "ana@x.com");

    /** Traçado reto de ~3 km com 1.001 pontos. */
    private static List<Coordenada> tracado() {
        List<Coordenada> t = new ArrayList<>();
        for (int i = 0; i <= 1000; i++) {
            t.add(new Coordenada(-23.5560 - 0.00002 * i, -46.6600 + 0.00003 * i));
        }
        return t;
    }

    private static Ocorrencia ocorrencia(TipoOcorrencia tipo, Coordenada c) {
        return new Ocorrencia(ANA, tipo, null, c.latitude(), c.longitude());
    }

    private static final List<AreaRiscoResponse> SEM_RISCO = AreaRiscoBO.calcular(List.of());

    @Test void distanciaAteRota() {
        List<Coordenada> t = tracado();
        assertTrue(Geo.distanciaAteRota(t.get(500), t) < 1);
        double d = Geo.distanciaAteRota(new Coordenada(t.get(500).latitude() + 0.003, t.get(500).longitude()), t);
        assertTrue(d > 200 && d < 340, "distância: " + d);
    }

    @Test void rotaLimpaTemNota100() {
        var r = RotaBO.avaliar(new RotaTomTom(0, 3000, 600, 0, tracado()),
                new RotaBO.Arredores(List.of(), List.of(), List.of(), SEM_RISCO));
        assertEquals(100, r.nota());
        assertEquals(Classificacao.SEGURA, r.classificacao());
        assertTrue(r.alertas().isEmpty());
    }

    @Test void cadaItemNoCaminhoTiraPontos() {
        List<Coordenada> t = tracado();
        var arredores = new RotaBO.Arredores(
                List.of(new Incidente("x", CategoriaIncidente.ACIDENTE, 3, "Av. Paulista", null, 300,
                                t.get(400).latitude(), t.get(400).longitude()),
                        new Incidente("y", CategoriaIncidente.CONGESTIONAMENTO, 1, null, null, 60,
                                t.get(800).latitude(), t.get(800).longitude())),
                List.of(ocorrencia(TipoOcorrencia.ASSALTO, t.get(100)),
                        ocorrencia(TipoOcorrencia.ASSALTO, new Coordenada(t.get(700).latitude() + 0.0005, t.get(700).longitude())),
                        ocorrencia(TipoOcorrencia.BURACO, t.get(300)),
                        ocorrencia(TipoOcorrencia.ASSALTO, new Coordenada(-23.70, -46.70))), // longe da rota
                List.of(new Radar(1L, t.get(200).latitude(), t.get(200).longitude(), 50),
                        new Radar(2L, -23.6, -46.7, 60)),
                SEM_RISCO);

        var r = RotaBO.avaliar(new RotaTomTom(1, 3000, 900, 600, t), arredores);

        assertEquals(1, r.resumo().incidentesGraves());
        assertEquals(1, r.resumo().incidentesLeves());
        assertEquals(3, r.resumo().ocorrenciasRecentes());
        assertEquals(2, r.resumo().assaltosRecentes());
        assertEquals(1, r.resumo().radares());
        // 100 - 12 (grave) - 4 (leve) - 2×12 (assaltos) - 3 (buraco) - 10 (atraso de 10 min) = 47
        assertEquals(47, r.nota());
        assertEquals(Classificacao.ATENCAO, r.classificacao());
        assertEquals("2 assaltos registrados no trajeto nas últimas 24 h", r.alertas().get(0));
        assertTrue(r.alertas().contains("Acidente em Av. Paulista"));
        assertTrue(r.alertas().size() <= RotaBO.MAX_ALERTAS);
    }

    @Test void areaDeRiscoAltoEntraNaNotaENosAlertas() {
        List<Ocorrencia> naSe = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            naSe.add(ocorrencia(TipoOcorrencia.ASSALTO, new Coordenada(-23.5505 + 0.0005 * i, -46.6333)));
        }
        List<AreaRiscoResponse> areas = AreaRiscoBO.calcular(naSe);
        List<Coordenada> pelaSe = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            pelaSe.add(new Coordenada(-23.5505, -46.6433 + 0.0002 * i));
        }

        var r = RotaBO.avaliar(new RotaTomTom(0, 2000, 500, 0, pelaSe),
                new RotaBO.Arredores(List.of(), List.of(), List.of(), areas));

        assertTrue(r.resumo().riscoMedioRegiao() > 1);
        assertTrue(r.nota() < 100);
        assertTrue(r.alertas().contains("Trecho em área de risco alto (Sé)"), r.alertas().toString());
    }

    @Test void nivelDaAreaPorPontos() {
        assertEquals(1, AreaRiscoBO.nivel(0));
        assertEquals(2, AreaRiscoBO.nivel(1));
        assertEquals(3, AreaRiscoBO.nivel(3));
        assertEquals(4, AreaRiscoBO.nivel(6));
        assertEquals(5, AreaRiscoBO.nivel(10));
    }

    @Test void ocorrenciaContaSoParaODistritoMaisProximo() {
        var areas = AreaRiscoBO.calcular(List.of(ocorrencia(TipoOcorrencia.ASSALTO, new Coordenada(-23.5505, -46.6333))));
        assertEquals("Sé", areas.get(0).distrito());
        assertEquals(3, areas.get(0).nivel());
        assertEquals(1, areas.get(1).nivel());
    }

    @Test void classificacaoPelaNota() {
        assertEquals(Classificacao.SEGURA, Classificacao.daNota(70));
        assertEquals(Classificacao.ATENCAO, Classificacao.daNota(69));
        assertEquals(Classificacao.ATENCAO, Classificacao.daNota(45));
        assertEquals(Classificacao.EVITE, Classificacao.daNota(44));
    }

    @Test void simplificarLimitaOsPontosEMantemOUltimo() {
        List<Coordenada> t = tracado();
        var pontos = RotaBO.simplificar(t);
        assertTrue(pontos.size() <= RotaBO.MAX_PONTOS + 1);
        assertEquals(t.get(t.size() - 1).latitude(), (double) pontos.get(pontos.size() - 1).latitude());
        assertEquals(3, RotaBO.simplificar(t.subList(0, 3)).size());
    }
}
