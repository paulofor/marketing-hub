package com.marketinghub.facebookadsworker.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlUtilsTest {

    @Test
    void joinsPathsAvoidingDuplicateSlashes() {
        assertEquals("http://host/api/creatives", UrlUtils.joinPath("http://host/", "/api/", "creatives"));
        assertEquals("http://host/api/creatives", UrlUtils.joinPath("http://host", "api", "/creatives"));
    }
}

