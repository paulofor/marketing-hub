import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.RunwayRouterRequestFactory;
import com.marketinghub.videomanagement.client.ApolloPlanningAiClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.service.ApolloStoryboardPlanner;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Responsabilidade: conferir o contrato real exportado pelo backend no executor local sem rede. */
class VerifyWorker {
  /** Serializa o pending, executa a montagem real e compara clipes com o painel e o job de Apolo. */
  public static void main(String[] args) throws Exception {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    var contract = mapper.readTree(Files.readString(Path.of(args[0])));
    var pending = mapper.treeToValue(contract.path("pending"), ProviderPreflightJob.class);
    var requests = new RunwayRouterRequestFactory(new VideoManagementProperties()).build(pending);
    if (requests.size() != 2
        || contract.path("cycle").path("generationClipCount").asInt() != requests.size()
        || contract.path("metadata").path("sceneCount").asInt() != requests.size()) {
      throw new IllegalStateException("Quantidade divergente entre backend, executor e painel.");
    }
    int total = 0;
    for (int index = 0; index < requests.size(); index++) {
      var request = requests.get(index);
      int duration = ((Number) ((Map<?, ?>) request.get("input")).get("duration")).intValue();
      if (duration != (index == 0 ? 10 : 5) || request.containsKey("dryRun")) {
        throw new IllegalStateException("Payload faturável não preserva as durações de 10s e 5s.");
      }
      total += duration;
    }
    if (total != pending.targetDurationSeconds()
        || contract.path("metadata").path("publicationAllowed").asBoolean()) {
      throw new IllegalStateException("Duração final ou limite de publicação violados.");
    }
    verifyPlanner(mapper, contract.path("metadata"));
    mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of(args[1]).toFile(), requests);
    System.out.println("PASS: pending real → executor real → dois clipes de 10s/5s; nenhuma chamada externa.");
  }

  /** Consome os metadados exportados no planejador real, substituindo somente a resposta da IA. */
  private static void verifyPlanner(ObjectMapper mapper, JsonNode metadata) throws Exception {
    var properties = new VideoManagementProperties();
    properties.getApolloPlanner().setEnabled(true);
    var client = new ApolloPlanningAiClient(properties, WebClient.builder()) {
      /** Devolve cinco funções distintas e durações reais sem acessar provedor ou credencial. */
      @Override
      public JsonNode plan(Long jobId, JsonNode request) {
        var plan = mapper.createObjectNode();
        var cuts = plan.putArray("cuts");
        for (JsonNode original : metadata.path("cut_plan")) {
          var cut = cuts.addObject();
          cut.put("order", original.path("order").asInt());
          cut.put("durationSeconds", original.path("duration_seconds").asInt());
          cut.put("commercialRole", original.path("role").asText());
          cut.put("narrativePhase", original.path("narrative_phase").asText());
          cut.put("visualObjective", "Ação visual local " + original.path("order").asInt());
          cut.put("continuityAnchor", "Mesma personagem e ambiente");
          cut.put("reuseExistingMaterial", false);
          cut.put("postProductionText", "Copy local aprovada");
        }
        var response = mapper.createObjectNode();
        response.putArray("output").addObject().putArray("content").addObject()
            .put("type", "output_text").put("text", plan.toString());
        return response;
      }
    };
    var jobNode = mapper.createObjectNode();
    jobNode.put("id", 91001).put("profileId", 91001).put("jobType", "RENDER")
        .put("providerName", "RUNWAY_ROUTER").put("metadataJson", metadata.toString());
    var profileNode = mapper.createObjectNode();
    profileNode.put("id", 91001).put("targetDurationSeconds", 15);
    profileNode.putObject("latestScript").put("status", "APPROVED")
        .put("scriptText", "Texto sintético aprovado").put("hookText", "Gancho local")
        .put("ctaText", "Experimentar");
    var job = mapper.treeToValue(jobNode, SalesVideoJob.class);
    var profile = mapper.treeToValue(profileNode, SalesVideoProfile.class);
    var result = new ApolloStoryboardPlanner(properties, mapper, client)
        .planAndApprove(job, profile, (progress, status, message) -> {});
    var audit = mapper.readTree(result.metadataJson());
    if (!"APPROVED".equals(audit.path("apollo_planner_status").asText())
        || audit.path("cut_plan").size() != 5 || audit.path("publicationAllowed").asBoolean()) {
      throw new IllegalStateException("Contrato backend/planejador não habilita os cinco cortes seguros.");
    }
    System.out.println("PASS: metadados backend → planejador Apolo real → cinco cortes em quinze segundos.");
  }
}
