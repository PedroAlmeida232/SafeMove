package com.safemove.incidente;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidenteService {

    private final IncidenteRepository repo;

    public IncidenteService(IncidenteRepository repo) {
        this.repo = repo;
    }

    /**
     * Troca os incidentes guardados pelos recebidos, tudo em uma transação: atualiza os que continuam,
     * insere os novos e apaga os que a TomTom não devolveu mais. Se algo falhar, o banco fica como estava.
     */
    @Transactional
    public void substituir(List<IncidenteTomTom> novos) {
        Map<String, Incidente> antigos = new HashMap<>();
        repo.findAll().forEach(i -> antigos.put(i.getIdTomtom(), i));

        for (IncidenteTomTom n : novos) {
            Incidente existente = antigos.remove(n.idTomtom());
            if (existente != null) {
                existente.atualizar(n.categoria(), n.gravidade(), n.via(), n.descricao(),
                        n.atrasoSegundos(), n.latitude(), n.longitude());
            } else {
                repo.save(new Incidente(n.idTomtom(), n.categoria(), n.gravidade(), n.via(), n.descricao(),
                        n.atrasoSegundos(), n.latitude(), n.longitude()));
            }
        }
        repo.deleteAll(antigos.values()); // o que sobrou não veio mais da TomTom
    }
}
