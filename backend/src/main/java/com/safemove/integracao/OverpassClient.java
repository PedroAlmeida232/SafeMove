package com.safemove.integracao;

import com.safemove.exception.ApiException;
import com.safemove.geo.Geo;
import com.safemove.geo.Retangulo;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * Busca radares de velocidade no OpenStreetMap pela Overpass API (gratuita, sem chave).
 * Os dados são © colaboradores do OpenStreetMap, licença ODbL: o app precisa exibir esse crédito.
 */
@Component
public class OverpassClient {

    private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);
    private static final String URL = "https://overpass-api.de/api/interpreter";
    /** A política de uso da Overpass pede um User-Agent que identifique o app. */
    private static final String USER_AGENT = "SafeMove/1.0 (projeto academico FIAP; github.com/PedroAlmeida232/SafeMove)";
    private static final Pattern NUMERO = Pattern.compile("^\\s*(\\d{1,3})");

    private final RestClient rest;

    @Autowired
    public OverpassClient() {
        this(RestClient.builder());
    }

    /** Usado nos testes, para trocar o servidor por um simulado. */
    OverpassClient(RestClient.Builder builder) {
        this.rest = builder.defaultHeader("User-Agent", USER_AGENT).build();
    }

    public List<RadarOsm> buscarRadares() {
        return buscarRadares(Geo.GRANDE_SAO_PAULO);
    }

    public List<RadarOsm> buscarRadares(Retangulo area) {
        // Overpass espera (sul, oeste, norte, leste).
        String consulta = "[out:json][timeout:60];node[\"highway\"=\"speed_camera\"](%s,%s,%s,%s);out body;"
                .formatted(area.minLatitude(), area.minLongitude(), area.maxLatitude(), area.maxLongitude());
        JsonNode corpo;
        try {
            corpo = rest.get().uri(URL + "?data={q}", consulta).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Falha ao buscar radares na Overpass", e);
            throw ApiException.indisponivel("Não foi possível obter os radares agora.");
        }
        return converter(corpo);
    }

    static List<RadarOsm> converter(JsonNode corpo) {
        List<RadarOsm> lista = new ArrayList<>();
        if (corpo == null) {
            return lista;
        }
        for (JsonNode e : corpo.path("elements")) {
            if (!"node".equals(e.path("type").asText()) || !e.path("id").isNumber()
                    || !e.path("lat").isNumber() || !e.path("lon").isNumber()) {
                continue;
            }
            double lat = e.get("lat").asDouble();
            double lon = e.get("lon").asDouble();
            if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                continue;
            }
            lista.add(new RadarOsm(e.get("id").asLong(), lat, lon, velocidade(e.path("tags"))));
        }
        return lista;
    }

    /** maxspeed pode vir como "50", "50 km/h" ou "BR:urban"; só aproveitamos o número. */
    static Integer velocidade(JsonNode tags) {
        JsonNode valor = tags.path("maxspeed");
        if (valor.isMissingNode() || valor.isNull()) {
            return null;
        }
        Matcher m = NUMERO.matcher(valor.asText());
        if (!m.find()) {
            return null;
        }
        int kmh = Integer.parseInt(m.group(1));
        return kmh >= 10 && kmh <= 150 ? kmh : null;
    }
}
