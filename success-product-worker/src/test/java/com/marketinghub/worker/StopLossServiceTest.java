package com.marketinghub.worker;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class StopLossServiceTest {
    @Test
    void shouldPauseWhenThresholdExceeded() {
        StopLossService svc = new StopLossService();
        boolean pause = svc.shouldPause(50.0, 1500, 20.0, 5.0);
        assertThat(pause).isTrue();
    }

    @Test
    void shouldNotPauseWhenBelowThreshold() {
        StopLossService svc = new StopLossService();
        boolean pause = svc.shouldPause(10.0, 900, 8.0, 5.0);
        assertThat(pause).isFalse();
    }
}
