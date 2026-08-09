package billetera.project.dto;

import java.math.BigDecimal;

public record EvolucionMensualDTO(
    String mesAnio,
    BigDecimal ingresos,
    BigDecimal gastos
) {}
