package billetera.project.service;

import billetera.project.dto.RegisterRequestDTO;
import billetera.project.dto.UsuarioRequestDTO;
import billetera.project.dto.UsuarioResponseDTO;
import billetera.project.model.Categoria;
import billetera.project.model.TipoTransaccion;
import billetera.project.model.Usuario;
import billetera.project.repository.CategoriaRepository;
import billetera.project.model.Cuenta;
import billetera.project.model.TipoCuenta;
import billetera.project.repository.CuentaRepository;
import billetera.project.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${google.client.id}")
    private String googleClientId;

    @Transactional
    public UsuarioResponseDTO registrarUsuario(RegisterRequestDTO dto) {
        if (usuarioRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("El email ya se encuentra registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(dto.nombre())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .build();
        usuario = usuarioRepository.save(usuario);

        crearDatosPorDefecto(usuario);

        return mapToDTO(usuario);
    }

    @Transactional
    public Usuario procesarLoginGoogle(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String picture = (String) payload.get("picture");

                Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
                if (usuario == null) {
                    usuario = Usuario.builder()
                            .nombre(name)
                            .email(email)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .fotoPerfil(picture)
                            .build();
                    usuario = usuarioRepository.save(usuario);
                    crearDatosPorDefecto(usuario);
                } else {
                    usuario.setNombre(name);
                    usuario.setFotoPerfil(picture);
                    usuario = usuarioRepository.save(usuario);
                }
                return usuario;
            } else {
                throw new IllegalArgumentException("Token de Google inválido");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al verificar token de Google: " + e.getMessage());
        }
    }

    private void crearDatosPorDefecto(Usuario usuario) {
        Cuenta cuentaPrincipal = Cuenta.builder()
                .nombre("Banco")
                .tipo(TipoCuenta.TARJETA_DEBITO)
                .esPrincipal(true)
                .esCuentaAhorro(false)
                .usuario(usuario)
                .build();
        cuentaRepository.save(cuentaPrincipal);

        List<String> gastos = List.of("Supermercado", "Salidas", "Suscripciones", "Auto", "Ropa", "Otros",
                "Deportes", "Transporte", "Casa", "Salud y Cuidado Personal", "Vacaciones", "Regalos");
        List<String> ingresos = List.of("Salario", "Venta", "Interés", "Aguinaldo", "Reintegro");

        gastos.forEach(nombre -> categoriaRepository.save(Categoria.builder()
                .nombre(nombre)
                .tipo(TipoTransaccion.GASTO)
                .usuario(usuario)
                .build()));

        ingresos.forEach(nombre -> categoriaRepository.save(Categoria.builder()
                .nombre(nombre)
                .tipo(TipoTransaccion.INGRESO)
                .usuario(usuario)
                .build()));
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    private UsuarioResponseDTO mapToDTO(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
