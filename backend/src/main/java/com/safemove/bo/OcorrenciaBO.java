package com.safemove.bo;

import com.safemove.config.Fuso;
import com.safemove.dao.ConfirmacaoDAO;
import com.safemove.dao.OcorrenciaDAO;
import com.safemove.dto.OcorrenciaRequest;
import com.safemove.dto.OcorrenciaResponse;
import com.safemove.exception.ApiException;
import com.safemove.geo.Coordenada;
import com.safemove.geo.Geo;
import com.safemove.geo.Retangulo;
import com.safemove.model.Confirmacao;
import com.safemove.model.Ocorrencia;
import com.safemove.model.TipoOcorrencia;
import com.safemove.model.Usuario;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OcorrenciaBO {

    static final double RAIO_PADRAO_METROS = 2_000;
    static final int HORAS_PADRAO = 24;
    static final int HORAS_MAXIMO = 24 * 30;

    private final OcorrenciaDAO dao;
    private final ConfirmacaoDAO confirmacoes;
    private final UsuarioBO usuarios;

    public OcorrenciaBO(OcorrenciaDAO dao, ConfirmacaoDAO confirmacoes, UsuarioBO usuarios) {
        this.dao = dao;
        this.confirmacoes = confirmacoes;
        this.usuarios = usuarios;
    }

    /**
     * Ocorrências num raio (metros, padrão 2.000, máximo 50.000) criadas nas últimas {@code horas}
     * (padrão 24, máximo 720), das mais recentes às mais antigas. {@code tipo} filtra por tipo.
     */
    public List<OcorrenciaResponse> listar(double lat, double lng, Double raio, Integer horas, TipoOcorrencia tipo) {
        Coordenada centro = Busca.centro(lat, lng);
        double raioMetros = Busca.raio(raio, RAIO_PADRAO_METROS);
        int janelaHoras = horas == null ? HORAS_PADRAO : horas;
        if (janelaHoras <= 0 || janelaHoras > HORAS_MAXIMO) {
            throw ApiException.requisicaoInvalida("As horas devem estar entre 1 e " + HORAS_MAXIMO + ".");
        }

        Retangulo caixa = Geo.retanguloEmVolta(centro, raioMetros);
        OffsetDateTime desde = Fuso.agora().minusHours(janelaHoras);
        return dao.findByLatitudeBetweenAndLongitudeBetweenAndCriadoEmGreaterThanEqual(
                        caixa.minLatitude(), caixa.maxLatitude(), caixa.minLongitude(), caixa.maxLongitude(), desde)
                .stream()
                .filter(o -> tipo == null || o.getTipo() == tipo)
                .filter(o -> Geo.distanciaMetros(centro, new Coordenada(o.getLatitude(), o.getLongitude())) <= raioMetros)
                .sorted(Comparator.comparing(Ocorrencia::getCriadoEm).reversed())
                .map(OcorrenciaResponse::de)
                .toList();
    }

    public OcorrenciaResponse buscar(Long id) {
        return OcorrenciaResponse.de(encontrar(id));
    }

    @Transactional
    public OcorrenciaResponse criar(Jwt jwt, OcorrenciaRequest req) {
        exigirAreaAtendida(req);
        Usuario autor = usuarios.perfilDoToken(jwt);
        Ocorrencia o = dao.save(new Ocorrencia(autor, req.tipo(), descricao(req), req.latitude(), req.longitude()));
        return OcorrenciaResponse.de(o);
    }

    @Transactional
    public OcorrenciaResponse atualizar(Jwt jwt, Long id, OcorrenciaRequest req) {
        Ocorrencia o = doAutor(jwt, id, "Só quem registrou a ocorrência pode alterá-la.");
        exigirAreaAtendida(req);
        o.atualizar(req.tipo(), descricao(req), req.latitude(), req.longitude());
        return OcorrenciaResponse.de(o);
    }

    @Transactional
    public void excluir(Jwt jwt, Long id) {
        Ocorrencia o = doAutor(jwt, id, "Só quem registrou a ocorrência pode excluí-la.");
        confirmacoes.deleteByOcorrencia(o);
        dao.delete(o);
    }

    /** "Eu também vi": cada usuário confirma uma vez, e não vale confirmar a própria ocorrência. */
    @Transactional
    public OcorrenciaResponse confirmar(Jwt jwt, Long id) {
        Ocorrencia o = encontrar(id);
        if (o.pertenceA(jwt.getSubject())) {
            throw ApiException.naoProcessavel("Você não pode confirmar a própria ocorrência.");
        }
        Usuario usuario = usuarios.perfilDoToken(jwt);
        if (confirmacoes.existsByOcorrenciaAndUsuario(o, usuario)) {
            throw ApiException.conflito("Você já confirmou esta ocorrência.");
        }
        confirmacoes.save(new Confirmacao(o, usuario));
        o.registrarConfirmacao();
        return OcorrenciaResponse.de(o);
    }

    private Ocorrencia encontrar(Long id) {
        return dao.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Ocorrência não encontrada."));
    }

    private Ocorrencia doAutor(Jwt jwt, Long id, String semPermissao) {
        Ocorrencia o = encontrar(id);
        if (!o.pertenceA(jwt.getSubject())) {
            throw ApiException.proibido(semPermissao);
        }
        return o;
    }

    private static void exigirAreaAtendida(OcorrenciaRequest req) {
        if (!Geo.GRANDE_SAO_PAULO.contem(new Coordenada(req.latitude(), req.longitude()))) {
            throw ApiException.naoProcessavel("O SafeMove só aceita ocorrências na Grande São Paulo.");
        }
    }

    private static String descricao(OcorrenciaRequest req) {
        return req.descricao() == null || req.descricao().isBlank() ? null : req.descricao().trim();
    }
}
