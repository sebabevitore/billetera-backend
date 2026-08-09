package billetera.project.dto;

import java.math.BigDecimal;
import java.util.List;

public record GastosFijosDashboardDTO(
    List<GastoFijoResponseDTO> gastosFijos,
    BigDecimal totalGastosFijos,
    BigDecimal totalIngresosMes
) {}
