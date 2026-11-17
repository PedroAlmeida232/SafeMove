package com.safemove.model;

import com.safemove.config.Fuso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

/** "Eu também vi": um usuário confirma uma ocorrência de outro, no máximo uma vez. */
@Entity
@Table(name = "confirmacoes_ocorrencia", uniqueConstraints = @UniqueConstraint(
        name = "uk_confirmacao_ocorr_usuario", columnNames = {"id_ocorrencia", "id_usuario"}))
public class Confirmacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_confirmacao")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_ocorrencia", nullable = false,
            foreignKey = @ForeignKey(name = "fk_confirmacao_ocorrencia"))
    private Ocorrencia ocorrencia;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_confirmacao_usuario"))
    private Usuario usuario;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = Fuso.agora();

    protected Confirmacao() {}

    public Confirmacao(Ocorrencia ocorrencia, Usuario usuario) {
        this.ocorrencia = ocorrencia;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public Ocorrencia getOcorrencia() { return ocorrencia; }
    public Usuario getUsuario() { return usuario; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}
