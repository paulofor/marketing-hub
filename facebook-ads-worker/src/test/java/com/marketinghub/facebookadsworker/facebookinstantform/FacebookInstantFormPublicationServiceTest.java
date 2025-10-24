package com.marketinghub.facebookadsworker.facebookinstantform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class FacebookInstantFormPublicationServiceTest {

    private FacebookInstantFormPublicationService service;
    private Class<?> questionClass;
    private Class<?> questionOptionClass;

    @BeforeEach
    void setUp() throws Exception {
        service = new FacebookInstantFormPublicationService(
            mock(FacebookAdsService.class),
            mock(FacebookAccessTokenManager.class),
            WebClient.builder(),
            mock(FacebookWorkerConfigurationClient.class),
            mock(MeterRegistry.class),
            "http://localhost:8000",
            "/api",
            false,
            new ObjectMapper()
        );
        questionClass = Class.forName(
            "com.marketinghub.facebookadsworker.facebookinstantform.FacebookInstantFormPublicationService$Question"
        );
        questionOptionClass = Class.forName(
            "com.marketinghub.facebookadsworker.facebookinstantform.FacebookInstantFormPublicationService$QuestionOption"
        );
    }

    @Test
    void mapQuestionSkipsLegalType() throws Exception {
        Object legalQuestion = newQuestion("LEGAL", null, null);
        Object result = ReflectionTestUtils.invokeMethod(service, "mapQuestion", legalQuestion);
        assertNull(result);
    }

    @Test
    void mapQuestionKeepsSupportedTypes() throws Exception {
        Object question = newQuestion("PHONE", "phone", "WhatsApp");
        FacebookAdsService.InstantFormCreationRequest.Question mapped = ReflectionTestUtils.invokeMethod(
            service,
            "mapQuestion",
            question
        );
        assertNotNull(mapped);
    }

    @Test
    void mapQuestionDropsInvalidOptionValues() throws Exception {
        Object option = newQuestionOption("Encher horários", "Encher horários");
        Object question = newQuestion(
            "CUSTOM",
            "objetivo",
            "Objetivo",
            java.util.List.of(option)
        );
        FacebookAdsService.InstantFormCreationRequest.Question mapped = ReflectionTestUtils.invokeMethod(
            service,
            "mapQuestion",
            question
        );
        assertNotNull(mapped);
        java.util.List<java.util.Map<String, Object>> options = mapped.options();
        assertNotNull(options);
        org.junit.jupiter.api.Assertions.assertEquals(1, options.size());
        java.util.Map<String, Object> mappedOption = options.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("Encher horários", mappedOption.get("label"));
        org.junit.jupiter.api.Assertions.assertFalse(mappedOption.containsKey("value"));
    }

    @Test
    void mapQuestionKeepsSafeOptionValues() throws Exception {
        Object option = newQuestionOption("Encher horários", "encher_horarios");
        Object question = newQuestion(
            "CUSTOM",
            "objetivo",
            "Objetivo",
            java.util.List.of(option)
        );
        FacebookAdsService.InstantFormCreationRequest.Question mapped = ReflectionTestUtils.invokeMethod(
            service,
            "mapQuestion",
            question
        );
        assertNotNull(mapped);
        java.util.List<java.util.Map<String, Object>> options = mapped.options();
        assertNotNull(options);
        org.junit.jupiter.api.Assertions.assertEquals(1, options.size());
        java.util.Map<String, Object> mappedOption = options.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("encher_horarios", mappedOption.get("value"));
    }

    private Object newQuestion(String type, String key, String label) throws Exception {
        Constructor<?> constructor = questionClass.getDeclaredConstructor(
            String.class,
            String.class,
            String.class,
            String.class,
            Boolean.class,
            Boolean.class,
            java.util.List.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(type, key, label, null, null, null, null);
    }

    private Object newQuestion(String type, String key, String label, java.util.List<?> options) throws Exception {
        Constructor<?> constructor = questionClass.getDeclaredConstructor(
            String.class,
            String.class,
            String.class,
            String.class,
            Boolean.class,
            Boolean.class,
            java.util.List.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(type, key, label, null, null, null, options);
    }

    private Object newQuestionOption(String label, String value) throws Exception {
        Constructor<?> constructor = questionOptionClass.getDeclaredConstructor(String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(label, value);
    }
}
