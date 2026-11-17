package com.safemove.bo;

import com.safemove.dao.ConfirmacaoDAO;
import com.safemove.dao.OcorrenciaDAO;
import com.safemove.dao.UsuarioDAO;
import com.safemove.dto.UsuarioRequest;
import com.safemove.dto.UsuarioResponse;
import com.safemove.integracao.FirebaseAuthClient;
import com.safemove.model.Ocorrencia;
import com.safemove.model.Usuario;
import java.util.List;
import java.util.Locale;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioBO {

    private final UsuarioDAO dao;
    private final OcorrenciaDAO ocorrencias;
    private final ConfirmacaoDAO confirmacoes;
    private final FirebaseAuthClient firebase;

    public UsuarioBO(UsuarioDAO dao, OcorrenciaDAO ocorrencias, ConfirmacaoDAO confirmacoes,
                     FirebaseAuthClient firebase) {
        this.dao = dao;
        this.ocorrencias = ocorrencias;
        this.confirmacoes = confirmacoes;
        this.firebase = firebase;
    }

    /** Se o token é válido mas o perfil ainda não existe aqui, cria a partir das claims do Firebase. */
    @Transactional
    public Usuario perfilDoToken(Jwt jwt) {
        return dao.findById(jwt.getSubject()).orElseGet(() -> {
            String email = jwt.getClaimAsString("email");
            String nome = jwt.getClaimAsString("name");
            if (nome == null || nome.isBlank()) nome = email.substring(0, email.indexOf('@'));
            return dao.save(new Usuario(jwt.getSubject(), nome, email.toLowerCase(Locale.ROOT)));
        });
    }

    public UsuarioResponse buscar(Jwt jwt) {
        return UsuarioResponse.de(perfilDoToken(jwt));
    }

    /** Troca o nome aqui e no Firebase. Se o Firebase recusar, nada muda. */
    @Transactional
    public UsuarioResponse atualizar(Jwt jwt, UsuarioRequest req) {
        Usuario u = perfilDoToken(jwt);
        String nome = req.nome().trim();
        u.alterarNome(nome);
        firebase.definirNome(jwt.getTokenValue(), nome);
        return UsuarioResponse.de(u);
    }

    /**
     * Exclui a conta: confirmações, ocorrências e perfil aqui e o usuário no Firebase.
     * O Firebase é chamado por último; se ele falhar, a transação desfaz o que foi apagado.
     */
    @Transactional
    public void excluir(Jwt jwt) {
        dao.findById(jwt.getSubject()).ifPresent(u -> {
            confirmacoes.deleteByUsuario(u);
            List<Ocorrencia> minhas = ocorrencias.findByAutor(u);
            minhas.forEach(confirmacoes::deleteByOcorrencia);
            ocorrencias.deleteAll(minhas);
            dao.delete(u);
            dao.flush();
        });
        firebase.excluirConta(jwt.getTokenValue());
    }
}
