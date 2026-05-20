package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DesignPresetProvisionalHtmlAssembler {

    private final DesignPresetProvisionalHtmlProcessor processor;
    private final ObjectMapper objectMapper;

    public DesignPresetProvisionalHtmlAssembler(DesignPresetProvisionalHtmlProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public String assemble(String wireframeJson,
                           String copyJson,
                           String imagePlanningJson,
                           String designPresetOutputJson,
                           String jobId) {
        if (!StringUtils.hasText(wireframeJson)
                || !StringUtils.hasText(copyJson)
                || !StringUtils.hasText(designPresetOutputJson)) {
            return null;
        }

        try {
            Map<String, Object> wireframePayload = normalizePayload(wireframeJson, "landingPageWireframe");
            Map<String, Object> copyPayload = normalizePayload(copyJson, "landingPageCopy");
            String html = processor.process(
                    objectMapper.writeValueAsString(wireframePayload),
                    objectMapper.writeValueAsString(copyPayload),
                    imagePlanningJson,
                    designPresetOutputJson);
            html = appendBehaviorTrackingAttributesAndScript(html);
            return appendJobIdCommentBeforeHead(html, jobId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao montar HTML provisório da fase landing-page-design-preset", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizePayload(String sourceJson, String preferredRoot) throws Exception {
        Map<String, Object> root = objectMapper.readValue(sourceJson, Map.class);
        if (root.get(preferredRoot) instanceof Map<?, ?> nested) {
            return (Map<String, Object>) nested;
        }
        return new LinkedHashMap<>(root);
    }

    private String appendJobIdCommentBeforeHead(String html, String jobId) {
        if (!StringUtils.hasText(html) || !StringUtils.hasText(jobId)) {
            return html;
        }
        String comment = "<!-- jobId = " + jobId + " -->\n";
        int headIndex = html.toLowerCase().indexOf("<head>");
        if (headIndex < 0) {
            return comment + html;
        }
        return html.substring(0, headIndex) + comment + html.substring(headIndex);
    }

    private String appendBehaviorTrackingAttributesAndScript(String html) {
        if (!StringUtils.hasText(html) || html.contains("data-mh-funnel-tracking")) {
            return html;
        }
        Document document = Jsoup.parse(html, "", Parser.htmlParser());
        document.outputSettings().prettyPrint(false);

        for (Element section : document.select("section[data-section-id], section[id], [data-section-id]")) {
            String sectionId = section.hasAttr("data-section-id") ? section.attr("data-section-id") : section.id();
            if (!StringUtils.hasText(sectionId)) {
                continue;
            }
            section.attr("data-track-section", sectionId.trim());
        }

        String script = """
                <script data-mh-funnel-tracking="true">
                (function(){
                  if (window.__mhFunnelTrackingInstalled) return;
                  window.__mhFunnelTrackingInstalled = true;
                  window.dataLayer = window.dataLayer || [];
                  function emit(name, payload){
                    window.dataLayer.push(Object.assign({event:name, source:'landing-page-design-preset'}, payload||{}));
                  }
                  emit('page_view', {ts: Date.now()});
                  var sections = Array.prototype.slice.call(document.querySelectorAll('[data-track-section]'));
                  var stats = {};
                  sections.forEach(function(node){
                    var id = node.getAttribute('data-track-section');
                    stats[id] = {visibleSince:null, elapsedMs:0};
                  });
                  function flushSection(id, reason){
                    var s = stats[id];
                    if (!s || s.visibleSince === null) return;
                    s.elapsedMs += Date.now() - s.visibleSince;
                    s.visibleSince = null;
                    emit('section_view_time', {sectionId:id, elapsedMs:s.elapsedMs, reason: reason || 'hidden'});
                  }
                  var observer = new IntersectionObserver(function(entries){
                    entries.forEach(function(entry){
                      var id = entry.target.getAttribute('data-track-section');
                      if (!id || !stats[id]) return;
                      if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
                        if (stats[id].visibleSince === null) {
                          stats[id].visibleSince = Date.now();
                          emit('section_view_start', {sectionId:id});
                        }
                      } else {
                        flushSection(id, 'intersection-change');
                      }
                    });
                  }, {threshold:[0,0.5,1]});
                  sections.forEach(function(node){ observer.observe(node); });
                  document.addEventListener('visibilitychange', function(){
                    if (document.hidden) Object.keys(stats).forEach(function(id){ flushSection(id, 'tab-hidden'); });
                  });
                  window.addEventListener('beforeunload', function(){
                    Object.keys(stats).forEach(function(id){ flushSection(id, 'before-unload'); });
                  });
                })();
                </script>
                """;
        if (document.head() != null) {
            document.head().append(script);
        } else {
            document.prepend(script);
        }
        return document.outerHtml();
    }
}
