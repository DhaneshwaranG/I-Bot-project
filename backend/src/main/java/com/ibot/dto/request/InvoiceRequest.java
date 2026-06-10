package com.ibot.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceRequest {
    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotBlank(message = "Vendor name is required")
    private String vendorName;

    private Long vendorId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @DecimalMin(value = "0.00", message = "Subtotal cannot be negative")
    private BigDecimal subtotal;

    @DecimalMin(value = "0.00", message = "GST amount cannot be negative")
    private BigDecimal gstAmount;

    private BigDecimal gstRate;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;

    private String currency = "INR";
    private String poNumber;
    private String paymentTerms;
    private String notes;
    private List<LineItemRequest> lineItems;

    @Data
    public static class LineItemRequest {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String hsnSacCode;
        private BigDecimal gstRate;
    }
}
