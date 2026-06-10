package com.marketinghub.oprm.nichocnae.meiaudienceprofile.controller;

import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.BackendMeiAudienceProfileService;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.detailAudienceProfile.MeiAudienceProfileDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller OPRM responsável por expor o perfil final aprovado de público-alvo MEI/autônomo. */
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
