package com.marketinghub.worker.salesvideo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.JobFailureRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orquestra o ciclo de vida dos jobs OpenAI vinculados ao módulo Avatar Sales Video.
 */
@Service
public class SalesVideoScriptJobService {
    private static final Logger log = LoggerFactory.getLogger(SalesVideoScriptJobService.class);

    private final SalesVideoBackendClient backendClient;
    private final SalesVideoOpenAiClient openAiClient;
    private final SalesVideoPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final int maxJobs;
    private final String workerId;
    private final AtomicBoolean warnedMissingKey = new AtomicBoolean(false);

    public SalesVideoScriptJobService(SalesVideoBackendClient backendClient,
                                      SalesVideoOpenAiClient openAiClient,
                                      SalesVideoPromptBuilder promptBuilder,
                                      ObjectMapper objectMapper,
                                      @Value("${sales-video.script.max-jobs:3}") int maxJobs,
                                      @Value("${sales-video.worker-id:sales-video-ai-worker}") String workerId) {
        this.backendClient = backendClient;
        this.openAiClient = openAiClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.maxJobs = Math.max(1, maxJobs);
        this.workerId = workerId;
    }

    public void processPendingScriptJobs() {
        if (!openAiClient.isEnabled()) {
            if (!warnedMissingKey.getAndSet(true)) {
                log.warn("OpenAI desabilitado, jobs de script serão ignorados até que a API key seja configurada");
            }
            return;
        }
        List<SalesVideoJobDto> jobs = backendClient.listOpenAiJobs(SalesVideoStatus.SCRIPT_PENDING,
                SalesVideoJobType.SCRIPT,
                maxJobs);
        if (jobs.isEmpty()) {
            return;
        }
        for (SalesVideoJobDto job : jobs) {
            handleJob(job);
        }
    }

    private void handleJob(SalesVideoJobDto job) {
        Long jobId = job.getId();
        try {
            SalesVideoJobDto claimed = backendClient.claimJob(jobId, workerId,
                    "ai-worker iniciando geração automática de script");
            SalesVideoProfileDto profile = backendClient.getProfile(claimed.getProfileId());
            if (profile == null) {
                throw new IllegalStateException("Perfil do vídeo não encontrado para o job " + jobId);
            }
            ProductDto product = profile.getProductId() != null
                    ? backendClient.getProduct(profile.getProductId())
                    : null;
            backendClient.reportProgress(jobId, 10, SalesVideoStatus.SCRIPT_PENDING,
                    "Contexto carregado", null);
            String prompt = promptBuilder.buildPrompt(profile, product);
            backendClient.reportProgress(jobId, 45, SalesVideoStatus.SCRIPT_PENDING,
                    "Prompt enviado à OpenAI", null);
            SalesVideoOpenAiClient.GeneratedScriptResult result = openAiClient.generateScript(jobId, prompt);
            JobCompletionRequest completionRequest = new JobCompletionRequest();
            completionRequest.setMessage("Script gerado automaticamente pela OpenAI");
            completionRequest.setMetadataJson(result.rawResponse());
            completionRequest.setDetailsJson(buildDetailsJson(result));
            attachScriptResult(completionRequest, result.payload());
            backendClient.completeJob(jobId, completionRequest);
            log.info("Job de script {} concluído (perfil {})", jobId, profile.getId());
        } catch (Exception ex) {
            log.error("Falha ao processar job de script {}", jobId, ex);
            JobFailureRequest failureRequest = new JobFailureRequest();
            failureRequest.setFailureCode(resolveFailureCode(ex));
            failureRequest.setFailureDetail(ex.getMessage());
            failureRequest.setStatus(SalesVideoStatus.VIDEO_FAILED);
            failureRequest.setMessage("Geração automática de script falhou");
            try {
                backendClient.failJob(jobId, failureRequest);
            } catch (Exception backendEx) {
                log.error("Falha ao reportar erro do job {} ao backend", jobId, backendEx);
            }
        }
    }

    private String buildDetailsJson(SalesVideoOpenAiClient.GeneratedScriptResult result) {
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("responseId", result.responseId());
            if (result.usage() != null) {
                details.put("inputTokens", result.usage().effectiveInputTokens());
                details.put("outputTokens", result.usage().effectiveOutputTokens());
                details.put("totalTokens", result.usage().totalTokens());
            }
            return objectMapper.writeValueAsString(details);
        } catch (Exception ex) {
            log.warn("Não foi possível serializar detalhes do job", ex);
            return null;
        }
    }

    private String resolveFailureCode(Exception ex) {
        if (ex instanceof SalesVideoOpenAiClient.SalesVideoOpenAiException) {
            return "OPENAI_ERROR";
        }
        if (ex instanceof SalesVideoBackendClient.SalesVideoBackendException) {
            return "BACKEND_ERROR";
        }
        if (ex instanceof IllegalStateException) {
            return "INVALID_STATE";
        }
        return "UNEXPECTED_ERROR";
    }

    private void attachScriptResult(JobCompletionRequest completionRequest, Map<String, Object> payload) {
        try {
            var setter = JobCompletionRequest.class.getMethod("setScriptResult", Object.class);
            setter.invoke(completionRequest, payload);
            return;
        } catch (NoSuchMethodException ignored) {
            // Continua para tentativa com assinatura tipada do pacote ads-service.
        } catch (Exception ex) {
            log.warn("Não foi possível preencher scriptResult no completion request", ex);
            return;
        }
        try {
            var setter = JobCompletionRequest.class.getMethods();
            for (var method : setter) {
                if (!"setScriptResult".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameterType = method.getParameterTypes()[0];
                Object converted = objectMapper.convertValue(payload, parameterType);
                method.invoke(completionRequest, converted);
                return;
            }
            log.warn("Método setScriptResult não encontrado em JobCompletionRequest");
        } catch (Exception ex) {
            log.warn("Não foi possível preencher scriptResult no completion request", ex);
        }
    }
}
