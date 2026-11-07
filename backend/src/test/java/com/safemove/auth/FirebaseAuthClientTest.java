package com.safemove.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.safemove.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FirebaseAuthClientTest {

    private static final String BASE = "https://identitytoolkit.googleapis.com/v1/accounts";

    private MockRestServiceServer server;
    private FirebaseAuthClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FirebaseAuthClient(builder, "CHAVE");
    }

    private void erro(String url, int status, String mensagem) {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":" + status + ",\"message\":\"" + mensagem + "\"}}"));
    }

    @Test void entrarDevolveTokens() {
        server.expect(requestTo(BASE + ":signInWithPassword?key=CHAVE"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"email\":\"a@x.com\",\"password\":\"123456\",\"returnSecureToken\":true}"))
                .andRespond(withSuccess("{\"localId\":\"u1\",\"email\":\"a@x.com\",\"displayName\":\"Ana\","
                        + "\"idToken\":\"ID\",\"refreshToken\":\"RT\",\"expiresIn\":\"3600\",\"kind\":\"x\"}",
                        MediaType.APPLICATION_JSON));
        var s = client.entrar("a@x.com", "123456");
        assertEquals("u1", s.localId());
        assertEquals("ID", s.idToken());
        assertEquals("RT", s.refreshToken());
        assertEquals("Ana", s.displayName());
        server.verify();
    }

    @Test void renovarLeCamposEmSnakeCase() {
        server.expect(requestTo("https://securetoken.googleapis.com/v1/token?key=CHAVE"))
                .andExpect(content().string("grant_type=refresh_token&refresh_token=RT"))
                .andRespond(withSuccess("{\"id_token\":\"NOVO\",\"refresh_token\":\"RT2\",\"user_id\":\"u1\"}",
                        MediaType.APPLICATION_JSON));
        var r = client.renovar("RT");
        assertEquals("NOVO", r.idToken());
        assertEquals("RT2", r.refreshToken());
        assertEquals("u1", r.userId());
    }

    private void esperaErro(HttpStatus esperado, String msg) {
        ApiException e = assertThrows(ApiException.class, () -> client.entrar("a@x.com", "123456"));
        assertEquals(esperado, e.getStatus());
        assertEquals(msg, e.getMessage());
    }

    @Test void emailExistenteViraConflito() {
        erro(BASE + ":signInWithPassword?key=CHAVE", 400, "EMAIL_EXISTS");
        esperaErro(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail.");
    }

    @Test void credenciaisInvalidasViram401() {
        erro(BASE + ":signInWithPassword?key=CHAVE", 400, "INVALID_LOGIN_CREDENTIALS");
        esperaErro(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
    }

    @Test void senhaFracaComTextoExtraViraBadRequest() {
        erro(BASE + ":signInWithPassword?key=CHAVE", 400, "WEAK_PASSWORD : Password should be at least 6 characters");
        esperaErro(HttpStatus.BAD_REQUEST, "A senha precisa ter pelo menos 6 caracteres.");
    }

    @Test void muitasTentativasViram429() {
        erro(BASE + ":signInWithPassword?key=CHAVE", 400, "TOO_MANY_ATTEMPTS_TRY_LATER");
        esperaErro(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde um pouco e tente de novo.");
    }

    @Test void erroDesconhecidoViraIndisponivel() {
        erro(BASE + ":signInWithPassword?key=CHAVE", 400, "OPERATION_NOT_ALLOWED");
        esperaErro(HttpStatus.SERVICE_UNAVAILABLE, "O serviço de login está indisponível no momento.");
    }

    @Test void corpoNaoJsonViraIndisponivel() {
        server.expect(requestTo(BASE + ":signInWithPassword?key=CHAVE"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("<html>bad gateway</html>"));
        esperaErro(HttpStatus.SERVICE_UNAVAILABLE, "O serviço de login está indisponível no momento.");
    }

    @Test void semChaveConfiguradaAvisaSemChamarFirebase() {
        var semChave = new FirebaseAuthClient(RestClient.builder(), "");
        ApiException e = assertThrows(ApiException.class, () -> semChave.entrar("a@x.com", "123456"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }
}
