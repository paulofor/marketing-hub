package com.marketinghub.worker.sampleemail;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.sampleemail.SampleEmail;
import com.marketinghub.sampleemail.dto.CreateSampleEmailRequest;
import com.marketinghub.sampleemail.service.SampleEmailService;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço que coordena a geração dos e-mails de envio de amostras pelo worker IA.
 */
@Service
public class ExperimentSampleEmailService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentSampleEmailService.class);

    private final ExperimentGenerationRepository generationRepository;
    private final ExperimentSampleEmailChatGptClient chatGptClient;
    private final SampleEmailService sampleEmailService;
    private final ExperimentRepository experimentRepository;

    public ExperimentSampleEmailService(ExperimentGenerationRepository generationRepository,
                                        ExperimentSampleEmailChatGptClient chatGptClient,
                                        SampleEmailService sampleEmailService,
                                        ExperimentRepository experimentRepository) {
        this.generationRepository = generationRepository;
        this.chatGptClient = chatGptClient;
        this.sampleEmailService = sampleEmailService;
        this.experimentRepository = experimentRepository;
    }

    @Transactional
    public Map<Long, List<SampleEmail>> generate() {
        Map<Long, List<SampleEmail>> result = new LinkedHashMap<>();
        List<Experiment> experiments = generationRepository.findAllToGenerateSampleEmails();
        for (Experiment experiment : experiments) {
            Integer quantity = experiment.getSampleEmailsToGenerate();
            if (quantity == null || quantity <= 0) {
                log.debug("Ignorando experimento {} sem solicitação de e-mails de amostra", experiment.getId());
                continue;
            }
            log.info("Gerando {} e-mails de amostra para o experimento {}", quantity, experiment.getId());
            try {
                ExperimentSampleEmailChatGptClient.Generation generation =
                        chatGptClient.generateSampleEmails(experiment, quantity);
                List<ExperimentSampleEmailChatGptClient.SampleEmailPlan> plans = generation.plans();
                if (plans.isEmpty()) {
                    log.warn("ChatGPT não retornou e-mails de amostra para o experimento {}", experiment.getId());
                    continue;
                }
                List<SampleEmail> savedEmails = new ArrayList<>();
                int processed = 0;
                for (ExperimentSampleEmailChatGptClient.SampleEmailPlan plan : plans) {
                    if (processed >= quantity) {
                        break;
                    }
                    if (plan == null || !StringUtils.hasText(plan.subject()) || !StringUtils.hasText(plan.body())) {
                        log.debug("Plano de e-mail de amostra inválido ignorado para experimento {}", experiment.getId());
                        continue;
                    }
                    CreateSampleEmailRequest request = new CreateSampleEmailRequest();
                    request.setSubject(plan.subject());
                    request.setPreviewText(plan.previewText());
                    request.setBody(plan.body());
                    request.setCallToAction(plan.callToAction());
                    request.setModel(generation.model());
                    request.setPrompt(generation.auditTrail());
                    SampleEmail saved = sampleEmailService.create(experiment, request);
                    savedEmails.add(saved);
                    processed++;
                }
                experiment.setSampleEmailsToGenerate(0);
                experimentRepository.save(experiment);
                result.put(experiment.getId(), savedEmails);
                log.info("{} e-mails de amostra salvos para o experimento {}", savedEmails.size(), experiment.getId());
            } catch (Exception ex) {
                log.error("Falha ao gerar e-mails de amostra para o experimento {}", experiment.getId(), ex);
            }
        }
        return result;
    }
}
