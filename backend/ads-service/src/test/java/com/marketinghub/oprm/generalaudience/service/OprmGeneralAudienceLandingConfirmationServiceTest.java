package com.marketinghub.oprm.generalaudience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.dto.CreateLeadPortalFlowRequest;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.landingConfirmation.CreateGeneralAudienceLandingConfirmationRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudiencePainAngleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Valida a criação de landing/formulário de confirmação para públicos gerais OPRM. */
class OprmGeneralAudienceLandingConfirmationServiceTest {

    /** Verifica se o formulário criado confirma público, dor, entrega e próximo passo sem publicar campanha. */
    @Test
    void shouldCreateConfirmationFlowWithMandatoryQuestions() {
        OprmGeneralAudiencePainAngle angle = approvedAngle(convertedSubniche());
        OprmGeneralAudiencePainAngleRepository angleRepository = angleRepositoryReturning(angle);
        LeadPortalFlowService leadPortalFlowService = leadPortalFlowServiceCreatingFlow();
        OprmGeneralAudienceLandingConfirmationService service = new OprmGeneralAudienceLandingConfirmationService(
                angleRepository,
                leadPortalFlowService,
                experimentRepositoryReturning(99L));

        var response = service.createConfirmationFlow(20L, new CreateGeneralAudienceLandingConfirmationRequest(
                77L,
                null,
                "Você trabalha como manicure hoje?",
                List.of("sim, atendo em casa", "sim, tenho espaço próprio", "não sou manicure"),
                null,
                null,
                null,
                null));

        assertThat(response.leadPortalFlowId()).isEqualTo(88L);
        assertThat(response.experimentId()).isEqualTo(77L);
        assertThat(response.deliveryDescription()).isEqualTo("kit com 12 mensagens de WhatsApp");
        assertThat(response.nextStep()).contains("receber a isca");
        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).required()).isTrue();
        assertThat(response.questions().get(0).options()).contains("não sou manicure");
        assertThat(response.questions().get(1).dataKey()).isEqualTo("pain_confirmation");
    }

    /** Verifica se a landing/formulário é bloqueada sem opções de qualificação suficientes. */
    @Test
    void shouldBlockConfirmationFlowWithoutQualificationOptions() {
        OprmGeneralAudiencePainAngle angle = approvedAngle(convertedSubniche());
        OprmGeneralAudienceLandingConfirmationService service = new OprmGeneralAudienceLandingConfirmationService(
                angleRepositoryReturning(angle),
                mock(LeadPortalFlowService.class),
                experimentRepositoryReturning(99L));

        assertThatThrownBy(() -> service.createConfirmationFlow(20L, new CreateGeneralAudienceLandingConfirmationRequest(
                77L,
                null,
                "Você trabalha como manicure hoje?",
                List.of("sim"),
                null,
                null,
                null,
                null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("qualificationOptions");
    }

    /** Verifica se o formulário é bloqueado antes da conversão controlada para MarketNiche. */
    @Test
    void shouldBlockConfirmationFlowBeforeMarketNicheConversion() {
        OprmGeneralAudiencePainAngle angle = approvedAngle(subniche());
        OprmGeneralAudienceLandingConfirmationService service = new OprmGeneralAudienceLandingConfirmationService(
                angleRepositoryReturning(angle),
                mock(LeadPortalFlowService.class),
                experimentRepositoryReturning(99L));

        assertThatThrownBy(() -> service.createConfirmationFlow(20L, new CreateGeneralAudienceLandingConfirmationRequest(
                77L,
                null,
                "Você trabalha como manicure hoje?",
                List.of("sim", "não"),
                null,
                null,
                null,
                null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("convertido em MarketNiche");
    }

    /** Monta repositório de experimento coerente com o MarketNiche convertido. */
    private ExperimentRepository experimentRepositoryReturning(Long marketNicheId) {
        ExperimentRepository repository = mock(ExperimentRepository.class);
        Experiment experiment = new Experiment();
        experiment.setId(77L);
        MarketNiche niche = new MarketNiche();
        niche.setId(marketNicheId);
        experiment.setNiche(niche);
        when(repository.findById(77L)).thenReturn(Optional.of(experiment));
        when(repository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return repository;
    }

    /** Monta repositório que retorna o ângulo informado. */
    private OprmGeneralAudiencePainAngleRepository angleRepositoryReturning(OprmGeneralAudiencePainAngle angle) {
        OprmGeneralAudiencePainAngleRepository repository = mock(OprmGeneralAudiencePainAngleRepository.class);
        when(repository.findById(20L)).thenReturn(Optional.of(angle));
        return repository;
    }

    /** Monta serviço do Lead Portal que devolve fluxo com perguntas persistidas. */
    private LeadPortalFlowService leadPortalFlowServiceCreatingFlow() {
        LeadPortalFlowService service = mock(LeadPortalFlowService.class);
        when(service.create(any(CreateLeadPortalFlowRequest.class))).thenAnswer(invocation -> {
            CreateLeadPortalFlowRequest request = invocation.getArgument(0);
            LeadPortalFlow flow = new LeadPortalFlow();
            flow.setId(88L);
            flow.setName(request.getName());
            flow.setSlug(request.getSlug());
            flow.setDescription(request.getDescription());
            flow.setQuestions(request.getQuestions().stream().map(questionRequest -> {
                LeadPortalFlowQuestion question = new LeadPortalFlowQuestion();
                question.setTitle(questionRequest.getTitle());
                question.setDataKey(questionRequest.getDataKey());
                question.setType(questionRequest.getType());
                question.setRequired(questionRequest.isRequired());
                question.setOptions(questionRequest.getOptions() == null ? List.of() : questionRequest.getOptions());
                return question;
            }).toList());
            return flow;
        });
        return service;
    }

    /** Monta um ângulo aprovado com isca e pergunta de confirmação. */
    private OprmGeneralAudiencePainAngle approvedAngle(OprmGeneralAudienceSubniche subniche) {
        OprmGeneralAudiencePainAngle angle = new OprmGeneralAudiencePainAngle();
        angle.setId(20L);
        angle.setSubniche(subniche);
        angle.setPain("Agenda vazia durante a semana");
        angle.setDesiredResult("Reativar clientes antigas");
        angle.setMechanismDirection("a dor percebida é horário vazio e dinheiro perdido");
        angle.setProofOrLeadMagnet("kit com 12 mensagens de WhatsApp");
        angle.setSafePromise("reativar clientes antigas pelo WhatsApp");
        angle.setLandingConfirmationQuestion("Você trabalha como manicure hoje?");
        angle.setStatus(OprmGeneralAudiencePainAngleStatus.APPROVED);
        return angle;
    }

    /** Monta subnicho convertido para MarketNiche. */
    private OprmGeneralAudienceSubniche convertedSubniche() {
        OprmGeneralAudienceSubniche subniche = subniche();
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE);
        subniche.setMarketNicheId(99L);
        return subniche;
    }

    /** Monta um subnicho de público geral com pergunta qualificadora. */
    private OprmGeneralAudienceSubniche subniche() {
        OprmGeneralAudienceSeed seed = new OprmGeneralAudienceSeed();
        seed.setId(1L);
        seed.setName("Beleza");
        OprmGeneralAudienceSubniche subniche = new OprmGeneralAudienceSubniche();
        subniche.setId(5L);
        subniche.setSeed(seed);
        subniche.setName("Manicure autônoma");
        subniche.setQualificationQuestion("Você trabalha como manicure hoje?");
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.APPROVED_FOR_EXPERIMENT);
        return subniche;
    }
}
