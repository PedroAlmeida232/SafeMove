package com.safemove.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Corpo padrão de erro da API: { status, detail, campos? }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(int status, String detail, Map<String, String> campos) {

    public ErroResponse(int status, String detail) {
        this(status, detail, null);
    }
}
