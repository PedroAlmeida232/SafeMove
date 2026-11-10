package com.safemove.incidente;

import com.safemove.geo.Coordenada;
import com.safemove.geo.Geo;
import com.safemove.geo.Retangulo;
import com.safemove.web.ApiException;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/incidentes")
public class IncidenteController {

    static final double RAIO_PADRAO_METROS = 5_000;
    static final double RAIO_MAXIMO_METROS = 50_000;

    private final IncidenteRepository repo;

    public IncidenteController(IncidenteRepository repo) {
        this.repo = repo;
    }

    /**
     * Com lat e lng: incidentes num raio (metros, padrão 5.000, máximo 50.000), do mais perto ao mais longe.
     * Sem lat e lng: todos os incidentes. Em qualquer caso, categoria filtra por tipo.
     */
    @GetMapping
    public List<IncidenteResponse> listar(@RequestParam(required = false) Double lat,
                                          @RequestParam(required = false) Double lng,
                                          @RequestParam(required = false) Double raio,
                                          @RequestParam(required = false) CategoriaIncidente categoria) {
        if ((lat == null) != (lng == null)) {
            throw ApiException.requisicaoInvalida("Informe lat e lng juntos.");
        }
        if (lat == null) {
            if (raio != null) {
                throw ApiException.requisicaoInvalida("O raio só vale junto com lat e lng.");
            }
            return repo.findAll().stream()
                    .filter(i -> categoria == null || i.getCategoria() == categoria)
                    .map(i -> IncidenteResponse.de(i, null))
                    .toList();
        }

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

        Retangulo caixa = Geo.retanguloEmVolta(centro, raioMetros);
        return repo.findByLatitudeBetweenAndLongitudeBetween(caixa.minLatitude(), caixa.maxLatitude(),
                        caixa.minLongitude(), caixa.maxLongitude()).stream()
                .filter(i -> categoria == null || i.getCategoria() == categoria)
                .map(i -> new Object[] {i, Geo.distanciaMetros(centro, new Coordenada(i.getLatitude(), i.getLongitude()))})
                .filter(p -> (double) p[1] <= raioMetros)
                .sorted(Comparator.comparingDouble(p -> (double) p[1]))
                .map(p -> IncidenteResponse.de((Incidente) p[0], (double) p[1]))
                .toList();
    }
}
