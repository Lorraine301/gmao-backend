package com.suprajit.gmao_backend.equipment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;
import com.suprajit.gmao_backend.equipment.dto.EquipmentImportResultDTO;
import com.suprajit.gmao_backend.equipment.dto.EquipmentRequestDTO;
import com.suprajit.gmao_backend.equipment.dto.EquipmentResponseDTO;
import com.suprajit.gmao_backend.repository.EquipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EquipmentImportExportService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentService equipmentService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // gère LocalDate correctement en JSON

    // ── Colonnes communes export/import (dans cet ordre précis) ──
    private static final String[] COLUMNS = {
            "code", "name", "description", "serialNumber", "manufacturer", "model",
            "type", "category", "plant", "productionLine", "location",
            "installationDate", "commissioningDate", "status", "criticalityLevel",
            "maintenanceTeam", "notes"
    };

    // ══════════════════════════════════════════════════════════
    // EXPORT
    // ══════════════════════════════════════════════════════════

    public byte[] exportToExcel() throws IOException {
        List<EquipmentResponseDTO> equipments = equipmentService.findAll(null, null, null, null);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Équipements");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMNS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (EquipmentResponseDTO eq : equipments) {
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, eq.getCode());
                setCell(row, 1, eq.getName());
                setCell(row, 2, eq.getDescription());
                setCell(row, 3, eq.getSerialNumber());
                setCell(row, 4, eq.getManufacturer());
                setCell(row, 5, eq.getModel());
                setCell(row, 6, eq.getType());
                setCell(row, 7, eq.getCategory());
                setCell(row, 8, eq.getPlant());
                setCell(row, 9, eq.getProductionLine());
                setCell(row, 10, eq.getLocation());
                setCell(row, 11, eq.getInstallationDate() != null ? eq.getInstallationDate().toString() : "");
                setCell(row, 12, eq.getCommissioningDate() != null ? eq.getCommissioningDate().toString() : "");
                setCell(row, 13, eq.getStatus() != null ? eq.getStatus().name() : "");
                setCell(row, 14, eq.getCriticalityLevel() != null ? eq.getCriticalityLevel().name() : "");
                setCell(row, 15, eq.getMaintenanceTeam());
                setCell(row, 16, eq.getNotes());
            }

            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private void setCell(Row row, int idx, String value) {
        row.createCell(idx).setCellValue(value != null ? value : "");
    }

    public byte[] exportToJson() throws IOException {
        List<EquipmentResponseDTO> equipments = equipmentService.findAll(null, null, null, null);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(equipments);
    }

    // ══════════════════════════════════════════════════════════
    // IMPORT
    // ══════════════════════════════════════════════════════════

    @Transactional
    public EquipmentImportResultDTO importFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Nom de fichier manquant");
        }

        if (filename.toLowerCase().endsWith(".xlsx")) {
            return importFromExcel(file.getInputStream());
        } else if (filename.toLowerCase().endsWith(".json")) {
            return importFromJson(file.getInputStream());
        } else {
            throw new IllegalArgumentException(
                    "Format de fichier non supporté. Utilisez .xlsx ou .json");
        }
    }

    private EquipmentImportResultDTO importFromExcel(InputStream inputStream) throws IOException {
        int created = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row)) continue;

                int excelRowNumber = rowIdx + 1; // pour un message d'erreur lisible (1-indexé Excel)

                try {
                    String code = getCellString(row, 0);
                    String name = getCellString(row, 1);

                    if (code == null || code.isBlank() || name == null || name.isBlank()) {
                        errors.add("Ligne " + excelRowNumber + " : code ou nom manquant, ignorée");
                        skipped++;
                        continue;
                    }

                    if (equipmentRepository.existsByCode(code)) {
                        errors.add("Ligne " + excelRowNumber + " : code '" + code + "' déjà existant, ignorée");
                        skipped++;
                        continue;
                    }

                    EquipmentRequestDTO dto = new EquipmentRequestDTO();
                    dto.setCode(code);
                    dto.setName(name);
                    dto.setDescription(getCellString(row, 2));
                    dto.setSerialNumber(getCellString(row, 3));
                    dto.setManufacturer(getCellString(row, 4));
                    dto.setModel(getCellString(row, 5));
                    dto.setType(getCellString(row, 6));
                    dto.setCategory(getCellString(row, 7));
                    dto.setPlant(getCellString(row, 8));
                    dto.setProductionLine(getCellString(row, 9));
                    dto.setLocation(getCellString(row, 10));
                    dto.setInstallationDate(parseDate(getCellString(row, 11)));
                    dto.setCommissioningDate(parseDate(getCellString(row, 12)));
                    dto.setMaintenanceTeam(getCellString(row, 15));
                    dto.setNotes(getCellString(row, 16));

                    String statusStr = getCellString(row, 13);
                    dto.setStatus(statusStr != null && !statusStr.isBlank()
                            ? EquipmentStatus.valueOf(statusStr.trim()) : EquipmentStatus.Active);

                    String criticalityStr = getCellString(row, 14);
                    dto.setCriticalityLevel(criticalityStr != null && !criticalityStr.isBlank()
                            ? CriticalityLevel.valueOf(criticalityStr.trim()) : CriticalityLevel.Medium);

                    Equipment entity = Equipment.builder()
                            .code(dto.getCode()).name(dto.getName()).description(dto.getDescription())
                            .serialNumber(dto.getSerialNumber()).manufacturer(dto.getManufacturer())
                            .model(dto.getModel()).type(dto.getType()).category(dto.getCategory())
                            .plant(dto.getPlant()).productionLine(dto.getProductionLine())
                            .location(dto.getLocation()).installationDate(dto.getInstallationDate())
                            .commissioningDate(dto.getCommissioningDate()).status(dto.getStatus())
                            .criticalityLevel(dto.getCriticalityLevel())
                            .maintenanceTeam(dto.getMaintenanceTeam()).notes(dto.getNotes())
                            .build();

                    equipmentRepository.save(entity);
                    created++;

                } catch (IllegalArgumentException e) {
                    errors.add("Ligne " + excelRowNumber + " : " + e.getMessage());
                    skipped++;
                } catch (Exception e) {
                    errors.add("Ligne " + excelRowNumber + " : erreur inattendue - " + e.getMessage());
                    skipped++;
                }
            }
        }

        return EquipmentImportResultDTO.builder()
                .createdCount(created).skippedCount(skipped).errors(errors).build();
    }

    private EquipmentImportResultDTO importFromJson(InputStream inputStream) throws IOException {
        int created = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        List<EquipmentRequestDTO> items = objectMapper.readValue(inputStream,
                objectMapper.getTypeFactory().constructCollectionType(List.class, EquipmentRequestDTO.class));

        int index = 0;
        for (EquipmentRequestDTO dto : items) {
            index++;
            try {
                if (dto.getCode() == null || dto.getCode().isBlank()
                        || dto.getName() == null || dto.getName().isBlank()) {
                    errors.add("Élément " + index + " : code ou nom manquant, ignoré");
                    skipped++;
                    continue;
                }
                if (equipmentRepository.existsByCode(dto.getCode())) {
                    errors.add("Élément " + index + " : code '" + dto.getCode() + "' déjà existant, ignoré");
                    skipped++;
                    continue;
                }

                if (dto.getStatus() == null) dto.setStatus(EquipmentStatus.Active);
                if (dto.getCriticalityLevel() == null) dto.setCriticalityLevel(CriticalityLevel.Medium);

                Equipment entity = Equipment.builder()
                        .code(dto.getCode()).name(dto.getName()).description(dto.getDescription())
                        .serialNumber(dto.getSerialNumber()).manufacturer(dto.getManufacturer())
                        .model(dto.getModel()).type(dto.getType()).category(dto.getCategory())
                        .plant(dto.getPlant()).productionLine(dto.getProductionLine())
                        .location(dto.getLocation()).installationDate(dto.getInstallationDate())
                        .commissioningDate(dto.getCommissioningDate()).status(dto.getStatus())
                        .criticalityLevel(dto.getCriticalityLevel())
                        .maintenanceTeam(dto.getMaintenanceTeam()).notes(dto.getNotes())
                        .build();

                equipmentRepository.save(entity);
                created++;

            } catch (Exception e) {
                errors.add("Élément " + index + " : " + e.getMessage());
                skipped++;
            }
        }

        return EquipmentImportResultDTO.builder()
                .createdCount(created).skippedCount(skipped).errors(errors).build();
    }

    // ── Helpers ──────────────────────────────────────────────
    private String getCellString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < COLUMNS.length; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}