package com.safemove.integracao;

/** Radar como veio do OpenStreetMap (nó highway=speed_camera). */
public record RadarOsm(long idOsm, double latitude, double longitude, Integer velocidadeMax) {}
