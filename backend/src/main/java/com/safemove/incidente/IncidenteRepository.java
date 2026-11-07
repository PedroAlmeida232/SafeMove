package com.safemove.incidente;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {
    Optional<Incidente> findByIdTomtom(String idTomtom);
}
