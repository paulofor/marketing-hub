package com.marketinghub.leadportal.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurações de segurança para pacotes de imagem do Lead Portal que ficam presos em PROCESSING.
 */
@Component
@ConfigurationProperties(prefix = "lead-portal.worker.processing-guard")
@Getter
@Setter
public class LeadPortalProcessingGuardProperties {

    /**
     * Ativa/desativa a rotina automática de recuperação de pacotes presos em PROCESSING.
     */
    private boolean enabled = true;

    /**
     * Tempo máximo permitido com status PROCESSING antes de reabrir ou falhar o pacote.
     */
    private Duration timeout = Duration.ofMinutes(30);

    /**
     * Quantidade máxima de vezes que permitimos que o pacote entre em PROCESSING
     * antes de marcá-lo como FAILED por timeout.
     */
    private int maxAttempts = 2;

    /**
     * Limite de pacotes processados por execução do scheduler.
     */
    private int batchSize = 20;
}
