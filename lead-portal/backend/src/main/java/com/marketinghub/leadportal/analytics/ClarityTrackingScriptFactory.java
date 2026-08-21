package com.marketinghub.leadportal.analytics;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: injetar o coletor Clarity em modo agregado, mascarado e sem armazenamento.
 */
@Component
public class ClarityTrackingScriptFactory {
    private static final Pattern SAFE_PROJECT_ID = Pattern.compile("[A-Za-z0-9_-]{3,64}");
    private final String projectId;

    /**
     * Configura o ID público do projeto Clarity sem receber token de exportação.
     */
    public ClarityTrackingScriptFactory(
            @Value("${lead-portal.clarity.project-id:}") String projectId) {
        String normalized = projectId == null ? "" : projectId.trim();
        this.projectId = SAFE_PROJECT_ID.matcher(normalized).matches() ? normalized : "";
    }

    /**
     * Injeta o script no head quando o projeto está configurado e evita duplicação.
     */
    public String inject(String html) {
        if (html == null || projectId.isBlank()
                || html.toLowerCase(Locale.ROOT).contains("data-mh-clarity-analytics")) {
            return html;
        }
        String script = script();
        if (html.toLowerCase(Locale.ROOT).contains("</head>")) {
            return html.replaceFirst(
                    "(?i)</head>", Matcher.quoteReplacement(script + "\n</head>"));
        }
        if (html.toLowerCase(Locale.ROOT).contains("</body>")) {
            return html.replaceFirst(
                    "(?i)</body>", Matcher.quoteReplacement(script + "\n</body>"));
        }
        return html + "\n" + script;
    }

    /**
     * Monta o coletor sem identificadores customizados e bloqueia tráfego interno de teste.
     */
    private String script() {
        return """
                <script data-mh-clarity-analytics="aggregate-v1">
                (function(window, document){
                  var params = new URLSearchParams(window.location.search);
                  var persistedInternalTest = false;
                  try {
                    persistedInternalTest = sessionStorage.getItem('mh_internal_test') === 'true';
                  } catch (ignored) {
                    persistedInternalTest = false;
                  }
                  var internalTest = params.get('mh_test') === '1'
                    || params.has('mh_audit')
                    || persistedInternalTest;
                  if (internalTest) return;
                  var startMaskedCollection = function(){
                    document.querySelectorAll('form, input, textarea, select, [contenteditable="true"]').forEach(function(node){
                      node.setAttribute('data-clarity-mask', 'true');
                    });
                    window.clarity = window.clarity || function(){
                      (window.clarity.q = window.clarity.q || []).push(arguments);
                    };
                    window.clarity('consentv2', {
                      ad_Storage: 'denied',
                      analytics_Storage: 'denied'
                    });
                    var script = document.createElement('script');
                    script.async = true;
                    script.src = 'https://www.clarity.ms/tag/' + %s;
                    var firstScript = document.getElementsByTagName('script')[0];
                    firstScript.parentNode.insertBefore(script, firstScript);
                  };
                  if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', startMaskedCollection, {once: true});
                  } else {
                    startMaskedCollection();
                  }
                })(window, document);
                </script>
                """.formatted(jsLiteral(projectId));
    }

    /**
     * Escapa o identificador público para uso seguro em JavaScript.
     */
    private String jsLiteral(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
