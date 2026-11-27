package com.safemove.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safemove.dao.RadarDAO;
import com.safemove.model.Radar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("dev")
@SpringBootTest(properties = {"safemove.incidentes.atraso-inicial-ms=3600000",
        "safemove.radares.atraso-inicial-ms=3600000"})
@AutoConfigureMockMvc
class RadarControllerTest {

    @Autowired MockMvc mvc;
    @Autowired RadarDAO repo;

    @BeforeEach void dados() {
        repo.deleteAll();
        // centro das buscas: -23.55, -46.63
        repo.save(new Radar(1L, -23.551, -46.631, 50));
        repo.save(new Radar(2L, -23.565, -46.63, null));
        repo.save(new Radar(3L, -23.9, -46.63, 60));
    }

    @Test void semFiltroDevolveTodosSemToken() throws Exception {
        mvc.perform(get("/radares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test void raioPadraoE2kmEOrdenaPelaDistancia() throws Exception {
        mvc.perform(get("/radares").param("lat", "-23.55").param("lng", "-46.63"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].velocidadeMax").value(50))
                .andExpect(jsonPath("$[0].distanciaMetros").isNumber())
                .andExpect(jsonPath("$[1].velocidadeMax").doesNotExist());
    }

    @Test void raioPequenoFiltra() throws Exception {
        mvc.perform(get("/radares").param("lat", "-23.55").param("lng", "-46.63").param("raio", "500"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test void parametrosInvalidosDao400() throws Exception {
        mvc.perform(get("/radares").param("lat", "-23.55")).andExpect(status().isBadRequest());
        mvc.perform(get("/radares").param("raio", "1000")).andExpect(status().isBadRequest());
        mvc.perform(get("/radares").param("lat", "-23.55").param("lng", "-46.63").param("raio", "60000"))
                .andExpect(status().isBadRequest());
    }
}
