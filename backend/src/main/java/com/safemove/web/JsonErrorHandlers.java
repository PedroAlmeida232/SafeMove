package com.safemove.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Erros que acontecem no filtro de segurança (antes dos controllers) e por isso não passam
 * pelo @RestControllerAdvice: token ausente/inválido (401) e acesso negado (403).
 */
@Component
public class JsonErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        escrever(res, HttpStatus.UNAUTHORIZED, "Sua sessão expirou. Entre novamente.");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
                       org.springframework.security.access.AccessDeniedException e) throws IOException {
        escrever(res, HttpStatus.FORBIDDEN, "Você não tem permissão para fazer isso.");
    }

    // Mensagens fixas, sem aspas: não precisa escapar JSON.
    private static void escrever(HttpServletResponse res, HttpStatus status, String detail) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"status\":" + status.value() + ",\"detail\":\"" + detail + "\"}");
    }
}
