package com.marketinghub.mcpserver.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseHostValidatorTest {

    @Test
    void shouldDetectInvalidKinghostInterfaceHost() {
        String invalidUrl = "jdbc:mysql://interface.vps-kinghost.net:3306/marketinghubdb";

        assertTrue(DatabaseHostValidator.usesInvalidHost(invalidUrl));
    }

    @Test
    void shouldNotDetectValidHost() {
        String validUrl = "jdbc:mysql://d555d.vps-kinghost.net:3306/marketinghubdb";

        assertFalse(DatabaseHostValidator.usesInvalidHost(validUrl));
    }
}
