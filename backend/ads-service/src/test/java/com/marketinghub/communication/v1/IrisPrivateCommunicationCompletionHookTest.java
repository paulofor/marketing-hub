package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: comprovar revalidação no callback e impedir disputa entre handlers
 * especializados.
 */
class IrisPrivateCommunicationCompletionHookTest {
  private final ObjectMapper json = new ObjectMapper();
  private final IrisPrivateProductContext context = mock(IrisPrivateProductContext.class);
  private final IrisPrivateCommunicationCompletionHook hook =
      new IrisPrivateCommunicationCompletionHook(context, json);
  private final ObjectNode input = json.createObjectNode();
  private final AgentTask task = new AgentTask();

  /** Prepara uma entrada privada auditada sem permitir qualquer publicação ou venda. */
  @BeforeEach
  void setup() {
    task.setId(91001L);
    task.setSourceReference("product:91010@agent-validation-v1");
    task.setAssignedAgent(Agent.builder().agentKey("communication-director").build());
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    task.setProcessDefinition(process);
    task.setProcessActivityId("communicationContract");
    input
        .put("inputReadiness", "READY")
        .put("sourceReference", task.getSourceReference())
        .put("mode", "PRODUCT_PRIVATE")
        .put("prototypeVersion", "local-v3")
        .put("gateInstanceId", 91002);
    for (String key :
        List.of(
            "marketStrategicContract",
            "privatePrototypeAcceptance",
            "validationGate",
            "discoveryLineage")) input.putObject(key).put("synthetic", true);
    when(context.resolve(task.getSourceReference()))
        .thenAnswer(i -> Optional.of(json.convertValue(input, Map.class)));
  }

  /** Conclui somente a mensagem cuja entrada permanece idêntica à usada pelo executor. */
  @Test
  void acceptsSameInputAndDoesNotCompeteWithVisualHook() {
    assertThat(hook.supports(task)).isTrue();
    var request =
        new CompleteAgentTaskRequest(
            "{}",
            json.createObjectNode()
                .set("communicationInputReference", input.deepCopy())
                .toString());
    assertThat(hook.apply(task, request))
        .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    task.setProcessActivityId("nonAudiovisual");
    assertThat(hook.supports(task)).isFalse();
  }

  /** O transporte JSON preserva IDs Long do JPA mesmo quando o parser os lê como Integer. */
  @Test
  void acceptsJpaIdentifiersAfterJsonTransport() {
    Map<String, Object> current = new HashMap<>(json.convertValue(input, Map.class));
    current.put("gateInstanceId", 91002L);
    current.put(
        "marketStrategicContract",
        Map.of("strategistTaskId", 91003L, "contentHash", "a".repeat(64)));
    when(context.resolve(task.getSourceReference())).thenReturn(Optional.of(current));
    var request =
        new CompleteAgentTaskRequest(
            "{}",
            json.createObjectNode()
                .set("communicationInputReference", json.valueToTree(current))
                .toString());
    assertThat(hook.apply(task, request))
        .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    current.put("gateInstanceId", 91004L);
    assertThatThrownBy(() -> hook.apply(task, request))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Recusa alterações ocorridas durante a tarefa sem converter auditoria antiga em prova atual. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "sourceReference",
        "mode",
        "prototypeVersion",
        "gateInstanceId",
        "marketStrategicContract",
        "privatePrototypeAcceptance",
        "validationGate",
        "discoveryLineage",
        "inputReadiness"
      })
  void rejectsConcurrentInputChange(String key) {
    var request =
        new CompleteAgentTaskRequest(
            "{}",
            json.createObjectNode()
                .set("communicationInputReference", input.deepCopy())
                .toString());
    input.put(key, "alterado");
    assertThatThrownBy(() -> hook.apply(task, request))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
