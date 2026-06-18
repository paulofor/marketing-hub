package com.marketinghub.targeting.service;

import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.TargetingOption;
import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.TargetingResolutionJob;
import com.marketinghub.targeting.TargetingResolutionJobStatus;
import com.marketinghub.targeting.dto.TargetingCandidateResolutionUpdateRequest;
import com.marketinghub.targeting.mapper.TargetingResolutionSummaryMapper;
import com.marketinghub.repository.jpa.targeting.TargetingCandidateRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.repository.jpa.targeting.TargetingRequestRepository;
import com.marketinghub.repository.jpa.targeting.TargetingResolutionJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa o fluxo de solicitações de targeting e materialização de públicos Meta aprovados.
 */
@ExtendWith(MockitoExtension.class)
class TargetingRequestServiceTest {

    @Mock
    private TargetingRequestRepository requestRepository;
    @Mock
    private TargetingCandidateRepository candidateRepository;
    @Mock
    private TargetingElementRepository targetingElementRepository;
    @Mock
    private TargetingResolutionJobRepository resolutionJobRepository;
    @Mock
    private TargetingResolutionJobService resolutionJobService;
    @Mock
    private TargetingResolutionSummaryMapper resolutionSummaryMapper;
    @Mock
    private MarketNicheRepository nicheRepository;
    @Mock
    private HypothesisRepository hypothesisRepository;

    private TargetingRequestService service;

    /** Prepara o serviço com dependências simuladas para cada teste. */
    @BeforeEach
    void setUp() {
        service = new TargetingRequestService(
                requestRepository,
                candidateRepository,
                targetingElementRepository,
                resolutionJobRepository,
                resolutionJobService,
                resolutionSummaryMapper,
                nicheRepository,
                hypothesisRepository
        );
    }

