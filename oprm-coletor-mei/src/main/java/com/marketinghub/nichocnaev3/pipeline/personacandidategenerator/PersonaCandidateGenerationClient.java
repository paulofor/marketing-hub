package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import java.util.Map;

/** Contrato da integração que gera personas candidatas para a etapa NichoCNAE v3. */
public interface PersonaCandidateGenerationClient {
    /** Gera o payload funcional de personas candidatas a partir do contexto da etapa. */
    Map<String, Object> generate(PersonaCandidateGenerationRequest request);
}
