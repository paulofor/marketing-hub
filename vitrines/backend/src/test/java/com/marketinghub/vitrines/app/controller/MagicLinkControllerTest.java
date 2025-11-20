package com.marketinghub.vitrines.app.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.vitrines.app.config.MagicLinkProperties;
import com.marketinghub.vitrines.app.model.MagicLinkRequest;
import com.marketinghub.vitrines.app.model.MagicLinkResponse;
import com.marketinghub.vitrines.app.service.TestDataRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MagicLinkControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private MagicLinkProperties properties;

  @Autowired private TestDataRepository testDataRepository;

  @Test
  void shouldGenerateMagicLinkForDomainAndPersistUser() throws Exception {
    MagicLinkRequest request = new MagicLinkRequest("cliente@teste.com", "cliente", "plan_consulting", false);

    String body =
        mockMvc
            .perform(
                post("/vitrines/api/auth/magic-link")
                    .contextPath("/vitrines")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.link", startsWith(properties.domain())))
            .andExpect(jsonPath("$.role", is("CLIENTE")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    MagicLinkResponse response = objectMapper.readValue(body, MagicLinkResponse.class);
    Claims claims = parseClaims(response.token());

    assertEquals("cliente@teste.com", claims.get("email"));
    assertEquals("plan_consulting", claims.get("planId"));
    assertEquals(properties.domain(), claims.get("domain"));
    assertFalse((Boolean) claims.get("first_access"));
    assertTrue(
        testDataRepository.findAll().stream().anyMatch(user -> user.email().equals("cliente@teste.com")));
  }

  @Test
  void shouldFallbackToDefaultValuesWhenRequestIsEmpty() throws Exception {
    String body =
        mockMvc
            .perform(post("/vitrines/api/auth/magic-link").contextPath("/vitrines"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("cliente@vitrineproduto.shop")))
            .andExpect(jsonPath("$.planId", is("plan_premium_ads")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    MagicLinkResponse response = objectMapper.readValue(body, MagicLinkResponse.class);
    Claims claims = parseClaims(response.token());

    assertEquals("vitrines-backend", claims.getIssuer());
    assertEquals("plan_premium_ads", claims.get("planId"));
    assertEquals(properties.domain(), claims.get("domain"));
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(properties.signingKey()).build().parseClaimsJws(token).getBody();
  }
}
