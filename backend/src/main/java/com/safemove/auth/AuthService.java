package com.safemove.auth;

import com.safemove.auth.AuthDtos.AuthResponse;
import com.safemove.auth.AuthDtos.LoginRequest;
import com.safemove.auth.AuthDtos.RefreshRequest;
import com.safemove.auth.AuthDtos.RegistroRequest;
import com.safemove.auth.FirebaseAuthClient.Sessao;
import com.safemove.usuario.Usuario;
import com.safemove.usuario.UsuarioRepository;
import com.safemove.usuario.UsuarioResponse;
import com.safemove.web.ApiException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final FirebaseAuthClient firebase;

    public AuthService(UsuarioRepository usuarios, FirebaseAuthClient firebase) {
        this.usuarios = usuarios;
        this.firebase = firebase;
    }

    public AuthResponse registrar(RegistroRequest req) {
        String nome = req.nome().trim();
        Sessao criada = firebase.cadastrar(normalizar(req.email()), req.senha());
        // Grava o nome no Firebase. Essa chamada nem sempre devolve tokens novos: se vierem, usa
        // os novos (com o nome dentro); senão, mantém os do cadastro, que já são válidos.
        Sessao nomeada = firebase.definirNome(criada.idToken(), nome);
        Sessao sessao = new Sessao(
                temTexto(nomeada.idToken()) ? nomeada.idToken() : criada.idToken(),
                temTexto(nomeada.refreshToken()) ? nomeada.refreshToken() : criada.refreshToken(),
                criada.localId(), criada.email(), nome);
        Usuario u = usuarios.save(new Usuario(criada.localId(), nome, normalizar(req.email())));
        return resposta(sessao, u);
    }

    public AuthResponse entrar(LoginRequest req) {
        Sessao sessao = firebase.entrar(normalizar(req.email()), req.senha());
        // Quem foi criado direto no console do Firebase ainda não tem perfil aqui: cria no 1º login.
        Usuario u = usuarios.findById(sessao.localId()).orElseGet(() -> usuarios.save(new Usuario(
                sessao.localId(), nomeOuEmail(sessao.displayName(), sessao.email()), normalizar(sessao.email()))));
        return resposta(sessao, u);
    }

    public AuthResponse renovar(RefreshRequest req) {
        var nova = firebase.renovar(req.refreshToken());
        Usuario u = usuarios.findById(nova.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sua sessão expirou. Entre novamente."));
        return new AuthResponse(nova.idToken(), nova.refreshToken(), UsuarioResponse.de(u));
    }

    private static AuthResponse resposta(Sessao s, Usuario u) {
        return new AuthResponse(s.idToken(), s.refreshToken(), UsuarioResponse.de(u));
    }

    private static boolean temTexto(String v) {
        return v != null && !v.isBlank();
    }

    private static String nomeOuEmail(String nome, String email) {
        return nome != null && !nome.isBlank() ? nome : email.substring(0, email.indexOf('@'));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
