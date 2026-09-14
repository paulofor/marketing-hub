package com.marketinghub.product.executionprofile.v1.service;

import com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileView.*;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract;
import java.math.BigDecimal;
import java.util.*;

/** Responsabilidade: resolver percurso e economia por capacidade sem depender do mineral. */
public final class ExecutionProfileRules {
  public static final List<String> PHASES =
      List.of(
          "pde-opportunity-discovery",
          "pde-commercial-plan-offer",
          "pde-construction-approval",
          "pde-communication-sales-journey",
          "pde-commercial-homologation-activation",
          "pde-sales-delivery-learning");

  /** Impede instanciação de regras determinísticas. */
  private ExecutionProfileRules() {}

  /** Nomeia o percurso v1 pela capacidade efetivamente contratada. */
  public static String routeName(ProfileContract c) {
    return switch (c.capability()) {
      case PERSONALIZED_IMAGES -> "Imagens personalizadas · v1";
      case GUIDED_EXPERIENCE -> "Experiência guiada · v1";
      case AI_TOOL -> "Ferramenta com IA · v1";
      case DIGITAL_PACKAGE -> "Pacote digital · v1";
    };
  }

  /** Detalha trabalhos especializados dentro das mesmas seis etapas da cadeia. */
  public static List<Work> work(ProfileContract c) {
    String input =
        switch (c.capability()) {
          case PERSONALIZED_IMAGES -> "Definir personalização e referências autorizadas";
          case GUIDED_EXPERIENCE -> "Desenhar jornada e primeiro resultado útil";
          case AI_TOOL -> "Definir entradas, personalização e limites da ferramenta";
          case DIGITAL_PACKAGE -> "Definir organização e uso do pacote";
        };
    String production =
        switch (c.capability()) {
          case PERSONALIZED_IMAGES -> "Produzir e revisar o pacote de imagens personalizadas";
          case GUIDED_EXPERIENCE -> "Construir ações práticas e continuidade";
          case AI_TOOL -> "Implementar transformação, qualidade e recuperação";
          case DIGITAL_PACKAGE -> "Produzir e conferir os entregáveis do pacote";
        };
    return List.of(
        new Work(
            PHASES.get(0),
            "discovery",
            "Comprovar dor e resultado comprado",
            "Argos",
            List.of(c.purchasedOutcome(), "Preservar fontes e hipóteses concorrentes"),
            true,
            "Comum a todas as entregas"),
        new Work(
            PHASES.get(1),
            "economics",
            "Revisar oferta e economia do pacote",
            "Plutus",
            List.of("Três cenários, custo completo, margem e limites", "Checkpoint OFFER"),
            true,
            "Proteção financeira obrigatória"),
        new Work(
            PHASES.get(2),
            "journey",
            input,
            "Dédalo",
            c.inputs(),
            true,
            "Selecionada pela capacidade de entrega"),
        new Work(
            PHASES.get(2),
            "deliverables",
            production,
            "Dédalo",
            java.util.stream.Stream.concat(c.deliverables().stream(), c.qualityCriteria().stream())
                .toList(),
            true,
            "Checkpoint DELIVERY_DESIGN e limites antes da produção"),
        new Work(
            PHASES.get(2),
            "audiovisual",
            "Produzir audiovisual previsto no contrato",
            "Apolo",
            List.of("Somente material exigido pela entrega aprovada"),
            c.audiovisualRequired(),
            c.audiovisualRequired()
                ? "O contrato inclui audiovisual"
                : "O contrato não inclui audiovisual; dispensa não comprova objetivo"),
        new Work(
            PHASES.get(2),
            "access",
            "Validar acesso, entrega e recuperação",
            "Dédalo",
            List.of(
                c.deliveryMode(),
                "Isolamento por cliente, versão e pacote",
                "Falhas, retomada e entrega aproveitável"),
            true,
            "Comum aos formatos, sem exigir webapp"),
        new Work(
            PHASES.get(3),
            "communication",
            "Comunicar resultado, prova e limites reais",
            "Íris",
            List.of("Mensagem fiel ao resultado comprado", c.revenueModel()),
            true,
            "Comunicação eficaz sem promessa inventada"),
        new Work(
            PHASES.get(4),
            "homologation",
            "Homologar qualidade, economia e autorizações",
            "Têmis / Plutus / responsável humano",
            List.of(
                "Checkpoint HOMOLOGATION",
                "Preservar todos os gates humanos",
                "Desktop e dispositivos aplicáveis à entrega"),
            true,
            "Qualidade e aprovação obrigatórias"),
        new Work(
            PHASES.get(5),
            "operation",
            "Conciliar venda, entrega, satisfação e contribuição",
            "Hermes / Plutus",
            List.of(
                "Checkpoint OPERATION",
                "Entrega aproveitável, compra e contribuição por pacote",
                "Satisfação medida; ausência de fonte não é zero"),
            true,
            "Nenhuma projeção equivale a venda"));
  }

