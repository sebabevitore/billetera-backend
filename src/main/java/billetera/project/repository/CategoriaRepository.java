package billetera.project.repository;

import billetera.project.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByUsuarioId(Long usuarioId);
    List<Categoria> findByUsuarioEmail(String email);
    boolean existsByNombreIgnoreCaseAndUsuario(String nombre, billetera.project.model.Usuario usuario);

    void deleteByUsuarioId(Long usuarioId);
}
