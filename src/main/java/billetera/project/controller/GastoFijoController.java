package billetera.project.controller;

import billetera.project.dto.GastosFijosDashboardDTO;
import billetera.project.dto.GastoFijoResponseDTO;
import billetera.project.dto.GastoFijoRequestDTO;
import billetera.project.service.GastoFijoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gastos-fijos")
@RequiredArgsConstructor
public class GastoFijoController {

    private final GastoFijoService gastoFijoService;

    @GetMapping
    public ResponseEntity<GastosFijosDashboardDTO> obtenerDashboard() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(gastoFijoService.obtenerDashboard(email));
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody GastoFijoRequestDTO dto) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            GastoFijoResponseDTO creado = gastoFijoService.crear(dto, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody GastoFijoRequestDTO dto) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            GastoFijoResponseDTO actualizado = gastoFijoService.actualizar(id, dto, email);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            gastoFijoService.eliminar(id, email);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
