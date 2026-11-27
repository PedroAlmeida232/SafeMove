package com.safemove.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safemove.dao.ConfirmacaoDAO;
import com.safemove.dao.IncidenteDAO;
import com.safemove.dao.OcorrenciaDAO;
import com.safemove.dao.RadarDAO;
import com.safemove.dao.UsuarioDAO;
import com.safemove.exception.ApiException;
import com.safemove.geo.Coordenada;
import com.safemove.integracao.RotaTomTom;
import com.safemove.integracao.TomTomRotasClient;
import com.safemove.model.CategoriaIncidente;
import com.safemove.model.Incidente;
import com.safemove.model.Ocorrencia;
import com.safemove.model.TipoOcorrencia;
import com.safemove.model.Usuario;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("dev")
@SpringBootTest(properties = {"safemove.incidentes.atraso-inicial-ms=3600000",
        "safemove.radares.atraso-inicial-ms=3600000", "safemove.areas-risco.cache-ms=0",
        "safemove.rotas.cache-ms=0"})
@AutoConfigureMockMvc
class RotaControllerTest {

    @Autowired MockMvc mvc;
    @Autowired IncidenteDAO incidentes;
    @Autowired OcorrenciaDAO ocorrencias;
    @Autowired ConfirmacaoDAO confirmacoes;
    @Autowired UsuarioDAO usuarios;
    @Autowired RadarDAO radares;
    @MockitoBean TomTomRotasClient tomTom;

    /** Rota "direta" passa por um assalto e um acidente grave; a "desvio" passa longe de tudo. */
    List<Coordenada> direta = reta(-23.5610, -46.6560, -23.5500, -46.6330);
    List<Coordenada> desvio = reta(-23.5610, -46.6560, -23.5800, -46.6300);

    static List<Coordenada> reta(double lat1, double lng1, double lat2, double lng2) {
        List<Coordenada> pontos = new ArrayList<>();
        for (int i = 0; i <= 50; i++) {
            pontos.add(new Coordenada(lat1 + (lat2 - lat1) * i / 50, lng1 + (lng2 - lng1) * i / 50));
        }
        return pontos;
    }

    @BeforeEach void dados() {
        confirmacoes.deleteAll();
        ocorrencias.deleteAll();
        usuarios.deleteAll();
        incidentes.deleteAll();
        radares.deleteAll();
        Usuario ana = usuarios.save(new Usuario("uid-ana", "Ana", "ana@x.com"));
        Coordenada meio = direta.get(25);
        ocorrencias.save(new Ocorrencia(ana, TipoOcorrencia.ASSALTO, null, meio.latitude(), meio.longitude()));
        Coordenada outro = direta.get(10);
        incidentes.save(new Incidente("grave", CategoriaIncidente.ACIDENTE, 4, "Av. Paulista", null, 300,
                outro.latitude(), outro.longitude()));
    }

    private static String corpo(String modo) {
        return """
                {"origem":{"latitude":-23.561,"longitude":-46.656},
                 "destino":{"latitude":-23.550,"longitude":-46.633}%s}""".formatted(
                modo == null ? "" : ",\"modo\":\"" + modo + "\"");
    }

    @Test void avaliaSemTokenEOrdenaDaMaisSeguraParaAPior() throws Exception {
        when(tomTom.calcularRotas(any(), any(), any())).thenReturn(List.of(
                new RotaTomTom(0, 3000, 600, 120, direta),
                new RotaTomTom(1, 4200, 780, 0, desvio)));

        mvc.perform(post("/rotas/avaliar").contentType(MediaType.APPLICATION_JSON).content(corpo("CARRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].indice").value(1))
                .andExpect(jsonPath("$[0].classificacao").value("SEGURA"))
                .andExpect(jsonPath("$[1].indice").value(0))
                .andExpect(jsonPath("$[1].resumo.assaltosRecentes").value(1))
                .andExpect(jsonPath("$[1].resumo.incidentesGraves").value(1))
                .andExpect(jsonPath("$[1].alertas[0]").value("1 assalto registrado no trajeto nas últimas 24 h"))
                .andExpect(jsonPath("$[1].alertas[1]").value("Acidente em Av. Paulista"))
                .andExpect(jsonPath("$[1].pontos.length()").value(51))
                .andExpect(jsonPath("$[1].distanciaMetros").value(3000))
                .andExpect(jsonPath("$[1].atrasoTransitoSegundos").value(120));
    }

    @Test void modoEOpcional() throws Exception {
        when(tomTom.calcularRotas(any(), any(), any())).thenReturn(List.of(new RotaTomTom(0, 4200, 780, 0, desvio)));
        mvc.perform(post("/rotas/avaliar").contentType(MediaType.APPLICATION_JSON).content(corpo(null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].classificacao").value("SEGURA"));
    }

    @Test void foraDaGrandeSaoPauloDa422() throws Exception {
        String rio = """
                {"origem":{"latitude":-22.90,"longitude":-43.17},"destino":{"latitude":-22.95,"longitude":-43.20}}""";
        mvc.perform(post("/rotas/avaliar").contentType(MediaType.APPLICATION_JSON).content(rio))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test void semDestinoDa400() throws Exception {
        mvc.perform(post("/rotas/avaliar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origem\":{\"latitude\":-23.56,\"longitude\":-46.65}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.destino").exists());
    }

    @Test void modoInvalidoDa400() throws Exception {
        mvc.perform(post("/rotas/avaliar").contentType(MediaType.APPLICATION_JSON).content(corpo("AVIAO")))
                .andExpect(status().isBadRequest());
    }

    @Test void tomTomForaDoArDa503() throws Exception {
        when(tomTom.calcularRotas(any(), any(), any()))
                .thenThrow(ApiException.indisponivel("Não foi possível calcular as rotas agora."));
        String outro = """
                {"origem":{"latitude":-23.600,"longitude":-46.660},"destino":{"latitude":-23.610,"longitude":-46.680}}""";
        mvc.perform(post("/rotas/avaliar").contentType(MediaType.APPLICATION_JSON).content(outro))
                .andExpect(status().isServiceUnavailable());
    }
}
