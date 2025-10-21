package com.marketinghub.deliverable;

import com.marketinghub.FixtureUtils;
import com.marketinghub.deliverable.dto.CreateDeliverablePackageRequest;
import com.marketinghub.deliverable.dto.CreateDeliverableRequest;
import com.marketinghub.deliverable.service.DeliverablePackageService;
import com.marketinghub.deliverable.service.DeliverableService;
import com.marketinghub.niche.MarketNiche;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
@org.springframework.transaction.annotation.Transactional
class DeliverableServiceTest {
    @Autowired
    DeliverableService deliverableService;
    @Autowired
    DeliverablePackageService packageService;
    @Autowired
    FixtureUtils fixtures;

    @Test
    void createDeliverableRequiresPrompt() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        CreateDeliverableRequest request = new CreateDeliverableRequest();
        request.setMarketNicheId(niche.getId());
        request.setTitle("Landing page");

        assertThatThrownBy(() -> deliverableService.create(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createPackageWithDeliverablesStoresLinks() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        CreateDeliverableRequest request = new CreateDeliverableRequest();
        request.setMarketNicheId(niche.getId());
        request.setTitle("Sequência de emails");
        request.setPrompt("gerar sequencia");
        request.setModel("gpt-4.1");
        Deliverable deliverable = deliverableService.create(request);

        var experiment = fixtures.createAndSaveExperiment(niche);

        CreateDeliverablePackageRequest packageRequest = new CreateDeliverablePackageRequest();
        packageRequest.setExperimentId(experiment.getId());
        packageRequest.setName("Pacote inicial");
        packageRequest.setPrompt("organizar entregáveis");
        packageRequest.setDeliverableIds(List.of(deliverable.getId()));
        DeliverablePackage pack = packageService.create(packageRequest);

        assertThat(pack.getDeliverables()).extracting(Deliverable::getId)
                .containsExactly(deliverable.getId());
    }
}
