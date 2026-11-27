package com.safemove.model;

/** Como a pessoa vai se deslocar. Os nomes são o contrato com o app; o valor é o travelMode da TomTom. */
public enum ModoTransporte {
    CARRO("car"), A_PE("pedestrian"), BICICLETA("bicycle");

    private final String tomTom;

    ModoTransporte(String tomTom) {
        this.tomTom = tomTom;
    }

    public String tomTom() { return tomTom; }
}
