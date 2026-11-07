package com.safemove.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Confere as regras de emissor e audiência com tokens assinados por uma chave local. */
class SecurityConfigTokenTest {

    private static final String PROJETO = "safemove-teste";

    private static KeyPair chaves;
    private static NimbusJwtDecoder decoder;

    @BeforeAll
    static void prepara() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        chaves = g.generateKeyPair();
        decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) chaves.getPublic()).build();
        decoder.setJwtValidator(SecurityConfig.validadorFirebase(PROJETO));
    }

    private static String token(String issuer, String audiencia, Instant expira) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer).audience(audiencia).subject("uid-1")
                .issueTime(Date.from(Instant.now().minusSeconds(10)))
                .expirationTime(Date.from(expira)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) chaves.getPrivate()));
        return jwt.serialize();
    }

    private static final String ISSUER_OK = "https://securetoken.google.com/" + PROJETO;

    @Test void aceitaTokenDoProjeto() throws Exception {
        assertDoesNotThrow(() -> decoder.decode(token(ISSUER_OK, PROJETO, Instant.now().plusSeconds(3600))));
    }

    @Test void rejeitaEmissorDeOutroProjeto() throws Exception {
        String t = token("https://securetoken.google.com/outro-projeto", PROJETO, Instant.now().plusSeconds(3600));
        assertThrows(BadJwtException.class, () -> decoder.decode(t));
    }

    @Test void rejeitaAudienciaDeOutroProjeto() throws Exception {
        String t = token(ISSUER_OK, "outro-projeto", Instant.now().plusSeconds(3600));
        assertThrows(BadJwtException.class, () -> decoder.decode(t));
    }

    @Test void rejeitaTokenExpirado() throws Exception {
        String t = token(ISSUER_OK, PROJETO, Instant.now().minusSeconds(3600));
        assertThrows(BadJwtException.class, () -> decoder.decode(t));
    }

    @Test void rejeitaAssinaturaDeOutraChave() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair outra = g.generateKeyPair();
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(ISSUER_OK).audience(PROJETO).subject("uid-1")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600))).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) outra.getPrivate()));
        String t = jwt.serialize();
        assertThrows(BadJwtException.class, () -> decoder.decode(t));
    }
}
