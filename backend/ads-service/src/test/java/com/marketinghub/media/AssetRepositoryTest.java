package com.marketinghub.media;

import com.marketinghub.media.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class AssetRepositoryTest {

    @Autowired
    AssetRepository repository;

    @Test
    void testSaveAsset() {
        Asset asset = Asset.builder()
                .type(AssetType.AUDIO)
                .provider(MediaProvider.ELEVENLABS)
                .status(AssetStatus.PENDING)
                .build();
        repository.save(asset);
        assertThat(repository.findById(asset.getId())).isPresent();
    }

    @Test
    void shouldPersistLargePromptWithoutTruncation() {
        String prompt = "x".repeat(120_000);
        Asset asset = Asset.builder()
                .type(AssetType.IMAGE)
                .provider(MediaProvider.OPENAI)
                .status(AssetStatus.READY)
                .prompt(prompt)
                .payload("{}").build();

        Asset saved = repository.save(asset);

        assertThat(repository.findById(saved.getId()))
                .get()
                .extracting(Asset::getPrompt)
                .isEqualTo(prompt);
    }
}
