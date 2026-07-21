package com.suprajit.gmao_backend.sparepart.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.suprajit.gmao_backend.pdf.service.PdfBrandingHelper;
import com.suprajit.gmao_backend.sparepart.dto.PartConsumptionResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsumptionExportService {

    private final SparePartService sparePartService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] HEADERS = {
            "Date", "Référence", "Pièce", "Équipement", "Quantité",
            "Prix unitaire", "Prix total", "Type", "Technicien"
    };
    private final PdfBrandingHelper brandingHelper;

    // ══════════════════════════════════════════════════════════
    // EXCEL
    // ══════════════════════════════════════════════════════════

    public byte[] exportToExcel(String type) throws IOException {
        List<PartConsumptionResponseDTO> data = sparePartService.getConsumptionHistory(type);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Consommation");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            BigDecimal total = BigDecimal.ZERO;

            for (PartConsumptionResponseDTO c : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getConsumptionDate() != null
                        ? c.getConsumptionDate().format(DATE_FORMAT) : "");
                row.createCell(1).setCellValue(nullToEmpty(c.getSparePartReference()));
                row.createCell(2).setCellValue(nullToEmpty(c.getSparePartName()));
                row.createCell(3).setCellValue(nullToEmpty(c.getEquipmentCode()));
                row.createCell(4).setCellValue(c.getQuantityUsed() != null ? c.getQuantityUsed() : 0);
                row.createCell(5).setCellValue(c.getUnitPrice() != null ? c.getUnitPrice().doubleValue() : 0);
                row.createCell(6).setCellValue(c.getTotalPrice() != null ? c.getTotalPrice().doubleValue() : 0);
                row.createCell(7).setCellValue(translateType(c.getConsumptionType()));
                row.createCell(8).setCellValue(nullToEmpty(c.getTechnicianName()));

                if (c.getTotalPrice() != null) {
                    total = total.add(c.getTotalPrice());
                }
            }

            // Ligne de total
            Row totalRow = sheet.createRow(rowIdx + 1);
            org.apache.poi.ss.usermodel.Cell totalLabelCell = totalRow.createCell(5);
            totalLabelCell.setCellValue("Total :");
            totalLabelCell.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell totalValueCell = totalRow.createCell(6);
            totalValueCell.setCellValue(total.doubleValue());
            totalValueCell.setCellStyle(headerStyle);

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // ══════════════════════════════════════════════════════════
    // PDF
    // ══════════════════════════════════════════════════════════

    public byte[] exportToPdf(String type) throws IOException {
        List<PartConsumptionResponseDTO> data = sparePartService.getConsumptionHistory(type);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

    document.setMargins(20, 20, 20, 20);

            String title = "Historique de consommation des pièces"
                    + (type != null ? " — " + translateType(type) : "");

            brandingHelper.buildBrandedHeader(document, title, null,
                    "Généré le : " + java.time.LocalDateTime.now().format(DATE_FORMAT));

            Table table = new Table(UnitValue.createPercentArray(
                    new float[]{12, 10, 15, 10, 8, 10, 10, 12, 13}))
                    .useAllAvailableWidth();

            for (String h : HEADERS) {
                table.addHeaderCell(headerCell(h));
            }

            BigDecimal total = BigDecimal.ZERO;

            for (PartConsumptionResponseDTO c : data) {
                table.addCell(bodyCell(c.getConsumptionDate() != null
                        ? c.getConsumptionDate().format(DATE_FORMAT) : "—"));
                table.addCell(bodyCell(c.getSparePartReference()));
                table.addCell(bodyCell(c.getSparePartName()));
                table.addCell(bodyCell(c.getEquipmentCode()));
                table.addCell(bodyCell(String.valueOf(c.getQuantityUsed())));
                table.addCell(bodyCell(c.getUnitPrice() != null ? c.getUnitPrice() + " MAD" : "—"));
                table.addCell(bodyCell(c.getTotalPrice() != null ? c.getTotalPrice() + " MAD" : "—"));
                table.addCell(bodyCell(translateType(c.getConsumptionType())));
                table.addCell(bodyCell(c.getTechnicianName()));

                if (c.getTotalPrice() != null) {
                    total = total.add(c.getTotalPrice());
                }
            }

            document.add(table);

            document.add(new Paragraph("Total : " + total + " MAD")
                    .setBold().setFontSize(11).setMarginTop(10)
                    .setTextAlignment(TextAlignment.RIGHT));

            int pageCount = pdfDoc.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                document.showTextAligned(
                        new Paragraph("Page " + i + " / " + pageCount)
                                .setFontSize(8).setFontColor(ColorConstants.GRAY),
                        297.5f, 15, i, TextAlignment.CENTER,
                        com.itextpdf.layout.properties.VerticalAlignment.BOTTOM, 0
                );
            }
        }

        return baos.toByteArray();
    }

    // ── Helpers ──────────────────────────────────────────────
    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(8).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(30, 58, 95))
                .setPadding(4);
    }

    private Cell bodyCell(String text) {
        return new Cell().add(new Paragraph(text != null ? text : "—").setFontSize(8)).setPadding(4);
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private String translateType(String type) {
        if (type == null) return "—";
        return switch (type.toUpperCase()) {
            case "CORRECTIVE" -> "Intervention (panne)";
            case "PREVENTIVE" -> "Maintenance préventive";
            default -> type;
        };
    }
}