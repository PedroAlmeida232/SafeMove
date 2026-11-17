package com.safemove.dao;

import com.safemove.model.Ocorrencia;
import com.safemove.model.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcorrenciaDAO extends JpaRepository<Ocorrencia, Long> {

    /** Pré-filtro pelo retângulo e pela data; o raio exato é refinado depois com Haversine. */
    List<Ocorrencia> findByLatitudeBetweenAndLongitudeBetweenAndCriadoEmGreaterThanEqual(
            double minLat, double maxLat, double minLng, double maxLng, OffsetDateTime desde);

    List<Ocorrencia> findByAutor(Usuario autor);
}
