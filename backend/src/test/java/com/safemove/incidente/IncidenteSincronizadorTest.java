package com.safemove.incidente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.safemove.web.ApiException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("dev")
@SpringBootTest(properties = "safemove.incidentes.atraso-inicial-ms=3600000")
class IncidenteSincronizadorTest {

    @Autowired IncidenteSincronizador sincronizador;
    @Autowired IncidenteRepository repo;
    @MockitoBean TomTomClient tomTom;

    @BeforeEach void limpar() {
        repo.deleteAll();
    }

    private static IncidenteTomTom inc(String id, CategoriaIncidente c) {
        return new IncidenteTomTom(id, c, 2, "Via " + id, "desc", 60, -23.5, -46.6);
    }

    private Set<String> ids() {
        return repo.findAll().stream().map(Incidente::getIdTomtom).collect(Collectors.toSet());
    }

    @Test void trocaOsAntigosPelosNovos() {
        when(tomTom.buscarIncidentes()).thenReturn(List.of(inc("a", CategoriaIncidente.ACIDENTE), inc("b", CategoriaIncidente.OBRAS)));
        sincronizador.sincronizar();
        assertEquals(Set.of("a", "b"), ids());

        // "a" continua (e muda), "b" some, "c" é novo
        when(tomTom.buscarIncidentes()).thenReturn(List.of(inc("a", CategoriaIncidente.CONGESTIONAMENTO), inc("c", CategoriaIncidente.CHUVA)));
        sincronizador.sincronizar();
        assertEquals(Set.of("a", "c"), ids());
        assertEquals(CategoriaIncidente.CONGESTIONAMENTO, repo.findByIdTomtom("a").orElseThrow().getCategoria());
    }

    @Test void mantemOsAntigosSeATomTomFalhar() {
        when(tomTom.buscarIncidentes()).thenReturn(List.of(inc("a", CategoriaIncidente.ACIDENTE)));
        sincronizador.sincronizar();

        when(tomTom.buscarIncidentes()).thenThrow(ApiException.indisponivel("fora do ar"));
        sincronizador.sincronizar();
        assertEquals(Set.of("a"), ids());
    }

    @Test void respostaVaziaDaTomTomLimpaOsIncidentes() {
        when(tomTom.buscarIncidentes()).thenReturn(List.of(inc("a", CategoriaIncidente.ACIDENTE)));
        sincronizador.sincronizar();
        when(tomTom.buscarIncidentes()).thenReturn(List.of());
        sincronizador.sincronizar();
        assertTrue(repo.findAll().isEmpty());
    }
}
