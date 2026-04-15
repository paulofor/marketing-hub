package com.marketinghub.oprm.infra.enrichment;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HttpWebPageFetcher implements WebPageFetcher {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public FetchedWebPage fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "marketing-hub-oprm/0.1")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body() == null ? "" : response.body();
            String title = extractTitle(body);
            String stripped = body.replaceAll("<script[^>]*>.*?</script>", " ")
                    .replaceAll("<style[^>]*>.*?</style>", " ")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            String language = guessLanguage(url);
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return new FetchedWebPage(url, response.statusCode(), title, stripped, language,
                    success ? "captured-from-web" : "http-status-" + response.statusCode(), success);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return new FetchedWebPage(url, 499, "", "", "unknown", "interrupted", false);
        } catch (IOException | IllegalArgumentException exception) {
            return new FetchedWebPage(url, 598, "", "", "unknown", "fetch-error: " + exception.getClass().getSimpleName(), false);
        }
    }

    private String extractTitle(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).replaceAll("\\s+", " ").trim();
    }

    private String guessLanguage(String url) {
        if (url.contains("/pt") || url.contains("pt.")) {
            return "pt-BR";
        }
        return "en";
    }
}
