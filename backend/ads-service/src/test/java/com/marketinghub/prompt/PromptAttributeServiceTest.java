package com.marketinghub.prompt;

import com.marketinghub.prompt.dto.CreatePromptAttributeRequest;
import com.marketinghub.prompt.dto.PromptAttributeDto;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
import com.marketinghub.prompt.repository.PromptEntityRepository;
import com.marketinghub.prompt.service.PromptAttributeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
@org.springframework.transaction.annotation.Transactional
class PromptAttributeServiceTest {
    @Autowired
    PromptAttributeService service;
    @Autowired
    PromptEntityRepository entityRepository;
    @Autowired
    PromptAttributeRepository attributeRepository;
    @Autowired
    PromptAttributeDescriptionRepository descriptionRepository;

    @Test
    void createNewAttribute() {
        PromptEntity entity = entityRepository.save(PromptEntity.builder().name("hypothesis").build());
        CreatePromptAttributeRequest req = new CreatePromptAttributeRequest();
        req.setName("entrega");
        req.setDescription("d");

        PromptAttributeDto dto = service.create(entity.getName(), req);

        assertThat(dto.getName()).isEqualTo("entrega");
        assertThat(dto.getDescription()).isEqualTo("d");
        PromptAttribute attr = attributeRepository
                .findByEntity_NameAndName(entity.getName(), "entrega")
                .orElse(null);
        assertThat(attr).isNotNull();
        assertThat(descriptionRepository.findByAttribute_IdAndActiveTrue(attr.getId()))
                .map(PromptAttributeDescription::getDescription)
                .contains("d");
    }
}

