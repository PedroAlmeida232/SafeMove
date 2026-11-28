package com.safemove.controller;

import com.safemove.bo.RotaBO;
import com.safemove.dto.RotaDtos.AvaliarRotaRequest;
import com.safemove.dto.RotaDtos.RotaAvaliada;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Avaliação de rotas. Pública: o app avalia rotas mesmo sem login. */
@RestController
@RequestMapping("/rotas")
public class RotaController {

    private final RotaBO bo;

    public RotaController(RotaBO bo) {
        this.bo = bo;
    }

    /** Calcula até 3 rotas na TomTom e devolve cada uma com nota de segurança, da melhor para a pior. */
    @PostMapping("/avaliar")
    public List<RotaAvaliada> avaliar(@Valid @RequestBody AvaliarRotaRequest req) {
        return bo.avaliar(req);
    }
}
