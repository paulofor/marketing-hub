package com.marketinghub.videomanagement.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.videomanagement.service.provider.VideoProvider;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a arquitetura do video-management-service como executor operacional de vídeo. */
class ArquiteturaTest {
    private static final String BASE_PACKAGE = "com.marketinghub.videomanagement";
    private static final Set<String> PROVIDERS_CONCRETOS = Set.of(
            "RealVideoProvider",
            "StubVideoProvider",
            "VeoVideoProvider");

    private final JavaClasses classes = new ClassFileImporter().importPackages(BASE_PACKAGE);

    /** Garante que o núcleo operacional resolve providers por contrato, sem conhecer implementações concretas. */
    @Test
    void nucleoOperacionalNaoDeveDependerDeProvidersConcretos() {
        noClasses().that().resideInAPackage("..videomanagement.service..")
                .and().resideOutsideOfPackage("..videomanagement.service.provider..")
                .should().dependOnClassesThat().haveSimpleName("RealVideoProvider")
                .orShould().dependOnClassesThat().haveSimpleName("StubVideoProvider")
                .orShould().dependOnClassesThat().haveSimpleName("VeoVideoProvider")
                .because("[ARQUITETURA] O núcleo do executor de vídeo deve depender do contrato VideoProvider, nunca de provider concreto.")
                .check(classes);
    }

    /** Garante que toda implementação concreta de renderização obedece ao contrato único de etapa de vídeo. */
    @Test
    void providersConcretosDevemImplementarContratoDeVideo() {
        classes().that().resideInAPackage("..videomanagement.service.provider..")
                .and().haveSimpleNameEndingWith("VideoProvider")
                .should(implementarContratoVideoProvider())
                .because("[ARQUITETURA] Cada provider concreto é uma etapa plugável e deve implementar VideoProvider.")
                .check(classes);
    }

    /** Impede acoplamento direto entre providers, preservando substituição independente de VEO, real e stub. */
    @Test
    void providersConcretosNaoDevemDependerEntreSi() {
        classes().that().resideInAPackage("..videomanagement.service.provider..")
                .and().haveSimpleNameEndingWith("VideoProvider")
                .should(naoDependerDeOutroProviderConcreto())
                .because("[ARQUITETURA] Providers concretos de vídeo devem ser plugáveis e independentes entre si.")
                .check(classes);
    }

    /** Mantém chamadas externas, SDKs e tecnologias de execução fora do núcleo operacional do worker. */
    @Test
    void nucleoOperacionalNaoDeveDependerDeTecnologiasExternasDeRender() {
        noClasses().that().resideInAPackage("..videomanagement.service..")
                .and().resideOutsideOfPackage("..videomanagement.service.provider..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web.reactive.function.client..",
                        "software.amazon..",
                        "com.microsoft.playwright..",
                        "org.jsoup..",
                        "org.openqa.selenium..")
                .because("[ARQUITETURA] O núcleo operacional não deve chamar tecnologia externa de render; isso pertence ao provider ou client de infraestrutura.")
                .check(classes);
    }

    /** Protege o fluxo de entrada e callbacks oficiais usados pelo executor junto ao backend. */
    @Test
    void executorDeveUsarContratosInternosCanonicosDoBackend() throws IOException {
        String backendClient = Files.readString(Path.of("src/main/java/com/marketinghub/videomanagement/client/BackendVideoClient.java"));
        String assetClient = Files.readString(Path.of("src/main/java/com/marketinghub/videomanagement/client/VideoAssetClient.java"));

        assertThat(backendClient)
                .as("[ARQUITETURA] O executor deve iniciar consumo pela fila interna de jobs de vídeo.")
                .contains("/internal/video/jobs");
        assertThat(backendClient)
                .as("[ARQUITETURA] O executor deve reportar claim, heartbeat, progresso, sucesso, falha e expiração ao backend.")
                .contains("/internal/video/jobs/{jobId}/claim")
                .contains("/internal/video/jobs/{jobId}/heartbeat")
                .contains("/internal/video/jobs/{jobId}/progress")
                .contains("/internal/video/jobs/{jobId}/complete")
                .contains("/internal/video/jobs/{jobId}/fail")
                .contains("/internal/video/jobs/{jobId}/expired");
        assertThat(assetClient)
                .as("[ARQUITETURA] Assets finais do provider devem voltar ao backend pelo contrato interno de assets.")
                .contains("/internal/video/assets");
    }

    /** Protege o módulo contra avanço interno de pipeline sem contrato completo no backend. */
    @Test
    void executorNaoDeveDeclararProximaEtapaInternaSemContratoBackend() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        try (var paths = Files.walk(sourceRoot)) {
            String joinedSources = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::readUnchecked)
                    .reduce("", String::concat);

            assertThat(joinedSources)
                    .as("[ARQUITETURA] O video-management-service não deve retornar nextStageCode sem contrato backend/executor completo.")
                    .doesNotContain("nextStageCode");
        }
    }

    /** Verifica se providers concretos realmente implementam o contrato de renderização de vídeo. */
    private ArchCondition<JavaClass> implementarContratoVideoProvider() {
        return new ArchCondition<>("[ARQUITETURA] implementar VideoProvider") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!PROVIDERS_CONCRETOS.contains(item.getSimpleName())) {
                    return;
                }
                boolean implementaContrato = item.isAssignableTo(VideoProvider.class);
                if (!implementaContrato) {
                    events.add(SimpleConditionEvent.violated(item,
                            "[ARQUITETURA] " + item.getName() + " não implementa VideoProvider."));
                }
            }
        };
    }

    /** Verifica dependências diretas entre implementações concretas de provider. */
    private ArchCondition<JavaClass> naoDependerDeOutroProviderConcreto() {
        return new ArchCondition<>("[ARQUITETURA] não depender de outro provider concreto") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!PROVIDERS_CONCRETOS.contains(item.getSimpleName())) {
                    return;
                }
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetName = dependency.getTargetClass().getSimpleName();
                    if (PROVIDERS_CONCRETOS.contains(targetName) && !item.getSimpleName().equals(targetName)) {
                        events.add(SimpleConditionEvent.violated(item,
                                "[ARQUITETURA] " + item.getName() + " depende diretamente de " + targetName + "."));
                    }
                });
            }
        };
    }

    /** Lê um arquivo Java e propaga erro de leitura como falha objetiva do teste. */
    private String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("[ARQUITETURA] Não foi possível ler " + path, ex);
        }
    }
}
