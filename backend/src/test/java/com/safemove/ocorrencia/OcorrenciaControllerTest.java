package com.safemove.ocorrencia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safemove.config.Fuso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("dev")
@SpringBootTest(properties = "safemove.incidentes.atraso-inicial-ms=3600000")
@AutoConfigureMockMvc
class OcorrenciaControllerTest {

    @Autowired MockMvc mvc;
    @Autowired OcorrenciaRepository repo;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void dados() {
        repo.deleteAll();
        // centro das buscas: -23.55, -46.63
        repo.save(new Ocorrencia(TipoOcorrencia.ASSALTO, "Celular roubado", -23.551, -46.631));
        repo.save(new Ocorrencia(TipoOcorrencia.BURACO, null, -23.57, -46.63));
        repo.save(new Ocorrencia(TipoOcorrencia.ALAGAMENTO, null, -23.9, -46.63));
        var antiga = repo.save(new Ocorrencia(TipoOcorrencia.ASSALTO, "Ontem", -23.5505, -46.6305));
        jdbc.update("UPDATE ocorrencias SET criado_em = ? WHERE id_ocorrencia = ?",
                Fuso.agora().minusHours(30), antiga.getId());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder busca() {
        return get("/ocorrencias").param("lat", "-23.55").param("lng", "-46.63");
    }

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
}
