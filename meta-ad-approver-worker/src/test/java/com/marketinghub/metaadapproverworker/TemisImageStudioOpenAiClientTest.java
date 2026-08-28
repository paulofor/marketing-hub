package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: homologar os contratos HTTP de criação e edição visual de Íris. */
class TemisImageStudioOpenAiClientTest {
  private HttpServer server;
  private MetaAdApproverProperties properties;
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private final AtomicReference<String> requestContentType = new AtomicReference<>();

  /** Inicia uma API local segregada sem consumir a OpenAI real. */
  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/reference.png", this::reference);
    server.createContext("/v1/images/generations", this::openAi);
    server.createContext("/v1/images/edits", this::openAi);
    server.start();
    properties = new MetaAdApproverProperties();
    properties.setOpenAiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
    properties.setOpenAiApiKey("test-only-key");
    properties.setImageModel("gpt-image-2");
  }

  /** Encerra a API de homologação após cada cenário. */
  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  /** Confirma criação orientada por prova, modelo canônico e auditoria da peça. */
  @Test
  void createsPremiumCommercialAssetWithGptImage2() {
    String referenceUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.png";
    TemisImageStudioOpenAiClient.Result result =
        client().execute(job(List.of(referenceUrl), "CREATE"));

    assertThat(result.imageBytes()).isEqualTo("premium-image".getBytes(StandardCharsets.UTF_8));
    assertThat(result.model()).isEqualTo("gpt-image-2");
    assertThat(result.requestJson()).contains("ADS", "gpt-image-2", "produza");
    assertThat(result.responseJson())
        .contains("BINÁRIO PERSISTIDO SEPARADAMENTE", "image_sha256", "image_bytes")
        .doesNotContain(Base64.getEncoder().encodeToString(result.imageBytes()));
    assertThat(result.costUsd()).isEqualByComparingTo("0.00034000");
    assertThat(result.usageJson()).contains("input_tokens");
    assertThat(requestContentType.get()).startsWith("multipart/form-data");
    assertThat(requestBody.get()).contains("gpt-image-2");
  }

  /** Diferencia uma peça comercial de um entregável e proíbe prova visual inventada. */
  @Test
  void createsCommercialAssetFromApprovedProofWithoutCallingItDelivery() {
    String referenceUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.png";
    TemisImageStudioJob commercial =
        new TemisImageStudioJob(
            23L,
            4L,
            "CREATE",
            "Crie convite direto para o Kit WhatsApp Pronto",
            "Rigel - convite direto",
            List.of("ADS", "SOCIAL"),
            "1024x1536",
            "high",
            List.of(referenceUrl),
            "producer-23",
            playbook());

    TemisImageStudioOpenAiClient.Result result = client().execute(commercial);

    assertThat(result.requestJson())
        .contains("peça comercial, não um entregável")
        .contains("sem inventar tela, resultado, depoimento ou recurso")
        .doesNotContain("deve ser útil para a cliente final");
  }

  /** Confirma edição multipart usando a imagem real como referência, sem redesenho silencioso. */
  @Test
  void editsExistingDeliverableWithMultipartReference() {
    String referenceUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.png";

    TemisImageStudioOpenAiClient.Result result =
        client().execute(job(List.of(referenceUrl), "EDIT"));

    assertThat(result.requestJson())
        .contains("\"operation\":\"edit\"")
        .contains("sha256")
        .contains("mantenha todas as regiões");
    assertThat(requestContentType.get()).startsWith("multipart/form-data");
    assertThat(requestBody.get()).contains("name=\"image[]\"").contains("reference-bytes");
  }

  /** Usa exemplos aprovados como orientação sem afirmar que a criação não recebeu referências. */
  @Test
  void explainsApprovedExamplesInNewCreation() {
    String referenceUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.png";

    TemisImageStudioOpenAiClient.Result result =
        client().execute(job(List.of(referenceUrl), "CREATE"));

    assertThat(result.requestJson())
        .contains("criação orientada por exemplos premium aprovados")
        .doesNotContain("criação sem arquivo-base");
  }

  /** Confirma que um Story ocupa o quadro 9:16 sem completar a referência com barras. */
  @Test
  void expandsStoryReferenceAcrossNativeCanvas() {
    String referenceUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.png";
    TemisImageStudioJob story =
        new TemisImageStudioJob(
            22L,
            2L,
            "EDIT",
            "preserve o conteúdo útil",
            "Agenda Cheia - story-05",
            List.of("LANDING", "ADS", "SOCIAL"),
            "1152x2048",
            "high",
            List.of(referenceUrl),
            "producer-22",
            playbook());

    TemisImageStudioOpenAiClient.Result result = client().execute(story);

    assertThat(result.requestJson())
        .contains("componha todo o quadro nativo 9:16")
        .contains("nunca acrescente barras, áreas vazias ou preenchimento artificial");
  }

  /** Bloqueia finalidade pós-compra antes de qualquer request externo. */
  @Test
  void rejectsDeliveryPurposeOwnedByDedalo() {
    String referenceUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.png";
    TemisImageStudioJob invalid =
        new TemisImageStudioJob(
            24L,
            2L,
            "CREATE",
            "produza",
            "Entrega",
            List.of("DELIVERY"),
            "1024x1536",
            "high",
            List.of(referenceUrl),
            "producer-24",
            playbook());

    assertThatThrownBy(() -> client().execute(invalid))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("LANDING, ADS e SOCIAL");
    assertThat(requestBody.get()).isNull();
  }

  /** Bloqueia criação comercial livre sem prova real antes de chamar a API externa. */
  @Test
  void rejectsCommercialCreationWithoutProductEvidence() {
    assertThatThrownBy(() -> client().execute(job(List.of(), "CREATE")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("prova real aprovada");
    assertThat(requestBody.get()).isNull();
  }

  /** Cria o cliente exercitado contra a API de homologação. */
  private TemisImageStudioOpenAiClient client() {
    return new TemisImageStudioOpenAiClient(properties, new ObjectMapper());
  }

  /** Cria um job com segregação de plano e finalidades de reuso. */
  private TemisImageStudioJob job(List<String> references, String operation) {
    return new TemisImageStudioJob(
        21L,
        2L,
        operation,
        "produza uma peça premium do Agenda Cheia",
        "Post premium",
        List.of("LANDING", "ADS", "SOCIAL"),
        "1024x1536",
        "high",
        references,
        "producer-21",
        playbook());
  }

  /** Cria um playbook governado mínimo para comprovar sua injeção na produção. */
  private TemisVisualPlaybook playbook() {
    return new TemisVisualPlaybook(
        "temis-visual-playbook-v1",
        "agenda-cheia-feed",
        "CANONICAL_BASELINE",
        List.of("Preservar o produto real"),
        List.of("Não usar fotos genéricas de unhas"),
        List.of());
  }

  /** Entrega uma referência visual local válida para o cenário de edição. */
  private void reference(HttpExchange exchange) throws IOException {
    byte[] response = "reference-bytes".getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "image/png");
    exchange.sendResponseHeaders(200, response.length);
    exchange.getResponseBody().write(response);
    exchange.close();
  }

  /** Registra o request e devolve uma imagem base64 com usage auditável. */
  private void openAi(HttpExchange exchange) throws IOException {
    requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
    requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    String encoded =
        Base64.getEncoder().encodeToString("premium-image".getBytes(StandardCharsets.UTF_8));
    byte[] response =
        ("{\"data\":[{\"b64_json\":\""
                + encoded
                + "\"}],\"usage\":{\"input_tokens\":12,\"input_tokens_details\":{\"image_tokens\":10,\"text_tokens\":2},\"output_tokens\":8,\"output_tokens_details\":{\"image_tokens\":8,\"text_tokens\":1}}}")
            .getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, response.length);
    exchange.getResponseBody().write(response);
    exchange.close();
  }
}
