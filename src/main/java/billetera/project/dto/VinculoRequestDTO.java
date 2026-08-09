package billetera.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VinculoRequestDTO(
    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "Debe ser un email válido")
    String emailPareja
) {}
