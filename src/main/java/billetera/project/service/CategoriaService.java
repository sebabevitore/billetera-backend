package billetera.project.service;

import billetera.project.dto.CategoriaRequestDTO;
import billetera.project.dto.CategoriaResponseDTO;
import billetera.project.model.Categoria;
import billetera.project.model.Usuario;
import billetera.project.repository.CategoriaRepository;
import billetera.project.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {
    
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public CategoriaResponseDTO crear(CategoriaRequestDTO dto, String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
        if (categoriaRepository.existsByNombreIgnoreCaseAndUsuario(dto.nombre(), usuario)) {
            throw new IllegalArgumentException("La categoría ya existe");
        }
            
        Categoria categoria = Categoria.builder()
            .nombre(dto.nombre())
            .usuario(usuario)
            .tipo(dto.tipo())
            .build();
            
        categoria = categoriaRepository.save(categoria);
        return mapToDTO(categoria);
    }

    public CategoriaResponseDTO obtenerPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        return mapToDTO(categoria);
    }
    
    public List<CategoriaResponseDTO> listarPorUsuario(Long usuarioId) {
        return categoriaRepository.findByUsuarioId(usuarioId).stream()
            .map(this::mapToDTO)
            .toList();
    }

    public List<CategoriaResponseDTO> listarPorUsuarioEmail(String email) {
        return categoriaRepository.findByUsuarioEmail(email).stream()
            .map(this::mapToDTO)
            .toList();
    }

    public CategoriaResponseDTO actualizar(Long id, CategoriaRequestDTO dto, String userEmail) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            
        if (!categoria.getUsuario().getEmail().equals(userEmail)) {
            throw new SecurityException("No tienes permiso para modificar esta categoría");
        }
        
        categoria.setNombre(dto.nombre());
        if (dto.tipo() != null) {
            categoria.setTipo(dto.tipo());
        }
        
        categoria = categoriaRepository.save(categoria);
        return mapToDTO(categoria);
    }

    public void eliminar(Long id) {
        categoriaRepository.deleteById(id);
    }

    private CategoriaResponseDTO mapToDTO(Categoria categoria) {
        return new CategoriaResponseDTO(
            categoria.getId(), 
            categoria.getNombre(), 
            categoria.getUsuario().getId(),
            categoria.getTipo()
        );
    }
}