  /** Calcula custo integral por pacote com todas as tentativas cobertas pelo teto. */
  public static List<ScenarioResult> economics(ProfileContract c) {
    return c.scenarios().stream()
        .map(
            s -> {
              BigDecimal fullCost =
                  c.maximumDeliveryCostBrl()
                      .add(s.acquisitionBrl())
                      .add(s.feesBrl())
                      .add(s.supportBrl())
                      .add(s.storageDeliveryBrl())
                      .add(s.otherCostsBrl());
              BigDecimal contribution = s.priceBrl().subtract(fullCost);
              return new ScenarioResult(
                  s.code().name(),
                  s.priceBrl(),
                  fullCost,
                  contribution,
                  contribution.compareTo(c.minimumContributionBrl()) >= 0);
            })
        .toList();
  }

  /** Expõe incoerências e inviabilidade sem corrigir os números fornecidos silenciosamente. */
  public static List<String> blockers(ProfileContract c) {
    List<String> issues = new ArrayList<>();
    boolean noVariableCost = ProfileContract.NO_VARIABLE_AI_COST.equals(c.costModel());
    if (noVariableCost
        ? c.maximumAttemptCostBrl().signum() != 0 || c.maximumDeliveryCostBrl().signum() != 0
        : c.maximumAttemptCostBrl().signum() <= 0 || c.maximumDeliveryCostBrl().signum() <= 0)
      issues.add(
          "Declare entrega sem consumo variável de IA ou informe custos positivos do modelo de entrega.");
    var production = c.productionBudget();
    if (production.maximumAttempts() == 0) {
      if (production.maximumAttemptCostBrl().signum() != 0
          || production.maximumTotalCostBrl().signum() != 0)
        issues.add("Produção privada sem tentativas deve ter orçamento zero explícito.");
    } else if (production.maximumAttemptCostBrl().signum() <= 0
        || production.maximumTotalCostBrl().signum() <= 0
        || ProfileContract.NO_VARIABLE_AI_COST.equals(production.costModel())
        || production
                .maximumAttemptCostBrl()
                .multiply(BigDecimal.valueOf(production.maximumAttempts()))
                .compareTo(production.maximumTotalCostBrl())
            > 0) {
      issues.add(
          "O orçamento privado deve cobrir todas as tentativas de produção no modelo declarado.");
    }
    if (c.maximumAttempts() < c.includedUnits())
      issues.add("Tentativas máximas menores que a quantidade prometida.");
    if (c.maximumAttemptCostBrl()
            .multiply(BigDecimal.valueOf(c.maximumAttempts()))
            .compareTo(c.maximumDeliveryCostBrl())
        > 0) issues.add("O teto de entrega não cobre o pacote e todas as regenerações permitidas.");
    if (c.scenarios().stream().map(ProfileContract.Scenario::code).distinct().count() != 3)
      issues.add("Informe uma vez cada cenário: favorável, base e conservador.");
    if (economics(c).stream().anyMatch(s -> !s.viable()))
      issues.add("A contribuição mínima não é atendida nos três cenários.");
    return List.copyOf(issues);
  }
}
