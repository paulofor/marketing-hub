package com.marketinghub.worker.geraanunciov2.pipeline.criativo;

import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageProcessor;
import com.marketinghub.worker.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar a etapa Criativo do pipeline GeraAnuncio v2 a partir do contrato canônico do backend. */
@Component
public class GeraAnuncioCriativoProcessor implements StageProcessor<GeraAnuncioCriativoInput, GeraAnuncioCriativoOutput> {
    /** Processa o contexto pendente e devolve saída estruturada pronta para callback ao backend. */
    @Override
    public StageResult<GeraAnuncioCriativoOutput> process(StageContext<GeraAnuncioCriativoInput> context) {
        GeraAnuncioCriativoOutput output = new GeraAnuncioCriativoOutput(List.of(), Map.of("status", "SCaffold"));
        return new StageResult<>(output, List.of(), Map.of());
    }
}
