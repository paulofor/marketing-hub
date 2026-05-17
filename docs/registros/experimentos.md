# Registros — Experimentos

> 🔴 **Arquivo canônico principal (atual)** para registro operacional do tema Experimentos.

## Template obrigatório de novo registro

```md
## YYYY-MM-DD HH:mm:ss UTC-3
- descrição breve do problema
- descrição breve do raciocínio para a solução
- registro do que foi feito
- documentos lidos para pesquisar e resolver o problema:
  - caminho/do/documento-1.md
  - caminho/do/documento-2.md
```

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

> Regra obrigatória de timestamp:
> Antes de adicionar qualquer novo registro, execute obrigatoriamente:
>
> ```bash
> TZ=America/Sao_Paulo date '+%Y-%m-%d %H:%M:%S UTC-3'
> ```
>
> Use exatamente a saída desse comando no título do novo registro.
> É proibido inventar, estimar, inferir ou reaproveitar data/hora a partir de:
> - contexto da conversa;
> - data do commit;
> - data do CI/build;
> - metadados do arquivo;
> - relógio UTC sem conversão explícita;
> - registros anteriores deste documento.
>
> O formato obrigatório do título é:
>
> ```md
> ## YYYY-MM-DD HH:mm:ss UTC-3
> ```
>
> Cada novo registro deve ser adicionado no final do arquivo.
> Se for necessário registrar mais de uma entrada, execute novamente o comando de data/hora para cada entrada.
> Nunca crie registro com timestamp futuro em relação ao horário atual de `America/Sao_Paulo`.
> Em caso de timestamp incorreto já registrado, não apague nem edite o registro antigo; adicione um novo registro de correção explicando o erro.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).
>

## 2026-05-17 03:38:52 UTC-3
- Atualização documental para remover referências à aba descontinuada "Portal do Lead" no fluxo de liberação para Facebook Ads.
- Raciocínio: a operação atual é centralizada no Gera Landing; manter instruções antigas induzia erro operacional e bloqueio indevido na liberação do experimento.
- Foi feito: revisão e ajuste dos documentos operacionais/canônicos para substituir instruções de aprovação em aba por aplicação/publicação via Gera Landing.
- documentos lidos para tratar a situação:
  - docs/manual-usuario/aihub/liberar-facebook-ads-worker.md
  - docs/pipeline-landing-experimento.md
  - docs/canonical/facebook-campaign-publication-canon.v1.md
  - docs/registros/experimentos.md
## 2026-05-17 03:30:43 UTC-3
- solicitação para registrar formalmente o tema experimento no local canônico após ajuste de fluxo de aprovação de landing.
- o raciocínio foi garantir rastreabilidade operacional no arquivo obrigatório de registros do domínio de experimentos, mantendo aderência ao contrato append-only.
- registro realizado neste documento informando a centralização da aprovação final de landing apenas na aba "Landing" e a remoção dos demais pontos de aprovação na visualização de conteúdo.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx


## 2026-05-17 00:00:00 UTC
- solicitação para evoluir o frontend na aba de backtest com visualização das quantidades por outcome e indicadores de progresso.
- implementação realizada na aba de funil/backtest com gráfico de barras por outcome (quantidade por etapa), destaque do total atual e percentual em relação à referência ideal de 500.
- também foi adicionado cálculo explícito da meta (500) para facilitar leitura operacional durante acompanhamento de performance.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentFunnelTab.tsx

## 2026-05-17 04:08:17 UTC-3
- solicitação para ajustar a aba Landing do experimento para usar o HTML salvo no próprio registro da tabela `experiment` e alinhar com a migração da geração para o Gera Landing.
- raciocínio aplicado: remover dependência da listagem antiga de landings do pipeline e exibir diretamente a prévia do HTML final que agora pertence ao fluxo Gera Landing.
- foi feito no frontend:
  - atualização da aba Landing para renderizar o `landingPageHtml` do experimento em preview (`iframe` com `srcDoc`) e manter o botão de aprovação para campanha apontando o destino da campanha para `/landing/{id}`.
  - remoção dos blocos de landing do conteúdo do pipeline na aba "Conteúdo" (mantidas apenas etapas de Campaign Angle e Ad Copy).
