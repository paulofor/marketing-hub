package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.dto.AssetType;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: preservar e recuperar tomadas Runway por job, task, arquivo e SHA-256. */
@Component
public class RunwayRecoveryStore {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RunwayRecoveryStore.class);
    private final BackendVideoClient backend;
    private final VideoAssetClient assets;
    private final ObjectMapper mapper;
    private final WebClient downloads;
    private com.marketinghub.videomanagement.service.BoundedVideoSpending spending;

    /** Conecta o recibo da API para recuperar uma task cuja atualização de progresso se perdeu. */
    @org.springframework.beans.factory.annotation.Autowired
    public void setSpending(com.marketinghub.videomanagement.service.BoundedVideoSpending spending) { this.spending = spending; }

    /** Reutiliza os contratos e o storage oficiais; nenhum arquivo depende do filesystem do worker. */
    public RunwayRecoveryStore(BackendVideoClient backend, VideoAssetClient assets, ObjectMapper mapper,
                               WebClient.Builder builder) {
        this.backend = backend;
        this.assets = assets;
        this.mapper = mapper;
        this.downloads = builder.clone().codecs(c -> c.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)).build();
    }

    /** Carrega somente evidências do job, rejeitando checkpoint inválido ou envio de resultado incerto. */
    public State load(SalesVideoJob job) {
        Map<Integer, JsonNode> submissions = new LinkedHashMap<>();
        Map<Integer, JsonNode> clips = new LinkedHashMap<>();
        Map<Integer,JsonNode> started = new LinkedHashMap<>();
        JsonNode plan = null;
        var events = backend.fetchJobEvents(job.id());
        if (events == null) throw blocked("Backend não confirmou os checkpoints do job " + job.id());
        for (var event : events) {
            if (event.detailsJson() == null || !event.detailsJson().stripLeading().startsWith("{")) continue;
            JsonNode node;
            try { node = mapper.readTree(event.detailsJson()); }
            catch (Exception ex) {
                log.error("Checkpoint ilegível; jobId={} eventId={}", job.id(), event.id(), ex);
                throw blocked("Checkpoint ilegível; conciliar antes de repetir geração");
            }
            String type = node.path("eventType").asText();
            if (!type.startsWith("PROVIDER_") && !"APOLLO_PLAN_CHECKPOINT".equals(type)) continue;
            if (node.has("jobId") && node.path("jobId").asLong() != job.id())
                throw blocked("Checkpoint pertence a outro job");
            int scene = node.path("scene").asInt();
            if ("PROVIDER_SUBMISSION_STARTED".equals(type)) started.put(scene,node);
            if ("PROVIDER_SUBMISSION_ACCEPTED".equals(type)) {
                if (node.path("providerTaskId").asText().isBlank()) throw blocked("Task sem identidade persistida");
                JsonNode previous = submissions.putIfAbsent(scene, node);
                if (previous != null && !previous.path("providerTaskId").equals(node.path("providerTaskId")))
                    throw blocked("Duas tasks divergentes para a mesma tomada; conciliar");
            }
            if ("PROVIDER_CLIP_PERSISTED".equals(type)) clips.put(scene, node);
            if ("APOLLO_PLAN_CHECKPOINT".equals(type)) plan = node.path("metadata");
        }
        for (var pending : started.entrySet()) {
            if (submissions.containsKey(pending.getKey())) continue;
            JsonNode known = spending == null ? null : spending.replayOnly(job.id(),"CLIP_" + pending.getKey());
            JsonNode result = known == null ? null : known.path("response");
            String taskId = result == null ? "" : result.path("id").asText(result.path("taskId").asText(result.path("task_id").asText()));
            if (taskId.isBlank() || !known.path("request").equals(pending.getValue().path("request"))
                    || pending.getValue().path("estimatedCredits").decimalValue().signum() <= 0)
                throw blocked("Envio anterior sem task confirmada; resultado incerto exige conciliação, sem outro POST");
            var accepted = (com.fasterxml.jackson.databind.node.ObjectNode) pending.getValue().deepCopy();
            accepted.put("eventType","PROVIDER_SUBMISSION_ACCEPTED").put("providerTaskId",taskId);
            accepted.set("rawResponse",result);
            backend.reportProgress(job.id(),new com.marketinghub.videomanagement.client.payload.JobProgressPayload(
                    10,SalesVideoStatus.VIDEO_PROCESSING,"Identidade original recuperada sem geração",json(accepted)));
            submissions.put(pending.getKey(),accepted);
        }
        return new State(Map.copyOf(submissions), Map.copyOf(clips), plan, !started.isEmpty());
    }

    /** Restaura o plano aprovado e sua auditoria sem consumir novamente o planejador. */
    public SalesVideoJob restorePlan(SalesVideoJob job) {
        State state = load(job);
        if (state.plan() == null) {
            if (state.recovery()) throw blocked("Tomadas existentes sem plano aprovado persistido; conciliar");
            return job;
        }
        try {
            var value = mapper.valueToTree(job);
            ((com.fasterxml.jackson.databind.node.ObjectNode) value).put("metadataJson", state.plan().toString());
            return mapper.treeToValue(value, SalesVideoJob.class);
        } catch (Exception ex) {
            log.error("Falha ao restaurar plano; jobId={}", job.id(), ex);
            throw blocked("Não foi possível restaurar o plano aprovado");
        }
    }

    /** Grava bytes no storage antes da montagem e persiste recibo com identidade e hash. */
    public void preserve(SalesVideoJob job, int scene, String taskId, String sourceUrl,
                         JsonNode result, ProviderFile clip, ProgressCallback progress) {
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("job_id", job.id()); proof.put("profile_id", job.profileId());
        proof.put("tenant_id", job.tenantId()); proof.put("provider_task_id", taskId);
        proof.put("scene", scene); proof.put("sha256", hash(clip.content()));
        proof.put("role", "RUNWAY_RECOVERY_CLIP");
        var asset = assets.uploadAsset(clip, json(proof));
        if (asset == null || asset.id() == null || asset.url() == null)
            throw blocked("Storage não confirmou o clipe; consultar a mesma task antes de repetir");
        proof.put("eventType", "PROVIDER_CLIP_PERSISTED"); proof.put("jobId", job.id());
        proof.put("providerTaskId", taskId); proof.put("assetId", asset.id());
        proof.put("assetUrl", asset.url()); proof.put("sourceUrl", sourceUrl);
        proof.put("rawResponse", result); proof.put("byteSize", clip.content().length);
        progress.onProgress(85, SalesVideoStatus.VIDEO_PROCESSING,
                "Tomada " + scene + " persistida para recuperação sem geração", json(proof));
    }

    /** Baixa os mesmos bytes já persistidos e exige hash, tamanho e identidade do recibo. */
    public ProviderFile recover(SalesVideoJob job, int scene, JsonNode submission, JsonNode clip) {
        if (clip.path("jobId").asLong() != job.id() || clip.path("scene").asInt() != scene
                || !clip.path("providerTaskId").equals(submission.path("providerTaskId")))
            throw blocked("Clipe de recuperação pertence a outro contexto");
        byte[] bytes = downloads.get().uri(clip.path("assetUrl").asText()).retrieve().bodyToMono(byte[].class).block();
        if (bytes == null || bytes.length != clip.path("byteSize").asInt()
                || !hash(bytes).equals(clip.path("sha256").asText()))
            throw blocked("Hash ou tamanho do clipe persistido diverge; não regenerar");
        return new ProviderFile("sales-video-" + job.id() + "-scene-" + scene + ".mp4",
                MediaType.valueOf("video/mp4"), AssetType.VIDEO, ProviderAssetRole.VIDEO, bytes);
    }

    /** Serializa evidência estruturada sem perder a exceção original de auditoria. */
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception ex) { log.error("Falha ao serializar checkpoint Runway", ex); throw blocked("Checkpoint não serializado"); }
    }

    /** Calcula a identidade física antes de preservar ou reutilizar uma tomada. */
    private String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception ex) { log.error("Falha ao identificar clipe Runway", ex); throw blocked("SHA-256 indisponível"); }
    }

    /** Mantém a falha como pendência de conciliação sem liberar repetição paga. */
    private VideoProviderException blocked(String reason) { return new VideoProviderException("PROVIDER_RECONCILIATION_REQUIRED", reason); }

    /** Agrupa somente checkpoints persistidos, nunca estado privado de memória de outra tentativa. */
    public record State(Map<Integer, JsonNode> submissions, Map<Integer, JsonNode> clips, JsonNode plan, boolean recovery) {}
}
