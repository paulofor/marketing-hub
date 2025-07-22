package com.marketinghub.experiment;

import com.marketinghub.experiment.service.AdGeneratorService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class AdGeneratorServiceTest {
    @Test
    void generateCreatesTextsWithinLimits() {
        AdGeneratorService svc = new AdGeneratorService();
        var result = svc.generate("Perda de Peso", "Antes e Depois", "Urgência");
        assertThat(result.primaryText().length()).isLessThanOrEqualTo(125);
        assertThat(result.headline().length()).isLessThanOrEqualTo(40);
        assertThat(result.imageUrl()).isNotBlank();
    }
}
