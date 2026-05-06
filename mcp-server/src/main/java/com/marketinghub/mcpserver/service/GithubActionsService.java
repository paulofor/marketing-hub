package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GithubActionsService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final McpProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GithubActionsService(McpProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> listWorkflows(Integer perPage) {
        ensureEnabled();
        int pageSize = normalizePerPage(perPage);

        URI uri = UriComponentsBuilder.fromHttpUrl(properties.github().apiBaseUrl())
                .pathSegment("repos", properties.github().owner(), properties.github().repo(), "actions", "workflows")
                .queryParam("per_page", pageSize)
                .build(true)
                .toUri();

        return executeGet(uri);
    }

    public Map<String, Object> listRuns(String branch, String status, Integer perPage) {
        ensureEnabled();
        int pageSize = normalizePerPage(perPage);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.github().apiBaseUrl())
                .pathSegment("repos", properties.github().owner(), properties.github().repo(), "actions", "runs")
                .queryParam("per_page", pageSize);

        if (StringUtils.hasText(branch)) {
            builder.queryParam("branch", branch.trim());
        }
        if (StringUtils.hasText(status)) {
            builder.queryParam("status", status.trim());
        }

        return executeGet(builder.build(true).toUri());
    }


    public Map<String, Object> getRunSummary(Long runId) {
        ensureEnabled();
        if (runId == null || runId <= 0) {
            throw new IllegalArgumentException("run_id must be a positive integer");
        }

        URI runUri = UriComponentsBuilder.fromHttpUrl(properties.github().apiBaseUrl())
                .pathSegment("repos", properties.github().owner(), properties.github().repo(), "actions", "runs",
                        String.valueOf(runId))
                .build(true)
                .toUri();

        Map<String, Object> runResponse = executeGet(runUri);
        Map<String, Object> runPayload = runResponse.containsKey("payload")
                ? (Map<String, Object>) runResponse.get("payload")
                : Map.of();

        URI jobsUri = UriComponentsBuilder.fromHttpUrl(properties.github().apiBaseUrl())
                .pathSegment("repos", properties.github().owner(), properties.github().repo(), "actions", "runs",
                        String.valueOf(runId), "jobs")
                .queryParam("per_page", 100)
                .build(true)
                .toUri();

        Map<String, Object> jobsResponse = executeGet(jobsUri);
        Map<String, Object> jobsPayload = jobsResponse.containsKey("payload")
                ? (Map<String, Object>) jobsResponse.get("payload")
                : Map.of();

        return buildSummary(runId, runPayload, jobsPayload, runUri.toString(), jobsUri.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSummary(Long runId,
                                             Map<String, Object> runPayload,
                                             Map<String, Object> jobsPayload,
                                             String runApiUrl,
                                             String jobsApiUrl) {
        List<Map<String, Object>> failedJobs = new ArrayList<>();
        Object jobsObject = jobsPayload.get("jobs");
        if (jobsObject instanceof List<?> jobs) {
            for (Object item : jobs) {
                if (!(item instanceof Map<?, ?> rawJob)) {
                    continue;
                }
                Map<String, Object> job = (Map<String, Object>) rawJob;
                String conclusion = stringValue(job.get("conclusion"));
                if (!"failure".equalsIgnoreCase(conclusion)) {
                    continue;
                }
                List<String> failedSteps = new ArrayList<>();
                Object stepsObject = job.get("steps");
                if (stepsObject instanceof List<?> steps) {
                    for (Object stepItem : steps) {
                        if (!(stepItem instanceof Map<?, ?> rawStep)) {
                            continue;
                        }
                        Map<String, Object> step = (Map<String, Object>) rawStep;
                        if ("failure".equalsIgnoreCase(stringValue(step.get("conclusion")))) {
                            failedSteps.add(stringValue(step.get("name")));
                        }
                    }
                }
                failedJobs.add(Map.of(
                        "name", stringValue(job.get("name")),
                        "conclusion", conclusion,
                        "status", stringValue(job.get("status")),
                        "html_url", stringValue(job.get("html_url")),
                        "failed_steps", failedSteps
                ));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("run_id", runId);
        result.put("workflow_name", stringValue(runPayload.get("name")));
        result.put("display_title", stringValue(runPayload.get("display_title")));
        result.put("status", stringValue(runPayload.get("status")));
        result.put("conclusion", stringValue(runPayload.get("conclusion")));
        result.put("run_attempt", runPayload.get("run_attempt"));
        result.put("event", stringValue(runPayload.get("event")));
        result.put("branch", stringValue(runPayload.get("head_branch")));
        result.put("html_url", stringValue(runPayload.get("html_url")));
        result.put("run_api_url", runApiUrl);
        result.put("jobs_api_url", jobsApiUrl);
        result.put("failed", "failure".equalsIgnoreCase(stringValue(runPayload.get("conclusion"))));
        result.put("failed_jobs", failedJobs);
        result.put("has_job_errors", !failedJobs.isEmpty());
        return result;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> executeGet(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(requiredText(properties.github().token(), "mcp.github.token"));
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Object.class);

        Map<String, Object> payload = objectMapper.convertValue(response.getBody(), MAP_TYPE);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apiUrl", uri.toString());
        result.put("statusCode", response.getStatusCode().value());
        result.put("payload", payload);
        return result;
    }

    private int normalizePerPage(Integer perPage) {
        if (perPage == null) {
            return 20;
        }
        if (perPage < 1 || perPage > 100) {
            throw new IllegalArgumentException("per_page must be between 1 and 100");
        }
        return perPage;
    }

    private void ensureEnabled() {
        if (!properties.github().enabled()) {
            throw new IllegalArgumentException("github tools are disabled (set mcp.github.enabled=true)");
        }
    }

    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
