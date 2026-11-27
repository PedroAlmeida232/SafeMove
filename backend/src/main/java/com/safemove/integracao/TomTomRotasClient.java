package com.safemove.integracao;

import com.safemove.exception.ApiException;
import com.safemove.geo.Coordenada;
import com.safemove.model.ModoTransporte;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/** Calcula rotas com trânsito em tempo real na TomTom (Routing API v1), com até 2 alternativas. */
@Component
public class TomTomRotasClient {

    private static final Logger log = LoggerFactory.getLogger(TomTomRotasClient.class);
    private static final String URL = "https://api.tomtom.com/routing/1/calculateRoute/";
    private static final String PARAMETROS = "/json?key={k}&maxAlternatives=2&travelMode={modo}&traffic=true"
            + "&routeRepresentation=polyline&computeTravelTimeFor=all";

    private final RestClient rest;
    private final String apiKey;

    @Autowired
    public TomTomRotasClient(@Value("${safemove.tomtom.api-key}") String apiKey) {
        this(RestClient.builder(), apiKey);
    }

    /** Usado nos testes, para trocar o servidor por um simulado. */
    TomTomRotasClient(RestClient.Builder builder, String apiKey) {
        this.rest = builder.build();
        this.apiKey = apiKey;
    }

    public List<RotaTomTom> calcularRotas(Coordenada origem, Coordenada destino, ModoTransporte modo) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.indisponivel("Rotas indisponíveis: preencha TOMTOM_API_KEY no backend/.env.");
        }
        // lat,lng:lat,lng vai direto no caminho (só números), sem codificar ':' e ','.
        String locais = origem.latitude() + "," + origem.longitude() + ":" + destino.latitude() + "," + destino.longitude();
        JsonNode corpo;
        try {
            corpo = rest.get().uri(URL + locais + PARAMETROS, apiKey, modo.tomTom()).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 400) {
                // ex.: NO_ROUTE_FOUND (origem ou destino sem acesso por via)
                throw ApiException.naoProcessavel("Não encontramos um caminho entre esses pontos.");
            }
            log.error("TomTom Routing respondeu {}", e.getStatusCode(), e);
            throw ApiException.indisponivel("Não foi possível calcular as rotas agora.");
        } catch (RestClientException e) {
            log.error("Falha ao calcular rotas na TomTom", e);
            throw ApiException.indisponivel("Não foi possível calcular as rotas agora.");
        }
        return converter(corpo);
    }

    static List<RotaTomTom> converter(JsonNode corpo) {
        List<RotaTomTom> rotas = new ArrayList<>();
        if (corpo == null) {
            return rotas;
        }
        int indice = 0;
        for (JsonNode r : corpo.path("routes")) {
            JsonNode resumo = r.path("summary");
            List<Coordenada> pontos = new ArrayList<>();
            for (JsonNode perna : r.path("legs")) {
                for (JsonNode p : perna.path("points")) {
                    if (p.path("latitude").isNumber() && p.path("longitude").isNumber()) {
                        pontos.add(new Coordenada(p.get("latitude").asDouble(), p.get("longitude").asDouble()));
                    }
                }
            }
            if (pontos.size() >= 2) {
                rotas.add(new RotaTomTom(indice, resumo.path("lengthInMeters").asInt(0),
                        resumo.path("travelTimeInSeconds").asInt(0), resumo.path("trafficDelayInSeconds").asInt(0),
                        pontos));
            }
            indice++;
        }
        return rotas;
    }
}
