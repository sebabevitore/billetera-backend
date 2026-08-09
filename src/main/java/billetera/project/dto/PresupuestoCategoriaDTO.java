package billetera.project.dto;

import java.math.BigDecimal;

public record PresupuestoCategoriaDTO(
    Long categoriaId,
    String nombre,
    BigDecimal gastoMesCorriente,
    BigDecimal gastoMesAnterior,
    BigDecimal montoLimite
) {}
