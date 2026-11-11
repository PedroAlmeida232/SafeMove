package com.safemove.arearisco;

import com.safemove.config.Fuso;
import com.safemove.geo.Coordenada;
import com.safemove.ocorrencia.Ocorrencia;
import com.safemove.ocorrencia.OcorrenciaRepository;
import com.safemove.ocorrencia.TipoOcorrencia;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calcula o risco de cada distrito com os assaltos reportados no app e as estatísticas da SSP. */
@Service
public class AreaRiscoService {

    private final Distritos distritos;
    private final EstatisticasOficiais oficiais;
    private final OcorrenciaRepository ocorrencias;

    public AreaRiscoService(Distritos distritos, EstatisticasOficiais oficiais, OcorrenciaRepository ocorrencias) {
        this.distritos = distritos;
        this.oficiais = oficiais;
        this.ocorrencias = ocorrencias;
    }

    /** Todos os distritos, do mais arriscado ao menos arriscado (empate: ordem alfabética). */
    @Transactional(readOnly = true)
    public List<AreaRiscoResponse> listar() {
        Map<String, Integer> assaltos = assaltosPorDistrito();
        Map<String, EstatisticaOficial> estatisticas = oficiais.doUltimoMes();
        List<Double> indices = indicesOficiais(estatisticas);
        return distritos.todos().stream()
                .map(d -> area(d, assaltos, estatisticas, indices))
                .sorted(Comparator.comparingInt(AreaRiscoResponse::nivel).reversed()
                        .thenComparing(AreaRiscoResponse::distrito))
                .toList();
    }

    /** Avalia a área onde o ponto está; vazio se o ponto não estiver perto de nenhum distrito conhecido. */
    @Transactional(readOnly = true)
    public Optional<AvaliacaoAreaResponse> avaliar(Coordenada ponto) {
        return distritos.maisProximo(ponto).map(p -> {
            Map<String, EstatisticaOficial> estatisticas = oficiais.doUltimoMes();
            AreaRiscoResponse area = area(p.distrito(), assaltosPorDistrito(), estatisticas,
                    indicesOficiais(estatisticas));
            return new AvaliacaoAreaResponse(area, p.distanciaMetros(), titulo(area), mensagem(area));
        });
    }

    /** Cada assalto dos últimos 30 dias conta só para o distrito de centroide mais próximo. */
    private Map<String, Integer> assaltosPorDistrito() {
        Map<String, Integer> contagem = new HashMap<>();
        List<Ocorrencia> recentes = ocorrencias.findByTipoAndCriadoEmGreaterThanEqual(
                TipoOcorrencia.ASSALTO, Fuso.agora().minusDays(CalculadoraRisco.DIAS));
        for (Ocorrencia o : recentes) {
            distritos.maisProximo(new Coordenada(o.getLatitude(), o.getLongitude()))
                    .ifPresent(p -> contagem.merge(p.distrito().nome(), 1, Integer::sum));
        }
        return contagem;
    }

    /** Índices só dos distritos conhecidos, para que nomes que não batem não distorçam os quintis. */
    private List<Double> indicesOficiais(Map<String, EstatisticaOficial> estatisticas) {
        return distritos.todos().stream()
                .map(d -> estatisticas.get(Nomes.chave(d.nome())))
                .filter(e -> e != null)
                .map(EstatisticaOficial::indice)
                .toList();
    }

    private static AreaRiscoResponse area(Distrito d, Map<String, Integer> assaltos,
                                          Map<String, EstatisticaOficial> estatisticas, List<Double> indices) {
        int reportados = assaltos.getOrDefault(d.nome(), 0);
        EstatisticaOficial oficial = estatisticas.get(Nomes.chave(d.nome()));
        Integer nivelOficial = oficial == null ? null : CalculadoraRisco.nivelOficial(oficial.indice(), indices);
        int nivel = CalculadoraRisco.combinar(CalculadoraRisco.nivelPorAssaltos(reportados), nivelOficial);
        return new AreaRiscoResponse(d.nome(), d.centro().latitude(), d.centro().longitude(), nivel,
                CalculadoraRisco.segura(nivel), reportados,
                oficial == null ? null : oficial.roubos(),
                oficial == null ? null : oficial.furtos(),
                oficial == null ? null : oficial.mes().toString());
    }

    static String titulo(AreaRiscoResponse a) {
        if (a.nivel() >= 4) return "Você está em uma área de risco";
        if (a.nivel() == 3) return "Atenção nesta região";
        return "Região segura";
    }

    /** Ex.: "Sé: risco alto. 7 assaltos reportados no app nos últimos 30 dias e 412 roubos e 980 furtos
     *  registrados pela SSP em 10/2026. Evite exibir o celular e prefira vias movimentadas." */
    static String mensagem(AreaRiscoResponse a) {
        String nivel = switch (a.nivel()) {
            case 5 -> "risco muito alto";
            case 4 -> "risco alto";
            case 3 -> "risco moderado";
            default -> "região segura";
        };
        StringBuilder texto = new StringBuilder(a.distrito()).append(": ").append(nivel).append(". ");

        String reportados = a.assaltosReportados() == 0 ? null
                : a.assaltosReportados() + (a.assaltosReportados() == 1 ? " assalto reportado" : " assaltos reportados")
                        + " no app nos últimos " + CalculadoraRisco.DIAS + " dias";
        String oficial = a.mesOficial() == null ? null
                : a.roubosOficiais() + " roubos e " + a.furtosOficiais() + " furtos registrados pela SSP em "
                        + a.mesOficial().substring(5) + "/" + a.mesOficial().substring(0, 4);
        if (reportados != null && oficial != null) {
            texto.append(capitalizar(reportados)).append(" e ").append(oficial).append(".");
        } else if (reportados != null || oficial != null) {
            texto.append(capitalizar(reportados != null ? reportados : oficial)).append(".");
        } else {
            texto.append("Nenhum assalto reportado nos últimos ").append(CalculadoraRisco.DIAS).append(" dias.");
        }
        if (!a.segura()) {
            texto.append(" Evite exibir o celular e prefira vias movimentadas e iluminadas.");
        }
        return texto.toString();
    }

    private static String capitalizar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
