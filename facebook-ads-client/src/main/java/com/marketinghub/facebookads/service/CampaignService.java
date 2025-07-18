package com.marketinghub.facebookads.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookads.dto.ExperimentDTO;
import java.io.IOException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service that wraps Facebook Graph batch creation APIs. */
@Service
public class CampaignService {
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String token;
    private final String version;
    private final String baseUrl;
    private final String accountId;
    private final String fbEnv;

    public CampaignService(
            @Value("${facebook.token:}") String token,
            @Value("${facebook.version:v19.0}") String version,
            @Value("${facebook.url:https://graph.facebook.com}") String baseUrl,
            @Value("${facebook.account:}") String accountId,
            @Value("${facebook.env:PROD}") String fbEnv) {
        this.token = token;
        this.version = version;
        this.baseUrl = baseUrl;
        this.accountId = accountId;
        this.fbEnv = fbEnv;
        this.client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .build();
    }

    /**
     * Creates a campaign with ad sets and ads using Facebook batch requests.
     */
    public JsonNode createExperimentCampaign(ExperimentDTO experiment) {
        String name = experiment.getName();
        if ("SANDBOX".equalsIgnoreCase(fbEnv)) {
            name = "[SANDBOX] " + name;
        }
        ArrayNode batch = mapper.createArrayNode();
        ObjectNode campaignCall = mapper.createObjectNode();
        campaignCall.put("method", "POST");
        campaignCall.put("relative_url", "act_" + accountId + "/campaigns");
        ObjectNode body = mapper.createObjectNode();
        body.put("name", name);
        if (Boolean.TRUE.equals(experiment.getSplitTest())) {
            body.put("split_test_configs", "{}");
        }
        campaignCall.put("body", encode(body));
        batch.add(campaignCall);
        try {
            RequestBody reqBody = new FormBody.Builder()
                    .add("access_token", token)
                    .add("batch", mapper.writeValueAsString(batch))
                    .build();
            Request request = new Request.Builder()
                    .url(baseUrl + "/" + version + "/")
                    .post(reqBody)
                    .build();
            try (Response resp = client.newCall(request).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "{}";
                return mapper.readTree(respBody);
            }
        } catch (IOException e) {
            log.error("Batch creation failed", e);
            return mapper.createObjectNode();
        }
    }

    private String encode(ObjectNode body) {
        StringBuilder sb = new StringBuilder();
        body.fields().forEachRemaining(f -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(f.getKey()).append('=').append(f.getValue().asText());
        });
        return sb.toString();
    }
}
