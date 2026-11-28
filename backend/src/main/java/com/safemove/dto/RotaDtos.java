package com.safemove.dto;

import com.safemove.model.Classificacao;
import com.safemove.model.ModoTransporte;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Contrato de POST /rotas/avaliar (mesmos nomes de frontend/src/api/types.ts). */
public final class RotaDtos {

    private RotaDtos() {}

    public record Ponto(
            @NotNull(message = "Informe a latitude.")
            @DecimalMin(value = "-90", message = "Latitude inválida.")
            @DecimalMax(value = "90", message = "Latitude inválida.") Double latitude,
            @NotNull(message = "Informe a longitude.")
            @DecimalMin(value = "-180", message = "Longitude inválida.")
            @DecimalMax(value = "180", message = "Longitude inválida.") Double longitude) {}

    /** modo é opcional: sem ele, a rota é de carro. */
    public record AvaliarRotaRequest(
            @NotNull(message = "Informe a origem.") @Valid Ponto origem,
            @NotNull(message = "Informe o destino.") @Valid Ponto destino,
            ModoTransporte modo) {}

    /** Quantidades encontradas ao longo da rota. riscoMedioRegiao vai de 1 a 5. */
    public record ResumoRota(int incidentesGraves, int incidentesLeves, int ocorrenciasRecentes, int assaltosRecentes,
                             double riscoMedioRegiao, int radares) {}

    /** indice é a posição da rota na resposta da TomTom (0 = rota principal). nota vai de 0 a 100. */
    public record RotaAvaliada(int indice, int nota, Classificacao classificacao, int distanciaMetros,
                               int duracaoSegundos, int atrasoTransitoSegundos, ResumoRota resumo,
                               List<String> alertas, List<Ponto> pontos) {}
}
