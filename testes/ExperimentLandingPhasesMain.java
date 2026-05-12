import com.marketinghub.geralanding.CopyProvisionalHtmlProcessor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExperimentLandingPhasesMain {
    private static final Path INPUT_DIR = Path.of("testes/entradas");
    private static final Path WIREFRAME_FILE = INPUT_DIR.resolve("gera-wireframe.json");
    private static final Path COPY_FILE = INPUT_DIR.resolve("gera-copy.json");
    private static final Path OUTPUT_FILE = Path.of("testes/saidas/gera-wireframe-copy.html");

    public static void main(String[] args) throws Exception {
        String wireframeJson = Files.readString(WIREFRAME_FILE, StandardCharsets.UTF_8);
        String copyJson = Files.readString(COPY_FILE, StandardCharsets.UTF_8);

        CopyProvisionalHtmlProcessor processor = new CopyProvisionalHtmlProcessor();
        String html = processor.process(wireframeJson, copyJson);

        Files.createDirectories(OUTPUT_FILE.getParent());
        Files.writeString(OUTPUT_FILE, html, StandardCharsets.UTF_8);
        System.out.println("Arquivo HTML gerado em: " + OUTPUT_FILE);
        System.out.println("--- INÍCIO HTML GERADO ---");
        System.out.println(html);
        System.out.println("--- FIM HTML GERADO ---");
    }
}
