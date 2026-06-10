package com.ibot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalInvoices;
    private long processedInvoices;
    private long pendingInvoices;
    private long flaggedInvoices;
    private long approvedInvoices;
    private long rejectedInvoices;
    private long duplicateInvoices;
    private BigDecimal totalAmountProcessed;
    private BigDecimal pendingAmount;
    private Double averageOcrConfidence;
    private long invoicesThisMonth;
    private long invoicesToday;
    private List<MonthlyStatItem> monthlyStats;
    private List<VendorStatItem> topVendors;
    private long recentActivityCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStatItem {
        private int month;
        private int year;
        private long count;
        private BigDecimal totalAmount;
        private String monthName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorStatItem {
        private String vendorName;
        private long invoiceCount;
        private BigDecimal totalAmount;
    }
}
