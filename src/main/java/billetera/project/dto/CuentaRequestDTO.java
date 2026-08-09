package billetera.project.dto;

import billetera.project.model.TipoCuenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

public record CuentaRequestDTO(
    @NotBlank String nombre,
    @NotNull TipoCuenta tipo,
    Integer diaCierre,
    boolean esCuentaAhorro,
    BigDecimal saldoInicial,
    Boolean esPrincipal,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaCierre,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaVencimiento
) {}
