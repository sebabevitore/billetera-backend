package billetera.project.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenMensualDTO(
    BigDecimal totalIngresos,
    BigDecimal totalGastos,
    BigDecimal balanceNeto,
    List<CategoriaGastoDTO> categorias
) {}