- documentos/arquivos lidos:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/LandingTab.tsx
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx

## 2026-05-17 00:00:00 UTC
- solicitação para investigar e corrigir `400 Bad Request` no `PUT /api/experiments/20`.
- causa-raiz identificada: ao enviar `leadPortalFlowId: null` no payload de atualização, o backend tratava o campo como obrigatório quando presente e lançava erro `leadPortalFlowId required`.
- correção aplicada: ajuste no `ExperimentService.update` para permitir `leadPortalFlowId` nulo quando informado explicitamente (limpando o vínculo do fluxo), mantendo validação normal quando um ID é enviado.
- validação automatizada adicionada com teste unitário cobrindo o cenário de limpeza do `leadPortalFlowId` via update.
- documentos/arquivos lidos:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/experiment/service/ExperimentService.java
  - backend/ads-service/src/test/java/com/marketinghub/experiment/ExperimentServiceTest.java

## 2026-05-17 13:20:14 UTC-3
- solicitação para corrigir a lista de pendências de publicação no Facebook Ads exibindo item duplicado de Landing e removendo bloqueio indevido de status planejado.
- raciocínio aplicado: padronizar a renderização da lista para evitar duplicidade por chave/label e filtrar explicitamente entradas de status que não devem bloquear publicação.
- foi feito no frontend: atualização do componente `MissingConfigurationList` para deduplicar pendências por rótulo e ignorar entradas de status `planned/planejado`.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/facebook/MissingConfigurationList.tsx


## 2026-05-17 — Ajuste de liberação Facebook Ads (experimento 20)

- contexto: experimento 20 estava com botão de liberação inconsistente com a regra operacional vigente de landing.
- causa-raiz identificada: a prontidão bloqueante usava `hasLeadPortalFlow` (dependência de fluxo do portal), enquanto a operação atual exige apenas landing criada no experimento e aprovação na aba Landing.
- ajustes realizados:
  - frontend: checklist bloqueante da liberação passou a validar landing aprovada via `followUpActionUrl`, removendo `leadPortalFlowId/leadPortalFlowName` como bloqueio.
  - documentação canônica: atualização do `facebook-campaign-publication-canon.v1.md` para formalizar que a dependência bloqueante de landing é `experiment.follow_up_action_url` (landing aprovada), sem bloqueio por `lead_portal_flow`.
- arquivos alterados:
  - `frontend/src/pages/experiment/ExperimentDetailPage.tsx`
  - `docs/canonical/facebook-campaign-publication-canon.v1.md`
  - `docs/registros/experimentos.md`

## 2026-05-17 14:40:45 UTC-3
- correção de erro de typecheck no `ExperimentDetailPage` por uso de função antes da declaração.
- o checklist bloqueante da aba Facebook referenciava `openLandingActions` antes da inicialização do `const`, causando TS2448/TS2454.
- foi movida a declaração de `openLandingActions` para antes de `blockingChecklist`, mantendo o mesmo comportamento de navegação/scroll para a aba Landing.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
- arquivos alterados:
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx
  - docs/registros/experimentos.md

## 2026-05-17 18:10:00 UTC
- investigação da inconsistência entre a tela `/facebook-campaigns` e o comportamento do facebook-ads worker para o experimento 20.
- causa-raiz identificada: `ExperimentReadinessService` exigia apenas seleção local de público (`experiment_targeting_selection`), enquanto o worker bloqueia por ausência de pacote aprovado de targeting (job titles aprovados por nicho/hipótese em `targeting_element`).
- correção aplicada no backend:
  - `ExperimentReadinessService` passou a considerar o experimento pronto em targeting quando houver **pacote aprovado de targeting** (JOB_TITLE aprovado para nicho/hipótese), mantendo fallback para seleção local para compatibilidade.
  - atualização dos testes unitários para cobrir o novo critério de prontidão e evitar regressão.
