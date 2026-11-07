package com.safemove.web;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converte qualquer erro no formato { status, detail, campos? } que o app espera.
 * 400 requisição inválida · 401 não autenticado · 404 não encontrado · 409 conflito ·
 * 422 regra de negócio · 503 indisponível (ex.: banco fora do ar).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static ResponseEntity<ErroResponse> resposta(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(new ErroResponse(status.value(), detail));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErroResponse> api(ApiException e) {
        return resposta(e.getStatus(), e.getMessage());
    }

    // 400 -----------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return comCampos(campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErroResponse> parametrosInvalidos(ConstraintViolationException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> {
            String caminho = v.getPropertyPath().toString();
            campos.putIfAbsent(caminho.substring(caminho.lastIndexOf('.') + 1), v.getMessage());
        });
        return comCampos(campos);
    }

    private static ResponseEntity<ErroResponse> comCampos(Map<String, String> campos) {
        String detail = campos.values().stream().findFirst().orElse("Dados inválidos.");
        return ResponseEntity.badRequest().body(new ErroResponse(400, detail, campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + e.getParameterName() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro " + e.getName() + ".");
    }

    // 404 / 405 -----------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "Não encontramos o que você procurava.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResponse> metodoNaoPermitido(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido para esta rota.");
    }

    // 409 -----------------------------------------------------------------

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResponse> conflitoNoBanco(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "Esse registro já existe ou conflita com outro.");
    }

    // 503 -----------------------------------------------------------------

    @ExceptionHandler(DataAccessResourceFailureException.class)
    ResponseEntity<ErroResponse> bancoIndisponivel(DataAccessResourceFailureException e) {
        log.error("Banco de dados indisponível", e);
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, "O serviço está indisponível no momento.");
    }

    // 500 (rede de segurança: nunca vaza detalhes internos) ----------------

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "O servidor teve um problema. Tente de novo em instantes.");
    }
}
