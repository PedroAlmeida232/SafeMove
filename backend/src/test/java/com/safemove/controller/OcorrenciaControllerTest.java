package com.safemove.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safemove.config.Fuso;
import com.safemove.dao.ConfirmacaoDAO;
import com.safemove.dao.OcorrenciaDAO;
import com.safemove.dao.UsuarioDAO;
import com.safemove.model.Ocorrencia;
import com.safemove.model.TipoOcorrencia;
import com.safemove.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("dev")
@SpringBootTest(properties = "safemove.incidentes.atraso-inicial-ms=3600000")
@AutoConfigureMockMvc
class OcorrenciaControllerTest {

    @Autowired MockMvc mvc;
    @Autowired OcorrenciaDAO repo;
    @Autowired ConfirmacaoDAO confirmacoes;
    @Autowired UsuarioDAO usuarios;
    @Autowired JdbcTemplate jdbc;

    Ocorrencia assalto;

    @BeforeEach void dados() {
        confirmacoes.deleteAll();
        repo.deleteAll();
        usuarios.deleteAll();
        Usuario ana = usuarios.save(new Usuario("uid-ana", "Ana", "ana@x.com"));
        // centro das buscas: -23.55, -46.63
        assalto = repo.save(new Ocorrencia(ana, TipoOcorrencia.ASSALTO, "Celular roubado", -23.551, -46.631));
        repo.save(new Ocorrencia(ana, TipoOcorrencia.BURACO, null, -23.57, -46.63));
        repo.save(new Ocorrencia(ana, TipoOcorrencia.ALAGAMENTO, null, -23.9, -46.63));
        var antiga = repo.save(new Ocorrencia(ana, TipoOcorrencia.ASSALTO, "Ontem", -23.5505, -46.6305));
        jdbc.update("UPDATE ocorrencias SET criado_em = ? WHERE id_ocorrencia = ?",
                Fuso.agora().minusHours(30), antiga.getId());
    }

