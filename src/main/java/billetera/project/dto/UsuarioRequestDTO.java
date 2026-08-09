package billetera.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequestDTO(
    @NotBlank String nombre,
    @NotBlank @Email String email,
    @NotBlank String password
) {}
