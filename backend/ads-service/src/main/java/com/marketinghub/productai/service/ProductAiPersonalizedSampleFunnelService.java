package com.marketinghub.productai.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.productai.dto.PersonalizedSampleFunnelDto;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: criar e manter o funil de coleta para Produto IA com amostra personalizada. */
@Service
public class ProductAiPersonalizedSampleFunnelService {
    private static final Logger log = LoggerFactory.getLogger(ProductAiPersonalizedSampleFunnelService.class);
    private static final String SLUG_PREFIX = "product-ai-exp-";

    private final ExperimentRepository experimentRepository;
    private final LeadPortalFlowRepository leadPortalFlowRepository;
    private final LeadPortalFlowPublisher leadPortalFlowPublisher;

    /** Inicializa o serviço com repositórios e publicador do Lead Portal. */
    public ProductAiPersonalizedSampleFunnelService(
            ExperimentRepository experimentRepository,
            LeadPortalFlowRepository leadPortalFlowRepository,
            LeadPortalFlowPublisher leadPortalFlowPublisher) {
        this.experimentRepository = experimentRepository;
        this.leadPortalFlowRepository = leadPortalFlowRepository;
        this.leadPortalFlowPublisher = leadPortalFlowPublisher;
    }

    /** Cria ou normaliza o funil canônico de coleta de dados para o experimento Produto IA. */
    @Transactional
    public PersonalizedSampleFunnelDto createOrUpdate(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found"));
        validateExperiment(experiment);

        LeadPortalFlow flow = resolveFlow(experiment);
        flow.setName("Produto IA - amostra personalizada - exp " + experiment.getId());
        flow.setSlug(resolveSlug(flow, experiment.getId()));
        flow.setDescription(buildDescription(experiment));
        flow.setMarketNiche(experiment.getNiche());
        flow.setExperiment(experiment);
        flow.setSchemaFirst(true);
        flow.setModel("AI_PERSONALIZED_SAMPLE_FUNNEL");
        flow.getQuestions().clear();
        flow.getQuestions().addAll(buildQuestions(flow));
        flow.setApproved(true);
        if (flow.getApprovedAt() == null) {
            flow.setApprovedAt(Instant.now());
        }

        LeadPortalFlow saved = leadPortalFlowRepository.save(flow);
        experiment.setLeadPortalFlow(saved);
        experimentRepository.save(experiment);
        publish(saved);
        return toDto(saved, experiment.getId());
    }

