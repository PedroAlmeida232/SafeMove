package com.safemove.incidente;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** A cada 2 minutos busca os incidentes na TomTom e troca os guardados. Se a TomTom falhar, mantém os antigos. */
@Component
public class IncidenteSincronizador {

    private static final Logger log = LoggerFactory.getLogger(IncidenteSincronizador.class);

    private final TomTomClient tomTom;
    private final IncidenteService service;

    public IncidenteSincronizador(TomTomClient tomTom, IncidenteService service) {
        this.tomTom = tomTom;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${safemove.incidentes.intervalo-ms:120000}",
            initialDelayString = "${safemove.incidentes.atraso-inicial-ms:5000}")
    public void sincronizar() {
        List<IncidenteTomTom> novos;
        try {
            novos = tomTom.buscarIncidentes(); // fora da transação: nada é apagado antes de a TomTom responder
        } catch (RuntimeException e) {
            log.warn("TomTom falhou; mantendo os incidentes anteriores: {}", e.getMessage());
            return;
        }
        try {
            service.substituir(novos);
            log.info("Incidentes sincronizados: {}", novos.size());
        } catch (RuntimeException e) {
            log.error("Falha ao gravar incidentes; mantendo os anteriores", e);
        }
    }
}
