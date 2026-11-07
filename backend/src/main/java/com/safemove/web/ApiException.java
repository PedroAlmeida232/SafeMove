package com.safemove.web;

import org.springframework.http.HttpStatus;

/** Erro de negócio que vira { status, detail } na resposta. Use as fábricas abaixo. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    /** 400 */
    public static ApiException requisicaoInvalida(String msg) { return new ApiException(HttpStatus.BAD_REQUEST, msg); }
    /** 401 */
    public static ApiException naoAutenticado(String msg) { return new ApiException(HttpStatus.UNAUTHORIZED, msg); }
    /** 404 */
    public static ApiException naoEncontrado(String msg) { return new ApiException(HttpStatus.NOT_FOUND, msg); }
    /** 409 */
    public static ApiException conflito(String msg) { return new ApiException(HttpStatus.CONFLICT, msg); }
    /** 422: a requisição é válida, mas viola uma regra de negócio. */
    public static ApiException naoProcessavel(String msg) { return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, msg); }
    /** 503 */
    public static ApiException indisponivel(String msg) { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, msg); }
}
