package com.safemove.controller;

import com.safemove.bo.IncidenteBO;
import com.safemove.dto.IncidenteResponse;
import com.safemove.model.CategoriaIncidente;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Somente leitura: os incidentes vêm da TomTom e são trocados a cada sincronização. */
@RestController
@RequestMapping("/incidentes")
public class IncidenteController {

    private final IncidenteBO bo;

    public IncidenteController(IncidenteBO bo) {
        this.bo = bo;
    }

    /** Com lat e lng: incidentes num raio (padrão 5.000 m), do mais perto ao mais longe. Sem eles: todos. */
    @GetMapping
    public List<IncidenteResponse> listar(@RequestParam(required = false) Double lat,
                                          @RequestParam(required = false) Double lng,
                                          @RequestParam(required = false) Double raio,
                                          @RequestParam(required = false) CategoriaIncidente categoria) {
        return bo.listar(lat, lng, raio, categoria);
    }

    @GetMapping("/{id}")
    public IncidenteResponse buscar(@PathVariable Long id) {
        return bo.buscar(id);
    }
}
