package com.marketinghub.geralanding;

public class GeraLandingContractViolationException extends RuntimeException {
    public static final int HTTP_STATUS_CODE = 460;

    private final String operation;
    private final String endpoint;
    private final String expectedContract;
    private final String receivedPayload;
    private final String upstreamError;

    public GeraLandingContractViolationException(
            String operation,
            String endpoint,
            String expectedContract,
            String receivedPayload,
            String upstreamError,
            Throwable cause) {
        super("Falha de contrato no GeraLanding", cause);
        this.operation = operation;
        this.endpoint = endpoint;
        this.expectedContract = expectedContract;
        this.receivedPayload = receivedPayload;
        this.upstreamError = upstreamError;
    }

    public int getHttpStatusCode() {
        return HTTP_STATUS_CODE;
    }

    public String getOperation() {
        return operation;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getExpectedContract() {
        return expectedContract;
    }

    public String getReceivedPayload() {
        return receivedPayload;
    }

    public String getUpstreamError() {
        return upstreamError;
    }

    @Override
    public String toString() {
        return "GeraLandingContractViolationException{" +
                "httpStatusCode=" + HTTP_STATUS_CODE +
                ", operation='" + operation + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", expectedContract='" + expectedContract + '\'' +
                ", receivedPayload='" + receivedPayload + '\'' +
                ", upstreamError='" + upstreamError + '\'' +
                '}';
    }
}
