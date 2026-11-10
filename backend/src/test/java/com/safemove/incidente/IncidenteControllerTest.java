package com.safemove.incidente;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("dev")
@SpringBootTest(properties = "safemove.incidentes.atraso-inicial-ms=3600000")
@AutoConfigureMockMvc
class IncidenteControllerTest {

    @Autowired MockMvc mvc;
    @Autowired IncidenteRepository repo;

    @BeforeEach void dados() {
        repo.deleteAll();
        // centro das buscas: -23.55, -46.63
        repo.save(new Incidente("perto", CategoriaIncidente.ACIDENTE, 3, "Av. Paulista", "Colisão", 240, -23.551, -46.631));
        repo.save(new Incidente("medio", CategoriaIncidente.CONGESTIONAMENTO, 2, "Marginal", null, null, -23.57, -46.63));
        repo.save(new Incidente("longe", CategoriaIncidente.ACIDENTE, 1, "Rodovia", null, null, -23.9, -46.63));
    }

    @Test void semFiltroDevolveTudoSemToken() throws Exception {
        mvc.perform(get("/incidentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test void raioFiltraEOrdenaPelaDistancia() throws Exception {
        mvc.perform(get("/incidentes").param("lat", "-23.55").param("lng", "-46.63").param("raio", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].via").value("Av. Paulista"))
                .andExpect(jsonPath("$[0].categoria").value("ACIDENTE"))
                .andExpect(jsonPath("$[0].distanciaMetros").isNumber())
                .andExpect(jsonPath("$[1].via").value("Marginal"));
    }

    @Test void raioPequenoExcluiOMedio() throws Exception {
        mvc.perform(get("/incidentes").param("lat", "-23.55").param("lng", "-46.63").param("raio", "500"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test void raioPadraoE5km() throws Exception {
        mvc.perform(get("/incidentes").param("lat", "-23.55").param("lng", "-46.63"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test void filtraPorCategoria() throws Exception {
        mvc.perform(get("/incidentes").param("categoria", "ACIDENTE"))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/incidentes").param("lat", "-23.55").param("lng", "-46.63").param("categoria", "ACIDENTE"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test void categoriaInvalidaDa400() throws Exception {
        mvc.perform(get("/incidentes").param("categoria", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test void latSemLngDa400() throws Exception {
        mvc.perform(get("/incidentes").param("lat", "-23.55"))
                .andExpect(status().isBadRequest());
    }

    @Test void coordenadaForaDoIntervaloDa400() throws Exception {
        mvc.perform(get("/incidentes").param("lat", "95").param("lng", "-46.63"))
                .andExpect(status().isBadRequest());
    }

    @Test void raioInvalidoDa400() throws Exception {
        mvc.perform(get("/incidentes").param("lat", "-23.55").param("lng", "-46.63").param("raio", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/incidentes").param("lat", "-23.55").param("lng", "-46.63").param("raio", "60000"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/incidentes").param("raio", "1000"))
                .andExpect(status().isBadRequest());
    }
}
