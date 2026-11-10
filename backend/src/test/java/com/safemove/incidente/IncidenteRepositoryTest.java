package com.safemove.incidente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest
class IncidenteRepositoryTest {

    @Autowired IncidenteRepository repo;

    @Test void gravaEBuscaPeloIdDaTomtom() {
        repo.saveAndFlush(new Incidente("tt-1", CategoriaIncidente.ACIDENTE, 3, "Av. Paulista", "Colisão", 240, -23.56, -46.65));
        var i = repo.findByIdTomtom("tt-1").orElseThrow();
        assertEquals(CategoriaIncidente.ACIDENTE, i.getCategoria());
        assertEquals(3, i.getGravidade());
        assertEquals(240, i.getAtrasoSegundos());
        assertEquals(-23.56, i.getLatitude());
        assertEquals("-03:00", i.getAtualizadoEm().getOffset().toString());
    }

    @Test void atualizarMantemOMesmoRegistro() {
        repo.saveAndFlush(new Incidente("tt-2", CategoriaIncidente.OBRAS, 1, null, null, null, 0, 0));
        var i = repo.findByIdTomtom("tt-2").orElseThrow();
        i.atualizar(CategoriaIncidente.CONGESTIONAMENTO, 2, "Marginal", "Lento", 60, 1, 2);
        repo.saveAndFlush(i);
        assertEquals(CategoriaIncidente.CONGESTIONAMENTO, repo.findByIdTomtom("tt-2").orElseThrow().getCategoria());
        assertTrue(repo.findAll().stream().filter(x -> x.getIdTomtom().equals("tt-2")).count() == 1);
    }

    @Test void idDaTomtomNaoRepete() {
        repo.saveAndFlush(new Incidente("tt-3", CategoriaIncidente.PERIGO, 0, null, null, null, 0, 0));
        assertThrows(DataIntegrityViolationException.class,
                () -> repo.saveAndFlush(new Incidente("tt-3", CategoriaIncidente.CHUVA, 0, null, null, null, 0, 0)));
    }
}
