package com.safemove.controller;

import com.safemove.bo.RadarBO;
import com.safemove.dto.RadarResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Somente leitura: os radares vêm do OpenStreetMap e são atualizados uma vez por dia. */
@RestController
@RequestMapping("/radares")
public class RadarController {

    private final RadarBO bo;

    public RadarController(RadarBO bo) {
        this.bo = bo;
    }

    /** Com lat e lng: radares num raio (padrão 2.000 m), do mais perto ao mais longe. Sem eles: todos. */
    @GetMapping
    public List<RadarResponse> listar(@RequestParam(required = false) Double lat,
                                      @RequestParam(required = false) Double lng,
                                      @RequestParam(required = false) Double raio) {
        return bo.listar(lat, lng, raio);
    }
}
