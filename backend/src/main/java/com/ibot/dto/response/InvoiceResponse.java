package com.ibot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String vendorName;
    private Long vendorId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal gstAmount;
    private BigDecimal gstRate;
    private BigDecimal totalAmount;
    private String currency;
    private String poNumber;
    private String paymentTerms;
    private String status;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private BigDecimal ocrConfidence;
    private boolean isDuplicate;
    private Long duplicateOfId;
    private String notes;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LineItemResponse> lineItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemResponse {
        private Long id;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String hsnSacCode;
        private BigDecimal gstRate;
        private Integer lineNumber;
    }
}
