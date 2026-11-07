package com.safemove.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FusoTest {

    @Test void agoraVemComOffsetDeSaoPaulo() {
        assertEquals(ZoneOffset.ofHours(-3), Fuso.agora().getOffset());
    }

    @Test void converterUtcParaSaoPauloMantemOInstante() {
        OffsetDateTime utc = OffsetDateTime.parse("2026-11-07T02:50:30Z");
        OffsetDateTime sp = Fuso.emSaoPaulo(utc);
        assertEquals("2026-11-06T23:50:30-03:00", sp.toString());
        assertEquals(utc.toInstant(), sp.toInstant());
    }
}
