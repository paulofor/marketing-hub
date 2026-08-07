package com.marketinghub.productai.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.productai.PersonalizedSampleFunnelTemplate;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.productai.dto.PersonalizedSampleFunnelDto;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  private static final Logger log =
      LoggerFactory.getLogger(ProductAiPersonalizedSampleFunnelService.class);
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
  public PersonalizedSampleFunnelDto createOrUpdate(
      Long experimentId, PersonalizedSampleFunnelTemplate requestedTemplate) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found"));
    validateExperiment(experiment);

    LeadPortalFlow flow = resolveFlow(experiment);
    flow.setName("Produto IA - amostra personalizada - exp " + experiment.getId());
    flow.setSlug(resolveSlug(flow, experiment.getId()));
    PersonalizedSampleFunnelTemplate template =
        requestedTemplate == null ? PersonalizedSampleFunnelTemplate.GENERIC : requestedTemplate;
    flow.setDescription(buildDescription(experiment, template));
    flow.setMarketNiche(experiment.getNiche());
    flow.setExperiment(experiment);
    flow.setSchemaFirst(true);
    flow.setModel("AI_PERSONALIZED_SAMPLE_FUNNEL");
    reconcileQuestions(flow, template);
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
          HttpStatus.BAD_REQUEST, "experiment must be AI_PERSONALIZED_SAMPLE");
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
    return leadPortalFlowRepository
        .findAllByExperimentIdOrderByCreatedAtDesc(experiment.getId())
        .stream()
        .filter(
            flow ->
                flow.getSlug() != null
                    && flow.getSlug().startsWith(SLUG_PREFIX + experiment.getId()))
        .findFirst()
        .orElseGet(LeadPortalFlow::new);
  }

  /** Define slug estável e evita colisão quando o slug antigo pertence a outro fluxo. */
  private String resolveSlug(LeadPortalFlow flow, Long experimentId) {
    String base = SLUG_PREFIX + experimentId + "-personalized-sample";
    if (Objects.equals(flow.getSlug(), base)) {
      return base;
    }
    return leadPortalFlowRepository
        .findBySlug(base)
        .filter(existing -> !Objects.equals(existing.getId(), flow.getId()))
        .map(existing -> base + "-" + System.currentTimeMillis())
        .orElse(base);
  }

  /** Descreve a finalidade comercial do funil para auditoria operacional. */
  private String buildDescription(
      Experiment experiment, PersonalizedSampleFunnelTemplate template) {
    String promise =
        StringUtils.hasText(experiment.getFunnelPromise())
            ? experiment.getFunnelPromise()
            : experiment.getHypothesis();
    if (!StringUtils.hasText(promise)) {
      promise = "gerar uma amostra visual personalizada antes da compra";
    }
    return "Funil canônico para coletar dados do lead e permitir geração de amostra personalizada: "
        + promise.trim()
        + ". Template: "
        + template.name();
  }

  /**
   * Reconcilia as perguntas pela chave canônica, preservando IDs e respostas históricas em novas
   * execuções do mesmo comando.
   */
  private void reconcileQuestions(LeadPortalFlow flow, PersonalizedSampleFunnelTemplate template) {
    List<QuestionSpec> specs =
        switch (template) {
          case SOCIAL_MEDIA_MICRO_SAMPLE -> buildSocialMediaMicroSampleQuestionSpecs();
          case DECORATION_BY_PHOTO -> buildDecorationByPhotoQuestionSpecs();
          case GENERIC -> buildGenericQuestionSpecs();
        };
    Map<String, LeadPortalFlowQuestion> existingByDataKey = new LinkedHashMap<>();
    for (LeadPortalFlowQuestion question : flow.getQuestions()) {
      existingByDataKey.putIfAbsent(question.getDataKey(), question);
    }
    Map<String, QuestionSpec> specsByDataKey = new LinkedHashMap<>();
    specs.forEach(spec -> specsByDataKey.put(spec.dataKey(), spec));
    flow.getQuestions().removeIf(question -> !specsByDataKey.containsKey(question.getDataKey()));
    for (int index = 0; index < specs.size(); index++) {
      QuestionSpec spec = specs.get(index);
      LeadPortalFlowQuestion question = existingByDataKey.get(spec.dataKey());
      if (question == null) {
        question = LeadPortalFlowQuestion.builder().flow(flow).dataKey(spec.dataKey()).build();
        flow.getQuestions().add(question);
      }
      applyQuestionSpec(question, flow, spec, index);
    }
    flow.getQuestions().sort(Comparator.comparingInt(LeadPortalFlowQuestion::getPosition));
  }

  /** Atualiza uma pergunta existente ou nova com o contrato atual do template. */
  private void applyQuestionSpec(
      LeadPortalFlowQuestion question, LeadPortalFlow flow, QuestionSpec spec, int position) {
    question.setFlow(flow);
    question.setTitle(spec.title());
    question.setDataKey(spec.dataKey());
    question.setType(spec.type());
    question.setRequired(spec.required());
    question.setDescription(spec.description());
    question.setPlaceholder(spec.description());
    question.setPosition(position);
    question.getOptions().clear();
    question.getOptions().addAll(spec.options());
  }

  /** Monta o formulário padrão para Produto IA com amostra personalizada. */
  private List<QuestionSpec> buildGenericQuestionSpecs() {
    return List.of(
        new QuestionSpec(
            "Qual é o seu e-mail?",
            "email",
            LeadPortalQuestionType.EMAIL,
            true,
            "Canal para enviar a amostra e continuar o relacionamento.",
            List.of()),
        new QuestionSpec(
            "Qual é o nome do seu negócio ou perfil e o que você publica hoje?",
            "negocio_projeto",
            LeadPortalQuestionType.TEXTAREA,
            true,
            "Contexto principal para personalizar o post.",
            List.of()),
        new QuestionSpec(
            "Qual serviço você quer divulgar e qual dificuldade enfrenta para atrair clientes?",
            "contexto_atual",
            LeadPortalQuestionType.TEXTAREA,
            true,
            "Define a oferta e a dor que o post precisa comunicar.",
            List.of()),
        new QuestionSpec(
            "Qual ação você quer que a cliente tome ao ver o post?",
            "objetivo_visual",
            LeadPortalQuestionType.TEXTAREA,
            true,
            "Exemplo: pedir horários disponíveis pelo WhatsApp.",
            List.of()),
        new QuestionSpec(
            "Quais detalhes, cores e informações precisam aparecer?",
            "dados_personalizacao",
            LeadPortalQuestionType.TEXTAREA,
            true,
            "Inclua WhatsApp, cidade, estilo e qualquer informação que deva estar no post ou na legenda.",
            List.of()));
  }

  /** Monta o template de três decisões comerciais para post e story personalizados. */
  private List<QuestionSpec> buildSocialMediaMicroSampleQuestionSpecs() {
    return List.of(
        new QuestionSpec(
            "Qual é o seu nome profissional?",
            "nome_profissional",
            LeadPortalQuestionType.TEXT,
            true,
            "O nome que deve aparecer no post e no story.",
            List.of()),
        new QuestionSpec(
            "Qual serviço você quer divulgar?",
            "servico_divulgado",
            LeadPortalQuestionType.TEXT,
            true,
            "Exemplo: alongamento em gel, banho de gel ou manutenção.",
            List.of()),
        new QuestionSpec(
            "Qual estilo combina mais com o seu trabalho?",
            "estilo_visual",
            LeadPortalQuestionType.SINGLE_CHOICE,
            true,
            "Escolha uma identidade para a sua amostra.",
            List.of("Elegante e minimalista", "Delicada e romântica", "Marcante e moderna")),
        new QuestionSpec(
            "Qual é o seu e-mail?",
            "email",
            LeadPortalQuestionType.EMAIL,
            true,
            "Usaremos apenas para entregar a amostra e acompanhar sua solicitação.",
            List.of()),
        new QuestionSpec(
            "Envie uma foto de referência, se quiser.",
            "foto_referencia",
            LeadPortalQuestionType.IMAGE_UPLOAD,
            false,
            "Opcional. A amostra também pode ser criada sem foto.",
            List.of()));
  }

  /** Monta o formulário especializado do piloto DecoraIA Express por foto de ambiente. */
  private List<QuestionSpec> buildDecorationByPhotoQuestionSpecs() {
    return List.of(
        new QuestionSpec(
            "Qual é o seu nome?",
            "nome",
            LeadPortalQuestionType.TEXT,
            true,
            "Usado para personalizar a entrega visual.",
            List.of()),
        new QuestionSpec(
            "Qual é o seu e-mail?",
            "email",
            LeadPortalQuestionType.EMAIL,
            true,
            "Canal para enviar a amostra e continuar o relacionamento.",
            List.of()),
        new QuestionSpec(
            "Qual é o seu WhatsApp?",
            "whatsapp",
            LeadPortalQuestionType.PHONE,
            true,
            "Canal operacional para entrega e suporte.",
            List.of()),
        new QuestionSpec(
            "Envie uma foto do ambiente que você quer melhorar.",
            "foto_ambiente",
            LeadPortalQuestionType.IMAGE_UPLOAD,
            true,
            "A foto é o insumo principal para o diagnóstico visual personalizado.",
            List.of()),
        new QuestionSpec(
            "Qual ambiente você quer transformar?",
            "ambiente_a_transformar",
            LeadPortalQuestionType.TEXT,
            true,
            "Exemplos: sala, quarto, home office, cozinha, varanda ou loja.",
            List.of()),
        new QuestionSpec(
            "O que mais incomoda nesse ambiente?",
            "incomodo_principal",
            LeadPortalQuestionType.TEXTAREA,
            true,
            "Ajuda a IA a priorizar a dor visual e funcional do lead.",
            List.of()),
        new QuestionSpec(
            "Que resultado visual você quer enxergar?",
            "objetivo_visual",
            LeadPortalQuestionType.TEXTAREA,
            true,
            "Exemplos: mais bonito, funcional, aconchegante, moderno ou organizado.",
            List.of()),
        new QuestionSpec(
            "Quanto você pretende gastar aproximadamente?",
            "orcamento_aproximado",
            LeadPortalQuestionType.TEXT,
            true,
            "Permite sugerir melhorias compatíveis com o orçamento declarado.",
            List.of()),
        new QuestionSpec(
            "Quais detalhes precisam ser considerados?",
            "dados_personalizacao",
            LeadPortalQuestionType.TEXTAREA,
            true,
            "Inclua medidas aproximadas, móveis que devem ficar, restrições e preferências.",
            List.of()),
        new QuestionSpec(
            "Existe algum estilo de decoração que você prefere?",
            "preferencias_visuais",
            LeadPortalQuestionType.TEXTAREA,
            false,
            "Exemplos: moderno, minimalista, rústico, industrial, escandinavo ou colorido.",
            List.of()));
  }

  /** Publica o fluxo no Lead Portal sem esconder falhas de integração. */
  private void publish(LeadPortalFlow flow) {
    try {
      leadPortalFlowPublisher.publish(flow);
    } catch (LeadPortalPublicationException ex) {
      log.error(
          "Falha ao publicar funil Produto IA no Lead Portal: flowId={}, slug={}",
          flow.getId(),
          flow.getSlug(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "failed to publish product AI funnel", ex);
    }
  }

  /** Converte o fluxo persistido no contrato administrativo retornado pela API. */
  private PersonalizedSampleFunnelDto toDto(LeadPortalFlow flow, Long experimentId) {
    List<String> keys =
        flow.getQuestions().stream()
            .sorted(Comparator.comparingInt(LeadPortalFlowQuestion::getPosition))
            .map(LeadPortalFlowQuestion::getDataKey)
            .map(key -> key.toLowerCase(Locale.ROOT))
            .toList();
    return new PersonalizedSampleFunnelDto(
        experimentId, flow.getId(), flow.getSlug(), flow.isApproved(), flow.getApprovedAt(), keys);
  }

  /** Especificação interna de pergunta canônica do funil de personalização. */
  private record QuestionSpec(
      String title,
      String dataKey,
      LeadPortalQuestionType type,
      boolean required,
      String description,
      List<String> options) {}
}
