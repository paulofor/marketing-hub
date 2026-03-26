package com.marketinghub.hypothesis.framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkDto;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HypothesisFrameworkMapperSupport {
    private static final Logger log = LoggerFactory.getLogger(HypothesisFrameworkMapperSupport.class);
    private static final String VERSION = "dor-resultado-mecanismo-prova-oferta/v1";
    private final ObjectMapper mapper;

    public HypothesisFrameworkMapperSupport(ObjectMapper objectMapper) {
        this.mapper = objectMapper.copy();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Named("frameworkFromHypothesis")
    public HypothesisFrameworkDto toDto(Hypothesis hypothesis) {
        return clone(resolveInternal(hypothesis));
    }

    public HypothesisFrameworkDto resolve(Hypothesis hypothesis) {
        return clone(resolveInternal(hypothesis));
    }

    public void applyPartial(Hypothesis hypothesis, HypothesisFrameworkDto partial) {
        HypothesisFrameworkDto current = resolveInternal(hypothesis);
        HypothesisFrameworkDto merged = merge(current, partial);
        storeSnapshot(hypothesis, merged, partial);
    }

    public void storeSnapshot(Hypothesis hypothesis, HypothesisFrameworkDto snapshot, HypothesisFrameworkDto appliedPartial) {
        HypothesisFrameworkDto normalized = normalize(snapshot, hypothesis);
        try {
            hypothesis.setFrameworkJson(mapper.writeValueAsString(normalized));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize hypothesis framework JSON", e);
            hypothesis.setFrameworkJson(null);
        }
        syncLegacyFields(hypothesis, normalized, appliedPartial);
    }

    public HypothesisFrameworkDto merge(HypothesisFrameworkDto base, HypothesisFrameworkDto partial) {
        HypothesisFrameworkDto working = clone(base);
        if (partial == null) {
            return working;
        }
        if (StringUtils.hasText(partial.getVersion())) {
            working.setVersion(partial.getVersion().trim());
        }
        working.setPain(mergePain(working.getPain(), partial.getPain()));
        working.setResult(mergeResult(working.getResult(), partial.getResult()));
        working.setMechanism(mergeMechanism(working.getMechanism(), partial.getMechanism()));
        working.setProof(mergeProof(working.getProof(), partial.getProof()));
        working.setOffer(mergeOffer(working.getOffer(), partial.getOffer()));
        working.setChecklist(mergeChecklist(working.getChecklist(), partial.getChecklist()));
        return working;
    }

    private HypothesisFrameworkDto resolveInternal(Hypothesis hypothesis) {
        HypothesisFrameworkDto snapshot = null;
        if (hypothesis.getFrameworkJson() != null && !hypothesis.getFrameworkJson().isBlank()) {
            try {
                snapshot = mapper.readValue(hypothesis.getFrameworkJson(), HypothesisFrameworkDto.class);
            } catch (Exception e) {
                log.warn("Failed to parse framework JSON for hypothesis {}", hypothesis.getId(), e);
            }
        }
        if (snapshot == null) {
            snapshot = new HypothesisFrameworkDto();
        }
        ensureDefaults(snapshot);
        fillFallbacks(snapshot, hypothesis);
        return snapshot;
    }

    private void ensureDefaults(HypothesisFrameworkDto dto) {
        if (dto.getPain() == null) dto.setPain(new HypothesisFrameworkDto.Pain());
        if (dto.getResult() == null) dto.setResult(new HypothesisFrameworkDto.Result());
        if (dto.getMechanism() == null) dto.setMechanism(new HypothesisFrameworkDto.Mechanism());
        if (dto.getProof() == null) dto.setProof(new HypothesisFrameworkDto.Proof());
        if (dto.getOffer() == null) dto.setOffer(new HypothesisFrameworkDto.Offer());
        if (dto.getChecklist() == null) dto.setChecklist(new HypothesisFrameworkDto.Checklist());
        if (!StringUtils.hasText(dto.getVersion())) {
            dto.setVersion(VERSION);
        }
    }

    private void fillFallbacks(HypothesisFrameworkDto dto, Hypothesis hypothesis) {
        dto.getPain().setSurface(firstNonBlank(dto.getPain().getSurface(), hypothesis.getProblem()));
        dto.getPain().setRoot(firstNonBlank(dto.getPain().getRoot(), hypothesis.getProblem()));
        dto.getResult().setDesiredResult(firstNonBlank(dto.getResult().getDesiredResult(), hypothesis.getPromise()));
        dto.getMechanism().setCore(firstNonBlank(dto.getMechanism().getCore(), hypothesis.getMechanism()));
        dto.getMechanism().setUnique(firstNonBlank(dto.getMechanism().getUnique(), hypothesis.getUniqueMechanism()));
        dto.getProof().setMessage(firstNonBlank(dto.getProof().getMessage(), hypothesis.getEntrega()));
        dto.getOffer().setName(firstNonBlank(dto.getOffer().getName(), hypothesis.getTitle()));
        dto.getOffer().setCorePromise(firstNonBlank(dto.getOffer().getCorePromise(), hypothesis.getPromise()));
        if (dto.getOffer().getPriceAmount() == null) {
            dto.getOffer().setPriceAmount(hypothesis.getPrice());
        }
        if (!StringUtils.hasText(dto.getOffer().getOfferType()) && hypothesis.getOfferType() != null) {
            dto.getOffer().setOfferType(hypothesis.getOfferType().name());
        }
    }

    private HypothesisFrameworkDto.Pain mergePain(HypothesisFrameworkDto.Pain base, HypothesisFrameworkDto.Pain partial) {
        if (partial == null) return base;
        base.setSurface(firstNonBlank(partial.getSurface(), base.getSurface()));
        base.setRoot(firstNonBlank(partial.getRoot(), base.getRoot()));
        base.setEmotional(firstNonBlank(partial.getEmotional(), base.getEmotional()));
        base.setSocial(firstNonBlank(partial.getSocial(), base.getSocial()));
        base.setCost(firstNonBlank(partial.getCost(), base.getCost()));
        return base;
    }

    private HypothesisFrameworkDto.Result mergeResult(HypothesisFrameworkDto.Result base, HypothesisFrameworkDto.Result partial) {
        if (partial == null) return base;
        base.setDesiredResult(firstNonBlank(partial.getDesiredResult(), base.getDesiredResult()));
        base.setDesiredIdentity(firstNonBlank(partial.getDesiredIdentity(), base.getDesiredIdentity()));
        base.setBusinessOutcome(firstNonBlank(partial.getBusinessOutcome(), base.getBusinessOutcome()));
        base.setSuccessSignal(firstNonBlank(partial.getSuccessSignal(), base.getSuccessSignal()));
        return base;
    }

    private HypothesisFrameworkDto.Mechanism mergeMechanism(HypothesisFrameworkDto.Mechanism base, HypothesisFrameworkDto.Mechanism partial) {
        if (partial == null) return base;
        base.setCore(firstNonBlank(partial.getCore(), base.getCore()));
        base.setUnique(firstNonBlank(partial.getUnique(), base.getUnique()));
        base.setVisible(firstNonBlank(partial.getVisible(), base.getVisible()));
        base.setBelievability(firstNonBlank(partial.getBelievability(), base.getBelievability()));
        return base;
    }

    private HypothesisFrameworkDto.Proof mergeProof(HypothesisFrameworkDto.Proof base, HypothesisFrameworkDto.Proof partial) {
        if (partial == null) return base;
        base.setType(firstNonBlank(partial.getType(), base.getType()));
        base.setAsset(firstNonBlank(partial.getAsset(), base.getAsset()));
        base.setMessage(firstNonBlank(partial.getMessage(), base.getMessage()));
        base.setDeliveryStage(firstNonBlank(partial.getDeliveryStage(), base.getDeliveryStage()));
        return base;
    }

    private HypothesisFrameworkDto.Offer mergeOffer(HypothesisFrameworkDto.Offer base, HypothesisFrameworkDto.Offer partial) {
        if (partial == null) return base;
        base.setName(firstNonBlank(partial.getName(), base.getName()));
        base.setCorePromise(firstNonBlank(partial.getCorePromise(), base.getCorePromise()));
        base.setDeliverables(firstNonBlank(partial.getDeliverables(), base.getDeliverables()));
        base.setRiskReversal(firstNonBlank(partial.getRiskReversal(), base.getRiskReversal()));
        base.setPriceLogic(firstNonBlank(partial.getPriceLogic(), base.getPriceLogic()));
        base.setCta(firstNonBlank(partial.getCta(), base.getCta()));
        if (partial.getPriceAmount() != null) {
            base.setPriceAmount(partial.getPriceAmount());
        }
        base.setOfferType(firstNonBlank(partial.getOfferType(), base.getOfferType()));
        return base;
    }

    private HypothesisFrameworkDto.Checklist mergeChecklist(HypothesisFrameworkDto.Checklist base, HypothesisFrameworkDto.Checklist partial) {
        if (partial == null) return base;
        base.setPainReady(resolveBoolean(partial.getPainReady(), base.getPainReady()));
        base.setResultReady(resolveBoolean(partial.getResultReady(), base.getResultReady()));
        base.setMechanismReady(resolveBoolean(partial.getMechanismReady(), base.getMechanismReady()));
        base.setProofReady(resolveBoolean(partial.getProofReady(), base.getProofReady()));
        base.setOfferReady(resolveBoolean(partial.getOfferReady(), base.getOfferReady()));
        base.setApprovedForExperiment(resolveBoolean(partial.getApprovedForExperiment(), base.getApprovedForExperiment()));
        base.setNotes(firstNonBlank(partial.getNotes(), base.getNotes()));
        return base;
    }

    private Boolean resolveBoolean(Boolean candidate, Boolean fallback) {
        return candidate != null ? candidate : (fallback != null ? fallback : Boolean.FALSE);
    }

    private void syncLegacyFields(Hypothesis hypothesis, HypothesisFrameworkDto dto, HypothesisFrameworkDto appliedPartial) {
        if (shouldUpdatePain(appliedPartial)) {
            hypothesis.setProblem(firstNonBlank(dto.getPain().getRoot(), dto.getPain().getSurface()));
        }
        if (shouldUpdateResult(appliedPartial)) {
            hypothesis.setPromise(firstNonBlank(dto.getResult().getDesiredResult(), hypothesis.getPromise()));
        }
        if (shouldUpdateMechanism(appliedPartial)) {
            hypothesis.setMechanism(firstNonBlank(dto.getMechanism().getCore(), hypothesis.getMechanism()));
            hypothesis.setUniqueMechanism(firstNonBlank(dto.getMechanism().getUnique(), hypothesis.getUniqueMechanism()));
        }
        if (shouldUpdateProof(appliedPartial)) {
            hypothesis.setEntrega(firstNonBlank(dto.getProof().getMessage(), hypothesis.getEntrega()));
        }
        if (shouldUpdateOffer(appliedPartial)) {
            hypothesis.setTitle(firstNonBlank(dto.getOffer().getName(), hypothesis.getTitle()));
            if (dto.getOffer().getPriceAmount() != null) {
                hypothesis.setPrice(dto.getOffer().getPriceAmount());
            }
            if (StringUtils.hasText(dto.getOffer().getOfferType())) {
                try {
                    hypothesis.setOfferType(OfferType.valueOf(dto.getOffer().getOfferType().trim().toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown offer type '{}' for hypothesis {}", dto.getOffer().getOfferType(), hypothesis.getId());
                }
            }
        }
    }

    private boolean shouldUpdatePain(HypothesisFrameworkDto appliedPartial) {
        return appliedPartial != null && hasPainContent(appliedPartial.getPain());
    }

    private boolean shouldUpdateResult(HypothesisFrameworkDto appliedPartial) {
        return appliedPartial != null && hasResultContent(appliedPartial.getResult());
    }

    private boolean shouldUpdateMechanism(HypothesisFrameworkDto appliedPartial) {
        return appliedPartial != null && hasMechanismContent(appliedPartial.getMechanism());
    }

    private boolean shouldUpdateProof(HypothesisFrameworkDto appliedPartial) {
        return appliedPartial != null && hasProofContent(appliedPartial.getProof());
    }

    private boolean shouldUpdateOffer(HypothesisFrameworkDto appliedPartial) {
        return appliedPartial != null && hasOfferContent(appliedPartial.getOffer());
    }

    private boolean hasPainContent(HypothesisFrameworkDto.Pain pain) {
        return pain != null && (hasText(pain.getSurface()) || hasText(pain.getRoot())
                || hasText(pain.getEmotional()) || hasText(pain.getSocial()) || hasText(pain.getCost()));
    }

    private boolean hasResultContent(HypothesisFrameworkDto.Result result) {
        return result != null && (hasText(result.getDesiredResult()) || hasText(result.getDesiredIdentity())
                || hasText(result.getBusinessOutcome()) || hasText(result.getSuccessSignal()));
    }

    private boolean hasMechanismContent(HypothesisFrameworkDto.Mechanism mechanism) {
        return mechanism != null && (hasText(mechanism.getCore()) || hasText(mechanism.getUnique())
                || hasText(mechanism.getVisible()) || hasText(mechanism.getBelievability()));
    }

    private boolean hasProofContent(HypothesisFrameworkDto.Proof proof) {
        return proof != null && (hasText(proof.getType()) || hasText(proof.getAsset())
                || hasText(proof.getMessage()) || hasText(proof.getDeliveryStage()));
    }

    private boolean hasOfferContent(HypothesisFrameworkDto.Offer offer) {
        return offer != null && (hasText(offer.getName()) || hasText(offer.getCorePromise())
                || hasText(offer.getDeliverables()) || hasText(offer.getRiskReversal())
                || hasText(offer.getPriceLogic()) || hasText(offer.getCta())
                || offer.getPriceAmount() != null || hasText(offer.getOfferType()));
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private HypothesisFrameworkDto normalize(HypothesisFrameworkDto dto, Hypothesis hypothesis) {
        HypothesisFrameworkDto normalized = clone(dto);
        ensureDefaults(normalized);
        fillFallbacks(normalized, hypothesis);
        normalizeChecklist(normalized);
        return normalized;
    }

    private void normalizeChecklist(HypothesisFrameworkDto dto) {
        HypothesisFrameworkDto.Checklist checklist = dto.getChecklist();
        checklist.setPainReady(defaultBool(checklist.getPainReady()));
        checklist.setResultReady(defaultBool(checklist.getResultReady()));
        checklist.setMechanismReady(defaultBool(checklist.getMechanismReady()));
        checklist.setProofReady(defaultBool(checklist.getProofReady()));
        checklist.setOfferReady(defaultBool(checklist.getOfferReady()));
        checklist.setApprovedForExperiment(defaultBool(checklist.getApprovedForExperiment()));
    }

    private Boolean defaultBool(Boolean value) {
        return value != null ? value : Boolean.FALSE;
    }

    private String firstNonBlank(String candidate, String fallback) {
        if (StringUtils.hasText(candidate)) {
            return candidate.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return null;
    }

    private HypothesisFrameworkDto clone(HypothesisFrameworkDto dto) {
        try {
            return mapper.readValue(mapper.writeValueAsString(dto), HypothesisFrameworkDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to clone hypothesis framework dto", e);
            return new HypothesisFrameworkDto();
        }
    }
}
