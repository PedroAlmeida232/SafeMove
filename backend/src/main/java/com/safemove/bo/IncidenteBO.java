package com.safemove.bo;

import com.safemove.dao.IncidenteDAO;
import com.safemove.dto.IncidenteResponse;
import com.safemove.exception.ApiException;
import com.safemove.geo.Coordenada;
import com.safemove.geo.Geo;
import com.safemove.geo.Retangulo;
import com.safemove.integracao.IncidenteTomTom;
import com.safemove.model.CategoriaIncidente;
import com.safemove.model.Incidente;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidenteBO {

    static final double RAIO_PADRAO_METROS = 5_000;

    private final IncidenteDAO dao;

    public IncidenteBO(IncidenteDAO dao) {
        this.dao = dao;
    }

    /**
     * Com lat e lng: incidentes num raio (metros, padrão 5.000, máximo 50.000), do mais perto ao mais longe.
     * Sem lat e lng: todos os incidentes. Em qualquer caso, categoria filtra por tipo.
     */
    public List<IncidenteResponse> listar(Double lat, Double lng, Double raio, CategoriaIncidente categoria) {
        if ((lat == null) != (lng == null)) {
            throw ApiException.requisicaoInvalida("Informe lat e lng juntos.");
        }
        if (lat == null) {
            if (raio != null) {
                throw ApiException.requisicaoInvalida("O raio só vale junto com lat e lng.");
            }
            return dao.findAll().stream()
                    .filter(i -> categoria == null || i.getCategoria() == categoria)
                    .map(i -> IncidenteResponse.de(i, null))
                    .toList();
        }

        Coordenada centro = Busca.centro(lat, lng);
        double raioMetros = Busca.raio(raio, RAIO_PADRAO_METROS);
        Retangulo caixa = Geo.retanguloEmVolta(centro, raioMetros);
        return dao.findByLatitudeBetweenAndLongitudeBetween(caixa.minLatitude(), caixa.maxLatitude(),
                        caixa.minLongitude(), caixa.maxLongitude()).stream()
                .filter(i -> categoria == null || i.getCategoria() == categoria)
                .map(i -> IncidenteResponse.de(i, Geo.distanciaMetros(centro, new Coordenada(i.getLatitude(), i.getLongitude()))))
                .filter(r -> r.distanciaMetros() <= raioMetros)
                .sorted(Comparator.comparingDouble(IncidenteResponse::distanciaMetros))
                .toList();
    }

    public IncidenteResponse buscar(Long id) {
        return dao.findById(id).map(i -> IncidenteResponse.de(i, null))
                .orElseThrow(() -> ApiException.naoEncontrado("Incidente não encontrado."));
    }

    /**
     * Troca os incidentes guardados pelos recebidos, tudo em uma transação: atualiza os que continuam,
     * insere os novos e apaga os que a TomTom não devolveu mais. Se algo falhar, o banco fica como estava.
     */
    @Transactional
    public void substituir(List<IncidenteTomTom> novos) {
        Map<String, Incidente> antigos = new HashMap<>();
        dao.findAll().forEach(i -> antigos.put(i.getIdTomtom(), i));

        for (IncidenteTomTom n : novos) {
            Incidente existente = antigos.remove(n.idTomtom());
            if (existente != null) {
                existente.atualizar(n.categoria(), n.gravidade(), n.via(), n.descricao(),
                        n.atrasoSegundos(), n.latitude(), n.longitude());
            } else {
                dao.save(new Incidente(n.idTomtom(), n.categoria(), n.gravidade(), n.via(), n.descricao(),
                        n.atrasoSegundos(), n.latitude(), n.longitude()));
            }
        }
        dao.deleteAll(antigos.values()); // o que sobrou não veio mais da TomTom
    }
}
