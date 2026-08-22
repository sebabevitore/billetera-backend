package billetera.project.dto;

import billetera.project.model.TipoTransaccion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionResponseDTO(
    Long id,
    BigDecimal monto,
    String descripcion,
    TipoTransaccion tipo,
    LocalDate fechaCompra,
    LocalDate fechaImputacion,
    Long cuentaId,
    String cuentaNombre,
    Long categoriaId,
    String categoriaNombre,
    Long idTransferenciaVinculada,
    Boolean esCompartido,
    BigDecimal montoEstadistico,
    Boolean esGastoFijo,
    Long gastoFijoId,
    String iconoCategoria
) {}
