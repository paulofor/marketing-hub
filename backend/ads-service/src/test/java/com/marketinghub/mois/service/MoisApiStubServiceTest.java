package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.repository.MoisDiscoveryRequestRepository;
import com.marketinghub.mois.repository.MoisOfferCardRepository;
import com.marketinghub.mois.repository.MoisSourceSnapshotRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void shouldPersistRequestSnapshotAndOfferWithBasicLineage() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "personal trainer",
                        "retencao de alunos",
                        "agenda previsivel sem desconto",
                        List.of("personal trainer agenda cheia"),
                        List.of("https://example.com"),
                        List.of("landing"),
                        "BR",
                        "pt-BR",
                        Map.of("maxSources", 10)
                )
        );

        assertThat(discoveryRequestRepository.findByRequestId(accepted.requestId())).isPresent();
        assertThat(sourceSnapshotRepository.countByRequest_RequestId(accepted.requestId())).isGreaterThanOrEqualTo(1);
        assertThat(offerCardRepository.countByRequest_RequestId(accepted.requestId())).isGreaterThanOrEqualTo(1);

        var detail = service.getDiscoveryRequest(accepted.requestId());
        assertThat(detail).isPresent();
        assertThat(detail.get().artifacts()).extracting(MoisDiscoveryDtos.ArtifactRefResponse::artifactType)
                .contains("mois.marketOfferDiscoveryRequest.v1", "mois.marketOfferSourceSnapshot.v1", "mois.marketOfferCard.v1");
    }
}
