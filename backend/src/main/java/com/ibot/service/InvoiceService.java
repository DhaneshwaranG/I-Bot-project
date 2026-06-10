package com.ibot.service;

import com.ibot.dto.request.InvoiceRequest;
import com.ibot.dto.response.InvoiceResponse;
import com.ibot.dto.response.OcrResultResponse;
import com.ibot.entity.Invoice;
import com.ibot.entity.Invoice.InvoiceStatus;
import com.ibot.entity.InvoiceLineItem;
import com.ibot.entity.User;
import com.ibot.entity.Vendor;
import com.ibot.exception.ResourceNotFoundException;
import com.ibot.repository.InvoiceRepository;
import com.ibot.repository.UserRepository;
import com.ibot.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final OcrService ocrService;
    private final AuditService auditService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Transactional
    public InvoiceResponse uploadAndProcess(MultipartFile file, String userEmail) {
        User user = getUserByEmail(userEmail);

        // Validate file type
        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            throw new IllegalArgumentException("Unsupported file type. Please upload PDF, PNG, JPG, or JPEG.");
        }

        // Save file
        String filePath = saveFile(file, user.getId());

        // Create invoice with PROCESSING status
        Invoice invoice = Invoice.builder()
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileType(getFileExtension(file.getOriginalFilename()))
                .fileSize(file.getSize())
                .status(InvoiceStatus.PROCESSING)
                .uploadedBy(user)
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice file saved with ID: {}", invoice.getId());

        // Process OCR
        try {
            OcrResultResponse ocrResult = ocrService.processFile(file);
            applyOcrResults(invoice, ocrResult);
            invoice.setStatus(InvoiceStatus.EXTRACTED);
        } catch (Exception e) {
            log.error("OCR processing failed for invoice {}: {}", invoice.getId(), e.getMessage());
            invoice.setStatus(InvoiceStatus.PENDING);
        }

        // Check for duplicates
        checkDuplicates(invoice);

        invoice = invoiceRepository.save(invoice);

        auditService.logInvoiceAction(invoice.getId(), "UPLOAD",
                "Invoice uploaded and OCR processed", user.getId(), user.getName());

        return mapToResponse(invoice);
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .vendorName(request.getVendorName())
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .subtotal(request.getSubtotal())
                .gstAmount(request.getGstAmount())
                .gstRate(request.getGstRate())
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .poNumber(request.getPoNumber())
                .paymentTerms(request.getPaymentTerms())
                .notes(request.getNotes())
                .status(InvoiceStatus.PENDING)
                .uploadedBy(user)
                .build();

        // Link vendor if provided
        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor", request.getVendorId()));
            invoice.setVendor(vendor);
        }

        // Add line items
        if (request.getLineItems() != null) {
            List<InvoiceLineItem> lineItems = new ArrayList<>();
            for (int i = 0; i < request.getLineItems().size(); i++) {
                InvoiceRequest.LineItemRequest li = request.getLineItems().get(i);
                lineItems.add(InvoiceLineItem.builder()
                        .invoice(invoice)
                        .description(li.getDescription())
                        .quantity(li.getQuantity())
                        .unitPrice(li.getUnitPrice())
                        .totalPrice(li.getTotalPrice())
                        .hsnSacCode(li.getHsnSacCode())
                        .gstRate(li.getGstRate())
                        .lineNumber(i + 1)
                        .build());
            }
            invoice.setLineItems(lineItems);
        }

        checkDuplicates(invoice);
        invoice = invoiceRepository.save(invoice);

        auditService.logInvoiceAction(invoice.getId(), "CREATE",
                "Invoice created manually", user.getId(), user.getName());

        return mapToResponse(invoice);
    }

    @Transactional
    public InvoiceResponse updateInvoice(Long id, InvoiceRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        String oldStatus = invoice.getStatus().name();

        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setVendorName(request.getVendorName());
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setSubtotal(request.getSubtotal());
        invoice.setGstAmount(request.getGstAmount());
        invoice.setGstRate(request.getGstRate());
        invoice.setTotalAmount(request.getTotalAmount());
        invoice.setPoNumber(request.getPoNumber());
        invoice.setPaymentTerms(request.getPaymentTerms());
        invoice.setNotes(request.getNotes());

        invoice = invoiceRepository.save(invoice);

        auditService.logInvoiceAction(invoice.getId(), "UPDATE",
                "Invoice updated from status " + oldStatus, user.getId(), user.getName());

        return mapToResponse(invoice);
    }

    @Transactional
    public InvoiceResponse validateInvoice(Long id, InvoiceRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // Update all OCR-extracted fields with validated data
        if (request.getInvoiceNumber() != null) invoice.setInvoiceNumber(request.getInvoiceNumber());
        if (request.getVendorName() != null) invoice.setVendorName(request.getVendorName());
        if (request.getInvoiceDate() != null) invoice.setInvoiceDate(request.getInvoiceDate());
        if (request.getDueDate() != null) invoice.setDueDate(request.getDueDate());
        if (request.getSubtotal() != null) invoice.setSubtotal(request.getSubtotal());
        if (request.getGstAmount() != null) invoice.setGstAmount(request.getGstAmount());
        if (request.getGstRate() != null) invoice.setGstRate(request.getGstRate());
        if (request.getTotalAmount() != null) invoice.setTotalAmount(request.getTotalAmount());
        if (request.getNotes() != null) invoice.setNotes(request.getNotes());

        invoice.setStatus(InvoiceStatus.VALIDATED);
        invoice.setValidatedBy(user.getId());
        invoice.setValidatedAt(java.time.LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        auditService.logInvoiceAction(invoice.getId(), "VALIDATE",
                "Invoice validated by " + user.getName(), user.getId(), user.getName());

        return mapToResponse(invoice);
    }

    @Transactional
    public InvoiceResponse approveInvoice(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice = invoiceRepository.save(invoice);

        auditService.logInvoiceAction(id, "APPROVE", "Invoice approved", user.getId(), user.getName());
        return mapToResponse(invoice);
    }

    @Transactional
    public InvoiceResponse rejectInvoice(Long id, String reason, String userEmail) {
        User user = getUserByEmail(userEmail);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        invoice.setStatus(InvoiceStatus.REJECTED);
        invoice.setNotes(reason);
        invoice = invoiceRepository.save(invoice);

        auditService.logInvoiceAction(id, "REJECT", "Invoice rejected: " + reason, user.getId(), user.getName());
        return mapToResponse(invoice);
    }

    @Transactional
    public void deleteInvoice(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        invoiceRepository.delete(invoice);
        auditService.logInvoiceAction(id, "DELETE", "Invoice deleted", user.getId(), user.getName());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        return mapToResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> searchInvoices(InvoiceStatus status, String vendorName,
                                                  LocalDate startDate, LocalDate endDate,
                                                  BigDecimal minAmount, BigDecimal maxAmount,
                                                  Pageable pageable) {
        return invoiceRepository.searchInvoices(status, vendorName, startDate, endDate,
                        minAmount, maxAmount, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getUserInvoices(String userEmail, Pageable pageable) {
        User user = getUserByEmail(userEmail);
        return invoiceRepository.findByUploadedById(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    // ===== Private helpers =====

    private void checkDuplicates(Invoice invoice) {
        if (invoice.getInvoiceNumber() == null) return;

        Optional<Invoice> existing = invoiceRepository.findByInvoiceNumber(invoice.getInvoiceNumber());
        if (existing.isPresent() && !existing.get().getId().equals(invoice.getId())) {
            invoice.setDuplicate(true);
            invoice.setDuplicateOfId(existing.get().getId());
            invoice.setStatus(InvoiceStatus.DUPLICATE);
            log.warn("Duplicate invoice detected: {} matches existing id {}",
                    invoice.getInvoiceNumber(), existing.get().getId());
        }
    }

    private void applyOcrResults(Invoice invoice, OcrResultResponse ocr) {
        invoice.setInvoiceNumber(ocr.getInvoiceNumber());
        invoice.setVendorName(ocr.getVendorName());
        invoice.setInvoiceDate(ocr.getInvoiceDate());
        invoice.setSubtotal(ocr.getSubtotal());
        invoice.setGstAmount(ocr.getGstAmount());
        invoice.setGstRate(ocr.getGstRate());
        invoice.setTotalAmount(ocr.getTotalAmount());
        invoice.setCurrency(ocr.getCurrency());
        invoice.setOcrRawText(ocr.getRawText());
        invoice.setOcrConfidence(BigDecimal.valueOf(ocr.getConfidence() * 100).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private String saveFile(MultipartFile file, Long userId) {
        try {
            Path uploadPath = Paths.get(uploadDir, "invoices", userId.toString());
            Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
            contentType.equals("application/pdf") ||
            contentType.equals("image/png") ||
            contentType.equals("image/jpeg") ||
            contentType.equals("image/jpg")
        );
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public InvoiceResponse mapToResponse(Invoice invoice) {
        List<InvoiceResponse.LineItemResponse> lineItemResponses = invoice.getLineItems() != null ?
            invoice.getLineItems().stream()
                .map(li -> InvoiceResponse.LineItemResponse.builder()
                    .id(li.getId())
                    .description(li.getDescription())
                    .quantity(li.getQuantity())
                    .unitPrice(li.getUnitPrice())
                    .totalPrice(li.getTotalPrice())
                    .hsnSacCode(li.getHsnSacCode())
                    .gstRate(li.getGstRate())
                    .lineNumber(li.getLineNumber())
                    .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .vendorName(invoice.getVendorName())
                .vendorId(invoice.getVendor() != null ? invoice.getVendor().getId() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .gstAmount(invoice.getGstAmount())
                .gstRate(invoice.getGstRate())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .poNumber(invoice.getPoNumber())
                .paymentTerms(invoice.getPaymentTerms())
                .status(invoice.getStatus().name())
                .fileName(invoice.getFileName())
                .fileType(invoice.getFileType())
                .fileSize(invoice.getFileSize())
                .ocrConfidence(invoice.getOcrConfidence())
                .isDuplicate(invoice.isDuplicate())
                .duplicateOfId(invoice.getDuplicateOfId())
                .notes(invoice.getNotes())
                .uploadedById(invoice.getUploadedBy() != null ? invoice.getUploadedBy().getId() : null)
                .uploadedByName(invoice.getUploadedBy() != null ? invoice.getUploadedBy().getName() : null)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .lineItems(lineItemResponses)
                .build();
    }
}
