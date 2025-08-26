package com.marketinghub.worker;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

  @Test
  void parseRawJson() throws Exception {
    String raw = "[{\"title\":\"H1\"}]";
    List<Map<String, Object>> list =
        JsonUtils.parsePossiblyDoubleEncoded(raw, new TypeReference<List<Map<String, Object>>>() {});
    assertEquals(1, list.size());
    assertEquals("H1", list.get(0).get("title"));
  }

  @Test
  void parseJsonWithFences() throws Exception {
    String raw = "```json\n[{\"title\":\"H1\"}]\n```";
    List<Map<String, Object>> list =
        JsonUtils.parsePossiblyDoubleEncoded(raw, new TypeReference<List<Map<String, Object>>>() {});
    assertEquals(1, list.size());
    assertEquals("H1", list.get(0).get("title"));
  }

  @Test
  void parseDoubleEncodedJson() throws Exception {
    String doubleEncoded =
        "\"[{\\\"title\\\":\\\"H1\\\",\\\"promise\\\":\\\"p1\\\",\\\"problem\\\":\\\"pr1\\\",\\\"persona\\\":\\\"pe1\\\",\\\"successRule\\\":\\\"sr1\\\",\\\"offerType\\\":\\\"LEAD\\\",\\\"kpiTargetCpl\\\":1},{\\\"promise\\\":\\\"p2\\\",\\\"problem\\\":\\\"pr2\\\",\\\"persona\\\":\\\"pe2\\\",\\\"successRule\\\":\\\"sr2\\\",\\\"offerType\\\":\\\"LEAD\\\",\\\"kpiTargetCpl\\\":1}]\"";
    List<Map<String, Object>> list =
        JsonUtils.parsePossiblyDoubleEncoded(doubleEncoded, new TypeReference<List<Map<String, Object>>>() {});
    assertEquals(2, list.size());
    assertEquals("H1", list.get(0).get("title"));
    assertEquals("p2", list.get(1).get("promise"));
  }

  @Test
  void invalidJsonThrowsAndTruncates() {
    String invalid = "a".repeat(250);
    assertThrows(
        JsonProcessingException.class,
        () -> JsonUtils.parsePossiblyDoubleEncoded(invalid, new TypeReference<List<Map<String, Object>>>() {}));
    String preview = invalid.substring(0, Math.min(200, invalid.length()));
    assertEquals(200, preview.length());
  }
}
