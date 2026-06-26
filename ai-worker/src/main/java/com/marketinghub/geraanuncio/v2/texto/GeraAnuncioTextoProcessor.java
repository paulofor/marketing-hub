package com.marketinghub.geraanuncio.v2.texto;

import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageProcessor;
import com.marketinghub.worker.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar a etapa Texto do pipeline GeraAnuncio v2 a partir do contrato canônico do backend. */
@Component
public class GeraAnuncioTextoProcessor implements StageProcessor<GeraAnuncioTextoInput, GeraAnuncioTextoOutput> {
    /** Processa o contexto pendente e devolve saída estruturada pronta para callback ao backend. */
    @Override
    public StageResult<GeraAnuncioTextoOutput> process(StageContext<GeraAnuncioTextoInput> context) {
        GeraAnuncioTextoOutput output = new GeraAnuncioTextoOutput(List.of(), Map.of("status", "TEXT_SCAFFOLD"));
        return new StageResult<>(output, List.of(), Map.of());
    }
}
