package com.marketinghub.vitrines.app.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.vitrines.app.model.CheckoutRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContentControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldListContentsWithLockedFlag() throws Exception {
    mockMvc
        .perform(get("/vitrines/api/conteudos").contextPath("/vitrines").param("role", "LEAD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].accessType", is("FREE")))
        .andExpect(jsonPath("$[0].locked", is(false)))
        .andExpect(jsonPath("$[1].accessType", is("PREMIUM")))
        .andExpect(jsonPath("$[1].locked", is(true)));
  }

  @Test
  void shouldReturnContentDetailWhenRoleCanOpenPremium() throws Exception {
    mockMvc
        .perform(get("/vitrines/api/conteudos/2").contextPath("/vitrines").param("role", "CLIENTE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is("2")))
        .andExpect(jsonPath("$.locked", is(false)))
        .andExpect(jsonPath("$.signedUrl", containsString("signature")));
  }

  @Test
  void shouldReturnForbiddenWhenRoleCannotOpenPremium() throws Exception {
    mockMvc
        .perform(get("/vitrines/api/conteudos/2").contextPath("/vitrines").param("role", "LEAD"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldStartCheckoutForKnownPlan() throws Exception {
    CheckoutRequest request = new CheckoutRequest("lead@example.com", "plan_premium_ads");

    mockMvc
        .perform(
            post("/vitrines/api/checkout").contextPath("/vitrines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("PENDING")))
        .andExpect(jsonPath("$.paymentUrl", containsString("plan_premium_ads")))
        .andExpect(jsonPath("$.email", is("lead@example.com")));
  }

  @Test
  void shouldValidateCheckoutPayload() throws Exception {
    CheckoutRequest request = new CheckoutRequest("", "");

    mockMvc
        .perform(
            post("/vitrines/api/checkout").contextPath("/vitrines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
