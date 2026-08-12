package billetera.project.repository;

import billetera.project.model.GastoFijo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GastoFijoRepository extends JpaRepository<GastoFijo, Long> {
    @EntityGraph(attributePaths = {"categoria"})
    List<GastoFijo> findDistinctByUsuarioEmail(String email);

    void deleteByUsuarioId(Long usuarioId);
}
