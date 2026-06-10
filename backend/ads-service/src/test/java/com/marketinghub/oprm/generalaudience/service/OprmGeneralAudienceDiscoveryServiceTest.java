package com.marketinghub.oprm.generalaudience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceAdSignalStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceAdSignalType;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceFacebookAdsData;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSourceEvidence;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.CreateGeneralAudienceHypothesisRequest;
import com.marketinghub.oprm.generalaudience.service.createLeadExperiment.CreateGeneralAudienceLeadExperimentRequest;
import com.marketinghub.oprm.generalaudience.service.createPainAngle.CreateGeneralAudiencePainAngleRequest;
import com.marketinghub.oprm.generalaudience.service.prepareTargeting.GeneralAudienceTargetingPreparationRequest;
import com.marketinghub.oprm.generalaudience.service.createSourceEvidence.CreateGeneralAudienceSourceEvidenceRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceFacebookAdsDataRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceHypothesisMaterializationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceLeadExperimentMaterializationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceMaterializedLeadExperiment;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceMaterializedHypothesis;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudiencePainAngleRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSourceEvidenceRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSubnicheRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Valida o pipeline de descoberta de públicos gerais sem contaminar o fluxo NichoCNAE. */
class OprmGeneralAudienceDiscoveryServiceTest {

