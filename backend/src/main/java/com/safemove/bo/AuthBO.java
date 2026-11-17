package com.safemove.bo;

import com.safemove.dao.UsuarioDAO;
import com.safemove.dto.AuthDtos.AuthResponse;
import com.safemove.dto.AuthDtos.LoginRequest;
import com.safemove.dto.AuthDtos.RecuperarSenhaRequest;
import com.safemove.dto.AuthDtos.RefreshRequest;
import com.safemove.dto.AuthDtos.RegistroRequest;
import com.safemove.dto.UsuarioResponse;
import com.safemove.exception.ApiException;
import com.safemove.integracao.FirebaseAuthClient.Sessao;
import com.safemove.integracao.FirebaseAuthClient;
import com.safemove.model.Usuario;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthBO {

    private final UsuarioDAO usuarios;
    private final FirebaseAuthClient firebase;

    public AuthBO(UsuarioDAO usuarios, FirebaseAuthClient firebase) {
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

    public void recuperarSenha(RecuperarSenhaRequest req) {
        firebase.recuperarSenha(normalizar(req.email()));
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
