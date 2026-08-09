package billetera.project.controller;

import billetera.project.model.Usuario;
import billetera.project.security.JwtUtil;
import billetera.project.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El token es requerido"));
        }

        try {
            Usuario usuario = usuarioService.procesarLoginGoogle(token);

            // Generar JWT
            String jwt = jwtUtil.generateToken(usuario.getEmail(), Collections.emptySet());

            // Configurar cookie con Spring ResponseCookie para atributos seguros y SameSite
            ResponseCookie springCookie = ResponseCookie.from("jwt", jwt)
                    .httpOnly(true)
                    .secure(true) // Obligatorio para HTTPS en producción
                    .sameSite("None") // Obligatorio para peticiones cruzadas (Vercel -> Railway)
                    .path("/")
                    .maxAge(86400) // 1 día
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, springCookie.toString())
                    .body(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno del servidor"));
        }
    }
}
