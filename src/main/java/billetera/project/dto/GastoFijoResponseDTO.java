package billetera.project.dto;

import java.math.BigDecimal;

public record GastoFijoResponseDTO(
    Long id,
    String descripcion,
    BigDecimal montoEstimado,
    Long categoriaId,
    String categoriaNombre,
    Integer diaVencimiento,
    boolean pagadoEsteMes
) {}
