package com.marketinghub.ads;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FacebookTokenRevalidationServiceTest {

    @Test
    void shouldGenerateNewTokenWithSixtyDayExpiration() {
        RestTemplate restTemplate = new RestTemplate();
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);

        FacebookAccountRepository repository = mock(FacebookAccountRepository.class);
        when(repository.save(any(FacebookAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FacebookTokenRevalidationService service = new FacebookTokenRevalidationService(
            builder,
            repository,
            new ObjectMapper(),
            "https://graph.facebook.com",
            "v18.0"
        );

        MockRestServiceServer server = MockRestServiceServer
            .bindTo(restTemplate)
            .ignoreExpectOrder(true)
            .build();

        server
            .expect(requestTo("https://graph.facebook.com/v18.0/oauth/access_token?grant_type=fb_exchange_token&client_id=app-id&client_secret=secret&fb_exchange_token=old-token"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"access_token\":\"new-token\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        FacebookAccount account = FacebookAccount
            .builder()
            .id(1L)
            .name("Test")
            .accessToken("old-token")
            .appId("app-id")
            .appSecret("secret")
            .tokenRenewalEnabled(true)
            .build();

        LocalDateTime before = LocalDateTime.now();
        FacebookTokenRevalidationService.RevalidationResult result = service.revalidate(account);
        LocalDateTime after = LocalDateTime.now();

        server.verify();

        assertThat(result.status()).isEqualTo(FacebookTokenRenewalStatus.SUCCESS);
        assertThat(result.accessToken()).isEqualTo("new-token");
        assertThat(result.tokenExpiresAt()).isNotNull();
        assertThat(result.tokenExpiresAt()).isBetween(before.plusDays(60), after.plusDays(60));
        assertThat(account.getTokenExpiresAt()).isBetween(before.plusDays(60), after.plusDays(60));
        assertThat(account.getTokenLastRefreshedAt()).isBetween(before, after);
        assertThat(account.getTokenRenewedAt()).isBetween(before, after);
    }
}
