Gerar exclusivamente o artefato canônico `landingPageDesignPreset`, seguindo o schema JSON da etapa.

Regras mandatórias da Sprint 1:
- Defina primitives mínimas em `componentPresets.primitives`: `hero-title`, `section-title`, `body`, `btn-primary`, `btn-secondary`, `field`, `card`, `faq-item`.
- Defina `componentPresets.registry` com mapeamento explícito `componentKey -> templatePartial` para: `hero-form-split`, `proof`, `offer-cards`, `faq`.
- Não usar fallback silencioso: qualquer decisão de fallback deve ser descrita em `consistencyChecks[*].details`.
- Preencher tokens obrigatórios em `theme`: tipografia, spacing, radius, shadow, focus-ring e cores de superfície/contraste.
- Garantir legibilidade e toque mínimo de CTA/form com foco visual perceptível.

Saída:
- Apenas JSON válido aderente ao schema da etapa.
