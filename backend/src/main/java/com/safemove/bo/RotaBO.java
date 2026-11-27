package com.safemove.bo;

import com.safemove.config.Fuso;
import com.safemove.dao.IncidenteDAO;
import com.safemove.dao.OcorrenciaDAO;
import com.safemove.dao.RadarDAO;
import com.safemove.dto.AreaRiscoResponse;
import com.safemove.dto.RotaDtos.AvaliarRotaRequest;
import com.safemove.dto.RotaDtos.Ponto;
import com.safemove.dto.RotaDtos.ResumoRota;
import com.safemove.dto.RotaDtos.RotaAvaliada;
import com.safemove.exception.ApiException;
import com.safemove.geo.Coordenada;
import com.safemove.geo.Geo;
import com.safemove.geo.Retangulo;
import com.safemove.integracao.RotaTomTom;
import com.safemove.integracao.TomTomRotasClient;
import com.safemove.model.CategoriaIncidente;
import com.safemove.model.Classificacao;
import com.safemove.model.Incidente;
import com.safemove.model.ModoTransporte;
import com.safemove.model.Ocorrencia;
import com.safemove.model.Radar;
import com.safemove.model.TipoOcorrencia;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Avalia a segurança de cada rota sugerida pela TomTom e dá uma nota de 0 a 100.
 *
 * <p>Ao longo do traçado conta: incidentes de trânsito (graves = gravidade 3 ou 4), ocorrências das
 * últimas 24 h (assaltos à parte), radares e o nível médio das áreas de risco atravessadas.
 * Cada item tira pontos da nota; o atraso por trânsito tira até 10 pontos.
 */
@Service
public class RotaBO {

    static final double DISTANCIA_INCIDENTE_M = 150;
    static final double DISTANCIA_OCORRENCIA_M = 200;
    static final double DISTANCIA_RADAR_M = 40;
    static final int HORAS_OCORRENCIAS = 24;
    static final int GRAVIDADE_GRAVE = 3;
    static final double DISTANCIA_MINIMA_M = 50;
    /** A cada quantos metros o traçado é amostrado para calcular o risco médio da região. */
    static final double PASSO_AMOSTRA_M = 200;
    /** Máximo de pontos devolvidos por rota (o traçado é simplificado para o app). */
    static final int MAX_PONTOS = 400;
    static final int MAX_ALERTAS = 4;

    static final double PENALIDADE_INCIDENTE_GRAVE = 12;
    static final double PENALIDADE_INCIDENTE_LEVE = 4;
    static final double PENALIDADE_ASSALTO = 12;
    static final double PENALIDADE_OCORRENCIA = 3;
    static final double PENALIDADE_POR_NIVEL_DE_RISCO = 8;
    static final double PENALIDADE_ATRASO_MAXIMA = 10;

    private final TomTomRotasClient tomTom;
    private final IncidenteDAO incidentes;
    private final OcorrenciaDAO ocorrencias;
    private final RadarDAO radares;
    private final AreaRiscoBO areas;
    /** Por quanto tempo uma resposta da TomTom é reaproveitada (o app reavalia a rota ativa a cada minuto). */
    private final long cacheMs;
    private final Map<String, Guardado> cache = new ConcurrentHashMap<>();

    private record Guardado(Instant em, List<RotaTomTom> rotas) {}

    /** Tudo o que existe perto das rotas, buscado uma vez só para todas as alternativas. */
    record Arredores(List<Incidente> incidentes, List<Ocorrencia> ocorrencias, List<Radar> radares,
                     List<AreaRiscoResponse> areas) {}

    public RotaBO(TomTomRotasClient tomTom, IncidenteDAO incidentes, OcorrenciaDAO ocorrencias, RadarDAO radares,
                  AreaRiscoBO areas, @Value("${safemove.rotas.cache-ms:90000}") long cacheMs) {
        this.tomTom = tomTom;
        this.cacheMs = cacheMs;
        this.incidentes = incidentes;
        this.ocorrencias = ocorrencias;
        this.radares = radares;
        this.areas = areas;
    }

