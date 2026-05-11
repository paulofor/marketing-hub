package com.marketinghub.geralanding;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

@Component
public class CopyProvisionalHtmlProcessor {

    public String process(String html, Map<String, Object> copy) {
        Deque<String> copyTexts = collectCopyTexts(copy);
        String output = applyCopyToAllTextNodes(html, copyTexts);
        if (StringUtils.hasText(copyTexts.peekFirst())) {
            output = output.replaceFirst("(?is)<title>.*?</title>", "<title>" + escapeHtml(copyTexts.peekFirst()) + "</title>");
        }
        return output;
    }

    private String applyCopyToAllTextNodes(String html, Deque<String> copyTexts) {
        if (!StringUtils.hasText(html)) {
            throw new IllegalArgumentException("HTML provisório ausente para aplicar a copy");
        }
        if (copyTexts.isEmpty()) {
            throw new IllegalArgumentException("Copy da etapa Gera Copy sem textos para preencher a página");
        }

        Document document = Jsoup.parse(html, "", Parser.htmlParser());
        List<Element> textElements = document.select("h1, h2, h3, p, li, span, summary, a, button");
        for (Element element : textElements) {
            String text = copyTexts.pollFirst();
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("Copy insuficiente para preencher todos os campos de texto do HTML");
            }
            element.text(text);
        }
        return document.outerHtml();
    }

    private Deque<String> collectCopyTexts(Map<String, Object> copy) {
        List<String> values = new ArrayList<>();
        collectTextsRecursive(copy, null, values);
        Deque<String> queue = new ArrayDeque<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                queue.addLast(value.trim());
            }
        }
        return queue;
    }

    @SuppressWarnings("unchecked")
    private void collectTextsRecursive(Object node, String key, List<String> out) {
        if (node == null) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                collectTextsRecursive(entry.getValue(), String.valueOf(entry.getKey()), out);
            }
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                collectTextsRecursive(item, key, out);
            }
            return;
        }
        if (node instanceof String text && shouldUseAsCopyText(key, text)) {
            out.add(text);
        }
    }

    private boolean shouldUseAsCopyText(String key, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        if (key == null) {
            return true;
        }
        String normalized = key.toLowerCase();
        if (normalized.contains("id")
                || normalized.contains("slot")
                || normalized.contains("section")
                || normalized.contains("url")
                || normalized.contains("placement")
                || normalized.contains("code")
                || normalized.contains("status")) {
            return false;
        }
        return true;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
