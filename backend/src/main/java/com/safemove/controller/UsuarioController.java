package com.safemove.controller;

import com.safemove.bo.UsuarioBO;
import com.safemove.dto.UsuarioRequest;
import com.safemove.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Perfil de quem está logado (o uid vem do token do Firebase). */
@RestController
@RequestMapping("/usuarios/me")
public class UsuarioController {

    private final UsuarioBO bo;

    public UsuarioController(UsuarioBO bo) {
        this.bo = bo;
    }

    @GetMapping
    public UsuarioResponse buscar(@AuthenticationPrincipal Jwt jwt) {
        return bo.buscar(jwt);
    }

    @PutMapping
    public UsuarioResponse atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UsuarioRequest req) {
        return bo.atualizar(jwt, req);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt) {
        bo.excluir(jwt);
    }
}
