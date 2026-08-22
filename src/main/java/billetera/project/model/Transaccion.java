package billetera.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transacciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransaccion tipo;

    @Column(nullable = false)
    private LocalDate fechaCompra;

    @Column(nullable = false)
    private LocalDate fechaImputacion;

    private Long idTransferenciaVinculada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "es_compartido", nullable = false)
    @Builder.Default
    private Boolean esCompartido = false;

    @Column(name = "monto_estadistico", precision = 10, scale = 2)
    private BigDecimal montoEstadistico;

    @Column(name = "es_gasto_fijo", nullable = false)
    @Builder.Default
    private Boolean esGastoFijo = false;

    @Column(name = "gasto_fijo_id")
    private Long gastoFijoId;
}
