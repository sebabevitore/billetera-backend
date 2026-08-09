package billetera.project.dto;

import java.math.BigDecimal;

public record PresupuestoResponseDTO(
    Long id,
    BigDecimal montoAsignado,
    Integer mes,
    Integer anio,
    String categoriaNombre
) {}
