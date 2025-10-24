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
}
