package com.marketinghub.payments.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;

/** Protege o contrato HTTP dos endpoints de checkout temporário. */
class CheckoutControllerContractTest {

    /** Garante que os parâmetros de rota não dependam da opção de compilação {@code -parameters}. */
    @Test
    void declaresTemporaryCheckoutPathVariableNamesExplicitly() {
        assertPathVariableName("temporaryStatus", "productKey");
        assertPathVariableName("restoreTemporary", "productKey");
        assertPathVariableName("redirectTemporary", "productKey");
    }

    /** Verifica o nome explícito do parâmetro de rota no método informado. */
    private void assertPathVariableName(String methodName, String expectedName) {
        Method method = Arrays.stream(CheckoutController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Parameter parameter = Arrays.stream(method.getParameters())
                .filter(candidate -> candidate.isAnnotationPresent(PathVariable.class))
                .findFirst()
                .orElseThrow();

        assertThat(parameter.getAnnotation(PathVariable.class).value()).isEqualTo(expectedName);
    }
}
