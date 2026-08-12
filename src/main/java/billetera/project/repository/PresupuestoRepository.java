package billetera.project.repository;

import billetera.project.model.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {
    void deleteByCategoriaIdIn(List<Long> categoriaIds);
}
