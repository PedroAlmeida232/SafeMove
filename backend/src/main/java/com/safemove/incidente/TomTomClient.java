package com.safemove.incidente;

import tools.jackson.databind.JsonNode;
import com.safemove.geo.Retangulo;
import com.safemove.web.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Busca incidentes de trânsito na TomTom (Traffic Incident Details v5). A chave fica só no backend. */
@Component
public class TomTomClient {

    private static final Logger log = LoggerFactory.getLogger(TomTomClient.class);
    private static final String URL = "https://api.tomtom.com/traffic/services/5/incidentDetails";
    private static final String CAMPOS = "{incidents{geometry{type,coordinates},properties{id,iconCategory,"
            + "magnitudeOfDelay,events{description},from,to,roadNumbers,delay}}}";

    /** Grande São Paulo. A TomTom aceita no máximo ~10.000 km² por consulta; este retângulo tem ~3.300 km². */
    public static final Retangulo SAO_PAULO = new Retangulo(-24.01, -23.36, -46.83, -46.36);

    private static final int MAX_VIA = 255;
    private static final int MAX_DESCRICAO = 500;

    private final RestClient rest;
    private final String apiKey;

    @Autowired
    public TomTomClient(@Value("${safemove.tomtom.api-key}") String apiKey) {
        this(RestClient.builder(), apiKey);
    }

    /** Usado nos testes, para trocar o servidor por um simulado. */
    TomTomClient(RestClient.Builder builder, String apiKey) {
        this.rest = builder.build();
        this.apiKey = apiKey;
    }

    public List<IncidenteTomTom> buscarIncidentes() {
        return buscarIncidentes(SAO_PAULO);
    }

    public List<IncidenteTomTom> buscarIncidentes(Retangulo area) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.indisponivel("Incidentes indisponíveis: preencha TOMTOM_API_KEY no backend/.env.");
        }
        // A TomTom espera minLon,minLat,maxLon,maxLat.
        String bbox = "%s,%s,%s,%s".formatted(area.minLongitude(), area.minLatitude(),
                area.maxLongitude(), area.maxLatitude());
        JsonNode corpo;
        try {
            corpo = rest.get()
                    .uri(URL + "?key={k}&bbox={b}&fields={f}&language=pt-PT&timeValidityFilter=present",
                            apiKey, bbox, CAMPOS)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Falha ao buscar incidentes na TomTom", e);
            throw ApiException.indisponivel("Não foi possível obter os incidentes de trânsito agora.");
        }
        return converter(corpo);
    }

    static List<IncidenteTomTom> converter(JsonNode corpo) {
        List<IncidenteTomTom> lista = new ArrayList<>();
        if (corpo == null) {
            return lista;
        }
        for (JsonNode inc : corpo.path("incidents")) {
            JsonNode p = inc.path("properties");
            double[] ponto = ponto(inc.path("geometry"));
            String id = p.path("id").asText("");
            if (id.isBlank() || ponto == null) {
                continue; // sem id ou sem posição não dá para guardar
            }
            lista.add(new IncidenteTomTom(
                    id,
                    categoria(p.path("iconCategory").asInt(0)),
                    Math.max(0, Math.min(4, p.path("magnitudeOfDelay").asInt(0))),
                    via(p),
                    cortar(texto(p.path("events").path(0).path("description")), MAX_DESCRICAO),
                    p.hasNonNull("delay") ? p.get("delay").asInt() : null,
                    ponto[1], ponto[0]));
        }
        return lista;
    }

    /** GeoJSON da TomTom é [lon, lat]. Point usa o próprio ponto; LineString, o ponto do meio do trecho. */
    private static double[] ponto(JsonNode geometria) {
        JsonNode c = geometria.path("coordinates");
        if ("LineString".equals(geometria.path("type").asText())) {
            if (!c.isArray() || c.isEmpty()) {
                return null;
            }
            c = c.get(c.size() / 2);
        }
        if (!c.isArray() || c.size() < 2 || !c.get(0).isNumber() || !c.get(1).isNumber()) {
            return null;
        }
        double lon = c.get(0).asDouble();
        double lat = c.get(1).asDouble();
        return Math.abs(lat) <= 90 && Math.abs(lon) <= 180 ? new double[] {lon, lat} : null;
    }

    private static String via(JsonNode p) {
        List<String> numeros = new ArrayList<>();
        p.path("roadNumbers").forEach(n -> numeros.add(n.asText()));
        String de = texto(p.path("from"));
        String ate = texto(p.path("to"));
        String via = !numeros.isEmpty() ? String.join(", ", numeros)
                : de != null && ate != null ? de + " → " + ate
                : de != null ? de : ate;
        return cortar(via, MAX_VIA);
    }

    private static String texto(JsonNode n) {
        String t = n.isMissingNode() || n.isNull() ? null : n.asText().trim();
        return t == null || t.isEmpty() ? null : t;
    }

    private static String cortar(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    /** iconCategory da TomTom: 0 desconhecido, 1 acidente, 2 neblina, 3 perigo, 4 chuva, 5 gelo, 6 congestionamento,
     *  7 faixa interditada, 8 via interditada, 9 obras, 10 vento, 11 alagamento, 14 veículo quebrado. */
    static CategoriaIncidente categoria(int icone) {
        return switch (icone) {
            case 1 -> CategoriaIncidente.ACIDENTE;
            case 2 -> CategoriaIncidente.NEBLINA;
            case 3 -> CategoriaIncidente.PERIGO;
            case 4 -> CategoriaIncidente.CHUVA;
            case 5 -> CategoriaIncidente.GELO;
            case 6 -> CategoriaIncidente.CONGESTIONAMENTO;
            case 7 -> CategoriaIncidente.FAIXA_INTERDITADA;
            case 8 -> CategoriaIncidente.VIA_INTERDITADA;
            case 9 -> CategoriaIncidente.OBRAS;
            case 10 -> CategoriaIncidente.VENTO;
            case 11 -> CategoriaIncidente.ALAGAMENTO;
            case 14 -> CategoriaIncidente.VEICULO_QUEBRADO;
            default -> CategoriaIncidente.DESCONHECIDO;
        };
    }
}
