package com.suprajit.gmao_backend;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Role;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
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
                Role adminRole       = roleRepository.findByName("Admin").orElseThrow();
                Role supervisorRole  = roleRepository.findByName("Supervisor").orElseThrow();
                Role technicianRole  = roleRepository.findByName("Technician").orElseThrow();

                userRepository.save(User.builder()
                        .employeeCode("1001")
                        .fullName("Admin Suprajit")
                        .email("admin@suprajit.ma")
                        .password(passwordEncoder.encode("admin123"))
                        .role(adminRole)
                        .availabilityStatus("Available")
                        .build());

                userRepository.save(User.builder()
                        .employeeCode("1247")
                        .fullName("Supervisor Suprajit")
                        .email("supervisor@suprajit.ma")
                        .password(passwordEncoder.encode("supervisor123"))
                        .role(supervisorRole)
                        .availabilityStatus("Available")
                        .build());

                userRepository.save(User.builder()
                        .employeeCode("317")
                        .fullName("Technicien Suprajit")
                        .email("technician@suprajit.ma")
                        .password(passwordEncoder.encode("tech123"))
                        .role(technicianRole)
                        .speciality("Électromécanique")
                        .availabilityStatus("Available")
                        .build());

                System.out.println("[INIT] 3 utilisateurs de test créés");
            }
        };
    }
    @Bean
    CommandLineRunner initEquipments(EquipmentRepository equipmentRepository) {
        return args -> {
            if (equipmentRepository.count() == 0) {

                equipmentRepository.save(Equipment.builder()
                        .code("EXT-0006")
                        .name("Extrusion Machine")
                        .description("Ligne d'extrusion pour câbles automobiles")
                        .serialNumber("EXT-0006-SN")
                        .manufacturer("Maillefer")
                        .model("MX120")
                        .type("Extrusion")
                        .category("Production")
                        .plant("Suprajit Morocco")
                        .productionLine("Line A")
                        .location("Workshop A")
                        .installationDate(java.time.LocalDate.of(2025, 1, 10))
                        .commissioningDate(java.time.LocalDate.of(2025, 1, 15))
                        .status(EquipmentStatus.Active)
                        .criticalityLevel(CriticalityLevel.High)
                        .maintenanceTeam("Mechanical Team")
                        .notes("Quarterly maintenance required")
                        .build());

                equipmentRepository.save(Equipment.builder()
                        .code("WIN-0038")
                        .name("Winding Machine")
                        .description("Machine d'enroulement de câbles")
                        .serialNumber("WIN-0038-SN")
                        .manufacturer("Marsilli")
                        .model("WD300")
                        .type("Winding")
                        .category("Production")
                        .plant("Suprajit Morocco")
                        .productionLine("Line B")
                        .location("Workshop B")
                        .installationDate(java.time.LocalDate.of(2024, 6, 1))
                        .commissioningDate(java.time.LocalDate.of(2024, 6, 15))
                        .status(EquipmentStatus.Active)
                        .criticalityLevel(CriticalityLevel.High)
                        .maintenanceTeam("Electrical Team")
                        .notes("Critical for BMW line production")
                        .build());

                equipmentRepository.save(Equipment.builder()
                        .code("MO-0015")
                        .name("Molding Machine ARBURG 470S")
                        .description("Machine de moulage par injection")
                        .serialNumber("MO-0015-SN")
                        .manufacturer("ARBURG")
                        .model("470S")
                        .type("Molding")
                        .category("Production")
                        .plant("Suprajit Morocco")
                        .productionLine("Line C")
                        .location("Workshop C")
                        .installationDate(java.time.LocalDate.of(2023, 3, 20))
                        .commissioningDate(java.time.LocalDate.of(2023, 4, 1))
                        .status(EquipmentStatus.Active)
                        .criticalityLevel(CriticalityLevel.Medium)
                        .maintenanceTeam("Mechanical Team")
                        .notes("Periodic lubrication every 3 months")
                        .build());

                System.out.println("[INIT] 3 équipements de test créés");
            }
        };
}
}
//Le if isEmpty() est important car il évite de créer des doublons à chaque redémarrage.