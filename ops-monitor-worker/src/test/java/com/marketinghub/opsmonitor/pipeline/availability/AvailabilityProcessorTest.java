package com.marketinghub.opsmonitor.pipeline.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.opsmonitor.pipeline.StageContext;
import org.junit.jupiter.api.Test;

class AvailabilityProcessorTest {

    @Test
    void deveMarcarModuloComoOfflineAposFalhasConsecutivas() {
        var output = new AvailabilityProcessor().process(StageContext.simple("stage-3", "facebook-ads-worker"), new AvailabilityInput("facebook-ads-worker", false, 3, 100, 3));

        assertThat(output.availabilityStatus()).isEqualTo("OFFLINE");
    }
}
