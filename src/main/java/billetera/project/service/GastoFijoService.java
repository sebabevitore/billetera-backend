package billetera.project.service;

import billetera.project.dto.GastoFijoResponseDTO;
import billetera.project.dto.GastosFijosDashboardDTO;
import billetera.project.dto.GastoFijoRequestDTO;
import billetera.project.model.GastoFijo;
import billetera.project.model.Categoria;
import billetera.project.model.Usuario;
import billetera.project.repository.GastoFijoRepository;
import billetera.project.repository.TransaccionRepository;
import billetera.project.repository.CategoriaRepository;
import billetera.project.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GastoFijoService {

    private final GastoFijoRepository gastoFijoRepository;
    private final TransaccionRepository transaccionRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public GastosFijosDashboardDTO obtenerDashboard(String email) {
        List<GastoFijo> todos = gastoFijoRepository.findDistinctByUsuarioEmail(email);
        
        YearMonth mesActual = YearMonth.now();
        LocalDate inicioMes = mesActual.atDay(1);
        LocalDate finMes = mesActual.atEndOfMonth();

        java.math.BigDecimal totalGastosFijos = java.math.BigDecimal.ZERO;
        List<GastoFijoResponseDTO> dtos = new java.util.ArrayList<>();

        for (GastoFijo gf : todos) {
            long count = transaccionRepository.countByGastoFijoIdAndFechaCompraBetween(
                    gf.getId(), email, inicioMes, finMes);
            boolean pagadoEsteMes = count > 0;
            
            totalGastosFijos = totalGastosFijos.add(gf.getMontoEstimado());
            
            dtos.add(new GastoFijoResponseDTO(
                    gf.getId(),
                    gf.getDescripcion(),
                    gf.getMontoEstimado(),
                    gf.getCategoria().getId(),
                    gf.getCategoria().getNombre(),
                    gf.getDiaVencimiento(),
                    pagadoEsteMes
            ));
        }

        java.math.BigDecimal totalIngresosMes = transaccionRepository.sumIngresosByUsuarioAndFechaCompraBetween(email, inicioMes, finMes);
        if (totalIngresosMes == null) {
            totalIngresosMes = java.math.BigDecimal.ZERO;
        }

        return new GastosFijosDashboardDTO(dtos, totalGastosFijos, totalIngresosMes);
    }

    @Transactional
    public GastoFijoResponseDTO crear(GastoFijoRequestDTO dto, String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
        Categoria categoria = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        if (!categoria.getUsuario().getEmail().equals(email)) {
            throw new SecurityException("No tienes permiso para usar esta categoría");
        }

        GastoFijo nuevo = GastoFijo.builder()
                .descripcion(dto.descripcion())
                .montoEstimado(dto.montoEstimado())
                .categoria(categoria)
                .diaVencimiento(dto.diaVencimiento())
                .usuario(usuario)
                .build();

        return mapToDTO(gastoFijoRepository.save(nuevo));
    }

    @Transactional
    public GastoFijoResponseDTO actualizar(Long id, GastoFijoRequestDTO dto, String email) {
        GastoFijo existente = gastoFijoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gasto fijo no encontrado"));

        if (!existente.getUsuario().getEmail().equals(email)) {
            throw new SecurityException("No tienes permiso para editar este gasto fijo");
        }

        Categoria categoria = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        if (!categoria.getUsuario().getEmail().equals(email)) {
            throw new SecurityException("No tienes permiso para usar esta categoría");
        }

        existente.setDescripcion(dto.descripcion());
        existente.setMontoEstimado(dto.montoEstimado());
        existente.setCategoria(categoria);
        existente.setDiaVencimiento(dto.diaVencimiento());

        return mapToDTO(gastoFijoRepository.save(existente));
    }

    @Transactional
    public void eliminar(Long id, String email) {
        GastoFijo existente = gastoFijoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gasto fijo no encontrado"));

        if (!existente.getUsuario().getEmail().equals(email)) {
            throw new SecurityException("No tienes permiso para eliminar este gasto fijo");
        }

        gastoFijoRepository.delete(existente);
    }

    private GastoFijoResponseDTO mapToDTO(GastoFijo gf) {
        return new GastoFijoResponseDTO(
                gf.getId(),
                gf.getDescripcion(),
                gf.getMontoEstimado(),
                gf.getCategoria().getId(),
                gf.getCategoria().getNombre(),
                gf.getDiaVencimiento(),
                false // Por defecto false al crear o actualizar, luego se recalcula en el dashboard
        );
    }
}
