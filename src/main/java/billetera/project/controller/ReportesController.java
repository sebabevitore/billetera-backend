package billetera.project.controller;

import billetera.project.dto.EvolucionMensualDTO;
import billetera.project.dto.ResumenMensualDTO;
import billetera.project.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReportesController {

    private final ReporteService reporteService;

    @GetMapping("/mensual")
    public ResponseEntity<ResumenMensualDTO> getResumenMensual(
            @RequestParam int mes,
            @RequestParam int anio) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(reporteService.obtenerResumenMensual(email, mes, anio));
    }

    @GetMapping("/anual")
    public ResponseEntity<List<EvolucionMensualDTO>> getEvolucionAnual(
            @RequestParam int anio) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(reporteService.obtenerEvolucionAnual(email, anio));
    }
}
