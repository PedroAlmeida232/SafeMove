package com.safemove.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.safemove.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Fala com o Firebase Authentication pela API REST. É o backend que autentica: o app nunca
 * conversa com o Firebase nem conhece as chaves.
 */
@Component
public class FirebaseAuthClient {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthClient.class);
    private static final String IDENTITY = "https://identitytoolkit.googleapis.com/v1/accounts";
    private static final String TOKEN = "https://securetoken.googleapis.com/v1/token";

    /** Sessão devolvida pelo Firebase (idToken vale 1h; refreshToken renova). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sessao(String idToken, String refreshToken, String localId, String email, String displayName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Renovacao(@JsonProperty("id_token") String idToken,
                     @JsonProperty("refresh_token") String refreshToken,
                     @JsonProperty("user_id") String userId) {}

    record Credenciais(String email, String password, boolean returnSecureToken) {}
    record NovoNome(String idToken, String displayName, boolean returnSecureToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Erro(Detalhe error) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Detalhe(String message) {}
    }

    private final RestClient rest;
    private final String apiKey;

    @Autowired
    public FirebaseAuthClient(@Value("${safemove.firebase.api-key}") String apiKey) {
        this(RestClient.builder(), apiKey);
    }

    /** Usado nos testes, para trocar o servidor por um simulado. */
    FirebaseAuthClient(RestClient.Builder builder, String apiKey) {
        this.rest = builder.build();
        this.apiKey = apiKey;
    }

    public Sessao cadastrar(String email, String senha) {
        return identity("signUp", new Credenciais(email, senha, true));
    }

    public Sessao entrar(String email, String senha) {
        return identity("signInWithPassword", new Credenciais(email, senha, true));
    }

    /** Grava o nome no Firebase e devolve um token novo, já com o nome. */
    public Sessao definirNome(String idToken, String nome) {
        return identity("update", new NovoNome(idToken, nome, true));
    }

    public Renovacao renovar(String refreshToken) {
        exigirConfigurado();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return chamar(() -> rest.post().uri(TOKEN + "?key={k}", apiKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(Renovacao.class));
    }

    private Sessao identity(String operacao, Object corpo) {
        exigirConfigurado();
        return chamar(() -> rest.post().uri(IDENTITY + ":" + operacao + "?key={k}", apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(corpo)
                .retrieve().body(Sessao.class));
    }

    private void exigirConfigurado() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Login indisponível: preencha FIREBASE_API_KEY no backend/.env.");
        }
    }

    private <T> T chamar(java.util.function.Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RestClientResponseException e) {
            throw traduzir(codigoDoErro(e));
        } catch (RestClientException e) {
            log.error("Falha ao falar com o Firebase", e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "O serviço de login está indisponível no momento.");
        }
    }

    private static String codigoDoErro(RestClientResponseException e) {
        try {
            Erro erro = e.getResponseBodyAs(Erro.class);
            if (erro != null && erro.error() != null && erro.error().message() != null) {
                return erro.error().message();
            }
        } catch (RuntimeException ignorada) {
            // corpo não é JSON: cai no genérico
        }
        return "";
    }

    /** Códigos do Firebase podem vir com texto extra ("WEAK_PASSWORD : ..."), por isso startsWith. */
    static ApiException traduzir(String codigo) {
        if (codigo.startsWith("EMAIL_EXISTS")) {
            return new ApiException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail.");
        }
        if (codigo.startsWith("INVALID_LOGIN_CREDENTIALS") || codigo.startsWith("EMAIL_NOT_FOUND")
                || codigo.startsWith("INVALID_PASSWORD") || codigo.startsWith("USER_NOT_FOUND")) {
            return new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }
        if (codigo.startsWith("USER_DISABLED")) {
            return new ApiException(HttpStatus.UNAUTHORIZED, "Esta conta foi desativada.");
        }
        if (codigo.startsWith("TOKEN_EXPIRED") || codigo.startsWith("INVALID_REFRESH_TOKEN")
                || codigo.startsWith("INVALID_GRANT_TYPE") || codigo.startsWith("INVALID_ID_TOKEN")) {
            return new ApiException(HttpStatus.UNAUTHORIZED, "Sua sessão expirou. Entre novamente.");
        }
        if (codigo.startsWith("WEAK_PASSWORD")) {
            return new ApiException(HttpStatus.BAD_REQUEST, "A senha precisa ter pelo menos 6 caracteres.");
        }
        if (codigo.startsWith("INVALID_EMAIL")) {
            return new ApiException(HttpStatus.BAD_REQUEST, "E-mail inválido.");
        }
        if (codigo.startsWith("MISSING_PASSWORD")) {
            return new ApiException(HttpStatus.BAD_REQUEST, "Digite sua senha.");
        }
        if (codigo.startsWith("TOO_MANY_ATTEMPTS_TRY_LATER")) {
            return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde um pouco e tente de novo.");
        }
        // OPERATION_NOT_ALLOWED (provedor e-mail/senha desligado), chave inválida etc.: problema nosso, não do usuário.
        log.error("Erro do Firebase não mapeado: '{}'", codigo);
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "O serviço de login está indisponível no momento.");
    }
}
