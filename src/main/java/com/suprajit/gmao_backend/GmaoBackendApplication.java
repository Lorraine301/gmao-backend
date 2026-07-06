package com.suprajit.gmao_backend;

import java.time.LocalDate;
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
import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.Role;
import com.suprajit.gmao_backend.entity.SparePart;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;
import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceRepository;
import com.suprajit.gmao_backend.repository.RoleRepository;
import com.suprajit.gmao_backend.repository.SparePartRepository;
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
        List<String> roles = List.of("Admin", "Supervisor", "Technician");
        for (String roleName : roles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                System.out.println("[INIT] Rôle créé : " + roleName);
            }
        }

        if (userRepository.count() == 0) {
            Role adminRole      = roleRepository.findByName("Admin").orElseThrow();
            Role supervisorRole = roleRepository.findByName("Supervisor").orElseThrow();
            Role technicianRole = roleRepository.findByName("Technician").orElseThrow();

            userRepository.save(User.builder()
                    .employeeCode("1001").fullName("Admin Suprajit")
                    .email("admin@suprajit.ma").password(passwordEncoder.encode("admin123"))
                    .role(adminRole).availabilityStatus("Available").build());

            userRepository.save(User.builder()
                    .employeeCode("1247").fullName("Supervisor Suprajit")
                    .email("supervisor@suprajit.ma").password(passwordEncoder.encode("supervisor123"))
                    .role(supervisorRole).availabilityStatus("Available").build());

            userRepository.save(User.builder()
                    .employeeCode("317").fullName("Technicien Suprajit")
                    .email("technician@suprajit.ma").password(passwordEncoder.encode("tech123"))
                    .role(technicianRole).speciality("Électromécanique")
                    .availabilityStatus("Available").build());

            System.out.println("[INIT] 3 utilisateurs de base créés");
        }

        // ── Techniciens supplémentaires (insérés si absents par email) ──
        Role technicianRole = roleRepository.findByName("Technician").orElseThrow();

        if (userRepository.findByEmail("m.alami@suprajit.ma").isEmpty()) {
            userRepository.save(User.builder()
                    .employeeCode("422").fullName("Mohamed Alami")
                    .email("m.alami@suprajit.ma").password(passwordEncoder.encode("tech123"))
                    .role(technicianRole).speciality("Mécanique")
                    .availabilityStatus("Available").build());
            System.out.println("[INIT] Technicien ajouté : Mohamed Alami");
        }

        if (userRepository.findByEmail("y.benali@suprajit.ma").isEmpty()) {
            userRepository.save(User.builder()
                    .employeeCode("588").fullName("Youssef Benali")
                    .email("y.benali@suprajit.ma").password(passwordEncoder.encode("tech123"))
                    .role(technicianRole).speciality("Électrique")
                    .availabilityStatus("Available").build());
            System.out.println("[INIT] Technicien ajouté : Youssef Benali");
        }

        if (userRepository.findByEmail("f.mansouri@suprajit.ma").isEmpty()) {
            userRepository.save(User.builder()
                    .employeeCode("731").fullName("Fatima Zahra Mansouri")
                    .email("f.mansouri@suprajit.ma").password(passwordEncoder.encode("tech123"))
                    .role(technicianRole).speciality("Électromécanique")
                    .availabilityStatus("Available").build());
            System.out.println("[INIT] Technicien ajouté : Fatima Zahra Mansouri");
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

// Préventive Maintenance et autres initialisations peuvent être ajoutées ici si nécessaire
@Bean
CommandLineRunner initMaintenancesAndStock(
        PreventiveMaintenanceRepository pmRepository,
        SparePartRepository sparePartRepository,
        EquipmentRepository equipmentRepository) {
    return args -> {

        // ── Pièces de rechange ────────────────────────────────────
        if (sparePartRepository.count() == 0) {

            // Pièce 1 : stock OK
            sparePartRepository.save(SparePart.builder()
                    .name("Cooling Fan Motor")
                    .reference("FAN-CF05")
                    .supplier("Siemens")
                    .warehouseLocation("WH-A1")
                    .quantity(8)
                    .minimumStock(3)
                    .unit("Piece")
                    .unitPrice(new java.math.BigDecimal("250.00"))
                    .build());

            // Pièce 2 : stock OK
            sparePartRepository.save(SparePart.builder()
                    .name("SKF Bearing 6205")
                    .reference("BRG-6205")
                    .supplier("SKF")
                    .warehouseLocation("WH-B2")
                    .quantity(12)
                    .minimumStock(5)
                    .unit("Piece")
                    .unitPrice(new java.math.BigDecimal("45.00"))
                    .build());

            // Pièce 3 : stock OK
            sparePartRepository.save(SparePart.builder()
                    .name("Hydraulic Oil ISO 46")
                    .reference("OIL-HYD46")
                    .supplier("Total Maroc")
                    .warehouseLocation("WH-C1")
                    .quantity(20)
                    .minimumStock(5)
                    .unit("Litre")
                    .unitPrice(new java.math.BigDecimal("35.00"))
                    .build());

            // Pièce 4 : stock SOUS le minimum → déclenchera une alerte
            sparePartRepository.save(SparePart.builder()
                    .name("Drive Belt Type A")
                    .reference("BLT-DRV-A")
                    .supplier("Gates")
                    .warehouseLocation("WH-A3")
                    .quantity(2)
                    .minimumStock(5)   // ← 2 < 5 : alerte stock faible
                    .unit("Piece")
                    .unitPrice(new java.math.BigDecimal("180.00"))
                    .build());

            // Pièce 5 : stock SOUS le minimum
            sparePartRepository.save(SparePart.builder()
                    .name("Seal Kit Extruder")
                    .reference("SEAL-EXT01")
                    .supplier("Parker")
                    .warehouseLocation("WH-B1")
                    .quantity(1)
                    .minimumStock(4)   // ← 1 < 4 : alerte stock faible
                    .unit("Kit")
                    .unitPrice(new java.math.BigDecimal("320.00"))
                    .build());

            System.out.println("[INIT] 5 pièces de rechange créées (dont 2 sous le minimum)");
        }

        // ── Maintenances préventives ──────────────────────────────
        if (pmRepository.count() == 0) {

            Equipment ext0006 = equipmentRepository.findByCode("EXT-0006").orElse(null);
            Equipment win0038 = equipmentRepository.findByCode("WIN-0038").orElse(null);
            Equipment mo0015  = equipmentRepository.findByCode("MO-0015").orElse(null);

            if (ext0006 != null) {
                // Maintenance 1 : planifiée dans le futur
                LocalDate lastPM1 = java.time.LocalDate.now().minusDays(60);
                LocalDate nextPM1 = lastPM1.plusDays(90);
                pmRepository.save(PreventiveMaintenance.builder()
                        .equipment(ext0006)
                        .maintenanceType("PT") // Préventive Totale
                        .frequencyDays(90)
                        .lastMaintenanceDate(lastPM1)
                        .nextMaintenanceDate(nextPM1)
                        .nextReminderDate(nextPM1.minusDays(7))
                        .status(MaintenanceStatus.Scheduled)
                        .build());
            }

            if (win0038 != null) {
                // Maintenance 2 : EN RETARD (next_maintenance_date dépassée)
                LocalDate lastPM2 = java.time.LocalDate.now().minusDays(100);
                LocalDate nextPM2 = lastPM2.plusDays(90); // il y a 10 jours
                pmRepository.save(PreventiveMaintenance.builder()
                        .equipment(win0038)
                        .maintenanceType("PC") // Préventive Partielle
                        .frequencyDays(90)
                        .lastMaintenanceDate(lastPM2)
                        .nextMaintenanceDate(nextPM2) // dans le passé → Overdue
                        .nextReminderDate(nextPM2.minusDays(7))
                        .status(MaintenanceStatus.Overdue)
                        .build());
            }

            if (mo0015 != null) {
                // Maintenance 3 : planifiée, trimestrielle
                LocalDate lastPM3 = java.time.LocalDate.now().minusDays(30);
                LocalDate nextPM3 = lastPM3.plusDays(90);
                pmRepository.save(PreventiveMaintenance.builder()
                        .equipment(mo0015)
                        .maintenanceType("Lubrification")
                        .frequencyDays(90)
                        .lastMaintenanceDate(lastPM3)
                        .nextMaintenanceDate(nextPM3)
                        .nextReminderDate(nextPM3.minusDays(7))
                        .status(MaintenanceStatus.Scheduled)
                        .build());
            }

            System.out.println("[INIT] 3 maintenances préventives créées (dont 1 en retard)");
        }
    };
}
}
//Le if isEmpty() est important car il évite de créer des doublons à chaque redémarrage.