package com.safemove.usuario;

import com.safemove.config.Fuso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Perfil do usuário. A senha fica só no Firebase; aqui o uid é o do Firebase. */
@Entity
@Table(name = "perfis_usuario")
public class Usuario {

    @Id
    @Column(name = "id_usuario", length = 128)
    private String uid;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = Fuso.agora();

    protected Usuario() {}

    public Usuario(String uid, String nome, String email) {
        this.uid = uid;
        this.nome = nome;
        this.email = email;
    }

    public String getUid() { return uid; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}
