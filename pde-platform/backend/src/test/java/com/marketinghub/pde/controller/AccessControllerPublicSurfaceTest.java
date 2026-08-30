package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/** Protege a superfície de acesso contra autenticação fraca e bearer inserido em caminhos HTTP. */
class AccessControllerPublicSurfaceTest {

    /** Confirma que as rotas legadas capazes de devolver bearer token não existem no controller. */
    @Test
    void doesNotExposeRegisterOrEmailLoginRoutes() {
        Set<String> postMappings = Arrays.stream(AccessController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .collect(Collectors.toSet());

        assertThat(postMappings).doesNotContain("/register", "/login", "/checkout");
        assertThat(postMappings).contains("/magic-link", "/login-link", "/google");
    }

    /** Garante que download não carregue o bearer token no caminho registrado ou compartilhado. */
    @Test
    void exposesDeliveryDownloadOnlyThroughTokenlessPath() {
        Set<String> getMappings = Arrays.stream(AccessController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(GetMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .collect(Collectors.toSet());

        assertThat(getMappings)
                .contains("/deliveries/{missionId}/download")
                .doesNotContain("/{token}/deliveries/{missionId}/download");
    }

    /** Bloqueia qualquer rota da cliente que volte a transportar o bearer como segmento da URL. */
    @Test
    void exposesAllAuthenticatedCustomerOperationsThroughTokenlessPaths() {
        Set<String> mappings = Arrays.stream(AccessController.class.getDeclaredMethods())
                .flatMap(method -> {
                    GetMapping get = method.getAnnotation(GetMapping.class);
                    PostMapping post = method.getAnnotation(PostMapping.class);
                    if (get != null) {
                        return Arrays.stream(get.value());
                    }
                    return post == null ? java.util.stream.Stream.<String>empty() : Arrays.stream(post.value());
                })
                .collect(Collectors.toSet());

        assertThat(mappings)
                .contains(
                        "/workspace",
                        "/missions/{missionId}/complete",
                        "/missions/{missionId}/interactions",
                        "/support-requests",
                        "/privacy-requests")
                .allSatisfy(path -> assertThat(path).doesNotContain("{token}", "{accessToken}"));
    }

    /** Impede que IA, operação assistida ou QA interno reintroduzam a credencial em suas URLs. */
    @Test
    void keepsEveryAuthenticatedAccessControllerPathTokenless() {
        Set<String> mappings = Arrays.stream(new Class<?>[] {
                    AccessController.class,
                    AiGuidanceController.class,
                    AssistedOperationController.class,
                    InternalAccessController.class
                })
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .flatMap(method -> {
                    GetMapping get = method.getAnnotation(GetMapping.class);
                    PostMapping post = method.getAnnotation(PostMapping.class);
                    if (get != null) {
                        return Arrays.stream(get.value());
                    }
                    return post == null ? java.util.stream.Stream.<String>empty() : Arrays.stream(post.value());
                })
                .collect(Collectors.toSet());

        assertThat(mappings)
                .contains(
                        "/workspace",
                        "/api/pde/access/missions/{missionId}/ai-guidance",
                        "/api/pde/access/ai-guidance/{requestId}",
                        "/missions/{missionId}/complete",
                        "/expire")
                .allSatisfy(path -> assertThat(path).doesNotContain("{token}", "{accessToken}"));
    }
}