    /** Rotas da mais bem avaliada para a pior. */
    public List<RotaAvaliada> avaliar(AvaliarRotaRequest req) {
        Coordenada origem = Busca.centro(req.origem().latitude(), req.origem().longitude());
        Coordenada destino = Busca.centro(req.destino().latitude(), req.destino().longitude());
        if (!Geo.GRANDE_SAO_PAULO.contem(origem) || !Geo.GRANDE_SAO_PAULO.contem(destino)) {
            throw ApiException.naoProcessavel("O SafeMove só avalia rotas na Grande São Paulo.");
        }
        if (Geo.distanciaMetros(origem, destino) < DISTANCIA_MINIMA_M) {
            throw ApiException.naoProcessavel("Origem e destino estão muito próximos.");
        }
        ModoTransporte modo = req.modo() == null ? ModoTransporte.CARRO : req.modo();

        List<RotaTomTom> rotas = rotasDaTomTom(origem, destino, modo);
        if (rotas.isEmpty()) {
            throw ApiException.naoProcessavel("Não encontramos um caminho entre esses pontos.");
        }

        Retangulo caixa = caixaEmVolta(rotas, DISTANCIA_OCORRENCIA_M);
        Arredores arredores = new Arredores(
                incidentes.findByLatitudeBetweenAndLongitudeBetween(caixa.minLatitude(), caixa.maxLatitude(),
                        caixa.minLongitude(), caixa.maxLongitude()),
                ocorrencias.findByLatitudeBetweenAndLongitudeBetweenAndCriadoEmGreaterThanEqual(caixa.minLatitude(),
                        caixa.maxLatitude(), caixa.minLongitude(), caixa.maxLongitude(),
                        Fuso.agora().minusHours(HORAS_OCORRENCIAS)),
                radares.findByLatitudeBetweenAndLongitudeBetween(caixa.minLatitude(), caixa.maxLatitude(),
                        caixa.minLongitude(), caixa.maxLongitude()),
                areas.listar());

        return rotas.stream()
                .map(r -> avaliar(r, arredores))
                .sorted(Comparator.comparingInt(RotaAvaliada::nota).reversed()
                        .thenComparingInt(RotaAvaliada::duracaoSegundos))
                .toList();
    }

    private List<RotaTomTom> rotasDaTomTom(Coordenada origem, Coordenada destino, ModoTransporte modo) {
        String chave = "%.4f,%.4f:%.4f,%.4f:%s".formatted(origem.latitude(), origem.longitude(),
                destino.latitude(), destino.longitude(), modo);
        Instant agora = Instant.now();
        Guardado g = cache.get(chave);
        if (g != null && agora.isBefore(g.em().plusMillis(cacheMs))) {
            return g.rotas();
        }
        List<RotaTomTom> rotas = tomTom.calcularRotas(origem, destino, modo);
        if (cache.size() > 500) {
            cache.clear(); // evita crescer sem limite
        }
        cache.put(chave, new Guardado(agora, rotas));
        return rotas;
    }

