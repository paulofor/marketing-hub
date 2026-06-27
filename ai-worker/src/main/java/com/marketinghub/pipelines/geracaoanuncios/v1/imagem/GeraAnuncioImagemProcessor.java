package com.marketinghub.pipelines.geracaoanuncios.v1.imagem;

import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageProcessor;
import com.marketinghub.worker.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar a etapa Imagem do pipeline GeracaoAnuncios v1 a partir do contrato canônico do backend. */
@Component
public class GeraAnuncioImagemProcessor implements StageProcessor<GeraAnuncioImagemInput, GeraAnuncioImagemOutput> {
    /** Processa o contexto pendente e devolve saída estruturada pronta para callback ao backend. */
    @Override
    public StageResult<GeraAnuncioImagemOutput> process(StageContext<GeraAnuncioImagemInput> context) {
        GeraAnuncioImagemOutput output = new GeraAnuncioImagemOutput(List.of(), Map.of("status", "IMAGE_SCAFFOLD"));
        return new StageResult<>(output, List.of(), Map.of());
    }
}
