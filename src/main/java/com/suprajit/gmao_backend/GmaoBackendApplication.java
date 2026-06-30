package com.suprajit.gmao_backend;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.Role;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
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
@Bean
CommandLineRunner initFailuresAndInterventions(
        FailureRepository failureRepository,
        InterventionRepository interventionRepository,
        EquipmentRepository equipmentRepository,
        UserRepository userRepository) {
    return args -> {
        if (failureRepository.count() == 0) {

            // ── Équipements réels de la base ──────────────────────
            Equipment ext0006 = equipmentRepository.findByCode("EXT-0006").orElseThrow();
            Equipment win0038 = equipmentRepository.findByCode("WIN-0038").orElseThrow();
            Equipment win0011 = equipmentRepository.findByCode("WIN-0011").orElseThrow();
            Equipment rol0012 = equipmentRepository.findByCode("ROL-0012").orElseThrow();
            Equipment ext0009 = equipmentRepository.findByCode("EXT-0009").orElseThrow();

            // ── Utilisateurs réels de la base ──────────────────────
            User technician = userRepository.findByEmail("technician@suprajit.ma").orElseThrow();
            User supervisor = userRepository.findByEmail("supervisor@suprajit.ma").orElseThrow();

            // ── Panne 1 : EXT-0006, Critical, déjà résolue ──────────
            Failure failure1 = failureRepository.save(Failure.builder()
                    .failureCode("FAIL-0001")
                    .equipment(ext0006)
                    .title("Bearing overheating")
                    .description("Motor overheats during extrusion cycle, temperature exceeds threshold")
                    .failureType("Mechanical")
                    .priority(FailurePriority.Critical)
                    .status(FailureStatus.Resolved)
                    .reportedBy(technician)
                    .reportedChannel("Web")
                    .reportedAt(LocalDateTime.now().minusDays(2))
                    .resolvedAt(LocalDateTime.now().minusDays(1))
                    .llmProcessed(false)
                    .build());

            interventionRepository.save(Intervention.builder()
                    .failure(failure1)
                    .technician(technician)
                    .assignedBy(supervisor)
                    .startTime(LocalDateTime.now().minusDays(2).plusHours(1))
                    .endTime(LocalDateTime.now().minusDays(2).plusHours(3).plusMinutes(30))
                    .duration(2.5)
                    .priority(FailurePriority.Critical)
                    .status(InterventionStatus.Completed)
                    .solution("Bearing replaced with new SKF unit, lubrication redone")
                    .closedBy(supervisor)
                    .build());

            // ── Panne 2 : WIN-0011, High, machine déjà Inactive, urgente ──
            Failure failure2 = failureRepository.save(Failure.builder()
                    .failureCode("FAIL-0002")
                    .equipment(win0011)
                    .title("Machine arrêtée - intervention urgente requise")
                    .description("Machine en zone Préparation-Winding nécessite une intervention immédiate selon le rapport de validation QA")
                    .failureType("Mechanical")
                    .priority(FailurePriority.Critical)
                    .status(FailureStatus.In_Progress)
                    .reportedBy(supervisor)
                    .reportedChannel("Web")
                    .reportedAt(LocalDateTime.now().minusHours(5))
                    .llmProcessed(false)
                    .build());

            interventionRepository.save(Intervention.builder()
                    .failure(failure2)
                    .technician(technician)
                    .assignedBy(supervisor)
                    .startTime(LocalDateTime.now().minusHours(3))
                    .priority(FailurePriority.Critical)
                    .status(InterventionStatus.In_Progress)
                    .build());

            // ── Panne 3 : WIN-0038, High, pas encore affectée ──────
            failureRepository.save(Failure.builder()
                    .failureCode("FAIL-0003")
                    .equipment(win0038)
                    .title("Winding tension irregular")
                    .description("Cable winding tension fluctuates causing quality defects, critical for BMW line")
                    .failureType("Mechanical")
                    .priority(FailurePriority.High)
                    .status(FailureStatus.Open)
                    .reportedBy(supervisor)
                    .reportedChannel("Web")
                    .reportedAt(LocalDateTime.now().minusHours(1))
                    .llmProcessed(false)
                    .build());

            // ── Panne 4 : ROL-0012, Medium, machine déjà Under_Maintenance ──
            failureRepository.save(Failure.builder()
                    .failureCode("FAIL-0004")
                    .equipment(rol0012)
                    .title("Bruit anormal pendant le laminage")
                    .description("L'opérateur signale un bruit de grincement pendant le fonctionnement")
                    .failureType("Mechanical")
                    .priority(FailurePriority.Medium)
                    .status(FailureStatus.Open)
                    .reportedBy(technician)
                    .reportedChannel("Mobile")
                    .reportedAt(LocalDateTime.now().minusHours(2))
                    .llmProcessed(false)
                    .build());

            // ── Panne 5 : EXT-0009, machine critique sous maintenance ──
            failureRepository.save(Failure.builder()
                    .failureCode("FAIL-0005")
                    .equipment(ext0009)
                    .title("Vitesse d'extrusion instable")
                    .description("Machine critique d'extrusion haute vitesse présente des fluctuations de débit")
                    .failureType("Mechanical")
                    .priority(FailurePriority.High)
                    .status(FailureStatus.Open)
                    .reportedBy(supervisor)
                    .reportedChannel("Web")
                    .reportedAt(LocalDateTime.now().minusMinutes(30))
                    .llmProcessed(false)
                    .build());

            System.out.println("[INIT] 5 pannes et 2 interventions créées avec données réelles Suprajit");
        }
    };
}
}
//Le if isEmpty() est important car il évite de créer des doublons à chaque redémarrage.