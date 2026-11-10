package com.safemove.incidente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.safemove.web.ApiException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TomTomClientTest {

    private static final String RESPOSTA = """
            {"incidents":[
              {"geometry":{"type":"Point","coordinates":[-46.65,-23.56]},
               "properties":{"id":"A1","iconCategory":1,"magnitudeOfDelay":3,"delay":240,
                 "events":[{"description":"Colisão"}],"from":"Av. Paulista","to":"Consolação","roadNumbers":[]}},
              {"geometry":{"type":"LineString","coordinates":[[-46.60,-23.50],[-46.61,-23.51],[-46.62,-23.52]]},
               "properties":{"id":"B2","iconCategory":6,"magnitudeOfDelay":9,"roadNumbers":["SP-015","SP-070"]}},
              {"geometry":{"type":"Point","coordinates":[-46.7,-23.6]},"properties":{"iconCategory":9}},
              {"geometry":{"type":"Point","coordinates":[]},"properties":{"id":"C3","iconCategory":9}}
            ]}""";

    private MockRestServiceServer server;
    private TomTomClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TomTomClient(builder, "CHAVE");
    }

    @Test void buscaNoBboxDeSaoPauloEConverteOsIncidentes() {
        server.expect(requestTo(Matchers.startsWith("https://api.tomtom.com/traffic/services/5/incidentDetails?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("key", "CHAVE"))
                .andExpect(queryParam("bbox", "-46.83%2C-24.01%2C-46.36%2C-23.36"))
                .andExpect(queryParam("language", "pt-PT"))
                .andRespond(withSuccess(RESPOSTA, MediaType.APPLICATION_JSON));

        var lista = client.buscarIncidentes();

        assertEquals(2, lista.size()); // sem id e sem posição são descartados
        var a = lista.get(0);
        assertEquals("A1", a.idTomtom());
        assertEquals(CategoriaIncidente.ACIDENTE, a.categoria());
        assertEquals(3, a.gravidade());
        assertEquals("Av. Paulista → Consolação", a.via());
        assertEquals("Colisão", a.descricao());
        assertEquals(240, a.atrasoSegundos());
        assertEquals(-23.56, a.latitude());
        assertEquals(-46.65, a.longitude());

        var b = lista.get(1);
        assertEquals(CategoriaIncidente.CONGESTIONAMENTO, b.categoria());
        assertEquals(4, b.gravidade()); // 9 é limitado a 4
        assertEquals("SP-015, SP-070", b.via());
        assertNull(b.atrasoSegundos());
        assertEquals(-23.51, b.latitude()); // ponto do meio do trecho
        assertEquals(-46.61, b.longitude());
        server.verify();
    }

    @Test void respostaSemIncidentesDevolveListaVazia() {
        server.expect(requestTo(Matchers.startsWith("https://api.tomtom.com/"))).andRespond(
                withSuccess("{\"incidents\":[]}", MediaType.APPLICATION_JSON));
        assertEquals(0, client.buscarIncidentes().size());
    }

    @Test void categoriasDesconhecidasViramDesconhecido() {
        assertEquals(CategoriaIncidente.DESCONHECIDO, TomTomClient.categoria(0));
        assertEquals(CategoriaIncidente.DESCONHECIDO, TomTomClient.categoria(99));
        assertEquals(CategoriaIncidente.VEICULO_QUEBRADO, TomTomClient.categoria(14));
    }

    @Test void erroDaTomTomViraIndisponivel() {
        server.expect(requestTo(Matchers.startsWith("https://api.tomtom.com/"))).andRespond(withStatus(HttpStatus.FORBIDDEN));
        ApiException e = assertThrows(ApiException.class, () -> client.buscarIncidentes());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }

    @Test void falhaDoServidorViraIndisponivel() {
        server.expect(requestTo(Matchers.startsWith("https://api.tomtom.com/"))).andRespond(withServerError());
        ApiException e = assertThrows(ApiException.class, () -> client.buscarIncidentes());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }

    @Test void semChaveNaoChamaATomTom() {
        var semChave = new TomTomClient(RestClient.builder(), "");
        ApiException e = assertThrows(ApiException.class, semChave::buscarIncidentes);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }
}
