package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture;

/** Representa falha de captura HTML preservando URLs de redirecionamento auditáveis. */
public class HtmlCaptureFailureException extends Exception {

    private final String redirectDestinationUrl;
    private final String redirectRootUrl;
    private final Integer httpStatus;

    /** Cria a falha com mensagem operacional, destino redirecionado, raiz de fallback e status HTTP efetivo. */
    public HtmlCaptureFailureException(String message, String redirectDestinationUrl, String redirectRootUrl, Integer httpStatus) {
        super(message);
        this.redirectDestinationUrl = redirectDestinationUrl;
        this.redirectRootUrl = redirectRootUrl;
        this.httpStatus = httpStatus;
    }

    /** Retorna a URL final observada após redirecionamentos da URL canônica. */
    public String redirectDestinationUrl() {
        return redirectDestinationUrl;
    }

    /** Retorna a raiz derivada da URL final para fallback. */
    public String redirectRootUrl() {
        return redirectRootUrl;
    }

    /** Retorna o status HTTP efetivo que causou a falha, quando disponível. */
    public Integer httpStatus() {
        return httpStatus;
    }
}
