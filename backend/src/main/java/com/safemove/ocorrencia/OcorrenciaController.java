package com.safemove.ocorrencia;

import com.safemove.config.Fuso;
import com.safemove.geo.Coordenada;
import com.safemove.geo.Geo;
import com.safemove.geo.Retangulo;
import com.safemove.web.ApiException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocorrencias")
public class OcorrenciaController {

    static final double RAIO_PADRAO_METROS = 2_000;
    static final double RAIO_MAXIMO_METROS = 50_000;
    static final int HORAS_PADRAO = 24;
    static final int HORAS_MAXIMO = 24 * 30;

    private final OcorrenciaRepository repo;

    public OcorrenciaController(OcorrenciaRepository repo) {
        this.repo = repo;
    }

    /**
     * Ocorrências num raio (metros, padrão 2.000, máximo 50.000) criadas nas últimas {@code horas}
     * (padrão 24, máximo 720), das mais recentes às mais antigas. {@code tipo} filtra por tipo.
     */
    @GetMapping
    public List<OcorrenciaResponse> listar(@RequestParam Double lat,
                                           @RequestParam Double lng,
                                           @RequestParam(required = false) Double raio,
                                           @RequestParam(required = false) Integer horas,
                                           @RequestParam(required = false) TipoOcorrencia tipo) {
        Coordenada centro;
        try {
            centro = new Coordenada(lat, lng);
        } catch (IllegalArgumentException e) {
            throw ApiException.requisicaoInvalida(e.getMessage());
        }
        double raioMetros = raio == null ? RAIO_PADRAO_METROS : raio;
        if (!Double.isFinite(raioMetros) || raioMetros <= 0 || raioMetros > RAIO_MAXIMO_METROS) {
            throw ApiException.requisicaoInvalida("O raio deve estar entre 0 e 50000 metros.");
        }
        int janelaHoras = horas == null ? HORAS_PADRAO : horas;
        if (janelaHoras <= 0 || janelaHoras > HORAS_MAXIMO) {
            throw ApiException.requisicaoInvalida("As horas devem estar entre 1 e " + HORAS_MAXIMO + ".");
        }

        Retangulo caixa = Geo.retanguloEmVolta(centro, raioMetros);
        OffsetDateTime desde = Fuso.agora().minusHours(janelaHoras);
        return repo.findByLatitudeBetweenAndLongitudeBetweenAndCriadoEmGreaterThanEqual(
                        caixa.minLatitude(), caixa.maxLatitude(), caixa.minLongitude(), caixa.maxLongitude(), desde)
                .stream()
                .filter(o -> tipo == null || o.getTipo() == tipo)
                .filter(o -> Geo.distanciaMetros(centro, new Coordenada(o.getLatitude(), o.getLongitude())) <= raioMetros)
                .sorted(Comparator.comparing(Ocorrencia::getCriadoEm).reversed())
                .map(OcorrenciaResponse::de)
                .toList();
    }
}
