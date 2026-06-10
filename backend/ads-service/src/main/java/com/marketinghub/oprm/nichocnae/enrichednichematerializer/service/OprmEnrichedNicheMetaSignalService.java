package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementSource;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Serviço responsável por transformar nicho enriquecido OPRM em sinais iniciais pesquisáveis pela Meta Ads. */
@Service
public class OprmEnrichedNicheMetaSignalService {
  private static final int MAX_INTERESTS = 12;
  private static final int MAX_ROLES = 8;
  private static final int MAX_BEHAVIORS = 6;

  private final TargetingElementRepository targetingElementRepository;

  /** Inicializa o serviço com o repositório oficial de elementos de segmentação. */
  public OprmEnrichedNicheMetaSignalService(TargetingElementRepository targetingElementRepository) {
    this.targetingElementRepository = targetingElementRepository;
  }

  /** Monta o pacote de sinais Meta Ads a partir do CNAE, nome neutro e evidências do cartão de rotina. */
  public MetaSignalPackage buildSignalPackage(OprmRoutineResearchCycle cycle, OprmNicheRoutineCard card) {
    LinkedHashSet<String> interests = new LinkedHashSet<>();
    LinkedHashSet<String> roles = new LinkedHashSet<>();
    LinkedHashSet<String> behaviors = new LinkedHashSet<>();

    addIfText(interests, cycle == null ? null : cycle.getCnaeDescription());
    addIfText(interests, cycle == null ? null : cycle.getNeutralNicheName());
    addIfText(interests, cycle == null ? null : cycle.getNicheName());
    addDictionarySignals(cycle == null ? null : cycle.getCnaeCode(), interests, roles, behaviors);
    addOperationalTextSignals(card, interests, roles, behaviors);
    addDefaultCommercialSignals(interests, behaviors);

    return new MetaSignalPackage(
        limit(interests, MAX_INTERESTS),
        limit(roles, MAX_ROLES),
        limit(behaviors, MAX_BEHAVIORS));
  }

  /** Aplica o pacote de sinais nos campos legados/listas do nicho para consulta e auditoria humana. */
  public void applySignalsToNiche(MarketNiche niche, MetaSignalPackage signalPackage) {
    if (niche == null || signalPackage == null) {
      return;
    }
    niche.setInterestList(signalPackage.interests());
    niche.setRoleList(signalPackage.roles());
    niche.setBehaviorList(signalPackage.behaviors());
    niche.setInterests(buildReadableSignalSummary(signalPackage));
  }

  /** Publica elementos de segmentação aprovados para que o facebook-ads-worker resolva IDs e alcance na Graph API. */
  @Transactional
  public void publishTargetingElements(
      MarketNiche niche,
      MarketNicheEnrichmentProfile profile,
      MetaSignalPackage signalPackage) {
    if (niche == null || niche.getId() == null || signalPackage == null) {
      return;
    }
    List<TargetingElement> existing = targetingElementRepository.findByNicheId(niche.getId());
    Map<String, TargetingElement> existingByKey = new HashMap<>();
    for (TargetingElement element : existing) {
      if (element.getHypothesis() == null && StringUtils.hasText(element.getTerm())) {
        existingByKey.put(key(element.getType(), element.getTerm()), element);
      }
    }

    List<TargetingElement> toPersist = new ArrayList<>();
    addElements(niche, profile, TargetingElementType.INTEREST, signalPackage.interests(), existingByKey, toPersist);
    addElements(niche, profile, TargetingElementType.JOB_TITLE, signalPackage.roles(), existingByKey, toPersist);
    addElements(niche, profile, TargetingElementType.BEHAVIOR, signalPackage.behaviors(), existingByKey, toPersist);
    if (!toPersist.isEmpty()) {
      targetingElementRepository.saveAll(toPersist);
    }
  }

  /** Adiciona sinais de dicionários por CNAE para os melhores nichos já ranqueados pelo OPRM. */
  private void addDictionarySignals(
      String cnaeCode,
      Set<String> interests,
      Set<String> roles,
      Set<String> behaviors) {
    if (!StringUtils.hasText(cnaeCode)) {
      return;
    }
    switch (cnaeCode.trim()) {
      case "9602501" -> addBeautySignals(interests, roles, behaviors);
      case "4781400" -> addFashionRetailSignals(interests, roles, behaviors);
      case "7319002" -> addSalesPromotionSignals(interests, roles, behaviors);
      case "4399103" -> addMasonrySignals(interests, roles, behaviors);
      case "8219999" -> addAdministrativeServicesSignals(interests, roles, behaviors);
      case "4923002" -> addPassengerTransportSignals(interests, roles, behaviors);
      case "9602502" -> addAestheticServicesSignals(interests, roles, behaviors);
      case "9700500" -> addDomesticServicesSignals(interests, roles, behaviors);
      default -> { }
    }
  }

