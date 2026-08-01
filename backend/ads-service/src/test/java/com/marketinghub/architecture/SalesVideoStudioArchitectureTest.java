package com.marketinghub.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;

/** Garante que o Estúdio de Vídeo no backend fique separado dos módulos de integração. */
@AnalyzeClasses(packages = "com.marketinghub")
class SalesVideoStudioArchitectureTest {

  private static final String SALES_VIDEO_PACKAGE = "com.marketinghub.salesvideo";
  private static final String VIDEO_EXECUTOR_MODULE = "video-management-service";
  private static final List<String> FORBIDDEN_PROVIDER_RUNTIME_PACKAGES =
      List.of(
          "com.marketinghub.media.client",
          "org.springframework.web.reactive.function.client",
          "org.springframework.web.client",
          "java.net.http",
          "okhttp3",
          "retrofit2",
          "com.openai",
          "com.theokanning.openai");

  @ArchTest
  static final ArchRule salesVideoBackendMustNotIntegrateDirectlyWithVideoProviders =
      classes()
          .that()
          .resideInAPackage(SALES_VIDEO_PACKAGE + "..")
          .should(notAccessVideoProviderIntegrationRuntime())
          .because(
              "[ARQUITETURA] [BACKEND][SalesVideo Studio] o backend do Estúdio deve apenas"
                  + " persistir briefing, blueprint, jobs, eventos, assets e métricas; integrações"
                  + " com Luma, Kling, HeyGen, Runway, Veo ou outros providers pertencem ao módulo "
                  + VIDEO_EXECUTOR_MODULE);

  @ArchTest
  static final ArchRule salesVideoBackendMustNotDeclareProviderExecutionClasses =
      classes()
          .that()
          .resideInAPackage(SALES_VIDEO_PACKAGE + "..")
          .should(notDeclareVideoProviderExecutionClass())
          .because(
              "[ARQUITETURA] [BACKEND][SalesVideo Studio] classes de renderização, clientes,"
                  + " adaptadores e executores de provider não devem nascer no backend principal;"
                  + " o Marketing Hub cria e acompanha o trabalho, mas a execução fica no módulo "
                  + VIDEO_EXECUTOR_MODULE);

  /** Bloqueia dependências diretas em clientes HTTP, SDKs e clients de providers de vídeo. */
  private static ArchCondition<JavaClass> notAccessVideoProviderIntegrationRuntime() {
    return new ArchCondition<>(
        "[ARQUITETURA] [BACKEND][SalesVideo Studio] não acessa runtime de provider") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        item.getDirectDependenciesFromSelf()
            .forEach(
                dependency -> {
                  JavaClass targetClass = dependency.getTargetClass();
                  if (!isForbiddenProviderRuntimeDependency(targetClass)) {
                    return;
                  }
                  events.add(
                      SimpleConditionEvent.violated(
                          item,
                          "[ARQUITETURA] [BACKEND][SalesVideo Studio] classe="
                              + item.getName()
                              + " depende de integração operacional de vídeo: "
                              + dependency.getDescription()
                              + " (alvo="
                              + targetClass.getName()
                              + "). O backend deve expor contratos/pending/callbacks e persistir"
                              + " auditoria; chamadas HTTP/SDK de provider ficam no módulo "
                              + VIDEO_EXECUTOR_MODULE
                              + "."));
                });
      }
    };
  }

  /** Bloqueia nomes de classes que indicam execução direta de provider dentro do backend. */
  private static ArchCondition<JavaClass> notDeclareVideoProviderExecutionClass() {
    return new ArchCondition<>(
        "[ARQUITETURA] [BACKEND][SalesVideo Studio] não declara executor de provider") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        if (!hasForbiddenProviderExecutionName(item)) {
          return;
        }
        events.add(
            SimpleConditionEvent.violated(
                item,
                "[ARQUITETURA] [BACKEND][SalesVideo Studio] classe="
                    + item.getName()
                    + " parece implementar execução/adaptação de provider de vídeo. Use o backend"
                    + " para governança no-code, persistência e status; implemente a integração no"
                    + " módulo "
                    + VIDEO_EXECUTOR_MODULE
                    + "."));
      }
    };
  }

  /** Identifica dependências proibidas para o backend do Estúdio. */
  private static boolean isForbiddenProviderRuntimeDependency(JavaClass targetClass) {
    String targetPackage = targetClass.getPackageName();
    return FORBIDDEN_PROVIDER_RUNTIME_PACKAGES.stream().anyMatch(targetPackage::startsWith);
  }

  /** Identifica nomes que deslocariam integração operacional para o backend. */
  private static boolean hasForbiddenProviderExecutionName(JavaClass javaClass) {
    String simpleName = javaClass.getSimpleName().toLowerCase();
    boolean mentionsProvider =
        simpleName.contains("luma")
            || simpleName.contains("kling")
            || simpleName.contains("heygen")
            || simpleName.contains("runway")
            || simpleName.contains("veo")
            || simpleName.contains("provider");
    boolean mentionsExecutionRole =
        simpleName.endsWith("client")
            || simpleName.endsWith("adapter")
            || simpleName.endsWith("renderer")
            || simpleName.endsWith("executor")
            || simpleName.endsWith("worker");
    return mentionsProvider && mentionsExecutionRole;
  }
}
