package com.safemove.arearisco;

/**
 * Risco de um distrito. {@code assaltosReportados} conta os assaltos reportados no app nos últimos 30 dias.
 * {@code roubosOficiais}, {@code furtosOficiais} e {@code mesOficial} (ex.: "2026-10") vêm da SSP e ficam
 * nulos se não houver estatística oficial carregada para o distrito.
 */
public record AreaRiscoResponse(String distrito, double latitude, double longitude, int nivel, boolean segura,
                                int assaltosReportados, Integer roubosOficiais, Integer furtosOficiais,
                                String mesOficial) {}
