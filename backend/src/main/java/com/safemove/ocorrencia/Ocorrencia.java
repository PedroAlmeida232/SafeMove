package com.safemove.ocorrencia;

import com.safemove.config.Fuso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Ocorrência registrada por um usuário (assalto, buraco, alagamento...). */
@Entity
@Table(name = "ocorrencias", indexes = @Index(name = "ix_ocorrencias_lat_lng", columnList = "lat, lng"))
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ocorrencia")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoOcorrencia tipo;

    @Column(length = 500)
    private String descricao;

    @Column(name = "lat", nullable = false)
    private double latitude;

    @Column(name = "lng", nullable = false)
    private double longitude;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = Fuso.agora();

    /** Quantos usuários disseram "Eu também vi". */
    @Column(nullable = false)
    private int confirmacoes;

    protected Ocorrencia() {}

    public Ocorrencia(TipoOcorrencia tipo, String descricao, double latitude, double longitude) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public TipoOcorrencia getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public int getConfirmacoes() { return confirmacoes; }
}
