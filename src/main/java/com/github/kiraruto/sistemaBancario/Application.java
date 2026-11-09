package com.github.kiraruto.sistemaBancario;

import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class Application implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_EMAIL:admin@exemplo.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(EnumUserRole.ADMIN)) {
            log.info("Usuário ADMIN já existe. Nenhum admin padrão será criado.");
            return;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("Nenhum ADMIN existe e a variável ADMIN_PASSWORD não foi definida.");
            log.warn("Defina ADMIN_PASSWORD (mínimo 12 caracteres) e reinicie a aplicação.");
            return;
        }

        if (adminPassword.length() < 12) {
            log.error("ADMIN_PASSWORD deve ter no mínimo 12 caracteres. Admin não foi criado.");
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setFirstName("Administrador");
        admin.setLastName("Sistema");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(EnumUserRole.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
        log.info("Usuário ADMIN '{}' criado com sucesso.", adminUsername);
    }
}
