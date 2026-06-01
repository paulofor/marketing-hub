package com.marketinghub.settings;

import com.marketinghub.repository.jpa.settings.GeneralSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-settings;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class GeneralSettingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    GeneralSettingRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(GeneralSetting.builder()
                .name(GeneralSettingKeys.PRIVACY_POLICY_URL)
                .value("https://privacy.example.com/policy")
                .build());
    }

    @Test
    void shouldReturnSettingByName() throws Exception {
        mockMvc.perform(get("/api/settings/" + GeneralSettingKeys.PRIVACY_POLICY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(GeneralSettingKeys.PRIVACY_POLICY_URL))
                .andExpect(jsonPath("$.value").value("https://privacy.example.com/policy"));
    }

    @Test
    void shouldUpsertSetting() throws Exception {
        mockMvc.perform(put("/api/settings/" + GeneralSettingKeys.PRIVACY_POLICY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"https://privacy.example.com/updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("https://privacy.example.com/updated"));

        assertThat(repository.findByName(GeneralSettingKeys.PRIVACY_POLICY_URL))
                .isPresent()
                .get()
                .satisfies(setting -> assertThat(setting.getValue()).isEqualTo("https://privacy.example.com/updated"));
    }
}
