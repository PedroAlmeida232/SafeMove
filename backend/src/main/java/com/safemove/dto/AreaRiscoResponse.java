package com.safemove.dto;

/** Nível de risco de um distrito: 1 (baixo) a 5 (muito alto). */
public record AreaRiscoResponse(String distrito, double latitude, double longitude, int nivel, int ocorrencias) {}
