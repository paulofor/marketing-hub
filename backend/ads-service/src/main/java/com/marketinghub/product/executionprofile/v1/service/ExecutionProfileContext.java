package com.marketinghub.product.executionprofile.v1.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.executionprofile.v1.*;
import com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileView.*;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract;
import com.marketinghub.repository.jpa.planning.CommercialPlanVersionRepository;
import com.marketinghub.repository.jpa.productexecution.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: fornecer aos processos e agentes a ficha congelada e seus gates financeiros.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionProfileContext {
  private final ExecutionProfileRepository profiles;
  private final ExecutionProfileBindingRepository bindings;
  private final ExecutionProfileReviewRepository reviews;
  private final CommercialPlanVersionRepository planVersions;
  private final ObjectMapper json;

  /** Lê o contrato persistido sem usar dados atuais de outro produto ou revisão. */
  public ProfileContract contract(ExecutionProfile profile) {
    try {
      return json.readValue(profile.getContractJson(), ProfileContract.class);
    } catch (Exception ex) {
      log.error(
          "Ficha inválida. productId={} profileId={}", profile.getProductId(), profile.getId(), ex);
      throw new IllegalStateException("A ficha de execução possui contrato inválido.", ex);
    }
  }

  /** Lê a composição exata, incluindo versões dos subprocessos reutilizados. */
  public List<ProcessReference> composition(ExecutionProfile profile) {
    try {
      return json.readValue(
          profile.getCompositionJson(), new TypeReference<List<ProcessReference>>() {});
    } catch (Exception ex) {
      log.error(
          "Composição inválida. productId={} profileId={}",
          profile.getProductId(),
          profile.getId(),
          ex);
      throw new IllegalStateException("A composição da ficha é inválida.", ex);
    }
  }

  /** Localiza somente a ficha vinculada à referência exata. */
  @Transactional(readOnly = true)
  public Optional<ExecutionProfile> bound(Long productId, String sourceReference) {
    if (productId == null || sourceReference == null) return Optional.empty();
    return bindings
        .findByProductIdAndSourceReference(productId, sourceReference)
        .map(b -> profiles.findByIdAndProductId(b.getProfileId(), productId).orElseThrow());
  }

  /** Confirma que a referência congelou esta definição quando ela ainda estava publicada. */
  @Transactional(readOnly = true)
  public boolean pins(String reference, Long processId) {
    if (reference == null) return false;
    return bindings
        .findBySourceReference(reference)
        .map(b -> profiles.findByIdAndProductId(b.getProfileId(), b.getProductId()).orElseThrow())
        .map(p -> composition(p).stream().anyMatch(process -> process.id().equals(processId)))
        .orElse(false);
  }

  /** Rejeita troca silenciosa de cadeia ou ciclo de uma ficha já vinculada. */
  @Transactional(readOnly = true)
  public void requireScope(Long productId, String reference, Long chainId, Long cycleId) {
    bindings
        .findByProductIdAndSourceReference(productId, reference)
        .ifPresent(
            b -> {
              var profile =
                  profiles.findByIdAndProductId(b.getProfileId(), productId).orElseThrow();
              if (!Objects.equals(chainId, profile.getChainId())
                  || !Objects.equals(cycleId, b.getLearningCycleId()))
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "A ficha fixou outra cadeia ou ciclo para esta referência.");
            });
  }

  /** Resolve a ficha por referência global canônica para compor a entrada de tarefas BPM. */
  @Transactional(readOnly = true)
  public Optional<Map<String, Object>> taskContext(String reference) {
    return bindings
        .findBySourceReference(reference)
        .map(
            b -> {
              var profile =
                  profiles.findByIdAndProductId(b.getProfileId(), b.getProductId()).orElseThrow();
              return Map.<String, Object>of(
                  "contractVersion",
                  "PRODUCT_EXECUTION_PROFILE_V1",
                  "profileId",
                  profile.getId(),
                  "revision",
                  profile.getRevisionNumber(),
                  "productId",
                  profile.getProductId(),
                  "sourceReference",
                  reference,
                  "chainId",
                  profile.getChainId(),
                  "contract",
                  contract(profile),
                  "route",
                  ExecutionProfileRules.work(contract(profile)),
                  "processes",
                  composition(profile),
                  "financialCheckpoints",
                  decisions(profile));
            });
  }

  /**
   * Revalida a ficha na reserva do worker, inclusive quando a tarefa foi criada por outro comando.
   */
  @Transactional(readOnly = true)
  public String taskBlocker(
      Long processId, String processCode, String activityId, String reference) {
    var binding = bindings.findBySourceReference(reference);
    if (binding.isEmpty()) return null;
    var profile =
        profiles
            .findByIdAndProductId(binding.get().getProfileId(), binding.get().getProductId())
            .orElseThrow();
    if (composition(profile).stream().noneMatch(p -> p.id().equals(processId)))
      return "A tarefa pertence a outra versão de processo.";
    String checkpoint = checkpoint(processCode, activityId);
    return checkpoint == null ? null : financialBlocker(profile, checkpoint);
  }

  /** Expõe somente as quatro aprovações; o pedido de análise permanece no diário de auditoria. */
  public Map<String, Boolean> decisions(ExecutionProfile profile) {
    Map<String, Boolean> result = new LinkedHashMap<>();
    for (String checkpoint : List.of("OFFER", "DELIVERY_DESIGN", "HOMOLOGATION", "OPERATION"))
      result.put(checkpoint, false);
    reviews
        .findByProfileIdOrderByIdAsc(profile.getId())
        .forEach(
            r -> {
              if (result.containsKey(r.getCheckpoint()))
                result.put(r.getCheckpoint(), r.isApproved());
            });
    return result;
  }

  /** Bloqueia custo, contrato ou plano alterado sem invalidar a leitura do histórico. */
  public String financialBlocker(ExecutionProfile profile, String checkpoint) {
    var issues = ExecutionProfileRules.blockers(contract(profile));
    if (!issues.isEmpty()) return String.join(" ", issues);
    var current =
        planVersions.findTopByPlanIdOrderByVersionNumberDesc(profile.getCommercialPlanId());
    if (current.isEmpty() || current.get().getVersionNumber() != profile.getCommercialPlanVersion())
      return "O plano comercial mudou; crie uma nova revisão da ficha e reavalie com Plutus.";
    if (!Boolean.TRUE.equals(decisions(profile).get(checkpoint)))
      return "Plutus: checkpoint " + checkpoint + " pendente ou reprovado nesta revisão da ficha.";
    return null;
  }

  /** Escolhe o ponto financeiro necessário à atividade sem substituir seus demais gates. */
  public String checkpoint(String processCode, String activityId) {
    if ("pde-construction-approval".equals(processCode))
      return Set.of("journey", "prototypeCorrection").contains(activityId)
          ? "OFFER"
          : "DELIVERY_DESIGN";
    if (Set.of(
            "pde-communication-sales-journey",
            "creative-production-approval",
            "landing-page-generation")
        .contains(processCode)) return "DELIVERY_DESIGN";
    if (Set.of("pde-commercial-homologation-activation", "experiment-homologation-activation")
        .contains(processCode)) return "HOMOLOGATION";
    // Aprendizado e conciliação continuam disponíveis para descobrir perdas e preparar a correção.
    if ("pde-sales-delivery-learning".equals(processCode))
      return Set.of("optimization", "delivery").contains(activityId) ? "OPERATION" : null;
    if (Set.of("operacao-otimizacao-experimento", "venda-entrega-satisfacao-cliente")
        .contains(processCode)) return "OPERATION";
    return null;
  }
}
