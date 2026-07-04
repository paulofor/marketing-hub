package com.marketinghub.metaaudience.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.metaaudience.ExperimentMetaAudience;
import com.marketinghub.metaaudience.MetaAudience;
import com.marketinghub.metaaudience.MetaAudienceSegment;
import com.marketinghub.metaaudience.service.internalComplete.MetaAudienceSyncCompleteRequest;
import com.marketinghub.metaaudience.service.internalPending.MetaAudiencePendingResponse;
import com.marketinghub.metaaudience.service.linkExperiment.ExperimentMetaAudienceResponse;
import com.marketinghub.metaaudience.service.linkExperiment.LinkMetaAudienceExperimentRequest;
import com.marketinghub.metaaudience.service.requestAudience.MetaAudienceRequest;
import com.marketinghub.metaaudience.service.requestAudience.MetaAudienceResponse;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.metaaudience.ExperimentMetaAudienceRepository;
import com.marketinghub.repository.jpa.metaaudience.MetaAudienceRepository;
import com.marketinghub.repository.jpa.metaaudience.MetaAudienceSegmentRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Serviço de leitura e escrita das audiências Meta Ads decididas pelos módulos executores. */
@Service
@RequiredArgsConstructor
public class BackendMetaAudienceService {
    private final MetaAudienceRepository audienceRepository;
    private final MetaAudienceSegmentRepository segmentRepository;
    private final ExperimentMetaAudienceRepository experimentAudienceRepository;
    private final MarketNicheRepository nicheRepository;
    private final ExperimentRepository experimentRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Persiste uma audiência recebida do OPRM sem calcular elegibilidade, nome, recorte ou volume de negócio. */
    @Transactional
    public MetaAudienceResponse requestAudience(MetaAudienceRequest request) {
        validateRequiredPersistenceFields(request);
        MarketNiche niche = nicheRepository.findById(request.marketNicheId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nicho não encontrado."));
        Instant now = Instant.now();
        MetaAudience audience = new MetaAudience();
        audience.setMarketNiche(niche);
        audience.setSourceCnaeCode(normalizeCnae(request.cnaeCode()));
        audience.setAudienceName(limit(request.audienceName().trim(), 255));
        audience.setFacebookAdAccountId(request.facebookAdAccountId().trim());
        audience.setAudienceType(limit(request.audienceType().trim(), 32));
        audience.setSourceType(limit(request.sourceType().trim(), 32));
        audience.setFilterStrategy(request.filterStrategy());
        audience.setEligibilityStatus(limit(request.eligibilityStatus().trim(), 32));
        audience.setTotalContacts(nonNegative(request.totalContacts()));
        audience.setUniqueEmails(nonNegative(request.uniqueEmails()));
        audience.setSyncedContacts(0);
        audience.setCreatedAt(now);
        audience.setUpdatedAt(now);
        audience = audienceRepository.save(audience);

        MetaAudienceSegment segment = new MetaAudienceSegment();
        segment.setMetaAudience(audience);
        segment.setMarketNiche(niche);
        segment.setSegmentName(request.segmentName().trim());
        segment.setSegmentDescription(request.segmentDescription());
        segment.setPainFocus(request.painFocus());
        segment.setDesiredOutcomeFocus(request.desiredOutcomeFocus());
        segment.setOfferAngle(request.offerAngle());
        segment.setSelectionRule(request.filterStrategy());
        segment.setEstimatedContacts(nonNegative(request.estimatedContacts()));
        segment.setCreatedAt(now);
        segment.setUpdatedAt(now);
        segmentRepository.save(segment);
        return toResponse(audience);
    }

    /** Lista registros em READY e apenas anexa os emails brutos persistidos para execução técnica do worker. */
    @Transactional(readOnly = true)
    public List<MetaAudiencePendingResponse> listPending(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return audienceRepository.findByEligibilityStatusOrderByUpdatedAtAsc("READY", PageRequest.of(0, safeLimit)).stream()
                .map(a -> new MetaAudiencePendingResponse(a.getId(), a.getMarketNiche().getId(), a.getSourceCnaeCode(),
                        a.getAudienceName(), a.getFacebookAdAccountId(), loadEmails(a.getSourceCnaeCode())))
                .toList();
    }

    /** Lista os planos de audiência CNAE já persistidos para um nicho. */
    @Transactional(readOnly = true)
    public List<MetaAudienceResponse> listByNiche(Long nicheId) {
        if (nicheId == null || !nicheRepository.existsById(nicheId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nicho não encontrado.");
        }
        return audienceRepository.findByMarketNicheIdOrderByUpdatedAtDesc(nicheId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Vincula uma audiência CNAE e sua parcela funcional ao experimento que validará a hipótese. */
    @Transactional
    public ExperimentMetaAudienceResponse linkExperiment(LinkMetaAudienceExperimentRequest request) {
        validateExperimentLinkRequest(request);
        MetaAudience audience = audienceRepository.findById(request.metaAudienceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audiência não encontrada."));
        Experiment experiment = experimentRepository.findById(request.experimentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado."));
        MetaAudienceSegment segment = loadSegment(request.metaAudienceSegmentId(), audience.getId());
        validateExperimentAndAudienceNiche(experiment, audience, segment);

        Instant now = Instant.now();
        ExperimentMetaAudience link = experimentAudienceRepository
                .findByExperimentIdAndMetaAudienceIdAndMetaAudienceSegmentId(
                        experiment.getId(), audience.getId(), segment != null ? segment.getId() : null)
                .orElseGet(ExperimentMetaAudience::new);
        if (link.getId() == null) {
            link.setCreatedAt(now);
        }
        link.setExperiment(experiment);
        link.setMetaAudience(audience);
        link.setMetaAudienceSegment(segment);
        link.setMarketNiche(audience.getMarketNiche());
        link.setActivationStatus(defaultStatus(request.activationStatus()));
        link.setChannel(limit(request.channel(), 64));
        link.setPainAngle(request.painAngle());
        link.setPromise(request.promise());
        link.setOffer(request.offer());
        link.setDecisionSnapshotJson(request.decisionSnapshotJson());
        link.setUpdatedAt(now);
        return toExperimentAudienceResponse(experimentAudienceRepository.save(link));
    }

    /** Lista as audiências CNAE vinculadas a um experimento. */
    @Transactional(readOnly = true)
    public List<ExperimentMetaAudienceResponse> listByExperiment(Long experimentId) {
        if (experimentId == null || !experimentRepository.existsById(experimentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado.");
        }
        return experimentAudienceRepository.findByExperimentIdOrderByUpdatedAtDesc(experimentId).stream()
                .map(this::toExperimentAudienceResponse)
                .toList();
    }

    /** Lista os vínculos de audiência CNAE feitos dentro de um nicho. */
    @Transactional(readOnly = true)
    public List<ExperimentMetaAudienceResponse> listExperimentLinksByNiche(Long nicheId) {
        if (nicheId == null || !nicheRepository.existsById(nicheId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nicho não encontrado.");
        }
        return experimentAudienceRepository.findByMarketNicheIdOrderByUpdatedAtDesc(nicheId).stream()
                .map(this::toExperimentAudienceResponse)
                .toList();
    }

    /** Atualiza o estado persistido da audiência conforme o resultado técnico reportado pelo worker. */
    @Transactional
    public MetaAudienceResponse completeSync(Long id, MetaAudienceSyncCompleteRequest request) {
        MetaAudience audience = audienceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audiência não encontrada."));
        Instant now = Instant.now();
        if (request != null && "FAILED".equalsIgnoreCase(request.status())) {
            audience.setEligibilityStatus("FAILED");
            audience.setErrorMessage(limit(request.errorMessage(), 1000));
        } else {
            audience.setEligibilityStatus("SYNCED");
            audience.setFacebookAudienceId(request != null ? request.facebookAudienceId() : null);
            audience.setSyncedContacts(request != null ? request.syncedContacts() : 0);
            audience.setLastSyncAt(now);
            audience.setErrorMessage(null);
        }
        audience.setUpdatedAt(now);
        return toResponse(audienceRepository.save(audience));
    }

    /** Valida somente campos necessários para persistência e relacionamento, sem regra de negócio de público. */
    private void validateRequiredPersistenceFields(MetaAudienceRequest request) {
        if (request == null || request.marketNicheId() == null || !StringUtils.hasText(request.cnaeCode())
                || !StringUtils.hasText(request.audienceName()) || !StringUtils.hasText(request.segmentName())
                || !StringUtils.hasText(request.facebookAdAccountId()) || !StringUtils.hasText(request.audienceType())
                || !StringUtils.hasText(request.sourceType()) || !StringUtils.hasText(request.eligibilityStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Campos persistentes obrigatórios ausentes para registrar a audiência Meta.");
        }
    }

    /** Valida os identificadores mínimos para relacionar audiência e experimento. */
    private void validateExperimentLinkRequest(LinkMetaAudienceExperimentRequest request) {
        if (request == null || request.metaAudienceId() == null || request.metaAudienceSegmentId() == null
                || request.experimentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Audiência, parcela funcional e experimento são obrigatórios para vincular o plano CNAE.");
        }
    }

    /** Carrega a parcela funcional e garante que ela pertença à audiência informada. */
    private MetaAudienceSegment loadSegment(Long segmentId, Long audienceId) {
        MetaAudienceSegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parcela da audiência não encontrada."));
        if (!segment.getMetaAudience().getId().equals(audienceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parcela funcional não pertence à audiência informada.");
        }
        return segment;
    }

    /** Garante que audiência, parcela e experimento pertencem ao mesmo nicho. */
    private void validateExperimentAndAudienceNiche(
            Experiment experiment, MetaAudience audience, MetaAudienceSegment segment) {
        Long experimentNicheId = experiment.getNiche().getId();
        Long audienceNicheId = audience.getMarketNiche().getId();
        if (!experimentNicheId.equals(audienceNicheId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Audiência CNAE e experimento precisam pertencer ao mesmo nicho.");
        }
        if (segment != null && !segment.getMarketNiche().getId().equals(audienceNicheId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parcela funcional precisa pertencer ao mesmo nicho da audiência.");
        }
    }

    /** Lê os emails persistidos pelo backend para entrega técnica ao worker, sem calcular seleção de público. */
    private List<String> loadEmails(String cnaeCode) {
        return jdbcTemplate.queryForList(
                "SELECT email FROM oprm_estabelecimento_cnae_raiz WHERE cnae_code = ? AND email IS NOT NULL AND email <> ''",
                String.class,
                cnaeCode);
    }

    /** Normaliza o CNAE apenas para alinhar a chave física de leitura e escrita. */
    private String normalizeCnae(String cnaeCode) {
        return cnaeCode.replaceAll("\\D", "");
    }

    /** Converte valores nulos em zero para persistência numérica simples. */
    private long nonNegative(Long value) {
        return value == null || value < 0 ? 0 : value;
    }

    /** Converte a entidade persistida para resposta pública enxuta. */
    private MetaAudienceResponse toResponse(MetaAudience audience) {
        return new MetaAudienceResponse(audience.getId(), audience.getMarketNiche().getId(), audience.getAudienceName(),
                audience.getEligibilityStatus(), audience.getTotalContacts(), audience.getUniqueEmails());
    }

    /** Converte o vínculo experimento-audiência em contrato de leitura para a tela e análise. */
    private ExperimentMetaAudienceResponse toExperimentAudienceResponse(ExperimentMetaAudience link) {
        MetaAudience audience = link.getMetaAudience();
        MetaAudienceSegment segment = link.getMetaAudienceSegment();
        return new ExperimentMetaAudienceResponse(
                link.getId(),
                link.getExperiment().getId(),
                link.getMarketNiche().getId(),
                audience.getId(),
                segment != null ? segment.getId() : null,
                audience.getSourceCnaeCode(),
                audience.getAudienceName(),
                segment != null ? segment.getSegmentName() : null,
                link.getActivationStatus(),
                link.getChannel(),
                link.getPainAngle(),
                link.getPromise(),
                link.getOffer(),
                link.getDecisionSnapshotJson(),
                link.getAnalysisSummary());
    }

    /** Define o status inicial do plano quando o solicitante não envia um valor explícito. */
    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? limit(status.trim(), 32) : "APPROVED_FOR_EXPERIMENT";
    }

    /** Limita textos ao tamanho máximo da coluna de destino. */
    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
