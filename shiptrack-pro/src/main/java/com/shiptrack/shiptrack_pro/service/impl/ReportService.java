package com.shiptrack.shiptrack_pro.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ReportService {

    private final ShipmentRepository shipmentRepository;

    public ReportService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }


    // =========================
    // Get shipment data
    // =========================

    private List<Shipment> getShipments() {

        return shipmentRepository.findAll();
    }


    // =========================
    // PDF
    // =========================

    public byte[] generatePdf() throws Exception {

        List<Shipment> shipments = getShipments();

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        Document document = new Document();

        PdfWriter.getInstance(document, outputStream);

        document.open();


        // Title
        Font titleFont =
                new Font(Font.HELVETICA, 18, Font.BOLD);

        Paragraph title =
                new Paragraph("Shipment Report", titleFont);

        title.setAlignment(Element.ALIGN_CENTER);

        document.add(title);

        document.add(new Paragraph(" "));


        // Table
        PdfPTable table =
                new PdfPTable(6);

        table.setWidthPercentage(100);

        String[] headers = {
                "Tracking Number",
                "Receiver",
                "Status",
                "Priority",
                "Packages",
                "ETA"
        };


        // Table headers
        for (String header : headers) {

            PdfPCell cell =
                    new PdfPCell(new Phrase(header));

            table.addCell(cell);
        }


        // Table data
        for (Shipment shipment : shipments) {

            table.addCell(
                    shipment.getTrackingNumber()
            );

            table.addCell(
                    shipment.getReceiverName()
            );

            table.addCell(
                    shipment.getStatus().toString()
            );

            table.addCell(
                    shipment.getPriority().toString()
            );

            table.addCell(
                    String.valueOf(
                            shipment.getPackages().size()
                    )
            );

            table.addCell(
                    shipment.getEstimatedDeliveryDate() != null
                            ? shipment.getEstimatedDeliveryDate().toString()
                            : "-"
            );
        }


        document.add(table);

        document.close();

        return outputStream.toByteArray();
    }


    // =========================
    // EXCEL
    // =========================

    public byte[] generateExcel() throws Exception {

        List<Shipment> shipments = getShipments();

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();


        // Streaming workbook - better for large reports
        SXSSFWorkbook workbook =
                new SXSSFWorkbook();

        Sheet sheet =
                workbook.createSheet("Shipments");


        // Header
        Row headerRow =
                sheet.createRow(0);

        String[] headers = {
                "Tracking Number",
                "Receiver",
                "Status",
                "Priority",
                "Packages",
                "ETA"
        };


        for (int i = 0; i < headers.length; i++) {

            Cell cell =
                    headerRow.createCell(i);

            cell.setCellValue(headers[i]);
        }


        // Data
        int rowNumber = 1;

        for (Shipment shipment : shipments) {

            Row row =
                    sheet.createRow(rowNumber++);


            row.createCell(0)
                    .setCellValue(
                            shipment.getTrackingNumber()
                    );


            row.createCell(1)
                    .setCellValue(
                            shipment.getReceiverName()
                    );


            row.createCell(2)
                    .setCellValue(
                            shipment.getStatus().toString()
                    );


            row.createCell(3)
                    .setCellValue(
                            shipment.getPriority().toString()
                    );


            row.createCell(4)
                    .setCellValue(
                            shipment.getPackages().size()
                    );


            row.createCell(5)
                    .setCellValue(
                            shipment.getEstimatedDeliveryDate() != null
                                    ? shipment.getEstimatedDeliveryDate().toString()
                                    : "-"
                    );
        }


        // Column widths
        for (int i = 0; i < 6; i++) {

            sheet.setColumnWidth(i, 5000);
        }


        workbook.write(outputStream);

        workbook.close();

        return outputStream.toByteArray();
    }
}

