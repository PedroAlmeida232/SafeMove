package com.safemove.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros no formato que o app espera: { status, detail, campos? }. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail api(ApiException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String detail = campos.values().stream().findFirst().orElse("Dados inválidos.");
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        p.setProperty("campos", campos);
        return p;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail corpoInvalido(HttpMessageNotReadableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.");
    }
}