    /** Bloqueia criação do funil quando o experimento não é o MVP de Produto IA personalizado. */
    private void validateExperiment(Experiment experiment) {
        if (experiment.getProductAiSubtype() != ProductAiSubtype.AI_PERSONALIZED_SAMPLE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "experiment must be AI_PERSONALIZED_SAMPLE");
        }
        if (experiment.getNiche() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "experiment niche is required");
        }
    }

    /** Reaproveita o fluxo canônico existente antes de criar um novo. */
    private LeadPortalFlow resolveFlow(Experiment experiment) {
        if (experiment.getLeadPortalFlow() != null) {
            return experiment.getLeadPortalFlow();
        }
        return leadPortalFlowRepository.findAllByExperimentIdOrderByCreatedAtDesc(experiment.getId()).stream()
                .filter(flow -> flow.getSlug() != null && flow.getSlug().startsWith(SLUG_PREFIX + experiment.getId()))
                .findFirst()
                .orElseGet(LeadPortalFlow::new);
    }

    /** Define slug estável e evita colisão quando o slug antigo pertence a outro fluxo. */
    private String resolveSlug(LeadPortalFlow flow, Long experimentId) {
        String base = SLUG_PREFIX + experimentId + "-personalized-sample";
        if (Objects.equals(flow.getSlug(), base)) {
            return base;
        }
        return leadPortalFlowRepository.findBySlug(base)
                .filter(existing -> !Objects.equals(existing.getId(), flow.getId()))
                .map(existing -> base + "-" + System.currentTimeMillis())
                .orElse(base);
    }

    /** Descreve a finalidade comercial do funil para auditoria operacional. */
    private String buildDescription(Experiment experiment) {
        String promise = StringUtils.hasText(experiment.getFunnelPromise())
                ? experiment.getFunnelPromise()
                : experiment.getHypothesis();
        if (!StringUtils.hasText(promise)) {
            promise = "gerar uma amostra visual personalizada antes da compra";
        }
        return "Funil canônico para coletar dados do lead e permitir geração de amostra personalizada: "
                + promise.trim();
    }

    /** Monta as perguntas mínimas necessárias para gerar algo exclusivo para o lead. */
    private List<LeadPortalFlowQuestion> buildQuestions(LeadPortalFlow flow) {
        List<QuestionSpec> specs = List.of(
                new QuestionSpec("Qual é o seu nome?", "nome", LeadPortalQuestionType.TEXT, true,
                        "Usado para personalizar a entrega visual."),
                new QuestionSpec("Qual é o seu e-mail?", "email", LeadPortalQuestionType.EMAIL, true,
                        "Canal para enviar a amostra e continuar o relacionamento."),
                new QuestionSpec("Qual é o seu WhatsApp?", "whatsapp", LeadPortalQuestionType.PHONE, true,
                        "Canal operacional para entrega e suporte."),
                new QuestionSpec("Qual negócio, projeto ou situação você quer melhorar?", "negocio_projeto",
                        LeadPortalQuestionType.TEXTAREA, true, "Contexto principal que aparecerá na amostra."),
                new QuestionSpec("Como está a situação hoje?", "contexto_atual", LeadPortalQuestionType.TEXTAREA,
                        true, "Ajuda a IA a entender o ponto de partida."),
                new QuestionSpec("Que resultado visual você quer enxergar na amostra?", "objetivo_visual",
                        LeadPortalQuestionType.TEXTAREA, true, "Define o depois desejado pelo lead."),
                new QuestionSpec("Quais detalhes precisam aparecer para parecer feito para você?",
                        "dados_personalizacao", LeadPortalQuestionType.TEXTAREA, true,
                        "Inclua nomes, cores, ambiente, produto, público ou referências próprias."),
                new QuestionSpec("Existe algum estilo visual que você prefere?", "preferencias_visuais",
                        LeadPortalQuestionType.TEXTAREA, false,
                        "Exemplos: moderno, simples, premium, feminino, técnico, colorido.")
        );
        List<LeadPortalFlowQuestion> questions = new ArrayList<>();
        for (int index = 0; index < specs.size(); index++) {
            QuestionSpec spec = specs.get(index);
            questions.add(LeadPortalFlowQuestion.builder()
                    .flow(flow)
                    .title(spec.title())
                    .dataKey(spec.dataKey())
                    .type(spec.type())
                    .required(spec.required())
                    .description(spec.description())
                    .placeholder(spec.description())
                    .position(index)
                    .options(new ArrayList<>())
                    .build());
        }
        return questions;
    }

    /** Publica o fluxo no Lead Portal sem esconder falhas de integração. */
    private void publish(LeadPortalFlow flow) {
        try {
            leadPortalFlowPublisher.publish(flow);
        } catch (LeadPortalPublicationException ex) {
            log.error("Falha ao publicar funil Produto IA no Lead Portal: flowId={}, slug={}",
                    flow.getId(), flow.getSlug(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to publish product AI funnel", ex);
        }
    }

    /** Converte o fluxo persistido no contrato administrativo retornado pela API. */
    private PersonalizedSampleFunnelDto toDto(LeadPortalFlow flow, Long experimentId) {
        List<String> keys = flow.getQuestions().stream()
                .sorted(Comparator.comparingInt(LeadPortalFlowQuestion::getPosition))
                .map(LeadPortalFlowQuestion::getDataKey)
                .map(key -> key.toLowerCase(Locale.ROOT))
                .toList();
        return new PersonalizedSampleFunnelDto(
                experimentId,
                flow.getId(),
                flow.getSlug(),
                flow.isApproved(),
                flow.getApprovedAt(),
                keys);
    }

    /** Especificação interna de pergunta canônica do funil de personalização. */
    private record QuestionSpec(
            String title,
            String dataKey,
            LeadPortalQuestionType type,
            boolean required,
            String description) {
    }
}