    /** Verifica se o cadastro de ângulo bloqueia dor genérica antes de avançar no pipeline. */
    @Test
    void shouldRejectGenericPainAngle() {
        OprmGeneralAudienceDiscoveryService service = serviceWith(subnicheRepositoryReturning(subniche()));

        assertThatThrownBy(() -> service.createPainAngle(5L, new CreateGeneralAudiencePainAngleRequest(
                        "beleza",
                        "Ter mais clientes",
                        null,
                        null,
                        "Organizar a agenda sem promessa absoluta",
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pain genérico demais");
    }

    /** Verifica se o cadastro de ângulo bloqueia promessa absoluta ou arriscada. */
    @Test
    void shouldRejectRiskyPromise() {
        OprmGeneralAudienceDiscoveryService service = serviceWith(subnicheRepositoryReturning(subniche()));

        assertThatThrownBy(() -> service.createPainAngle(5L, new CreateGeneralAudiencePainAngleRequest(
                        "Agenda vazia durante a semana",
                        "Reativar clientes antigas",
                        "Mensagens de WhatsApp",
                        "Kit de mensagens",
                        "Agenda cheia garantida",
                        null,
                        "Você trabalha como manicure hoje?",
                        null,
                        null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("promessa arriscada");
    }

    /** Verifica se o quality gate aprova somente quando há dor, pergunta, evidência e ângulo aprovado. */
    @Test
    void shouldApproveQualityGateWithEvidenceAndApprovedAngle() {
        OprmGeneralAudienceSubniche subniche = subniche();
        OprmGeneralAudienceSubnicheRepository subnicheRepository = subnicheRepositoryReturning(subniche);
        OprmGeneralAudiencePainAngleRepository angleRepository = mock(OprmGeneralAudiencePainAngleRepository.class);
        OprmGeneralAudienceSourceEvidenceRepository evidenceRepository = mock(OprmGeneralAudienceSourceEvidenceRepository.class);
        when(angleRepository.countBySubnicheIdAndStatusIn(5L, Set.of(OprmGeneralAudiencePainAngleStatus.APPROVED)))
                .thenReturn(1L);
        when(evidenceRepository.countBySubnicheId(5L)).thenReturn(1L);
        OprmGeneralAudienceDiscoveryService service = new OprmGeneralAudienceDiscoveryService(
                mock(OprmGeneralAudienceSeedRepository.class),
                subnicheRepository,
                angleRepository,
                evidenceRepository,
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class),
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class),
                mock(OprmGeneralAudienceFacebookAdsDataRepository.class));

        var response = service.evaluateQualityGate(5L);

        assertThat(response.approved()).isTrue();
        assertThat(response.blockers()).isEmpty();
    }

    /** Verifica se evidência de outro seed é bloqueada para preservar rastreabilidade correta. */
    @Test
    void shouldRejectEvidenceForSubnicheFromAnotherSeed() {
        OprmGeneralAudienceSeed seed = seed(1L);
        OprmGeneralAudienceSubniche subniche = subniche();
        subniche.setSeed(seed(2L));
        OprmGeneralAudienceSeedRepository seedRepository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceSubnicheRepository subnicheRepository = subnicheRepositoryReturning(subniche);
        when(seedRepository.findById(1L)).thenReturn(Optional.of(seed));
        OprmGeneralAudienceDiscoveryService service = new OprmGeneralAudienceDiscoveryService(
                seedRepository,
                subnicheRepository,
                mock(OprmGeneralAudiencePainAngleRepository.class),
                mock(OprmGeneralAudienceSourceEvidenceRepository.class),
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class),
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class),
                mock(OprmGeneralAudienceFacebookAdsDataRepository.class));

        assertThatThrownBy(() -> service.createSourceEvidence(1L, new CreateGeneralAudienceSourceEvidenceRequest(
                        5L,
                        "https://example.com/forum",
                        "example.com",
                        "FORUM",
                        "Manicures relatam agenda vazia em dias úteis.",
                        Instant.parse("2026-06-10T12:00:00Z"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não pertence à semente");
    }

    /** Verifica se a criação de hipótese usa a dor principal e bloqueia dependência de CNAE. */
    @Test
    void shouldCreateHypothesisFromApprovedPainAngle() {
        OprmGeneralAudienceSubniche subniche = subniche();
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE);
        subniche.setMarketNicheId(99L);
        OprmGeneralAudiencePainAngle angle = approvedAngle(subniche);
        OprmGeneralAudiencePainAngleRepository angleRepository = mock(OprmGeneralAudiencePainAngleRepository.class);
        OprmGeneralAudienceHypothesisMaterializationRepository hypothesisRepository =
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class);
        when(angleRepository.findById(20L)).thenReturn(Optional.of(angle));
        when(hypothesisRepository.createHypothesis(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new OprmGeneralAudienceMaterializedHypothesis(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "Hipótese Público Geral - Manicure autônoma",
                        "BACKLOG",
                        Instant.parse("2026-06-10T13:00:00Z")));
        OprmGeneralAudienceDiscoveryService service = new OprmGeneralAudienceDiscoveryService(
                mock(OprmGeneralAudienceSeedRepository.class),
                subnicheRepositoryReturning(subniche),
                angleRepository,
                mock(OprmGeneralAudienceSourceEvidenceRepository.class),
                hypothesisRepository,
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class),
                mock(OprmGeneralAudienceFacebookAdsDataRepository.class));

        var response = service.createHypothesis(20L, new CreateGeneralAudienceHypothesisRequest(null, null, null));

        assertThat(response.hypothesisId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(response.marketNicheId()).isEqualTo(99L);
        assertThat(response.statement()).contains("Manicure autônoma com Agenda vazia durante a semana");
        assertThat(response.statement()).contains("do que a uma mensagem genérica");
    }

    /** Verifica se hipótese é bloqueada antes da conversão controlada para MarketNiche. */
    @Test
    void shouldBlockHypothesisBeforeMarketNicheConversion() {
        OprmGeneralAudienceSubniche subniche = subniche();
        OprmGeneralAudiencePainAngle angle = approvedAngle(subniche);
        OprmGeneralAudiencePainAngleRepository angleRepository = mock(OprmGeneralAudiencePainAngleRepository.class);
        when(angleRepository.findById(20L)).thenReturn(Optional.of(angle));
        OprmGeneralAudienceDiscoveryService service = new OprmGeneralAudienceDiscoveryService(
                mock(OprmGeneralAudienceSeedRepository.class),
                subnicheRepositoryReturning(subniche),
                angleRepository,
                mock(OprmGeneralAudienceSourceEvidenceRepository.class),
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class),
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class),
                mock(OprmGeneralAudienceFacebookAdsDataRepository.class));

        assertThatThrownBy(() -> service.createHypothesis(
                20L,
                new CreateGeneralAudienceHypothesisRequest(null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("convertido em MarketNiche");
    }

    /** Verifica se a criação do experimento de lead exige pacote curto e sem venda direta. */
    @Test
    void shouldCreateLeadExperimentFromApprovedPainAngle() {
        OprmGeneralAudienceSubniche subniche = subniche();
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE);
        subniche.setMarketNicheId(99L);
        OprmGeneralAudiencePainAngle angle = approvedAngle(subniche);
        angle.setLandingConfirmationQuestion("Você trabalha como manicure hoje?");
        OprmGeneralAudiencePainAngleRepository angleRepository = mock(OprmGeneralAudiencePainAngleRepository.class);
        OprmGeneralAudienceLeadExperimentMaterializationRepository experimentRepository =
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class);
        when(angleRepository.findById(20L)).thenReturn(Optional.of(angle));
        when(experimentRepository.createLeadExperiment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new OprmGeneralAudienceMaterializedLeadExperiment(
                        77L,
                        "Lead Público Geral - Manicure autônoma",
                        "PLANNED",
                        "CPL de lead qualificado",
                        new BigDecimal("12.00"),
                        new BigDecimal("30.00"),
                        LocalDate.parse("2026-06-10"),
                        LocalDate.parse("2026-06-12")));
        OprmGeneralAudienceDiscoveryService service = new OprmGeneralAudienceDiscoveryService(
                mock(OprmGeneralAudienceSeedRepository.class),
                subnicheRepositoryReturning(subniche),
                angleRepository,
                mock(OprmGeneralAudienceSourceEvidenceRepository.class),
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class),
                experimentRepository,
                mock(OprmGeneralAudienceFacebookAdsDataRepository.class));

        var response = service.createLeadExperiment(20L, new CreateGeneralAudienceLeadExperimentRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null,
                "CPL de lead qualificado",
                new BigDecimal("12.00"),
                new BigDecimal("30.00"),
                3,
                new BigDecimal("8.00"),
                30));

        assertThat(response.experimentId()).isEqualTo(77L);
        assertThat(response.status()).isEqualTo("PLANNED");
        assertThat(response.stopLossCpl()).isEqualByComparingTo("12.00");
        assertThat(response.dailyBudget()).isEqualByComparingTo("30.00");
    }

    /** Verifica se orçamento grande é bloqueado para manter o teste inicial pequeno. */
    @Test
    void shouldBlockLeadExperimentWithLargeBudget() {
        OprmGeneralAudienceSubniche subniche = subniche();
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE);
        subniche.setMarketNicheId(99L);
        OprmGeneralAudiencePainAngle angle = approvedAngle(subniche);
        OprmGeneralAudiencePainAngleRepository angleRepository = mock(OprmGeneralAudiencePainAngleRepository.class);
        when(angleRepository.findById(20L)).thenReturn(Optional.of(angle));
        OprmGeneralAudienceDiscoveryService service = new OprmGeneralAudienceDiscoveryService(
                mock(OprmGeneralAudienceSeedRepository.class),
                subnicheRepositoryReturning(subniche),
                angleRepository,
                mock(OprmGeneralAudienceSourceEvidenceRepository.class),
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class),
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class),
                mock(OprmGeneralAudienceFacebookAdsDataRepository.class));

