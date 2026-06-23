package com.marketinghub.opsmonitor.pipeline.logscan;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.opsmonitor.pipeline.StageContext;
import org.junit.jupiter.api.Test;

class LogScanProcessorTest {

    @Test
    void deveIdentificarErroRelevanteNoPayloadDeLog() {
        var output = new LogScanProcessor().process(StageContext.simple("stage-4", "backend"), new LogScanInput("backend", "RuntimeException timeout ao chamar OpenAI"));

        assertThat(output.incidentSignalFound()).isTrue();
        assertThat(output.signals()).contains("exception", "timeout", "openai");
    }
}
