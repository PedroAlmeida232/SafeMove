package com.safemove.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safemove.dao.ConfirmacaoDAO;
import com.safemove.dao.OcorrenciaDAO;
import com.safemove.dao.UsuarioDAO;
import com.safemove.exception.ApiException;
import com.safemove.integracao.FirebaseAuthClient;
import com.safemove.model.Confirmacao;
import com.safemove.model.Ocorrencia;
import com.safemove.model.TipoOcorrencia;
import com.safemove.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("dev")
@SpringBootTest(properties = "safemove.incidentes.atraso-inicial-ms=3600000")
@AutoConfigureMockMvc
class UsuarioControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioDAO usuarios;
    @Autowired OcorrenciaDAO ocorrencias;
    @Autowired ConfirmacaoDAO confirmacoes;
    @MockitoBean FirebaseAuthClient firebase;

    @BeforeEach void limpar() {
        confirmacoes.deleteAll();
        ocorrencias.deleteAll();
        usuarios.deleteAll();
    }

    private static JwtRequestPostProcessor ana() {
        return jwt().jwt(j -> j.tokenValue("tk-ana").subject("uid-ana").claim("email", "Ana@X.com").claim("name", "Ana"));
    }

    @Test void meCriaOPerfilAPartirDoToken() throws Exception {
        mvc.perform(get("/usuarios/me").with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid-ana"))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@x.com"));
    }

    @Test void putTrocaONomeAquiENoFirebase() throws Exception {
        mvc.perform(put("/usuarios/me").with(ana()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  Ana Paula \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Paula"));
        assertEquals("Ana Paula", usuarios.findById("uid-ana").orElseThrow().getNome());
        verify(firebase).definirNome("tk-ana", "Ana Paula");
    }

    @Test void putSemNomeDa400() throws Exception {
        mvc.perform(put("/usuarios/me").with(ana()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").value("Informe seu nome."));
    }

    @Test void deleteApagaPerfilOcorrenciasEConfirmacoes() throws Exception {
        Usuario ana = usuarios.save(new Usuario("uid-ana", "Ana", "ana@x.com"));
        Usuario bia = usuarios.save(new Usuario("uid-bia", "Bia", "bia@x.com"));
        Ocorrencia daAna = ocorrencias.save(new Ocorrencia(ana, TipoOcorrencia.BURACO, null, -23.55, -46.63));
        Ocorrencia daBia = ocorrencias.save(new Ocorrencia(bia, TipoOcorrencia.ASSALTO, null, -23.55, -46.63));
        confirmacoes.save(new Confirmacao(daAna, bia));
        confirmacoes.save(new Confirmacao(daBia, ana));

        mvc.perform(delete("/usuarios/me").with(ana())).andExpect(status().isNoContent());

        assertFalse(usuarios.existsById("uid-ana"));
        assertFalse(ocorrencias.existsById(daAna.getId()));
        assertTrue(ocorrencias.existsById(daBia.getId()));
        assertEquals(0, confirmacoes.count());
        verify(firebase).excluirConta("tk-ana");
    }

    @Test void seOFirebaseRecusaNadaEApagado() throws Exception {
        usuarios.save(new Usuario("uid-ana", "Ana", "ana@x.com"));
        doThrow(ApiException.naoAutenticado("Por segurança, entre novamente antes de continuar."))
                .when(firebase).excluirConta("tk-ana");

        mvc.perform(delete("/usuarios/me").with(ana()))
                .andExpect(status().isUnauthorized());
        assertTrue(usuarios.existsById("uid-ana"));
    }
}
