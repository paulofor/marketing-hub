package com.marketinghub.product.executionprofile.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.product.executionprofile.v1.*;
import com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileView.ProcessReference;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract.Capability;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que dispensa de formato elimine um gate ou trave o consumidor BPM. */
class ExecutionProfileOmissionsTest {
  /** Persiste prova reconhecida pelo worker sem contar a atividade como objetivo atingido. */
  @Test
  void persistsProofRecognizedByQueue() {
    var context = mock(ExecutionProfileContext.class);
    var definitions = mock(BusinessProcessActivityDefinitionRepository.class);
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var profile = new ExecutionProfile();
    profile.setId(2L);
    profile.setRevisionNumber(1);
    var binding = new ExecutionProfileBinding();
    binding.setId(3L);
    binding.setSourceReference("experiment:94001");
    var definition = ExecutionProfileLocalApplication.audio();
    when(context.contract(profile))
        .thenReturn(ExecutionProfileRulesTest.contract(Capability.PERSONALIZED_IMAGES));
    when(context.composition(profile))
        .thenReturn(
            List.of(
                new ProcessReference(94103L, "pde-construction-approval", 1, "Construção", null)));
    when(definitions.findByProcessDefinitionIdAndActivityId(94103L, "audiovisual"))
        .thenReturn(Optional.of(definition));
    var service =
        new ExecutionProfileOmissions(context, definitions, instances, new ObjectMapper());
    service.record(profile, binding);
    var captured = org.mockito.ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(captured.capture());
    var proof = captured.getValue();
    assertThat(BusinessProcessOptionalActivity.isOmitted(proof)).isTrue();
    assertThat(proof.isObjectiveAchieved()).isFalse();
    definition.setOwnerName("Pessoa responsável");
    assertThat(BusinessProcessOptionalActivity.isOmitted(proof)).isFalse();
    assertThatThrownBy(() -> service.record(profile, binding))
        .hasMessageContaining("não pode ser dispensada");
  }
}
