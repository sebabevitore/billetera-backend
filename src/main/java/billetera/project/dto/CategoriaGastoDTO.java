package billetera.project.dto;

import java.math.BigDecimal;

public record CategoriaGastoDTO(
    String categoria,
    BigDecimal total
) {}
