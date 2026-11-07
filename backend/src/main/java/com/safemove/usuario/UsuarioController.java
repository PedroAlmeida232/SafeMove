package com.safemove.usuario;

import java.util.Locale;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarios;

    public UsuarioController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Se o token é válido mas o perfil ainda não existe aqui, cria a partir das claims do Firebase. */
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        Usuario u = usuarios.findById(jwt.getSubject()).orElseGet(() -> {
            String email = jwt.getClaimAsString("email");
            String nome = jwt.getClaimAsString("name");
            if (nome == null || nome.isBlank()) nome = email.substring(0, email.indexOf('@'));
            return usuarios.save(new Usuario(jwt.getSubject(), nome, email.toLowerCase(Locale.ROOT)));
        });
        return UsuarioResponse.de(u);
    }
}
