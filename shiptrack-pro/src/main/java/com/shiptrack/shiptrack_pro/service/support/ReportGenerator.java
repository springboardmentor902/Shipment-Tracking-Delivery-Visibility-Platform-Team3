package com.shiptrack.shiptrack_pro.service.support;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Renders a title + table of rows as either a PDF or an Excel file.
 * Shared by every report type so PDF/Excel layout logic exists in one place.
 */
@Component
public class ReportGenerator {

    public byte[] toPdf(String title, List<String> headers, List<List<String>> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 36, 24);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setSpacingAfter(12f);
            document.add(titleParagraph);

            Font metaFont = new Font(Font.HELVETICA, 9, Font.ITALIC, java.awt.Color.GRAY);
            Paragraph generatedAt = new Paragraph(
                    "Generated at " + java.time.LocalDateTime.now(), metaFont);
            generatedAt.setSpacingAfter(12f);
            document.add(generatedAt);

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new java.awt.Color(39, 39, 42)); // zinc-800
                cell.setPadding(6f);
                table.addCell(cell);
            }

            Font cellFont = new Font(Font.HELVETICA, 9);
            for (List<String> row : rows) {
                for (String value : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : value, cellFont));
                    cell.setPadding(5f);
                    table.addCell(cell);
                }
            }

            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No data available for this report.", cellFont));
                empty.setColspan(headers.size());
                empty.setPadding(8f);
                table.addCell(empty);
            }

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    public byte[] toExcel(String sheetName, List<String> headers, List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (List<String> rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowData.size(); i++) {
                    row.createCell(i).setCellValue(rowData.get(i) == null ? "-" : rowData.get(i));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }
}
