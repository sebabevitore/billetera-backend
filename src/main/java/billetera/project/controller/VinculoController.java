package billetera.project.controller;

import billetera.project.dto.VinculoRequestDTO;
import billetera.project.dto.VinculoResponseDTO;
import billetera.project.service.VinculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vinculos")
@RequiredArgsConstructor
public class VinculoController {

    private final VinculoService vinculoService;

    @PostMapping
    public ResponseEntity<?> vincular(@Valid @RequestBody VinculoRequestDTO dto) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(vinculoService.vincular(email, dto.emailPareja()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<VinculoResponseDTO> getVinculoActivo() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        VinculoResponseDTO vinculo = vinculoService.getVinculoActivo(email);
        if (vinculo == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(vinculo);
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> eliminarVinculo() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            vinculoService.eliminarVinculo(email);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pendientes")
    public ResponseEntity<java.util.List<VinculoResponseDTO>> getPendientes() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(vinculoService.getPendientes(email));
    }

    @PutMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptarInvitacion(@PathVariable Long id) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            vinculoService.responderInvitacion(id, email, true);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarInvitacion(@PathVariable Long id) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            vinculoService.responderInvitacion(id, email, false);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/desvincular")
    public ResponseEntity<?> desvincularPareja() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            vinculoService.desvincularPareja(email);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
