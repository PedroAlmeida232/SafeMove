package com.safemove.usuario;

import com.safemove.config.Fuso;
import java.time.OffsetDateTime;

/** criadoEm sai sempre no horário de São Paulo, ex.: 2026-11-06T23:50:30-03:00. */
public record UsuarioResponse(String uid, String nome, String email, OffsetDateTime criadoEm) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getUid(), u.getNome(), u.getEmail(), Fuso.emSaoPaulo(u.getCriadoEm()));
    }
}
