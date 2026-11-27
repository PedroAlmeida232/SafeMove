package com.safemove.model;

import com.safemove.config.Fuso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Radar de velocidade vindo do OpenStreetMap. O id_osm identifica o mesmo radar entre sincronizações. */
@Entity
@Table(name = "radares", indexes = @Index(name = "ix_radares_lat_lng", columnList = "lat, lng"))
public class Radar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_radar")
    private Long id;

    @Column(name = "id_osm", nullable = false, unique = true)
    private Long idOsm;

    @Column(name = "lat", nullable = false)
    private double latitude;

    @Column(name = "lng", nullable = false)
    private double longitude;

    /** Velocidade máxima em km/h (tag maxspeed do OSM), quando informada. */
    @Column(name = "velocidade_max")
    private Integer velocidadeMax;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = Fuso.agora();

    protected Radar() {}

    public Radar(Long idOsm, double latitude, double longitude, Integer velocidadeMax) {
        this.idOsm = idOsm;
        atualizar(latitude, longitude, velocidadeMax);
    }

    public void atualizar(double latitude, double longitude, Integer velocidadeMax) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.velocidadeMax = velocidadeMax;
        this.atualizadoEm = Fuso.agora();
    }

    public Long getId() { return id; }
    public Long getIdOsm() { return idOsm; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Integer getVelocidadeMax() { return velocidadeMax; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
