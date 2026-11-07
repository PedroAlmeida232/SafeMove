package com.safemove.auth;

import com.safemove.usuario.UsuarioResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegistroRequest(
            @NotBlank(message = "Informe seu nome.") @Size(max = 120, message = "Nome muito longo.") String nome,
            @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email,
            @NotBlank(message = "Informe uma senha.")
            @Size(min = 6, max = 72, message = "A senha precisa ter entre 6 e 72 caracteres.") String senha) {}

    public record LoginRequest(
            @NotBlank(message = "Informe seu e-mail.") String email,
            @NotBlank(message = "Digite sua senha.") String senha) {}

    public record RefreshRequest(@NotBlank(message = "Sessão inválida.") String refreshToken) {}

    public record RecuperarSenhaRequest(
            @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email) {}

    /** token = ID token do Firebase (1h), enviado como Bearer; refreshToken renova em /auth/refresh. */
    public record AuthResponse(String token, String refreshToken, UsuarioResponse usuario) {}
}
