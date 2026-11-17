package com.safemove.dto;

import com.safemove.model.TipoOcorrencia;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de POST e PUT /ocorrencias. */
public record OcorrenciaRequest(
        @NotNull(message = "Escolha o tipo da ocorrência.") TipoOcorrencia tipo,
        @Size(max = 500, message = "A descrição pode ter até 500 caracteres.") String descricao,
        @NotNull(message = "Informe a latitude.")
        @DecimalMin(value = "-90", message = "Latitude inválida.")
        @DecimalMax(value = "90", message = "Latitude inválida.") Double latitude,
        @NotNull(message = "Informe a longitude.")
        @DecimalMin(value = "-180", message = "Longitude inválida.")
        @DecimalMax(value = "180", message = "Longitude inválida.") Double longitude) {}
