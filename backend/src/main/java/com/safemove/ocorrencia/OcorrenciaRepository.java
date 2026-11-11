package com.safemove.ocorrencia;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    /** Pré-filtro pelo retângulo e pela data; o raio exato é refinado depois com Haversine. */
    List<Ocorrencia> findByLatitudeBetweenAndLongitudeBetweenAndCriadoEmGreaterThanEqual(
            double minLat, double maxLat, double minLng, double maxLng, OffsetDateTime desde);

    /** Usado no cálculo das áreas de risco (ex.: assaltos dos últimos 30 dias). */
    List<Ocorrencia> findByTipoAndCriadoEmGreaterThanEqual(TipoOcorrencia tipo, OffsetDateTime desde);
}
