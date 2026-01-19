package com.screenleads.backend.app.domain.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.InvoiceItem;
import com.screenleads.backend.app.domain.model.InvoiceItemType;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    List<InvoiceItem> findByPromotionId(Long promotionId);

    List<InvoiceItem> findByItemType(InvoiceItemType itemType);

    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.invoice.id = :invoiceId AND ii.itemType = :itemType")
    List<InvoiceItem> findByInvoiceAndItemType(@Param("invoiceId") Long invoiceId,
            @Param("itemType") InvoiceItemType itemType);
}
