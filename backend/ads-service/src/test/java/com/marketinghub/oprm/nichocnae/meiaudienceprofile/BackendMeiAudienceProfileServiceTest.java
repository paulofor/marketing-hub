package com.marketinghub.oprm.nichocnae.meiaudienceprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.BackendMeiAudienceProfileService;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.upsertAudienceProfile.UpsertMeiAudienceProfileRequest;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Testes responsáveis por validar a orquestração do serviço de perfil MEI/autônomo. */
class BackendMeiAudienceProfileServiceTest {
  private final OprmMeiAudienceProfileRepository repository = mock(OprmMeiAudienceProfileRepository.class);
  private final BackendMeiAudienceProfileService service = new BackendMeiAudienceProfileService(repository);

  /** Valida que o serviço grava somente campos de público-alvo e normaliza scores ausentes para zero. */
  @Test
  void upsertAudienceProfilePersistsAudienceContractWithoutCommercialFields() {
    when(repository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.empty());
    when(repository.save(any(OprmMeiAudienceProfile.class))).thenAnswer(invocation -> {
      OprmMeiAudienceProfile profile = invocation.getArgument(0);
      profile.setId(55L);
      return profile;
    });

    var response = service.upsertAudienceProfile(request());

    ArgumentCaptor<OprmMeiAudienceProfile> captor = ArgumentCaptor.forClass(OprmMeiAudienceProfile.class);
    verify(repository).save(captor.capture());
    OprmMeiAudienceProfile saved = captor.getValue();
    assertThat(saved.getResearchCycleId()).isEqualTo(1001L);
    assertThat(saved.getAudienceName()).isEqualTo("manicures MEI que atendem por agenda própria");
    assertThat(saved.getAutonomousProfessionalFitScore()).isEqualTo(92);
    assertThat(saved.getSourceFreshnessScore()).isZero();
    assertThat(saved.getSolutionLanguageRiskScore()).isZero();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(response.id()).isEqualTo(55L);
    assertThat(response.researchCycleId()).isEqualTo(1001L);
  }

  /** Valida que o serviço entrega o detalhe completo a partir do ciclo de pesquisa. */
  @Test
  void detailByResearchCycleIdReturnsAudienceProfileDetail() {
    OprmMeiAudienceProfile profile = new OprmMeiAudienceProfile();
    profile.setId(55L);
    profile.setResearchCycleId(1001L);
    profile.setCnaeCode("9602501");
    profile.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    profile.setNeutralNicheName("serviços pessoais de beleza");
    profile.setAudienceName("manicures MEI que atendem por agenda própria");
    profile.setAutonomousProfessionalFitScore(92);
    profile.setBehavioralEvidenceScore(88);
    profile.setSourceFreshnessScore(81);
    profile.setOutdatedSourceRiskScore(12);
    profile.setStructuredBusinessDriftRiskScore(9);
    profile.setSolutionLanguageRiskScore(0);
    when(repository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(profile));

    var detail = service.detailByResearchCycleId(1001L);

    assertThat(detail).get().satisfies(response -> {
      assertThat(response.id()).isEqualTo(55L);
      assertThat(response.audienceName()).isEqualTo("manicures MEI que atendem por agenda própria");
      assertThat(response.cnaeCode()).isEqualTo("9602501");
    });
  }

  /** Monta uma requisição válida de perfil MEI/autônomo sem qualquer campo comercial. */
  private UpsertMeiAudienceProfileRequest request() {
    return new UpsertMeiAudienceProfileRequest(
        1001L,
        2002L,
        3003L,
        4004L,
        "9602501",
        "Cabeleireiros, manicure e pedicure",
        "serviços pessoais de beleza",
        "manicures MEI que atendem por agenda própria",
        "manicure autônoma; nail designer MEI; profissional de alongamento de unhas",
        "Atendimento em domicílio, salão parceiro ou espaço próprio pequeno.",
        "Captação por indicação, WhatsApp, Instagram e clientes recorrentes.",
        "Agenda clientes, compra materiais, atende, cobra e reorganiza horários.",
        "Reposição de insumos, confirmação de agenda e manutenção de relacionamento.",
        "Cancelamentos, atrasos, retrabalho e compra de material sem previsibilidade.",
        "Medo de renda instável e insegurança para cobrar preço justo.",
        "Ter agenda cheia, renda previsível e reconhecimento profissional.",
        "Perder clientes, receber avaliações ruins e ficar sem caixa.",
        "agenda cheia; cliente fixa; meu próprio horário; cobrar certo",
        "WhatsApp; Instagram; Google Perfil da Empresa",
        "Fontes brasileiras recentes sobre rotina de manicures autônomas e MEI.",
        92,
        88,
        null,
        null,
        9,
        null);
  }
}
