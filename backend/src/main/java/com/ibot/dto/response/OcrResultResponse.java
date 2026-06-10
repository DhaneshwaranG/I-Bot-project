package com.ibot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResultResponse {
    private String invoiceNumber;
    private String vendorName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal gstAmount;
    private BigDecimal gstRate;
    private BigDecimal totalAmount;
    private String currency;
    private String poNumber;
    private String rawText;
    private double confidence;
    private Map<String, FieldConfidence> fieldConfidences;
    private List<String> warnings;
    private boolean potentialDuplicate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldConfidence {
        private String value;
        private double confidence;
        private boolean requiresReview;
    }
}
