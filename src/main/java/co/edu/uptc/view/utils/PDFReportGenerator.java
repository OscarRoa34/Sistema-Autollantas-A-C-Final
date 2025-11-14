package co.edu.uptc.view.utils;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class PDFReportGenerator {

    @SuppressWarnings("unused")
    public static void generarReporteConTabla(String plantillaPath, String destino,
            String titulo,
            List<String> headers,
            List<List<String>> rows) throws IOException {

        PdfReader reader = new PdfReader(plantillaPath);
        PdfWriter writer = new PdfWriter(destino);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);
        Document document = new Document(pdfDoc);

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        document.setFont(font);

        Rectangle pageSize = pdfDoc.getFirstPage().getPageSize();
        float topMargin = 140;
        float sideMargin = 50;
        float bottomMargin = 50;
        document.setMargins(topMargin, sideMargin, bottomMargin, sideMargin);

        int numPages = pdfDoc.getNumberOfPages();
        for (int i = 1; i <= numPages; i++) {
            PdfPage page = pdfDoc.getPage(i);
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
            canvas.addXObjectAt(pdfDoc.getPage(1).copyAsFormXObject(pdfDoc), 0, 0);
        }

        Paragraph titleParagraph = new Paragraph(titulo)
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(titleParagraph);

        Table table = new Table(UnitValue.createPercentArray(headers.size())).useAllAvailableWidth();
        table.setMarginTop(10);

        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header).setBold())
                    .setTextAlignment(TextAlignment.LEFT));
        }

        for (List<String> row : rows) {
            for (int j = 0; j < headers.size(); j++) {
                String cellText = (j < row.size()) ? row.get(j) : "";
                TextAlignment align = (j == headers.size() - 1) ? TextAlignment.RIGHT : TextAlignment.LEFT;
                table.addCell(new Cell().add(new Paragraph(cellText)).setTextAlignment(align));
            }
        }

        document.add(table);

        Paragraph dateParagraph = new Paragraph("Generado el: " + new Date())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(20);
        document.add(dateParagraph);

        document.close();
        pdfDoc.close();
    }
}
