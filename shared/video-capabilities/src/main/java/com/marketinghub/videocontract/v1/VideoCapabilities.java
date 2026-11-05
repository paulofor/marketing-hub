package com.marketinghub.videocontract.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: interpretar o único catálogo versionado de capacidades e tarifas de vídeo. */
public final class VideoCapabilities {
  private static final Logger log = LoggerFactory.getLogger(VideoCapabilities.class);
  private static final Catalog CATALOG = load();

  /** Impede instanciar o contrato determinístico compartilhado. */
  private VideoCapabilities() {}

  /** Entrega a revisão imutável empacotada tanto no backend quanto no executor. */
  public static Catalog catalog() {
    return CATALOG;
  }

  /** Localiza somente identificador oficial exato, sem trocar modelo ou plataforma. */
  public static Optional<Model> model(String id) {
    return CATALOG.models().stream().filter(m -> m.modelId().equals(id)).findFirst();
  }

  /** Resolve aliases explicitamente documentados sem usar correspondência parcial de Seedance. */
  public static Optional<Model> provider(String name) {
    String alias = "RUNWAY_GEN_4_5".equalsIgnoreCase(name) ? "RUNWAY" : name;
    return CATALOG.models().stream()
        .filter(m -> m.providerName().equalsIgnoreCase(alias))
        .findFirst();
  }

