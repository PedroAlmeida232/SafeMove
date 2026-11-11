package com.safemove.arearisco;

import com.safemove.geo.Coordenada;
import com.safemove.geo.Geo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Distritos do município de São Paulo, lidos de {@code areas-risco/distritos-sp.csv}. */
@Component
public class Distritos {

    /** Uma ocorrência só conta para um distrito se estiver a até esta distância do centroide. */
    public static final double DISTANCIA_MAXIMA_METROS = 3_000;

    private static final String ARQUIVO = "areas-risco/distritos-sp.csv";

    private final List<Distrito> todos;

    public Distritos() {
        this(lerArquivo());
    }

    Distritos(List<Distrito> todos) {
        this.todos = List.copyOf(todos);
    }

    public List<Distrito> todos() {
        return todos;
    }

    /** Distrito de centroide mais próximo, se estiver a até {@link #DISTANCIA_MAXIMA_METROS}. */
    public Optional<Proximo> maisProximo(Coordenada ponto) {
        return todos.stream()
                .map(d -> new Proximo(d, Geo.distanciaMetros(ponto, d.centro())))
                .filter(p -> p.distanciaMetros() <= DISTANCIA_MAXIMA_METROS)
                .min(Comparator.comparingDouble(Proximo::distanciaMetros));
    }

    public record Proximo(Distrito distrito, double distanciaMetros) {}

    private static List<Distrito> lerArquivo() {
        List<Distrito> lista = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new ClassPathResource(ARQUIVO).getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            boolean cabecalho = true;
            while ((linha = in.readLine()) != null) {
                linha = linha.strip();
                if (linha.isEmpty() || linha.startsWith("#")) continue;
                if (cabecalho) { // distrito;lat;lng
                    cabecalho = false;
                    continue;
                }
                String[] c = linha.split(";");
                lista.add(new Distrito(c[0].strip(),
                        new Coordenada(Double.parseDouble(c[1].strip()), Double.parseDouble(c[2].strip()))));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + ARQUIVO, e);
        }
        return lista;
    }
}
