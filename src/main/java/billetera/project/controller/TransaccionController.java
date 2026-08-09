package billetera.project.controller;

import billetera.project.dto.TransaccionRequestDTO;
import billetera.project.dto.TransaccionResponseDTO;
import billetera.project.dto.TransferenciaRequestDTO;
import billetera.project.model.Categoria;
import billetera.project.model.Cuenta;
import billetera.project.model.Transaccion;
import billetera.project.service.TransaccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody TransaccionRequestDTO dto) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

            Transaccion transaccion = new Transaccion();
            transaccion.setMonto(dto.monto());
            transaccion.setDescripcion(dto.descripcion());
            transaccion.setTipo(dto.tipo());
            transaccion.setFechaCompra(dto.fechaCompra());
            transaccion.setEsCompartido(dto.esCompartido() != null ? dto.esCompartido() : false);
            transaccion.setEsGastoFijo(dto.esGastoFijo() != null ? dto.esGastoFijo() : false);
            transaccion.setGastoFijoId(dto.gastoFijoId());
            
            Cuenta cuenta = new Cuenta();
            cuenta.setId(dto.cuentaId());
            transaccion.setCuenta(cuenta);

            if (dto.categoriaId() != null) {
                Categoria categoria = new Categoria();
                categoria.setId(dto.categoriaId());
                transaccion.setCategoria(categoria);
            }

            TransaccionResponseDTO guardada = transaccionService.registrarTransaccion(transaccion, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> crearBatch(@Valid @RequestBody java.util.List<TransaccionRequestDTO> dtos) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            
            java.util.List<Transaccion> transacciones = new java.util.ArrayList<>();
            for (TransaccionRequestDTO dto : dtos) {
                Transaccion transaccion = new Transaccion();
                transaccion.setMonto(dto.monto());
                transaccion.setDescripcion(dto.descripcion());
                transaccion.setTipo(dto.tipo());
                transaccion.setFechaCompra(dto.fechaCompra());
                transaccion.setEsCompartido(dto.esCompartido() != null ? dto.esCompartido() : false);
                transaccion.setEsGastoFijo(dto.esGastoFijo() != null ? dto.esGastoFijo() : false);
                transaccion.setGastoFijoId(dto.gastoFijoId());
                
                Cuenta cuenta = new Cuenta();
                cuenta.setId(dto.cuentaId());
                transaccion.setCuenta(cuenta);

                if (dto.categoriaId() != null) {
                    Categoria categoria = new Categoria();
                    categoria.setId(dto.categoriaId());
                    transaccion.setCategoria(categoria);
                }
                transacciones.add(transaccion);
            }

            java.util.List<TransaccionResponseDTO> guardadas = transaccionService.registrarTransaccionesBatch(transacciones, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardadas);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/transferencia")
    public ResponseEntity<?> transferir(@Valid @RequestBody TransferenciaRequestDTO dto) {
        try {
            transaccionService.registrarTraslado(
                    dto.cuentaOrigenId(),
                    dto.cuentaDestinoId(),
                    dto.monto(),
                    dto.descripcion(),
                    dto.fecha()
            );
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<java.util.List<TransaccionResponseDTO>> listar() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(transaccionService.listarTransacciones(email));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id, 
            @Valid @RequestBody TransaccionRequestDTO dto) {
        
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

            Transaccion transaccion = new Transaccion();
            transaccion.setMonto(dto.monto());
            transaccion.setDescripcion(dto.descripcion());
            transaccion.setTipo(dto.tipo());
            transaccion.setFechaCompra(dto.fechaCompra());
            transaccion.setEsCompartido(dto.esCompartido() != null ? dto.esCompartido() : false);
            transaccion.setEsGastoFijo(dto.esGastoFijo() != null ? dto.esGastoFijo() : false);
            transaccion.setGastoFijoId(dto.gastoFijoId());
            
            Cuenta cuenta = new Cuenta();
            cuenta.setId(dto.cuentaId());
            transaccion.setCuenta(cuenta);

            if (dto.categoriaId() != null) {
                Categoria categoria = new Categoria();
                categoria.setId(dto.categoriaId());
                transaccion.setCategoria(categoria);
            }

            TransaccionResponseDTO actualizada = transaccionService.actualizarTransaccion(id, transaccion, email);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        transaccionService.eliminarTransaccion(id, email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/liquidar")
    public ResponseEntity<Void> liquidarDeuda() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            transaccionService.liquidarDeudaBalance(email);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
