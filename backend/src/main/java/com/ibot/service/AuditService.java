package com.ibot.service;

import com.ibot.entity.AuditLog;
import com.ibot.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String entityType, Long entityId, String action,
                    String oldValue, String newValue, Long userId, String userName) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .userId(userId)
                .userName(userName)
                .build();
        auditLogRepository.save(auditLog);
    }

    @Async
    public void logInvoiceAction(Long invoiceId, String action, String description,
                                  Long userId, String userName) {
        AuditLog auditLog = AuditLog.builder()
                .entityType("INVOICE")
                .entityId(invoiceId)
                .action(action)
                .description(description)
                .userId(userId)
                .userName(userName)
                .build();
        auditLogRepository.save(auditLog);
    }
}
