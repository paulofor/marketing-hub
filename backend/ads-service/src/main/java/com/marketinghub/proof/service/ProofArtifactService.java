package com.marketinghub.proof.service;

import com.marketinghub.creative.label.VisualProof;
import com.marketinghub.creative.label.repository.VisualProofRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.proof.ProofArtifact;
import com.marketinghub.proof.ProofStage;
import com.marketinghub.proof.ProofStatus;
import com.marketinghub.proof.dto.CreateProofArtifactRequest;
import com.marketinghub.proof.dto.UpdateProofArtifactRequest;
import com.marketinghub.proof.repository.ProofArtifactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ProofArtifactService {
    private final ProofArtifactRepository repository;
    private final HypothesisRepository hypothesisRepository;
    private final ExperimentRepository experimentRepository;
    private final VisualProofRepository visualProofRepository;

    public ProofArtifactService(ProofArtifactRepository repository,
                                HypothesisRepository hypothesisRepository,
                                ExperimentRepository experimentRepository,
                                VisualProofRepository visualProofRepository) {
        this.repository = repository;
        this.hypothesisRepository = hypothesisRepository;
        this.experimentRepository = experimentRepository;
        this.visualProofRepository = visualProofRepository;
    }

    public List<ProofArtifact> listByHypothesis(UUID hypothesisId) {
        return repository.findByHypothesisIdOrderByCreatedAtDesc(hypothesisId);
    }

    public List<ProofArtifact> listByExperiment(Long experimentId) {
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    @Transactional
    public ProofArtifact createForHypothesis(UUID hypothesisId, CreateProofArtifactRequest request) {
        Hypothesis hypothesis = hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Hypothesis not found: " + hypothesisId));
        ProofArtifact artifact = new ProofArtifact();
        artifact.setHypothesis(hypothesis);
        artifact.setMarketNiche(hypothesis.getMarketNiche());
        applyPayload(artifact, request);
        return repository.save(artifact);
    }

    @Transactional
    public ProofArtifact createForExperiment(Long experimentId, CreateProofArtifactRequest request) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Experiment not found: " + experimentId));
        ProofArtifact artifact = new ProofArtifact();
        artifact.setExperiment(experiment);
        artifact.setHypothesis(experiment.getHypothesisRef());
        artifact.setMarketNiche(experiment.getNiche());
        applyPayload(artifact, request);
        return repository.save(artifact);
    }

    @Transactional
    public ProofArtifact update(Long id, UpdateProofArtifactRequest request) {
        ProofArtifact artifact = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proof artifact not found: " + id));
        applyUpdate(artifact, request);
        return repository.save(artifact);
    }

    private void applyPayload(ProofArtifact artifact, CreateProofArtifactRequest request) {
        artifact.setStage(ProofStage.fromString(request.getStage()));
        artifact.setStatus(ProofStatus.fromString(request.getStatus()));
        artifact.setCustomType(StringUtils.hasText(request.getCustomType()) ? request.getCustomType().trim() : null);
        artifact.setAssetPlan(StringUtils.hasText(request.getAssetPlan()) ? request.getAssetPlan() : null);
        artifact.setAssetUrl(StringUtils.hasText(request.getAssetUrl()) ? request.getAssetUrl().trim() : null);
        artifact.setMessage(StringUtils.hasText(request.getMessage()) ? request.getMessage() : null);
        artifact.setDeliveryNotes(StringUtils.hasText(request.getDeliveryNotes()) ? request.getDeliveryNotes() : null);
        artifact.setPrompt(StringUtils.hasText(request.getPrompt()) ? request.getPrompt() : null);
        artifact.setModel(StringUtils.hasText(request.getModel()) ? request.getModel().trim() : null);
        artifact.setVisualProof(resolveVisualProof(request.getVisualProofId()));
    }

    private void applyUpdate(ProofArtifact artifact, UpdateProofArtifactRequest request) {
        if (request.getStage() != null) {
            artifact.setStage(ProofStage.fromString(request.getStage()));
        }
        if (request.getStatus() != null) {
            artifact.setStatus(ProofStatus.fromString(request.getStatus()));
        }
        if (request.getCustomType() != null) {
            artifact.setCustomType(StringUtils.hasText(request.getCustomType()) ? request.getCustomType().trim() : null);
        }
        if (request.getAssetPlan() != null) {
            artifact.setAssetPlan(StringUtils.hasText(request.getAssetPlan()) ? request.getAssetPlan() : null);
        }
        if (request.getAssetUrl() != null) {
            artifact.setAssetUrl(StringUtils.hasText(request.getAssetUrl()) ? request.getAssetUrl().trim() : null);
        }
        if (request.getMessage() != null) {
            artifact.setMessage(StringUtils.hasText(request.getMessage()) ? request.getMessage() : null);
        }
        if (request.getDeliveryNotes() != null) {
            artifact.setDeliveryNotes(StringUtils.hasText(request.getDeliveryNotes()) ? request.getDeliveryNotes() : null);
        }
        if (request.getPrompt() != null) {
            artifact.setPrompt(StringUtils.hasText(request.getPrompt()) ? request.getPrompt() : null);
        }
        if (request.getModel() != null) {
            artifact.setModel(StringUtils.hasText(request.getModel()) ? request.getModel().trim() : null);
        }
        if (request.getVisualProofId() != null) {
            artifact.setVisualProof(resolveVisualProof(request.getVisualProofId()));
        }
    }

    private VisualProof resolveVisualProof(Long id) {
        if (id == null) {
            return null;
        }
        return visualProofRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Visual proof not found: " + id));
    }
}
