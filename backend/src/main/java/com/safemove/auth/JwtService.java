package com.safemove.auth;

import com.safemove.usuario.Usuario;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final Duration expiracao;

    public JwtService(JwtEncoder encoder, @Value("${safemove.jwt.expiracao-horas}") long horas) {
        this.encoder = encoder;
        this.expiracao = Duration.ofHours(horas);
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("safemove")
                .subject(usuario.getUid())
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracao))
                .claim("email", usuario.getEmail())
                .claim("nome", usuario.getNome())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
