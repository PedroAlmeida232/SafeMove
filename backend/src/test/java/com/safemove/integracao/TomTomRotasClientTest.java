package com.safemove.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.safemove.exception.ApiException;
import com.safemove.geo.Coordenada;
import com.safemove.model.ModoTransporte;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TomTomRotasClientTest {

    private static final Coordenada ORIGEM = new Coordenada(-23.561, -46.656);
    private static final Coordenada DESTINO = new Coordenada(-23.550, -46.633);

    private static final String RESPOSTA = """
            {"routes":[
              {"summary":{"lengthInMeters":3200,"travelTimeInSeconds":720,"trafficDelayInSeconds":120},
               "legs":[{"points":[{"latitude":-23.561,"longitude":-46.656},{"latitude":-23.555,"longitude":-46.645},
                                  {"latitude":-23.550,"longitude":-46.633}]}]},
              {"summary":{"lengthInMeters":3900,"travelTimeInSeconds":800,"trafficDelayInSeconds":0},
               "legs":[{"points":[{"latitude":-23.561,"longitude":-46.656},{"latitude":-23.550,"longitude":-46.633}]}]},
              {"summary":{"lengthInMeters":10},"legs":[{"points":[{"latitude":-23.561,"longitude":-46.656}]}]}
            ]}""";

    private MockRestServiceServer server;
    private TomTomRotasClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TomTomRotasClient(builder, "CHAVE");
    }

    @Test void calculaRotasComAlternativasEConverte() {
        server.expect(requestTo(Matchers.startsWith(
                        "https://api.tomtom.com/routing/1/calculateRoute/-23.561,-46.656:-23.55,-46.633/json?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("key", "CHAVE"))
                .andExpect(queryParam("travelMode", "pedestrian"))
                .andExpect(queryParam("maxAlternatives", "2"))
                .andRespond(withSuccess(RESPOSTA, MediaType.APPLICATION_JSON));

        var rotas = client.calcularRotas(ORIGEM, DESTINO, ModoTransporte.A_PE);

        assertEquals(2, rotas.size()); // a terceira tem só 1 ponto e é descartada
        assertEquals(0, rotas.get(0).indice());
        assertEquals(3200, rotas.get(0).distanciaMetros());
        assertEquals(720, rotas.get(0).duracaoSegundos());
        assertEquals(120, rotas.get(0).atrasoSegundos());
        assertEquals(3, rotas.get(0).pontos().size());
        assertEquals(1, rotas.get(1).indice());
        server.verify();
    }

    @Test void semCaminhoViraNaoProcessavel() {
        server.expect(requestTo(Matchers.startsWith("https://api.tomtom.com/")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        ApiException e = assertThrows(ApiException.class, () -> client.calcularRotas(ORIGEM, DESTINO, ModoTransporte.CARRO));
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, e.getStatus());
    }

    @Test void chaveRecusadaViraIndisponivel() {
        server.expect(requestTo(Matchers.startsWith("https://api.tomtom.com/"))).andRespond(withStatus(HttpStatus.FORBIDDEN));
        ApiException e = assertThrows(ApiException.class, () -> client.calcularRotas(ORIGEM, DESTINO, ModoTransporte.CARRO));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }

    @Test void falhaDoServidorViraIndisponivel() {
        server.expect(requestTo(Matchers.startsWith("https://api.tomtom.com/"))).andRespond(withServerError());
        ApiException e = assertThrows(ApiException.class, () -> client.calcularRotas(ORIGEM, DESTINO, ModoTransporte.CARRO));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }

    @Test void semChaveNaoChamaATomTom() {
        var semChave = new TomTomRotasClient(RestClient.builder(), " ");
        ApiException e = assertThrows(ApiException.class,
                () -> semChave.calcularRotas(ORIGEM, DESTINO, ModoTransporte.CARRO));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }
}
