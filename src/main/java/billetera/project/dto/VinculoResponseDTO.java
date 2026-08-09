package billetera.project.dto;

import java.math.BigDecimal;

public record VinculoResponseDTO(
    Long vinculoId,
    Long parejaId,
    String parejaNombre,
    String parejaEmail,
    BigDecimal miBalance,
    String estado,
    boolean soyEmisor
) {}
