package com.marketinghub.worker.architecture;

import com.marketinghub.worker.pipeline.StageProcessor;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Guarda arquitetural do pipeline executor GeracaoAnuncios v1 no AI Worker. */
@AnalyzeClasses(packages = "com.marketinghub", importOptions = ImportOption.DoNotIncludeTests.class)
class GeracaoAnunciosV1ArchitectureTest {
    private static final String PIPELINE_ROOT = "com.marketinghub.pipelines.geracaoanuncios.v1";
    private static final List<String> REQUIRED_STAGE_CLASS_SUFFIXES = List.of(
            "BackendClient",
            "ExecutionScheduler",
            "Input",
            "Output",
            "PromptBuilder",
            "ResponseHandler",
            "ResponseValidator",
            "WorkerConfiguration",
            "WorkerProperties");

    @ArchTest
    static final ArchRule nucleo_geracaoanuncios_v1_nao_deve_depender_de_etapas = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT)
            .should(naoDependerDeEtapasConcretas())
            .because("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] o núcleo genérico não pode conhecer etapas concretas");

    @ArchTest
    static final ArchRule etapas_geracaoanuncios_v1_nao_devem_depender_umas_das_outras = slices()
            .matching("com.marketinghub.pipelines.geracaoanuncios.v1.(*)..")
            .should()
            .notDependOnEachOther()
            .because("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] etapas concretas não podem depender umas das outras");

    @ArchTest
    static final ArchRule etapas_geracaoanuncios_v1_nao_devem_ter_ciclos = slices()
            .matching("com.marketinghub.pipelines.geracaoanuncios.v1.(*)..")
            .should()
            .beFreeOfCycles()
            .because("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] etapas do pipeline não podem formar ciclos");

    @ArchTest
    static final ArchRule processors_geracaoanuncios_v1_devem_implementar_stage_processor = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT + "..")
            .and()
            .haveSimpleNameEndingWith("Processor")
            .should()
            .implement(StageProcessor.class)
            .because("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] processors concretos devem implementar StageProcessor");

    @ArchTest
    static final ArchRule nucleo_geracaoanuncios_v1_nao_deve_depender_de_tecnologias_concretas = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT)
            .should(naoDependerDeTecnologiasConcretas())
            .because("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] o núcleo deve depender de contratos, não de tecnologias concretas");

    /** Bloqueia dependência do núcleo em qualquer subpacote de etapa concreta. */
    private static ArchCondition<JavaClass> naoDependerDeEtapasConcretas() {
        return new ArchCondition<>("[ARQUITETURA] não depender de etapas concretas do GeracaoAnuncios v1") {
            /** Avalia dependências diretas da classe do núcleo contra subpacotes de etapa. */
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    if (targetPackage.startsWith(PIPELINE_ROOT + ".")) {
                        events.add(SimpleConditionEvent.violated(item, "[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] "
                                + item.getName() + " depende da etapa concreta " + dependency.getTargetClass().getName()));
                    }
                }
            }
        };
    }

    /** Bloqueia tecnologias concretas no núcleo declarativo do pipeline. */
    private static ArchCondition<JavaClass> naoDependerDeTecnologiasConcretas() {
        List<String> pacotesTecnicos = Arrays.asList(
                "org.springframework.web.reactive.function.client",
                "org.jsoup",
                "com.microsoft.playwright",
                "software.amazon.awssdk",
                "okhttp3");
        return new ArchCondition<>("[ARQUITETURA] não depender de tecnologias concretas no núcleo") {
            /** Avalia se alguma dependência direta aponta para tecnologia permitida apenas em etapas/infra. */
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    Optional<String> pacoteBloqueado = pacotesTecnicos.stream()
                            .filter(targetPackage::startsWith)
                            .findFirst();
                    pacoteBloqueado.ifPresent(pacote -> events.add(SimpleConditionEvent.violated(item,
                            "[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] " + item.getName()
                                    + " depende de tecnologia concreta " + dependency.getTargetClass().getName()
                                    + " do pacote " + pacote)));
                }
            }
        };
    }


    /** Garante que cada etapa concreta tenha somente as nove classes padronizadas do executor. */
    @ArchTest
    static void subpacotes_geracaoanuncios_v1_devem_ter_apenas_nove_classes_padronizadas(JavaClasses importedClasses) {
        importedClasses.stream().filter(javaClass -> javaClass.getPackageName().startsWith(PIPELINE_ROOT)).findAny();
        Path workerRoot = repositoryRoot().resolve(Path.of(
                "ai-worker", "src", "main", "java", "com", "marketinghub", "pipelines", "geracaoanuncios", "v1"));
        List<String> violations = new ArrayList<>();
        directSubpackages(workerRoot).forEach(stage -> validateStageClassPattern(workerRoot.resolve(stage), stage, violations));
        failWithArchitectureViolations(violations);
    }

    /** Valida quantidade, prefixo e sufixos obrigatórios das classes Java de uma etapa. */
    private static void validateStageClassPattern(Path stageDirectory, String stage, List<String> violations) {
        List<String> classNames = javaFiles(stageDirectory).stream()
                .map(path -> path.getFileName().toString().replaceFirst("\\.java$", ""))
                .sorted()
                .toList();
        if (classNames.size() != REQUIRED_STAGE_CLASS_SUFFIXES.size()) {
            violations.add("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] etapa " + stage
                    + " deve ter exatamente " + REQUIRED_STAGE_CLASS_SUFFIXES.size()
                    + " classes Java padronizadas; encontradas=" + classNames);
        }
        Set<String> detectedPrefixes = detectedStagePrefixes(classNames);
        if (detectedPrefixes.size() != 1) {
            violations.add("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] etapa " + stage
                    + " deve usar um único <nome-etapa> antes dos sufixos padronizados; prefixos="
                    + detectedPrefixes + "; classes=" + classNames);
            return;
        }
        String stageClassPrefix = detectedPrefixes.iterator().next();
        Set<String> expectedClassNames = new TreeSet<>();
        REQUIRED_STAGE_CLASS_SUFFIXES.forEach(suffix -> expectedClassNames.add(stageClassPrefix + suffix));
        Set<String> actualClassNames = new TreeSet<>(classNames);
        if (!actualClassNames.equals(expectedClassNames)) {
            violations.add("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] etapa " + stage
                    + " deve conter somente as classes " + expectedClassNames
                    + "; encontradas=" + actualClassNames);
        }
    }

    /** Identifica o prefixo de etapa removendo os sufixos arquiteturais permitidos. */
    private static Set<String> detectedStagePrefixes(List<String> classNames) {
        Set<String> prefixes = new TreeSet<>();
        for (String className : classNames) {
            Optional<String> suffix = REQUIRED_STAGE_CLASS_SUFFIXES.stream().filter(className::endsWith).findFirst();
            if (suffix.isPresent()) {
                prefixes.add(className.substring(0, className.length() - suffix.get().length()));
            } else {
                prefixes.add(className);
            }
        }
        return prefixes;
    }

    /** Garante que o módulo externo espelhe as etapas backend e consuma o pending canônico de cada etapa. */
    @ArchTest
    static void modulo_externo_geracaoanuncios_v1_deve_espelhar_subpacotes_backend_e_consumir_pending(JavaClasses importedClasses) {
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
            violations.add("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] subpacotes do módulo externo devem espelhar o backend; backend="
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
            violations.add("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] etapa " + stage
                    + " deve existir no módulo externo antes de validar pending");
            return;
        }
        boolean containsPendingEndpoint = javaFiles(stageDirectory).stream().anyMatch(path -> fileContains(path, expectedEndpoint));
        if (!containsPendingEndpoint) {
            violations.add("[ARQUITETURA] [AI Worker][GeracaoAnuncios v1] etapa " + stage
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
        throw new AssertionError("[ARQUITETURA] raiz do repositório não foi localizada para validar GeracaoAnuncios v1");
    }

    /** Falha com mensagem de arquitetura consolidada quando houver violações. */
    private static void failWithArchitectureViolations(List<String> violations) {
        if (!violations.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), violations));
        }
    }
}