  /** Identifica a seleção literal do projeto; ausência não autoriza escolher um novo modelo. */
  public static Optional<Model> selected(String plan) {
    String value = plan == null ? "" : plan.toUpperCase(java.util.Locale.ROOT);
    var chosen =
        java.util.regex.Pattern.compile("PROVIDER ESCOLHIDO[^\\r\\n]*?\\((RUNWAY[A-Z0-9_]*)\\)")
            .matcher(value);
    String explicit = null;
    while (chosen.find()) explicit = chosen.group(1);
    if ("RUNWAY_ROUTER".equals(explicit) || "RUNWAY_PRODUCT_UGC".equals(explicit))
      return Optional.empty();
    if (explicit != null)
      return Optional.of(
          provider(explicit)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Modelo escolhido sem contrato de capacidades")));
    var markers = java.util.regex.Pattern.compile("\\((RUNWAY[A-Z0-9_]*)\\)").matcher(value);
    while (markers.find()) {
      String marker = markers.group(1);
      if (!"RUNWAY_ROUTER".equals(marker)
          && !"RUNWAY_PRODUCT_UGC".equals(marker)
          && provider(marker).isEmpty())
        throw new IllegalArgumentException("Rota escolhida sem contrato compartilhado: " + marker);
    }
    var matches =
        CATALOG.models().stream()
            .filter(
                m ->
                    value.contains("(" + m.providerName() + ")")
                        || java.util.regex.Pattern.compile(
                                "(?<![A-Z0-9_])" + m.providerName() + "(?![A-Z0-9_])")
                            .matcher(value)
                            .find())
            .toList();
    if (matches.size() > 1)
      throw new IllegalArgumentException(
          "Plano cita mais de um modelo; selecione explicitamente o provider no Estúdio antes de gerar");
    if (matches.isEmpty() && value.contains("(RUNWAY_GEN_4_5)")) return provider("RUNWAY");
    return matches.stream().findFirst();
  }

  /** Carrega o recurso obrigatório e impede iniciar com contrato ausente ou inválido. */
  private static Catalog load() {
    try (var stream =
        VideoCapabilities.class.getResourceAsStream("/video-capabilities/catalog.json")) {
      if (stream == null) throw new IOException("Catálogo de vídeo ausente");
      return new ObjectMapper().readValue(stream, Catalog.class);
    } catch (IOException ex) {
      log.error("Falha ao carregar contrato compartilhado de capacidades de vídeo", ex);
      throw new IllegalStateException("Contrato de vídeo indisponível", ex);
    }
  }

  /** Mantém a proveniência e a validade do snapshot sem presumir homologação operacional. */
  public record Catalog(
      String revision,
      String observedAt,
      String priceExpiresAt,
      List<String> sources,
      List<Model> models) {}

  /** Descreve capacidade pública, subconjunto implementado e tarifa por resolução e áudio. */
  public record Model(
      String code,
      List<String> modalities,
      int adapterMaxImageReferences,
      int maxVideoReferenceSeconds,
      String modelId,
      String providerName,
      String displayName,
      String manufacturer,
      String aggregator,
      String accountKey,
      int minSeconds,
      int maxSeconds,
      List<Integer> validSeconds,
      List<String> resolutions,
      boolean nativeAudio,
      int maxImageReferences,
      List<String> adapterModes,
      Map<String, Map<String, BigDecimal>> rates,
      int minimumCredits,
      Map<String, BigDecimal> referenceVideoRates,
      int referenceItemCredits) {
    /** Confere duração exata, inclusive conjuntos discretos, antes de aceitar uma tomada. */
    public void requireDuration(int seconds) {
      if (seconds < minSeconds
          || seconds > maxSeconds
          || (!validSeconds.isEmpty() && !validSeconds.contains(seconds)))
        throw new IllegalArgumentException(
            displayName
                + " não aceita tomada de "
                + seconds
                + "s; use "
                + (validSeconds.isEmpty()
                    ? minSeconds + "–" + maxSeconds + "s"
                    : validSeconds + "s")
                + " ou planeje montagem. Nenhuma geração foi autorizada.");
    }

    /** Divide a duração final em tomadas válidas, minimizando quantidade e excedente explícito. */
    public ClipPlan plan(int finalSeconds) {
      if (finalSeconds < 1 || finalSeconds > 3600)
        throw new IllegalArgumentException("Duração final deve estar entre 1 e 3600s");
      List<Integer> allowed =
          validSeconds.isEmpty()
              ? java.util.stream.IntStream.rangeClosed(minSeconds, maxSeconds).boxed().toList()
              : validSeconds;
      int bound = finalSeconds + maxSeconds;
      int[] counts = new int[bound + 1];
      java.util.Arrays.fill(counts, 100000);
      counts[0] = 0;
      int[] previous = new int[bound + 1];
      for (int total = 1; total <= bound; total++) {
        for (int duration : allowed.stream().sorted(Comparator.reverseOrder()).toList()) {
          if (total >= duration && counts[total - duration] + 1 < counts[total]) {
            counts[total] = counts[total - duration] + 1;
            previous[total] = duration;
          }
        }
      }
      int chosen = finalSeconds;
      while (chosen <= bound && counts[chosen] == 100000) chosen++;
      List<Integer> clips = new ArrayList<>();
      for (int total = chosen; total > 0; total -= previous[total]) clips.add(previous[total]);
      clips.sort(Comparator.reverseOrder());
      return new ClipPlan(modelId, finalSeconds, chosen, chosen - finalSeconds, List.copyOf(clips));
    }

    /** Calcula cada tomada com mínimo, referências e arredondamento, recusando preço vencido. */
    public BigDecimal credits(
        int seconds,
        String resolution,
        boolean audio,
        int referenceCount,
        int referenceVideoSeconds,
        Instant now) {
      requireDuration(seconds);
      if (!now.isBefore(Instant.parse(CATALOG.priceExpiresAt())))
        throw new IllegalArgumentException(
            "Preço vencido; renovar pesquisa antes de reservar ou gerar");
      if (referenceCount < 0
          || referenceCount > maxImageReferences
          || referenceVideoSeconds < 0
          || referenceVideoSeconds > maxVideoReferenceSeconds)
        throw new IllegalArgumentException("Referências fora do contrato do modelo");
      var rate = rates.get(resolution);
      BigDecimal unit = rate == null ? null : rate.get(audio ? "audio" : "silent");
      if (unit == null)
        throw new IllegalArgumentException("Resolução/áudio não precificados para " + modelId);
      BigDecimal cost = unit.multiply(BigDecimal.valueOf(seconds));
      if (referenceVideoSeconds > 0) {
        BigDecimal videoRate = referenceVideoRates.get(resolution);
        if (videoRate == null)
          throw new IllegalArgumentException("Referência de vídeo sem tarifa comprovada");
        if ("kling_4".equals(modelId))
          cost =
              videoRate.multiply(
                  BigDecimal.valueOf(seconds + Math.max(seconds, referenceVideoSeconds)));
        else cost = cost.add(videoRate.multiply(BigDecimal.valueOf(referenceVideoSeconds)));
      }
      cost =
          cost.add(BigDecimal.valueOf((long) referenceCount * referenceItemCredits))
              .max(BigDecimal.valueOf(minimumCredits));
      cost = cost.setScale(0, RoundingMode.CEILING);
      if ("kling_4".equals(modelId) && now.isBefore(Instant.parse("2026-12-01T00:00:00Z")))
        cost = cost.multiply(new BigDecimal("0.8")).setScale(0, RoundingMode.CEILING);
      return cost;
    }
  }

  /** Separa duração final, duração faturada e excedente de montagem auditável. */
  public record ClipPlan(
      String modelId,
      int finalSeconds,
      int generatedSeconds,
      int trimSeconds,
      List<Integer> clipSeconds) {}
}
