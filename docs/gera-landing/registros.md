# Registros — Gera Landing

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.

- 2026-05-01 23:33:32 (UTC-3): criado o pacote `com.marketinghub.geralanding` no backend (`ads-service`) para centralizar os componentes do módulo Gera Landing.
- 2026-05-02 00:00:00 (UTC-3): adicionados o card **Gera WireFrame** e o botão **Iniciar** na aba "Gera landing" da tela de experimento no frontend; o botão agora envia POST para `/api/experiments/{experimentId}/geralanding/wireframe/start` no backend (package `com.marketinghub.geralanding`) com retorno `202 Accepted` sem processamento adicional neste momento.

- 2026-05-03 09:00:00 (UTC-3): no `ai-worker`, refatorado `GeraLandingService` para expor métodos tipados de leitura de `campaignAngle`, `adCopy`, `adImageBriefing` e `experimentMetadata` com DTOs existentes; criado `LandingPageWireframeDto` no pacote `geralanding` para encapsular `landingPageWireframe`.
