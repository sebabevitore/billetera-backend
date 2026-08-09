package billetera.project.repository;

import billetera.project.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    List<Transaccion> findByCuentaId(Long cuentaId);
    
    @EntityGraph(attributePaths = {"cuenta", "categoria", "cuenta.usuario"})
    List<Transaccion> findByCuentaUsuarioEmailOrderByFechaCompraDesc(String email);

    @Query("SELECT SUM(t.montoEstadistico) FROM Transaccion t WHERE t.categoria.id = :categoriaId AND t.fechaImputacion BETWEEN :inicio AND :fin")
    java.math.BigDecimal sumMontoEstadisticoByCategoriaAndFechaImputacionBetween(
        @Param("categoriaId") Long categoriaId,
        @Param("inicio") java.time.LocalDate inicio,
        @Param("fin") java.time.LocalDate fin
    );

    @Query("SELECT COUNT(t) FROM Transaccion t WHERE t.gastoFijoId = :gastoFijoId AND t.cuenta.usuario.email = :email AND t.fechaCompra BETWEEN :inicioMes AND :finMes")
    long countByGastoFijoIdAndFechaCompraBetween(
        @Param("gastoFijoId") Long gastoFijoId, 
        @Param("email") String email, 
        @Param("inicioMes") java.time.LocalDate inicioMes, 
        @Param("finMes") java.time.LocalDate finMes
    );

    @Query("SELECT SUM(t.monto) FROM Transaccion t WHERE t.cuenta.usuario.email = :email AND t.tipo = billetera.project.model.TipoTransaccion.INGRESO AND t.fechaCompra BETWEEN :inicioMes AND :finMes")
    java.math.BigDecimal sumIngresosByUsuarioAndFechaCompraBetween(
        @Param("email") String email, 
        @Param("inicioMes") java.time.LocalDate inicioMes, 
        @Param("finMes") java.time.LocalDate finMes
    );

    @Query("SELECT SUM(t.monto) FROM Transaccion t WHERE t.cuenta.usuario.email = :email AND t.tipo = billetera.project.model.TipoTransaccion.GASTO AND t.fechaCompra BETWEEN :inicioMes AND :finMes")
    java.math.BigDecimal sumGastosByUsuarioAndFechaCompraBetween(
        @Param("email") String email, 
        @Param("inicioMes") java.time.LocalDate inicioMes, 
        @Param("finMes") java.time.LocalDate finMes
    );

    @Query("SELECT new billetera.project.dto.CategoriaGastoDTO(t.categoria.nombre, SUM(t.monto)) " +
           "FROM Transaccion t " +
           "WHERE t.cuenta.usuario.email = :email " +
           "AND t.tipo = billetera.project.model.TipoTransaccion.GASTO " +
           "AND t.fechaCompra BETWEEN :inicioMes AND :finMes " +
           "GROUP BY t.categoria.nombre")
    List<billetera.project.dto.CategoriaGastoDTO> sumGastosAgrupadosPorCategoria(
        @Param("email") String email, 
        @Param("inicioMes") java.time.LocalDate inicioMes, 
        @Param("finMes") java.time.LocalDate finMes
    );

    @Query("SELECT MONTH(t.fechaCompra), t.tipo, SUM(t.monto) " +
           "FROM Transaccion t " +
           "WHERE t.cuenta.usuario.email = :email " +
           "AND YEAR(t.fechaCompra) = :anio " +
           "GROUP BY MONTH(t.fechaCompra), t.tipo")
    List<Object[]> sumIngresosYGastosPorMes(
        @Param("email") String email,
        @Param("anio") int anio
    );
}
