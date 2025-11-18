package com.marketinghub.ads;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.liquibase.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
class FacebookInterestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookInterestRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldReturnOnlyPendingInterestsWithoutFacebookId() throws Exception {
        repository.save(FacebookInterest.builder().name("Yoga").status(FacebookInterestStatus.PENDING).build());
        repository.save(
            FacebookInterest
                .builder()
                .name("Pilates")
                .facebookInterestId("6003139266461")
                .status(FacebookInterestStatus.VALID)
                .build()
        );
        repository.save(FacebookInterest.builder().name("Surf").status(FacebookInterestStatus.INVALID).build());

        mockMvc
            .perform(get("/api/facebook-interests/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[*].name", contains("Yoga")));
    }

    @Test
    void shouldCreatePendingInterest() throws Exception {
        String payload = "{\"name\":\"Mountain biking\",\"model\":\"gpt-4.1\",\"prompt\":\"Generate interest\"}";

        mockMvc
            .perform(post("/api/facebook-interests").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Mountain biking"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.model").value("gpt-4.1"));
    }

    @Test
    void shouldUpdateInterestStatusAndGraphMetadata() throws Exception {
        FacebookInterest pending = repository
            .save(FacebookInterest.builder().name("Pilates").status(FacebookInterestStatus.PENDING).build());

        String payload = "{\"status\":\"VALID\",\"facebookInterestId\":\"6003139266461\",\"name\":\"Pilates Training\"}";

        mockMvc
            .perform(
                patch("/api/facebook-interests/{id}", pending.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.facebookInterestId").value("6003139266461"))
            .andExpect(jsonPath("$.name").value("Pilates Training"))
            .andExpect(jsonPath("$.status").value("VALID"));
    }

    @Test
    void shouldRejectUpdateWithoutStatus() throws Exception {
        FacebookInterest pending = repository
            .save(FacebookInterest.builder().name("Ciclismo").status(FacebookInterestStatus.PENDING).build());

        mockMvc
            .perform(
                patch("/api/facebook-interests/{id}", pending.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("status is required")));
    }
}
