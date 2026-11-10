package com.safemove.incidente;

import com.safemove.config.Fuso;
import java.time.OffsetDateTime;

/** distanciaMetros só vem preenchida quando a busca foi feita com lat/lng. */
public record IncidenteResponse(Long id, CategoriaIncidente categoria, int gravidade, String via, String descricao,
                                Integer atrasoSegundos, double latitude, double longitude,
                                Double distanciaMetros, OffsetDateTime atualizadoEm) {

    public static IncidenteResponse de(Incidente i, Double distanciaMetros) {
        return new IncidenteResponse(i.getId(), i.getCategoria(), i.getGravidade(), i.getVia(), i.getDescricao(),
                i.getAtrasoSegundos(), i.getLatitude(), i.getLongitude(), distanciaMetros,
                Fuso.emSaoPaulo(i.getAtualizadoEm()));
    }
}
