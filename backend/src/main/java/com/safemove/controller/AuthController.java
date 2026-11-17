package com.safemove.controller;

import com.safemove.bo.AuthBO;
import com.safemove.dto.AuthDtos.AuthResponse;
import com.safemove.dto.AuthDtos.LoginRequest;
import com.safemove.dto.AuthDtos.RecuperarSenhaRequest;
import com.safemove.dto.AuthDtos.RefreshRequest;
import com.safemove.dto.AuthDtos.RegistroRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthBO auth;

    public AuthController(AuthBO auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return auth.registrar(req);
    }

    @PostMapping("/login")
    public AuthResponse entrar(@Valid @RequestBody LoginRequest req) {
        return auth.entrar(req);
    }

    @PostMapping("/refresh")
    public AuthResponse renovar(@Valid @RequestBody RefreshRequest req) {
        return auth.renovar(req);
    }

    /** Sempre 204, exista ou não conta com o e-mail. */
    @PostMapping("/recuperar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest req) {
        auth.recuperarSenha(req);
    }
}
