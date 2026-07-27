package com.marketinghub.fashionchat.service.login;

import com.fasterxml.jackson.databind.JsonNode;

/** Representa a resposta de início do login ChatGPT no serviço Chat Moda. */
public record StartFashionChatLoginResponse(
    String serviceBaseUrl,
    Integer httpStatus,
    String verificationUri,
    String userCode,
    Integer expiresIn,
    Integer interval,
    JsonNode payload,
    String errorMessage) {}
