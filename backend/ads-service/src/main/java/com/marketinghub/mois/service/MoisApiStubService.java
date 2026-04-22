package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisArtifactDtos;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MoisApiStubService {

    private static final MoisDiscoveryDtos.ArtifactRefResponse SAMPLE_ARTIFACT_REF =
            new MoisDiscoveryDtos.ArtifactRefResponse(
                    "mois-art-001",
                    "mois.marketOfferDiscoveryRequest.v1",
                    "v1"
            );

    public MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse createDiscoveryRequest(
            MoisDiscoveryDtos.CreateDiscoveryRequest request
    ) {
        return new MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse(
                UUID.randomUUID().toString(),
                "ACCEPTED"
        );
    }

    public MoisDiscoveryDtos.DiscoveryRequestListResponse listDiscoveryRequests(
            String status,
            String nicheName,
            String marketTheme
    ) {
        return new MoisDiscoveryDtos.DiscoveryRequestListResponse(List.of(sampleDiscoverySummary()));
    }

    public Optional<MoisDiscoveryDtos.DiscoveryRequestDetailResponse> getDiscoveryRequest(String requestId) {
        if (!"mois-req-001".equals(requestId)) {
            return Optional.empty();
        }
        return Optional.of(new MoisDiscoveryDtos.DiscoveryRequestDetailResponse(
                "mois-req-001",
                "personal trainer",
                "retencao de alunos",
                "agenda previsivel sem desconto",
                "DRAFT",
                Instant.parse("2026-04-22T00:00:00Z"),
                List.of(SAMPLE_ARTIFACT_REF)
        ));
    }

    public MoisDiscoveryDtos.AsyncAcceptedResponse runDiscoveryRequest(String requestId) {
        return new MoisDiscoveryDtos.AsyncAcceptedResponse("ACCEPTED", "mois-run-" + requestId);
    }

    public MoisOfferDtos.OfferCardListResponse listOffers(String requestId, String nicheName, String sellerOrBrand) {
        return new MoisOfferDtos.OfferCardListResponse(List.of(sampleOfferSummary()));
    }

    public Optional<MoisOfferDtos.OfferCardResponse> getOffer(String offerId) {
        if (!"mois-offer-001".equals(offerId)) {
            return Optional.empty();
        }

        return Optional.of(new MoisOfferDtos.OfferCardResponse(
                "mois-offer-001",
                "mois-req-001",
                "personal trainer",
                "Agenda Cheia Sem Desconto",
                "Studio Exemplo",
                "Agenda previsivel com onboarding estruturado",
                "mentoria",
                "R$ 1.497",
                0.79,
                List.of("diagnostico inicial", "check-ins semanais"),
                List.of("R$ 1.497 a vista", "12x R$ 149"),
                "Provas com depoimentos e antes/depois",
                "Mecanismo alegado de ciclo guiado de 8 semanas",
                "Captura por landing + WhatsApp",
                List.of(SAMPLE_ARTIFACT_REF)
        ));
    }

    public MoisInsightDtos.InsightReportListResponse listInsightReports(String requestId, String nicheName) {
        return new MoisInsightDtos.InsightReportListResponse(List.of(sampleInsightSummary()));
    }

    public Optional<MoisInsightDtos.InsightReportResponse> getInsightReport(String reportId) {
        if (!"mois-report-001".equals(reportId)) {
            return Optional.empty();
        }

        return Optional.of(new MoisInsightDtos.InsightReportResponse(
                "mois-report-001",
                "mois-req-001",
                "personal trainer",
                "retencao de alunos",
                "DRAFT",
                Instant.parse("2026-04-22T00:10:00Z"),
                List.of("resultado em 8 semanas"),
                List.of("depoimentos com antes/depois"),
                List.of("ticket medio entre R$ 1.200 e R$ 1.800"),
                List.of("lead capture via landing e follow-up no WhatsApp"),
                List.of("lacuna de oferta para onboarding de primeira semana"),
                List.of("validar diferencial de onboarding no proximo experimento")
        ));
    }

    public Optional<MoisArtifactDtos.ArtifactEnvelopeResponse> getArtifact(String artifactId) {
        if (!"mois-art-001".equals(artifactId)) {
            return Optional.empty();
        }

        return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                "mois-art-001",
                "mois.marketOfferDiscoveryRequest.v1",
                "v1",
                "DRAFT",
                "MOIS",
                Instant.parse("2026-04-22T00:00:00Z"),
                Instant.parse("2026-04-22T00:00:00Z"),
                Map.of("requestId", "mois-req-001"),
                Map.of("source", "stub"),
                Map.of("nicheName", "personal trainer", "marketTheme", "retencao de alunos")
        ));
    }

    private MoisDiscoveryDtos.DiscoveryRequestSummaryResponse sampleDiscoverySummary() {
        return new MoisDiscoveryDtos.DiscoveryRequestSummaryResponse(
                "mois-req-001",
                "personal trainer",
                "retencao de alunos",
                "agenda previsivel sem desconto",
                "DRAFT",
                Instant.parse("2026-04-22T00:00:00Z")
        );
    }

    private MoisOfferDtos.OfferCardSummaryResponse sampleOfferSummary() {
        return new MoisOfferDtos.OfferCardSummaryResponse(
                "mois-offer-001",
                "mois-req-001",
                "personal trainer",
                "Agenda Cheia Sem Desconto",
                "Studio Exemplo",
                "Agenda previsivel com onboarding estruturado",
                "mentoria",
                "R$ 1.497",
                0.79
        );
    }

    private MoisInsightDtos.InsightReportSummaryResponse sampleInsightSummary() {
        return new MoisInsightDtos.InsightReportSummaryResponse(
                "mois-report-001",
                "mois-req-001",
                "personal trainer",
                "retencao de alunos",
                "DRAFT",
                Instant.parse("2026-04-22T00:10:00Z")
        );
    }
}
