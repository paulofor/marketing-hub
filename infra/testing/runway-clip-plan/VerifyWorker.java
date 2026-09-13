import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.RunwayRouterRequestFactory;
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
    mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of(args[1]).toFile(), requests);
    System.out.println("PASS: pending real → executor real → dois clipes de 10s/5s; nenhuma chamada externa.");
  }
}
