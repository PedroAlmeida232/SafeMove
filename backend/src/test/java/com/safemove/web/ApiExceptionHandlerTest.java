package com.safemove.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles("dev")
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiExceptionHandlerTest.Fake.class)
class ApiExceptionHandlerTest {

    @TestConfiguration
    @RestController
    static class Fake {
        @GetMapping("/_t/conflito") void conflito() { throw ApiException.conflito("conflito"); }
        @GetMapping("/_t/regra") void regra() { throw ApiException.naoProcessavel("fora de São Paulo"); }
        @GetMapping("/_t/indisponivel") void indisponivel() { throw ApiException.indisponivel("fora"); }
        @GetMapping("/_t/banco") void banco() { throw new DataAccessResourceFailureException("db down"); }
        @GetMapping("/_t/quebra") void quebra() { throw new IllegalStateException("segredo interno"); }
    }

    @Autowired MockMvc mvc;

    @Test void status400ComCampos() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"x\",\"senha\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.campos.email").value("E-mail inválido."));
    }

    @Test void status400CorpoIlegivel() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test void status401SemToken() throws Exception {
        mvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test void status401LoginErrado() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao@existe.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test void status404RotaInexistente() throws Exception {
        mvc.perform(get("/nao-existe").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test void status409() throws Exception {
        mvc.perform(get("/_t/conflito").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test void status422() throws Exception {
        mvc.perform(get("/_t/regra").with(jwt()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("fora de São Paulo"));
    }

    @Test void status503() throws Exception {
        mvc.perform(get("/_t/indisponivel").with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test void status503BancoForaDoAr() throws Exception {
        mvc.perform(get("/_t/banco").with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("O serviço está indisponível no momento."));
    }

    @Test void status500NaoVazaDetalhes() throws Exception {
        mvc.perform(get("/_t/quebra").with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("O servidor teve um problema. Tente de novo em instantes."));
    }
}
