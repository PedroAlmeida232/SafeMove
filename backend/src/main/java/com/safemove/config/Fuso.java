package com.safemove.config;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Fuso do SafeMove: todas as datas são gravadas e devolvidas no horário de São Paulo (-03:00). */
public final class Fuso {

    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private Fuso() {}

    public static OffsetDateTime agora() {
        return OffsetDateTime.now(SAO_PAULO);
    }

    /** Converte para São Paulo sem mudar o instante (ex.: o que o banco devolve em UTC). */
    public static OffsetDateTime emSaoPaulo(OffsetDateTime data) {
        return data.atZoneSameInstant(SAO_PAULO).toOffsetDateTime();
    }
}
