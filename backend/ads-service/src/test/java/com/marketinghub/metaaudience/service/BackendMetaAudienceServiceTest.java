package com.marketinghub.metaaudience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.metaaudience.ExperimentMetaAudience;
import com.marketinghub.metaaudience.MetaAudience;
import com.marketinghub.metaaudience.MetaAudienceSegment;
import com.marketinghub.metaaudience.service.linkExperiment.ExperimentMetaAudienceResponse;
import com.marketinghub.metaaudience.service.linkExperiment.LinkMetaAudienceExperimentRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.metaaudience.ExperimentMetaAudienceRepository;
import com.marketinghub.repository.jpa.metaaudience.MetaAudienceRepository;
import com.marketinghub.repository.jpa.metaaudience.MetaAudienceSegmentRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Testa o serviço backend de leitura/escrita de planos de audiência CNAE. */
@ExtendWith(MockitoExtension.class)
class BackendMetaAudienceServiceTest {
    @Mock
    private MetaAudienceRepository audienceRepository;

    @Mock
    private MetaAudienceSegmentRepository segmentRepository;

    @Mock
    private ExperimentMetaAudienceRepository experimentAudienceRepository;

    @Mock
    private MarketNicheRepository nicheRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BackendMetaAudienceService service;

    /** Deve vincular a audiência ao experimento mantendo o mesmo nicho e status padrão. */
    @Test
    void linkExperimentPersistsAudienceExperimentRelation() {
        MarketNiche niche = niche(10L);
        MetaAudience audience = audience(20L, niche);
        MetaAudienceSegment segment = segment(30L, audience, niche);
        Experiment experiment = experiment(40L, niche);

        when(audienceRepository.findById(20L)).thenReturn(Optional.of(audience));
        when(experimentRepository.findById(40L)).thenReturn(Optional.of(experiment));
        when(segmentRepository.findById(30L)).thenReturn(Optional.of(segment));
        when(experimentAudienceRepository.findByExperimentIdAndMetaAudienceIdAndMetaAudienceSegmentId(40L, 20L, 30L))
                .thenReturn(Optional.empty());
        when(experimentAudienceRepository.save(any(ExperimentMetaAudience.class))).thenAnswer(invocation -> {
            ExperimentMetaAudience saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        ExperimentMetaAudienceResponse response = service.linkExperiment(new LinkMetaAudienceExperimentRequest(
                20L,
                30L,
                40L,
                "Meta Ads",
                "agenda lotada no WhatsApp",
                "organizar pedidos sem planilha",
                "produto low-ticket",
                null,
                "{\"uniqueEmails\":97490}"));

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.marketNicheId()).isEqualTo(10L);
        assertThat(response.metaAudienceId()).isEqualTo(20L);
        assertThat(response.metaAudienceSegmentId()).isEqualTo(30L);
        assertThat(response.experimentId()).isEqualTo(40L);
        assertThat(response.activationStatus()).isEqualTo("APPROVED_FOR_EXPERIMENT");
        assertThat(response.cnaeCode()).isEqualTo("5620104");
        assertThat(response.decisionSnapshotJson()).contains("uniqueEmails");
    }

    /** Deve preservar snapshot grande para manter a evidência usada na decisão do piloto CNAE. */
    @Test
    void linkExperimentPreservesLargeDecisionSnapshot() {
        MarketNiche niche = niche(10L);
        MetaAudience audience = audience(20L, niche);
        MetaAudienceSegment segment = segment(30L, audience, niche);
        Experiment experiment = experiment(40L, niche);
        String largeSnapshot = "{\"evidences\":\"" + "volume-cnae-".repeat(7000) + "\"}";

        when(audienceRepository.findById(20L)).thenReturn(Optional.of(audience));
        when(experimentRepository.findById(40L)).thenReturn(Optional.of(experiment));
        when(segmentRepository.findById(30L)).thenReturn(Optional.of(segment));
        when(experimentAudienceRepository.findByExperimentIdAndMetaAudienceIdAndMetaAudienceSegmentId(40L, 20L, 30L))
                .thenReturn(Optional.empty());
        when(experimentAudienceRepository.save(any(ExperimentMetaAudience.class))).thenAnswer(invocation -> {
            ExperimentMetaAudience saved = invocation.getArgument(0);
            saved.setId(51L);
            return saved;
        });

        ExperimentMetaAudienceResponse response = service.linkExperiment(new LinkMetaAudienceExperimentRequest(
                20L,
                30L,
                40L,
                "Meta Ads",
                "agenda lotada no WhatsApp",
                "organizar agenda sem perder cliente",
                "produto low-ticket",
                "PLANNED",
                largeSnapshot));

        assertThat(response.decisionSnapshotJson()).isEqualTo(largeSnapshot);
        assertThat(response.decisionSnapshotJson().length()).isGreaterThan(65_535);
    }

    /** Deve bloquear vínculo quando a audiência e o experimento pertencem a nichos diferentes. */
    @Test
    void linkExperimentRejectsDifferentNiches() {
        MetaAudience audience = audience(20L, niche(10L));
        Experiment experiment = experiment(40L, niche(11L));

        when(audienceRepository.findById(20L)).thenReturn(Optional.of(audience));
        when(experimentRepository.findById(40L)).thenReturn(Optional.of(experiment));

        MetaAudienceSegment segment = segment(30L, audience, audience.getMarketNiche());
        when(segmentRepository.findById(30L)).thenReturn(Optional.of(segment));

        assertThatThrownBy(() -> service.linkExperiment(new LinkMetaAudienceExperimentRequest(
                        20L, 30L, 40L, null, null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("mesmo nicho");
    }

    /** Deve listar vínculos de audiência CNAE por experimento para análise do piloto. */
    @Test
    void listByExperimentReturnsLinkedAudiences() {
        MarketNiche niche = niche(10L);
        ExperimentMetaAudience link = new ExperimentMetaAudience();
        link.setId(50L);
        link.setExperiment(experiment(40L, niche));
        link.setMarketNiche(niche);
        link.setMetaAudience(audience(20L, niche));
        link.setMetaAudienceSegment(segment(30L, link.getMetaAudience(), niche));
        link.setActivationStatus("RUNNING");

        when(experimentRepository.existsById(40L)).thenReturn(true);
        when(experimentAudienceRepository.findByExperimentIdOrderByUpdatedAtDesc(40L)).thenReturn(List.of(link));

        List<ExperimentMetaAudienceResponse> response = service.listByExperiment(40L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).activationStatus()).isEqualTo("RUNNING");
        assertThat(response.get(0).segmentName()).isEqualTo("Dono-operador de marmitas");
    }

    /** Cria um nicho mínimo para os testes de relacionamento. */
    private MarketNiche niche(Long id) {
        MarketNiche niche = new MarketNiche();
        niche.setId(id);
        niche.setName("Marmitas");
        return niche;
    }

    /** Cria uma audiência CNAE mínima para os testes de plano. */
    private MetaAudience audience(Long id, MarketNiche niche) {
        MetaAudience audience = new MetaAudience();
        audience.setId(id);
        audience.setMarketNiche(niche);
        audience.setSourceCnaeCode("5620104");
        audience.setAudienceName("Marmitas CNAE 5620104");
        audience.setEligibilityStatus("READY");
        audience.setTotalContacts(97490L);
        audience.setUniqueEmails(97490L);
        return audience;
    }

    /** Cria uma parcela funcional mínima para os testes de vínculo. */
    private MetaAudienceSegment segment(Long id, MetaAudience audience, MarketNiche niche) {
        MetaAudienceSegment segment = new MetaAudienceSegment();
        segment.setId(id);
        segment.setMetaAudience(audience);
        segment.setMarketNiche(niche);
        segment.setSegmentName("Dono-operador de marmitas");
        return segment;
    }

    /** Cria um experimento mínimo para validar o vínculo com audiência. */
    private Experiment experiment(Long id, MarketNiche niche) {
        Experiment experiment = new Experiment();
        experiment.setId(id);
        experiment.setNiche(niche);
        experiment.setName("Piloto marmitas");
        return experiment;
    }
}
