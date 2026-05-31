package com.example.medly_proyecto.util;

import android.content.Context;
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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReporteMedicoManager {

    /**
     * Genera un reporte médico PDF en la caché de la aplicación y devuelve el archivo.
     * Este enfoque evita subidas a la nube y permite apertura inmediata.
     */
    public static File generarReporteLocal(Context context, Usuario usuario, DatosMedicos datos) throws Exception {
        // Usamos el directorio de caché para archivos temporales
        String nombreArchivo = "Informe_Medico_" + usuario.getUid() + ".pdf";
        File pdfFile = new File(context.getCacheDir(), nombreArchivo);

        PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        DeviceRgb clinicalTeal = new DeviceRgb(0, 168, 150);

        // Cabecera profesional
        document.add(new Paragraph("MEDLY - REPORTE MÉDICO OFICIAL")
                .setFontColor(clinicalTeal)
                .setFontSize(22)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Resumen clínico generado desde la aplicación Medly.")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginBottom(20));

        // Tabla de datos
        Table table = new Table(UnitValue.createPointArray(new float[]{160, 290}))
                .setWidth(UnitValue.createPercentValue(100));

        String nombre = (usuario.getNombreCompleto() != null && !usuario.getNombreCompleto().isEmpty())
                ? usuario.getNombreCompleto()
                : (usuario.getNombres() + " " + usuario.getApellidos());

        addTableRow(table, "Paciente:", nombre);
        addTableRow(table, "Edad:", datos.getEdad() + " años");
        addTableRow(table, "Peso:", datos.getPeso() + " kg");
        addTableRow(table, "Estatura:", datos.getEstatura() + " cm");
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        addTableRow(table, "Fecha de Emisión:", sdf.format(new Date()));

        document.add(table);

        document.add(new Paragraph("\n\n__________________________")
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(40));
        document.add(new Paragraph("Firma Digital Medly")
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER));

        document.close();
        return pdfFile;
    }

    private static void addTableRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "No disponible")).setPadding(6));
    }
}
