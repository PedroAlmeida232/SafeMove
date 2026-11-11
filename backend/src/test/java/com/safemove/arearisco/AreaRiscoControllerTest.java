package com.safemove.arearisco;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safemove.config.Fuso;
import com.safemove.ocorrencia.Ocorrencia;
import com.safemove.ocorrencia.OcorrenciaRepository;
import com.safemove.ocorrencia.TipoOcorrencia;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("dev")
@SpringBootTest(properties = "safemove.incidentes.atraso-inicial-ms=3600000")
@AutoConfigureMockMvc
class AreaRiscoControllerTest {

    // Índices (roubos + furtos/2): Moema 10 · Pinheiros 20 · República 150 · Brás 300 · Sé 600 → quintis 1..5
    @DynamicPropertySource
    static void csvDaSsp(DynamicPropertyRegistry r) throws IOException {
        Path csv = Files.createTempFile("ssp", ".csv");
        csv.toFile().deleteOnExit();
        Files.writeString(csv, """
                distrito;mes;roubos;furtos
                Sé;2026-09;9999;9999
                SÉ;2026-10;400;400
                Se;2026-10;0;400
                Brás;2026-10;300;0
                República;2026-10;100;100
                Pinheiros;2026-10;10;20
                Moema;2026-10;5;10
                """);
        r.add("safemove.areas-risco.estatisticas-csv", csv::toString);
    }

    private static final double SE_LAT = -23.5505, SE_LNG = -46.6333;
    private static final double PINHEIROS_LAT = -23.5672, PINHEIROS_LNG = -46.6917;

    @Autowired MockMvc mvc;
    @Autowired OcorrenciaRepository repo;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void dados() {
        repo.deleteAll();
        for (int i = 0; i < 7; i++) {
            repo.save(new Ocorrencia(TipoOcorrencia.ASSALTO, "Celular", SE_LAT + i * 0.0005, SE_LNG));
        }
        repo.save(new Ocorrencia(TipoOcorrencia.BURACO, null, SE_LAT, SE_LNG)); // não é crime
        var antigo = repo.save(new Ocorrencia(TipoOcorrencia.ASSALTO, "Antigo", SE_LAT, SE_LNG));
        jdbc.update("UPDATE ocorrencias SET criado_em = ? WHERE id_ocorrencia = ?",
                Fuso.agora().minusDays(40), antigo.getId()); // fora dos 30 dias
        repo.save(new Ocorrencia(TipoOcorrencia.ASSALTO, null, PINHEIROS_LAT, PINHEIROS_LNG));
        repo.save(new Ocorrencia(TipoOcorrencia.ASSALTO, null, -22.9, -43.2)); // Rio: fora de SP
    }

    @Test void listaTodosOsDistritosDoMaisArriscadoSemToken() throws Exception {
        mvc.perform(get("/areas-risco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(95))
                .andExpect(jsonPath("$[0].distrito").value("Sé"))
                .andExpect(jsonPath("$[0].nivel").value(5))
                .andExpect(jsonPath("$[0].segura").value(false))
                .andExpect(jsonPath("$[0].assaltosReportados").value(7))
                .andExpect(jsonPath("$[0].roubosOficiais").value(400))
                .andExpect(jsonPath("$[0].furtosOficiais").value(800))
                .andExpect(jsonPath("$[0].mesOficial").value("2026-10"))
                .andExpect(jsonPath("$[1].distrito").value("Brás"))
                .andExpect(jsonPath("$[1].nivel").value(4))
                .andExpect(jsonPath("$[2].distrito").value("República"))
                .andExpect(jsonPath("$[2].nivel").value(3))
                .andExpect(jsonPath("$[?(@.distrito == 'Pinheiros')].nivel").value(2))
                .andExpect(jsonPath("$[?(@.distrito == 'Pinheiros')].segura").value(true))
                .andExpect(jsonPath("$[?(@.distrito == 'Pinheiros')].assaltosReportados").value(1))
                .andExpect(jsonPath("$[?(@.distrito == 'Liberdade')].nivel").value(1))
                .andExpect(jsonPath("$[?(@.distrito == 'Liberdade')].mesOficial").value(org.hamcrest.Matchers.contains((Object) null)));
    }

    @Test void avaliaAreaInseguraComAlerta() throws Exception {
        mvc.perform(get("/areas-risco/avaliar").param("lat", "-23.5507").param("lng", "-46.6330"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area.distrito").value("Sé"))
                .andExpect(jsonPath("$.area.segura").value(false))
                .andExpect(jsonPath("$.titulo").value("Você está em uma área de risco"))
                .andExpect(jsonPath("$.mensagem").value(containsString("7 assaltos reportados no app nos últimos 30 dias")))
                .andExpect(jsonPath("$.mensagem").value(containsString("400 roubos e 800 furtos registrados pela SSP em 10/2026")))
                .andExpect(jsonPath("$.distanciaMetros").value(org.hamcrest.Matchers.lessThan(100.0)));
    }

    @Test void avaliaAreaSegura() throws Exception {
        mvc.perform(get("/areas-risco/avaliar").param("lat", String.valueOf(PINHEIROS_LAT))
                        .param("lng", String.valueOf(PINHEIROS_LNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area.distrito").value("Pinheiros"))
                .andExpect(jsonPath("$.area.segura").value(true))
                .andExpect(jsonPath("$.titulo").value("Região segura"));
    }

    @Test void foraDeSaoPauloDa404EParametrosInvalidos400() throws Exception {
        mvc.perform(get("/areas-risco/avaliar").param("lat", "-22.9").param("lng", "-43.2"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/areas-risco/avaliar").param("lat", "95").param("lng", "-46.63"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/areas-risco/avaliar").param("lat", "-23.55"))
                .andExpect(status().isBadRequest());
    }
}