  /** Adiciona sinais específicos de beleza e salão. */
  private void addBeautySignals(Set<String> interests, Set<String> roles, Set<String> behaviors) {
    addAll(interests, "Salão de beleza", "Cabeleireiro", "Manicure", "Pedicure", "Unhas decoradas", "Barbearia", "Cosméticos profissionais", "Produtos para salão", "Sebrae");
    addAll(roles, "Cabeleireiro", "Manicure", "Pedicure", "Barbeiro", "Designer de sobrancelhas", "Proprietário de salão de beleza");
    addAll(behaviors, "Small business owners", "Engaged Shoppers", "Facebook Page admins");
  }

  /** Adiciona sinais específicos de varejo de moda. */
  private void addFashionRetailSignals(Set<String> interests, Set<String> roles, Set<String> behaviors) {
    addAll(interests, "Loja de roupas", "Moda feminina", "Moda masculina", "Boutique", "Varejo", "Instagram Shopping", "E-commerce", "Marketplace", "Atacado de roupas");
    addAll(roles, "Lojista", "Vendedor de loja", "Consultor de moda", "Proprietário de loja", "Empreendedor de moda");
    addAll(behaviors, "Small business owners", "Engaged Shoppers", "Facebook Page admins");
  }

  /** Adiciona sinais específicos de promoção e força de vendas. */
  private void addSalesPromotionSignals(Set<String> interests, Set<String> roles, Set<String> behaviors) {
    addAll(interests, "Vendas", "Marketing", "Trade marketing", "Prospecção", "CRM", "Funil de vendas", "Automação de vendas");
    addAll(roles, "Promotor de vendas", "Representante comercial", "Vendedor externo", "Consultor comercial", "Supervisor de vendas");
    addAll(behaviors, "Small business owners", "Facebook Page admins", "Engaged Shoppers");
  }

  /** Adiciona sinais específicos de obras e alvenaria. */
  private void addMasonrySignals(Set<String> interests, Set<String> roles, Set<String> behaviors) {
    addAll(interests, "Construção civil", "Reforma", "Materiais de construção", "Obra", "Arquitetura", "Engenharia civil", "Leroy Merlin", "Telhanorte");
    addAll(roles, "Pedreiro", "Mestre de obras", "Empreiteiro", "Construtor", "Profissional de construção civil");
    addAll(behaviors, "Small business owners", "Facebook Page admins", "Engaged Shoppers");
  }

  /** Adiciona sinais específicos de serviços administrativos. */
  private void addAdministrativeServicesSignals(Set<String> interests, Set<String> roles, Set<String> behaviors) {
    addAll(interests, "Administração", "Produtividade", "Excel", "Gestão de documentos", "Gestão financeira", "Google Workspace", "Microsoft 365", "Automação");
    addAll(roles, "Assistente administrativo", "Secretária", "Consultor administrativo", "Back office", "Despachante");
    addAll(behaviors, "Small business owners", "Facebook Page admins", "Business decision makers");
  }

  /** Adiciona sinais específicos de transporte de passageiros com motorista. */
  private void addPassengerTransportSignals(Set<String> interests, Set<String> roles, Set<String> behaviors) {
    addAll(interests, "Motorista particular", "Transporte executivo", "Aplicativo de transporte", "Uber", "99", "Turismo", "Viagens");
    addAll(roles, "Motorista", "Motorista particular", "Motorista profissional", "Condutor");
    addAll(behaviors, "Small business owners", "Frequent travelers", "Facebook Page admins");
  }

  /** Adiciona sinais específicos de estética e cuidados com beleza. */
  private void addAestheticServicesSignals(Set<String> interests, Set<String> roles, Set<String> behaviors) {
    addAll(interests, "Estética", "Clínica de estética", "Limpeza de pele", "Design de sobrancelhas", "Depilação", "Cosméticos", "Beleza");
    addAll(roles, "Esteticista", "Designer de sobrancelhas", "Depiladora", "Profissional de estética");
    addAll(behaviors, "Small business owners", "Engaged Shoppers", "Facebook Page admins");
  }

  /** Adiciona sinais específicos de serviços domésticos. */
  private void addDomesticServicesSignals(Set<String> interests, Set<String> roles, Set<String> behaviors) {
    addAll(interests, "Serviços domésticos", "Limpeza residencial", "Diarista", "Organização doméstica", "Casa e jardim");
    addAll(roles, "Diarista", "Empregada doméstica", "Faxineira", "Profissional de limpeza");
    addAll(behaviors, "Small business owners", "Facebook Page admins", "Engaged Shoppers");
  }

