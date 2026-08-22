package billetera.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import billetera.project.model.TipoTransaccion;

public record CategoriaRequestDTO(
    @NotBlank String nombre,
    @NotNull TipoTransaccion tipo,
    String icono
) {}
