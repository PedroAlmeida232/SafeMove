package com.safemove.dao;

import com.safemove.model.Confirmacao;
import com.safemove.model.Ocorrencia;
import com.safemove.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmacaoDAO extends JpaRepository<Confirmacao, Long> {

    boolean existsByOcorrenciaAndUsuario(Ocorrencia ocorrencia, Usuario usuario);

    void deleteByOcorrencia(Ocorrencia ocorrencia);

    void deleteByUsuario(Usuario usuario);
}
