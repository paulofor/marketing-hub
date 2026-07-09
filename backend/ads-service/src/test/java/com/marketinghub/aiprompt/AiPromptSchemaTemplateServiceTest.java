package com.marketinghub.aiprompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.aiprompt.service.AiPromptSchemaTemplateService;
import com.marketinghub.aiprompt.service.alterar.UpdateAiPromptSchemaTemplateRequest;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar regras administrativas dos templates operacionais de IA. */
@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-aiprompt;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
@org.springframework.transaction.annotation.Transactional
class AiPromptSchemaTemplateServiceTest {
    @Autowired
    AiPromptSchemaTemplateRepository repository;

    @Autowired
    ObjectMapper objectMapper;

    /** Deve ativar apenas um template por etapa do pipeline. */
    @Test
    void activateShouldDeactivateOtherTemplatesForSameStage() {
        AiPromptSchemaTemplate first = saveTemplate("gera-sales-page-v1:sales-page-html:v1", "v1", true);
        AiPromptSchemaTemplate second = saveTemplate("gera-sales-page-v1:sales-page-html:v2", "v2", false);
        AiPromptSchemaTemplateService service = new AiPromptSchemaTemplateService(repository, objectMapper);

        service.activate(second.getTemplateKey());

        assertThat(repository.findById(first.getTemplateKey())).get().extracting(AiPromptSchemaTemplate::isActive).isEqualTo(false);
        assertThat(repository.findById(second.getTemplateKey())).get().extracting(AiPromptSchemaTemplate::isActive).isEqualTo(true);
    }

    /** Deve bloquear JSON schema inválido antes de persistir alteração operacional. */
    @Test
    void updateShouldRejectInvalidSchemaJson() {
        AiPromptSchemaTemplate template = saveTemplate("gera-sales-page-v1:sales-page-html:v1", "v1", true);
        AiPromptSchemaTemplateService service = new AiPromptSchemaTemplateService(repository, objectMapper);

        UpdateAiPromptSchemaTemplateRequest request = new UpdateAiPromptSchemaTemplateRequest(
                "v1", "gpt-5.5", "sales_page_html", "Prompt", "{invalid", true);

        assertThatThrownBy(() -> service.update(template.getTemplateKey(), request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("schemaJson must be valid JSON");
    }

    /** Persiste um template mínimo para os cenários de teste. */
    private AiPromptSchemaTemplate saveTemplate(String key, String version, boolean active) {
        Instant now = Instant.now();
        return repository.save(AiPromptSchemaTemplate.builder()
                .templateKey(key)
                .pipelineCode("gera-sales-page-v1")
                .stageCode("sales-page-html")
                .version(version)
                .openAiModel("gpt-5.5")
                .schemaName("sales_page_html")
                .promptMarkdownContent("Prompt")
                .schemaJson("{\"type\":\"object\"}")
                .active(active)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
