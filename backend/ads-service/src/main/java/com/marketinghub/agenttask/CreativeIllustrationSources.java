package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.imagegenerator.service.ImageGeneratorService;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Responsabilidade: reutilizar ilustrações auditadas sem tratá-las como prova do produto. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreativeIllustrationSources {
  private final ImageGenerationRequestRepository images;
  private final ImageGeneratorService generator;

  /** Lista duas imagens já concluídas na identidade exata, sem gerar ou promover ativos. */
  public List<Illustration> list(JsonNode context, String reference) {
    Scope scope = scope(context, reference);
    if (scope == null) return List.of();
    return images
        .findRecentCompleted(
            scope.productId(), scope.planId(), scope.experimentId(), PageRequest.of(0, 2))
        .stream()
        .map(row -> read(context, reference, row.getJobId()).metadata())
        .toList();
  }

  /** Recupera bytes persistidos com contexto, formato e hash conferidos novamente. */
  public Content read(JsonNode context, String reference, String jobId) {
    Scope scope = scope(context, reference);
    if (scope == null)
      throw new IllegalArgumentException("Ilustração sem contexto comercial exato.");
    try {
      var result =
          generator.getGeneratedImage(
              scope.productId(), scope.planId(), scope.experimentId(), jobId);
      if (!"png".equals(result.outputFormat()))
        throw new IllegalArgumentException("Ilustração deve ser PNG.");
      byte[] bytes = Base64.getDecoder().decode(result.imageBase64());
      var pixels = ImageIO.read(new ByteArrayInputStream(bytes));
      if (pixels == null || pixels.getWidth() < 300 || pixels.getHeight() < 300)
        throw new IllegalArgumentException("Ilustração sem resolução suficiente.");
      String hash =
          HexFormat.of()
              .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
      return new Content(
          new Illustration(
              jobId, hash, pixels.getWidth(), pixels.getHeight(), "GENERATED_ILLUSTRATION", false),
          bytes);
    } catch (Exception ex) {
      log.error(
          "Falha ao recuperar ilustração. productId={} experimentId={} jobId={}",
          scope.productId(),
          scope.experimentId(),
          jobId,
          ex);
      throw new IllegalArgumentException("Ilustração indisponível no contexto da tarefa.", ex);
    }
  }

  /** Valida origem e recorte sem transformar uma imagem gerada em captura funcional. */
  public Illustration validate(JsonNode context, String reference, JsonNode selected) {
    var source = read(context, reference, selected.path("jobId").asText()).metadata();
    if (!source.sha256().equals(selected.path("sha256").asText())
        || !"GENERATED_ILLUSTRATION".equals(selected.path("role").asText()))
      throw new IllegalArgumentException("Identidade ou papel da ilustração divergente.");
    var crop = selected.path("crop");
    for (String key : List.of("x", "y", "width", "height"))
      if (!crop.path(key).isIntegralNumber() || !crop.path(key).canConvertToInt())
        throw new IllegalArgumentException("Recorte ilustrativo inválido.");
    int x = crop.path("x").asInt(),
        y = crop.path("y").asInt(),
        w = crop.path("width").asInt(),
        h = crop.path("height").asInt();
    if (x < 0
        || y < 0
        || w < 300
        || h < 300
        || (long) x + w > source.width()
        || (long) y + h > source.height())
      throw new IllegalArgumentException("Recorte ilustrativo fora dos pixels auditados.");
    return source;
  }

  /**
   * Resolve somente a identidade integral; ausência não permite buscar imagens de outro contexto.
   */
  private Scope scope(JsonNode context, String reference) {
    long product = context.path("product").path("id").asLong(),
        plan = context.path("commercialPlanId").asLong();
    if (product < 1
        || plan < 1
        || reference == null
        || !reference.matches("experiment:[1-9][0-9]*")) return null;
    return new Scope(product, plan, Long.parseLong(reference.substring(11)));
  }

  /** Responsabilidade: identificar uma ilustração sem conceder aprovação de uso. */
  public record Illustration(
      String jobId, String sha256, int width, int height, String role, boolean productProof) {}

  /** Responsabilidade: transportar bytes somente após conferência de identidade. */
  public record Content(Illustration metadata, byte[] bytes) {}

  /** Responsabilidade: conservar a segregação por produto, plano e experimento. */
  private record Scope(long productId, long planId, long experimentId) {}
}
