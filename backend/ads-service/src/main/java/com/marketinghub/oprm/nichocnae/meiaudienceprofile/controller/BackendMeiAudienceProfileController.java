package com.marketinghub.oprm.nichocnae.meiaudienceprofile.controller;

import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.BackendMeiAudienceProfileService;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.detailAudienceProfile.MeiAudienceProfileDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller OPRM responsável por expor o perfil final aprovado de público-alvo MEI/autônomo. */
@Tag(
    name = "OPRM NichoCNAE — Perfil MEI/autônomo",
    description = "Contratos de leitura do perfil comportamental MEI/autônomo aprovado, sem produto, oferta ou campanha.")
@RestController
@RequestMapping("/api/oprm/nichocnae/mei-audience-profiles")
public class BackendMeiAudienceProfileController {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendMeiAudienceProfileController.class);

  private final BackendMeiAudienceProfileService profileService;

  /** Inicializa o controller com o serviço canônico de perfil MEI/autônomo. */
  public BackendMeiAudienceProfileController(BackendMeiAudienceProfileService profileService) {
    this.profileService = profileService;
  }

  /** Detalha o perfil MEI/autônomo aprovado de um ciclo para consumo posterior sem criar produto ou oferta. */
  @Operation(
      summary = "Detalha perfil MEI/autônomo aprovado por ciclo",
      description =
          "Retorna somente o perfil comportamental aprovado pelo gate MEI_AUDIENCE_READY, bloqueando conteúdo com produto, oferta, campanha ou solução.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Perfil MEI/autônomo aprovado retornado."),
    @ApiResponse(responseCode = "404", description = "Perfil aprovado não encontrado para o ciclo."),
    @ApiResponse(responseCode = "409", description = "Perfil encontrado, mas bloqueado por qualidade ou contaminação de solução.")
  })
  @GetMapping("/research-cycles/{researchCycleId}")
  public ResponseEntity<MeiAudienceProfileDetailResponse> detailByResearchCycleId(@PathVariable Long researchCycleId) {
    try {
      return profileService.approvedDetailByResearchCycleId(researchCycleId)
          .map(ResponseEntity::ok)
          .orElseGet(() -> ResponseEntity.notFound().build());
    } catch (IllegalStateException ex) {
      LOGGER.error("Erro ao detalhar perfil MEI/autônomo aprovado do OPRM nichocnae (researchCycleId={})", researchCycleId, ex);
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }
}
