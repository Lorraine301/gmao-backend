package com.suprajit.gmao_backend;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.suprajit.gmao_backend.entity.Role;
import com.suprajit.gmao_backend.repository.RoleRepository;

@SpringBootApplication
public class GmaoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmaoBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            List<String> roles = List.of("Admin", "Supervisor", "Technician");
            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    roleRepository.save(Role.builder().name(roleName).build());
                    System.out.println("[INIT] Rôle créé : " + roleName);
                }
            }
        };
    }
}
//Le if isEmpty() est important car il évite de créer des doublons à chaque redémarrage.