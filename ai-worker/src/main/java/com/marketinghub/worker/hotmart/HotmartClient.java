package com.marketinghub.worker.hotmart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Client used to interact with the Hotmart API.
 */
@Component
public class HotmartClient {

    private final WebClient webClient;

    @Autowired
    public HotmartClient(
            @Value("${hotmart.base-url:https://api.hotmart.com}") String baseUrl,
            @Value("${hotmart.username:}") String username,
            @Value("${hotmart.password:}") String password,
            WebClient.Builder builder) {
        this(builder
                .baseUrl(baseUrl)
                .filter(ExchangeFilterFunctions.basicAuthentication(username, password))
                .build());
    }

    // package-private constructor used for tests
    HotmartClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetch products ordered by temperature in descending order.
     *
     * @param limit maximum number of products to retrieve
     * @return list of products from Hotmart
     */
    public List<HotmartProduct> fetchTopProducts(int limit) {
        HotmartResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/products")
                        .queryParam("sort", "temperature")
                        .queryParam("order", "desc")
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(HotmartResponse.class)
                .block();

        return response != null && response.getItems() != null ? response.getItems() : List.of();
    }
}
