# Marco de inicio real da versao MUSA PDE

## Inicio real

- Produto: Consultora MUSA / PDE Platform
- Ambiente: producao
- URL: `https://clubemusa.com.br`
- Inicio real considerado: `2026-07-22 18:35:34 UTC`
- Commit em producao: `f7095a41727e9965daa26865dc508943572b0f4a`
- Deploy tecnico confirmado em: `2026-07-22T18:19:55Z`
- Imagem/tag: `f7095a41727e9965daa26865dc508943572b0f4a`

## Criterio usado

Este horario marca o inicio real da versao porque houve confirmacao manual de teste funcional pelo usuario em producao apos a correcao do fluxo de espera do plano da Consultora MUSA.

Para analise de marketing, metricas anteriores a `2026-07-22 18:35:34 UTC` devem ser tratadas como periodo contaminado por falha operacional do funil. A partir desse horario, os dados podem ser usados como base inicial da versao corrigida.

## Evidencias operacionais

- Backend de producao reportou `environment=production`.
- `commitSha` e `imageTag` em producao estavam em `f7095a41727e9965daa26865dc508943572b0f4a`.
- Servicos publicados na mesma tag:
  - `pde-platform-backend`
  - `pde-platform-frontend`
  - `pde-ai-worker`
- Schema de IA pronto:
  - `aiGuidanceTableExists=true`
  - `aiGuidanceAccessTokenLength=120`
  - `aiGuidanceAccessTokenReady=true`
- Confirmacao do usuario: teste real em producao funcionou apos o deploy da correcao do wait.

## Uso recomendado em relatorios

- Separar qualquer analise de conversao em dois blocos:
  - antes de `2026-07-22 18:35:34 UTC`: diagnostico operacional, nao performance comercial limpa;
  - depois de `2026-07-22 18:35:34 UTC`: primeira janela valida da versao corrigida.
- Medir principalmente:
  - visitantes na pagina;
  - envios de diagnostico;
  - diagnosticos concluidos;
  - tempo medio ate plano pronto;
  - cliques no proximo passo/oferta;
  - abandono durante o estado de espera;
  - conversao final em compra, quando conectada ao checkout.

## Observacao

No momento da validacao, o status de deploy ainda apontava um alerta operacional antigo de `HTTP_5XX` em `/api/actuator/health` visto em `2026-07-22T18:21:46Z`. Esse alerta deve ser monitorado, mas nao invalida o marco de inicio real enquanto o fluxo publico de diagnostico permanecer funcional.