    static RotaAvaliada avaliar(RotaTomTom rota, Arredores a) {
        List<Coordenada> tracado = rota.pontos();

        List<Incidente> noCaminho = a.incidentes().stream()
                .filter(i -> perto(i.getLatitude(), i.getLongitude(), tracado, DISTANCIA_INCIDENTE_M))
                .sorted(Comparator.comparingInt(Incidente::getGravidade).reversed())
                .toList();
        List<Incidente> graves = noCaminho.stream().filter(i -> i.getGravidade() >= GRAVIDADE_GRAVE).toList();
        int leves = noCaminho.size() - graves.size();

        List<Ocorrencia> ocorrenciasNoCaminho = a.ocorrencias().stream()
                .filter(o -> perto(o.getLatitude(), o.getLongitude(), tracado, DISTANCIA_OCORRENCIA_M))
                .toList();
        int assaltos = (int) ocorrenciasNoCaminho.stream().filter(o -> o.getTipo() == TipoOcorrencia.ASSALTO).count();
        int outras = ocorrenciasNoCaminho.size() - assaltos;

        int radaresNoCaminho = (int) a.radares().stream()
                .filter(r -> perto(r.getLatitude(), r.getLongitude(), tracado, DISTANCIA_RADAR_M))
                .count();

        Risco risco = risco(tracado, a.areas());

        double penalidade = graves.size() * PENALIDADE_INCIDENTE_GRAVE
                + leves * PENALIDADE_INCIDENTE_LEVE
                + assaltos * PENALIDADE_ASSALTO
                + outras * PENALIDADE_OCORRENCIA
                + (risco.medio() - 1) * PENALIDADE_POR_NIVEL_DE_RISCO
                + Math.min(PENALIDADE_ATRASO_MAXIMA, rota.atrasoSegundos() / 60.0);
        int nota = (int) Math.max(0, Math.min(100, Math.round(100 - penalidade)));

        ResumoRota resumo = new ResumoRota(graves.size(), leves, ocorrenciasNoCaminho.size(), assaltos,
                Math.round(risco.medio() * 10) / 10.0, radaresNoCaminho);

        return new RotaAvaliada(rota.indice(), nota, Classificacao.daNota(nota), rota.distanciaMetros(),
                rota.duracaoSegundos(), rota.atrasoSegundos(), resumo,
                alertas(graves, ocorrenciasNoCaminho, assaltos, risco, rota.atrasoSegundos()),
                simplificar(tracado));
    }

    private static boolean perto(double lat, double lng, List<Coordenada> tracado, double limite) {
        return Geo.distanciaAteRota(new Coordenada(lat, lng), tracado) <= limite;
    }

    /** Nível médio das áreas de risco ao longo da rota (1 a 5) e a área mais perigosa atravessada. */
    record Risco(double medio, AreaRiscoResponse pior) {}

    static Risco risco(List<Coordenada> tracado, List<AreaRiscoResponse> areas) {
        List<AreaRiscoResponse> relevantes = areas.stream().filter(a -> a.nivel() > 1).toList();
        List<Coordenada> amostras = amostrar(tracado, PASSO_AMOSTRA_M);
        double soma = 0;
        AreaRiscoResponse pior = null;
        double distanciaDoPior = Double.POSITIVE_INFINITY;
        for (Coordenada p : amostras) {
            // Em cada ponto vale a área de maior nível que o contém; no empate, a mais próxima.
            AreaRiscoResponse aqui = null;
            double distanciaAqui = Double.POSITIVE_INFINITY;
            for (AreaRiscoResponse area : relevantes) {
                double d = Geo.distanciaMetros(p, new Coordenada(area.latitude(), area.longitude()));
                if (d > AreaRiscoBO.raioDaArea(area.nivel())) continue;
                if (aqui == null || area.nivel() > aqui.nivel() || (area.nivel() == aqui.nivel() && d < distanciaAqui)) {
                    aqui = area;
                    distanciaAqui = d;
                }
            }
            soma += aqui == null ? 1 : aqui.nivel();
            if (aqui != null && (pior == null || aqui.nivel() > pior.nivel()
                    || (aqui.nivel() == pior.nivel() && distanciaAqui < distanciaDoPior))) {
                pior = aqui;
                distanciaDoPior = distanciaAqui;
            }
        }
        return new Risco(amostras.isEmpty() ? 1 : soma / amostras.size(), pior);
    }

    /** Pontos a cada {@code passo} metros ao longo do traçado (sempre inclui o primeiro e o último). */
    static List<Coordenada> amostrar(List<Coordenada> tracado, double passo) {
        List<Coordenada> amostras = new ArrayList<>();
        if (tracado.isEmpty()) {
            return amostras;
        }
        amostras.add(tracado.get(0));
        double acumulado = 0;
        for (int i = 1; i < tracado.size(); i++) {
            acumulado += Geo.distanciaMetros(tracado.get(i - 1), tracado.get(i));
            if (acumulado >= passo) {
                amostras.add(tracado.get(i));
                acumulado = 0;
            }
        }
        Coordenada ultimo = tracado.get(tracado.size() - 1);
        if (amostras.get(amostras.size() - 1) != ultimo) {
            amostras.add(ultimo);
        }
        return amostras;
    }

