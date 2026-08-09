package billetera.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "gastos_fijos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoFijo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "monto_estimado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoEstimado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "dia_vencimiento", nullable = false)
    private Integer diaVencimiento;
}
