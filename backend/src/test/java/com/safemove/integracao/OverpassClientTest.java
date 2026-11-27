package com.safemove.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.safemove.exception.ApiException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OverpassClientTest {

    private static final String RESPOSTA = """
            {"elements":[
              {"type":"node","id":101,"lat":-23.56,"lon":-46.65,"tags":{"highway":"speed_camera","maxspeed":"50"}},
              {"type":"node","id":102,"lat":-23.57,"lon":-46.66,"tags":{"highway":"speed_camera","maxspeed":"60 km/h"}},
              {"type":"node","id":103,"lat":-23.58,"lon":-46.67,"tags":{"highway":"speed_camera","maxspeed":"BR:urban"}},
              {"type":"node","id":104,"lat":-23.59,"lon":-46.68,"tags":{"highway":"speed_camera"}},
              {"type":"way","id":105,"tags":{"highway":"speed_camera"}},
              {"type":"node","lat":-23.6,"lon":-46.7}
            ]}""";

    private MockRestServiceServer server;
    private OverpassClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OverpassClient(builder);
    }

    @Test void buscaRadaresDaGrandeSaoPauloEConverte() {
        server.expect(requestTo(Matchers.startsWith("https://overpass-api.de/api/interpreter?data=")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("User-Agent", Matchers.startsWith("SafeMove/")))
                .andRespond(withSuccess(RESPOSTA, MediaType.APPLICATION_JSON));

        var lista = client.buscarRadares();

        assertEquals(4, lista.size()); // way e nó sem id são descartados
        assertEquals(101, lista.get(0).idOsm());
        assertEquals(-23.56, lista.get(0).latitude());
        assertEquals(-46.65, lista.get(0).longitude());
        assertEquals(50, lista.get(0).velocidadeMax());
        assertEquals(60, lista.get(1).velocidadeMax());
        assertNull(lista.get(2).velocidadeMax()); // "BR:urban" não tem número
        assertNull(lista.get(3).velocidadeMax()); // sem maxspeed
        server.verify();
    }

    @Test void respostaVaziaDevolveListaVazia() {
        server.expect(requestTo(Matchers.startsWith("https://overpass-api.de/")))
                .andRespond(withSuccess("{\"elements\":[]}", MediaType.APPLICATION_JSON));
        assertEquals(0, client.buscarRadares().size());
    }

    @Test void falhaDaOverpassViraIndisponivel() {
        server.expect(requestTo(Matchers.startsWith("https://overpass-api.de/"))).andRespond(withServerError());
        ApiException e = assertThrows(ApiException.class, () -> client.buscarRadares());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }
}