  /** Extrai sinais simples do texto operacional sem transformar dor em promessa ou oferta. */
  private void addOperationalTextSignals(
      OprmNicheRoutineCard card,
      Set<String> interests,
      Set<String> roles,
      Set<String> behaviors) {
    if (card == null) {
      return;
    }
    String text = String.join(
            "\n",
            safe(card.getRoutineSummary()),
            safe(card.getPainsSummary()),
            safe(card.getMechanismOpportunitiesSummary()))
        .toLowerCase(Locale.ROOT);
    if (text.contains("whatsapp")) {
      addIfText(interests, "WhatsApp Business");
    }
    if (text.contains("agenda") || text.contains("atendimento")) {
      addIfText(interests, "Agenda");
      addIfText(interests, "Atendimento ao cliente");
    }
    if (text.contains("venda") || text.contains("cliente")) {
      addIfText(interests, "Vendas");
      addIfText(behaviors, "Engaged Shoppers");
    }
    if (text.contains("instagram")) {
      addIfText(interests, "Instagram para negócios");
    }
  }

  /** Adiciona sinais comerciais de baixa contaminação úteis para profissionais autônomos e pequenos negócios. */
  private void addDefaultCommercialSignals(Set<String> interests, Set<String> behaviors) {
    addAll(interests, "Empreendedorismo", "Pequenos negócios", "WhatsApp Business", "Instagram para negócios");
    addAll(behaviors, "Small business owners", "Facebook Page admins");
  }

  /** Cria ou atualiza elementos de targeting do pacote OPRM sem apagar curadoria manual existente. */
  private void addElements(
      MarketNiche niche,
      MarketNicheEnrichmentProfile profile,
      TargetingElementType type,
      List<String> terms,
      Map<String, TargetingElement> existingByKey,
      List<TargetingElement> toPersist) {
    for (String term : terms) {
      String key = key(type, term);
      TargetingElement existing = existingByKey.get(key);
      if (existing != null) {
        if (existing.getSource() == TargetingElementSource.OPRM_NICHE) {
          existing.setDescription(buildDescription(profile));
          existing.setStatus(TargetingElementStatus.APPROVED);
          toPersist.add(existing);
        }
        continue;
      }
      toPersist.add(TargetingElement.builder()
          .niche(niche)
          .type(type)
          .term(term)
          .description(buildDescription(profile))
          .source(TargetingElementSource.OPRM_NICHE)
          .status(TargetingElementStatus.APPROVED)
          .build());
    }
  }

  /** Monta uma descrição auditável para o sinal gerado pelo nicho enriquecido. */
  private String buildDescription(MarketNicheEnrichmentProfile profile) {
    if (profile == null) {
      return "Sinal inicial Meta Ads gerado a partir de nicho enriquecido OPRM.";
    }
    return "Sinal inicial Meta Ads gerado a partir do nicho enriquecido OPRM "
        + profile.getCnaeCode()
        + " - "
        + profile.getNeutralNicheName()
        + ".";
  }

  /** Monta resumo textual dos sinais para exibição nos campos legados do nicho. */
  private String buildReadableSignalSummary(MetaSignalPackage signalPackage) {
    return String.join("\n",
        "Sinais iniciais Meta Ads gerados pelo OPRM NichoCNAE.",
        "Interesses: " + String.join(", ", signalPackage.interests()),
        "Cargos: " + String.join(", ", signalPackage.roles()),
        "Comportamentos: " + String.join(", ", signalPackage.behaviors()));
  }

  /** Gera chave estável por tipo e termo para evitar duplicidade dentro do nicho. */
  private String key(TargetingElementType type, String term) {
    return type.name() + "::" + term.trim().toLowerCase(Locale.ROOT);
  }

  /** Adiciona valores textuais válidos preservando a primeira ocorrência. */
  private void addAll(Set<String> target, String... values) {
    for (String value : values) {
      addIfText(target, value);
    }
  }

  /** Adiciona texto normalizado quando existe conteúdo útil. */
  private void addIfText(Set<String> target, String value) {
    if (StringUtils.hasText(value)) {
      target.add(value.trim());
    }
  }

  /** Limita a quantidade de sinais mantendo a ordem de prioridade. */
  private List<String> limit(LinkedHashSet<String> values, int max) {
    return values.stream().filter(StringUtils::hasText).limit(max).toList();
  }

  /** Retorna texto seguro para concatenação operacional. */
  private String safe(String value) {
    return value == null ? "" : value;
  }

  /** Pacote fechado de sinais iniciais que será persistido para resolução no Facebook Ads Worker. */
  public record MetaSignalPackage(List<String> interests, List<String> roles, List<String> behaviors) {}
}
