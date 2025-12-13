package com.marketinghub.microservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.TestBootApplication;
import com.marketinghub.microservice.Microservice;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionRequest;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionSummary;
import com.marketinghub.microservice.exception.repository.MicroserviceExceptionRepository;
import com.marketinghub.microservice.exception.service.MicroserviceExceptionService;
import com.marketinghub.microservice.exception.MicroserviceExceptionLog;
import com.marketinghub.microservice.repository.MicroserviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestBootApplication.class)
@ActiveProfiles("test")
class MicroserviceExceptionServiceTest {

    @Autowired
    private MicroserviceExceptionRepository exceptionRepository;

    @Autowired
    private MicroserviceRepository microserviceRepository;

    private MicroserviceExceptionService service;

    @BeforeEach
    void setUp() {
        service = new MicroserviceExceptionService(exceptionRepository, microserviceRepository, new ObjectMapper());
    }

    @Test
    void shouldLogAndListExceptions() {
        Microservice microservice = microserviceRepository.save(Microservice.builder()
                .name("Background Worker")
                .status("ACTIVE")
                .build());

        MicroserviceExceptionRequest request = new MicroserviceExceptionRequest(
                "java.lang.IllegalStateException",
                "Falha ao processar fila",
                "stack",
                "error",
                "1.0.0",
                "worker-1",
                Map.of("jobId", "123"),
                Instant.parse("2024-03-20T10:15:30Z")
        );

        MicroserviceExceptionLog persisted = service.logException(microservice.getId(), request);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getSeverity()).isEqualTo("ERROR");
        assertThat(persisted.getOccurredAt()).isEqualTo(Instant.parse("2024-03-20T10:15:30Z"));

        var page = service.list(microservice.getId(), null, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getMessage()).contains("Falha ao processar fila");
    }

    @Test
    void shouldSummarizeLatestException() {
        Microservice microservice = microserviceRepository.save(Microservice.builder()
                .name("AI Worker")
                .status("ACTIVE")
                .build());

        service.logException(microservice.getId(), new MicroserviceExceptionRequest(
                "java.lang.RuntimeException",
                "Primeira falha",
                null,
                "WARN",
                null,
                null,
                null,
                Instant.parse("2024-03-20T10:15:30Z")
        ));

        service.logException(microservice.getId(), new MicroserviceExceptionRequest(
                "java.lang.IllegalArgumentException",
                "Falha mais recente",
                null,
                "ERROR",
                null,
                null,
                null,
                Instant.parse("2024-03-22T08:00:00Z")
        ));

        Map<Long, MicroserviceExceptionSummary> summaries = service.summarizeByMicroservices(List.of(microservice));
        MicroserviceExceptionSummary summary = summaries.get(microservice.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalCount()).isEqualTo(2);
        assertThat(summary.getLastMessage()).contains("Falha mais recente");
        assertThat(summary.getLastSeverity()).isEqualTo("ERROR");
        assertThat(summary.getLastOccurredAt()).isEqualTo(Instant.parse("2024-03-22T08:00:00Z"));
    }
}