    private static JwtRequestPostProcessor como(String uid) {
        return jwt().jwt(j -> j.subject(uid).claim("email", uid.substring(4) + "@x.com"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder busca() {
        return get("/ocorrencias").param("lat", "-23.55").param("lng", "-46.63");
    }

    // GET ------------------------------------------------------------------

    @Test void padraoE2kmE24hSemToken() throws Exception {
        mvc.perform(busca())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("ASSALTO"))
                .andExpect(jsonPath("$[0].descricao").value("Celular roubado"))
                .andExpect(jsonPath("$[0].confirmacoes").value(0))
                .andExpect(jsonPath("$[0].criadoEm").value(org.hamcrest.Matchers.endsWith("-03:00")));
    }

    @Test void raioMaiorTrazOMedio() throws Exception {
        mvc.perform(busca().param("raio", "5000"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test void horasMaioresTrazAAntigaEOrdenaDaMaisRecente() throws Exception {
        mvc.perform(busca().param("horas", "48"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].descricao").value("Celular roubado"))
                .andExpect(jsonPath("$[1].descricao").value("Ontem"));
    }

    @Test void filtraPorTipo() throws Exception {
        mvc.perform(busca().param("raio", "5000").param("tipo", "BURACO"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("BURACO"));
    }

    @Test void latELngSaoObrigatorios() throws Exception {
        mvc.perform(get("/ocorrencias")).andExpect(status().isBadRequest());
        mvc.perform(get("/ocorrencias").param("lat", "-23.55")).andExpect(status().isBadRequest());
    }

    @Test void parametrosInvalidosDao400() throws Exception {
        mvc.perform(get("/ocorrencias").param("lat", "95").param("lng", "-46.63")).andExpect(status().isBadRequest());
        mvc.perform(busca().param("raio", "0")).andExpect(status().isBadRequest());
        mvc.perform(busca().param("raio", "999999")).andExpect(status().isBadRequest());
        mvc.perform(busca().param("horas", "0")).andExpect(status().isBadRequest());
        mvc.perform(busca().param("horas", "100000")).andExpect(status().isBadRequest());
        mvc.perform(busca().param("tipo", "INEXISTENTE")).andExpect(status().isBadRequest());
    }

    @Test void buscaPorIdSemToken() throws Exception {
        mvc.perform(get("/ocorrencias/{id}", assalto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Celular roubado"));
        mvc.perform(get("/ocorrencias/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ocorrência não encontrada."));
    }

    // POST -----------------------------------------------------------------

    @Test void criarExigeToken() throws Exception {
        mvc.perform(post("/ocorrencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"BURACO\",\"latitude\":-23.55,\"longitude\":-46.63}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void criarGravaComOAutorECriaOPerfil() throws Exception {
        mvc.perform(post("/ocorrencias").with(como("uid-bia")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"BURACO\",\"descricao\":\"  Buraco fundo \",\"latitude\":-23.55,\"longitude\":-46.63}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipo").value("BURACO"))
                .andExpect(jsonPath("$.descricao").value("Buraco fundo"))
                .andExpect(jsonPath("$.confirmacoes").value(0));
        assertEquals("bia", usuarios.findById("uid-bia").orElseThrow().getNome());
    }

    @Test void criarValidaOCorpo() throws Exception {
        mvc.perform(post("/ocorrencias").with(como("uid-bia")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-23.55}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipo").value("Escolha o tipo da ocorrência."))
                .andExpect(jsonPath("$.campos.longitude").value("Informe a longitude."));
    }

    @Test void criarForaDeSaoPauloDa422() throws Exception {
        mvc.perform(post("/ocorrencias").with(como("uid-bia")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"BURACO\",\"latitude\":-22.90,\"longitude\":-43.17}"))
                .andExpect(status().isUnprocessableContent());
    }

    // PUT ------------------------------------------------------------------

    @Test void autorAtualiza() throws Exception {
        mvc.perform(put("/ocorrencias/{id}", assalto.getId()).with(como("uid-ana")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ACIDENTE\",\"descricao\":\"Batida\",\"latitude\":-23.551,\"longitude\":-46.631}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ACIDENTE"))
                .andExpect(jsonPath("$.descricao").value("Batida"));
        assertEquals(TipoOcorrencia.ACIDENTE, repo.findById(assalto.getId()).orElseThrow().getTipo());
    }

    @Test void outroUsuarioNaoAtualizaNemExclui() throws Exception {
        mvc.perform(put("/ocorrencias/{id}", assalto.getId()).with(como("uid-bia")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ACIDENTE\",\"latitude\":-23.551,\"longitude\":-46.631}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(delete("/ocorrencias/{id}", assalto.getId()).with(como("uid-bia")))
                .andExpect(status().isForbidden());
    }

    @Test void atualizarInexistenteDa404() throws Exception {
        mvc.perform(put("/ocorrencias/999999").with(como("uid-ana")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ACIDENTE\",\"latitude\":-23.551,\"longitude\":-46.631}"))
                .andExpect(status().isNotFound());
    }

    // DELETE ---------------------------------------------------------------

    @Test void autorExcluiInclusiveComConfirmacoes() throws Exception {
        mvc.perform(post("/ocorrencias/{id}/confirmar", assalto.getId()).with(como("uid-bia")))
                .andExpect(status().isOk());
        mvc.perform(delete("/ocorrencias/{id}", assalto.getId()).with(como("uid-ana")))
                .andExpect(status().isNoContent());
        assertFalse(repo.existsById(assalto.getId()));
        assertEquals(0, confirmacoes.count());
    }

    // confirmar ------------------------------------------------------------

    @Test void confirmarSomaUmaVezPorUsuario() throws Exception {
        mvc.perform(post("/ocorrencias/{id}/confirmar", assalto.getId()).with(como("uid-bia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmacoes").value(1));
        mvc.perform(post("/ocorrencias/{id}/confirmar", assalto.getId()).with(como("uid-bia")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Você já confirmou esta ocorrência."));
        mvc.perform(post("/ocorrencias/{id}/confirmar", assalto.getId()).with(como("uid-edu")))
                .andExpect(jsonPath("$.confirmacoes").value(2));
    }

    @Test void autorNaoConfirmaAPropria() throws Exception {
        mvc.perform(post("/ocorrencias/{id}/confirmar", assalto.getId()).with(como("uid-ana")))
                .andExpect(status().isUnprocessableContent());
    }
}
