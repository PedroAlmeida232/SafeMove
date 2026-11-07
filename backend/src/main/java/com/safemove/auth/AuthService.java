package com.safemove.auth;

import com.safemove.auth.AuthDtos.AuthResponse;
import com.safemove.auth.AuthDtos.LoginRequest;
import com.safemove.auth.AuthDtos.RegistroRequest;
import com.safemove.usuario.Usuario;
import com.safemove.usuario.UsuarioRepository;
import com.safemove.usuario.UsuarioResponse;
import com.safemove.web.ApiException;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse registrar(RegistroRequest req) {
        String email = normalizar(req.email());
        if (usuarios.existsByEmail(email)) {
            throw ApiException.conflito("Já existe uma conta com esse e-mail.");
        }
        Usuario u = usuarios.save(new Usuario(req.nome().trim(), email, encoder.encode(req.senha())));
        return new AuthResponse(jwt.gerar(u), UsuarioResponse.de(u));
    }

    public AuthResponse entrar(LoginRequest req) {
        Usuario u = usuarios.findByEmail(normalizar(req.email()))
                .filter(x -> encoder.matches(req.senha(), x.getSenhaHash()))
                .orElseThrow(() -> ApiException.naoAutenticado("E-mail ou senha incorretos."));
        return new AuthResponse(jwt.gerar(u), UsuarioResponse.de(u));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
