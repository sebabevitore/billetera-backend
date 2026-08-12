package billetera.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoCleanupTask {

    private final DemoService demoService;

    // Se ejecuta cada hora en punto
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupDemos() {
        log.info("Iniciando tarea programada: limpieza de usuarios demo...");
        try {
            demoService.limpiarUsuariosDemoAntiguos();
            log.info("Limpieza de usuarios demo finalizada correctamente.");
        } catch (Exception e) {
            log.error("Error durante la limpieza de usuarios demo: {}", e.getMessage(), e);
        }
    }
}
