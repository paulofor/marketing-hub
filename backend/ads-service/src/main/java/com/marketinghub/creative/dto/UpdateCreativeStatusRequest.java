package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeStatus;

/**
 * Corpo da requisição para alterar apenas o status operacional do criativo.
 */
public record UpdateCreativeStatusRequest(CreativeStatus status) {
}