    static List<String> alertas(List<Incidente> graves, List<Ocorrencia> ocorrencias, int assaltos, Risco risco,
                                int atrasoSegundos) {
        List<String> alertas = new ArrayList<>();
        if (assaltos > 0) {
            alertas.add(assaltos == 1 ? "1 assalto registrado no trajeto nas últimas 24 h"
                    : assaltos + " assaltos registrados no trajeto nas últimas 24 h");
        }
        graves.stream().limit(2).forEach(i -> alertas.add(i.getVia() == null
                ? rotulo(i.getCategoria()) + " no trajeto"
                : rotulo(i.getCategoria()) + " em " + i.getVia()));
        if (risco.pior() != null && risco.pior().nivel() >= 4) {
            alertas.add("Trecho em área de risco alto (" + risco.pior().distrito() + ")");
        }
        if (ocorrencias.stream().anyMatch(o -> o.getTipo() == TipoOcorrencia.ILUMINACAO)) {
            alertas.add("Trecho com falta de iluminação pública");
        }
        if (ocorrencias.stream().anyMatch(o -> o.getTipo() == TipoOcorrencia.ALAGAMENTO)) {
            alertas.add("Alagamento registrado no trajeto");
        }
        int outras = ocorrencias.size() - assaltos;
        if (outras > 0 && alertas.size() < MAX_ALERTAS) {
            alertas.add(outras == 1 ? "1 ocorrência registrada no trajeto nas últimas 24 h"
                    : outras + " ocorrências registradas no trajeto nas últimas 24 h");
        }
        if (atrasoSegundos >= 300) {
            alertas.add("Trânsito: +" + Math.round(atrasoSegundos / 60.0) + " min de atraso");
        }
        return alertas.size() > MAX_ALERTAS ? List.copyOf(alertas.subList(0, MAX_ALERTAS)) : alertas;
    }

    static String rotulo(CategoriaIncidente c) {
        return switch (c) {
            case ACIDENTE -> "Acidente";
            case CONGESTIONAMENTO -> "Congestionamento";
            case VIA_INTERDITADA -> "Via interditada";
            case FAIXA_INTERDITADA -> "Faixa interditada";
            case OBRAS -> "Obras";
            case ALAGAMENTO -> "Alagamento";
            case PERIGO -> "Perigo na via";
            case VEICULO_QUEBRADO -> "Veículo quebrado";
            case CHUVA -> "Chuva forte";
            case NEBLINA -> "Neblina";
            case VENTO -> "Vento forte";
            case GELO -> "Gelo na pista";
            case DESCONHECIDO -> "Incidente";
        };
    }

    /** Reduz o traçado a no máximo MAX_PONTOS, mantendo o primeiro e o último ponto. */
    static List<Ponto> simplificar(List<Coordenada> tracado) {
        int passo = Math.max(1, (int) Math.ceil(tracado.size() / (double) MAX_PONTOS));
        List<Ponto> pontos = new ArrayList<>();
        for (int i = 0; i < tracado.size(); i += passo) {
            pontos.add(ponto(tracado.get(i)));
        }
        if ((tracado.size() - 1) % passo != 0) {
            pontos.add(ponto(tracado.get(tracado.size() - 1)));
        }
        return pontos;
    }

    private static Ponto ponto(Coordenada c) {
        return new Ponto(c.latitude(), c.longitude());
    }

    private static Retangulo caixaEmVolta(List<RotaTomTom> rotas, double margemMetros) {
        double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
        for (RotaTomTom r : rotas) {
            for (Coordenada c : r.pontos()) {
                minLat = Math.min(minLat, c.latitude());
                maxLat = Math.max(maxLat, c.latitude());
                minLng = Math.min(minLng, c.longitude());
                maxLng = Math.max(maxLng, c.longitude());
            }
        }
        Retangulo sul = Geo.retanguloEmVolta(new Coordenada(minLat, minLng), margemMetros);
        Retangulo norte = Geo.retanguloEmVolta(new Coordenada(maxLat, maxLng), margemMetros);
        return new Retangulo(sul.minLatitude(), norte.maxLatitude(), sul.minLongitude(), norte.maxLongitude());
    }
}
