package com.marketinghub.oprmcoletormei.opportunity.service;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeCycleUpsertRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeEnrichmentRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityCandidateDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreResponseDto;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente responsável por chamar apenas APIs OPRM do backend para leitura e gravação do fluxo CNAE de oportunidade.
 */
@Component
public class OprmCnaeOpportunityBackendClient {
    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com propriedades do coletor e cliente HTTP compartilhado. */
    public OprmCnaeOpportunityBackendClient(OprmMarketImportCollectorProperties collectorProperties, RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Busca CNAEs sem score para o ciclo automático de score. */
    public List<OprmCnaeOpportunityCandidateDto> findMissingScores(int limit) {
        OprmCnaeOpportunityCandidateDto[] response = restClient.get()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/cnaes/opportunity-scores/missing?limit=" + limit)
                .retrieve()
                .body(OprmCnaeOpportunityCandidateDto[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    /** Grava score calculado pelo OPRM no backend. */
    public void saveScore(String cnaeCode, OprmCnaeOpportunityScoreRequestDto request) {
        restClient.put()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/cnaes/" + cnaeCode + "/opportunity-score")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    /** Busca os melhores scores ainda não enriquecidos para o ciclo automático de enriquecimento. */
    public List<OprmCnaeOpportunityScoreResponseDto> findTopScores(int limit) {
        OprmCnaeOpportunityScoreResponseDto[] response = restClient.get()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/cnaes/opportunity-scores/top?notEnriched=true&limit=" + limit)
                .retrieve()
                .body(OprmCnaeOpportunityScoreResponseDto[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    /** Cria ou atualiza ciclo operacional no backend. */
    public void upsertCycle(OprmCnaeCycleUpsertRequestDto request) {
        restClient.post()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/cnae-cycles")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    /** Obtém o próximo número sequencial de ciclo para o tipo informado. */
    public Long nextCycleNumber(String cycleType) {
        Long response = restClient.get()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/cnae-cycles/next-number?cycleType=" + cycleType)
                .retrieve()
                .body(Long.class);
        return response == null ? 1L : response;
    }

    /** Publica enriquecimento e candidatos de nicho no backend. */
    public void saveEnrichment(OprmCnaeEnrichmentRequestDto request) {
        restClient.post()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/cnae-enrichments")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
