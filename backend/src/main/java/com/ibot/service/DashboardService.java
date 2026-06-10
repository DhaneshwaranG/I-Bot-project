package com.ibot.service;

import com.ibot.dto.response.DashboardStatsResponse;
import com.ibot.entity.Invoice.InvoiceStatus;
import com.ibot.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private static final String[] MONTH_NAMES = {"","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long total     = invoiceRepository.count();
        long processed = invoiceRepository.countByStatus(InvoiceStatus.VALIDATED)
                       + invoiceRepository.countByStatus(InvoiceStatus.APPROVED);
        long pending   = invoiceRepository.countByStatus(InvoiceStatus.PENDING)
                       + invoiceRepository.countByStatus(InvoiceStatus.PROCESSING)
                       + invoiceRepository.countByStatus(InvoiceStatus.EXTRACTED);
        long flagged   = invoiceRepository.countByStatus(InvoiceStatus.FLAGGED);
        long approved  = invoiceRepository.countByStatus(InvoiceStatus.APPROVED);
        long rejected  = invoiceRepository.countByStatus(InvoiceStatus.REJECTED);
        long dups      = invoiceRepository.countByStatus(InvoiceStatus.DUPLICATE);

        BigDecimal totalAmount = Optional.ofNullable(invoiceRepository.sumTotalAmountByStatus(InvoiceStatus.APPROVED))
                .orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(invoiceRepository.sumTotalAmountByStatus(InvoiceStatus.VALIDATED)).orElse(BigDecimal.ZERO));

        BigDecimal pendingAmount = Optional.ofNullable(invoiceRepository.sumTotalAmountByStatus(InvoiceStatus.PENDING))
                .orElse(BigDecimal.ZERO);

        Double avgConf = invoiceRepository.getAverageOcrConfidence();
        long thisMonth = invoiceRepository.countSince(LocalDate.now().withDayOfMonth(1).atStartOfDay());
        long today     = invoiceRepository.countSince(LocalDate.now().atStartOfDay());

        List<DashboardStatsResponse.MonthlyStatItem> monthlyStats = new ArrayList<>();
        for (Object[] row : invoiceRepository.getMonthlyStats(LocalDate.now().minusMonths(11).withDayOfMonth(1))) {
            monthlyStats.add(DashboardStatsResponse.MonthlyStatItem.builder()
                .month(((Number)row[0]).intValue()).year(((Number)row[1]).intValue())
                .count(((Number)row[2]).longValue())
                .totalAmount(row[3] != null ? (BigDecimal)row[3] : BigDecimal.ZERO)
                .monthName(MONTH_NAMES[((Number)row[0]).intValue()])
                .build());
        }

        List<DashboardStatsResponse.VendorStatItem> topVendors = new ArrayList<>();
        for (Object[] row : invoiceRepository.getTopVendors(PageRequest.of(0, 5))) {
            topVendors.add(DashboardStatsResponse.VendorStatItem.builder()
                .vendorName((String)row[0])
                .invoiceCount(((Number)row[1]).longValue())
                .totalAmount(row[2] != null ? (BigDecimal)row[2] : BigDecimal.ZERO)
                .build());
        }

        return DashboardStatsResponse.builder()
                .totalInvoices(total).processedInvoices(processed).pendingInvoices(pending)
                .flaggedInvoices(flagged).approvedInvoices(approved).rejectedInvoices(rejected)
                .duplicateInvoices(dups).totalAmountProcessed(totalAmount).pendingAmount(pendingAmount)
                .averageOcrConfidence(avgConf != null ? avgConf : 0.0)
                .invoicesThisMonth(thisMonth).invoicesToday(today)
                .monthlyStats(monthlyStats).topVendors(topVendors).recentActivityCount(today)
                .build();
    }
}
