package com.safemove.agendamento;

import com.safemove.bo.RadarBO;
import com.safemove.integracao.OverpassClient;
import com.safemove.integracao.RadarOsm;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Uma vez por dia busca os radares do OpenStreetMap e troca os guardados (radares mudam pouco).
 * Se a Overpass falhar ou responder vazio, mantém os antigos.
 */
@Component
public class RadarSincronizador {

    private static final Logger log = LoggerFactory.getLogger(RadarSincronizador.class);

    private final OverpassClient overpass;
    private final RadarBO service;

    public RadarSincronizador(OverpassClient overpass, RadarBO service) {
        this.overpass = overpass;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${safemove.radares.intervalo-ms:86400000}",
            initialDelayString = "${safemove.radares.atraso-inicial-ms:20000}")
    public void sincronizar() {
        List<RadarOsm> novos;
        try {
            novos = overpass.buscarRadares();
        } catch (RuntimeException e) {
            log.warn("Overpass falhou; mantendo os radares anteriores: {}", e.getMessage());
            return;
        }
        if (novos.isEmpty() && service.total() > 0) {
            log.warn("Overpass não devolveu radares; mantendo os anteriores");
            return;
        }
        try {
            service.substituir(novos);
            log.info("Radares sincronizados: {}", novos.size());
        } catch (RuntimeException e) {
            log.error("Falha ao gravar radares; mantendo os anteriores", e);
        }
    }
}
