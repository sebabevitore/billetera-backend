package billetera.project.dto;

import billetera.project.model.TipoTransaccion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionRequestDTO(
    @NotNull @Positive BigDecimal monto,
    String descripcion,
    @NotNull TipoTransaccion tipo,
    @NotNull LocalDate fechaCompra,
    @NotNull Long cuentaId,
    Long categoriaId,
    Boolean esCompartido,
    Boolean esGastoFijo,
    Long gastoFijoId
) {}
