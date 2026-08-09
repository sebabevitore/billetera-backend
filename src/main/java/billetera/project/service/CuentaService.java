package billetera.project.service;

import billetera.project.dto.CuentaRequestDTO;
import billetera.project.dto.CuentaResponseDTO;
import billetera.project.model.Cuenta;
import billetera.project.model.Usuario;
import billetera.project.model.TipoTransaccion;
import billetera.project.model.Transaccion;
import billetera.project.model.Transaccion;
import billetera.project.model.TipoCuenta;
import billetera.project.repository.CuentaRepository;
import billetera.project.repository.TransaccionRepository;
import billetera.project.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransaccionRepository transaccionRepository;

    public CuentaResponseDTO crear(CuentaRequestDTO dto, String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cuenta cuenta = Cuenta.builder()
                .nombre(dto.nombre())
                .tipo(dto.tipo())
                .diaCierre(dto.diaCierre())
                .esCuentaAhorro(dto.esCuentaAhorro())
                .fechaCierre(dto.fechaCierre())
                .fechaVencimiento(dto.fechaVencimiento())
                .usuario(usuario)
                .build();

        cuenta = cuentaRepository.save(cuenta);

        if (dto.saldoInicial() != null && dto.saldoInicial().compareTo(BigDecimal.ZERO) > 0) {
            Transaccion transaccionInicial = Transaccion.builder()
                    .cuenta(cuenta)
                    .monto(dto.saldoInicial())
                    .descripcion("Saldo Inicial")
                    .tipo(TipoTransaccion.INGRESO)
                    .fechaCompra(java.time.LocalDate.now())
                    .fechaImputacion(java.time.LocalDate.now())
                    .build();
            transaccionRepository.save(transaccionInicial);
        }

        return mapToDTO(cuenta);
    }

    @Transactional(readOnly = true)
    public CuentaResponseDTO obtenerPorId(Long id) {
        Cuenta cuenta = cuentaRepository.findByIdWithUsuario(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        return mapToDTO(cuenta);
    }

    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> listarPorUsuarioEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
        return cuentaRepository.findByUsuarioId(usuario.getId()).stream()
                .map(this::mapToDTO)
                .toList();
    }

    public CuentaResponseDTO actualizar(Long id, CuentaRequestDTO dto, String email) {
        Cuenta cuenta = cuentaRepository.findByIdWithUsuario(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));

        if (!cuenta.getUsuario().getEmail().equals(email)) {
            throw new SecurityException("No tienes permiso para modificar esta cuenta");
        }

        cuenta.setNombre(dto.nombre());
        cuenta.setFechaCierre(dto.fechaCierre());
        cuenta.setFechaVencimiento(dto.fechaVencimiento());
        
        if (dto.esPrincipal() != null && dto.esPrincipal()) {
            cuenta.setEsPrincipal(true);
            List<Cuenta> cuentasUsuario = cuentaRepository.findByUsuarioId(cuenta.getUsuario().getId());
            for (Cuenta c : cuentasUsuario) {
                if (!c.getId().equals(cuenta.getId()) && c.isEsPrincipal()) {
                    c.setEsPrincipal(false);
                    cuentaRepository.save(c);
                }
            }
        } else if (dto.esPrincipal() != null && !dto.esPrincipal()) {
            cuenta.setEsPrincipal(false);
        }

        cuenta = cuentaRepository.save(cuenta);
        return mapToDTO(cuenta);
    }

    public void eliminar(Long id, String email) {
        Cuenta cuenta = cuentaRepository.findByIdWithUsuario(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));

        if (!cuenta.getUsuario().getEmail().equals(email)) {
            throw new SecurityException("No tienes permiso para eliminar esta cuenta");
        }

        try {
            cuentaRepository.delete(cuenta);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new IllegalArgumentException("No se puede eliminar la cuenta porque tiene transacciones asociadas.");
        }
    }

    private CuentaResponseDTO mapToDTO(Cuenta cuenta) {
        BigDecimal saldoActual = transaccionRepository.findByCuentaId(cuenta.getId()).stream()
                .filter(t -> {
                    if (cuenta.getTipo() == TipoCuenta.TARJETA_CREDITO && cuenta.getFechaCierre() != null) {
                        if (t.getTipo() == TipoTransaccion.INGRESO || t.getTipo() == TipoTransaccion.TRANSFERENCIA_ENTRADA) {
                            return true;
                        }
                        return !t.getFechaCompra().isAfter(cuenta.getFechaCierre());
                    }
                    return true;
                })
                .map(t -> {
                    if (t.getTipo() == TipoTransaccion.INGRESO || t.getTipo() == TipoTransaccion.TRANSFERENCIA_ENTRADA) {
                        return t.getMonto();
                    } else {
                        return t.getMonto().negate();
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CuentaResponseDTO(
                cuenta.getId(),
                cuenta.getNombre(),
                cuenta.getTipo(),
                cuenta.getDiaCierre(),
                cuenta.getUsuario().getId(),
                cuenta.isEsPrincipal(),
                cuenta.isEsCuentaAhorro(),
                saldoActual,
                cuenta.getFechaCierre(),
                cuenta.getFechaVencimiento()
        );
    }
}
