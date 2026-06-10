package com.marketinghub.oprm.generalaudience.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedType;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.OprmGeneralAudienceService;
import com.marketinghub.oprm.generalaudience.service.convertToMarketNiche.ConvertGeneralAudienceSubnicheToMarketNicheRequest;
import com.marketinghub.oprm.generalaudience.service.convertToMarketNiche.GeneralAudienceMarketNicheConversionResponse;
import com.marketinghub.oprm.generalaudience.service.createSeed.CreateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.createSubniche.CreateGeneralAudienceSubnicheRequest;
import com.marketinghub.oprm.generalaudience.service.getSeed.GeneralAudienceSeedResponse;
import com.marketinghub.oprm.generalaudience.service.getSubniche.GeneralAudienceSubnicheResponse;
import com.marketinghub.oprm.generalaudience.service.listSeeds.GeneralAudienceSeedSummaryResponse;
import com.marketinghub.oprm.generalaudience.service.listSubniches.GeneralAudienceSubnicheSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Valida o contrato HTTP da etapa manual de sementes e subnichos de público geral do OPRM. */
@WebMvcTest(OprmGeneralAudienceController.class)
class OprmGeneralAudienceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OprmGeneralAudienceService service;

    /** Verifica se a API lista sementes para seleção operacional. */
    @Test
    void shouldListSeedsThroughApi() throws Exception {
        when(service.listSeeds()).thenReturn(List.of(new GeneralAudienceSeedSummaryResponse(
                1L,
                "Beleza",
                "Agenda, WhatsApp e Instagram",
                "BR",
                "pt-BR",
                OprmGeneralAudienceSeedType.CATEGORY,
                OprmGeneralAudienceSeedStatus.DRAFT,
                Instant.parse("2026-06-10T10:00:00Z"))));

        mockMvc.perform(get("/api/oprm/general-audiences/seeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Beleza"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    /** Verifica se a API cadastra semente sem acionar campanha ou descoberta automática. */
    @Test
    void shouldCreateSeedThroughApi() throws Exception {
        when(service.createSeed(any())).thenReturn(new GeneralAudienceSeedResponse(
                2L,
                "Beleza",
                "Profissionais autônomas",
                "Agenda, WhatsApp e Instagram",
                "BR",
                "pt-BR",
                OprmGeneralAudienceSeedType.CATEGORY,
                OprmGeneralAudienceSeedStatus.DRAFT,
                "Gerar leads qualificados",
                "Evitar promessa absoluta",
                Instant.parse("2026-06-10T10:00:00Z"),
                Instant.parse("2026-06-10T10:00:00Z")));
        CreateGeneralAudienceSeedRequest request = new CreateGeneralAudienceSeedRequest(
                "Beleza",
                "Profissionais autônomas",
                "Agenda, WhatsApp e Instagram",
                null,
                null,
                OprmGeneralAudienceSeedType.CATEGORY,
                null,
                "Gerar leads qualificados",
                "Evitar promessa absoluta");

        mockMvc.perform(post("/api/oprm/general-audiences/seeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/oprm/general-audiences/seeds/2"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Beleza"))
                .andExpect(jsonPath("$.seedType").value("CATEGORY"));
    }

    /** Verifica se a API lista subnichos de uma semente para revisão operacional. */
    @Test
    void shouldListSubnichesThroughApi() throws Exception {
        when(service.listSubniches(2L)).thenReturn(List.of(subnicheSummaryResponse()));

        mockMvc.perform(get("/api/oprm/general-audiences/seeds/2/subniches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].seedId").value(2))
                .andExpect(jsonPath("$[0].name").value("Manicure autônoma"))
                .andExpect(jsonPath("$[0].status").value("DISCOVERED"));
    }

    /** Verifica se a API cadastra subnicho sem misturar dados de público geral com CNAE. */
    @Test
    void shouldCreateSubnicheThroughApi() throws Exception {
        when(service.createSubniche(any(), any())).thenReturn(subnicheResponse(OprmGeneralAudienceSubnicheStatus.DISCOVERED));
        CreateGeneralAudienceSubnicheRequest request = new CreateGeneralAudienceSubnicheRequest(
                "Manicure autônoma",
                "Profissional que atende com agenda própria",
                "Agenda vazia durante a semana",
                "Preencher horários ociosos",
                "Clientes somem",
                "WhatsApp e Instagram",
                "Você trabalha como manicure hoje?",
                null,
                new BigDecimal("82.50"),
                new BigDecimal("18.00"),
                null);

        mockMvc.perform(post("/api/oprm/general-audiences/seeds/2/subniches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/oprm/general-audiences/subniches/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.seedId").value(2))
                .andExpect(jsonPath("$.qualificationQuestion").value("Você trabalha como manicure hoje?"));
    }

    /** Verifica se a API atualiza um subnicho manualmente. */
    @Test
    void shouldUpdateSubnicheThroughApi() throws Exception {
        when(service.updateSubniche(any(), any())).thenReturn(subnicheResponse(OprmGeneralAudienceSubnicheStatus.NEEDS_REVIEW));

        mockMvc.perform(patch("/api/oprm/general-audiences/subniches/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NEEDS_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("NEEDS_REVIEW"));
    }

    /** Verifica se a API aprova um subnicho para experimento futuro. */
    @Test
    void shouldApproveSubnicheThroughApi() throws Exception {
        when(service.approveSubniche(5L)).thenReturn(subnicheResponse(OprmGeneralAudienceSubnicheStatus.APPROVED_FOR_EXPERIMENT));

        mockMvc.perform(post("/api/oprm/general-audiences/subniches/5/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED_FOR_EXPERIMENT"));
    }

    /** Verifica se a API rejeita um subnicho amplo ou inseguro. */
    @Test
    void shouldRejectSubnicheThroughApi() throws Exception {
        when(service.rejectSubniche(5L)).thenReturn(subnicheResponse(OprmGeneralAudienceSubnicheStatus.REJECTED));

        mockMvc.perform(post("/api/oprm/general-audiences/subniches/5/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    /** Verifica se a API converte subnicho aprovado em MarketNiche sem exigir CNAE. */
    @Test
    void shouldConvertSubnicheToMarketNicheThroughApi() throws Exception {
        when(service.convertSubnicheToMarketNiche(any(), any())).thenReturn(
                new GeneralAudienceMarketNicheConversionResponse(
                        5L,
                        2L,
                        99L,
                        "Manicure autônoma",
                        OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE,
                        false,
                        Instant.parse("2026-06-10T12:00:00Z")));
        ConvertGeneralAudienceSubnicheToMarketNicheRequest request =
                new ConvertGeneralAudienceSubnicheToMarketNicheRequest(null, null, null, null, null);

        mockMvc.perform(post("/api/oprm/general-audiences/subniches/5/convert-to-market-niche")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subnicheId").value(5))
                .andExpect(jsonPath("$.marketNicheId").value(99))
                .andExpect(jsonPath("$.subnicheStatus").value("CONVERTED_TO_NICHE"));
    }

    /** Monta resposta detalhada de subnicho para os testes HTTP. */
    private GeneralAudienceSubnicheResponse subnicheResponse(OprmGeneralAudienceSubnicheStatus status) {
        return new GeneralAudienceSubnicheResponse(
                5L,
                2L,
                "Manicure autônoma",
                "Profissional que atende com agenda própria",
                "Agenda vazia durante a semana",
                "Preencher horários ociosos",
                "Clientes somem",
                "WhatsApp e Instagram",
                "Você trabalha como manicure hoje?",
                status,
                new BigDecimal("82.50"),
                new BigDecimal("18.00"),
                null,
                Instant.parse("2026-06-10T11:00:00Z"),
                Instant.parse("2026-06-10T11:00:00Z"));
    }

    /** Monta resposta resumida de subnicho para os testes HTTP. */
    private GeneralAudienceSubnicheSummaryResponse subnicheSummaryResponse() {
        return new GeneralAudienceSubnicheSummaryResponse(
                5L,
                2L,
                "Manicure autônoma",
                "Profissional que atende com agenda própria",
                "Agenda vazia durante a semana",
                "WhatsApp e Instagram",
                "Você trabalha como manicure hoje?",
                OprmGeneralAudienceSubnicheStatus.DISCOVERED,
                new BigDecimal("82.50"),
                new BigDecimal("18.00"),
                null,
                Instant.parse("2026-06-10T11:00:00Z"));
    }

}
