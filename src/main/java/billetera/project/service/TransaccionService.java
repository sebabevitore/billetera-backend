package billetera.project.service;

import billetera.project.model.Categoria;
import billetera.project.model.Cuenta;
import billetera.project.model.TipoCuenta;
import billetera.project.model.TipoTransaccion;
import billetera.project.model.Transaccion;
import billetera.project.dto.TransaccionResponseDTO;
import billetera.project.model.Usuario;
import billetera.project.repository.CategoriaRepository;
import billetera.project.repository.CuentaRepository;
import billetera.project.repository.TransaccionRepository;
import billetera.project.repository.UsuarioRepository;
import billetera.project.repository.GastoFijoRepository;
import billetera.project.model.GastoFijo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final billetera.project.repository.VinculoParejaRepository vinculoParejaRepository;
    private final GastoFijoRepository gastoFijoRepository;

    @Transactional
    public TransaccionResponseDTO registrarTransaccion(Transaccion transaccion, String userEmail) {
        Cuenta cuenta = cuentaRepository.findById(transaccion.getCuenta().getId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        
        if (!cuenta.getUsuario().getEmail().equals(userEmail)) {
            throw new SecurityException("No tienes permiso para operar con esta cuenta");
        }

        if (transaccion.getCategoria() != null) {
            Categoria categoria = categoriaRepository.findById(transaccion.getCategoria().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            
            if (!categoria.getUsuario().getEmail().equals(userEmail)) {
                throw new SecurityException("No tienes permiso para operar con esta categoría");
            }
            transaccion.setCategoria(categoria);
        }
        
        transaccion.setCuenta(cuenta);
        calcularFechaImputacion(transaccion, cuenta);

        if (Boolean.TRUE.equals(transaccion.getEsCompartido()) && transaccion.getTipo() == TipoTransaccion.GASTO) {
            BigDecimal mitad = transaccion.getMonto().divide(BigDecimal.valueOf(2), java.math.RoundingMode.HALF_UP);
            transaccion.setMontoEstadistico(mitad);
            
            Usuario creador = cuenta.getUsuario();
            java.util.List<billetera.project.model.VinculoPareja> vinculos = vinculoParejaRepository.findByUsuarioIdAndEstado(creador.getId(), billetera.project.model.EstadoVinculo.ACEPTADO);
            
            if (!vinculos.isEmpty()) {
                billetera.project.model.VinculoPareja vinculo = vinculos.get(0);
                boolean soyUsuario1 = vinculo.getUsuario1().getId().equals(creador.getId());
                Usuario pareja = soyUsuario1 ? vinculo.getUsuario2() : vinculo.getUsuario1();
                
                // Si soy usuario 1 y pagué yo, el usuario 2 me debe "mitad". El balance suma "mitad".
                // Si soy usuario 2 y pagué yo, el usuario 1 me debe "mitad". El balance (perspectiva u1) resta "mitad".
                if (soyUsuario1) {
                    vinculo.setBalance(vinculo.getBalance().add(mitad));
                } else {
                    vinculo.setBalance(vinculo.getBalance().subtract(mitad));
                }
                vinculoParejaRepository.save(vinculo);
                
                Cuenta cuentaPareja = cuentaRepository.findByUsuarioId(pareja.getId()).stream().findFirst().orElse(null);
                if (cuentaPareja != null) {
                    Transaccion statTx = Transaccion.builder()
                        .monto(BigDecimal.ZERO)
                        .montoEstadistico(mitad)
                        .descripcion(transaccion.getDescripcion() + " (Compartido)")
                        .tipo(TipoTransaccion.GASTO)
                        .fechaCompra(transaccion.getFechaCompra())
                        .fechaImputacion(transaccion.getFechaImputacion())
                        .cuenta(cuentaPareja)
                        .esCompartido(true)
                        .build();
                    
                    if (transaccion.getCategoria() != null) {
                        Categoria catPareja = categoriaRepository.findByUsuarioEmail(pareja.getEmail())
                            .stream().filter(c -> c.getNombre().equals(transaccion.getCategoria().getNombre()))
                            .findFirst().orElse(null);
                        statTx.setCategoria(catPareja);
                    }
                    transaccionRepository.save(statTx);
                }
            }
        } else {
            transaccion.setMontoEstadistico(transaccion.getMonto());
        }

        if (Boolean.TRUE.equals(transaccion.getEsGastoFijo()) && transaccion.getGastoFijoId() == null) {
            GastoFijo nuevoGastoFijo = GastoFijo.builder()
                    .descripcion(transaccion.getDescripcion())
                    .montoEstimado(transaccion.getMonto())
                    .categoria(transaccion.getCategoria())
                    .diaVencimiento(transaccion.getFechaCompra().getDayOfMonth())
                    .usuario(cuenta.getUsuario())
                    .build();
            nuevoGastoFijo = gastoFijoRepository.save(nuevoGastoFijo);
            transaccion.setGastoFijoId(nuevoGastoFijo.getId());
        }

        if (transaccion.getGastoFijoId() != null) {
            GastoFijo gf = gastoFijoRepository.findById(transaccion.getGastoFijoId()).orElse(null);
            if (gf != null) {
                gf.setMontoEstimado(transaccion.getMonto());
                gf.setDescripcion(transaccion.getDescripcion());
                gastoFijoRepository.save(gf);
            }
        }

        Transaccion guardada = transaccionRepository.save(transaccion);
        return mapToDTO(guardada);
    }

    private void calcularFechaImputacion(Transaccion transaccion, Cuenta cuenta) {
        if (transaccion.getTipo() == TipoTransaccion.GASTO && 
            cuenta.getTipo() == TipoCuenta.TARJETA_CREDITO && 
            cuenta.getDiaCierre() != null) {

            LocalDate fechaCompra = transaccion.getFechaCompra();
            
            if (fechaCompra.getDayOfMonth() > cuenta.getDiaCierre()) {
                transaccion.setFechaImputacion(fechaCompra.plusMonths(1));
            } else {
                transaccion.setFechaImputacion(fechaCompra);
            }
        } else {
            transaccion.setFechaImputacion(transaccion.getFechaCompra());
        }
    }

    @Transactional
    public void registrarTraslado(Long idCuentaOrigen, Long idCuentaDestino, BigDecimal monto, String descripcion, LocalDate fecha) {
        Cuenta cuentaOrigen = cuentaRepository.findById(idCuentaOrigen)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de origen no encontrada"));
        Cuenta cuentaDestino = cuentaRepository.findById(idCuentaDestino)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de destino no encontrada"));

        BigDecimal saldoOrigen = transaccionRepository.findByCuentaId(cuentaOrigen.getId()).stream()
                .map(t -> {
                    if (t.getTipo() == TipoTransaccion.INGRESO || t.getTipo() == TipoTransaccion.TRANSFERENCIA_ENTRADA) {
                        return t.getMonto();
                    } else {
                        return t.getMonto().negate();
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (monto.compareTo(saldoOrigen) > 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar el traslado");
        }

        Transaccion salida = Transaccion.builder()
                .cuenta(cuentaOrigen)
                .monto(monto)
                .montoEstadistico(monto)
                .esCompartido(false)
                .descripcion(descripcion)
                .tipo(TipoTransaccion.TRANSFERENCIA_SALIDA)
                .fechaCompra(fecha)
                .fechaImputacion(fecha)
                .build();

        Transaccion entrada = Transaccion.builder()
                .cuenta(cuentaDestino)
                .monto(monto)
                .montoEstadistico(monto)
                .esCompartido(false)
                .descripcion(descripcion)
                .tipo(TipoTransaccion.TRANSFERENCIA_ENTRADA)
                .fechaCompra(fecha)
                .fechaImputacion(fecha)
                .build();

        salida = transaccionRepository.save(salida);
        entrada = transaccionRepository.save(entrada);

        salida.setIdTransferenciaVinculada(entrada.getId());
        entrada.setIdTransferenciaVinculada(salida.getId());

        transaccionRepository.save(salida);
        transaccionRepository.save(entrada);
    }
    
    @Transactional(readOnly = true)
    public java.util.List<TransaccionResponseDTO> listarTransacciones(String userEmail) {
        return transaccionRepository.findByCuentaUsuarioEmailOrderByFechaCompraDesc(userEmail).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public TransaccionResponseDTO actualizarTransaccion(Long id, Transaccion transaccionActualizada, String userEmail) {
        Transaccion transaccionOriginal = transaccionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));

        if (!transaccionOriginal.getCuenta().getUsuario().getEmail().equals(userEmail)) {
            throw new SecurityException("No tienes permiso para modificar esta transacción");
        }

        // Validate new account if changed
        if (!transaccionOriginal.getCuenta().getId().equals(transaccionActualizada.getCuenta().getId())) {
            Cuenta nuevaCuenta = cuentaRepository.findById(transaccionActualizada.getCuenta().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
            if (!nuevaCuenta.getUsuario().getEmail().equals(userEmail)) {
                throw new SecurityException("No tienes permiso para operar con esta cuenta");
            }
            transaccionOriginal.setCuenta(nuevaCuenta);
        }

        // Validate new category if changed
        if (transaccionActualizada.getCategoria() != null) {
            if (transaccionOriginal.getCategoria() == null || !transaccionOriginal.getCategoria().getId().equals(transaccionActualizada.getCategoria().getId())) {
                Categoria nuevaCategoria = categoriaRepository.findById(transaccionActualizada.getCategoria().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                if (!nuevaCategoria.getUsuario().getEmail().equals(userEmail)) {
                    throw new SecurityException("No tienes permiso para operar con esta categoría");
                }
                transaccionOriginal.setCategoria(nuevaCategoria);
            }
        } else {
            transaccionOriginal.setCategoria(null);
        }

        boolean eraCompartido = Boolean.TRUE.equals(transaccionOriginal.getEsCompartido());
        boolean esAhoraCompartido = Boolean.TRUE.equals(transaccionActualizada.getEsCompartido());
        BigDecimal montoOriginal = transaccionOriginal.getMonto();
        BigDecimal montoNuevo = transaccionActualizada.getMonto();
        
        Usuario creador = transaccionOriginal.getCuenta().getUsuario();
        java.util.List<billetera.project.model.VinculoPareja> vinculos = vinculoParejaRepository.findByUsuarioIdAndEstado(creador.getId(), billetera.project.model.EstadoVinculo.ACEPTADO);
        billetera.project.model.VinculoPareja vinculo = vinculos.isEmpty() ? null : vinculos.get(0);

        if (vinculo != null && transaccionActualizada.getTipo() == TipoTransaccion.GASTO) {
            boolean soyUsuario1 = vinculo.getUsuario1().getId().equals(creador.getId());
            
            // Caso A: Era compartido y AHORA NO LO ES (Deshacer)
            if (eraCompartido && !esAhoraCompartido) {
                BigDecimal mitadOriginal = montoOriginal.divide(BigDecimal.valueOf(2), java.math.RoundingMode.HALF_UP);
                if (soyUsuario1) {
                    vinculo.setBalance(vinculo.getBalance().subtract(mitadOriginal));
                } else {
                    vinculo.setBalance(vinculo.getBalance().add(mitadOriginal));
                }
                transaccionOriginal.setEsCompartido(false);
                transaccionOriginal.setMontoEstadistico(montoNuevo);
            }
            // Caso B: No era compartido y AHORA SÍ LO ES
            else if (!eraCompartido && esAhoraCompartido) {
                BigDecimal mitadNueva = montoNuevo.divide(BigDecimal.valueOf(2), java.math.RoundingMode.HALF_UP);
                if (soyUsuario1) {
                    vinculo.setBalance(vinculo.getBalance().add(mitadNueva));
                } else {
                    vinculo.setBalance(vinculo.getBalance().subtract(mitadNueva));
                }
                transaccionOriginal.setEsCompartido(true);
                transaccionOriginal.setMontoEstadistico(mitadNueva);
            }
            // Caso C: Era compartido y SIGUE SIENDO (posible cambio de monto)
            else if (eraCompartido && esAhoraCompartido) {
                BigDecimal mitadOriginal = montoOriginal.divide(BigDecimal.valueOf(2), java.math.RoundingMode.HALF_UP);
                BigDecimal mitadNueva = montoNuevo.divide(BigDecimal.valueOf(2), java.math.RoundingMode.HALF_UP);
                BigDecimal diferencia = mitadNueva.subtract(mitadOriginal);
                
                if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
                    if (soyUsuario1) {
                        vinculo.setBalance(vinculo.getBalance().add(diferencia));
                    } else {
                        vinculo.setBalance(vinculo.getBalance().subtract(diferencia));
                    }
                }
                transaccionOriginal.setEsCompartido(true);
                transaccionOriginal.setMontoEstadistico(mitadNueva);
            } else {
                transaccionOriginal.setEsCompartido(false);
                transaccionOriginal.setMontoEstadistico(montoNuevo);
            }
            vinculoParejaRepository.save(vinculo);
        } else {
            transaccionOriginal.setEsCompartido(false);
            transaccionOriginal.setMontoEstadistico(montoNuevo);
        }

        transaccionOriginal.setMonto(montoNuevo);
        transaccionOriginal.setDescripcion(transaccionActualizada.getDescripcion());
        transaccionOriginal.setTipo(transaccionActualizada.getTipo());
        transaccionOriginal.setFechaCompra(transaccionActualizada.getFechaCompra());
        transaccionOriginal.setEsGastoFijo(transaccionActualizada.getEsGastoFijo());

        if (Boolean.TRUE.equals(transaccionActualizada.getEsGastoFijo()) && transaccionActualizada.getGastoFijoId() == null && transaccionOriginal.getGastoFijoId() == null) {
            GastoFijo nuevoGastoFijo = GastoFijo.builder()
                    .descripcion(transaccionActualizada.getDescripcion())
                    .montoEstimado(transaccionActualizada.getMonto())
                    .categoria(transaccionOriginal.getCategoria())
                    .diaVencimiento(transaccionActualizada.getFechaCompra().getDayOfMonth())
                    .usuario(creador)
                    .build();
            nuevoGastoFijo = gastoFijoRepository.save(nuevoGastoFijo);
            transaccionOriginal.setGastoFijoId(nuevoGastoFijo.getId());
        } else {
            transaccionOriginal.setGastoFijoId(transaccionActualizada.getGastoFijoId() != null ? transaccionActualizada.getGastoFijoId() : transaccionOriginal.getGastoFijoId());
        }
        
        calcularFechaImputacion(transaccionOriginal, transaccionOriginal.getCuenta());

        if (transaccionOriginal.getGastoFijoId() != null) {
            GastoFijo gf = gastoFijoRepository.findById(transaccionOriginal.getGastoFijoId()).orElse(null);
            if (gf != null) {
                gf.setMontoEstimado(transaccionOriginal.getMonto());
                gf.setDescripcion(transaccionOriginal.getDescripcion());
                gastoFijoRepository.save(gf);
            }
        }

        Transaccion guardada = transaccionRepository.save(transaccionOriginal);
        return mapToDTO(guardada);
    }

    @Transactional
    public void eliminarTransaccion(Long id, String userEmail) {
        Transaccion transaccion = transaccionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));

        if (!transaccion.getCuenta().getUsuario().getEmail().equals(userEmail)) {
            throw new SecurityException("No tienes permiso para eliminar esta transacción");
        }

        transaccionRepository.delete(transaccion);
    }

    @Transactional
    public void liquidarDeudaBalance(String userEmail) {
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        java.util.List<billetera.project.model.VinculoPareja> vinculos = vinculoParejaRepository.findByUsuarioIdAndEstado(usuario.getId(), billetera.project.model.EstadoVinculo.ACEPTADO);
        if (vinculos.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene una pareja vinculada activa");
        }
        billetera.project.model.VinculoPareja vinculo = vinculos.get(0);
                
        if (vinculo.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            return; // Nada que liquidar
        }

        boolean soyUsuario1 = vinculo.getUsuario1().getId().equals(usuario.getId());
        Usuario pareja = soyUsuario1 ? vinculo.getUsuario2() : vinculo.getUsuario1();

        Cuenta cuentaUsuario = cuentaRepository.findByUsuarioId(usuario.getId()).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene cuentas"));
        Cuenta cuentaPareja = cuentaRepository.findByUsuarioId(pareja.getId()).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La pareja no tiene cuentas"));

        // El balance desde mi perspectiva:
        BigDecimal balance = soyUsuario1 ? vinculo.getBalance() : vinculo.getBalance().negate();
        LocalDate hoy = LocalDate.now();
        
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            // Pareja me debe, Pareja paga -> Usuario recibe
            Transaccion egresoPareja = Transaccion.builder()
                .cuenta(cuentaPareja)
                .monto(balance)
                .montoEstadistico(BigDecimal.ZERO)
                .descripcion("Liquidación de deuda a " + usuario.getNombre())
                .tipo(TipoTransaccion.LIQUIDACION_DEUDA)
                .fechaCompra(hoy)
                .fechaImputacion(hoy)
                .esCompartido(false)
                .build();
            Transaccion ingresoUsuario = Transaccion.builder()
                .cuenta(cuentaUsuario)
                .monto(balance)
                .montoEstadistico(BigDecimal.ZERO)
                .descripcion("Liquidación de deuda de " + pareja.getNombre())
                .tipo(TipoTransaccion.LIQUIDACION_DEUDA) // or INGRESO? No, LIQUIDACION_DEUDA is fine if frontend treats it as ingreso or if we sum it differently.
                .fechaCompra(hoy)
                .fechaImputacion(hoy)
                .esCompartido(false)
                .build();
            transaccionRepository.save(egresoPareja);
            transaccionRepository.save(ingresoUsuario);
        } else {
            // Yo debo, Yo pago -> Pareja recibe
            BigDecimal deuda = balance.negate();
            Transaccion egresoUsuario = Transaccion.builder()
                .cuenta(cuentaUsuario)
                .monto(deuda)
                .montoEstadistico(BigDecimal.ZERO)
                .descripcion("Liquidación de deuda a " + pareja.getNombre())
                .tipo(TipoTransaccion.LIQUIDACION_DEUDA)
                .fechaCompra(hoy)
                .fechaImputacion(hoy)
                .esCompartido(false)
                .build();
            Transaccion ingresoPareja = Transaccion.builder()
                .cuenta(cuentaPareja)
                .monto(deuda)
                .montoEstadistico(BigDecimal.ZERO)
                .descripcion("Liquidación de deuda de " + usuario.getNombre())
                .tipo(TipoTransaccion.LIQUIDACION_DEUDA)
                .fechaCompra(hoy)
                .fechaImputacion(hoy)
                .esCompartido(false)
                .build();
            transaccionRepository.save(egresoUsuario);
            transaccionRepository.save(ingresoPareja);
        }

        // Resetear balance del vínculo
        vinculo.setBalance(BigDecimal.ZERO);
        vinculoParejaRepository.save(vinculo);
    }

    @Transactional
    public java.util.List<TransaccionResponseDTO> registrarTransaccionesBatch(java.util.List<Transaccion> transacciones, String userEmail) {
        java.util.List<TransaccionResponseDTO> registradas = new java.util.ArrayList<>();
        for (Transaccion t : transacciones) {
            registradas.add(registrarTransaccion(t, userEmail));
        }
        return registradas;
    }

    private TransaccionResponseDTO mapToDTO(Transaccion t) {
        return new TransaccionResponseDTO(
                t.getId(),
                t.getMonto(),
                t.getDescripcion(),
                t.getTipo(),
                t.getFechaCompra(),
                t.getFechaImputacion(),
                t.getCuenta().getId(),
                t.getCuenta().getNombre(),
                t.getCategoria() != null ? t.getCategoria().getId() : null,
                t.getCategoria() != null ? t.getCategoria().getNombre() : null,
                t.getIdTransferenciaVinculada(),
                t.getEsCompartido(),
                t.getMontoEstadistico(),
                t.getEsGastoFijo(),
                t.getGastoFijoId()
        );
    }
}
