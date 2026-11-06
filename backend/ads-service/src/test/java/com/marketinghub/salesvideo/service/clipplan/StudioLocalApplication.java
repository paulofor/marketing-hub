package com.marketinghub.salesvideo.service.clipplan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.controller.SalesVideoController;
import com.marketinghub.salesvideo.service.SalesVideoProviderCatalogService;
import com.marketinghub.salesvideo.service.SalesVideoService;
import com.marketinghub.videocontract.v1.VideoCapabilities;
import java.time.Instant;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Responsabilidade: homologar HTTP e navegador com controller/planner reais e catálogo sintético
 * sem geração.
 */
@org.springframework.context.annotation.Import(com.marketinghub.web.ApiExceptionHandler.class)
@Configuration(proxyBeanMethods = false)
@Profile("studio-local")
@EnableAutoConfiguration(
    exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      JpaRepositoriesAutoConfiguration.class
    })
public class StudioLocalApplication {
  /** Inicia somente a topologia local sem workers, banco produtivo ou endpoints de providers. */
  public static void main(String[] args) {
    var app = new SpringApplication(StudioLocalApplication.class);
    app.setAdditionalProfiles("studio-local");
    app.setDefaultProperties(
        java.util.Map.of("server.port", "18080", "spring.main.banner-mode", "off"));
    app.run(args);
  }

  /** Mantém o controller oficial ligado ao serviço determinístico real, sem executar mutações. */
  @Bean
  SalesVideoController controller() {
    var repository = mock(SalesVideoProviderModelRepository.class);
    var rows =
        VideoCapabilities.catalog().models().stream()
            .map(
                cap -> {
                  var row = new SalesVideoProviderModel();
                  row.setId((long) VideoCapabilities.catalog().models().indexOf(cap) + 1);
                  row.setCode(cap.providerName().toLowerCase());
                  row.setDisplayName(cap.displayName());
                  row.setProviderName(cap.providerName());
                  row.setManufacturerName(cap.manufacturer());
                  row.setAggregatorName(cap.aggregator());
                  row.setProviderAccountKey(cap.accountKey());
                  row.setAdapterKey("RUNWAY");
                  row.setExternalModelId(cap.modelId());
                  row.setProviderFamily("EXTERNAL_VIDEO_MODULE");
                  row.setLifecycleStatus("gen4.5".equals(cap.modelId()) ? "ACTIVE" : "DRAFT");
                  row.setRecommendedUse("Fixture local: nenhuma geração autorizada");
                  row.setClipDurationSeconds(cap.maxSeconds());
                  row.setMaxDirectDurationSeconds(cap.maxSeconds());
                  row.setSupportsSceneAssembly(true);
                  row.setAdapterVerified(!cap.adapterModes().isEmpty());
                  row.setPricingVerified(true);
                  row.setCommercialLicenseVerified(true);
                  row.setQualityGateVerified("gen4.5".equals(cap.modelId()));
                  row.setPricingObservedAt(Instant.now());
                  row.setDocumentationUrl(VideoCapabilities.catalog().sources().getFirst());
                  return row;
                })
            .toList();
    when(repository.findAllByOrderByDisplayNameAsc()).thenReturn(rows);
    when(repository.findByProviderName(any()))
        .thenAnswer(
            call ->
                rows.stream()
                    .filter(row -> row.getProviderName().equals(call.getArgument(0)))
                    .findFirst());
    var catalog = new SalesVideoProviderCatalogService(repository);
    var facade = mock(SalesVideoService.class, CALLS_REAL_METHODS);
    org.springframework.test.util.ReflectionTestUtils.setField(
        facade, "providerCatalogService", catalog);
    return new SalesVideoController(facade, mock(com.marketinghub.media.mapper.AssetMapper.class));
  }
}
