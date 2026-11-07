package com.safemove.usuario;

import java.time.Instant;

public record UsuarioResponse(String uid, String nome, String email, Instant criadoEm) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getUid(), u.getNome(), u.getEmail(), u.getCriadoEm());
    }
}
