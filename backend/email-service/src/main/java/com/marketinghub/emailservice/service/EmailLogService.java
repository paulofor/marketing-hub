package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.model.EmailLog;
import com.marketinghub.emailservice.model.EmailStatus;
import com.marketinghub.emailservice.repository.EmailLogRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailLogService {

    private final EmailLogRepository repository;

    public EmailLogService(EmailLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EmailLog createPendingLog(String recipients, String subject, String templateId) {
        EmailLog emailLog = new EmailLog();
        emailLog.setRequestId(UUID.randomUUID().toString());
        emailLog.setRecipients(recipients);
        emailLog.setSubject(subject);
        emailLog.setTemplateId(templateId);
        emailLog.setStatus(EmailStatus.PENDING);
        emailLog.setCreatedAt(Instant.now());
        return repository.save(emailLog);
    }

    @Transactional
    public EmailLog markSent(String requestId) {
        EmailLog emailLog = repository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Requisição " + requestId + " não localizada"));
        emailLog.setStatus(EmailStatus.SENT);
        emailLog.setSentAt(Instant.now());
        emailLog.setErrorMessage(null);
        return repository.save(emailLog);
    }

    @Transactional
    public EmailLog markFailed(String requestId, String reason) {
        EmailLog emailLog = repository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Requisição " + requestId + " não localizada"));
        emailLog.setStatus(EmailStatus.FAILED);
        emailLog.setErrorMessage(reason);
        emailLog.setSentAt(null);
        return repository.save(emailLog);
    }

    @Transactional(readOnly = true)
    public Optional<EmailLog> findByRequestId(String requestId) {
        return repository.findByRequestId(requestId);
    }
}
