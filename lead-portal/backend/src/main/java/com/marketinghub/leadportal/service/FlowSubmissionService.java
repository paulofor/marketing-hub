package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.FlowSubmissionRequest;
import com.marketinghub.leadportal.exception.FlowSubmissionNotFoundException;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.model.FlowSubmission;
import com.marketinghub.leadportal.storage.FileStorageService;
import com.marketinghub.leadportal.storage.FlowSubmissionStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FlowSubmissionService {

    private final FlowService flowService;
    private final FlowSubmissionStorage storage;
    private final FileStorageService fileStorageService;
    private final Map<UUID, FlowSubmission> submissions;
    private final Object writeLock = new Object();

    public FlowSubmissionService(
            FlowService flowService, FlowSubmissionStorage storage, FileStorageService fileStorageService) {
        this.flowService = flowService;
        this.storage = storage;
        this.fileStorageService = fileStorageService;
        this.submissions = new ConcurrentHashMap<>(storage.loadAll());
    }

    public FlowSubmission create(String slug, FlowSubmissionRequest request, MultipartFile imageFile) {
        Flow flow = flowService.get(slug);
        validateRequiredQuestions(flow, request, imageFile);

        UUID id = UUID.randomUUID();
        String storedFileName = null;
        String originalFileName = null;
        String contentType = null;

        if (imageFile != null && !imageFile.isEmpty()) {
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
                Instant.now());

        synchronized (writeLock) {
            submissions.put(id, submission);
            persist();
        }

        return submission;
    }

    public FlowSubmission get(UUID id) {
        FlowSubmission submission = submissions.get(id);
        if (submission == null) {
            throw new FlowSubmissionNotFoundException(id);
        }
        return submission;
    }

    public Resource loadImage(String storedFileName) {
        return fileStorageService.loadAsResource(storedFileName);
    }

    private void persist() {
        storage.saveAll(new ArrayList<>(submissions.values()));
    }

    private void validateRequiredQuestions(Flow flow, FlowSubmissionRequest request, MultipartFile imageFile) {
        List<FlowQuestion> questions = flow.questions() == null ? List.of() : flow.questions();
        boolean hasImage = imageFile != null && !imageFile.isEmpty();
        for (FlowQuestion question : questions) {
            if (question.type() == FlowQuestionType.IMAGE_UPLOAD) {
                if (question.required()
                        && (request.getImageKey() == null
                                || !question.dataKey().equals(request.getImageKey())
                                || !hasImage)) {
                    throw new IllegalArgumentException("Envie uma imagem para continuar.");
                }
                continue;
            }

            Object value = request.getAnswers().get(question.dataKey());
            if (question.type() == FlowQuestionType.MULTIPLE_CHOICE) {
                List<?> options = value instanceof List<?> list ? list : List.of();
                if (question.required() && options.isEmpty()) {
                    throw new IllegalArgumentException("Selecione ao menos uma opção em " + question.title());
                }
                continue;
            }

            String stringValue = value == null ? "" : value.toString().trim();
            boolean shouldRequire = question.required() || question.type() == FlowQuestionType.EMAIL;
            if (shouldRequire && stringValue.isEmpty()) {
                throw new IllegalArgumentException("Preencha o campo " + question.title());
            }
        }

        if (request.getImageKey() != null) {
            boolean matchesFlowQuestion = questions.stream()
                    .anyMatch(q -> q.dataKey().equals(request.getImageKey()) && q.type() == FlowQuestionType.IMAGE_UPLOAD);
            if (!matchesFlowQuestion) {
                throw new IllegalArgumentException("O campo de imagem enviado não pertence a este fluxo.");
            }
        }
    }

    private Map<String, Object> sanitizeAnswers(Flow flow, FlowSubmissionRequest request) {
        Map<String, Object> sanitized = new LinkedHashMap<>();

        List<FlowQuestion> questions = flow.questions() == null ? List.of() : flow.questions();
        for (FlowQuestion question : questions) {
            if (question.type() == FlowQuestionType.IMAGE_UPLOAD) {
                continue;
            }
            Object raw = request.getAnswers().get(question.dataKey());
            if (raw == null) {
                continue;
            }

            if (question.type() == FlowQuestionType.MULTIPLE_CHOICE) {
                List<?> rawList = raw instanceof List<?> list ? list : List.of(raw);
                List<String> filtered = rawList.stream()
                        .map(Object::toString)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList();
                sanitized.put(question.dataKey(), filtered);
            } else {
                String cleaned = raw.toString().trim();
                if (!cleaned.isEmpty()) {
                    sanitized.put(question.dataKey(), cleaned);
                }
            }
        }

        return sanitized;
    }
}
