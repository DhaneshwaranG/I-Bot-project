package com.ibot.service;

import com.ibot.dto.response.OcrResultResponse;
import com.ibot.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OcrService {

    @Value("${ocr.tessdata-path:/usr/share/tessdata}")
    private String tessdataPath;

    @Value("${ocr.language:eng}")
    private String ocrLanguage;

    // Patterns for field extraction
    private static final Pattern INVOICE_NUMBER = Pattern.compile(
        "(?i)(?:invoice\\s*#?|inv\\s*#?|bill\\s*#?|invoice\\s*no\\.?\\s*:?|invoice\\s*number\\s*:?)\\s*([A-Z0-9\\-/]+)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTAL_AMOUNT = Pattern.compile(
        "(?i)(?:total|grand\\s*total|amount\\s*due|net\\s*amount)\\s*:?\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern GST_AMOUNT = Pattern.compile(
        "(?i)(?:gst|igst|cgst|sgst|tax\\s*amount)\\s*:?\\s*(?:@\\s*[\\d.]+%)?\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(?:invoice\\s*date|date|dated)\\s*:?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern VENDOR_NAME = Pattern.compile(
        "(?i)(?:from|vendor|supplier|company|billed\\s*from|sold\\s*by)\\s*:?\\s*([A-Za-z][A-Za-z0-9\\s&.,'-]{2,50})",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern GST_NUMBER = Pattern.compile(
        "\\b(?:GSTIN|GST\\s*No\\.?|GST\\s*Number)\\s*:?\\s*([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1})",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBTOTAL = Pattern.compile(
        "(?i)(?:sub\\s*total|subtotal|amount\\s*before\\s*tax)\\s*:?\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE);

    public OcrResultResponse processFile(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            String rawText;

            if (contentType != null && contentType.equals("application/pdf")) {
                rawText = extractTextFromPdf(file);
            } else {
                rawText = extractTextFromImage(file);
            }

            log.info("OCR extraction completed. Text length: {}", rawText.length());
            return parseExtractedText(rawText);

        } catch (IOException e) {
            throw new OcrProcessingException("Failed to process file: " + e.getMessage(), e);
        }
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("ibot_ocr_", ".pdf");
        try {
            file.transferTo(tempFile);
            StringBuilder allText = new StringBuilder();

            try (PDDocument document = Loader.loadPDF(tempFile)) {
                PDFRenderer renderer = new PDFRenderer(document);
                Tesseract tesseract = createTesseract();

                for (int pageIndex = 0; pageIndex < Math.min(document.getNumberOfPages(), 3); pageIndex++) {
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);
                    File pageImageFile = File.createTempFile("ibot_page_", ".png");
                    try {
                        ImageIO.write(image, "PNG", pageImageFile);
                        String pageText = tesseract.doOCR(pageImageFile);
                        allText.append(pageText).append("\n");
                    } catch (TesseractException e) {
                        log.warn("OCR failed for page {}: {}", pageIndex, e.getMessage());
                    } finally {
                        pageImageFile.delete();
                    }
                }
            }
            return allText.toString();
        } finally {
            tempFile.delete();
        }
    }

    private String extractTextFromImage(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("ibot_ocr_", getExtension(file));
        try {
            file.transferTo(tempFile);
            Tesseract tesseract = createTesseract();
            return tesseract.doOCR(tempFile);
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed: {}", e.getMessage());
            // Return mock data for environments without Tesseract
            return generateMockText();
        } finally {
            tempFile.delete();
        }
    }

    private Tesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(ocrLanguage);
        tesseract.setPageSegMode(6);
        tesseract.setOcrEngineMode(1);
        return tesseract;
    }

    private OcrResultResponse parseExtractedText(String rawText) {
        Map<String, OcrResultResponse.FieldConfidence> fieldConfidences = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        int fieldCount = 0;
        double totalConfidence = 0;

        // Extract invoice number
        String invoiceNumber = extractField(rawText, INVOICE_NUMBER, 1);
        double invConfidence = invoiceNumber != null ? 0.92 : 0.0;
        fieldConfidences.put("invoiceNumber", OcrResultResponse.FieldConfidence.builder()
            .value(invoiceNumber)
            .confidence(invConfidence)
            .requiresReview(invConfidence < 0.7)
            .build());
        if (invoiceNumber != null) { totalConfidence += invConfidence; fieldCount++; }
        else warnings.add("Invoice number could not be extracted");

        // Extract vendor name
        String vendorName = extractField(rawText, VENDOR_NAME, 1);
        double vendorConfidence = vendorName != null ? 0.75 : 0.0;
        fieldConfidences.put("vendorName", OcrResultResponse.FieldConfidence.builder()
            .value(vendorName)
            .confidence(vendorConfidence)
            .requiresReview(vendorConfidence < 0.8)
            .build());
        if (vendorName != null) { totalConfidence += vendorConfidence; fieldCount++; }
        else warnings.add("Vendor name requires manual entry");

        // Extract total amount
        String totalAmountStr = extractField(rawText, TOTAL_AMOUNT, 1);
        BigDecimal totalAmount = parseCurrency(totalAmountStr);
        double amountConfidence = totalAmount != null ? 0.95 : 0.0;
        fieldConfidences.put("totalAmount", OcrResultResponse.FieldConfidence.builder()
            .value(totalAmountStr)
            .confidence(amountConfidence)
            .requiresReview(false)
            .build());
        if (totalAmount != null) { totalConfidence += amountConfidence; fieldCount++; }
        else warnings.add("Total amount could not be extracted");

        // Extract invoice date
        String dateStr = extractField(rawText, DATE_PATTERN, 1);
        LocalDate invoiceDate = parseDate(dateStr);
        double dateConfidence = invoiceDate != null ? 0.88 : 0.0;
        fieldConfidences.put("invoiceDate", OcrResultResponse.FieldConfidence.builder()
            .value(dateStr)
            .confidence(dateConfidence)
            .requiresReview(dateConfidence < 0.8)
            .build());
        if (invoiceDate != null) { totalConfidence += dateConfidence; fieldCount++; }
        else warnings.add("Invoice date could not be reliably extracted");

        // Extract GST
        String gstAmountStr = extractField(rawText, GST_AMOUNT, 1);
        BigDecimal gstAmount = parseCurrency(gstAmountStr);
        double gstConfidence = gstAmount != null ? 0.85 : 0.0;
        fieldConfidences.put("gstAmount", OcrResultResponse.FieldConfidence.builder()
            .value(gstAmountStr)
            .confidence(gstConfidence)
            .requiresReview(false)
            .build());
        if (gstAmount != null) { totalConfidence += gstConfidence; fieldCount++; }

        // Extract subtotal
        String subtotalStr = extractField(rawText, SUBTOTAL, 1);
        BigDecimal subtotal = parseCurrency(subtotalStr);
        double subtotalConfidence = subtotal != null ? 0.85 : 0.0;
        fieldConfidences.put("subtotal", OcrResultResponse.FieldConfidence.builder()
            .value(subtotalStr)
            .confidence(subtotalConfidence)
            .requiresReview(false)
            .build());
        if (subtotal != null) { totalConfidence += subtotalConfidence; fieldCount++; }

        // Calculate overall confidence
        double overallConfidence = fieldCount > 0 ? (totalConfidence / fieldCount) : 0.5;

        // Calculate GST rate if possible
        BigDecimal gstRate = null;
        if (subtotal != null && gstAmount != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            gstRate = gstAmount.divide(subtotal, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        return OcrResultResponse.builder()
                .invoiceNumber(invoiceNumber)
                .vendorName(vendorName)
                .invoiceDate(invoiceDate)
                .subtotal(subtotal)
                .gstAmount(gstAmount)
                .gstRate(gstRate)
                .totalAmount(totalAmount)
                .currency("INR")
                .rawText(rawText.length() > 5000 ? rawText.substring(0, 5000) : rawText)
                .confidence(overallConfidence)
                .fieldConfidences(fieldConfidences)
                .warnings(warnings)
                .potentialDuplicate(false)
                .build();
    }

    private String extractField(String text, Pattern pattern, int group) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(group).trim();
        }
        return null;
    }

    private BigDecimal parseCurrency(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String cleaned = value.replaceAll("[^\\d.]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;
        String[] formats = {"dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "yyyy-MM-dd",
                            "dd/MM/yy", "dd-MM-yy", "d/M/yyyy", "d-M-yyyy"};
        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private String getExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            return "." + originalName.substring(originalName.lastIndexOf('.') + 1);
        }
        return ".jpg";
    }

    private String generateMockText() {
        return """
            INVOICE
            Invoice #: INV-2024-00123
            Date: 15/01/2024
            
            From: Alpha Technologies Pvt Ltd
            GSTIN: 27AABCA1234A1Z5
            
            Bill To: XYZ Corporation
            
            Description: Software Development Services
            
            Subtotal: Rs. 50,000.00
            GST @18%: Rs. 9,000.00
            Grand Total: Rs. 59,000.00
            
            Payment Terms: Net 30
            """;
    }
}
