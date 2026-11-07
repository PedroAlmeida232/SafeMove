package com.safemove.incidente;

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

/** Incidente de trânsito vindo da TomTom. O id_tomtom identifica o mesmo incidente entre sincronizações. */
@Entity
@Table(name = "incidentes", indexes = @Index(name = "ix_incidentes_lat_lng", columnList = "lat, lng"))
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidente")
    private Long id;

    @Column(name = "id_tomtom", nullable = false, unique = true, length = 128)
    private String idTomtom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaIncidente categoria;

    /** 0 a 4 (magnitudeOfDelay da TomTom). */
    @Column(nullable = false)
    private int gravidade;

    @Column(length = 255)
    private String via;

    @Column(length = 500)
    private String descricao;

    /** Atraso causado, em segundos. */
    @Column(name = "atraso")
    private Integer atrasoSegundos;

    @Column(name = "lat", nullable = false)
    private double latitude;

    @Column(name = "lng", nullable = false)
    private double longitude;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = Fuso.agora();

    protected Incidente() {}

    public Incidente(String idTomtom, CategoriaIncidente categoria, int gravidade, String via, String descricao,
                     Integer atrasoSegundos, double latitude, double longitude) {
        this.idTomtom = idTomtom;
        atualizar(categoria, gravidade, via, descricao, atrasoSegundos, latitude, longitude);
    }

    /** Atualiza com o que a TomTom devolveu na sincronização mais recente. */
    public void atualizar(CategoriaIncidente categoria, int gravidade, String via, String descricao,
                          Integer atrasoSegundos, double latitude, double longitude) {
        this.categoria = categoria;
        this.gravidade = gravidade;
        this.via = via;
        this.descricao = descricao;
        this.atrasoSegundos = atrasoSegundos;
        this.latitude = latitude;
        this.longitude = longitude;
        this.atualizadoEm = Fuso.agora();
    }

    public Long getId() { return id; }
    public String getIdTomtom() { return idTomtom; }
    public CategoriaIncidente getCategoria() { return categoria; }
    public int getGravidade() { return gravidade; }
    public String getVia() { return via; }
    public String getDescricao() { return descricao; }
    public Integer getAtrasoSegundos() { return atrasoSegundos; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
