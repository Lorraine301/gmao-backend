package com.suprajit.gmao_backend.equipment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    // NOTE : location/productionLine/maintenanceTeam retirés,
    // area ajouté (Zone → Area → Adresse usine)
    private static final String[] COLUMNS = {
            "code", "name", "description", "serialNumber", "manufacturer", "model",
            "type", "category", "area", "plant",
            "installationDate", "commissioningDate", "status", "criticalityLevel",
            "notes"
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
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
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
                setCell(row, 8, eq.getArea());
                setCell(row, 9, eq.getPlant());
                setCell(row, 10, eq.getInstallationDate() != null ? eq.getInstallationDate().toString() : "");
                setCell(row, 11, eq.getCommissioningDate() != null ? eq.getCommissioningDate().toString() : "");
                setCell(row, 12, eq.getStatus() != null ? eq.getStatus().name() : "");
                setCell(row, 13, eq.getCriticalityLevel() != null ? eq.getCriticalityLevel().name() : "");
                setCell(row, 14, eq.getNotes());
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

            // ── Lit la ligne d'en-tête et associe chaque nom de colonne à son index ──
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Le fichier est vide ou ne contient pas d'en-tête.");
            }

            Map<String, Integer> columnIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                String headerName = cell.getStringCellValue().trim();
                columnIndex.put(headerName, cell.getColumnIndex());
            }

            // ── Vérifie que les colonnes obligatoires sont bien présentes ──
            if (!columnIndex.containsKey("code") || !columnIndex.containsKey("name")) {
                throw new IllegalArgumentException(
                        "Le fichier doit contenir au minimum les colonnes 'code' et 'name'.");
            }

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row, columnIndex)) continue;

                int excelRowNumber = rowIdx + 1;

                try {
                    String code = getCellString(row, columnIndex, "code");
                    String name = getCellString(row, columnIndex, "name");

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
                    dto.setDescription(getCellString(row, columnIndex, "description"));
                    dto.setSerialNumber(getCellString(row, columnIndex, "serialNumber"));
                    dto.setManufacturer(getCellString(row, columnIndex, "manufacturer"));
                    dto.setModel(getCellString(row, columnIndex, "model"));
                    dto.setType(getCellString(row, columnIndex, "type"));
                    dto.setCategory(getCellString(row, columnIndex, "category"));
                    dto.setArea(getCellString(row, columnIndex, "area"));
                    dto.setPlant(getCellString(row, columnIndex, "plant"));
                    dto.setInstallationDate(parseDate(getCellString(row, columnIndex, "installationDate")));
                    dto.setCommissioningDate(parseDate(getCellString(row, columnIndex, "commissioningDate")));
                    dto.setNotes(getCellString(row, columnIndex, "notes"));

                    String statusStr = getCellString(row, columnIndex, "status");
                    dto.setStatus(statusStr != null && !statusStr.isBlank()
                            ? EquipmentStatus.valueOf(statusStr.trim()) : EquipmentStatus.Active);

                    String criticalityStr = getCellString(row, columnIndex, "criticalityLevel");
                    dto.setCriticalityLevel(criticalityStr != null && !criticalityStr.isBlank()
                            ? CriticalityLevel.valueOf(criticalityStr.trim()) : CriticalityLevel.Medium);

                    Equipment entity = Equipment.builder()
                            .code(dto.getCode()).name(dto.getName()).description(dto.getDescription())
                            .serialNumber(dto.getSerialNumber()).manufacturer(dto.getManufacturer())
                            .model(dto.getModel()).type(dto.getType()).category(dto.getCategory())
                            .area(dto.getArea()).plant(dto.getPlant())
                            .installationDate(dto.getInstallationDate())
                            .commissioningDate(dto.getCommissioningDate()).status(dto.getStatus())
                            .criticalityLevel(dto.getCriticalityLevel())
                            .notes(dto.getNotes())
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

    // ── Récupère la valeur d'une cellule par NOM de colonne (pas par position) ──
    private String getCellString(Row row, Map<String, Integer> columnIndex, String columnName) {
        Integer idx = columnIndex.get(columnName);
        if (idx == null) return null; // colonne absente du fichier → traité comme vide

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

    private boolean isRowEmpty(Row row, Map<String, Integer> columnIndex) {
        for (Integer idx : columnIndex.values()) {
            Cell cell = row.getCell(idx);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
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
                        .area(dto.getArea()).plant(dto.getPlant())
                        .installationDate(dto.getInstallationDate())
                        .commissioningDate(dto.getCommissioningDate()).status(dto.getStatus())
                        .criticalityLevel(dto.getCriticalityLevel())
                        .notes(dto.getNotes())
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
  
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}