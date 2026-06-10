package com.marketinghub.oprm.generalaudience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedType;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.convertToMarketNiche.ConvertGeneralAudienceSubnicheToMarketNicheRequest;
import com.marketinghub.oprm.generalaudience.service.createSeed.CreateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.createSubniche.CreateGeneralAudienceSubnicheRequest;
import com.marketinghub.oprm.generalaudience.service.updateSeed.UpdateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.updateSubniche.UpdateGeneralAudienceSubnicheRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceMarketNicheMaterializationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceMaterializedMarketNiche;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSubnicheRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Valida o cadastro manual de sementes e subnichos de público geral sem acionar o fluxo NichoCNAE. */
class OprmGeneralAudienceServiceTest {

    /** Verifica se a criação normaliza campos e aplica padrões comerciais seguros. */
    @Test
    void shouldCreateSeedWithDefaults() {
        OprmGeneralAudienceSeedRepository repository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(
                repository,
                mock(OprmGeneralAudienceSubnicheRepository.class),
                mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        when(repository.save(any())).thenAnswer(invocation -> {
            OprmGeneralAudienceSeed seed = invocation.getArgument(0);
            seed.setId(10L);
            seed.setCreatedAt(Instant.parse("2026-06-10T10:00:00Z"));
            seed.setUpdatedAt(Instant.parse("2026-06-10T10:00:00Z"));
            return seed;
        });

        var response = service.createSeed(new CreateGeneralAudienceSeedRequest(
                " Beleza ",
                " Profissionais autônomas ",
                " WhatsApp e Instagram ",
                null,
                null,
                OprmGeneralAudienceSeedType.CATEGORY,
                null,
                " Gerar leads qualificados ",
                " Evitar promessa de resultado garantido "));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Beleza");
        assertThat(response.country()).isEqualTo("BR");
        assertThat(response.language()).isEqualTo("pt-BR");
        assertThat(response.status()).isEqualTo(OprmGeneralAudienceSeedStatus.DRAFT);
        assertThat(response.seedType()).isEqualTo(OprmGeneralAudienceSeedType.CATEGORY);
    }

    /** Verifica se a listagem retorna sementes em contrato resumido para seleção operacional. */
    @Test
    void shouldListSeedSummaries() {
        OprmGeneralAudienceSeedRepository repository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(
                repository,
                mock(OprmGeneralAudienceSubnicheRepository.class),
                mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        OprmGeneralAudienceSeed seed = seed(7L, OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH);
        when(repository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(seed));

        var response = service.listSeeds();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(7L);
        assertThat(response.get(0).name()).isEqualTo("Beleza");
        assertThat(response.get(0).status()).isEqualTo(OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH);
    }

    /** Verifica se a atualização mantém campos ausentes e altera apenas decisões enviadas. */
    @Test
    void shouldUpdateOnlyProvidedFields() {
        OprmGeneralAudienceSeedRepository repository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(
                repository,
                mock(OprmGeneralAudienceSubnicheRepository.class),
                mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        OprmGeneralAudienceSeed seed = seed(8L, OprmGeneralAudienceSeedStatus.DRAFT);
        when(repository.findById(8L)).thenReturn(Optional.of(seed));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateSeed(8L, new UpdateGeneralAudienceSeedRequest(
                " Beleza profissional ",
                null,
                null,
                null,
                null,
                null,
                OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH,
                null,
                " Revisado para evitar promessa absoluta "));

        assertThat(response.name()).isEqualTo("Beleza profissional");
        assertThat(response.description()).isEqualTo("Profissionais autônomas de beleza");
        assertThat(response.status()).isEqualTo(OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH);
        assertThat(response.riskNotes()).isEqualTo("Revisado para evitar promessa absoluta");
    }

    /** Verifica se o arquivamento preserva a semente e altera somente o status operacional. */
    @Test
    void shouldArchiveSeed() {
        OprmGeneralAudienceSeedRepository repository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(
                repository,
                mock(OprmGeneralAudienceSubnicheRepository.class),
                mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        OprmGeneralAudienceSeed seed = seed(9L, OprmGeneralAudienceSeedStatus.PAUSED);
        when(repository.findById(9L)).thenReturn(Optional.of(seed));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.archiveSeed(9L);

        assertThat(response.status()).isEqualTo(OprmGeneralAudienceSeedStatus.ARCHIVED);
        verify(repository).save(seed);
    }

    /** Verifica se a busca de semente inexistente devolve erro claro de recurso não encontrado. */
    @Test
    void shouldRejectMissingSeed() {
        OprmGeneralAudienceSeedRepository repository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(
                repository,
                mock(OprmGeneralAudienceSubnicheRepository.class),
                mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSeed(404L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Semente de público geral não encontrada");
    }


    /** Verifica se a criação de subnicho preserva vínculo com a semente e aplica status inicial seguro. */
    @Test
    void shouldCreateSubnicheWithSeedLink() {
        OprmGeneralAudienceSeedRepository seedRepository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceSubnicheRepository subnicheRepository = mock(OprmGeneralAudienceSubnicheRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(seedRepository, subnicheRepository, mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        OprmGeneralAudienceSeed seed = seed(10L, OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH);
        when(seedRepository.findById(10L)).thenReturn(Optional.of(seed));
        when(subnicheRepository.save(any())).thenAnswer(invocation -> {
            OprmGeneralAudienceSubniche subniche = invocation.getArgument(0);
            subniche.setId(20L);
            subniche.setCreatedAt(Instant.parse("2026-06-10T11:00:00Z"));
            subniche.setUpdatedAt(Instant.parse("2026-06-10T11:00:00Z"));
            return subniche;
        });

        var response = service.createSubniche(10L, new CreateGeneralAudienceSubnicheRequest(
                " Manicure autônoma ",
                "Profissional que atende com agenda própria",
                "Agenda vazia durante a semana",
                "Preencher horários ociosos",
                "Fala em clientes que somem",
                "WhatsApp e Instagram",
                "Você trabalha como manicure hoje?",
                null,
                new BigDecimal("82.50"),
                new BigDecimal("18.00"),
                null));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.seedId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Manicure autônoma");
        assertThat(response.status()).isEqualTo(OprmGeneralAudienceSubnicheStatus.DISCOVERED);
        assertThat(response.qualificationQuestion()).isEqualTo("Você trabalha como manicure hoje?");
    }

    /** Verifica se a atualização de subnicho altera somente campos enviados pela revisão manual. */
    @Test
    void shouldUpdateSubnicheOnlyProvidedFields() {
        OprmGeneralAudienceSeedRepository seedRepository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceSubnicheRepository subnicheRepository = mock(OprmGeneralAudienceSubnicheRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(seedRepository, subnicheRepository, mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        OprmGeneralAudienceSubniche subniche = subniche(21L, seed(10L, OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH));
        when(subnicheRepository.findById(21L)).thenReturn(Optional.of(subniche));
        when(subnicheRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateSubniche(21L, new UpdateGeneralAudienceSubnicheRequest(
                null,
                null,
                "Clientes somem depois do primeiro atendimento",
                null,
                null,
                null,
                "Você atende clientes de manicure hoje?",
                OprmGeneralAudienceSubnicheStatus.NEEDS_REVIEW,
                null,
                new BigDecimal("22.00"),
                null));

        assertThat(response.name()).isEqualTo("Manicure autônoma");
        assertThat(response.painSummary()).isEqualTo("Clientes somem depois do primeiro atendimento");
        assertThat(response.qualificationQuestion()).isEqualTo("Você atende clientes de manicure hoje?");
        assertThat(response.status()).isEqualTo(OprmGeneralAudienceSubnicheStatus.NEEDS_REVIEW);
        assertThat(response.riskScore()).isEqualByComparingTo("22.00");
    }

    /** Verifica se a aprovação de subnicho marca a preparação para experimento sem criar campanha. */
    @Test
    void shouldApproveSubnicheForExperiment() {
        OprmGeneralAudienceSeedRepository seedRepository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceSubnicheRepository subnicheRepository = mock(OprmGeneralAudienceSubnicheRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(seedRepository, subnicheRepository, mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        OprmGeneralAudienceSubniche subniche = subniche(22L, seed(10L, OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH));
        when(subnicheRepository.findById(22L)).thenReturn(Optional.of(subniche));
        when(subnicheRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.approveSubniche(22L);

        assertThat(response.status()).isEqualTo(OprmGeneralAudienceSubnicheStatus.APPROVED_FOR_EXPERIMENT);
        verify(subnicheRepository).save(subniche);
    }

    /** Verifica se a rejeição de subnicho bloqueia avanço de público amplo demais. */
    @Test
    void shouldRejectSubniche() {
        OprmGeneralAudienceSeedRepository seedRepository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceSubnicheRepository subnicheRepository = mock(OprmGeneralAudienceSubnicheRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(seedRepository, subnicheRepository, mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class));
        OprmGeneralAudienceSubniche subniche = subniche(23L, seed(10L, OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH));
        when(subnicheRepository.findById(23L)).thenReturn(Optional.of(subniche));
        when(subnicheRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.rejectSubniche(23L);

        assertThat(response.status()).isEqualTo(OprmGeneralAudienceSubnicheStatus.REJECTED);
    }


    /** Verifica se a conversão controlada cria MarketNiche somente após aprovação humana. */
    @Test
    void shouldConvertApprovedSubnicheToMarketNicheWithoutCnae() {
        OprmGeneralAudienceSeedRepository seedRepository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceSubnicheRepository subnicheRepository = mock(OprmGeneralAudienceSubnicheRepository.class);
        OprmGeneralAudienceMarketNicheMaterializationRepository materializationRepository =
                mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(
                seedRepository, subnicheRepository, materializationRepository);
        OprmGeneralAudienceSubniche subniche = subniche(24L, seed(10L, OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH));
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.APPROVED_FOR_EXPERIMENT);
        when(subnicheRepository.findById(24L)).thenReturn(Optional.of(subniche));
        when(materializationRepository.saveMarketNiche(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new OprmGeneralAudienceMaterializedMarketNiche(99L, "Manicure autônoma", false));
        when(subnicheRepository.save(any())).thenAnswer(invocation -> {
            OprmGeneralAudienceSubniche saved = invocation.getArgument(0);
            saved.setUpdatedAt(Instant.parse("2026-06-10T12:00:00Z"));
            return saved;
        });

        var response = service.convertSubnicheToMarketNiche(24L, new ConvertGeneralAudienceSubnicheToMarketNicheRequest(
                null, null, null, null, null));

        assertThat(response.marketNicheId()).isEqualTo(99L);
        assertThat(response.reusedExistingMarketNiche()).isFalse();
        assertThat(response.subnicheStatus()).isEqualTo(OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE);
        assertThat(subniche.getMarketNicheId()).isEqualTo(99L);
        verify(materializationRepository).saveMarketNiche(
                any(),
                argThat(name -> name.equals("Manicure autônoma")),
                argThat(description -> description.contains("Público Geral") && !description.contains("CNAE")),
                argThat(segmentation -> segmentation.contains("Triagem obrigatória")),
                any(),
                any(),
                argThat(extraTips -> extraTips.contains("Não foi criado a partir de CNAE.")));
    }

    /** Verifica se subnicho não aprovado é bloqueado antes de criar MarketNiche. */
    @Test
    void shouldBlockMarketNicheConversionBeforeApproval() {
        OprmGeneralAudienceSeedRepository seedRepository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceSubnicheRepository subnicheRepository = mock(OprmGeneralAudienceSubnicheRepository.class);
        OprmGeneralAudienceMarketNicheMaterializationRepository materializationRepository =
                mock(OprmGeneralAudienceMarketNicheMaterializationRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(
                seedRepository, subnicheRepository, materializationRepository);
        OprmGeneralAudienceSubniche subniche = subniche(25L, seed(10L, OprmGeneralAudienceSeedStatus.READY_FOR_RESEARCH));
        when(subnicheRepository.findById(25L)).thenReturn(Optional.of(subniche));

        assertThatThrownBy(() -> service.convertSubnicheToMarketNiche(
                25L,
                new ConvertGeneralAudienceSubnicheToMarketNicheRequest(null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Subnicho precisa estar aprovado");
    }

    /** Monta um subnicho completo para os cenários de revisão manual. */
    private OprmGeneralAudienceSubniche subniche(Long id, OprmGeneralAudienceSeed seed) {
        OprmGeneralAudienceSubniche subniche = new OprmGeneralAudienceSubniche();
        subniche.setId(id);
        subniche.setSeed(seed);
        subniche.setName("Manicure autônoma");
        subniche.setPersonaSummary("Profissional que atende com agenda própria");
        subniche.setPainSummary("Agenda vazia durante a semana");
        subniche.setDesiredOutcomeSummary("Preencher horários ociosos");
        subniche.setLanguagePatterns("Clientes que somem");
        subniche.setChannelsSummary("WhatsApp e Instagram");
        subniche.setQualificationQuestion("Você trabalha como manicure hoje?");
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.DISCOVERED);
        subniche.setOpportunityScore(new BigDecimal("82.50"));
        subniche.setRiskScore(new BigDecimal("18.00"));
        subniche.setCreatedAt(Instant.parse("2026-06-10T11:00:00Z"));
        subniche.setUpdatedAt(Instant.parse("2026-06-10T11:00:00Z"));
        return subniche;
    }

    /** Monta uma semente completa para os cenários de revisão manual. */
    private OprmGeneralAudienceSeed seed(Long id, OprmGeneralAudienceSeedStatus status) {
        OprmGeneralAudienceSeed seed = new OprmGeneralAudienceSeed();
        seed.setId(id);
        seed.setName("Beleza");
        seed.setDescription("Profissionais autônomas de beleza");
        seed.setMarketContext("Agenda, WhatsApp e Instagram");
        seed.setCountry("BR");
        seed.setLanguage("pt-BR");
        seed.setSeedType(OprmGeneralAudienceSeedType.CATEGORY);
        seed.setStatus(status);
        seed.setBusinessGoal("Gerar leads qualificados");
        seed.setRiskNotes("Evitar promessa absoluta");
        seed.setCreatedAt(Instant.parse("2026-06-10T09:00:00Z"));
        seed.setUpdatedAt(Instant.parse("2026-06-10T10:00:00Z"));
        return seed;
    }
}
