package com.marketinghub.worker.experimentpromise;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra o consumo das solicitações pendentes de promessa pelo AI Worker. */
@Service
public class ExperimentPromiseWorkerService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPromiseWorkerService.class);
    private static final String WORKER_ID = "ai-worker-experiment-promise";

    private final ExperimentPromiseBackendClient backendClient;
    private final ExperimentPromiseOpenAiClient openAiClient;

    /** Inicializa o serviço com o cliente do backend e o cliente da OpenAI. */
    public ExperimentPromiseWorkerService(ExperimentPromiseBackendClient backendClient,
                                          ExperimentPromiseOpenAiClient openAiClient) {
        this.backendClient = backendClient;
        this.openAiClient = openAiClient;
    }

    /** Processa as solicitações pendentes e sempre informa sucesso ou falha ao backend. */
    public void processPending() {
        for (ExperimentPromiseOptionsResponse pending : backendClient.listPending(5)) {
            Long requestId = pending.requestId();
            try {
                ExperimentPromiseOptionsResponse claimed = backendClient.claim(requestId, WORKER_ID);
                ExperimentPromiseOptionsResponse response = openAiClient.generate(claimed);
                backendClient.complete(requestId, response);
                log.info("Solicitação de promessa concluída; requestId={}", requestId);
            } catch (Exception ex) {
                log.error("Falha ao processar promessa; operation=experiment-promise requestId={}", requestId, ex);
                backendClient.fail(requestId, ex.getMessage());
            }
        }
    }
}
