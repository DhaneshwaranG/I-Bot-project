package com.ibot.controller;

import com.ibot.dto.request.InvoiceRequest;
import com.ibot.dto.response.ApiResponse;
import com.ibot.dto.response.InvoiceResponse;
import com.ibot.entity.Invoice.InvoiceStatus;
import com.ibot.service.InvoiceService;
import com.ibot.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ReportService reportService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InvoiceResponse>> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        InvoiceResponse response = invoiceService.uploadAndProcess(file, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice uploaded and processed", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody InvoiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        InvoiceResponse response = invoiceService.createInvoice(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<InvoiceResponse> invoices = invoiceService.searchInvoices(
                status, vendorName, startDate, endDate, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getMyInvoices(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InvoiceResponse> invoices = invoiceService.getUserInvoices(
                userDetails.getUsername(), PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        InvoiceResponse response = invoiceService.updateInvoice(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Invoice updated", response));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<InvoiceResponse>> validateInvoice(
            @PathVariable Long id,
            @RequestBody InvoiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        InvoiceResponse response = invoiceService.validateInvoice(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Invoice validated", response));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<InvoiceResponse>> approveInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        InvoiceResponse response = invoiceService.approveInvoice(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Invoice approved", response));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<InvoiceResponse>> rejectInvoice(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String reason = body.getOrDefault("reason", "Rejected by user");
        InvoiceResponse response = invoiceService.rejectInvoice(id, reason, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Invoice rejected", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        invoiceService.deleteInvoice(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Invoice deleted", null));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] excelData = reportService.generateExcelReport(status, startDate, endDate);
        String filename = "invoices_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportToPdf(@PathVariable Long id) {
        byte[] pdfData = reportService.generateInvoicePdf(id);
        String filename = "invoice_" + id + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}
