package com.marketinghub.worker.architecture;

import com.marketinghub.worker.pipeline.StageProcessor;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Guarda arquitetural do pipeline executor GeraAnuncio v2 no AI Worker. */
@AnalyzeClasses(packages = "com.marketinghub", importOptions = ImportOption.DoNotIncludeTests.class)
class GeraAnuncioV2ArchitectureTest {
    private static final String PIPELINE_ROOT = "com.marketinghub.pipelines.geracaoanuncios.v1";

    @ArchTest
    static final ArchRule nucleo_geraanuncio_v2_nao_deve_depender_de_etapas = noClasses()
            .that()
            .resideInAPackage(PIPELINE_ROOT)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(PIPELINE_ROOT + ".*..")
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] o núcleo genérico não pode conhecer etapas concretas");

    @ArchTest
    static final ArchRule etapas_geraanuncio_v2_nao_devem_depender_umas_das_outras = slices()
            .matching("com.marketinghub.pipelines.geracaoanuncios.v1.(*)..")
            .should()
            .notDependOnEachOther()
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] etapas concretas não podem depender umas das outras");

    @ArchTest
    static final ArchRule etapas_geraanuncio_v2_nao_devem_ter_ciclos = slices()
            .matching("com.marketinghub.pipelines.geracaoanuncios.v1.(*)..")
            .should()
            .beFreeOfCycles()
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] etapas do pipeline não podem formar ciclos");

    @ArchTest
    static final ArchRule processors_geraanuncio_v2_devem_implementar_stage_processor = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT + "..")
            .and()
            .haveSimpleNameEndingWith("Processor")
            .should()
            .implement(StageProcessor.class)
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] processors concretos devem implementar StageProcessor");

    @ArchTest
    static final ArchRule nucleo_geraanuncio_v2_nao_deve_depender_de_tecnologias_concretas = noClasses()
            .that()
            .resideInAPackage(PIPELINE_ROOT)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web.reactive.function.client..", "org.jsoup..", "com.microsoft.playwright..", "software.amazon.awssdk..", "okhttp3..")
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] o núcleo deve depender de contratos, não de tecnologias concretas");

    /** Garante que o módulo externo espelhe as etapas backend e consuma o pending canônico de cada etapa. */
    @ArchTest
    static void modulo_externo_geraanuncio_v2_deve_espelhar_subpacotes_backend_e_consumir_pending(JavaClasses importedClasses) {
        importedClasses.stream().filter(javaClass -> javaClass.getPackageName().startsWith(PIPELINE_ROOT)).findAny();
        Path repositoryRoot = repositoryRoot();
        Path backendRoot = repositoryRoot.resolve(Path.of(
                "backend", "ads-service", "src", "main", "java", "com", "marketinghub", "pipelines", "aiworker", "geracaoanuncios", "v1"));
        Path workerRoot = repositoryRoot.resolve(Path.of(
                "ai-worker", "src", "main", "java", "com", "marketinghub", "pipelines", "geracaoanuncios", "v1"));
        Set<String> backendStages = directSubpackages(backendRoot);
        Set<String> workerStages = directSubpackages(workerRoot);
        List<String> violations = new ArrayList<>();
        if (!workerStages.equals(backendStages)) {
            violations.add("[ARQUITETURA] [AI Worker][GeraAnuncio v2] subpacotes do módulo externo devem espelhar o backend; backend="
                    + backendStages + "; ai-worker=" + workerStages);
        }
        backendStages.forEach(stage -> validatePendingEndpoint(workerRoot, stage, violations));
        failWithArchitectureViolations(violations);
    }

    /** Valida que a etapa do AI Worker chama o endpoint pending canônico da etapa par no backend. */
    private static void validatePendingEndpoint(Path workerRoot, String stage, List<String> violations) {
        Path stageDirectory = workerRoot.resolve(stage);
        String expectedEndpoint = "/internal/aiworker/geracaoanuncios/v1/" + stage + "/stage-executions/pending";
        if (!Files.isDirectory(stageDirectory)) {
            violations.add("[ARQUITETURA] [AI Worker][GeraAnuncio v2] etapa " + stage
                    + " deve existir no módulo externo antes de validar pending");
            return;
        }
        boolean containsPendingEndpoint = javaFiles(stageDirectory).stream().anyMatch(path -> fileContains(path, expectedEndpoint));
        if (!containsPendingEndpoint) {
            violations.add("[ARQUITETURA] [AI Worker][GeraAnuncio v2] etapa " + stage
                    + " deve chamar o endpoint pending canônico " + expectedEndpoint);
        }
    }

    /** Lista os subpacotes diretos de um pacote Java raiz. */
    private static Set<String> directSubpackages(Path root) {
        Set<String> directories = new TreeSet<>();
        if (!Files.isDirectory(root)) {
            return directories;
        }
        try (Stream<Path> stream = Files.list(root)) {
            stream.filter(Files::isDirectory).map(path -> path.getFileName().toString()).forEach(directories::add);
        } catch (IOException ex) {
            throw new AssertionError("[ARQUITETURA] falha ao listar subpacotes de " + root + ": " + ex.getMessage(), ex);
        }
        return directories;
    }

    /** Lista arquivos Java dentro de uma etapa. */
    private static List<Path> javaFiles(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException ex) {
            throw new AssertionError("[ARQUITETURA] falha ao listar arquivos Java de " + root + ": " + ex.getMessage(), ex);
        }
    }

    /** Verifica se o arquivo contém o texto esperado. */
    private static boolean fileContains(Path path, String expectedText) {
        try {
            return Files.readString(path).contains(expectedText);
        } catch (IOException ex) {
            throw new AssertionError("[ARQUITETURA] falha ao ler arquivo " + path + ": " + ex.getMessage(), ex);
        }
    }

    /** Localiza a raiz do repositório a partir do diretório atual de execução dos testes. */
    private static Path repositoryRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("backend/ads-service")) && Files.isDirectory(current.resolve("ai-worker"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("[ARQUITETURA] raiz do repositório não foi localizada para validar GeraAnuncio v2");
    }

    /** Falha com mensagem de arquitetura consolidada quando houver violações. */
    private static void failWithArchitectureViolations(List<String> violations) {
        if (!violations.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), violations));
        }
    }
}
