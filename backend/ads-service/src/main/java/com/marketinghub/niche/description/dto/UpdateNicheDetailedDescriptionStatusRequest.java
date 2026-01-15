package com.marketinghub.niche.description.dto;

import lombok.Data;

/**
 * Payload para atualizar o status ativo de uma descrição detalhada.
 */
@Data
public class UpdateNicheDetailedDescriptionStatusRequest {
    private boolean active;
}
