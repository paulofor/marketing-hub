package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageProcessor;
import com.marketinghub.worker.pipeline.StageResult;

/** Responsabilidade: executar a etapa Texto do pipeline GeracaoAnuncios v1 a partir do contrato canônico do backend. */
public class GeraAnuncioTextoProcessor implements StageProcessor<GeraAnuncioTextoInput, GeraAnuncioTextoOutput> {
    private final GeraAnuncioTextoPromptBuilder promptBuilder;
    private final GeraAnuncioTextoResponseValidator responseValidator;
    private final GeraAnuncioTextoResponseHandler responseHandler;

    /** Inicializa o processor com montagem de prompt, validação de resposta e tratamento de saída da etapa. */
    public GeraAnuncioTextoProcessor(
            GeraAnuncioTextoPromptBuilder promptBuilder,
            GeraAnuncioTextoResponseValidator responseValidator,
            GeraAnuncioTextoResponseHandler responseHandler) {
        this.promptBuilder = promptBuilder;
        this.responseValidator = responseValidator;
        this.responseHandler = responseHandler;
    }

    /** Processa a execução pendente gerando payload auditável e saída funcional estruturada. */
    @Override
    public StageResult<GeraAnuncioTextoOutput> process(StageContext<GeraAnuncioTextoInput> context) {
        String requestPayload = promptBuilder.buildRequestPayload(context);
        GeraAnuncioTextoOutput output = responseValidator.validateAndParse(requestPayload);
        return responseHandler.handle(context, requestPayload, output);
    }
}
