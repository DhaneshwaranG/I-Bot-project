package com.ibot.service;

import com.ibot.entity.Invoice;
import com.ibot.entity.Invoice.InvoiceStatus;
import com.ibot.exception.ResourceNotFoundException;
import com.ibot.repository.InvoiceRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    public byte[] generateExcelReport(InvoiceStatus status, LocalDate startDate, LocalDate endDate) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Invoices");

            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₹#,##0.00"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/MM/yyyy"));

            // Header row
            String[] headers = {"ID", "Invoice #", "Vendor", "Date", "Due Date",
                                 "Subtotal", "GST", "Total", "Currency", "Status",
                                 "OCR Confidence%", "File Name", "Uploaded At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            List<Invoice> invoices = invoiceRepository.searchInvoices(
                    status, null, startDate, endDate, null, null,
                    PageRequest.of(0, 10000)).getContent();

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            int rowNum = 1;
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(inv.getId());
                row.createCell(1).setCellValue(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "");
                row.createCell(2).setCellValue(inv.getVendorName() != null ? inv.getVendorName() : "");
                row.createCell(3).setCellValue(inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(dtf) : "");
                row.createCell(4).setCellValue(inv.getDueDate() != null ? inv.getDueDate().format(dtf) : "");

                setCurrencyCell(row, 5, inv.getSubtotal(), currencyStyle);
                setCurrencyCell(row, 6, inv.getGstAmount(), currencyStyle);
                setCurrencyCell(row, 7, inv.getTotalAmount(), currencyStyle);

                row.createCell(8).setCellValue(inv.getCurrency() != null ? inv.getCurrency() : "INR");
                row.createCell(9).setCellValue(inv.getStatus().name());
                row.createCell(10).setCellValue(inv.getOcrConfidence() != null ? inv.getOcrConfidence().doubleValue() : 0);
                row.createCell(11).setCellValue(inv.getFileName() != null ? inv.getFileName() : "");
                row.createCell(12).setCellValue(inv.getCreatedAt() != null ? inv.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            summarySheet.createRow(0).createCell(0).setCellValue("Total Records: " + invoices.size());
            BigDecimal total = invoices.stream()
                    .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summarySheet.createRow(1).createCell(0).setCellValue("Grand Total: ₹" + total);
            summarySheet.createRow(2).createCell(0).setCellValue("Generated: " + java.time.LocalDateTime.now());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 20, com.itextpdf.text.Font.BOLD,
                    new BaseColor(36, 56, 156));
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Invoice Details
            com.itextpdf.text.Font labelFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font valueFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10);

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            addCell(headerTable, "Invoice Number:", labelFont);
            addCell(headerTable, invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "-", valueFont);
            addCell(headerTable, "Vendor:", labelFont);
            addCell(headerTable, invoice.getVendorName() != null ? invoice.getVendorName() : "-", valueFont);
            addCell(headerTable, "Invoice Date:", labelFont);
            addCell(headerTable, invoice.getInvoiceDate() != null ?
                    invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) : "-", valueFont);
            addCell(headerTable, "Due Date:", labelFont);
            addCell(headerTable, invoice.getDueDate() != null ?
                    invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) : "-", valueFont);
            addCell(headerTable, "Status:", labelFont);
            addCell(headerTable, invoice.getStatus().name(), valueFont);
            document.add(headerTable);
            document.add(Chunk.NEWLINE);

            // Financial Summary
            PdfPTable financialTable = new PdfPTable(2);
            financialTable.setWidthPercentage(50);
            financialTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            com.itextpdf.text.Font amountFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD);

            addCell(financialTable, "Subtotal:", labelFont);
            addCell(financialTable, formatAmount(invoice.getSubtotal(), invoice.getCurrency()), valueFont);
            addCell(financialTable, "GST (" + (invoice.getGstRate() != null ? invoice.getGstRate() + "%" : "N/A") + "):", labelFont);
            addCell(financialTable, formatAmount(invoice.getGstAmount(), invoice.getCurrency()), valueFont);

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL AMOUNT:", amountFont));
            totalLabelCell.setBorder(Rectangle.TOP);
            PdfPCell totalValueCell = new PdfPCell(new Phrase(formatAmount(invoice.getTotalAmount(), invoice.getCurrency()), amountFont));
            totalValueCell.setBorder(Rectangle.TOP);
            financialTable.addCell(totalLabelCell);
            financialTable.addCell(totalValueCell);
            document.add(financialTable);
            document.add(Chunk.NEWLINE);

            // Footer
            com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC,
                    BaseColor.GRAY);
            Paragraph footer = new Paragraph(
                    "Generated by I-Bot Intelligent Invoice Processing System • " + java.time.LocalDateTime.now(), footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addCell(PdfPTable table, String text, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) return "-";
        return (currency != null ? currency : "INR") + " " + String.format("%,.2f", amount);
    }

    private void setCurrencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        }
    }
}
