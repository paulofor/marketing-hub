package com.marketinghub.payments.service.kit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.integration.image.AgendaCheiaPhotoGenerator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: fornecer referências licenciadas e íntegras somente à demonstração v2. */
public final class LicensedKitReferences implements AgendaCheiaPhotoGenerator {
  private static final Logger log = LoggerFactory.getLogger(LicensedKitReferences.class);
  private static final String ROOT = "/kits/reference-v2/";

  /** Recusa seleção sem especialidade explícita, evitando repertório de outro serviço. */
  @Override
  public BufferedImage generate(String executionId, int variant) {
    throw new IllegalArgumentException("A referência exige especialidade explícita.");
  }

  /** Confere licença, identidade e hash antes de carregar os pixels locais, sem rede ou geração. */
  @Override
  public BufferedImage generate(String executionId, int variant, CapellaKitProfile profile) {
    try (var manifest = getClass().getResourceAsStream(ROOT + "manifest.json")) {
      if (!CapellaKitCatalog.isReference(profile.code()) || manifest == null)
        throw new IllegalStateException("Biblioteca de referência indisponível para este perfil.");
      var json = new ObjectMapper().readTree(manifest);
      validateManifest(json);
      var sources = new java.util.ArrayList<JsonNode>();
      for (var asset : json.path("assets"))
        if (profile.code().equals(asset.path("profileCode").asText())) sources.add(asset);
      if (sources.stream().map(a -> a.path("sha256").asText()).distinct().count() != 5
          || sources.size() != 5)
        throw new IllegalStateException("Biblioteca de referência incompleta.");
      var source = sources.get(Math.floorMod(variant, sources.size()));
      String file = source.path("file").asText();
      if (!file.matches("[a-z]+-[0-9]+\\.jpg")
          || !"https://www.pexels.com/license/".equals(source.path("licenseUrl").asText())
          || !source.path("sourceUrl").asText().matches("https://www[.]pexels[.]com/photo/[0-9]+/")
          || !"LICENSED_REFERENCE_DEMO".equals(source.path("use").asText())
          || !source.path("notCustomerWork").asBoolean())
        throw new IllegalStateException("Origem ou limite de uso da referência inválido.");
      try (var input = getClass().getResourceAsStream(ROOT + file)) {
        if (input == null) throw new IllegalStateException("Arquivo de referência ausente.");
        byte[] bytes = input.readAllBytes();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!hash.equals(source.path("sha256").asText()))
          throw new IllegalStateException("A referência diverge da origem conferida.");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null || image.getWidth() < 1024 || image.getHeight() < 1024)
          throw new IllegalStateException("Referência sem resolução suficiente.");
        log.info(
            "Referência licenciada: executionId={} profile={} file={} sha256={}",
            executionId,
            profile.code(),
            file,
            hash);
        return bounded(image);
      }
    } catch (Exception ex) {
      log.error(
          "Falha na biblioteca de referência executionId={} profile={}",
          executionId,
          profile.code(),
          ex);
      throw new IllegalStateException("Não foi possível conferir a referência visual.", ex);
    }
  }

  /** Recusa bibliotecas sem contrato explícito ou apresentadas como aprovação comercial. */
  private static void validateManifest(JsonNode manifest) {
    if (!"KIT_LICENSED_REFERENCE_V1".equals(manifest.path("contractVersion").asText())
        || !manifest.path("commercialApproval").isBoolean()
        || manifest.path("commercialApproval").asBoolean())
      throw new IllegalStateException("Contrato da biblioteca de referência inválido.");
  }

  /** Produz o recibo de origem do perfil, separado dos arquivos úteis entregues na tela. */
  public static JsonNode provenance(String profileCode) throws java.io.IOException {
    if (!CapellaKitCatalog.isReference(profileCode)) return null;
    var mapper = new ObjectMapper();
    try (var input = LicensedKitReferences.class.getResourceAsStream(ROOT + "manifest.json")) {
      if (input == null) throw new java.io.IOException("Manifesto visual ausente.");
      byte[] bytes = input.readAllBytes();
      var manifest = mapper.readTree(bytes);
      validateManifest(manifest);
      var result =
          mapper
              .createObjectNode()
              .put("contractVersion", "KIT_LICENSED_REFERENCE_V1")
              .put("profileCode", profileCode)
              .put("commercialApproval", false);
      try {
        result.put(
            "librarySha256",
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
      } catch (java.security.NoSuchAlgorithmException ex) {
        log.error("Hash da origem visual indisponível profile={}", profileCode, ex);
        throw new java.io.IOException("Hash da biblioteca indisponível.", ex);
      }
      var assets = result.putArray("assets");
      for (var asset : manifest.path("assets"))
        if (profileCode.equals(asset.path("profileCode").asText())) assets.add(asset);
      if (assets.size() != 5) throw new java.io.IOException("Origem visual incompleta.");
      result.put(
          "compositionMapping",
          "post/story 01-05 usam fontes 1-5; 06-10 reutilizam fontes 1-5 com outra mensagem");
      return result;
    }
  }

  /** Limita a memória da composição mantendo o menor lado em 1080 pixels e a proporção original. */
  private static BufferedImage bounded(BufferedImage original) {
    double scale = Math.min(1.0, 1080.0 / Math.min(original.getWidth(), original.getHeight()));
    if (scale == 1.0) return original;
    var image =
        new BufferedImage(
            (int) Math.ceil(original.getWidth() * scale),
            (int) Math.ceil(original.getHeight() * scale),
            BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setRenderingHint(
        java.awt.RenderingHints.KEY_INTERPOLATION,
        java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.drawImage(original, 0, 0, image.getWidth(), image.getHeight(), null);
    graphics.dispose();
    return image;
  }

  /** Preserva a região do serviço no recorte, usando o enquadramento conferido da fonte. */
  public static double focusY(String profile, int variant) throws java.io.IOException {
    var proof = provenance(profile);
    double focus = proof.path("assets").get(Math.floorMod(variant, 5)).path("focusY").asDouble(-1);
    if (focus < 0 || focus > 1) throw new java.io.IOException("Enquadramento visual inválido.");
    return focus;
  }
}
