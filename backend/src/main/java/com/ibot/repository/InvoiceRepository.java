package com.ibot.repository;

import com.ibot.entity.Invoice;
import com.ibot.entity.Invoice.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    Page<Invoice> findByUploadedById(Long userId, Pageable pageable);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    @Query("""
        SELECT i FROM Invoice i
        WHERE (:status IS NULL OR i.status = :status)
        AND (:vendorName IS NULL OR LOWER(i.vendorName) LIKE LOWER(CONCAT('%', :vendorName, '%')))
        AND (:startDate IS NULL OR i.invoiceDate >= :startDate)
        AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
        AND (:minAmount IS NULL OR i.totalAmount >= :minAmount)
        AND (:maxAmount IS NULL OR i.totalAmount <= :maxAmount)
        ORDER BY i.createdAt DESC
        """)
    Page<Invoice> searchInvoices(
        @Param("status") InvoiceStatus status,
        @Param("vendorName") String vendorName,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("minAmount") BigDecimal minAmount,
        @Param("maxAmount") BigDecimal maxAmount,
        Pageable pageable);

    @Query("""
        SELECT COUNT(i) FROM Invoice i
        WHERE i.status = :status
        """)
    long countByStatus(@Param("status") InvoiceStatus status);

    @Query("""
        SELECT SUM(i.totalAmount) FROM Invoice i
        WHERE i.status = :status
        """)
    BigDecimal sumTotalAmountByStatus(@Param("status") InvoiceStatus status);

    @Query("""
        SELECT COUNT(i) FROM Invoice i
        WHERE i.createdAt >= :since
        """)
    long countSince(@Param("since") LocalDateTime since);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.invoiceNumber = :invoiceNumber
        AND i.vendorName = :vendorName
        AND i.totalAmount = :totalAmount
        AND i.id != :excludeId
        """)
    List<Invoice> findPotentialDuplicates(
        @Param("invoiceNumber") String invoiceNumber,
        @Param("vendorName") String vendorName,
        @Param("totalAmount") BigDecimal totalAmount,
        @Param("excludeId") Long excludeId);

    @Query("""
        SELECT MONTH(i.invoiceDate) as month,
               YEAR(i.invoiceDate) as year,
               COUNT(i) as count,
               SUM(i.totalAmount) as totalAmount
        FROM Invoice i
        WHERE i.invoiceDate >= :startDate
        GROUP BY YEAR(i.invoiceDate), MONTH(i.invoiceDate)
        ORDER BY YEAR(i.invoiceDate) ASC, MONTH(i.invoiceDate) ASC
        """)
    List<Object[]> getMonthlyStats(@Param("startDate") LocalDate startDate);

    @Query("""
        SELECT i.vendorName, COUNT(i) as count, SUM(i.totalAmount) as total
        FROM Invoice i
        GROUP BY i.vendorName
        ORDER BY total DESC
        """)
    List<Object[]> getTopVendors(Pageable pageable);

    @Query("""
        SELECT AVG(i.ocrConfidence) FROM Invoice i
        WHERE i.ocrConfidence IS NOT NULL
        """)
    Double getAverageOcrConfidence();

    List<Invoice> findByUploadedByIdAndStatus(Long userId, InvoiceStatus status);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.uploadedBy.id = :userId
        AND i.createdAt >= :since
        ORDER BY i.createdAt DESC
        """)
    List<Invoice> findRecentByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
