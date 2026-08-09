package billetera.project.dto;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record PresupuestoRequestDTO(
    @Min(0)
    BigDecimal montoLimite
) {}