        assertThatThrownBy(() -> service.createLeadExperiment(20L, new CreateGeneralAudienceLeadExperimentRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null,
                "CPL de lead qualificado",
                new BigDecimal("12.00"),
                new BigDecimal("150.00"),
                3,
                null,
                null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("orçamento pequeno");
    }

    /** Verifica se o targeting inicial exige JOB_TITLE aprovado/resolvido para evitar público amplo puro. */
    @Test
    void shouldPrepareConservativeTargetingAndBlockPureBroadAudience() {
        OprmGeneralAudienceSubniche subniche = subniche();
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE);
        subniche.setMarketNicheId(99L);
        OprmGeneralAudiencePainAngle angle = approvedAngle(subniche);
        OprmGeneralAudiencePainAngleRepository angleRepository = mock(OprmGeneralAudiencePainAngleRepository.class);
        OprmGeneralAudienceFacebookAdsDataRepository facebookAdsDataRepository = facebookAdsDataRepositoryAssigningIds(false);
        when(angleRepository.findById(20L)).thenReturn(Optional.of(angle));
        OprmGeneralAudienceDiscoveryService service = new OprmGeneralAudienceDiscoveryService(
                mock(OprmGeneralAudienceSeedRepository.class),
                subnicheRepositoryReturning(subniche),
                angleRepository,
                mock(OprmGeneralAudienceSourceEvidenceRepository.class),
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class),
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class),
                facebookAdsDataRepository);

        var response = service.prepareInitialTargeting(20L, new GeneralAudienceTargetingPreparationRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                List.of("Manicure"),
                List.of(),
                List.of("Esmaltação"),
                List.of("Atendimento por WhatsApp"),
                "Você trabalha como manicure hoje?",
                "Mulheres adultas quando fizer sentido para o subnicho",
                "Confirmar profissão no formulário",
                false,
                "operador"));

        assertThat(response.publishableForCurrentPublisher()).isFalse();
        assertThat(response.elements()).hasSize(3);
        assertThat(response.elements()).extracting("status").contains(OprmGeneralAudienceAdSignalStatus.RECEIVED);
        assertThat(response.blockers()).anyMatch(blocker -> blocker.contains("Nenhum JOB_TITLE aprovado"));
    }

    /** Verifica se um JOB_TITLE já aprovado e resolvido libera o targeting conservador para o publicador atual. */
    @Test
    void shouldMarkTargetingPublishableWhenResolvedApprovedJobTitleExists() {
        OprmGeneralAudienceSubniche subniche = subniche();
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE);
        subniche.setMarketNicheId(99L);
        OprmGeneralAudiencePainAngle angle = approvedAngle(subniche);
        OprmGeneralAudiencePainAngleRepository angleRepository = mock(OprmGeneralAudiencePainAngleRepository.class);
        OprmGeneralAudienceFacebookAdsDataRepository facebookAdsDataRepository = facebookAdsDataRepositoryAssigningIds(true);
        when(angleRepository.findById(20L)).thenReturn(Optional.of(angle));
        OprmGeneralAudienceDiscoveryService service = new OprmGeneralAudienceDiscoveryService(
                mock(OprmGeneralAudienceSeedRepository.class),
                subnicheRepositoryReturning(subniche),
                angleRepository,
                mock(OprmGeneralAudienceSourceEvidenceRepository.class),
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class),
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class),
                facebookAdsDataRepository);

        var response = service.prepareInitialTargeting(20L, new GeneralAudienceTargetingPreparationRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                List.of("Manicure"),
                List.of("6000000000001"),
                List.of("Esmaltação"),
                List.of(),
                "Você trabalha como manicure hoje?",
                null,
                "Confirmar profissão no formulário",
                true,
                "operador"));

        assertThat(response.publishableForCurrentPublisher()).isTrue();
        assertThat(response.blockers()).isEmpty();
        assertThat(response.elements()).anyMatch(element -> element.type() == OprmGeneralAudienceAdSignalType.JOB_TITLE
                && element.publishableForCurrentPublisher());
    }

    /** Monta serviço com repositório de subnicho e repositórios auxiliares mockados. */
    private OprmGeneralAudienceDiscoveryService serviceWith(OprmGeneralAudienceSubnicheRepository subnicheRepository) {
        OprmGeneralAudiencePainAngleRepository angleRepository = mock(OprmGeneralAudiencePainAngleRepository.class);
        when(angleRepository.save(any())).thenAnswer(invocation -> {
            OprmGeneralAudiencePainAngle angle = invocation.getArgument(0);
            angle.setId(20L);
            angle.setCreatedAt(Instant.parse("2026-06-10T12:00:00Z"));
            angle.setUpdatedAt(Instant.parse("2026-06-10T12:00:00Z"));
            return angle;
        });
        OprmGeneralAudienceSourceEvidenceRepository evidenceRepository = mock(OprmGeneralAudienceSourceEvidenceRepository.class);
        when(evidenceRepository.save(any())).thenAnswer(invocation -> {
            OprmGeneralAudienceSourceEvidence evidence = invocation.getArgument(0);
            evidence.setId(30L);
            return evidence;
        });
        return new OprmGeneralAudienceDiscoveryService(
                mock(OprmGeneralAudienceSeedRepository.class),
                subnicheRepository,
                angleRepository,
                evidenceRepository,
                mock(OprmGeneralAudienceHypothesisMaterializationRepository.class),
                mock(OprmGeneralAudienceLeadExperimentMaterializationRepository.class),
                mock(OprmGeneralAudienceFacebookAdsDataRepository.class));
    }

    /** Monta repositório que retorna um subnicho específico. */
    private OprmGeneralAudienceSubnicheRepository subnicheRepositoryReturning(OprmGeneralAudienceSubniche subniche) {
        OprmGeneralAudienceSubnicheRepository repository = mock(OprmGeneralAudienceSubnicheRepository.class);
        when(repository.findById(5L)).thenReturn(Optional.of(subniche));
        return repository;
    }

    /** Monta repositório de dados do Facebook Ads que devolve itens como se fossem persistidos. */
    private OprmGeneralAudienceFacebookAdsDataRepository facebookAdsDataRepositoryAssigningIds(boolean resolveMetaForReadyJobs) {
        OprmGeneralAudienceFacebookAdsDataRepository repository = mock(OprmGeneralAudienceFacebookAdsDataRepository.class);
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            List<OprmGeneralAudienceFacebookAdsData> items = invocation.getArgument(0);
            for (int index = 0; index < items.size(); index++) {
                OprmGeneralAudienceFacebookAdsData item = items.get(index);
                item.setId((long) index + 1L);
                if (resolveMetaForReadyJobs
                        && item.getSignalType() == OprmGeneralAudienceAdSignalType.JOB_TITLE
                        && item.isReadyForFacebookAds()) {
                    item.setStatus(OprmGeneralAudienceAdSignalStatus.READY_FOR_FACEBOOK_ADS);
                }
            }
            return items;
        });
        return repository;
    }

    /** Monta um ângulo aprovado com dor, isca e mecanismo para criação de hipótese. */
    private OprmGeneralAudiencePainAngle approvedAngle(OprmGeneralAudienceSubniche subniche) {
        OprmGeneralAudiencePainAngle angle = new OprmGeneralAudiencePainAngle();
        angle.setId(20L);
        angle.setSubniche(subniche);
        angle.setPain("Agenda vazia durante a semana");
        angle.setDesiredResult("Reativar clientes antigas");
        angle.setMechanismDirection("a dor percebida é horário vazio e dinheiro perdido");
        angle.setProofOrLeadMagnet("kit com 12 mensagens de WhatsApp");
        angle.setSafePromise("reativar clientes antigas pelo WhatsApp");
        angle.setStatus(OprmGeneralAudiencePainAngleStatus.APPROVED);
        return angle;
    }

    /** Monta uma semente mínima para testes. */
    private OprmGeneralAudienceSeed seed(Long id) {
        OprmGeneralAudienceSeed seed = new OprmGeneralAudienceSeed();
        seed.setId(id);
        seed.setName("Beleza");
        return seed;
    }

    /** Monta um subnicho com dados suficientes para quality gate. */
    private OprmGeneralAudienceSubniche subniche() {
        OprmGeneralAudienceSubniche subniche = new OprmGeneralAudienceSubniche();
        subniche.setId(5L);
        subniche.setSeed(seed(1L));
        subniche.setName("Manicure autônoma");
        subniche.setPersonaSummary("Profissional que atende com agenda própria");
        subniche.setPainSummary("Agenda vazia durante a semana");
        subniche.setDesiredOutcomeSummary("Reativar clientes antigas");
        subniche.setChannelsSummary("WhatsApp e Instagram");
        subniche.setQualificationQuestion("Você trabalha como manicure hoje?");
        return subniche;
    }
}
