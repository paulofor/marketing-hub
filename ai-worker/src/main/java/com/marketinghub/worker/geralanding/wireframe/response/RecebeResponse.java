package com.marketinghub.worker.geralanding.wireframe.response;

import com.marketinghub.worker.geralanding.wireframe.backend.GeraLandingWireframeBackendClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: receber a resposta crua da OpenAI de uma etapa do GeraLanding,
 * montar o payload de callback e enviar os dados ao backend.
 */
@Component("geraLandingWireframeRecebeResponse")
public class RecebeResponse {

    private static final Logger log = LoggerFactory.getLogger(RecebeResponse.class);

    private final GeraLandingWireframeBackendClient backendClient;

    public RecebeResponse(GeraLandingWireframeBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /**
     * Envia para o backend os dados de despacho e de resultado da etapa, incluindo a resposta crua da OpenAI.
     */
    public void processar(Long experimentId, String stageCode, String idJob, RecordWireframeResponse payload) {
        log.info("Enviando payload de resultado ao backend. stageCode={}, idJob={}, experimentId={}, hasResponseContent={}, hasRawResponse={}",
                stageCode,
                idJob,
                experimentId,
                payload != null && payload.responseContent() != null && !payload.responseContent().isBlank(),
                payload != null && payload.rawResponse() != null && !payload.rawResponse().isBlank());
        backendClient.receiveResult(idJob, experimentId, stageCode, payload);
    }
}
