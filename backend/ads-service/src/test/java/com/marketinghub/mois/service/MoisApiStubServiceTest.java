package com.marketinghub.mois.service;

import com.marketinghub.mois.MoisDiscoveryRequestStatus;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.repository.MoisDiscoveryRequestRepository;
import com.marketinghub.mois.repository.MoisOfferCardRepository;
import com.marketinghub.mois.repository.MoisOfferFunnelPatternRepository;
import com.marketinghub.mois.repository.MoisOfferMechanismClaimRepository;
import com.marketinghub.mois.repository.MoisOfferProofSignalRepository;
import com.marketinghub.mois.repository.MoisOfferPromiseSignalRepository;
import com.marketinghub.mois.repository.MoisSourceSnapshotRepository;
import com.marketinghub.mois.service.MoisResearchGateway.MoisDiscoveredSource;
import com.marketinghub.mois.service.MoisResearchGateway.MoisResearchResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class MoisApiStubServiceTest {

    @Autowired
    private MoisApiStubService service;

    @Autowired
    private MoisDiscoveryRequestRepository discoveryRequestRepository;

    @Autowired
    private MoisSourceSnapshotRepository sourceSnapshotRepository;

    @Autowired
    private MoisOfferCardRepository offerCardRepository;

    @Autowired
    private MoisOfferPromiseSignalRepository promiseSignalRepository;

    @Autowired
    private MoisOfferProofSignalRepository proofSignalRepository;

    @Autowired
    private MoisOfferMechanismClaimRepository mechanismClaimRepository;

    @Autowired
    private MoisOfferFunnelPatternRepository funnelPatternRepository;

    @MockBean
    private MoisResearchGateway researchGateway;

    @Test
    void shouldPersistRealSnapshotsAndOffersAfterRun() {
        when(researchGateway.discoverSources(any(), anyList(), anyList()))
                .thenReturn(new MoisResearchResult(List.of(
                        new MoisDiscoveredSource(
                                "https://example.com/oferta-real",
                                "Oferta Real Teste",
                                "landing-page",
                                200,
                                "Texto normalizado da oferta real para sprint 3.",
                                "captured via test gateway",
                                true
                        )
                ), List.of()));

        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "personal trainer",
                        "retencao de alunos",
                        "agenda previsivel sem desconto",
                        List.of("personal trainer agenda cheia"),
                        List.of("https://example.com/oferta-real"),
                        List.of("landing"),
                        "BR",
                        "pt-BR",
                        Map.of("maxSources", 10)
                )
        );

        service.runDiscoveryRequest(accepted.requestId());

        assertThat(discoveryRequestRepository.findByRequestId(accepted.requestId())).isPresent();
        assertThat(sourceSnapshotRepository.countByRequest_RequestId(accepted.requestId())).isEqualTo(1);
        assertThat(offerCardRepository.countByRequest_RequestId(accepted.requestId())).isEqualTo(1);
        assertThat(promiseSignalRepository.countByRequest_RequestId(accepted.requestId())).isEqualTo(1);
        assertThat(proofSignalRepository.countByRequest_RequestId(accepted.requestId())).isEqualTo(1);
        assertThat(mechanismClaimRepository.countByRequest_RequestId(accepted.requestId())).isEqualTo(1);
        assertThat(funnelPatternRepository.countByRequest_RequestId(accepted.requestId())).isEqualTo(1);

        var persistedRequest = discoveryRequestRepository.findByRequestId(accepted.requestId()).orElseThrow();
        assertThat(persistedRequest.getStatus()).isEqualTo(MoisDiscoveryRequestStatus.COLLECTED);

        var detail = service.getDiscoveryRequest(accepted.requestId());
        assertThat(detail).isPresent();
        assertThat(detail.get().artifacts()).extracting(MoisDiscoveryDtos.ArtifactRefResponse::artifactType)
                .contains(
                        "mois.marketOfferDiscoveryRequest.v1",
                        "mois.marketOfferSourceSnapshot.v1",
                        "mois.marketOfferCard.v1",
                        "mois.marketOfferPromiseSignal.v1",
                        "mois.marketOfferProofSignal.v1",
                        "mois.marketOfferMechanismClaim.v1",
                        "mois.marketOfferFunnelPattern.v1"
                );
    }

    @Test
    void shouldMarkRequestAsFailedWhenNoSourceCollected() {
        when(researchGateway.discoverSources(any(), anyList(), anyList()))
                .thenReturn(new MoisResearchResult(List.of(), List.of("timeout on provider")));

        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "fisioterapia",
                        "captacao local",
                        null,
                        List.of("fisioterapia captação"),
                        List.of(),
                        List.of("landing"),
                        "BR",
                        "pt-BR",
                        Map.of()
                )
        );

        service.runDiscoveryRequest(accepted.requestId());

        var persistedRequest = discoveryRequestRepository.findByRequestId(accepted.requestId()).orElseThrow();
        assertThat(persistedRequest.getStatus()).isEqualTo(MoisDiscoveryRequestStatus.FAILED);
        assertThat(sourceSnapshotRepository.countByRequest_RequestId(accepted.requestId())).isEqualTo(1);
        assertThat(offerCardRepository.countByRequest_RequestId(accepted.requestId())).isZero();
    }
}
