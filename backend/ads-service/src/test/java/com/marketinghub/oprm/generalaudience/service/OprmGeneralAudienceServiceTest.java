package com.marketinghub.oprm.generalaudience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedType;
import com.marketinghub.oprm.generalaudience.service.createSeed.CreateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.updateSeed.UpdateGeneralAudienceSeedRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Valida o cadastro manual de sementes amplas de público geral sem acionar o fluxo NichoCNAE. */
class OprmGeneralAudienceServiceTest {

    /** Verifica se a criação normaliza campos e aplica padrões comerciais seguros. */
    @Test
    void shouldCreateSeedWithDefaults() {
        OprmGeneralAudienceSeedRepository repository = mock(OprmGeneralAudienceSeedRepository.class);
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(repository);
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
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(repository);
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
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(repository);
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
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(repository);
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
        OprmGeneralAudienceService service = new OprmGeneralAudienceService(repository);
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSeed(404L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Semente de público geral não encontrada");
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
