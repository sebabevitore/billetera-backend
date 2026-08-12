package billetera.project.repository;

import billetera.project.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    
    long countByEmailEndingWith(String suffix);
    
    List<Usuario> findByEmailEndingWithAndFechaCreacionBefore(String suffix, LocalDateTime fecha);
}
