package com.safemove.arearisco;

import java.text.Normalizer;
import java.util.Locale;

/** Compara nomes de distrito sem depender de acentos, caixa ou espaços extras ("Sé" = "SE" = " se "). */
final class Nomes {

    private Nomes() {}

    static String chave(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
