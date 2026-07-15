package com.suprajit.gmao_backend.pdf.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.WeeklyReport;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.WeeklyReportRepository;
import com.suprajit.gmao_backend.ruleengine.dto.AtRiskEquipmentDTO;
import com.suprajit.gmao_backend.ruleengine.service.RuleEngineService;
import com.suprajit.gmao_backend.sparepart.dto.InterventionPartResponseDTO;
import com.suprajit.gmao_backend.sparepart.service.SparePartService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PdfService {

    private final InterventionRepository interventionRepository;
    private final SparePartService sparePartService;
    private final WeeklyReportRepository weeklyReportRepository;
    private final FailureRepository failureRepository;
    private final RuleEngineService ruleEngineService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String REPORTS_DIR = "reports/interventions";

    // ── Génère (ou régénère) le PDF et le sauvegarde sur disque ──
    public String generateInterventionReport(Long interventionId) throws IOException {
        Intervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Intervention non trouvée avec l'id : " + interventionId));

        Failure failure = intervention.getFailure();
        List<InterventionPartResponseDTO> parts = sparePartService.findPartsByIntervention(interventionId);

        Path dirPath = Paths.get(REPORTS_DIR);
        Files.createDirectories(dirPath);
        String filePath = REPORTS_DIR + "/intervention_" + interventionId + ".pdf";

        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            buildHeader(document);
            buildEquipmentSection(document, failure);
            buildFailureSection(document, failure);
            buildInterventionSection(document, intervention);
            buildSolutionSection(document, intervention);
            buildPartsSection(document, parts);
            buildFooter(document, pdfDoc);
        }

        return filePath;
    }

    // ── Retourne les bytes du PDF (génère si absent) ──
    public byte[] getOrGenerateReportBytes(Long interventionId) throws IOException {
        String filePath = REPORTS_DIR + "/intervention_" + interventionId + ".pdf";
        File file = new File(filePath);

        if (!file.exists()) {
            generateInterventionReport(interventionId);
        }

        return Files.readAllBytes(Paths.get(filePath));
    }

    // ── Force la régénération et retourne les bytes ──
    public byte[] regenerateReportBytes(Long interventionId) throws IOException {
        String filePath = generateInterventionReport(interventionId);
        return Files.readAllBytes(Paths.get(filePath));
    }

    // ══════════════════════════════════════════════════════════
    // Sections du document — Rapport d'intervention
    // ══════════════════════════════════════════════════════════

    private void buildHeader(Document document) {
        buildBrandedHeader(document,
                "GMAO Intelligente — Suprajit Morocco",
                "Rapport d'intervention",
                "Généré le : " + java.time.LocalDateTime.now().format(DATE_FORMAT));
    }

    // ── En-tête générique avec logo, réutilisé par les 3 types de rapports ──
    private void buildBrandedHeader(Document document, String title, String subtitle, String dateLine) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{18, 82}))
                .useAllAvailableWidth()
                .setMarginBottom(4);

        Cell logoCell = new Cell().setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        Image logo = loadLogo();
        if (logo != null) {
            logo.setWidth(45).setAutoScaleHeight(true);
            logoCell.add(logo);
        }
        headerTable.addCell(logoCell);

        Cell textCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingLeft(12)
                .setPaddingTop(0)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

        textCell.add(new Paragraph(title)
                .setFontSize(16).setBold()
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(30, 58, 95))
                .setMarginBottom(2));

        if (subtitle != null) {
            textCell.add(new Paragraph(subtitle)
                    .setFontSize(11).setFontColor(ColorConstants.GRAY)
                    .setMarginTop(0).setMarginBottom(2));
        }

        textCell.add(new Paragraph(dateLine)
                .setFontSize(9).setFontColor(ColorConstants.GRAY)
                .setMarginTop(0));

        headerTable.addCell(textCell);
        document.add(headerTable);

        LineSeparator separator = new LineSeparator(new SolidLine(1f));
        separator.setMarginTop(10).setMarginBottom(12);
        document.add(separator);
    }

    // ── Charge le logo depuis les ressources du backend (silencieux si absent) ──
    private Image loadLogo() {
        try (InputStream is = new ClassPathResource("static/images/logo_suprajit.png").getInputStream()) {
            byte[] logoBytes = is.readAllBytes();
            ImageData imageData = ImageDataFactory.create(logoBytes);
            return new Image(imageData);
        } catch (IOException e) {
            System.out.println("[PDF] Logo introuvable, génération du rapport sans logo : " + e.getMessage());
            return null;
        }
    }

    private void buildEquipmentSection(Document document, Failure failure) {
        addSectionTitle(document, "Équipement concerné");

        Table table = createInfoTable();
        addRow(table, "Code", failure.getEquipment().getCode());
        addRow(table, "Nom", failure.getEquipment().getName());
        addRow(table, "Type", failure.getEquipment().getType() != null ? failure.getEquipment().getType() : "—");
        addRow(table, "Localisation", failure.getEquipment().getLocation() != null ? failure.getEquipment().getLocation() : "—");
        document.add(table);
    }

    private void buildFailureSection(Document document, Failure failure) {
        addSectionTitle(document, "Panne associée");

        Table table = createInfoTable();
        addRow(table, "Code panne", failure.getFailureCode());
        addRow(table, "Titre", failure.getTitle());
        addRow(table, "Type", failure.getFailureType() != null ? failure.getFailureType() : "—");
        addRow(table, "Priorité (Rule Engine)", failure.getPriority() != null ? failure.getPriority().name() : "—");
        addRow(table, "Priorité (IA)", failure.getLlmPriority() != null ? failure.getLlmPriority().name() : "Non analysée");
        document.add(table);

        document.add(new Paragraph("Description").setBold().setFontSize(10).setMarginTop(6));
        document.add(new Paragraph(failure.getDescription() != null ? failure.getDescription() : "—")
                .setFontSize(10).setMarginBottom(10));
    }

    private void buildInterventionSection(Document document, Intervention intervention) {
        addSectionTitle(document, "Intervention");

        Table table = createInfoTable();
        addRow(table, "Technicien", intervention.getTechnician().getFullName()
                + " (" + intervention.getTechnician().getEmployeeCode() + ")");
        addRow(table, "Date début", intervention.getStartTime() != null
                ? intervention.getStartTime().format(DATE_FORMAT) : "—");
        addRow(table, "Date fin", intervention.getEndTime() != null
                ? intervention.getEndTime().format(DATE_FORMAT) : "—");
        addRow(table, "Durée", intervention.getDuration() != null
                ? intervention.getDuration() + " h" : "—");
        addRow(table, "Statut", intervention.getStatus() != null ? intervention.getStatus().name() : "—");
        document.add(table);
    }

    private void buildSolutionSection(Document document, Intervention intervention) {
        addSectionTitle(document, "Solution apportée");
        document.add(new Paragraph(
                intervention.getSolution() != null && !intervention.getSolution().isBlank()
                        ? intervention.getSolution() : "Aucune solution renseignée")
                .setFontSize(10).setMarginBottom(10));
    }

    private void buildPartsSection(Document document, List<InterventionPartResponseDTO> parts) {
        addSectionTitle(document, "Pièces utilisées");

        if (parts.isEmpty()) {
            document.add(new Paragraph("Aucune pièce consommée pour cette intervention.")
                    .setFontSize(10).setItalic().setFontColor(ColorConstants.GRAY));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 50, 20}))
                .useAllAvailableWidth()
                .setMarginBottom(10);

        table.addHeaderCell(headerCell("Référence"));
        table.addHeaderCell(headerCell("Nom"));
        table.addHeaderCell(headerCell("Quantité"));

        for (InterventionPartResponseDTO part : parts) {
            table.addCell(bodyCell(part.getSparePartReference()));
            table.addCell(bodyCell(part.getSparePartName()));
            table.addCell(bodyCell(String.valueOf(part.getQuantityUsed())));
        }

        document.add(table);
    }

    private void buildFooter(Document document, PdfDocument pdfDoc) {
        int pageCount = pdfDoc.getNumberOfPages();
        for (int i = 1; i <= pageCount; i++) {
            document.showTextAligned(
                    new Paragraph("Page " + i + " / " + pageCount + "   —   Signature : ______________________")
                            .setFontSize(8).setFontColor(ColorConstants.GRAY),
                    297.5f, 20, i, TextAlignment.CENTER,
                    com.itextpdf.layout.properties.VerticalAlignment.BOTTOM, 0
            );
        }
    }

    // ══════════════════════════════════════════════════════════
    // Helpers de mise en forme
    // ══════════════════════════════════════════════════════════

    private void addSectionTitle(Document document, String title) {
        document.add(new Paragraph(title)
                .setBold()
                .setFontSize(12)
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(30, 58, 95))
                .setMarginTop(10)
                .setMarginBottom(6));
    }

    private Table createInfoTable() {
        return new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth()
                .setMarginBottom(6);
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(9))
                .setBorder(Border.NO_BORDER).setPadding(3));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "—").setFontSize(9))
                .setBorder(Border.NO_BORDER).setPadding(3));
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(30, 58, 95))
                .setPadding(5);
    }

    private Cell bodyCell(String text) {
        return new Cell().add(new Paragraph(text != null ? text : "—").setFontSize(9)).setPadding(5);
    }

    // ══════════════════════════════════════════════════════════
    // Bilan hebdomadaire
    // ══════════════════════════════════════════════════════════

    private static final String WEEKLY_REPORTS_DIR = "reports/weekly";

    // ── Génère le PDF du bilan hebdomadaire et le sauvegarde sur disque ──
    public String generateWeeklyReportPdf(WeeklyReport report) throws IOException {
        Path dirPath = Paths.get(WEEKLY_REPORTS_DIR);
        Files.createDirectories(dirPath);

        int year = report.getGeneratedAt().getYear();
        String filePath = WEEKLY_REPORTS_DIR + "/weekly_" + report.getWeekNumber() + "_" + year + ".pdf";

        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            buildWeeklyReportHeader(document, report, year);
            buildWeeklyStatsSection(document, report);
            buildAtRiskSection(document);
            buildAiSynthesisSection(document, report);
            buildRecommendationsSection(document, report);
            buildFooter(document, pdfDoc);
        }

        return filePath;
    }

    // ── Retourne les bytes du bilan hebdomadaire (regénère si le fichier a disparu) ──
    public byte[] getOrGenerateWeeklyReportBytes(Long reportId) throws IOException {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Rapport hebdomadaire non trouvé avec l'id : " + reportId));

        if (report.getPdfPath() != null && new File(report.getPdfPath()).exists()) {
            return Files.readAllBytes(Paths.get(report.getPdfPath()));
        }

        String pdfPath = generateWeeklyReportPdf(report);
        report.setPdfPath(pdfPath);
        weeklyReportRepository.save(report);

        return Files.readAllBytes(Paths.get(pdfPath));
    }

    private void buildWeeklyReportHeader(Document document, WeeklyReport report, int year) {
        buildBrandedHeader(document,
                "Bilan hebdomadaire — Semaine " + report.getWeekNumber() + " — " + year,
                null,
                "Généré le : " + report.getGeneratedAt().format(DATE_FORMAT)
                    + (report.getGeneratedBy() != null ? "  —  par " + report.getGeneratedBy() : ""));
    }

    private void buildWeeklyStatsSection(Document document, WeeklyReport report) {
        addSectionTitle(document, "Statistiques de la semaine");

        Table table = createInfoTable();
        addRow(table, "Total pannes", String.valueOf(report.getTotalFailures()));
        addRow(table, "Pannes résolues", String.valueOf(report.getResolvedFailures()));
        addRow(table, "MTTR moyen", report.getAverageRepairTime() != null
                ? report.getAverageRepairTime() + " h" : "—");
        addRow(table, "Équipements critiques", report.getCriticalMachines() != null
                ? report.getCriticalMachines() : "Aucun");
        document.add(table);
    }

    private void buildAtRiskSection(Document document) {
        addSectionTitle(document, "Équipements à risque");

        List<AtRiskEquipmentDTO> atRisk = ruleEngineService.findAtRiskEquipments();

        if (atRisk.isEmpty()) {
            document.add(new Paragraph("Aucun équipement détecté à risque cette semaine.")
                    .setFontSize(10).setItalic().setFontColor(ColorConstants.GRAY));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{20, 30, 50}))
                .useAllAvailableWidth().setMarginBottom(10);
        table.addHeaderCell(headerCell("Code"));
        table.addHeaderCell(headerCell("Équipement"));
        table.addHeaderCell(headerCell("Raison"));

        for (AtRiskEquipmentDTO eq : atRisk) {
            table.addCell(bodyCell(eq.getEquipmentCode()));
            table.addCell(bodyCell(eq.getEquipmentName()));
            table.addCell(bodyCell(eq.getRiskReason()));
        }
        document.add(table);
    }

    private void buildAiSynthesisSection(Document document, WeeklyReport report) {
        addSectionTitle(document, "Synthèse IA");
        String summary = report.getLlmSummary();
        document.add(new Paragraph(summary != null && !summary.isBlank() ? summary : "En cours d'analyse…")
                .setFontSize(10)
                .setFontColor(summary != null ? ColorConstants.BLACK : ColorConstants.GRAY)
                .setMarginBottom(10));
    }

    private void buildRecommendationsSection(Document document, WeeklyReport report) {
        addSectionTitle(document, "Recommandations");
        String recommendations = report.getRecommendations();
        document.add(new Paragraph(recommendations != null && !recommendations.isBlank()
                ? recommendations : "En cours d'analyse…")
                .setFontSize(10)
                .setFontColor(recommendations != null ? ColorConstants.BLACK : ColorConstants.GRAY));
    }

    // ══════════════════════════════════════════════════════════
    // Bilan mensuel (généré à la volée, non persisté)
    // ══════════════════════════════════════════════════════════

    public byte[] generateMonthlyReportBytes(int month, int year) throws IOException {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Failure> monthFailures = failureRepository.findByReportedAtBetween(start, end);
        List<Intervention> monthInterventions = interventionRepository.findByStartTimeBetween(start, end);

        List<Double> durations = monthInterventions.stream()
                .filter(i -> i.getStatus() == InterventionStatus.Completed)
                .map(Intervention::getDuration)
                .filter(Objects::nonNull)
                .toList();

        Double avgMttr = durations.isEmpty() ? null
                : durations.stream().mapToDouble(Double::doubleValue).average().getAsDouble();

        Map<Long, Long> countByEquipmentId = monthFailures.stream()
                .collect(Collectors.groupingBy(f -> f.getEquipment().getId(), Collectors.counting()));
        Map<Long, Equipment> equipmentById = monthFailures.stream()
                .map(Failure::getEquipment)
                .collect(Collectors.toMap(Equipment::getId, e -> e, (a, b) -> a));

        List<Map.Entry<Long, Long>> top3 = countByEquipmentId.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .toList();

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.FRENCH);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            String capitalizedMonth = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
            buildBrandedHeader(document,
                    "Bilan mensuel — " + capitalizedMonth + " " + year,
                    null,
                    "Généré le : " + LocalDateTime.now().format(DATE_FORMAT));

            addSectionTitle(document, "Statistiques du mois");
            Table statsTable = createInfoTable();
            addRow(statsTable, "Total pannes", String.valueOf(monthFailures.size()));
            addRow(statsTable, "Total interventions", String.valueOf(monthInterventions.size()));
            addRow(statsTable, "MTTR moyen", avgMttr != null
                    ? Math.round(avgMttr * 100.0) / 100.0 + " h" : "—");
            document.add(statsTable);

            addSectionTitle(document, "Top 3 équipements les plus défaillants");
            if (top3.isEmpty()) {
                document.add(new Paragraph("Aucune panne enregistrée ce mois-ci.")
                        .setFontSize(10).setItalic().setFontColor(ColorConstants.GRAY));
            } else {
                Table topTable = new Table(UnitValue.createPercentArray(new float[]{30, 50, 20}))
                        .useAllAvailableWidth().setMarginBottom(10);
                topTable.addHeaderCell(headerCell("Code"));
                topTable.addHeaderCell(headerCell("Équipement"));
                topTable.addHeaderCell(headerCell("Nb pannes"));

                for (Map.Entry<Long, Long> entry : top3) {
                    Equipment eq = equipmentById.get(entry.getKey());
                    topTable.addCell(bodyCell(eq.getCode()));
                    topTable.addCell(bodyCell(eq.getName()));
                    topTable.addCell(bodyCell(String.valueOf(entry.getValue())));
                }
                document.add(topTable);
            }

            buildFooter(document, pdfDoc);
        }

        return baos.toByteArray();
    }

}