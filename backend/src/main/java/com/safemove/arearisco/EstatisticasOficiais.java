package com.safemove.arearisco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Estatísticas criminais oficiais (SSP-SP) do último mês disponível.
 *
 * <p>A SSP publica os dados mensais por delegacia só em planilhas (ssp.sp.gov.br › Estatística),
 * sem API. Por isso o backend lê um CSV gerado a partir delas, apontado por
 * {@code safemove.areas-risco.estatisticas-csv} (variável SSP_ESTATISTICAS_CSV), no formato:
 * <pre>
 * distrito;mes;roubos;furtos
 * Sé;2026-10;412;980
 * </pre>
 * Linhas do mesmo distrito e mês são somadas (um distrito pode ter várias delegacias). Só o mês mais
 * recente do arquivo é usado. O arquivo é relido a cada hora; sem arquivo, o risco usa só os
 * assaltos reportados no app.
 */
@Component
public class EstatisticasOficiais {

    private static final Logger log = LoggerFactory.getLogger(EstatisticasOficiais.class);

    private final String caminho;
    private volatile Map<String, EstatisticaOficial> doUltimoMes = Map.of();

    public EstatisticasOficiais(@Value("${safemove.areas-risco.estatisticas-csv:}") String caminho) {
        this.caminho = caminho == null ? "" : caminho.strip();
        recarregar();
    }

    /** Estatísticas do mês mais recente, pela chave do nome do distrito ({@link Nomes#chave}). */
    public Map<String, EstatisticaOficial> doUltimoMes() {
        return doUltimoMes;
    }

    @Scheduled(fixedDelayString = "${safemove.areas-risco.recarregar-ms:3600000}",
            initialDelayString = "${safemove.areas-risco.recarregar-ms:3600000}")
    public void recarregar() {
        if (caminho.isEmpty()) return;
        try (BufferedReader in = Files.newBufferedReader(Path.of(caminho), StandardCharsets.UTF_8)) {
            doUltimoMes = ultimoMes(ler(in));
            log.info("Estatísticas da SSP carregadas: {} distritos", doUltimoMes.size());
        } catch (IOException | RuntimeException e) {
            log.warn("Não foi possível ler {}; mantendo as estatísticas anteriores: {}", caminho, e.getMessage());
        }
    }

    /** Lê o CSV (separador ; ou ,). Linhas inválidas são ignoradas com um aviso no log. */
    static List<EstatisticaOficial> ler(Reader reader) throws IOException {
        BufferedReader in = reader instanceof BufferedReader b ? b : new BufferedReader(reader);
        List<EstatisticaOficial> lista = new ArrayList<>();
        String separador = null;
        String linha;
        int numero = 0;
        while ((linha = in.readLine()) != null) {
            numero++;
            linha = linha.strip().replace("﻿", "");
            if (linha.isEmpty() || linha.startsWith("#")) continue;
            if (separador == null) { // cabeçalho
                separador = linha.contains(";") ? ";" : ",";
                continue;
            }
            String[] c = linha.split(separador, -1);
            try {
                int roubos = Integer.parseInt(c[2].strip());
                int furtos = Integer.parseInt(c[3].strip());
                if (c[0].isBlank() || roubos < 0 || furtos < 0) throw new IllegalArgumentException();
                lista.add(new EstatisticaOficial(c[0].strip(), YearMonth.parse(c[1].strip()), roubos, furtos));
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException | DateTimeParseException e) {
                log.warn("Linha {} do CSV da SSP ignorada: {}", numero, linha);
            }
        }
        return lista;
    }

    /** Fica só com o mês mais recente e soma as linhas de um mesmo distrito. */
    static Map<String, EstatisticaOficial> ultimoMes(List<EstatisticaOficial> todas) {
        YearMonth mes = todas.stream().map(EstatisticaOficial::mes).max(Comparator.naturalOrder()).orElse(null);
        Map<String, EstatisticaOficial> porDistrito = new HashMap<>();
        for (EstatisticaOficial e : todas) {
            if (!e.mes().equals(mes)) continue;
            porDistrito.merge(Nomes.chave(e.distrito()), e, (a, b) ->
                    new EstatisticaOficial(a.distrito(), mes, a.roubos() + b.roubos(), a.furtos() + b.furtos()));
        }
        return Map.copyOf(porDistrito);
    }
}
