import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExperimentLandingPhasesMain {
    private static final Path INPUT_DIR = Path.of("entrada");
    private static final Path WIREFRAME_FILE = INPUT_DIR.resolve("gera-wireframe.json");
    private static final Path COPY_FILE = INPUT_DIR.resolve("gera-copy.json");
    private static final Path OUTPUT_FILE = Path.of("testes/saídas/gera-wireframe-copy.txt");

    public static void main(String[] args) throws Exception {
        String wireframeJson = Files.readString(WIREFRAME_FILE, StandardCharsets.UTF_8);
        String copyJson = Files.readString(COPY_FILE, StandardCharsets.UTF_8);

        ExperimentLandingPhasesDto dto = new ExperimentLandingPhasesDto(wireframeJson, copyJson);

        Files.createDirectories(OUTPUT_FILE.getParent());
        String content = "=== Gera Wireframe ===\n"
                + dto.geraWireframeJson() + "\n\n"
                + "=== Gera Copy ===\n"
                + dto.geraCopyJson() + "\n";

        Files.writeString(OUTPUT_FILE, content, StandardCharsets.UTF_8);
        System.out.println("Arquivo gerado em: " + OUTPUT_FILE);
    }

    public record ExperimentLandingPhasesDto(
            String geraWireframeJson,
            String geraCopyJson
    ) {
    }
}
