package com.safemove.arearisco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EstatisticasOficiaisTest {

    @Test void leSomaDelegaciasEFicaSoComOUltimoMes() throws Exception {
        String csv = """
                distrito;mes;roubos;furtos
                # comentário
                Sé;2026-09;500;900
                Sé;2026-10;300;600
                SE ;2026-10;100;200
                Pinheiros;2026-10;50;400
                Linha quebrada
                Moema;2026-13;1;1
                Moema;2026-10;-1;1
                """;
        Map<String, EstatisticaOficial> m = EstatisticasOficiais.ultimoMes(
                EstatisticasOficiais.ler(new StringReader(csv)));

        assertEquals(2, m.size());
        EstatisticaOficial se = m.get(Nomes.chave("Sé"));
        assertEquals(YearMonth.of(2026, 10), se.mes());
        assertEquals(400, se.roubos());
        assertEquals(800, se.furtos());
        assertEquals(800.0, se.indice());
        assertEquals(50, m.get("PINHEIROS").roubos());
    }

    @Test void aceitaVirgulaComoSeparador() throws Exception {
        var lista = EstatisticasOficiais.ler(new StringReader("distrito,mes,roubos,furtos\nBrás,2026-10,10,20\n"));
        assertEquals(1, lista.size());
        assertEquals("Brás", lista.get(0).distrito());
    }

    @Test void semArquivoFicaVazioEArquivoInexistenteNaoQuebra(@TempDir Path dir) throws Exception {
        assertTrue(new EstatisticasOficiais("").doUltimoMes().isEmpty());
        assertTrue(new EstatisticasOficiais(dir.resolve("nao-existe.csv").toString()).doUltimoMes().isEmpty());

        Path arquivo = dir.resolve("ssp.csv");
        Files.writeString(arquivo, "distrito;mes;roubos;furtos\nSé;2026-10;10;20\n");
        assertEquals(10, new EstatisticasOficiais(arquivo.toString()).doUltimoMes().get("SE").roubos());
    }
}