    /** Verifica que resolução validada conclui o job com sucesso e limpa erro anterior. */
    @Test
    void applyResolutionShouldUpdateJobAsSucceededAndClearLastError() {
        Long candidateId = 10L;
        TargetingRequest request = TargetingRequest.builder().id(UUID.randomUUID()).descricao("desc").build();
        TargetingCandidate candidate = TargetingCandidate.builder()
                .id(candidateId)
                .request(request)
                .seed("marketing")
                .type(TargetingCandidateType.INTEREST)
                .status(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH)
                .options(new LinkedHashSet<>(Set.of(TargetingOption.builder().facebookId("legacy").name("legacy").candidate(null).build())))
                .build();
        TargetingResolutionJob job = TargetingResolutionJob.builder()
                .candidate(candidate)
                .request(request)
                .status(TargetingResolutionJobStatus.PROCESSING)
                .lastError("timeout")
                .build();

        TargetingCandidateResolutionUpdateRequest payload = new TargetingCandidateResolutionUpdateRequest();
        payload.setStatus(TargetingCandidateStatus.VALIDATED);
        payload.setOptions(List.of(option("123", "Option 1"), option("456", "Option 2")));

        when(candidateRepository.findDetailedById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(TargetingCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resolutionJobRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(job));

        service.applyResolution(candidateId, payload);

        ArgumentCaptor<TargetingResolutionJob> jobCaptor = ArgumentCaptor.forClass(TargetingResolutionJob.class);
        verify(resolutionJobRepository).save(jobCaptor.capture());
        TargetingResolutionJob savedJob = jobCaptor.getValue();

        assertThat(savedJob.getStatus()).isEqualTo(TargetingResolutionJobStatus.SUCCEEDED);
        assertThat(savedJob.getLastError()).isNull();
        assertThat(savedJob.getResultCount()).isEqualTo(2);
        assertThat(savedJob.getStartedAt()).isNotNull();
        assertThat(savedJob.getFinishedAt()).isNotNull();
        assertThat(savedJob.getFinishedAt()).isAfterOrEqualTo(savedJob.getStartedAt());
    }


    /** Verifica que opções Meta validadas viram elementos aprovados disponíveis para experimentos. */
    @Test
    void applyResolutionShouldMaterializeValidatedMetaOptionsAsApprovedTargetingElements() {
        Long candidateId = 12L;
        MarketNiche niche = MarketNiche.builder().id(22L).name("Nicho unhas").build();
        TargetingRequest request = TargetingRequest.builder()
                .id(UUID.randomUUID())
                .descricao("desc")
                .niche(niche)
                .build();
        TargetingCandidate candidate = TargetingCandidate.builder()
                .id(candidateId)
                .request(request)
                .seed("manicure")
                .type(TargetingCandidateType.WORK_POSITION)
                .status(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH)
                .rationale("Profissional do nicho")
                .build();
        TargetingResolutionJob job = TargetingResolutionJob.builder()
                .candidate(candidate)
                .request(request)
                .status(TargetingResolutionJobStatus.PROCESSING)
                .build();
        TargetingCandidateResolutionUpdateRequest.OptionPayload option = option("meta-1", "Manicure");
        option.setType(TargetingCandidateType.WORK_POSITION);
        option.setAudienceSize(12345L);

        TargetingCandidateResolutionUpdateRequest payload = new TargetingCandidateResolutionUpdateRequest();
        payload.setStatus(TargetingCandidateStatus.VALIDATED);
        payload.setOptions(List.of(option));

        when(candidateRepository.findDetailedById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(TargetingCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetingElementRepository.findFirstByNicheIdAndTypeAndMetaId(22L, TargetingElementType.JOB_TITLE, "meta-1"))
                .thenReturn(Optional.empty());
        when(resolutionJobRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(job));

        service.applyResolution(candidateId, payload);

        ArgumentCaptor<TargetingElement> elementCaptor = ArgumentCaptor.forClass(TargetingElement.class);
        verify(targetingElementRepository).save(elementCaptor.capture());
        TargetingElement savedElement = elementCaptor.getValue();

        assertThat(savedElement.getNiche()).isEqualTo(niche);
        assertThat(savedElement.getType()).isEqualTo(TargetingElementType.JOB_TITLE);
        assertThat(savedElement.getStatus()).isEqualTo(TargetingElementStatus.APPROVED);
        assertThat(savedElement.getTerm()).isEqualTo("Manicure");
        assertThat(savedElement.getMetaId()).isEqualTo("meta-1");
        assertThat(savedElement.getMetaKey()).isEqualTo("Manicure");
        assertThat(savedElement.getMetaAudienceSizeLowerBound()).isEqualTo(12345L);
        assertThat(savedElement.getMetaAudienceSizeUpperBound()).isEqualTo(12345L);
    }

    /** Verifica que falha técnica mantém o candidato pendente e marca o job como falho. */
    @Test
    void applyResolutionShouldMarkJobFailedForTechnicalFailureStatus() {
        Long candidateId = 11L;
        TargetingRequest request = TargetingRequest.builder().id(UUID.randomUUID()).descricao("desc").build();
        TargetingCandidate candidate = TargetingCandidate.builder()
                .id(candidateId)
                .request(request)
                .seed("marketing")
                .type(TargetingCandidateType.INTEREST)
                .status(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH)
                .build();
        Instant startedAt = Instant.now().minusSeconds(30);
        TargetingResolutionJob job = TargetingResolutionJob.builder()
                .candidate(candidate)
                .request(request)
                .status(TargetingResolutionJobStatus.PROCESSING)
                .startedAt(startedAt)
                .build();

        TargetingCandidateResolutionUpdateRequest payload = new TargetingCandidateResolutionUpdateRequest();
        payload.setStatus(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH);
        payload.setRejectionReason("worker timeout");

        when(candidateRepository.findDetailedById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(TargetingCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resolutionJobRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(job));

        service.applyResolution(candidateId, payload);

        ArgumentCaptor<TargetingResolutionJob> jobCaptor = ArgumentCaptor.forClass(TargetingResolutionJob.class);
        verify(resolutionJobRepository).save(jobCaptor.capture());
        TargetingResolutionJob savedJob = jobCaptor.getValue();

        assertThat(savedJob.getStatus()).isEqualTo(TargetingResolutionJobStatus.FAILED);
        assertThat(savedJob.getLastError()).isEqualTo("worker timeout");
        assertThat(savedJob.getResultCount()).isZero();
        assertThat(savedJob.getFinishedAt()).isNotNull();
        assertThat(savedJob.getStartedAt()).isEqualTo(startedAt);
    }

    /** Monta uma opção Meta mínima para os testes de resolução. */
    private TargetingCandidateResolutionUpdateRequest.OptionPayload option(String facebookId, String name) {
        TargetingCandidateResolutionUpdateRequest.OptionPayload payload = new TargetingCandidateResolutionUpdateRequest.OptionPayload();
        payload.setFacebookId(facebookId);
        payload.setName(name);
        payload.setType(TargetingCandidateType.INTEREST);
        return payload;
    }
}
