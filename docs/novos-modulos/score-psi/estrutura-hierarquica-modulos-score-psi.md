# Estrutura Hierárquica de Módulos — Score PSI

```mermaid
graph TD
    A[MarketNiche] --> B[Hypothesis]

    B --> C[HypothesisSymbolicProfile]
    B --> D[CommunicationConcept A]
    B --> E[CommunicationConcept B]
    B --> F[CommunicationConcept C]

    D --> D1[Experiment A]
    E --> E1[Experiment B]
    F --> F1[Experiment C]

    D1 --> D2[Creative]
    D1 --> D3[LandingPage]
    D1 --> D4[LandingPageSectionMetrics]
    D1 --> D5[MarketPsychologicalScore]

    E1 --> E2[Creative]
    E1 --> E3[LandingPage]
    E1 --> E4[LandingPageSectionMetrics]
    E1 --> E5[MarketPsychologicalScore]

    F1 --> F2[Creative]
    F1 --> F3[LandingPage]
    F1 --> F4[LandingPageSectionMetrics]
    F1 --> F5[MarketPsychologicalScore]
```

## Leitura rápida

- `MarketNiche` é o contexto macro de mercado.
- Cada `Hypothesis` concentra o perfil simbólico e múltiplos conceitos de comunicação.
- Cada `CommunicationConcept` gera um experimento próprio para validação.
- Cada experimento é medido por criativo, landing, métricas por seção e score psicológico final.
