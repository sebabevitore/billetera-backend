package billetera.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferenciaRequestDTO(
    @NotNull Long cuentaOrigenId,
    @NotNull Long cuentaDestinoId,
    @NotNull @Positive BigDecimal monto,
    String descripcion,
    @NotNull LocalDate fecha
) {}
