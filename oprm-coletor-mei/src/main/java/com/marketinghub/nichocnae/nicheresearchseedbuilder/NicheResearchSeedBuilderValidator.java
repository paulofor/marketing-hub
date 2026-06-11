package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.List;
import org.springframework.stereotype.Component;

/** Valida somente a integridade mínima da etapa dois antes de enviar a saída do modelo ao backend. */
@Component
public class NicheResearchSeedBuilderValidator {

    /** Garante que a resposta do modelo pertence ao ciclo processado e possui estrutura persistível. */
    public void validate(NicheResearchSeedBuilderPending input, NicheResearchSeedBuilderOutput output) {
        if (output == null) {
            throw new IllegalArgumentException("Saída da etapa dois não pode ser nula.");
        }
        if (!input.researchCycleId().equals(output.researchCycleId())) {
            throw new IllegalArgumentException("researchCycleId da saída não corresponde ao ciclo processado.");
        }
        validateSeed(input, output.seed());
        validateQueries(output.seed(), output.queries());
    }

    /** Confere apenas os campos mínimos do seed necessários para persistir o contrato do backend. */
    private void validateSeed(NicheResearchSeedBuilderPending input, NicheResearchSeed seed) {
        if (seed == null) {
            throw new IllegalArgumentException("Seed da etapa dois não pode ser nulo.");
        }
        if (!input.researchCycleId().equals(seed.researchCycleId())) {
            throw new IllegalArgumentException("researchCycleId do seed não corresponde ao ciclo processado.");
        }
        requireText(seed.nicheName(), "nicheName");
        requireText(seed.businessType(), "businessType");
        requireText(seed.operationType(), "operationType");
        requireText(seed.customerType(), "customerType");
        requireText(seed.commercialObjects(), "commercialObjects");
        requireText(seed.initialAssumptions(), "initialAssumptions");
    }

    /** Confere apenas se as queries existem e possuem campos persistíveis, sem julgar conteúdo semântico. */
    private void validateQueries(NicheResearchSeed seed, List<ResearchQuery> queries) {
        if (queries == null || queries.isEmpty()) {
            throw new IllegalArgumentException("A etapa dois deve retornar pelo menos uma query persistível.");
        }
        for (ResearchQuery query : queries) {
            validateQuery(seed, query);
        }
    }

    /** Valida a associação da query ao ciclo e os textos mínimos exigidos pelo banco. */
    private void validateQuery(NicheResearchSeed seed, ResearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Query da etapa dois não pode ser nula.");
        }
        if (!seed.researchCycleId().equals(query.researchCycleId())) {
            throw new IllegalArgumentException("researchCycleId da query não corresponde ao seed.");
        }
        requireText(query.queryText(), "queryText");
        requireText(query.queryGoal(), "queryGoal");
    }

    /** Exige texto funcional preenchido para campos obrigatórios de persistência da etapa dois. */
    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório vazio na etapa dois: " + fieldName);
        }
    }
}
