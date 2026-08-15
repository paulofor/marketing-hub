package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: proteger o contrato estruturado usado na geração de copy dos criativos. */
class CreativeChatGptClientContractTest {

  /** Exige objeto-raiz compatível com a Responses API e a coleção obrigatória de criativos. */
  @Test
  void usesObjectRootSchemaWithRequiredCreativesEnvelope() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode schema =
        mapper.readTree(
            new ClassPathResource("prompts/creative/meta-ad-copy-schema.json").getInputStream());

    assertThat(schema.path("type").asText()).isEqualTo("object");
    assertThat(schema.path("required").toString()).contains("creatives");
    assertThat(schema.path("properties").path("creatives").path("type").asText())
        .isEqualTo("array");
  }

  /** Desserializa o envelope e aplica o estado inicial canônico em cada copy retornada. */
  @Test
  @SuppressWarnings("unchecked")
  void parsesCreativesFromStructuredEnvelope() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    CreativeChatGptClient client =
        new CreativeChatGptClient(
            WebClient.builder(),
            mapper,
            "",
            "http://localhost",
            "gpt-5.2",
            Duration.ofMillis(1),
            Duration.ofSeconds(1),
            mock(AiGenerationRecorder.class));
    Method parser = CreativeChatGptClient.class.getDeclaredMethod("parseContent", String.class);
    parser.setAccessible(true);

    List<CreateCreativeRequest> creatives =
        (List<CreateCreativeRequest>)
            parser.invoke(
                client,
                "{\"creatives\":[{\"headline\":\"Kit visual\",\"primaryText\":\"Peças prontas\",\"description\":\"Veja o kit\",\"cta\":\"Comprar\"}]}");

    assertThat(creatives).hasSize(1);
    assertThat(creatives.getFirst().getHeadline()).isEqualTo("Kit visual");
    assertThat(creatives.getFirst().getStatus()).isEqualTo(CreativeStatus.DRAFT);
  }
}
