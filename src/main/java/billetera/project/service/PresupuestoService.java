package billetera.project.service;

import billetera.project.dto.PresupuestoCategoriaDTO;
import billetera.project.dto.PresupuestoRequestDTO;
import billetera.project.model.Categoria;
import billetera.project.model.TipoTransaccion;
import billetera.project.model.Usuario;
import billetera.project.repository.CategoriaRepository;
import billetera.project.repository.TransaccionRepository;
import billetera.project.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PresupuestoService {

    private final CategoriaRepository categoriaRepository;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<PresupuestoCategoriaDTO> obtenerPresupuestos(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Categoria> categoriasGasto = categoriaRepository.findByUsuarioId(usuario.getId())
                .stream()
                .filter(c -> c.getTipo() == TipoTransaccion.GASTO)
                .toList();

        YearMonth mesCorriente = YearMonth.now();
        YearMonth mesAnterior = mesCorriente.minusMonths(1);

        return categoriasGasto.stream().map(categoria -> {
            BigDecimal gastoMesCorriente = transaccionRepository.sumMontoEstadisticoByCategoriaAndFechaImputacionBetween(
                    categoria.getId(),
                    mesCorriente.atDay(1),
                    mesCorriente.atEndOfMonth()
            );

            BigDecimal gastoMesAnterior = transaccionRepository.sumMontoEstadisticoByCategoriaAndFechaImputacionBetween(
                    categoria.getId(),
                    mesAnterior.atDay(1),
                    mesAnterior.atEndOfMonth()
            );

            return new PresupuestoCategoriaDTO(
                    categoria.getId(),
                    categoria.getNombre(),
                    gastoMesCorriente != null ? gastoMesCorriente : BigDecimal.ZERO,
                    gastoMesAnterior != null ? gastoMesAnterior : BigDecimal.ZERO,
                    categoria.getMontoLimite()
            );
        })
        .sorted((c1, c2) -> c2.gastoMesAnterior().compareTo(c1.gastoMesAnterior()))
        .collect(Collectors.toList());
    }

    @Transactional
    public void actualizarLimite(Long categoriaId, PresupuestoRequestDTO dto, String email) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        if (!categoria.getUsuario().getEmail().equals(email)) {
            throw new SecurityException("No tienes permiso para modificar esta categoría");
        }

        categoria.setMontoLimite(dto.montoLimite());
        categoriaRepository.save(categoria);
    }
}