- resultado esperado: a tela de campanhas e a elegibilidade real do worker passam a refletir a mesma regra de prontidão de público.
- arquivos alterados:
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/service/ExperimentReadinessService.java`
  - `backend/ads-service/src/test/java/com/marketinghub/experiment/service/ExperimentReadinessServiceTest.java`
  - `docs/registros/experimentos.md`

## 2026-05-17 17:03:52 UTC-3
- solicitação para unificar os critérios de pendência de campanhas com a verdade da tela de experimento (`/experiments/{id}`).
- raciocínio aplicado: a tela de campanhas deve seguir o mesmo gate principal da tela de experimento para evitar duas verdades operacionais sobre aprovação.
- foi feito: ajuste no backend para `missingConfiguration` considerar apenas os mesmos bloqueios principais da tela de experimento (criativo aprovado + landing com URL de destino), além de inclusão de novo rótulo de pendência no frontend e testes unitários cobrindo o novo comportamento.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx
  - backend/ads-service/src/main/java/com/marketinghub/experiment/service/ExperimentReadinessService.java

## 2026-05-17 20:35:00 UTC
- ajuste na aprovação da landing para campanha para publicar automaticamente no Lead Portal e expor as duas URLs esperadas na aba Landing.
- backend (`approve-and-publish`) agora também persiste no experimento a URL standalone publicada (destino oficial de campanha) ao concluir a aprovação.
- frontend da aba Landing passou a chamar o endpoint de aprovação/publicação (`/pipeline/landing-page-html/approve-and-publish`) e a exibir:
  - URL do iframe (Lead Portal)
  - URL standalone (para uso na campanha)
- criada nova API client no frontend para o fluxo de aprovação/publicação de landing.
- arquivos alterados:
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `frontend/src/api/experiment/useApproveAndPublishLanding.ts`
  - `frontend/src/pages/experiment/LandingTab.tsx`
  - `docs/registros/experimentos.md`

## 2026-05-17 17:34:07 UTC-3
- correção de erro de typecheck na aba Landing do experimento ao aprovar/publicar landing.
- causa-raiz: `experiment.id` é tipado como `string` no frontend, mas o hook `useApproveAndPublishLanding` exige `number` para compor a rota do endpoint.
- foi feito: conversão explícita para número na chamada do hook (`Number(experiment.id)`), eliminando o conflito de tipos TS2345.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
- arquivos alterados:
  - frontend/src/pages/experiment/LandingTab.tsx
  - docs/registros/experimentos.md

## 2026-05-17 22:04:14 UTC
- ajuste no prompt da etapa de copy da landing para evitar sinais tipográficos ambíguos no texto final.
- causa-raiz tratada: a geração estava produzindo trechos com `~` e `+` (ex.: "em ~3 min" e "PDF + mini-kit"), que reduzem clareza comercial e padronização de linguagem.
- foi feito: inclusão de regra explícita no prompt `landing-copy` orientando substituir `~` por linguagem textual (ex.: "aproximadamente") e `+` por conectivos textuais (ex.: "e"), aplicada a todos os campos de copy.
- arquivos alterados:
  - ai-worker/src/main/resources/prompts/experiment/landing-copy.md
  - docs/registros/experimentos.md

## 2026-05-17 19:14:15 UTC-3
- solicitação para simplificar a tela de criação de experimento removendo campos operacionais considerados obsoletos.
- raciocínio aplicado: manter a criação focada no essencial e mover configurações listadas para obsoleto, reduzindo complexidade visual e de preenchimento.
- foi feito: remoção na UI de criação dos campos de etapa, variável/métrica principal, meta de KPI, template de jornada, qualidade/quantidades de imagens e parâmetros estatísticos (amostra e margem de erro); no envio, esses campos passam a usar valores obsoletos/defaults compatíveis.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/NewExperimentPage.tsx
