package com.marketinghub.nichocnae.meiaudiencesegmenter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Valida a prontidão operacional da chave OpenAI antes da segmentação comportamental MEI/autônomo. */
@Component
public class MeiAudienceSegmenterOperationalGuard implements ApplicationRunner {
    static final String MODULE = "oprm-coletor-mei";
    static final String OPERATION = "mei-audience-segmenter";
    static final String DEFAULT_EXPECTED_VARIABLE = "OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY";
    static final String DEFAULT_FALLBACK_VARIABLE = "OPENAI_API_KEY";

    private static final Logger log = LoggerFactory.getLogger(MeiAudienceSegmenterOperationalGuard.class);

    private final MeiAudienceSegmenterOpenAiProperties properties;

    /** Inicializa a validação operacional com as propriedades OpenAI da etapa MEI. */
    public MeiAudienceSegmenterOperationalGuard(MeiAudienceSegmenterOpenAiProperties properties) {
        this.properties = properties;
    }

    /** Registra no startup se a etapa MEI possui chave configurada para execução com OpenAI. */
    @Override
    public void run(ApplicationArguments args) {
        if (isReadyForExecution()) {
            log.info(
                    "Configuração operacional presente (module={}, operation={}, expectedVariable={}, fallbackVariable={}, researchCycleId={})",
                    MODULE,
                    OPERATION,
                    expectedVariable(),
                    fallbackVariable(),
                    "n/a");
            return;
        }
        log.warn(
                "Configuração operacional ausente; pendências MEI não serão processadas (module={}, operation={}, expectedVariable={}, fallbackVariable={}, researchCycleId={})",
                MODULE,
                OPERATION,
                expectedVariable(),
                fallbackVariable(),
                "n/a");
    }

    /** Bloqueia a busca ou execução de pendências quando a chave OpenAI operacional não estiver configurada. */
    public void assertReadyForExecution(Long researchCycleId) {
        if (isReadyForExecution()) {
            return;
        }
        log.error(
                "Configuração operacional ausente; bloqueando pendências MEI (module={}, operation={}, expectedVariable={}, fallbackVariable={}, researchCycleId={})",
                MODULE,
                OPERATION,
                expectedVariable(),
                fallbackVariable(),
                researchCycleId == null ? "n/a" : researchCycleId);
        throw new MeiAudienceSegmenterOperationalException(missingApiKeyMessage(researchCycleId));
    }

    /** Informa se existe chave direta, fallback via OPENAI_API_KEY ou arquivo montado configurado. */
    public boolean isReadyForExecution() {
        return !resolveApiKeyForOperationalCheck().isBlank();
    }

    /** Resolve a chave apenas para verificação operacional, sem expor o valor em logs ou respostas. */
    String resolveApiKeyForOperationalCheck() {
        if (!properties.apiKey().isBlank()) {
            return properties.apiKey().trim();
        }
        if (properties.apiKeyFile().isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(properties.apiKeyFile())).trim();
        } catch (IOException ex) {
            log.error(
                    "Erro ao ler arquivo de chave OpenAI; etapa MEI ficará bloqueada (module={}, operation={}, expectedVariable={}, fallbackVariable={}, apiKeyFile={})",
                    MODULE,
                    OPERATION,
                    expectedVariable(),
                    fallbackVariable(),
                    properties.apiKeyFile(),
                    ex);
            throw new MeiAudienceSegmenterOperationalException(
                    "Falha operacional na etapa mei-audience-segmenter: arquivo de chave OpenAI configurado em "
                            + properties.apiKeyFile() + " não pôde ser lido.",
                    ex);
        }
    }

    /** Retorna a variável operacional esperada para a chave dedicada da etapa. */
    String expectedVariable() {
        return properties.expectedApiKeyVariable().isBlank()
                ? DEFAULT_EXPECTED_VARIABLE
                : properties.expectedApiKeyVariable();
    }

    /** Retorna a variável de fallback aceita quando a chave dedicada não estiver definida. */
    String fallbackVariable() {
        return properties.fallbackApiKeyVariable().isBlank()
                ? DEFAULT_FALLBACK_VARIABLE
                : properties.fallbackApiKeyVariable();
    }

    /** Monta uma mensagem operacional clara sem revelar segredos. */
    private String missingApiKeyMessage(Long researchCycleId) {
        String cycle = researchCycleId == null ? "n/a" : researchCycleId.toString();
        return "Falha operacional na etapa mei-audience-segmenter: variável " + expectedVariable()
                + " ausente e fallback " + fallbackVariable()
                + " indisponível. module=" + MODULE
                + ", operation=" + OPERATION
                + ", expectedVariable=" + expectedVariable()
                + ", fallbackVariable=" + fallbackVariable()
                + ", researchCycleId=" + cycle + ".";
    }
}
