package com.safemove.auth;

import com.safemove.auth.AuthDtos.AuthResponse;
import com.safemove.auth.AuthDtos.LoginRequest;
import com.safemove.auth.AuthDtos.RefreshRequest;
import com.safemove.auth.AuthDtos.RegistroRequest;
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

    private final AuthService auth;

    public AuthController(AuthService auth) {
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
}
