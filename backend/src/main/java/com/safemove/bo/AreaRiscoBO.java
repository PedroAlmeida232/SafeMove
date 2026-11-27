package com.safemove.bo;

import com.safemove.config.Fuso;
import com.safemove.dao.OcorrenciaDAO;
import com.safemove.dto.AreaRiscoResponse;
import com.safemove.geo.Coordenada;
import com.safemove.geo.Distrito;
import com.safemove.geo.Geo;
import com.safemove.geo.Retangulo;
import com.safemove.model.Ocorrencia;
import com.safemove.model.TipoOcorrencia;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Áreas de risco calculadas pelas próprias ocorrências do SafeMove: cada ocorrência dos últimos 30 dias conta
 * para o distrito mais próximo (até 1,5 km do centro); a soma dos pesos vira um nível de 1 a 5.
 * Não usa estatística oficial: o nível reflete só o que a comunidade registrou no app.
 */
@Service
public class AreaRiscoBO {

    static final int DIAS = 30;
    static final double RAIO_DISTRITO_METROS = 1_500;

    /** Peso de cada tipo no risco. Assalto pesa mais; trânsito não é risco à segurança. */
    static final Map<TipoOcorrencia, Double> PESO = Map.of(
            TipoOcorrencia.ASSALTO, 3.0,
            TipoOcorrencia.ACIDENTE, 1.0,
            TipoOcorrencia.ILUMINACAO, 1.0,
            TipoOcorrencia.ALAGAMENTO, 1.0,
            TipoOcorrencia.BURACO, 0.5,
            TipoOcorrencia.TRANSITO, 0.0);

    /** Pontos mínimos para cada nível (2 a 5). Abaixo de 1 ponto o nível é 1. */
    static final double[] LIMITES = {1, 3, 6, 10};

    /** Raio (m) que o app desenha para cada nível; usado para saber se um ponto está "dentro" da área. */
    public static double raioDaArea(int nivel) {
        return nivel >= 5 ? 1_100 : nivel == 4 ? 900 : 700;
    }

    private final OcorrenciaDAO ocorrencias;
    private final long cacheMs;
    private volatile List<AreaRiscoResponse> cache;
    private volatile Instant calculadoEm = Instant.EPOCH;

    public AreaRiscoBO(OcorrenciaDAO ocorrencias, @Value("${safemove.areas-risco.cache-ms:300000}") long cacheMs) {
        this.ocorrencias = ocorrencias;
        this.cacheMs = cacheMs;
    }

    /** Todos os distritos com o nível atual, do maior risco ao menor. Recalcula no máximo a cada 5 minutos. */
    public List<AreaRiscoResponse> listar() {
        List<AreaRiscoResponse> atual = cache;
        if (atual != null && Instant.now().isBefore(calculadoEm.plusMillis(cacheMs))) {
            return atual;
        }
        Retangulo sp = Geo.GRANDE_SAO_PAULO;
        List<Ocorrencia> recentes = ocorrencias.findByLatitudeBetweenAndLongitudeBetweenAndCriadoEmGreaterThanEqual(
                sp.minLatitude(), sp.maxLatitude(), sp.minLongitude(), sp.maxLongitude(), Fuso.agora().minusDays(DIAS));
        atual = calcular(recentes);
        cache = atual;
        calculadoEm = Instant.now();
        return atual;
    }

    static List<AreaRiscoResponse> calcular(List<Ocorrencia> recentes) {
        List<Distrito> distritos = Distrito.SAO_PAULO;
        double[] pontos = new double[distritos.size()];
        int[] quantidade = new int[distritos.size()];
        // Cada ocorrência conta só para o distrito mais próximo (se estiver a até 1,5 km do centro dele).
        for (Ocorrencia o : recentes) {
            Coordenada local = new Coordenada(o.getLatitude(), o.getLongitude());
            int maisPerto = -1;
            double menor = RAIO_DISTRITO_METROS;
            for (int i = 0; i < distritos.size(); i++) {
                double d = Geo.distanciaMetros(distritos.get(i).centro(), local);
                if (d <= menor) {
                    menor = d;
                    maisPerto = i;
                }
            }
            if (maisPerto >= 0) {
                pontos[maisPerto] += PESO.getOrDefault(o.getTipo(), 0.0);
                quantidade[maisPerto]++;
            }
        }
        List<AreaRiscoResponse> lista = new ArrayList<>();
        for (int i = 0; i < distritos.size(); i++) {
            Distrito d = distritos.get(i);
            lista.add(new AreaRiscoResponse(d.nome(), d.centro().latitude(), d.centro().longitude(),
                    nivel(pontos[i]), quantidade[i]));
        }
        lista.sort(Comparator.comparingInt(AreaRiscoResponse::nivel).reversed()
                .thenComparing(AreaRiscoResponse::distrito));
        return lista;
    }

    static int nivel(double pontos) {
        int nivel = 1;
        for (double limite : LIMITES) {
            if (pontos >= limite) nivel++;
        }
        return nivel;
    }
}
