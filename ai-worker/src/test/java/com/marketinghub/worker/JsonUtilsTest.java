package com.marketinghub.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void parsesPlainJson() throws Exception {
        String raw = "[{\"title\":\"H1\"}]";
        List<Map<String, Object>> list = JsonUtils.parsePossiblyDoubleEncoded(raw,
                new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(1, list.size());
        assertEquals("H1", list.get(0).get("title"));
    }

    @Test
    void parsesJsonWithFences() throws Exception {
        String raw = "```json\n[{\"title\":\"H1\"}]\n```";
        List<Map<String, Object>> list = JsonUtils.parsePossiblyDoubleEncoded(raw,
                new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(1, list.size());
    }

    @Test
    void parsesDoubleEncodedJson() throws Exception {
        String raw = "[{\\\"title\\\":\\\"H1\\\",\\\"promise\\\":\\\"p1\\\",\\\"problem\\\":\\\"pr1\\\",\\\"persona\\\":\\\"pe1\\\",\\\"successRule\\\":\\\"sr1\\\",\\\"offerType\\\":\\\"LEAD\\\",\\\"kpiTargetCpl\\\":1},{\\\"promise\\\":\\\"p2\\\",\\\"problem\\\":\\\"pr2\\\",\\\"persona\\\":\\\"pe2\\\",\\\"successRule\\\":\\\"sr2\\\",\\\"offerType\\\":\\\"LEAD\\\",\\\"kpiTargetCpl\\\":1}]";
        List<Map<String, Object>> list = JsonUtils.parsePossiblyDoubleEncoded(raw,
                new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(2, list.size());
        assertEquals("H1", list.get(0).get("title"));
        assertEquals("p1", list.get(0).get("promise"));
    }

    @Test
    void logsPreviewOnParseFailure() {
        String invalid = "[" + "x".repeat(250);
        Logger logger = (Logger) LoggerFactory.getLogger(JsonUtils.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        assertThrows(JsonProcessingException.class, () ->
                JsonUtils.parsePossiblyDoubleEncoded(invalid, new TypeReference<Map<String, Object>>() {}));
        assertFalse(appender.list.isEmpty());
        String msg = appender.list.get(0).getFormattedMessage();
        String expected = invalid.trim().substring(0, 200) + "...";
        assertTrue(msg.contains(expected));
    }
}
