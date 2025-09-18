package com.marketinghub.journey.execution;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration knobs for journey orchestration runtime.
 */
@Component
@ConfigurationProperties(prefix = "journey.execution")
@Getter
@Setter
@ToString
public class JourneyExecutionProperties {
    @NotNull
    private Duration pollInterval = Duration.ofSeconds(30);

    @Min(1)
    @Max(500)
    private int batchSize = 50;

    @NotNull
    private FrequencyCapProperties frequencyCap = new FrequencyCapProperties();

    @NotNull
    private RetryProperties retry = new RetryProperties();

    @Getter
    @Setter
    @ToString
    public static class FrequencyCapProperties {
        private boolean enabled = true;
        @Min(1)
        private int perDay = 3;
        @Min(1)
        private int perWeek = 10;
        @NotNull
        private Duration cooldown = Duration.ofHours(6);
    }

    @Getter
    @Setter
    @ToString
    public static class RetryProperties {
        @Min(1)
        private int maxAttempts = 3;
        @NotNull
        private Duration initialBackoff = Duration.ofSeconds(30);
        @NotNull
        private Duration maxBackoff = Duration.ofMinutes(30);
        /**
         * Jitter factor applied to backoff (percentage in the range [0,100]).
         */
        @Min(0)
        @Max(100)
        private int jitterPercentage = 25;
    }
}
