package billetera.project.service;

import billetera.project.model.*;
import billetera.project.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransaccionRepository transaccionRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final GastoFijoRepository gastoFijoRepository;
    private final VinculoParejaRepository vinculoParejaRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_SUFFIX = "@demo.billetin.com";
    private static final int MAX_DEMO_USERS = 30;

    @Transactional
    public Usuario generarUsuarioDemo() {
        long currentDemos = usuarioRepository.countByEmailEndingWith(DEMO_SUFFIX);
        if (currentDemos >= MAX_DEMO_USERS) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Capacidad de demos llena");
        }

        String email = "demo-" + UUID.randomUUID() + DEMO_SUFFIX;
        Usuario usuario = Usuario.builder()
                .nombre("Usuario Demo")
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fechaCreacion(LocalDateTime.now())
                .build();
        
        usuario = usuarioRepository.save(usuario);

        crearDatosPorDefecto(usuario);
        generarTransaccionesAleatorias(usuario);

        return usuario;
    }

    private void crearDatosPorDefecto(Usuario usuario) {
        Cuenta cuentaPrincipal = Cuenta.builder()
                .nombre("Caja de Ahorro")
                .tipo(TipoCuenta.TARJETA_DEBITO)
                .esPrincipal(true)
                .esCuentaAhorro(false)
                .usuario(usuario)
                .build();
        cuentaRepository.save(cuentaPrincipal);

        Map<String, String> gastos = Map.of(
                "Supermercado", "🛒",
                "Factura de Internet", "🌐",
                "Cena Restaurante", "🍽️",
                "Carga de Transporte / Gasolina", "⛽",
                "Suscripción Streaming", "🎬",
                "Gimnasio", "🏋️‍♂️"
        );

        gastos.forEach((nombre, icono) -> categoriaRepository.save(Categoria.builder()
                .nombre(nombre)
                .icono(icono)
                .tipo(TipoTransaccion.GASTO)
                .usuario(usuario)
                .build()));

        Map<String, String> ingresosMap = Map.of(
                "Sueldo Mensual", "💰",
                "Cobro de Honorarios", "💼"
        );

        ingresosMap.forEach((nombre, icono) -> categoriaRepository.save(Categoria.builder()
                .nombre(nombre)
                .icono(icono)
                .tipo(TipoTransaccion.INGRESO)
                .usuario(usuario)
                .build()));
    }

    private void generarTransaccionesAleatorias(Usuario usuario) {
        List<Cuenta> cuentas = cuentaRepository.findByUsuarioId(usuario.getId());
        List<Categoria> categorias = categoriaRepository.findByUsuarioId(usuario.getId());
        
        List<Categoria> categoriasGasto = categorias.stream().filter(c -> c.getTipo() == TipoTransaccion.GASTO).toList();
        List<Categoria> categoriasIngreso = categorias.stream().filter(c -> c.getTipo() == TipoTransaccion.INGRESO).toList();
        
        Random random = new Random();
        LocalDate hoy = LocalDate.now();
        Cuenta cuentaPrincipal = cuentas.get(0);

        // Generar 1 o 2 Ingresos (Sueldo / Honorarios)
        int cantidadIngresos = 1 + random.nextInt(2);
        for (int i = 0; i < cantidadIngresos; i++) {
            Categoria catIngreso = categoriasIngreso.get(random.nextInt(categoriasIngreso.size()));
            double montoIngreso = 400000 + random.nextDouble() * 200000; // Entre 400.000 y 600.000
            
            // Ingresos generalmente a principio de mes (ej. día 1 al 5)
            LocalDate fechaIngreso = hoy.minusDays(20 + random.nextInt(10)); 

            Transaccion ingreso = Transaccion.builder()
                    .monto(BigDecimal.valueOf(montoIngreso))
                    .montoEstadistico(BigDecimal.valueOf(montoIngreso))
                    .descripcion(catIngreso.getNombre())
                    .tipo(TipoTransaccion.INGRESO)
                    .fechaCompra(fechaIngreso)
                    .fechaImputacion(fechaIngreso)
                    .cuenta(cuentaPrincipal)
                    .categoria(catIngreso)
                    .esCompartido(false)
                    .esGastoFijo(false)
                    .build();
            transaccionRepository.save(ingreso);
        }

        // Generar 8 a 12 Gastos escalonados
        int cantidadGastos = 8 + random.nextInt(5);
        for (int i = 0; i < cantidadGastos; i++) {
            Categoria catGasto = categoriasGasto.get(random.nextInt(categoriasGasto.size()));
            
            // Montos de gastos menores para asegurar saldo positivo (ej. 5.000 a 30.000)
            double montoGasto = 5000 + random.nextDouble() * 25000; 
            
            // Fechas escalonadas a lo largo de los últimos 25 días
            LocalDate fechaGasto = hoy.minusDays(random.nextInt(25));

            Transaccion gasto = Transaccion.builder()
                    .monto(BigDecimal.valueOf(montoGasto))
                    .montoEstadistico(BigDecimal.valueOf(montoGasto))
                    .descripcion(catGasto.getNombre())
                    .tipo(TipoTransaccion.GASTO)
                    .fechaCompra(fechaGasto)
                    .fechaImputacion(fechaGasto)
                    .cuenta(cuentaPrincipal)
                    .categoria(catGasto)
                    .esCompartido(false)
                    .esGastoFijo(false)
                    .build();
            transaccionRepository.save(gasto);
        }
    }

    @Transactional
    public void limpiarUsuariosDemoAntiguos() {
        LocalDateTime haceDosHoras = LocalDateTime.now().minusHours(2);
        List<Usuario> usuariosDemo = usuarioRepository.findByEmailEndingWith(DEMO_SUFFIX);

        for (Usuario usuario : usuariosDemo) {
            // Manejo de nulls para evitar NullPointerException si hay usuarios antiguos sin fecha
            if (usuario.getFechaCreacion() == null || usuario.getFechaCreacion().isBefore(haceDosHoras)) {
                eliminarUsuarioCompleto(usuario.getId());
            }
        }
    }

    private void eliminarUsuarioCompleto(Long usuarioId) {
        // Eliminar vinculos
        vinculoParejaRepository.deleteByUsuario1IdOrUsuario2Id(usuarioId);
        
        // Eliminar presupuestos de sus categorías
        List<Categoria> categorias = categoriaRepository.findByUsuarioId(usuarioId);
        List<Long> categoriaIds = categorias.stream().map(Categoria::getId).toList();
        if (!categoriaIds.isEmpty()) {
            presupuestoRepository.deleteByCategoriaIdIn(categoriaIds);
        }
        
        // Eliminar transacciones de sus cuentas
        List<Cuenta> cuentas = cuentaRepository.findByUsuarioId(usuarioId);
        for (Cuenta cuenta : cuentas) {
            transaccionRepository.deleteByCuentaId(cuenta.getId());
        }
        
        // Eliminar gastos fijos
        gastoFijoRepository.deleteByUsuarioId(usuarioId);
        
        // Eliminar cuentas
        cuentaRepository.deleteByUsuarioId(usuarioId);
        
        // Eliminar categorías
        categoriaRepository.deleteByUsuarioId(usuarioId);
        
        // Finalmente eliminar el usuario
        usuarioRepository.deleteById(usuarioId);
    }
}
