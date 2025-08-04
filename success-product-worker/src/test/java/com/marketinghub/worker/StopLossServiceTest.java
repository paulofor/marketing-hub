package com.marketinghub.worker;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class StopLossServiceTest {
    @Test
    void shouldPauseWhenThresholdExceeded() {
        StopLossService svc = new StopLossService(new MetricPresetCache());
        boolean pause = svc.shouldPause(50.0, 51, 20.0, 5.0, 300);
        assertThat(pause).isTrue();
    }

    @Test
    void shouldNotPauseWhenBelowThreshold() {
        StopLossService svc = new StopLossService(new MetricPresetCache());
        boolean pause = svc.shouldPause(10.0, 10, 8.0, 5.0, 150);
        assertThat(pause).isFalse();
    }
}
