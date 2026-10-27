package com.marketinghub.payments.service.kit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.integration.image.AgendaCheiaPhotoGenerator;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.repository.AgendaCheiaDeliveryRepository;
import com.marketinghub.payments.service.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: provar fonte, especialidade, composição e recuperação sem geração paga. */
class LicensedKitReferencesTest {
  @TempDir Path root;

  /** Confere os trinta arquivos reais e seus hashes e impede uso acidental em perfil histórico. */
  @Test
  void validatesEveryLicensedSourceAndKeepsHistoricalProfileSeparate() throws Exception {
    var library = new LicensedKitReferences();
    var hashes = new HashSet<String>();
    for (String name : List.of("nails", "barber", "hair", "makeup", "brows", "aesthetics")) {
      var profile = CapellaKitCatalog.byCode(name + "-v2");
      var provenance = LicensedKitReferences.provenance(profile.code());
      assertThat(provenance.path("commercialApproval").asBoolean()).isFalse();
      assertThat(provenance.path("assets").size()).isEqualTo(5);
      for (int i = 0; i < 5; i++) {
        assertThat(library.generate("teste-origem", i, profile).getWidth())
            .isGreaterThanOrEqualTo(1024);
        assertThat(hashes.add(provenance.path("assets").get(i).path("sha256").asText())).isTrue();
      }
    }
    assertThatThrownBy(() -> library.generate("teste", 0, CapellaKitCatalog.byCode("nails-v1")))
        .hasMessageContaining("conferir");
    assertThat(LicensedKitReferences.provenance("nails-v1")).isNull();
    assertThatThrownBy(() -> CapellaKitCatalog.forPaymentReference("nails-v2"))
        .hasMessageContaining("homologada");
  }

  /**
   * Produz pacote real, preserva o mesmo ZIP no replay e não consulta geração ou entrega comercial.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"nails-v2", "barber-v2", "hair-v2", "makeup-v2", "brows-v2", "aesthetics-v2"})
  void composesAndRecoversReadablePackageWithoutPaidProvider(String profile) throws Exception {
    var repo = mock(AgendaCheiaDeliveryRepository.class);
    var email = mock(DigitalProductPostPurchaseEmailService.class);
    var paid = mock(AgendaCheiaPhotoGenerator.class);
    var service =
        new AgendaCheiaKitProductionService(
            repo, email, new ObjectMapper(), paid, root.toString(), "http://localhost");
    var input = new AgendaCheiaBriefing();
    input.setProfessionalName("Studio Aurora");
    input.setBuyerEmail("teste+referencias@sandbox.local");
    input.setCityRegion("Cidade Exemplo");
    input.setWhatsapp("11999999999");
    input.setServices(CapellaKitCatalog.byCode(profile).serviceExamples().split(":")[0]);
    input.setVisualStyle("Contemporâneo");
    input.setPreferredColors("Vinho e creme");
    input.setWeeklyGoal("Divulgar horários");
    String id = UUID.randomUUID().toString();
    var result = service.preparePrivateCandidate(input, profile, id);
    assertThat(result.qualityScore()).isGreaterThanOrEqualTo(90);
    byte[] before = Files.readAllBytes(result.zipPath());
    assertThat(before.length).as("Limite binário do backend").isLessThanOrEqualTo(32 * 1024 * 1024);
    assertThat(Files.readAllBytes(service.preparePrivateCandidate(input, profile, id).zipPath()))
        .isEqualTo(before);
    try (var zip = new ZipFile(result.zipPath().toFile())) {
      assertThat(zip.size()).isEqualTo(36);
      assertThat(zip.stream().map(e -> e.getName()).toList())
          .noneMatch(n -> n.contains("manifest"));
      String target = System.getProperty("capella.validation.output");
      if (target != null) {
        Files.createDirectories(Path.of(target));
        for (String entry :
            List.of(
                "posts/post-01.png",
                "posts/post-02.png",
                "posts/post-03.png",
                "posts/post-04.png",
                "posts/post-05.png",
                "stories/story-01.png"))
          Files.write(
              Path.of(target).resolve(profile + "-" + entry.replace('/', '-')),
              zip.getInputStream(zip.getEntry(entry)).readAllBytes());
      }
    }
    verifyNoInteractions(repo, email, paid);
  }
}
