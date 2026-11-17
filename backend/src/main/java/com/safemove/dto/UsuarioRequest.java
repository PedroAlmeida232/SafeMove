package com.safemove.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de PUT /usuarios/me. O e-mail e a senha ficam no Firebase. */
public record UsuarioRequest(
        @NotBlank(message = "Informe seu nome.") @Size(max = 120, message = "Nome muito longo.") String nome) {}
