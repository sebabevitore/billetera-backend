package billetera.project.service;

import billetera.project.dto.VinculoResponseDTO;
import billetera.project.model.Usuario;
import billetera.project.model.VinculoPareja;
import billetera.project.repository.UsuarioRepository;
import billetera.project.repository.VinculoParejaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import billetera.project.model.EstadoVinculo;

@Service
@RequiredArgsConstructor
public class VinculoService {

    private final VinculoParejaRepository vinculoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public VinculoResponseDTO vincular(String miEmail, String emailPareja) {
        if (miEmail.equalsIgnoreCase(emailPareja)) {
            throw new IllegalArgumentException("No puedes vincularte contigo mismo");
        }

        Usuario miUsuario = usuarioRepository.findByEmail(miEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        Usuario pareja = usuarioRepository.findByEmail(emailPareja)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no está registrado"));

        List<VinculoPareja> misVinculos = vinculoRepository.findByUsuarioId(miUsuario.getId());
        if (misVinculos.stream().anyMatch(v -> v.getEstado() == EstadoVinculo.ACEPTADO)) {
            throw new IllegalArgumentException("Ya tienes una pareja vinculada");
        }
        if (misVinculos.stream().anyMatch(v -> v.getEstado() == EstadoVinculo.PENDIENTE && v.getUsuario1().getId().equals(miUsuario.getId()))) {
            throw new IllegalArgumentException("Ya has enviado una invitación. Cancélala antes de enviar otra.");
        }

        List<VinculoPareja> parejaVinculos = vinculoRepository.findByUsuarioId(pareja.getId());
        if (parejaVinculos.stream().anyMatch(v -> v.getEstado() == EstadoVinculo.ACEPTADO)) {
            throw new IllegalArgumentException("El usuario destino ya tiene una pareja vinculada");
        }

        VinculoPareja nuevoVinculo = VinculoPareja.builder()
                .usuario1(miUsuario)
                .usuario2(pareja)
                .balance(BigDecimal.ZERO)
                .estado(EstadoVinculo.PENDIENTE)
                .build();

        vinculoRepository.save(nuevoVinculo);

        return mapToDTO(nuevoVinculo, miUsuario.getId());
    }

    @Transactional(readOnly = true)
    public VinculoResponseDTO getVinculoActivo(String miEmail) {
        Usuario miUsuario = usuarioRepository.findByEmail(miEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<VinculoPareja> vinculosAceptados = vinculoRepository.findByUsuarioIdAndEstado(miUsuario.getId(), EstadoVinculo.ACEPTADO);
        if (!vinculosAceptados.isEmpty()) {
            return mapToDTO(vinculosAceptados.get(0), miUsuario.getId());
        }

        // Si no hay aceptado, buscamos uno que yo haya enviado y esté pendiente
        List<VinculoPareja> misVinculos = vinculoRepository.findByUsuarioId(miUsuario.getId());
        return misVinculos.stream()
                .filter(v -> v.getEstado() == EstadoVinculo.PENDIENTE && v.getUsuario1().getId().equals(miUsuario.getId()))
                .findFirst()
                .map(v -> mapToDTO(v, miUsuario.getId()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<VinculoResponseDTO> getPendientes(String miEmail) {
        Usuario miUsuario = usuarioRepository.findByEmail(miEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return vinculoRepository.findByUsuario2IdAndEstado(miUsuario.getId(), EstadoVinculo.PENDIENTE).stream()
                .map(v -> mapToDTO(v, miUsuario.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void responderInvitacion(Long vinculoId, String miEmail, boolean aceptar) {
        Usuario miUsuario = usuarioRepository.findByEmail(miEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        VinculoPareja vinculo = vinculoRepository.findById(vinculoId)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada"));

        if (!vinculo.getUsuario2().getId().equals(miUsuario.getId())) {
            throw new IllegalArgumentException("No tienes permiso para responder a esta invitación");
        }

        if (vinculo.getEstado() != EstadoVinculo.PENDIENTE) {
            throw new IllegalArgumentException("La invitación ya no está pendiente");
        }

        if (aceptar) {
            // Check if user already has an accepted link
            if (!vinculoRepository.findByUsuarioIdAndEstado(miUsuario.getId(), EstadoVinculo.ACEPTADO).isEmpty()) {
                throw new IllegalArgumentException("Ya tienes una pareja vinculada");
            }
            if (!vinculoRepository.findByUsuarioIdAndEstado(vinculo.getUsuario1().getId(), EstadoVinculo.ACEPTADO).isEmpty()) {
                throw new IllegalArgumentException("El emisor ya se vinculó con otra persona");
            }
            
            vinculo.setEstado(EstadoVinculo.ACEPTADO);
            vinculoRepository.save(vinculo);
            
            // Delete any other pending invitations received or sent by these two users
            List<VinculoPareja> todosMisVinculos = vinculoRepository.findByUsuarioId(miUsuario.getId());
            todosMisVinculos.stream()
                    .filter(v -> v.getEstado() == EstadoVinculo.PENDIENTE && !v.getId().equals(vinculoId))
                    .forEach(vinculoRepository::delete);
                    
            List<VinculoPareja> todosEmisorVinculos = vinculoRepository.findByUsuarioId(vinculo.getUsuario1().getId());
            todosEmisorVinculos.stream()
                    .filter(v -> v.getEstado() == EstadoVinculo.PENDIENTE && !v.getId().equals(vinculoId))
                    .forEach(vinculoRepository::delete);
        } else {
            vinculoRepository.delete(vinculo);
        }
    }

    @Transactional
    public void eliminarVinculo(String miEmail) {
        Usuario miUsuario = usuarioRepository.findByEmail(miEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<VinculoPareja> vinculos = vinculoRepository.findByUsuarioId(miUsuario.getId());
        VinculoPareja vinculoAceptado = vinculos.stream()
                .filter(v -> v.getEstado() == EstadoVinculo.ACEPTADO)
                .findFirst()
                .orElse(null);
                
        if (vinculoAceptado != null) {
            vinculoRepository.delete(vinculoAceptado);
            return;
        }
        
        // Si no tiene aceptado, eliminar la pendiente enviada
        VinculoPareja vinculoPendiente = vinculos.stream()
                .filter(v -> v.getEstado() == EstadoVinculo.PENDIENTE && v.getUsuario1().getId().equals(miUsuario.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No tienes ningún vínculo para cancelar"));
                
        vinculoRepository.delete(vinculoPendiente);
    }

    @Transactional
    public void desvincularPareja(String miEmail) {
        Usuario miUsuario = usuarioRepository.findByEmail(miEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<VinculoPareja> vinculosAceptados = vinculoRepository.findByUsuarioIdAndEstado(miUsuario.getId(), EstadoVinculo.ACEPTADO);
        if (vinculosAceptados.isEmpty()) {
            throw new IllegalArgumentException("No tienes una pareja vinculada activa para desvincular");
        }

        VinculoPareja vinculo = vinculosAceptados.get(0);

        if (vinculo.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("No pueden desvincularse si existen deudas pendientes. Liquiden el balance a $0 primero.");
        }

        vinculo.setEstado(EstadoVinculo.INACTIVO);
        vinculoRepository.save(vinculo);
    }

    private VinculoResponseDTO mapToDTO(VinculoPareja vinculo, Long miUsuarioId) {
        boolean soyUsuario1 = vinculo.getUsuario1().getId().equals(miUsuarioId);
        Usuario pareja = soyUsuario1 ? vinculo.getUsuario2() : vinculo.getUsuario1();
        
        // Si soy usuario1, mi balance es el balance del vínculo.
        // Si soy usuario2, mi balance es el inverso del balance del vínculo.
        BigDecimal miBalance = soyUsuario1 ? vinculo.getBalance() : vinculo.getBalance().negate();

        return new VinculoResponseDTO(
                vinculo.getId(),
                pareja.getId(),
                pareja.getNombre(),
                pareja.getEmail(),
                miBalance,
                vinculo.getEstado().name(),
                soyUsuario1
        );
    }
}
