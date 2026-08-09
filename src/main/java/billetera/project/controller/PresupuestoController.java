package billetera.project.controller;

import billetera.project.dto.PresupuestoCategoriaDTO;
import billetera.project.dto.PresupuestoRequestDTO;
import billetera.project.service.PresupuestoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presupuestos")
@RequiredArgsConstructor
public class PresupuestoController {

    private final PresupuestoService presupuestoService;

    @GetMapping
    public ResponseEntity<List<PresupuestoCategoriaDTO>> obtenerPresupuestos() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(presupuestoService.obtenerPresupuestos(email));
    }

    @PutMapping("/{categoriaId}")
    public ResponseEntity<Void> actualizarLimite(@PathVariable Long categoriaId, @Valid @RequestBody PresupuestoRequestDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        presupuestoService.actualizarLimite(categoriaId, dto, email);
        return ResponseEntity.ok().build();
    }
}
