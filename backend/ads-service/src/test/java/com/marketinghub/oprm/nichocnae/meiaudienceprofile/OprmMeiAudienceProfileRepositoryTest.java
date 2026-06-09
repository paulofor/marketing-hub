package com.marketinghub.oprm.nichocnae.meiaudienceprofile;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/** Testes responsáveis por validar a persistência do perfil de público-alvo MEI/autônomo. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class OprmMeiAudienceProfileRepositoryTest {
  @Autowired OprmMeiAudienceProfileRepository repository;

  /** Valida que o contrato de público-alvo MEI/autônomo persiste sem campos de produto, oferta ou campanha. */
  @Test
  void saveAndFindByResearchCycleKeepsAudienceContractTraceable() {
    OprmMeiAudienceProfile profile = new OprmMeiAudienceProfile();
    profile.setResearchCycleId(1001L);
    profile.setRoutineCardId(2002L);
    profile.setSourceNicheCandidateId(3003L);
    profile.setMarketNicheId(4004L);
    profile.setCnaeCode("9602501");
    profile.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    profile.setNeutralNicheName("serviços pessoais de beleza");
    profile.setAudienceName("manicures MEI que atendem por agenda própria");
    profile.setOccupationTerms("manicure autônoma; nail designer MEI; profissional de alongamento de unhas");
    profile.setWorkMode("Atendimento em domicílio, salão parceiro ou espaço próprio pequeno.");
    profile.setCustomerAcquisitionBehavior("Captação por indicação, WhatsApp, Instagram e clientes recorrentes.");
    profile.setDailyRoutineSummary("Agenda clientes, compra materiais, atende, cobra e reorganiza horários.");
    profile.setRecurringTasksSummary("Reposição de insumos, confirmação de agenda e manutenção de relacionamento.");
    profile.setOperationalPainsSummary("Cancelamentos, atrasos, retrabalho e compra de material sem previsibilidade.");
    profile.setEmotionalPainsSummary("Medo de renda instável e insegurança para cobrar preço justo.");
    profile.setDreamsSummary("Ter agenda cheia, renda previsível e reconhecimento profissional.");
    profile.setFearsSummary("Perder clientes, receber avaliações ruins e ficar sem caixa.");
    profile.setLanguagePatterns("agenda cheia; cliente fixa; meu próprio horário; cobrar certo");
    profile.setChannelsUsed("WhatsApp; Instagram; Google Perfil da Empresa");
    profile.setRecentSourceSummary("Fontes brasileiras recentes sobre rotina de manicures autônomas e MEI.");
    profile.setAutonomousProfessionalFitScore(92);
    profile.setBehavioralEvidenceScore(88);
    profile.setSourceFreshnessScore(81);
    profile.setOutdatedSourceRiskScore(12);
    profile.setStructuredBusinessDriftRiskScore(9);
    profile.setSolutionLanguageRiskScore(0);
    Instant now = Instant.parse("2026-06-09T00:00:00Z");
    profile.setCreatedAt(now);
    profile.setUpdatedAt(now);

    repository.saveAndFlush(profile);

    assertThat(repository.existsByResearchCycleId(1001L)).isTrue();
    assertThat(repository.findFirstByResearchCycleIdOrderByIdDesc(1001L))
        .get()
        .extracting(
            OprmMeiAudienceProfile::getAudienceName,
            OprmMeiAudienceProfile::getCnaeCode,
            OprmMeiAudienceProfile::getAutonomousProfessionalFitScore,
            OprmMeiAudienceProfile::getSolutionLanguageRiskScore)
        .containsExactly(
            "manicures MEI que atendem por agenda própria", "9602501", 92, 0);
    assertThat(repository.findByCnaeCodeOrderByCreatedAtDesc("9602501"))
        .extracting(OprmMeiAudienceProfile::getResearchCycleId)
        .containsExactly(1001L);
  }
}
