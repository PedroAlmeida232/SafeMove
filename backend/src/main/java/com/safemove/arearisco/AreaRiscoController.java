package com.safemove.arearisco;

import com.safemove.geo.Coordenada;
import com.safemove.web.ApiException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/areas-risco")
public class AreaRiscoController {

    private final AreaRiscoService service;

    public AreaRiscoController(AreaRiscoService service) {
        this.service = service;
    }

    /** Risco de todos os distritos de São Paulo, do mais arriscado ao menos arriscado. */
    @GetMapping
    public List<AreaRiscoResponse> listar() {
        return service.listar();
    }

    /**
     * Diz se a região do ponto é segura e devolve o texto do alerta. O app chama com a posição do
     * usuário e, se {@code area.segura} for falso, mostra o alerta. 404 se o ponto estiver fora de São Paulo.
     */
    @GetMapping("/avaliar")
    public AvaliacaoAreaResponse avaliar(@RequestParam Double lat, @RequestParam Double lng) {
        Coordenada ponto;
        try {
            ponto = new Coordenada(lat, lng);
        } catch (IllegalArgumentException e) {
            throw ApiException.requisicaoInvalida(e.getMessage());
        }
        return service.avaliar(ponto)
                .orElseThrow(() -> ApiException.naoEncontrado("Não há dados de risco para este local."));
    }
}
