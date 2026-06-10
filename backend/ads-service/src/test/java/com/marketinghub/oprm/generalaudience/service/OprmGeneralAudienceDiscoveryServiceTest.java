package com.marketinghub.oprm.generalaudience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSourceEvidence;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.service.createPainAngle.CreateGeneralAudiencePainAngleRequest;
import com.marketinghub.oprm.generalaudience.service.createSourceEvidence.CreateGeneralAudienceSourceEvidenceRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudiencePainAngleRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSourceEvidenceRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSubnicheRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
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
                evidenceRepository);

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
                mock(OprmGeneralAudienceSourceEvidenceRepository.class));

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
                evidenceRepository);
    }

    /** Monta repositório que retorna um subnicho específico. */
    private OprmGeneralAudienceSubnicheRepository subnicheRepositoryReturning(OprmGeneralAudienceSubniche subniche) {
        OprmGeneralAudienceSubnicheRepository repository = mock(OprmGeneralAudienceSubnicheRepository.class);
        when(repository.findById(5L)).thenReturn(Optional.of(subniche));
        return repository;
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
