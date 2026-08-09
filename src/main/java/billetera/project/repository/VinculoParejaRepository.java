package billetera.project.repository;

import billetera.project.model.VinculoPareja;
import billetera.project.model.EstadoVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface VinculoParejaRepository extends JpaRepository<VinculoPareja, Long> {

    @EntityGraph(attributePaths = {"usuario1", "usuario2"})
    @Query("SELECT v FROM VinculoPareja v WHERE (v.usuario1.id = :usuarioId OR v.usuario2.id = :usuarioId) AND v.estado != billetera.project.model.EstadoVinculo.INACTIVO")
    List<VinculoPareja> findByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    // Obtener los vínculos activos (ACEPTADOS) o los enviados pendientes para mostrarlos en el panel
    @EntityGraph(attributePaths = {"usuario1", "usuario2"})
    @Query("SELECT v FROM VinculoPareja v WHERE (v.usuario1.id = :usuarioId OR v.usuario2.id = :usuarioId) AND v.estado = :estado")
    List<VinculoPareja> findByUsuarioIdAndEstado(@Param("usuarioId") Long usuarioId, @Param("estado") EstadoVinculo estado);

    // Obtener las invitaciones que el usuario recibió y están PENDIENTES
    @EntityGraph(attributePaths = {"usuario1", "usuario2"})
    List<VinculoPareja> findByUsuario2IdAndEstado(Long usuario2Id, EstadoVinculo estado);
}
