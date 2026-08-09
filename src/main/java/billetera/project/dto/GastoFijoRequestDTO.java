package billetera.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record GastoFijoRequestDTO(
    @NotBlank(message = "La descripción no puede estar vacía")
    String descripcion,

    @NotNull(message = "El monto estimado es obligatorio")
    @Positive(message = "El monto estimado debe ser mayor a 0")
    BigDecimal montoEstimado,

    @NotNull(message = "La categoría es obligatoria")
    Long categoriaId,

    @NotNull(message = "El día de vencimiento es obligatorio")
    Integer diaVencimiento
) {}
