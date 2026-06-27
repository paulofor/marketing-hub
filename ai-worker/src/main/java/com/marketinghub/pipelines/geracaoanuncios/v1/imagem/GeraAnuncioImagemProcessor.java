package com.marketinghub.pipelines.geracaoanuncios.v1.imagem;

import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageProcessor;
import com.marketinghub.worker.pipeline.StageResult;

/** Responsabilidade: executar a etapa Imagem do pipeline GeracaoAnuncios v1 a partir do contrato canônico do backend. */
public class GeraAnuncioImagemProcessor implements StageProcessor<GeraAnuncioImagemInput, GeraAnuncioImagemOutput> {
    private final GeraAnuncioImagemPromptBuilder promptBuilder;
    private final GeraAnuncioImagemResponseValidator responseValidator;
    private final GeraAnuncioImagemResponseHandler responseHandler;

    /** Inicializa o processor com montagem de prompt, validação de resposta e tratamento de saída da etapa. */
    public GeraAnuncioImagemProcessor(
            GeraAnuncioImagemPromptBuilder promptBuilder,
            GeraAnuncioImagemResponseValidator responseValidator,
            GeraAnuncioImagemResponseHandler responseHandler) {
        this.promptBuilder = promptBuilder;
        this.responseValidator = responseValidator;
        this.responseHandler = responseHandler;
    }

    /** Processa a execução pendente gerando payload auditável e saída funcional estruturada. */
    @Override
    public StageResult<GeraAnuncioImagemOutput> process(StageContext<GeraAnuncioImagemInput> context) {
        String requestPayload = promptBuilder.buildRequestPayload(context);
        GeraAnuncioImagemOutput output = responseValidator.validateAndParse(requestPayload);
        return responseHandler.handle(context, requestPayload, output);
    }
}
