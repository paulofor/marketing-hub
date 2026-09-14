package com.marketinghub.leadportal.service;

import com.marketinghub.imagegeneration.OpenAiImageGenerationPolicy;
import com.marketinghub.leadportal.service.executionprofile.ProfilePackageSource;
import com.marketinghub.product.executionprofile.v1.service.ExecutionProfileBudget;
import com.marketinghub.product.executionprofile.v1.service.ExecutionProfileContext;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract;
import com.marketinghub.repository.jdbc.leadportal.ExecutionProfileImagePackageRepository;
import com.marketinghub.repository.jpa.productexecution.ExecutionProfileBindingRepository;
import com.marketinghub.repository.jpa.productexecution.ExecutionProfileConsumptionRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: aplicar a reserva financeira da ficha aos pacotes do Lead Portal. */
@Component
@RequiredArgsConstructor
public class LeadPortalExecutionProfileBudget {
  // O contrato local comprova o limite de três chamadas por imagem-base no AI Worker.
  static final int MAX_BASE_IMAGE_ATTEMPTS = 3;
  private final ExecutionProfileImagePackageRepository packages;
  private final ExecutionProfileBindingRepository bindings;
  private final ExecutionProfileConsumptionRepository consumption;
  private final ExecutionProfileContext context;
  private final ExecutionProfileBudget budget;

  /** Reserva o pior caso do worker antes de entregar a posse do pacote ao executor. */
  @Transactional
  public void reserve(long packageId) {
    resolve(packageId)
        .ifPresent(
            source -> {
              var profile = context.bound(source.productId(), source.reference()).orElseThrow();
              var contract = context.contract(profile);
              if (contract.capability() != ProfileContract.Capability.PERSONALIZED_IMAGES
                  || source.outputs() != contract.includedUnits())
                throw conflict(
                    "O pacote não corresponde à capacidade e à quantidade prometidas na ficha.");
              String usage = "lead-portal-package:" + packageId;
              var binding =
                  bindings
                      .findByProductIdAndSourceReference(source.productId(), source.reference())
                      .orElseThrow();
              int attempt =
                  consumption
                          .findByBindingIdAndUsageKeyAndTestData(binding.getId(), usage, false)
                          .size()
                      + 1;
              budget.reserve(
                  source.productId(),
                  source.reference(),
                  usage,
                  usage + ":attempt:" + attempt,
                  source.input(),
                  OpenAiImageGenerationPolicy.CANONICAL_MODEL,
                  Math.multiplyExact(
                      source.outputs(), source.withBase() ? MAX_BASE_IMAGE_ATTEMPTS : 1),
                  false);
            });
  }

  /** Preserva consumo desconhecido inclusive em falha ou reagendamento do pacote. */
  @Transactional
  public void settle(long packageId, boolean failed) {
    resolve(packageId)
        .ifPresent(
            source -> {
              var binding =
                  bindings
                      .findByProductIdAndSourceReference(source.productId(), source.reference())
                      .orElseThrow();
              var rows =
                  consumption.findByBindingIdAndUsageKeyAndTestData(
                      binding.getId(), "lead-portal-package:" + packageId, false);
              rows.stream()
                  .filter(r -> "RESERVED".equals(r.getStatus()))
                  .forEach(
                      r ->
                          budget.settle(
                              source.productId(),
                              r.getId(),
                              null,
                              "flow_submission_image_package:" + packageId + ";failed=" + failed,
                              failed));
            });
  }

  /** Exige o pacote completo antes de promover a entrega, mantendo a revisão visual posterior. */
  public void requireComplete(long packageId, int outputCount) {
    resolve(packageId)
        .ifPresent(
            source -> {
              if (source.outputs() != outputCount)
                throw conflict("O pacote retornado não contém a quantidade contratada na ficha.");
            });
  }

  /** Resolve a correlação persistida; fluxo compartilhado ambíguo não autoriza gasto. */
  private Optional<ProfilePackageSource> resolve(long packageId) {
    var sources = packages.findSources(packageId);
    if (sources.stream().noneMatch(s -> bindings.existsByProductId(s.productId())))
      return Optional.empty();
    if (sources.size() != 1
        || context.bound(sources.getFirst().productId(), sources.getFirst().reference()).isEmpty())
      throw conflict("O pacote exige experimento único e ficha vinculada antes de consumir IA.");
    return Optional.of(sources.getFirst());
  }

  /** Expõe o bloqueio funcional sem iniciar consumo externo. */
  private ResponseStatusException conflict(String reason) {
    return new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }
}
