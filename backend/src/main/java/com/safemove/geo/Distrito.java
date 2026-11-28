package com.safemove.geo;

import java.util.List;

/** Distrito da cidade de São Paulo com o centro aproximado (usado para agrupar as ocorrências). */
public record Distrito(String nome, Coordenada centro) {

    private static Distrito d(String nome, double lat, double lng) {
        return new Distrito(nome, new Coordenada(lat, lng));
    }

    /** Distritos atendidos, com o centro aproximado de cada um. */
    public static final List<Distrito> SAO_PAULO = List.of(
            // Centro
            d("Sé", -23.5505, -46.6333), d("República", -23.5431, -46.6424), d("Bela Vista", -23.5614, -46.6457),
            d("Consolação", -23.5530, -46.6600), d("Santa Cecília", -23.5373, -46.6536), d("Liberdade", -23.5590, -46.6340),
            d("Cambuci", -23.5700, -46.6200), d("Bom Retiro", -23.5270, -46.6420), d("Brás", -23.5435, -46.6158),
            d("Pari", -23.5260, -46.6150),
            // Oeste
            d("Barra Funda", -23.5260, -46.6650), d("Perdizes", -23.5360, -46.6780), d("Lapa", -23.5250, -46.7030),
            d("Pinheiros", -23.5672, -46.6917), d("Jardim Paulista", -23.5710, -46.6610), d("Itaim Bibi", -23.5850, -46.6800),
            d("Butantã", -23.5720, -46.7210), d("Morumbi", -23.5990, -46.7200),
            // Sul
            d("Vila Mariana", -23.5890, -46.6350), d("Moema", -23.6009, -46.6624), d("Saúde", -23.6180, -46.6390),
            d("Campo Belo", -23.6250, -46.6700), d("Jabaquara", -23.6460, -46.6420), d("Santo Amaro", -23.6530, -46.7100),
            d("Vila Andrade", -23.6300, -46.7300), d("Campo Limpo", -23.6400, -46.7600), d("Capão Redondo", -23.6700, -46.7800),
            d("Cidade Ademar", -23.6700, -46.6500), d("Jardim Ângela", -23.7100, -46.7700), d("Grajaú", -23.7600, -46.6800),
            // Leste
            d("Mooca", -23.5580, -46.5990), d("Belém", -23.5420, -46.5920), d("Tatuapé", -23.5400, -46.5760),
            d("Penha", -23.5260, -46.5440), d("Ipiranga", -23.5900, -46.6070), d("Vila Prudente", -23.5830, -46.5800),
            d("Sapopemba", -23.6000, -46.5100), d("São Mateus", -23.6000, -46.4800), d("Itaquera", -23.5400, -46.4560),
            d("Guaianases", -23.5420, -46.4100), d("Cidade Tiradentes", -23.5900, -46.4000),
            // Norte
            d("Santana", -23.5000, -46.6250), d("Tucuruvi", -23.4800, -46.6050), d("Vila Maria", -23.5150, -46.5800),
            d("Casa Verde", -23.5050, -46.6550), d("Freguesia do Ó", -23.5000, -46.6950), d("Brasilândia", -23.4700, -46.6900),
            d("Pirituba", -23.4870, -46.7250));
}
