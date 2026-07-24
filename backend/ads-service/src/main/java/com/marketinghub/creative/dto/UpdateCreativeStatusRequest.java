package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeStatus;

/**
 * Corpo da requisição para alterar o status operacional e o motivo de reprovação do criativo.
 */
public record UpdateCreativeStatusRequest(CreativeStatus status, String rejectionReason) {
}
