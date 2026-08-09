package billetera.project.controller;

import billetera.project.model.Usuario;
import billetera.project.security.JwtUtil;
import billetera.project.service.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String token = body.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El token es requerido"));
        }

        try {
            Usuario usuario = usuarioService.procesarLoginGoogle(token);
            
            // Generar JWT
            String jwt = jwtUtil.generateToken(usuario.getEmail(), Collections.emptySet());
            
            // Configurar cookie
            Cookie cookie = new Cookie("jwt", jwt);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Cambiar a true en producción con HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(86400); // 1 día
            response.addCookie(cookie);
            
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno del servidor"));
        }
    }
}
