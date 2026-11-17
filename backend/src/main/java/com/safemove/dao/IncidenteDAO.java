package com.safemove.dao;

import com.safemove.model.Incidente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidenteDAO extends JpaRepository<Incidente, Long> {
    Optional<Incidente> findByIdTomtom(String idTomtom);

    /** Pré-filtro pelo retângulo; o raio exato é refinado depois com Haversine. */
    List<Incidente> findByLatitudeBetweenAndLongitudeBetween(double minLat, double maxLat, double minLng, double maxLng);
}
