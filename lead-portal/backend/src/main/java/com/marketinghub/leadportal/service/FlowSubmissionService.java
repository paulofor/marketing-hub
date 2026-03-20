package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.FlowSubmissionRequest;
import com.marketinghub.leadportal.exception.FlowSubmissionNotFoundException;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.model.FlowSubmission;
import com.marketinghub.leadportal.model.FlowImagePrompt;
import com.marketinghub.leadportal.repository.FlowSubmissionRepository;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageRepository;
import com.marketinghub.leadportal.storage.FileStorageService;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FlowSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(FlowSubmissionService.class);
    private static final Set<String> SIMPLE_FORM_METADATA_KEYS = Set.of(
            "cabecalho_titulo",
            "cabecalho_subtitulo",
            "cabecalho_promessa",
            "exemplos_reais_titulo",
            "exemplos_reais_subtitulo",
            "exemplo_real_card_1_titulo",
            "exemplo_real_card_1_imagem_url",
            "exemplo_real_card_1_texto_sobreposto",
            "exemplo_real_card_1_subtitulo",
            "exemplo_real_card_2_titulo",
            "exemplo_real_card_2_imagem_url",
            "exemplo_real_card_2_texto_sobreposto",
            "exemplo_real_card_2_subtitulo",
            "exemplo_real_card_3_titulo",
            "exemplo_real_card_3_imagem_url",
            "exemplo_real_card_3_texto_sobreposto",
            "exemplo_real_card_3_subtitulo",
            "bullets_titulo",
            "bullet_item_1",
            "bullet_item_2",
            "bullet_item_3");
    private static final Set<String> OPTIONAL_SIMPLE_FORM_KEYS = Set.of("local_trabalho", "academia_ou_studio");

    private final FlowService flowService;
    private final FlowSubmissionRepository repository;
    private final FlowSubmissionImagePackageRepository imagePackageRepository;
    private final FileStorageService fileStorageService;
    private final FlowSubmissionImagePackageStatusHistoryService statusHistoryService;
    private final FlowImagePromptService imagePromptService;
    private final ExperimentFunnelTrackingClient trackingClient;

    public FlowSubmissionService(
            FlowService flowService,
            FlowSubmissionRepository repository,
            FlowSubmissionImagePackageRepository imagePackageRepository,
            FileStorageService fileStorageService,
            FlowSubmissionImagePackageStatusHistoryService statusHistoryService,
            FlowImagePromptService imagePromptService,
            ExperimentFunnelTrackingClient trackingClient) {
        this.flowService = flowService;
        this.repository = repository;
        this.imagePackageRepository = imagePackageRepository;
        this.fileStorageService = fileStorageService;
        this.statusHistoryService = statusHistoryService;
        this.imagePromptService = imagePromptService;
        this.trackingClient = trackingClient;
    }

    public FlowSubmission create(String slug, FlowSubmissionRequest request, MultipartFile imageFile) {
        Flow flow = flowService.get(slug);
        validateRequiredQuestions(flow, request, imageFile);

        UUID id = UUID.randomUUID();
        String storedFileName = null;
        String originalFileName = null;
        String contentType = null;
        boolean hasImage = imageFile != null && !imageFile.isEmpty();

        if (hasImage) {
            storedFileName = fileStorageService.store(imageFile, id.toString());
            originalFileName = imageFile.getOriginalFilename();
            contentType = imageFile.getContentType();
        }

        Map<String, Object> sanitizedAnswers = sanitizeAnswers(flow, request);

        FlowSubmission submission = new FlowSubmission(
                id,
                slug,
                request.getName(),
                request.getEmail(),
                sanitizedAnswers,
                request.getImageKey(),
                storedFileName,
                originalFileName,
                contentType,
                Instant.now(),
                normalizeCampaignCode(request.getCampaignCode()));

        repository.save(com.marketinghub.leadportal.entity.FlowSubmissionEntity.fromModel(submission));
        registerImagePackage(flow, submission, hasImage);
        notifyExperimentFunnel(flow, submission);
        return submission;
    }

    public FlowSubmission get(UUID id) {
        return repository
                .findById(id)
                .map(com.marketinghub.leadportal.entity.FlowSubmissionEntity::toModel)
                .orElseThrow(() -> new FlowSubmissionNotFoundException(id));
    }

    public Resource loadImage(String storedFileName) {
        return fileStorageService.loadAsResource(storedFileName);
    }

    public Optional<String> resolvePublicUrl(String storedFileName) {
        return fileStorageService.resolvePublicUrl(storedFileName);
    }

    private void validateRequiredQuestions(Flow flow, FlowSubmissionRequest request, MultipartFile imageFile) {
        List<FlowQuestion> questions = flow.questions() == null ? List.of() : flow.questions();
        boolean hasImage = imageFile != null && !imageFile.isEmpty();

        enforceImageRequirements(questions, request, hasImage);

        if (hasCustomTemplate(flow)) {
            validateImageKeyBelongsToFlow(request, questions);
            return;
        }

        for (FlowQuestion question : questions) {
            FlowQuestionType questionType = question.type();
            if (questionType == null) {
                log.warn(
                        "Flow '{}' has question '{}' without type; skipping validation",
                        flow.slug(),
                        question.dataKey());
                continue;
            }

            if (isSimpleFormMetadataQuestion(question)) {
                continue;
            }

            if (questionType == FlowQuestionType.IMAGE_UPLOAD) {
                continue;
            }

            Object value = request.getAnswers().get(question.dataKey());
            if (questionType == FlowQuestionType.MULTIPLE_CHOICE) {
                List<?> options = value instanceof List<?> list ? list : List.of();
                if (question.required() && options.isEmpty()) {
                    throw new IllegalArgumentException("Selecione ao menos uma opção em " + question.title());
                }
                continue;
            }

            String stringValue = value == null ? "" : value.toString().trim();
            boolean shouldRequire = (question.required() || questionType == FlowQuestionType.EMAIL)
                    && !isOptionalSimpleFormField(question);
            if (shouldRequire && stringValue.isEmpty()) {
                throw new IllegalArgumentException("Preencha o campo " + question.title());
            }
        }

        validateImageKeyBelongsToFlow(request, questions);
    }

    private void enforceImageRequirements(List<FlowQuestion> questions, FlowSubmissionRequest request, boolean hasImage) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        for (FlowQuestion question : questions) {
            if (question == null || question.type() != FlowQuestionType.IMAGE_UPLOAD || !question.required()) {
                continue;
            }
            boolean matchesDataKey = question.dataKey() != null && question.dataKey().equals(request.getImageKey());
            if (!matchesDataKey || !hasImage) {
                throw new IllegalArgumentException("Envie uma imagem para continuar.");
            }
        }
    }

    private void validateImageKeyBelongsToFlow(FlowSubmissionRequest request, List<FlowQuestion> questions) {
        if (request.getImageKey() == null || questions == null) {
            return;
        }
        boolean matchesFlowQuestion = questions.stream()
                .filter(question -> question != null && question.dataKey() != null)
                .anyMatch(q -> q.dataKey().equals(request.getImageKey()) && q.type() == FlowQuestionType.IMAGE_UPLOAD);
        if (!matchesFlowQuestion) {
            throw new IllegalArgumentException("O campo de imagem enviado não pertence a este fluxo.");
        }
    }

    private boolean hasCustomTemplate(Flow flow) {
        return flow != null && StringUtils.hasText(flow.customFormHtml());
    }

    private boolean isSimpleFormMetadataQuestion(FlowQuestion question) {
        if (question == null || question.dataKey() == null) {
            return false;
        }
        return SIMPLE_FORM_METADATA_KEYS.contains(question.dataKey());
    }

    private boolean isOptionalSimpleFormField(FlowQuestion question) {
        if (question == null || question.dataKey() == null) {
            return false;
        }
        return OPTIONAL_SIMPLE_FORM_KEYS.contains(question.dataKey());
    }

    private void notifyExperimentFunnel(Flow flow, FlowSubmission submission) {
        if (trackingClient == null || flow == null || submission == null) {
            return;
        }
        try {
            ExperimentFunnelTrackingClient.TrackingResult result = trackingClient.registerSubmission(
                    flow.slug(), submission.id(), submission.createdAt());
            if (result == ExperimentFunnelTrackingClient.TrackingResult.FAILED) {
                log.warn("Falha ao reenviar submission {} do fluxo {} para o Marketing Hub",
                        submission.id(), flow.slug());
            } else if (result == ExperimentFunnelTrackingClient.TrackingResult.FORWARDED) {
                log.debug("Submission {} do fluxo {} encaminhada ao Marketing Hub", submission.id(), flow.slug());
            }
        } catch (RuntimeException ex) {
            log.warn("Erro ao reenviar submission {} do fluxo {} ao Marketing Hub", submission.id(), flow.slug(), ex);
        }
    }

    private void registerImagePackage(Flow flow, FlowSubmission submission, boolean hasImage) {
        Optional<FlowImagePrompt> promptSpec = imagePromptService.buildPrompt(flow, submission);
        if (promptSpec.isEmpty() && !hasImage) {
            log.info("Skipping image package for submission {} in flow {} because there is no prompt/template.",
                    submission.id(), flow.slug());
            return;
        }

        FlowImagePrompt spec = promptSpec.orElseGet(() -> new FlowImagePrompt(
                Optional.ofNullable(flow.prompt()).orElse(""),
                flow.model(),
                null,
                null));

        FlowSubmissionImagePackageEntity imagePackage = new FlowSubmissionImagePackageEntity();
        imagePackage.setSubmissionId(submission.id());
        imagePackage.setStatus(FlowSubmissionImagePackageEntity.Status.RECENT.name());
        if (StringUtils.hasText(spec.model())) {
            imagePackage.setModel(spec.model());
        } else {
            imagePackage.setModel(flow.model());
        }
        imagePackage.setPrompt(spec.prompt());
        if (spec.plannedOutputs() != null) {
            imagePackage.setPlannedOutputs(spec.plannedOutputs());
        }
        if (spec.freeImages() != null) {
            imagePackage.setFreeImages(spec.freeImages());
        }

        FlowSubmissionImagePackageEntity savedPackage = imagePackageRepository.save(imagePackage);
        statusHistoryService.recordStatusChange(savedPackage.getId(), FlowSubmissionImagePackageEntity.Status.RECENT, null);
    }

    private String normalizeCampaignCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 190 ? trimmed.substring(0, 190) : trimmed;
    }

    private Map<String, Object> sanitizeAnswers(Flow flow, FlowSubmissionRequest request) {
        Map<String, Object> sanitized = new LinkedHashMap<>();

        Map<String, FlowQuestion> questionIndex = new LinkedHashMap<>();
        List<FlowQuestion> questions = flow.questions() == null ? List.of() : flow.questions();
        for (FlowQuestion question : questions) {
            if (question == null || question.dataKey() == null) {
                continue;
            }
            questionIndex.putIfAbsent(question.dataKey(), question);
        }

        request.getAnswers().forEach((key, raw) -> {
            if (key == null) {
                return;
            }
            FlowQuestion question = questionIndex.get(key);
            if (question == null || question.type() == null) {
                Object generic = sanitizeGenericAnswer(raw);
                if (generic != null) {
                    sanitized.put(key, generic);
                }
                return;
            }

            FlowQuestionType questionType = question.type();
            if (questionType == FlowQuestionType.IMAGE_UPLOAD) {
                return;
            }
            if (questionType == FlowQuestionType.MULTIPLE_CHOICE) {
                List<String> filtered = sanitizeAsList(raw);
                if (!filtered.isEmpty()) {
                    sanitized.put(key, filtered);
                }
            } else {
                String cleaned = sanitizeText(raw);
                if (cleaned != null) {
                    sanitized.put(key, cleaned);
                }
            }
        });

        return sanitized;
    }

    private Object sanitizeGenericAnswer(Object raw) {
        if (raw instanceof Object[] array) {
            List<String> values = sanitizeAsList(Arrays.asList(array));
            return values.isEmpty() ? null : values;
        }
        if (raw instanceof Collection<?>) {
            List<String> values = sanitizeAsList(raw);
            return values.isEmpty() ? null : values;
        }
        return sanitizeText(raw);
    }

    private List<String> sanitizeAsList(Object raw) {
        Collection<?> source;
        if (raw instanceof Collection<?> collection) {
            source = collection;
        } else {
            source = Collections.singletonList(raw);
        }

        return source.stream()
                .map(this::sanitizeText)
                .filter(value -> value != null && !value.isEmpty())
                .toList();
    }

    private String sanitizeText(Object raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.toString().trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

}
