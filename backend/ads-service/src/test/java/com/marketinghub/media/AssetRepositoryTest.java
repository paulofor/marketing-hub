package com.marketinghub.media;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.media.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import com.marketinghub.test.MySqlContainerBase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AdsServiceApplication.class)
class AssetRepositoryTest extends MySqlContainerBase {

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
}
