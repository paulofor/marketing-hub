package com.marketinghub.ads;

import java.util.Arrays;

public enum FacebookWorkerValidationError {
    ACCESS_TOKEN_MISSING(
        "ACCESS_TOKEN_MISSING",
        "Facebook worker account is missing access token",
        "Informe um token de acesso válido na conta selecionada para o worker."
    ),
    AD_ACCOUNT_ID_MISSING(
        "AD_ACCOUNT_ID_MISSING",
        "Facebook worker account is missing ad account id",
        "Preencha o ID da conta de anúncios (act_...) na conta ativa do worker."
    ),
    DEFAULT_WEBSITE_URL_MISSING(
        "DEFAULT_WEBSITE_URL_MISSING",
        "Facebook worker account is missing default website URL",
        "Informe a URL padrão do site para que o worker possa criar anúncios."
    ),
    AD_SET_DAILY_BUDGET_MISSING(
        "AD_SET_DAILY_BUDGET_MISSING",
        "Facebook worker account is missing ad set daily budget",
        "Defina o orçamento diário do conjunto de anúncios em centavos."
    ),
    AD_SET_BILLING_EVENT_MISSING(
        "AD_SET_BILLING_EVENT_MISSING",
        "Facebook worker account is missing ad set billing event",
        "Informe o evento de cobrança padrão (por exemplo, IMPRESSIONS)."
    ),
    AD_SET_OPTIMIZATION_GOAL_MISSING(
        "AD_SET_OPTIMIZATION_GOAL_MISSING",
        "Facebook worker account is missing ad set optimization goal",
        "Defina o objetivo de otimização do conjunto (por exemplo, LINK_CLICKS)."
    ),
    AD_SET_DESTINATION_TYPE_MISSING(
        "AD_SET_DESTINATION_TYPE_MISSING",
        "Facebook worker account is missing ad set destination type",
        "Informe o tipo de destino padrão (WEBSITE, APP, MESSENGER...)."
    ),
    TARGET_COUNTRY_MISSING(
        "TARGET_COUNTRY_MISSING",
        "Facebook worker account is missing target country",
        "Informe pelo menos um país de segmentação para o conjunto de anúncios."
    );

    private final String code;
    private final String apiMessage;
    private final String userMessage;

    FacebookWorkerValidationError(String code, String apiMessage, String userMessage) {
        this.code = code;
        this.apiMessage = apiMessage;
        this.userMessage = userMessage;
    }

    public String code() {
        return code;
    }

    public String apiMessage() {
        return apiMessage;
    }

    public String userMessage() {
        return userMessage;
    }

    public static FacebookWorkerValidationError fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays
            .stream(values())
            .filter(value -> value.code.equals(code))
            .findFirst()
            .orElse(null);
    }
}
