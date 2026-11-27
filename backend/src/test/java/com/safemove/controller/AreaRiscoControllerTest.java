package com.safemove.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("dev")
@SpringBootTest(properties = {"safemove.incidentes.atraso-inicial-ms=3600000",
        "safemove.radares.atraso-inicial-ms=3600000", "safemove.areas-risco.cache-ms=0"})
@AutoConfigureMockMvc
class AreaRiscoControllerTest {

    @Autowired MockMvc mvc;
    @Autowired OcorrenciaDAO ocorrencias;
    @Autowired ConfirmacaoDAO confirmacoes;
    @Autowired UsuarioDAO usuarios;
    @Autowired JdbcTemplate jdbc;

    Usuario ana;

    @BeforeEach void limpar() {
        confirmacoes.deleteAll();
        ocorrencias.deleteAll();
        usuarios.deleteAll();
        ana = usuarios.save(new Usuario("uid-ana", "Ana", "ana@x.com"));
    }

    @Test void semOcorrenciasTodosOsDistritosTemNivel1() throws Exception {
        mvc.perform(get("/areas-risco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(48))
                .andExpect(jsonPath("$[0].nivel").value(1));
    }

    @Test void assaltosNaSeElevamONivelDaSe() throws Exception {
        // 4 assaltos (12 pontos) perto da Praça da Sé → nível 5
        for (int i = 0; i < 4; i++) {
            ocorrencias.save(new Ocorrencia(ana, TipoOcorrencia.ASSALTO, null, -23.5505 + i * 0.0005, -46.6333));
        }
        mvc.perform(get("/areas-risco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distrito").value("Sé"))
                .andExpect(jsonPath("$[0].nivel").value(5))
                .andExpect(jsonPath("$[0].ocorrencias").value(4))
                .andExpect(jsonPath("$[1].nivel").value(1)); // não conta para a República
    }

    @Test void ocorrenciasAntigasNaoContam() throws Exception {
        var antiga = ocorrencias.save(new Ocorrencia(ana, TipoOcorrencia.ASSALTO, null, -23.5505, -46.6333));
        jdbc.update("UPDATE ocorrencias SET criado_em = ? WHERE id_ocorrencia = ?",
                Fuso.agora().minusDays(31), antiga.getId());
        mvc.perform(get("/areas-risco"))
                .andExpect(jsonPath("$[0].nivel").value(1));
    }
}
