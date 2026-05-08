import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WireframeMain {
    public static void main(String[] args) throws IOException {
        Path input = Path.of("testes/entradas/geralanding-wirefram.json");
        Path output = Path.of("testes/saídas/wireframe.html");

        String json = Files.readString(input, StandardCharsets.UTF_8);
        WireframeHtmlGenerator generator = new WireframeHtmlGenerator();
        String html = generator.generateFromJson(json);

        Files.createDirectories(output.getParent());
        Files.writeString(output, html, StandardCharsets.UTF_8);
        System.out.println("Arquivo gerado em: " + output);
    }
}
