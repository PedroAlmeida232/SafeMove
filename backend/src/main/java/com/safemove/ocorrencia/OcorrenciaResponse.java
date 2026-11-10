package com.safemove.ocorrencia;

import com.safemove.config.Fuso;
import java.time.OffsetDateTime;

public record OcorrenciaResponse(Long id, TipoOcorrencia tipo, String descricao, double latitude, double longitude,
                                 OffsetDateTime criadoEm, int confirmacoes) {

    public static OcorrenciaResponse de(Ocorrencia o) {
        return new OcorrenciaResponse(o.getId(), o.getTipo(), o.getDescricao(), o.getLatitude(), o.getLongitude(),
                Fuso.emSaoPaulo(o.getCriadoEm()), o.getConfirmacoes());
    }
}
