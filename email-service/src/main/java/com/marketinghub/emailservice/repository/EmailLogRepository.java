package com.marketinghub.emailservice.repository;

import com.marketinghub.emailservice.model.EmailLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Optional<EmailLog> findByRequestId(String requestId);
}
