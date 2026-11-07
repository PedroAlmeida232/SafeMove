package com.safemove.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safemove.auth.FirebaseAuthClient.Sessao;
import com.safemove.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.safemove.web.ApiException;

@ActiveProfiles("dev")
@SpringBootTest
@AutoConfigureMockMvc
class AuthFluxoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @MockitoBean FirebaseAuthClient firebase;

    @Test void registroCriaPerfilEDevolveTokens() throws Exception {
        when(firebase.cadastrar("ana@x.com", "123456")).thenReturn(new Sessao("ID0", "RT0", "uid-ana", "ana@x.com", null));
        when(firebase.definirNome("ID0", "Ana")).thenReturn(new Sessao("ID1", "RT1", "uid-ana", "ana@x.com", "Ana"));

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\" Ana \",\"email\":\"Ana@X.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("ID1"))
                .andExpect(jsonPath("$.refreshToken").value("RT1"))
                .andExpect(jsonPath("$.usuario.uid").value("uid-ana"))
                .andExpect(jsonPath("$.usuario.nome").value("Ana"));
        org.junit.jupiter.api.Assertions.assertTrue(usuarios.existsById("uid-ana"));
    }

    @Test void registroUsaTokensDoCadastroQuandoAtualizarNomeNaoDevolveTokens() throws Exception {
        when(firebase.cadastrar("edu@x.com", "123456")).thenReturn(new Sessao("ID0", "RT0", "uid-edu", "edu@x.com", null));
        when(firebase.definirNome("ID0", "Edu")).thenReturn(new Sessao(null, null, "uid-edu", "edu@x.com", "Edu"));

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Edu\",\"email\":\"edu@x.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("ID0"))
                .andExpect(jsonPath("$.refreshToken").value("RT0"));
    }

    @Test void cadastroSemNomeERejeitadoSemChamarOFirebase() throws Exception {
        for (String corpo : new String[] {
                "{\"email\":\"sem@x.com\",\"senha\":\"123456\"}",
                "{\"nome\":\"\",\"email\":\"sem@x.com\",\"senha\":\"123456\"}",
                "{\"nome\":\"   \",\"email\":\"sem@x.com\",\"senha\":\"123456\"}"}) {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.nome").value("Informe seu nome."));
        }
        org.mockito.Mockito.verifyNoInteractions(firebase);
    }

    @Test void loginDeQuemSoExisteNoFirebaseCriaOPerfil() throws Exception {
        when(firebase.entrar("bia@x.com", "123456")).thenReturn(new Sessao("ID", "RT", "uid-bia", "bia@x.com", null));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bia@x.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.nome").value("bia"))
                .andExpect(jsonPath("$.usuario.email").value("bia@x.com"));
    }

    @Test void erroDoFirebasePassaComOStatusCerto() throws Exception {
        when(firebase.entrar(any(), any())).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@x.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void refreshRenovaOToken() throws Exception {
        when(firebase.entrar(any(), any())).thenReturn(new Sessao("ID", "RT", "uid-caio", "caio@x.com", "Caio"));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"caio@x.com\",\"senha\":\"123456\"}")).andExpect(status().isOk());
        when(firebase.renovar(eq("RT"))).thenReturn(new FirebaseAuthClient.Renovacao("NOVO", "RT2", "uid-caio"));

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"RT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("NOVO"))
                .andExpect(jsonPath("$.refreshToken").value("RT2"))
                .andExpect(jsonPath("$.usuario.nome").value("Caio"));
    }

    @Test void meCriaPerfilAPartirDasClaimsDoToken() throws Exception {
        mvc.perform(get("/usuarios/me").with(jwt().jwt(j -> j.subject("uid-dani").claim("email", "Dani@x.com").claim("name", "Dani"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid-dani"))
                .andExpect(jsonPath("$.email").value("dani@x.com"))
                .andExpect(jsonPath("$.nome").value("Dani"));
    }

    @Test void meSemTokenDa401() throws Exception {
        mvc.perform(get("/usuarios/me")).andExpect(status().isUnauthorized());
    }
}
