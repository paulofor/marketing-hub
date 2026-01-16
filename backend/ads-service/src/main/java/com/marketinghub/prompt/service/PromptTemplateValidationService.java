package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptDomainObjectType;
import com.marketinghub.prompt.dto.PromptTemplateValidationRequest;
import com.marketinghub.prompt.dto.PromptTemplateValidationResponse;
import jakarta.persistence.EntityNotFoundException;
import freemarker.core.InvalidReferenceException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class PromptTemplateValidationService {
    private final Configuration configuration;
    private final PromptDomainService promptDomainService;
    private final PromptDomainContextFactory contextFactory;

    public PromptTemplateValidationService(PromptDomainService promptDomainService,
                                           PromptDomainContextFactory contextFactory) {
        this.promptDomainService = promptDomainService;
        this.contextFactory = contextFactory;
        this.configuration = new Configuration(Configuration.VERSION_2_3_32);
        this.configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.configuration.setLogTemplateExceptions(false);
        this.configuration.setFallbackOnNullLoopVariable(false);
        this.configuration.setWrapUncheckedExceptions(true);
    }

    public PromptTemplateValidationResponse validate(PromptTemplateValidationRequest request) {
        if (request == null || request.getTemplate() == null || request.getTemplate().isBlank()) {
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("O template não pode estar vazio.")
                    .missingVariables(List.of())
                    .availableVariables(List.of())
                    .build();
        }

        Map<String, Object> context;
        List<String> availableVariables;
        try {
            List<PromptDomainObjectType> objects = promptDomainService.getObjectTypes(request.getDomain());
            context = contextFactory.buildSampleContext(objects);
            availableVariables = contextFactory.availableVariables(objects);
        } catch (EntityNotFoundException ex) {
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("Domínio não encontrado para validação.")
                    .missingVariables(List.of())
                    .availableVariables(List.of())
                    .build();
        }

        if (context.isEmpty()) {
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("Domínio não possui objetos configurados para validação.")
                    .missingVariables(List.of())
                    .availableVariables(availableVariables)
                    .build();
        }

        try {
            Template template = new Template("prompt-template-validation", new StringReader(request.getTemplate()), configuration);
            template.process(context, new StringWriter());
            return PromptTemplateValidationResponse.builder()
                    .valid(true)
                    .message("Template válido.")
                    .missingVariables(List.of())
                    .availableVariables(availableVariables)
                    .build();
        } catch (InvalidReferenceException invalidReferenceException) {
            String missing = sanitizeExpression(invalidReferenceException.getBlamedExpressionString());
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("Variável ausente ou inválida no template.")
                    .missingVariables(missing == null ? List.of() : List.of(missing))
                    .availableVariables(availableVariables)
                    .build();
        } catch (IOException | TemplateException exception) {
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("Erro de sintaxe no template: " + exception.getMessage())
                    .missingVariables(List.of())
                    .availableVariables(availableVariables)
                    .build();
        }
    }

    private String sanitizeExpression(String expression) {
        if (expression == null) {
            return null;
        }
        return expression.replace("${", "").replace("}", "").trim();
    }
}
