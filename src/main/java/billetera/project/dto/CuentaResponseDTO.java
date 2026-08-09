package billetera.project.dto;

import billetera.project.model.TipoCuenta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaResponseDTO(
    Long id,
    String nombre,
    TipoCuenta tipo,
    Integer diaCierre,
    Long usuarioId,
    boolean esPrincipal,
    boolean esCuentaAhorro,
    BigDecimal saldoActual,
    LocalDate fechaCierre,
    LocalDate fechaVencimiento
) {}
