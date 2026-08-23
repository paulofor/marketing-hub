package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

/** Protege a superfície pública contra autenticação baseada apenas no conhecimento do e-mail. */
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
}
