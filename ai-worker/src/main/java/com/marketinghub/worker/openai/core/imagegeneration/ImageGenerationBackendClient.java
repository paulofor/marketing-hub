package com.marketinghub.worker.openai.core.imagegeneration;

import com.marketinghub.worker.creative.CreativeImageOptimizer;
import com.marketinghub.worker.frameworkimage.FrameworkImageBackendClient;
import com.marketinghub.worker.frameworkimage.FrameworkImageJobCompletionPayload;
import com.marketinghub.worker.frameworkimage.FrameworkImageJobDto;
import com.marketinghub.worker.frameworkimage.FrameworkImageJobStage;
import com.marketinghub.worker.frameworkimage.FrameworkImageStorageClient;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageBackendPort;
import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: integrar a etapa imagegeneration do core OpenAI aos endpoints de framework-image do backend. */
public class ImageGenerationBackendClient implements StageBackendPort<ImageGenerationInput, ImageGenerationOutput> {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationBackendClient.class);
    private static final String STAGE_CODE = "framework-image-generation";

    private final FrameworkImageBackendClient backendClient;
    private final FrameworkImageStorageClient storageClient;
    private final CreativeImageOptimizer imageOptimizer;
    private final WebClient webClient;
    private final ImageGenerationWorkerProperties properties;
    private final String workerId;

    /** Inicializa o adapter com clientes de backend, storage, otimização e propriedades da etapa. */
    public ImageGenerationBackendClient(
            FrameworkImageBackendClient backendClient,
            FrameworkImageStorageClient storageClient,
            CreativeImageOptimizer imageOptimizer,
            WebClient.Builder webClientBuilder,
            ImageGenerationWorkerProperties properties
    ) {
        this.backendClient = backendClient;
        this.storageClient = storageClient;
        this.imageOptimizer = imageOptimizer;
        this.webClient = webClientBuilder.build();
        this.properties = properties;
        this.workerId = resolveWorkerId(properties.workerId());
    }

    /** Busca jobs pendentes, aplica rollout e faz claim antes de devolver execuções ao worker genérico. */
    @Override
    public List<StageExecution<ImageGenerationInput>> listPending(int limit) {
        if (properties.rolloutPercentage() <= 0) {
            log.debug("ImageGeneration worker rollout disabled (rolloutPercentage={})", properties.rolloutPercentage());
            return List.of();
        }

        return backendClient.listPending(Math.max(1, limit)).stream()
                .filter(job -> job != null && job.id() != null)
                .filter(job -> isRolloutEligible(job.experimentId()))
                .map(this::claim)
                .filter(job -> job != null && job.id() != null)
                .map(this::toStageExecution)
                .toList();
    }

    /** Registra no backend que o request bruto foi despachado para a OpenAI pela etapa imagegeneration. */
    @Override
    public void markDispatched(StageExecution<ImageGenerationInput> execution, OpenAiDispatch dispatch) {
        UUID jobId = execution.input().job().id();
        backendClient.updateStage(jobId, FrameworkImageJobStage.SENT_TO_OPENAI_BATCH);
        backendClient.updateStage(jobId, FrameworkImageJobStage.WAITING_OPENAI_BATCH);
        log.info(
                "Envio para backend após despacho OpenAI [jobId={}, experimentId={}, payload={stage={}, openAiJobId={}, requestBodyJson={}}]",
                jobId,
                execution.aggregateId(),
                FrameworkImageJobStage.WAITING_OPENAI_BATCH,
                dispatch.openAiJobId(),
                dispatch.requestBodyJson()
        );
    }

    /** Publica a imagem validada no storage e conclui o job no backend com o payload contratual. */
    @Override
    public void markCompleted(StageExecution<ImageGenerationInput> execution, OpenAiResult<ImageGenerationOutput> result) {
        FrameworkImageJobDto job = execution.input().job();
        backendClient.updateStage(job.id(), FrameworkImageJobStage.OPENAI_IMAGE_READY);
        byte[] imageContent = resolveImageContent(result.parsedResponse(), job.id());
        CreativeImageOptimizer.OptimizedImage optimized = imageOptimizer.optimize(imageContent);
        FrameworkImageStorageClient.UploadedFrameworkImage uploaded = uploadWithRetry(optimized.content(), buildFilename(job));
        backendClient.updateStage(job.id(), FrameworkImageJobStage.UPLOADED_TO_CLOUDFLARE);

        FrameworkImageJobCompletionPayload completionPayload = new FrameworkImageJobCompletionPayload(
                FrameworkImageJobStage.NOTIFIED_BACKEND.name(),
                firstText(result.parsedResponse().model(), job.model(), properties.imageModel()),
                firstText(result.parsedResponse().prompt(), job.prompt()),
                result.openAiJobId(),
                job.assetId(),
                uploaded.publicUrl(),
                job.webUrl()
        );
        log.info(
                "Envio para backend concluindo imagegeneration [jobId={}, experimentId={}, payload={}]",
                job.id(),
                job.experimentId(),
                completionPayload
        );
        backendClient.complete(job.id(), completionPayload);
        log.info(
                "ImageGeneration job completed. jobId={}, experimentId={}, assetId={}, openAiJobId={}, objectKey={}",
                job.id(),
                job.experimentId(),
                job.assetId(),
                result.openAiJobId(),
                uploaded.objectKey()
        );
    }

    /** Envia ao backend os dados de falha quando a etapa imagegeneration não é concluída. */
    @Override
    public void markFailed(StageExecution<ImageGenerationInput> execution, Throwable error) {
        FrameworkImageJobDto job = execution.input().job();
        String reason = buildFailureReason(error);
        log.info(
                "Envio para backend falhando imagegeneration [jobId={}, experimentId={}, payload={errorMessage={}}]",
                job.id(),
                job.experimentId(),
                reason
        );
        backendClient.fail(job.id(), reason);
    }

    /** Faz claim do job no backend para evitar processamento concorrente por outro worker. */
    private FrameworkImageJobDto claim(FrameworkImageJobDto job) {
        FrameworkImageJobDto claimed = backendClient.claim(job.id(), workerId);
        if (claimed == null) {
            log.info("ImageGeneration job {} could not be claimed by worker {}", job.id(), workerId);
        }
        return claimed;
    }

    /** Converte o DTO do backend em execução padronizada do core OpenAI. */
    private StageExecution<ImageGenerationInput> toStageExecution(FrameworkImageJobDto job) {
        Instant requestedAt = job.createdAt() != null ? job.createdAt() : Instant.now();
        return new StageExecution<>(
                job.id().toString(),
                job.experimentId(),
                STAGE_CODE,
                job.status(),
                requestedAt,
                new ImageGenerationInput(job)
        );
    }

    /** Resolve os bytes da imagem usando o base64 retornado ou baixando a URL informada pela OpenAI. */
    private byte[] resolveImageContent(ImageGenerationOutput output, UUID jobId) {
        if (output.imageContent() != null && output.imageContent().length > 0) {
            return output.imageContent();
        }
        if (StringUtils.hasText(output.imageUrl())) {
            byte[] downloaded = webClient.get()
                    .uri(output.imageUrl())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(properties.timeout());
            if (downloaded != null && downloaded.length > 0) {
                return downloaded;
            }
        }
        throw new IllegalStateException("OpenAI did not provide image bytes for job " + jobId);
    }

    /** Faz upload com retentativas simples para reduzir falhas transitórias de storage. */
    private FrameworkImageStorageClient.UploadedFrameworkImage uploadWithRetry(byte[] content, String preferredName) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= properties.uploadAttempts(); attempt++) {
            try {
                return storageClient.upload(content, preferredName);
            } catch (RuntimeException error) {
                lastError = error;
                if (attempt >= properties.uploadAttempts()) {
                    log.error(
                            "ImageGeneration upload failed definitively on attempt {}/{} for preferredName={}",
                            attempt,
                            properties.uploadAttempts(),
                            preferredName,
                            error
                    );
                    break;
                }
                long backoffMillis = properties.uploadBackoff().toMillis() * attempt;
                log.warn(
                        "ImageGeneration upload failed on attempt {}/{} for preferredName={}: {}. Retrying in {}ms",
                        attempt,
                        properties.uploadAttempts(),
                        preferredName,
                        error.getMessage(),
                        backoffMillis,
                        error
                );
                sleep(backoffMillis);
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("ImageGeneration upload failed");
    }

    /** Aguarda antes de uma nova tentativa de upload preservando interrupção da thread. */
    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(50L, millis));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            log.error("ImageGeneration upload retry sleep interrupted. millis={}", millis, error);
            throw new RuntimeException("Interrupted while waiting to retry imagegeneration upload", error);
        }
    }

    /** Monta um nome de arquivo estável para o item planejado da landing. */
    private String buildFilename(FrameworkImageJobDto job) {
        String planningItemKey = StringUtils.hasText(job.planningItemKey()) ? job.planningItemKey() : "item";
        String normalized = planningItemKey.toLowerCase()
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-");
        return job.id() + "-" + normalized + ".jpg";
    }

    /** Resolve o identificador operacional deste worker para claim no backend. */
    private String resolveWorkerId(String configuredWorkerId) {
        if (configuredWorkerId != null && !configuredWorkerId.isBlank()) {
            return configuredWorkerId.trim();
        }
        try {
            return "imagegeneration-" + InetAddress.getLocalHost().getHostName();
        } catch (Exception error) {
            log.warn("Could not resolve hostname for imagegeneration workerId; using fallback", error);
            return "imagegeneration-worker";
        }
    }

    /** Gera uma mensagem curta de erro adequada para persistência no backend. */
    private String buildFailureReason(Throwable error) {
        String message = error != null ? error.getMessage() : null;
        if (message == null || message.isBlank()) {
            return "Falha desconhecida no processamento do job de imagem";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /** Retorna o primeiro texto preenchido entre os candidatos informados. */
    private String firstText(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** Verifica se o experimento pertence ao bucket de rollout configurado. */
    private boolean isRolloutEligible(Long experimentId) {
        if (properties.rolloutPercentage() >= 100) {
            return true;
        }
        long normalizedExperimentId = experimentId == null ? 0L : Math.abs(experimentId);
        long bucket = normalizedExperimentId % 100;
        boolean eligible = bucket < properties.rolloutPercentage();
        if (!eligible) {
            log.info(
                    "ImageGeneration rollout skipped experimentId={} workerId={} rolloutPercentage={}",
                    experimentId,
                    workerId,
                    properties.rolloutPercentage()
            );
        }
        return eligible;
    }
}
