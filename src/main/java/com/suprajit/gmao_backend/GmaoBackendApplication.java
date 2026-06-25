package com.suprajit.gmao_backend;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.suprajit.gmao_backend.entity.Role;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.repository.RoleRepository;
import com.suprajit.gmao_backend.repository.UserRepository;

@SpringBootApplication
public class GmaoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmaoBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            // Initialiser les rôles
            List<String> roles = List.of("Admin", "Supervisor", "Technician");
            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    roleRepository.save(Role.builder().name(roleName).build());
                    System.out.println("[INIT] Rôle créé : " + roleName);
                }
            }

            // Insérer un admin de test si aucun utilisateur en base
            if (userRepository.count() == 0) {
                Role adminRole = roleRepository.findByName("Admin").orElseThrow();
                userRepository.save(User.builder()
                        .fullName("Admin Suprajit")
                        .email("admin@suprajit.ma")
                        .password(passwordEncoder.encode("admin123"))
                        .role(adminRole)
                        .availabilityStatus("Available")
                        .build());
                System.out.println("[INIT] Utilisateur admin créé : admin@suprajit.ma / admin123");
            }
        };
    }
}
//Le if isEmpty() est important car il évite de créer des doublons à chaque redémarrage.