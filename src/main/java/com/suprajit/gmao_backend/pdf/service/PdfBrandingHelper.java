package com.suprajit.gmao_backend.pdf.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

// ── Composant partagé : en-tête avec logo, réutilisé par tous les
// générateurs de PDF (interventions, bilans, historique consommation...) ──
@Component
public class PdfBrandingHelper {

    public void buildBrandedHeader(Document document, String title, String subtitle, String dateLine) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{18, 82}))
                .useAllAvailableWidth()
                .setMarginBottom(4);

        Cell logoCell = new Cell().setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        Image logo = loadLogo();
        if (logo != null) {
            logo.setWidth(45).setAutoScaleHeight(true);
            logoCell.add(logo);
        }
        headerTable.addCell(logoCell);

        Cell textCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingLeft(12)
                .setPaddingTop(0)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        textCell.add(new Paragraph(title)
                .setFontSize(16).setBold()
                .setFontColor(new DeviceRgb(30, 58, 95))
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

    public Image loadLogo() {
        try (InputStream is = new ClassPathResource("static/images/logo_suprajit.png").getInputStream()) {
            byte[] logoBytes = is.readAllBytes();
            ImageData imageData = ImageDataFactory.create(logoBytes);
            return new Image(imageData);
        } catch (IOException e) {
            System.out.println("[PDF] Logo introuvable, génération sans logo : " + e.getMessage());
            return null;
        }
    }
}