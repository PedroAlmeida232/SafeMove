package com.safemove.controller;

import com.safemove.bo.AreaRiscoBO;
import com.safemove.dto.AreaRiscoResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Somente leitura: o nível de cada distrito é calculado a partir das ocorrências dos últimos 30 dias. */
@RestController
@RequestMapping("/areas-risco")
public class AreaRiscoController {

    private final AreaRiscoBO bo;

    public AreaRiscoController(AreaRiscoBO bo) {
        this.bo = bo;
    }

    @GetMapping
    public List<AreaRiscoResponse> listar() {
        return bo.listar();
    }
}
