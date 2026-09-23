package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar que a ordem operacional vem do grafo BPM versionado. */
class BusinessProcessGraphTopologyTest {
  /** Ignora ordem física e de declaração quando os fluxos definem outra sequência causal. */
  @Test
  void ordersActivitiesByCausalGraphAndAppendsHistoricalEntries() throws Exception {
    var diagram =
        new ObjectMapper()
            .readTree(
                """
                {
                  "nodes":[
                    {"id":"start","type":"START"},
                    {"id":"authorization","type":"TASK"},
                    {"id":"review","type":"TASK"},
                    {"id":"preparation","type":"TASK"},
                    {"id":"end","type":"END"}
                  ],
                  "flows":[
                    {"from":"start","to":"preparation"},
                    {"from":"preparation","to":"review"},
                    {"from":"review","to":"authorization"},
                    {"from":"authorization","to":"end"}
                  ]
                }
                """);

    var ordered =
        BusinessProcessGraphTopology.orderActivities(
            diagram,
            List.of("authorization", "historical", "review", "preparation"),
            value -> value);

    assertThat(ordered).containsExactly("preparation", "review", "authorization", "historical");
  }

  /** Mantém a ordem persistida somente para definições históricas que não possuem topologia. */
  @Test
  void preservesLegacyOrderWithoutExecutableGraph() throws Exception {
    var diagram = new ObjectMapper().readTree("{\"nodes\":[]}");

    assertThat(
            BusinessProcessGraphTopology.orderActivities(
                diagram, List.of("second", "first"), value -> value))
        .containsExactly("second", "first");
  }
}
