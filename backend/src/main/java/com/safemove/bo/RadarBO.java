package com.safemove.bo;

import com.safemove.dao.RadarDAO;
import com.safemove.dto.RadarResponse;
import com.safemove.exception.ApiException;
import com.safemove.geo.Coordenada;
import com.safemove.geo.Geo;
import com.safemove.geo.Retangulo;
import com.safemove.integracao.RadarOsm;
import com.safemove.model.Radar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RadarBO {

    static final double RAIO_PADRAO_METROS = 2_000;

    private final RadarDAO dao;

    public RadarBO(RadarDAO dao) {
        this.dao = dao;
    }

    /**
     * Com lat e lng: radares num raio (metros, padrão 2.000, máximo 50.000), do mais perto ao mais longe.
     * Sem lat e lng: todos os radares.
     */
    public List<RadarResponse> listar(Double lat, Double lng, Double raio) {
        if ((lat == null) != (lng == null)) {
            throw ApiException.requisicaoInvalida("Informe lat e lng juntos.");
        }
        if (lat == null) {
            if (raio != null) {
                throw ApiException.requisicaoInvalida("O raio só vale junto com lat e lng.");
            }
            return dao.findAll().stream().map(r -> RadarResponse.de(r, null)).toList();
        }

        Coordenada centro = Busca.centro(lat, lng);
        double raioMetros = Busca.raio(raio, RAIO_PADRAO_METROS);
        Retangulo caixa = Geo.retanguloEmVolta(centro, raioMetros);
        return dao.findByLatitudeBetweenAndLongitudeBetween(caixa.minLatitude(), caixa.maxLatitude(),
                        caixa.minLongitude(), caixa.maxLongitude()).stream()
                .map(r -> RadarResponse.de(r, Geo.distanciaMetros(centro, new Coordenada(r.getLatitude(), r.getLongitude()))))
                .filter(r -> r.distanciaMetros() <= raioMetros)
                .sorted(Comparator.comparingDouble(RadarResponse::distanciaMetros))
                .toList();
    }

    /** Troca os radares guardados pelos recebidos do OSM, numa transação (mesma ideia dos incidentes). */
    @Transactional
    public void substituir(List<RadarOsm> novos) {
        Map<Long, Radar> antigos = new HashMap<>();
        dao.findAll().forEach(r -> antigos.put(r.getIdOsm(), r));

        for (RadarOsm n : novos) {
            Radar existente = antigos.remove(n.idOsm());
            if (existente != null) {
                existente.atualizar(n.latitude(), n.longitude(), n.velocidadeMax());
            } else {
                dao.save(new Radar(n.idOsm(), n.latitude(), n.longitude(), n.velocidadeMax()));
            }
        }
        dao.deleteAll(antigos.values()); // o que sobrou foi removido do OSM
    }

    public long total() {
        return dao.count();
    }
}
