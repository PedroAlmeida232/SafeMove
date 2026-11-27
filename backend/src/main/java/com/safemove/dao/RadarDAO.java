package com.safemove.dao;

import com.safemove.model.Radar;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RadarDAO extends JpaRepository<Radar, Long> {
    Optional<Radar> findByIdOsm(Long idOsm);

    /** Pré-filtro pelo retângulo; o raio exato é refinado depois com Haversine. */
    List<Radar> findByLatitudeBetweenAndLongitudeBetween(double minLat, double maxLat, double minLng, double maxLng);
}
