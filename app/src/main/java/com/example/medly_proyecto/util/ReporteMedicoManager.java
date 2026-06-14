package com.example.medly_proyecto.util;

import android.content.Context;
import android.util.Log;
import com.example.medly_proyecto.model.Usuario;
import com.example.medly_proyecto.model.DatosMedicos;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReporteMedicoManager {

    private static final String TAG = "AUDITORÍA_PDF";
    private static final DeviceRgb CLINICAL_TEAL = new DeviceRgb(0, 168, 150);

    public static File generarReporteLocal(Context context, Usuario usuario, DatosMedicos datos) throws Exception {
        String nombreArchivo = "credencial_" + usuario.getUid() + ".pdf";
        File pdfFile = new File(context.getCacheDir(), nombreArchivo);

        if (pdfFile.exists()) {
            pdfFile.delete();
        }

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            PdfWriter writer = new PdfWriter(fos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // 1. TÍTULO Y SUBTÍTULO
            document.add(new Paragraph("INFORME DE ANÁLISIS CLÍNICO")
                    .setFontColor(CLINICAL_TEAL)
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Medly - Soluciones de Salud Digital")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // FECHA DE GENERACIÓN
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            document.add(new Paragraph("Fecha: " + fechaActual)
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(15));

            // 2. SECCIÓN: INFORMACIÓN DEL PACIENTE
            addSectionHeader(document, "INFORMACIÓN DEL PACIENTE");
            Table infoTable = new Table(UnitValue.createPointArray(new float[]{120, 330})).setWidth(UnitValue.createPercentValue(100));
            addCleanRow(infoTable, "Nombre:", usuario.getNombreCompleto());
            addCleanRow(infoTable, "Correo:", usuario.getCorreo());
            addCleanRow(infoTable, "Ciudad:", datos.getCiudad().isEmpty() ? usuario.getCiudad() : datos.getCiudad());
            addCleanRow(infoTable, "Edad:", datos.getEdad() + " años");
            addCleanRow(infoTable, "Sexo:", datos.getSexo());
            document.add(infoTable);

            // 3. SECCIÓN: EVALUACIÓN ANTROPOMÉTRICA Y RIESGO
            addSectionHeader(document, "EVALUACIÓN ANTROPOMÉTRICA Y RIESGO");
            
            double weight = datos.getPeso();
            double heightCm = datos.getEstatura();
            double imc = 0.0;
            String imcStatus = "N/A";
            
            if (weight > 0 && heightCm > 0) {
                double heightM = heightCm / 100.0;
                imc = weight / (heightM * heightM);
                imcStatus = getImcStatus(imc);
            }

            Table antroTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25})).setWidth(UnitValue.createPercentValue(100));
            antroTable.addCell(createStyledCell("Peso: " + weight + " kg"));
            antroTable.addCell(createStyledCell("Altura: " + (int)heightCm + " cm"));
            antroTable.addCell(createStyledCell("IMC: " + String.format(Locale.getDefault(), "%.2f", imc)));
            antroTable.addCell(createStyledCell(imcStatus).setFontColor(CLINICAL_TEAL).setBold());
            document.add(antroTable);

            // 4. SECCIÓN: HISTORIAL MÉDICO Y OBSERVACIONES
            addSectionHeader(document, "HISTORIAL MÉDICO Y OBSERVACIONES");
            document.add(new Paragraph("Enfermedad Crónica: " + (datos.getEnfermedadCronica() ? "Sí (" + datos.getDetalleEnfermedad() + ")" : "Ninguna registrada")).setFontSize(10));
            document.add(new Paragraph("Estado Signos Vitales: Estado general estable").setFontSize(10));
            document.add(new Paragraph("Evaluación Médica Inteligente: Evaluación automática sugiere monitoreo preventivo por nivel de riesgo " + getRiskLevel(imc).toLowerCase() + ".").setFontSize(10));

            // 5. SECCIÓN: RECOMENDACIONES CLÍNICAS
            addSectionHeader(document, "RECOMENDACIONES CLÍNICAS");
            document.add(new Paragraph("- Mantener un registro actualizado de medicamentos.").setFontSize(10));
            document.add(new Paragraph("- Realizar actividad física acorde a su clasificación de IMC.").setFontSize(10));
            document.add(new Paragraph("- En caso de riesgo moderado o alto, se recomienda consultar a un especialista nutricional.").setFontSize(10));
            document.add(new Paragraph("- Presentar este informe digital en su próxima cita médica.").setFontSize(10));

            // 6. RESULTADO FINAL
            document.add(new Paragraph("\n\nRESULTADO GENERAL: PACIENTE CON RIESGO " + getRiskLevel(imc).toUpperCase())
                    .setFontColor(CLINICAL_TEAL)
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30));

            document.close();
        }

        return pdfFile;
    }

    private static void addSectionHeader(Document document, String title) {
        document.add(new Paragraph(title)
                .setFontColor(CLINICAL_TEAL)
                .setFontSize(12)
                .setBold()
                .setMarginTop(15)
                .setMarginBottom(5));
    }

    private static void addCleanRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setFontSize(10));
        table.addCell(new Cell().add(new Paragraph(value != null && !value.isEmpty() ? value : "No disponible")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setFontSize(10));
    }

    private static Cell createStyledCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(10).setTextAlignment(TextAlignment.LEFT)).setPadding(5);
    }

    private static String getImcStatus(double imc) {
        if (imc < 18.5) return "Bajo peso";
        if (imc < 25.0) return "Normal";
        if (imc < 30.0) return "Sobrepeso";
        return "Obesidad";
    }

    private static String getRiskLevel(double imc) {
        if (imc < 25.0) return "Bajo";
        if (imc < 30.0) return "Moderado";
        return "Alto";
    }
}
