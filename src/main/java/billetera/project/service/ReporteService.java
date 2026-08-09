package billetera.project.service;

import billetera.project.dto.CategoriaGastoDTO;
import billetera.project.dto.EvolucionMensualDTO;
import billetera.project.dto.ResumenMensualDTO;
import billetera.project.model.TipoTransaccion;
import billetera.project.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private final TransaccionRepository transaccionRepository;

    public ResumenMensualDTO obtenerResumenMensual(String email, int mes, int anio) {
        YearMonth yearMonth = YearMonth.of(anio, mes);
        LocalDate inicioMes = yearMonth.atDay(1);
        LocalDate finMes = yearMonth.atEndOfMonth();

        BigDecimal ingresos = transaccionRepository.sumIngresosByUsuarioAndFechaCompraBetween(email, inicioMes, finMes);
        if (ingresos == null) ingresos = BigDecimal.ZERO;

        BigDecimal gastos = transaccionRepository.sumGastosByUsuarioAndFechaCompraBetween(email, inicioMes, finMes);
        if (gastos == null) gastos = BigDecimal.ZERO;

        BigDecimal balanceNeto = ingresos.subtract(gastos);

        List<CategoriaGastoDTO> categorias = transaccionRepository.sumGastosAgrupadosPorCategoria(email, inicioMes, finMes);

        return new ResumenMensualDTO(ingresos, gastos, balanceNeto, categorias);
    }

    public List<EvolucionMensualDTO> obtenerEvolucionAnual(String email, int anio) {
        List<Object[]> resultados = transaccionRepository.sumIngresosYGastosPorMes(email, anio);
        
        // Inicializar los 12 meses
        BigDecimal[] ingresosPorMes = new BigDecimal[12];
        BigDecimal[] gastosPorMes = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            ingresosPorMes[i] = BigDecimal.ZERO;
            gastosPorMes[i] = BigDecimal.ZERO;
        }

        // Mapear resultados
        for (Object[] fila : resultados) {
            int mes = ((Number) fila[0]).intValue();
            TipoTransaccion tipo = (TipoTransaccion) fila[1];
            BigDecimal suma = (BigDecimal) fila[2];

            if (tipo == TipoTransaccion.INGRESO) {
                ingresosPorMes[mes - 1] = suma;
            } else if (tipo == TipoTransaccion.GASTO) {
                gastosPorMes[mes - 1] = suma;
            }
        }

        List<EvolucionMensualDTO> evolucion = new ArrayList<>();
        String[] nombresMeses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        for (int i = 0; i < 12; i++) {
            evolucion.add(new EvolucionMensualDTO(nombresMeses[i] + " " + anio, ingresosPorMes[i], gastosPorMes[i]));
        }

        return evolucion;
    }
}
