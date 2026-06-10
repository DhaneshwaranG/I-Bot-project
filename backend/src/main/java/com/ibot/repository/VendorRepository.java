package com.ibot.repository;

import com.ibot.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByName(String name);
    Optional<Vendor> findByVendorCode(String vendorCode);
    boolean existsByName(String name);
    boolean existsByGstNumber(String gstNumber);

    @Query("""
        SELECT v FROM Vendor v
        WHERE :search IS NULL
           OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(v.gstNumber) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<Vendor> searchVendors(@Param("search") String search, Pageable pageable);

    List<Vendor> findByActiveTrue();
}
