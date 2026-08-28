package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar o proprietário funcional de cada papel isolado do worker. */
class AutomaticExecutionControlTest {

  /** Mantém a revisão comercial sob o controle PLAY/STOP de Têmis. */
  @Test
  void reviewRoleUsesTemisControl() {
    assertThat(AutomaticExecutionControl.ownerAgentKey("review")).isEqualTo("meta-ad-approver");
  }

  /** Coloca a materialização visual sob o controle PLAY/STOP de Dédalo. */
  @Test
  void imageStudioRoleUsesDedaloControl() {
    assertThat(AutomaticExecutionControl.ownerAgentKey("image-studio"))
        .isEqualTo("landing-generator");
  }
}
