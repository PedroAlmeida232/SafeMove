package com.safemove.controller;

import com.safemove.bo.OcorrenciaBO;
import com.safemove.dto.OcorrenciaRequest;
import com.safemove.dto.OcorrenciaResponse;
import com.safemove.model.TipoOcorrencia;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaBO bo;

    public OcorrenciaController(OcorrenciaBO bo) {
        this.bo = bo;
    }

    /** Ocorrências num raio (padrão 2.000 m) nas últimas horas (padrão 24), das mais recentes às mais antigas. */
    @GetMapping
    public List<OcorrenciaResponse> listar(@RequestParam Double lat,
                                           @RequestParam Double lng,
                                           @RequestParam(required = false) Double raio,
                                           @RequestParam(required = false) Integer horas,
                                           @RequestParam(required = false) TipoOcorrencia tipo) {
        return bo.listar(lat, lng, raio, horas, tipo);
    }

    @GetMapping("/{id}")
    public OcorrenciaResponse buscar(@PathVariable Long id) {
        return bo.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OcorrenciaResponse criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OcorrenciaRequest req) {
        return bo.criar(jwt, req);
    }

    @PutMapping("/{id}")
    public OcorrenciaResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                        @Valid @RequestBody OcorrenciaRequest req) {
        return bo.atualizar(jwt, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        bo.excluir(jwt, id);
    }

    @PostMapping("/{id}/confirmar")
    public OcorrenciaResponse confirmar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bo.confirmar(jwt, id);
    }
}
