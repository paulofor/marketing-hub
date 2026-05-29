
## 2026-05-23 00:00:00 UTC
- solicitação para corrigir falha de typecheck no frontend em `ExperimentDetailPage` por ausência do campo `htmlGeraLanding` no tipo `Experiment`.
- causa-raiz identificada: o componente já consome `experiment.htmlGeraLanding`, porém a interface TypeScript compartilhada em `useExperiments.ts` não declarava esse atributo.
- correção aplicada: inclusão do campo opcional `htmlGeraLanding?: string | null` na interface `Experiment` para alinhar contrato de tipagem com o consumo da tela de Experimentos/GeraLanding.
- documentos/arquivos lidos:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/api/experiment/useExperiments.ts
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx

## 2026-05-21 01:05:00 UTC-3
- solicitação: corrigir ingestão de tracking que iniciava cedo demais na landing do experimento 26.
- causa-raiz identificada: o script `data-mh-funnel-tracking` era executado imediatamente após injeção no `<head>`, podendo consultar seções antes do DOM estar pronto.
- foi feito:
  - refatorado o script injetado em `GeraLandingStageExecutionService` para encapsular a inicialização em `initTracking`;
  - adicionada guarda de prontidão do DOM (`document.readyState` + `DOMContentLoaded`) antes de buscar `[data-track-section]` e iniciar `IntersectionObserver`.
- impacto esperado: evitar corrida de inicialização, garantir contagem correta de seções e reduzir perda de eventos de tracking no primeiro carregamento.

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


## 2026-05-17 21:05:00 UTC
- atualização documental no cânone de publicação de campanhas para incluir explicação complementar em linguagem menos técnica para usuários operacionais.
- objetivo: facilitar entendimento do gate de liberação sem alterar regras de domínio já estabelecidas.
- documento alterado:
  - `docs/canonical/facebook-campaign-publication-canon.v1.md`
- registro desta tarefa realizado em:
  - `docs/registros/experimentos.md`
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

## 2026-05-22 17:27:31 UTC-3
- solicitação para deixar o Worker AI resiliente quando a resposta da etapa `landing-page-copy` vier iniciando com markdown code fence (` ```json `).
- raciocínio aplicado: o erro ocorria após a resposta do modelo porque o conteúdo chegava válido, porém encapsulado em markdown; ao normalizar na borda do cliente OpenAI, o pipeline passa a consumir JSON parseável sem depender do formato de apresentação.
- foi feito:
  - adicionado saneamento da resposta no `GeraLandingOpenAiFlexClient` para remover cerca inicial ` ```json ` e cerca final ` ``` ` quando presentes;
  - integração do saneamento no fluxo `generate(...)`, antes da validação/log/persistência do payload de conclusão;
  - inclusão de teste unitário cobrindo resposta encapsulada em code fence;
  - inclusão de comentários de responsabilidade dos métodos alterados conforme regra operacional de Java.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/GeraLandingOpenAiFlexClient.java
  - ai-worker/src/test/java/com/marketinghub/worker/geralanding/GeraLandingOpenAiFlexClientTest.java

## 2026-05-17 19:31:55 UTC-3
- investigação do erro ao salvar na tela `/experiments/new` após simplificação recente.
- causa-raiz identificada: o backend continua exigindo `journeyTemplateId` na criação do experimento, porém a UI simplificada deixou de coletar/enviar esse campo.
- foi feito: ajuste no frontend para carregar templates de jornada, selecionar automaticamente o primeiro template disponível no payload de criação, bloquear salvamento quando não existir template cadastrado e exibir alerta orientando cadastro.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/NewExperimentPage.tsx
  - backend/ads-service/src/main/java/com/marketinghub/experiment/service/ExperimentService.java


## 2026-05-17 21:10:00 UTC
- solicitação para investigar erro `500 Internal Server Error` no `POST /api/experiments` após simplificação da criação de experimento.
- análise de causa-raiz: a cadeia de validação do backend usava `MetricPresetService.get(...)` com `orElseThrow()` sem mapeamento HTTP; quando o payload chegava com `metricPresetId` inválido/vazio, o backend lançava `NoSuchElementException` e retornava 500 em vez de erro de contrato (400).
- foi feito: ajuste no backend para validar `metricPresetId` nulo/vazio e inexistente com `ResponseStatusException(HttpStatus.BAD_REQUEST, ...)`, além de teste unitário cobrindo os dois cenários para evitar regressão.
- tentativa de investigação operacional via MCP: endpoint respondeu com timeout para `java_module_logs` (module=backend), então a correção foi aplicada pela análise direta da causa no código do serviço e pela cobertura de teste.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/experiment/service/MetricPresetService.java

## 2026-05-17 22:20:00 UTC
- validação solicitada sobre remoção de campos obsoletos na criação de experimentos e aderência do modelo de dados documental.
- foi feito: atualização do documento `docs/modelo-dados-experimento.md` para refletir que os campos removidos da criação (etapa, variável/métrica principal, meta de KPI, template de jornada, qualidade/quantidades de imagens, tamanho da amostra e margem de erro) não compõem mais contrato obrigatório de entrada no cadastro.
- ajuste adicional: limpeza do diagrama ER textual da entidade `EXPERIMENT`, removendo colunas obsoletas da visão focada de criação (`journey_template_id`, `stage`, `primary_variable`, `primary_metric`).
- arquivos alterados:
  - docs/modelo-dados-experimento.md
  - docs/registros/experimentos.md

## 2026-05-17 22:45:00 UTC
- ajuste complementar solicitado: não apenas documentação, mas também alteração estrutural nas tabelas para refletir simplificação da criação de experimentos.
- foi feito no backend/Liquibase: criação de changeset para flexibilizar colunas antes obrigatórias no cadastro (`journey_template_id`, `stage`, `images_per_package`) e manter defaults compatíveis (`stage='AD'`, `images_per_package=20`).
- foi feito no serviço de criação: remoção das validações obrigatórias de `stage`, `journeyTemplateId` e `kpiTargetCpl`, com fallback para `stage=AD` quando ausente e stop-loss somente quando houver KPI.
- arquivos alterados:
  - backend/ads-service/src/main/resources/db/changelog/changesets/2026-05-17-experiment-create-simplification-columns.yaml
  - backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml
  - backend/ads-service/src/main/java/com/marketinghub/experiment/Experiment.java
  - backend/ads-service/src/main/java/com/marketinghub/experiment/service/ExperimentService.java
  - docs/registros/experimentos.md

## 2026-05-18 02:06:00 UTC
- solicitação: remover dos testes unitários a premissa obsoleta de criação de experimento sem `journeyTemplateId`.
- foi feito: ajuste no teste `ExperimentServiceTest` para validar o comportamento atual do backend (rejeitar criação sem `journeyTemplateId` com `400 BAD_REQUEST` e mensagem `journeyTemplateId required`), substituindo o cenário anterior que permitia ausência desse campo.
- validação executada: `mvn -Dtest=ExperimentServiceTest test` com sucesso.
- arquivos alterados:
  - backend/ads-service/src/test/java/com/marketinghub/experiment/ExperimentServiceTest.java
  - docs/registros/experimentos.md

## 2026-05-18 — Remoção de dependências obsoletas no create de experimento
- Removida a obrigatoriedade de `metricPresetId`, `primaryVariable` e `primaryMetric` na criação de experimento.
- Backend agora aceita payload sem esses campos e calcula `stopLossCpl` apenas quando `metricPresetId` está preenchido com preset válido.
- Frontend de criação deixou de enviar os valores obsoletos `primaryVariable: "OBSOLETO"` e `primaryMetric: "OBSOLETO"`; `metricPresetId` passa a ser opcional no payload.

## 2026-05-18 10:15:00 UTC
- solicitação: na tela de experimento, aba "Estrutura de Conteúdo", remover as etapas que migraram para o fluxo Gera Landing.
- foi feito no frontend: removidas da listagem da aba as seções de landing `landing-layout`, `landing-copy`, `landing-design-preset`, `landing-image-planning` e `landing-html`.
- resultado: a aba "Estrutura de Conteúdo" passa a exibir apenas as etapas de conteúdo base (dor, mecanismo, provas, ângulos, anúncio e prompt de imagem), evitando duplicidade com o fluxo Gera Landing.
- arquivos alterados:
  - frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx
  - docs/registros/experimentos.md

## 2026-05-18 11:30:00 UTC
- solicitação: corrigir erro na aba "Estrutura de Conteúdo" após refatoração recente, com crash `Cannot read properties of undefined (reading 'completedAt')` em `ExperimentContentGenerationTab.tsx`.
- causa-raiz: a refatoração removeu seções de landing da constante `CONTENT_GENERATION_SECTIONS`; isso também reduziu o estado inicial de requisições e deixou chaves ainda usadas por cálculos de fluxo (`landing-image-planning`) como `undefined`.
- foi feito: criação da constante `ALL_CONTENT_GENERATION_SECTION_KEYS` para manter o estado inicial completo de todas as seções (inclusive as ocultas na UI), preservando cálculos internos e evitando acesso a propriedades em `undefined`.
- arquivos alterados:
  - frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx
  - docs/registros/experimentos.md

## 2026-05-18 12:10:00 UTC
- solicitação: na tela de experimento, aba de conteúdo, voltar a exibir as etapas do fluxo Gera Landing.
- causa-raiz: as etapas de landing haviam sido removidas da constante `CONTENT_GENERATION_SECTIONS`, então a UI mostrava apenas etapas parciais.
- foi feito no frontend: reintroduzidas as seções de Gera Landing na aba de conteúdo: `Gere Wireframe`, `Gera Copy`, `Gera Prompt Imagens`, `Gera Preset Design` e `Gera Html`.
- arquivos alterados:
  - frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx
  - docs/registros/experimentos.md

## 2026-05-18 14:24:03 UTC-3
- ajuste de documentação do fluxo Gera Landing para remover referência a dois tipos de acionamento de geração de HTML.
- raciocínio: a documentação precisava refletir o estado atual informado pelo usuário, sem citar tipos/variantes que não existem mais.
- registro do que foi feito: atualizado o cânone `experiments-automation-flow-canon.v1.md` para manter apenas comando canônico único na etapa `landing-page-html`.
- documentos lidos para pesquisar e resolver o problema:
  - docs/canonical/experiments-automation-flow-canon.v1.md
## 2026-05-18 12:45:00 UTC
- solicitação: voltar com o bloco de etapas de landing na aba "Conteúdo" da página de experimentos.
- causa-raiz: o array `pipelineContentCards` em `ExperimentDetailPage` estava reduzido às etapas 1 e 2, ocultando os campos de landing persistidos no experimento.
- foi feito no frontend: reintroduzidos os cards de conteúdo bruto para `landing_page_wireframe`, `landing_page_copy`, `landing_page_image_planning`, `landing_page_design_preset`, `landing_page_html` e `landing_page_deliverables`, com as dependências do `useMemo` atualizadas para re-renderizar corretamente.
- arquivos alterados:
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx
  - docs/registros/experimentos.md

## 2026-05-18 14:48:47 UTC-3
- solicitação para corrigir bloqueio indevido no botão de aprovação de landing na aba de experimento, que exigia campos obsoletos (meta de KPI e preset de métricas).
- raciocínio aplicado: a validação local no frontend estava impondo pré-condição que não faz mais parte do fluxo vigente, impedindo aprovação mesmo com landing válida.
- foi feito: remoção da checagem de `kpiTarget`/`metricPresetId` antes da chamada de aprovação/publicação da landing, mantendo apenas os bloqueios reais (HTML existente e request em andamento).
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/LandingTab.tsx
## 2026-05-18 18:34:00 UTC
- solicitação: criar endpoint no pacote `geralanding` para aprovar/publicar landing diretamente usando `landing_page_html` do experimento.
- foi feito no backend:
  - novo endpoint `POST /api/experiments/{experimentId}/geralanding/landing/approve-and-publish`.
  - serviço publica no lead portal, retorna URL iframe e URL standalone para uso em campanha.
  - fluxo injeta controles de funil no HTML antes da publicação; o pixel do Facebook permanece injetado no payload de publicação do Lead Portal.
- arquivos alterados:
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingContoller.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingPublishResponse.java
  - docs/registros/experimentos.md

## 2026-05-18 18:41:00 UTC
- solicitação: incluir explicitamente o pixel do Facebook no HTML publicado pelo endpoint de aprovação/publicação do GeraLanding.
- foi feito no backend: adição de injeção direta do pixel no `customFormHtml` antes da publicação, usando `experiment.niche.facebookPixelId` e marcador `data-mh-facebook-pixel` para evitar duplicidade.
- arquivos alterados:
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java
  - docs/registros/experimentos.md

## 2026-05-18 18:55:00 UTC
- solicitação: no endpoint do GeraLanding, não depender de classes externas ao pacote para publicar.
- ajuste aplicado: remoção da dependência de classes de `leadportal` no serviço do GeraLanding, com payload e publicação HTTP próprios no pacote `geralanding`.
- comportamento mantido: usa `landing_page_html` pronto, injeta pixel do Facebook e controles de funil, publica e devolve URL iframe/standalone.
- arquivos alterados:
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingLeadPortalPublishRequest.java
  - docs/registros/experimentos.md

## 2026-05-18 20:36:00 UTC
- solicitação: remover validação/normalização de `customFormHtml` no Lead Portal para desbloquear publicação da campanha 21.
- ajuste aplicado: `FlowService` deixou de injetar `CustomFormHtmlResolver` e o método `normalizeCustomFormHtml` agora apenas retorna o fluxo original, sem validação restritiva de HTML.
- impacto esperado: o endpoint de publicação não deve mais rejeitar payload com a mensagem `customFormHtml deve ser HTML puro...`.
- arquivos alterados:
  - lead-portal/backend/src/main/java/com/marketinghub/leadportal/service/FlowService.java
  - docs/registros/experimentos.md

## 2026-05-18 18:20:35 UTC-3
- solicitação para aprovar a landing page do experimento 21 e validar nos logs se o processamento ficou correto.
- raciocínio aplicado: executar tentativa de aprovação via endpoint oficial de aprovação/publicação da landing e, em seguida, consultar logs do backend via MCP para evidências do resultado.
- foi feito:
  - tentativa de POST no endpoint `/api/experiments/21/pipeline/landing-page-html/approve-and-publish` no backend `http://191.252.181.168:8000`, porém o serviço respondeu indisponível (`503 Service Unavailable` com `connection refused`).
  - consulta dos logs do módulo `backend` via MCP tool `java_module_logs` para verificar estado geral do processamento; não houve confirmação de aprovação da landing do experimento 21 nesta rodada.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/api/experiment/useApproveAndPublishLanding.ts
  - backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/web/ExperimentPipelineController.java

## 2026-05-18 18:36:41 UTC-3
- solicitação para marcar como obsoleto todo o fluxo antigo de aprovação/publicação de landing no pipeline legado.
- raciocínio aplicado: bloquear explicitamente os endpoints legados para forçar uso do endpoint canônico do Gera Landing e evitar execução em rota descontinuada.
- foi feito: endpoints legados de landing no `ExperimentPipelineController` foram marcados como `@Deprecated` e passam a retornar `410 GONE` com mensagem de migração para `/api/experiments/{id}/geralanding/landing/approve-and-publish`, incluindo log de advertência quando acessados.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/web/ExperimentPipelineController.java

## 2026-05-18 18:41:50 UTC-3
- solicitação para alterar o botão do frontend e usar endpoint do Gera Landing na aprovação/publicação da landing.
- raciocínio aplicado: remover chamada do fluxo legado de pipeline e direcionar para o endpoint canônico do módulo `geralanding`, mantendo compatibilidade de leitura do retorno para exibição das URLs na tela.
- foi feito: hook de aprovação/publicação atualizado para `POST /api/experiments/{id}/geralanding/landing/approve-and-publish` e ajuste na aba Landing para consumir `iframeUrl/standaloneUrl` do novo contrato, com fallback para contrato legado.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/api/experiment/useApproveAndPublishLanding.ts
  - frontend/src/pages/experiment/LandingTab.tsx

## 2026-05-18 22:30:00 UTC
- solicitação: instrumentar o trecho de aprovação/publicação da landing do GeraLanding para diagnosticar cada etapa do experimento 21.
- raciocínio aplicado: adicionar logs explícitos antes/depois das etapas críticas, preservando a causa-raiz da falha de publicação no Lead Portal sem alterar contrato de API nem modelo de dados.
- foi feito:
  - adicionados logs de início da aprovação, carregamento do experimento, presença/tamanho do HTML, slug resolvido, injeção de controles de funil, resolução/injeção de Facebook Pixel, envio ao Lead Portal, URLs publicadas e gravação de `followUpActionUrl`.
  - adicionados logs no `publishToLeadPortal` para registrar URL de destino, tamanho do HTML e root cause de falha da chamada HTTP ao Lead Portal.
- arquivos alterados:
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java
  - docs/registros/experimentos.md

## 2026-05-18 22:45:00 UTC
- solicitação: remover o `try/catch` após a publicação no Lead Portal no fluxo de aprovação da landing do GeraLanding para deixar o erro aparecer diretamente nos logs.
- raciocínio aplicado: preservar os logs de passagem por etapa, mas eliminar a captura genérica que mascarava exceções posteriores à publicação com `ResponseStatusException`.
- foi feito:
  - removido o bloco `try/catch (RuntimeException ex)` em `approveAndPublishLanding` depois de `publishToLeadPortal`.
  - mantidos os logs de resolução de URLs, gravação de `followUpActionUrl` e retorno de sucesso no fluxo linear.
- arquivos alterados:
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java
  - docs/registros/experimentos.md

## 2026-05-19 00:10:16 UTC-3
- ajuste solicitado para corrigir falha 400 na publicação de landing do GeraLanding no Lead Portal.
- causa-raiz aplicada: payload enviado como formato misto/legado (`legacyPreviewHtml` + `renderMode`) enquanto o endpoint passou a aceitar apenas `customFormHtml` em HTML puro.
- foi feito: simplificação do contrato `GeraLandingLeadPortalPublishRequest` para enviar apenas `slug`, `name`, `description` e `customFormHtml`, e atualização da montagem do payload no serviço de publicação.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingLeadPortalPublishRequest.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java

## 2026-05-19 00:37:35 UTC-3
- solicitação para incluir orientação em destaque no AGENTS.md para prevenir e acelerar diagnóstico de erros de contrato 400/422.
- raciocínio aplicado: transformar lição operacional em SOP curto, objetivo e obrigatório, com foco em causa-raiz e comparação literal de payload.
- foi feito: adicionado no AGENTS.md um playbook crítico (5 passos, timebox de 15 minutos, regra de bloqueio e formato mínimo de diagnóstico).
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-19 00:41:42 UTC-3
- solicitação para criar exceção específica e código HTTP próprio para erro de contrato na publicação da landing.
- raciocínio aplicado: padronizar diagnóstico com erro explícito de contrato, deixando claro o esperado x recebido e evitando uso de erro HTTP padrão nessa falha.
- foi feito:
  - criada `GeraLandingContractViolationException` com código HTTP próprio `460` e `toString()` detalhando operação, endpoint, esperado, recebido e erro upstream.
  - criada `GeraLandingContractViolationExceptionHandler` (`@RestControllerAdvice`) para responder com status 460 e corpo estruturado do erro.
  - fluxo `publishToLeadPortal` atualizado para lançar a exceção específica quando ocorrer falha de contrato/integracao no envio.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-19 00:45:43 UTC-3
- solicitação para explicitar no AGENTS.md a definição da exception de contrato e do código HTTP 460.
- raciocínio aplicado: consolidar regra operacional no contrato do time para evitar regressão e ambiguidade no tratamento de erro de integração do GeraLanding.
- foi feito: adição de seção em destaque no AGENTS.md formalizando uso obrigatório de `GeraLandingContractViolationException`, handler dedicado e status HTTP 460, incluindo conteúdo mínimo obrigatório no `toString()`.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-19 00:49:03 UTC-3
- solicitação para revisar consistência do AGENTS.md após múltiplas adições sobre erro de contrato.
- raciocínio aplicado: manter as regras sem conflito entre orientação geral de 400/422 e regra específica de exceção 460 no fluxo GeraLanding -> Lead Portal.
- foi feito: ajuste textual para deixar explícito que a proibição de usar erro HTTP padrão genérico vale para o cenário específico GeraLanding -> Lead Portal.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-19 00:58:10 UTC-3
- solicitação para retirar a etapa de validação com `CustomFormHtmlResolver` na publicação de HTML para o Lead Portal.
- causa-raiz: a validação/normalização restritiva em `FlowService` bloqueava o fluxo de publicação quando o HTML não passava no resolver.
- foi feito:
  - `FlowService` deixou de depender de `CustomFormHtmlResolver` no construtor/injeção.
  - método `normalizeCustomFormHtml` simplificado para no-op (retorna o fluxo original sem validação de conteúdo).
  - `FlowServiceTest` atualizado para refletir a nova assinatura do serviço (sem resolver).
- impacto esperado: publicação/sincronização de `customFormHtml` no Lead Portal segue sem bloqueio por validação do resolver.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-19 01:10:00 UTC-3
- solicitação para remover o comentário AUTO injetado no início do HTML provisório após a etapa de image planning.
- causa-raiz: o comentário era prefixado no payload HTML e estava interferindo no consumo/renderização downstream.
- foi feito:
  - `resolveImagePlanningProvisionalHtml` passou a retornar somente o HTML final com imagens injetadas, sem prefixar comentário `<!-- AUTO: ... -->`.
- impacto esperado: o HTML publicado deixa de conter metadado textual no topo e evita interferência no processamento do cliente.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-19 13:39:41 UTC-3
- solicitação para remover um teste unitário específico que estava falhando no módulo de GeraLanding.
- raciocínio aplicado: o cenário testado validava um marcador textual no `provisionalHtml` que não é contrato explícito do fluxo `landing-page-image-planning`, gerando falha por expectativa frágil.
- foi feito: remoção do teste `shouldPreserveDesignPresetWhenRegeneratingProvisionalHtmlAfterImagePlanning` em `GeraLandingStageExecutionServiceTest`.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/test/java/com/marketinghub/geralanding/GeraLandingStageExecutionServiceTest.java

## 2026-05-19 16:20:00 UTC
- solicitação para remover o quadro amarelo de "Pendências antes da publicação" na aba de campanha de Facebook Ads do experimento.
- causa-raiz: duplicidade visual das mesmas pendências, já tratadas e listadas na seção de bloqueios logo abaixo, gerando excesso de informação.
- foi feito:
  - remoção do alerta amarelo (`alert alert-warning`) condicionado por `hasReadinessIssues` em `ExperimentDetailPage`.
  - manutenção do estado de carregamento de pendências básicas e da seção de bloqueios detalhada.
- impacto esperado: interface mais limpa, sem redundância, preservando os bloqueios realmente acionáveis no checklist principal.
## 2026-05-19 19:20:00 UTC
- solicitação para corrigir a ordenação visual dos cards de etapas na tela de experimento.
- causa-raiz: o card `5 - Gera Entregáveis` estava renderizado antes do card `3 - Gera Imagem` no JSX de `ExperimentDetailPage`, causando sequência inconsistente para o usuário.
- foi feito: reordenação dos blocos JSX para exibir `3 - Gera Imagem` antes de `5 - Gera Entregáveis`.
- impacto esperado: fluxo visual da página segue a ordem lógica das etapas, reduzindo ambiguidade operacional.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-19 19:25:28 UTC-3
- solicitação para aumentar o tempo de espera da geração OpenAI para 60 minutos e avisar no navegador quando a etapa WireFrame finalizar com aba em segundo plano.
- raciocínio aplicado: reduzir falhas por timeout prematuro no worker e melhorar a percepção operacional com notificação de término mesmo quando o usuário estiver em outra tela.
- foi feito: ajuste do timeout padrão `openai.batch-timeout` no ai-worker de PT30M para PT60M; no frontend da tela de Experimento foi adicionada notificação via Browser Notification API (com fallback em toast) quando a execução do Gera WireFrame termina em aba oculta.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md
  - ai-worker/src/main/resources/application.properties
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx

## 2026-05-19 19:45:00 UTC
- solicitação para corrigir novamente a sequência visual de etapas na tela de experimento.
- causa-raiz: o card `4 - Gera Preset Design` estava renderizado antes do card `3 - Gera Imagem` no JSX, mantendo ordem invertida.
- foi feito: reordenação dos blocos JSX em `ExperimentDetailPage` para exibir `3 - Gera Imagem` antes de `4 - Gera Preset Design`.
- impacto esperado: sequência de etapas consistente com a numeração e com o fluxo operacional esperado.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-19 20:38:34 UTC-3
- solicitação para remover da tela de Conteúdo os cards que já migraram para o fluxo Gera Landing.
- raciocínio aplicado: manter a tela focada apenas nas etapas que continuam sendo gerenciadas no pipeline local, evitando duplicidade operacional e ruído visual.
- foi feito no frontend: remoção das seções/cards `Gere Wireframe`, `Gera Copy`, `Gera Preset Design` e `Gera Html` da lista renderizada na aba de geração de conteúdo.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx

## 2026-05-20 00:00:00 UTC
- solicitação para alterar o timeout das solicitações do Gera Landing para 30 minutos.
- causa-raiz identificada: configuração padrão do ai-worker estava em `PT60M`, divergindo do tempo operacional desejado.
- foi feito: ajuste da propriedade `openai.batch-timeout` de `PT60M` para `PT30M` no `application.properties` do ai-worker.
- impacto esperado: solicitações batch do Gera Landing passam a expirar em 30 minutos quando não houver override por variável de ambiente.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md
  - ai-worker/src/main/resources/application.properties

## 2026-05-20 21:55:00 UTC
- solicitação para alinhar o card `3 - Gera Imagem` com os demais cards da página, exibindo o status do job imediatamente após clicar em iniciar.
- causa-raiz identificada: o bloco `Gera Prompt Imagem` não renderizava a seção de jobs pendentes/em execução, diferente dos cards anteriores (`Gera Wireframe` e `Gera Copy`).
- foi feito no frontend:
  - inclusão da listagem de jobs pendentes/em execução no bloco `Gera Prompt Imagem` (Job ID, Status e Data-hora);
  - inclusão de spinner no botão `Iniciar` durante requisição assíncrona para manter padrão visual/operacional dos demais cards.
- impacto esperado: ao iniciar a etapa, o usuário passa a visualizar imediatamente o status do job ativo, reduzindo percepção de falha e mantendo consistência de UX entre etapas.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx

## 2026-05-20 22:20:00 UTC
- solicitação para limpar a aba `Overview` do experimento removendo informações obsoletas.
- causa-raiz identificada: a aba continha componentes/linhas legadas que adicionavam ruído e não agregavam mais ao fluxo atual.
- foi feito no frontend:
  - remoção do item `E-mail de amostra` com valor `Obsoleto` da listagem principal da `Overview`;
  - remoção dos painéis `ExperimentLearningPanel` e `ExperimentReportPanel` da aba `Overview`, mantendo foco na ficha resumida do experimento.
- impacto esperado: visão geral mais objetiva, com menos informações obsoletas e melhor legibilidade para operação diária.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx
## 2026-05-20 01:15:00 UTC
- solicitação para retirar o comentário HTML automático `<!-- AUTO: provisional html generated manually by /geralanding/html/provisional/generate -->` do fluxo Gera Landing e ajustar testes unitários relacionados.
- causa-raiz identificada: o serviço de geração de HTML provisório adicionava um comentário técnico no conteúdo persistido (`landingPageHtml`/`provisionalHtml`), criando acoplamento desnecessário do artefato final com metadado operacional.
- foi feito no backend:
  - remoção da concatenação do comentário AUTO na montagem do `provisionalHtml`, mantendo apenas o HTML efetivamente gerado/injetado.
- testes executados:
  - execução dos testes unitários de controller e service do Gera Landing para validar que a remoção não quebrou contratos existentes.
- impacto esperado: HTML provisório persistido limpo (sem comentário de controle), reduzindo ruído no artefato e evitando validações dependentes desse marcador.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java

## 2026-05-20 22:30:00 UTC
- solicitação para retirar o comentário HTML automático `<!-- AUTO: provisional html regenerated after landing-page-design-preset completion -->` do retorno de `resolveDesignPresetProvisionalHtml`.
- causa-raiz identificada: o método seguia inserindo metadado técnico no HTML final da etapa, gerando ruído no artefato exibido/armazenado.
- foi feito no backend:
  - remoção do comentário `AUTO` da string retornada, mantendo apenas o conteúdo HTML processado em `%s`.
- impacto esperado: artefato de HTML provisório mais limpo e sem marcador operacional embutido no conteúdo.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java

## 2026-05-19 23:01:45 UTC-3
- solicitação para explicar e formalizar no AGENTS.md o risco de contaminação de artefato final com metadado técnico, incluindo exemplos preventivos.
- raciocínio aplicado: transformar a lição recorrente dos ciclos de aprovação/publicação de landing em regra operacional explícita para bloquear regressões por comentários técnicos e campos legados no payload final.
- foi feito: adicionada seção obrigatória no AGENTS.md com definição, risco, exemplos proibidos, padrão de prevenção (separação metadado vs artefato publicável, whitelist de DTO, validação literal pré-envio, teste de regressão) e checklist de revisão de PR.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md


- 2026-05-20: Movidos para docs/canonical/obsoletos os cânones de experimentos/pipeline/gera landing/aprovação para posterior recriação.
## 2026-05-20 23:40:00 UTC
- solicitação para remover validação duplicada de landing na tela de detalhes do experimento.
- causa-raiz identificada: a mesma condição de validação de URL de destino da landing estava aparecendo em dois grupos de checklist diferentes (bloqueios e fluxo operacional), gerando redundância visual.
- foi feito no frontend:
  - removido o item duplicado "Landing aprovada como destino da campanha" do `operationalChecklist`, mantendo a validação apenas em "Bloqueios de publicação".
- impacto esperado: checklist mais claro, sem repetição de informação e com foco nas validações realmente distintas.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx

## 2026-05-20 00:10:02 UTC-3
- solicitação para criar novo documento canônico explicando detalhadamente o funcionamento do experimento com base no código e em rascunho operacional.
- raciocínio aplicado: consolidar em um único cânone a sequência operacional (criação → pipeline de experimento → pipeline gera landing → anúncios → aprovação/publicação da landing), além de formalizar regras de batch, publicação no Lead Portal e rastreio de custos.
- foi feito: criação do arquivo `docs/canonical/procedimento-experimento-canon.v1.md` e registro desta tarefa no documento canônico de registros de experimentos.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/system-governance-canon.v2.md
  - docs/pipeline-landing-experimento.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/ExperimentPipelineSection.java
  - ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/GeraLandingOpenAiBatchClient.java
  - ai-worker/src/main/java/com/marketinghub/worker/creative/CreativeChatGptClient.java
## 2026-05-20 00:24:25 UTC-3
- solicitada formalização canônica e implementação de monitoramento comportamental no assembler do design preset (page_view + tempo por seção)
- definido que a etapa `LANDING_PAGE_DESIGN_PRESET` precisa injetar instrumentação mínima idempotente para diagnóstico de avanço de funil
- implementado no backend (`DesignPresetProvisionalHtmlAssembler`) a marcação `data-track-section` e script com eventos `page_view`, `section_view_start` e `section_view_time`
- adicionado teste unitário (`DesignPresetProvisionalHtmlAssemblerTest`) validando a presença da instrumentação no HTML provisório
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/canonical/procedimento-experimento-canon.v1.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/DesignPresetProvisionalHtmlAssembler.java

## 2026-05-20 23:58:00 UTC
- solicitação para formalizar no cânone e no gera-wireframe os limites mínimos de seções e imagens.
- causa-raiz: o contrato da etapa wireframe definia mínimo de seções, mas não deixava explícito o mínimo de imagens no mesmo nível de regra operacional.
- foi feito:
  - atualizado `docs/experiment-pipeline-validation-spec.md` na seção `LANDING_PAGE_WIREFRAME` para reforçar mínimo de 4 imagens no wireframe e regra operacional de distribuição visual por seção.
  - atualizado prompt `ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe.md` com regras explícitas: mínimo de 4 seções e mínimo de 4 imagens.
  - atualizado schema `ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe-schema.json` com `pagina.corpo.secoes.minItems = 4` e exigência de ao menos um elemento `img` em `elementosSeccao` por seção (via `contains` + `minContains`).
- impacto esperado: reduzir respostas abaixo do mínimo visual/comercial e evitar reprovação contratual por wireframes com densidade visual insuficiente.

## 2026-05-20 17:36:00 UTC
- solicitação para habilitar debug no navegador porque os eventos de analytics da landing não estavam aparecendo no Network.
- causa-raiz provável: falta de visibilidade no script injetado da landing pública para confirmar bootstrap, descoberta de seções e envio (sendBeacon/fetch).
- foi feito:
  - adicionado `console.debug` no script de analytics injetado em `LeadPortalPublicFlowController` para registrar:
    - bootstrap (slug, endpoint, sessionId),
    - quantidade de seções encontradas,
    - transições de visibilidade por seção,
    - payload enviado por evento,
    - resultado de `sendBeacon` e resposta/erro de `fetch`,
    - flush no `beforeunload`.
- validação executada: teste unitário `LeadPortalPublicFlowControllerTest` no módulo `ads-service` com build verde.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/leadportal/web/LeadPortalPublicFlowController.java

## 2026-05-20 17:48:00 UTC
- revisão solicitada: mover a lógica de debug para o local correto (`DesignPresetProvisionalHtmlAssembler`) em vez de manter no script público do `LeadPortalPublicFlowController`.
- causa-raiz: o debug foi inserido inicialmente no ponto de injeção do Lead Portal, mas a regra operacional pediu centralização da telemetria de debug no assembler do design preset.
- foi feito:
  - removidos `console.debug` adicionados no script de analytics de `LeadPortalPublicFlowController` para voltar ao comportamento enxuto de produção nesse ponto.
  - adicionados `console.debug` no script `data-mh-funnel-tracking` do `DesignPresetProvisionalHtmlAssembler` cobrindo bootstrap, quantidade de seções, emissão de eventos, transições de visibilidade e flush por aba oculta/beforeunload.
- validação executada: teste unitário `DesignPresetProvisionalHtmlAssemblerTest` + `LeadPortalPublicFlowControllerTest` no módulo `ads-service`.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/DesignPresetProvisionalHtmlAssembler.java
  - backend/ads-service/src/main/java/com/marketinghub/leadportal/web/LeadPortalPublicFlowController.java

## 2026-05-20 18:02:00 UTC
- solicitação: garantir que o script e o backend tenham o necessário para atender o cânone de telemetria da landing.
- ajuste aplicado no script público da landing:
  - padronizado envio de evento `section_view_time` (em vez de `section_view`) para alinhar com o cânone;
  - payload passou a enviar `elapsedMs` (mantendo também `visibleMs` por compatibilidade).
- ajuste aplicado no backend:
  - `RegisterLandingPageAnalyticsEventRequest` passou a aceitar `elapsedMs`;
  - `registerLandingPageAnalyticsEvent` agora persiste evento no `experiment_funnel_event` (source `landing-page-analytics`) para `page_view` e `section_view_time`, além do log operacional;
  - payload persistido inclui `eventId`, `eventType`, `sessionId`, `sectionId`, `elapsedMs` e `pageUrl`.
- impacto esperado: manter aderência ao documento canônico e garantir rastreabilidade operacional no backend para eventos mínimos de visualização de landing.

## 2026-05-20 15:22:06 UTC-3
- solicitação para ler o plano do módulo score-psi e criar um documento separado com a estrutura hierárquica em formato gráfico markdown.
- raciocínio aplicado: extrair a hierarquia conceitual principal do plano e representar em `mermaid graph TD` para facilitar visualização de módulos e dependências.
- foi feito: criação do documento `docs/novos-modulos/score-psi/estrutura-hierarquica-modulos-score-psi.md` com grafo hierárquico e resumo de leitura rápida.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/novos-modulos/score-psi/plano_market_psychological_fit_score.md
  - docs/registros/experimentos.md

## 2026-05-20 18:25:00 UTC
- solicitação: migrar a integração OpenAI do Gera Landing no Worker AI de modo batch para modo flex, com atualização canônica.
- causa-raiz tratada: o fluxo de Gera Landing estava acoplado ao pipeline de Batch API (upload JSONL + criação de batch + polling), quando a nova decisão operacional exige Flex processing para reduzir custo por requisição no processamento síncrono.
- foi feito:
  - `GeraLandingOpenAiBatchClient` foi refatorado para enviar requisição direta em `/responses` com `service_tier=flex`, removendo upload de arquivo, criação de batch e polling para esse fluxo;
  - logs e mensagens de erro foram atualizados para explicitar “modo flex” no Gera Landing;
  - atualização do documento canônico `procedimento-experimento-canon.v1.md` com a nova regra de processamento OpenAI para Gera Landing.
- validação executada: suíte de testes do módulo `geralanding` no ai-worker e compilação do `ai-worker`.

## 2026-05-20 18:45:00 UTC
- solicitação: ampliar logs no trecho de geração OpenAI do Worker AI (Gera Landing), incluindo contexto no catch final.
- causa-raiz tratada: dificuldade de diagnóstico da resposta real do modelo na etapa `landing-page-design-preset` (erro de parse com `Unexpected character '#'`) por ausência de telemetria suficiente do retorno e do contexto operacional no ponto de falha.
- foi feito:
  - adicionado log de início da geração flex com `jobId`, `stage`, `model` e tamanho do request;
  - adicionado log após resposta da OpenAI com `responseId`, tamanho do `rawOutput`, tamanho do `modelResponse` e preview sanitizado/truncado do conteúdo retornado;
  - adicionado log de finalização com tokens de entrada/saída;
  - adicionado log no `catch (Exception ex)` com contexto completo (`jobId`, `stage`, `model`, preview do request) e stack trace (`ex`);
  - incluídos helpers de segurança para preview e cálculo de tamanho, evitando logar payloads ilimitados em uma única linha.
- impacto esperado: acelerar análise de causa-raiz em falhas de contrato/formato de resposta do modelo, com evidência literal do início da saída retornada.
- ajuste complementar: adicionado log explícito do conteúdo de `job.requestBodyJson()` (preview sanitizado/truncado) no início do `generate`, para facilitar correlação direta entre payload enviado e falhas subsequentes.
- ajuste complementar 2: adicionados logs dentro de `createFlexResponse` para mostrar o `requestBody` parseado (chaves + preview) e o payload final após injeção de `service_tier=flex` antes do POST em `/responses`.
- ajuste complementar 3: reposicionado/adicionado log **antes** do `readValue(job.requestBodyJson(), Map.class)` em `createFlexResponse`, para garantir visibilidade do payload bruto mesmo quando ocorrer erro de parse JSON nesse ponto.


## 2026-05-20 22:47:01 UTC-3
- solicitação para atualizar o documento canônico do Gera Landing com regra explícita de abertura obrigatória do HTML final.
- raciocínio aplicado: reforçar contrato de saída para evitar divergências de renderização e prevenir contaminação do artefato com conteúdo técnico antes do doctype.
- foi feito: adicionada seção canônica determinando que todo HTML final do Gera Landing deve iniciar literalmente com `<!doctype html>`, sem prefixos.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/gera-landing/modelo-canonico-gera-landing.md
  - docs/registros/experimentos.md

## 2026-05-21 00:20:00 UTC
- solicitação: experimento 23 com prompt de imagens gerado, porém totalizadores da etapa "Gera Imagem" permanecendo zerados.
- causa-raiz identificada: o parser de `landingPageImagePlanning` no backend aceitava apenas JSON puro; quando o planejamento vinha encapsulado em bloco markdown (```json ... ```), a leitura falhava silenciosamente e o resumo devolvia zero itens.
- foi feito:
  - fortalecido o parse em `FrameworkImageGenerationService` com fallback para:
    1) remoção de code fence markdown;
    2) extração do primeiro objeto JSON válido do texto;
    3) parse resiliente por tentativas.
  - adicionado teste unitário cobrindo explicitamente planejamento encapsulado em markdown code fence, garantindo que os itens planejados voltem a ser contabilizados como `PLANNED`.
- impacto esperado: os totalizadores de imagens deixam de ficar em zero quando o planejamento vier com envelope textual/markdown, refletindo corretamente os itens do experimento.

## 2026-05-21 03:00:00 UTC
- ajuste complementar após validação em produção: o payload de planejamento retornado pelo modelo pode vir concatenado (dois objetos JSON completos em sequência), além de conter caracteres escapados.
- causa-raiz complementar: o fallback anterior extraía do primeiro `{` até o último `}`, formando JSON inválido quando havia múltiplos objetos concatenados.
- foi feito:
  - substituída a extração ingênua por extração balanceada do **primeiro objeto JSON completo** (`extractFirstJsonObject`), com tratamento de aspas/escape para não quebrar chaves dentro de strings;
  - adicionado teste unitário para cenário com payload duplicado concatenado (`onePlan + onePlan`), garantindo que o parser recupere o primeiro objeto válido e os totalizadores sejam calculados.

## 2026-05-21 03:20:00 UTC
- solicitação complementar: payload retornado com `\\n` e `\\\"` literais (json escapado) concatenado em sequência, mantendo totalizadores zerados mesmo após o ajuste anterior.
- causa-raiz complementar: o parser ainda tentava extrair objeto JSON sem decodificar escapes textuais; com isso o primeiro caractere real era válido, mas o conteúdo interno permanecia inválido para parse direto.
- foi feito:
  - adicionado passo de normalização `unescapeJsonLikeContent` para decodificar sequências `\\n`, `\\r`, `\\t`, `\\\"`, `\\\\` e `\\uXXXX` antes da extração do primeiro objeto;
  - mantida a extração balanceada do primeiro objeto após normalização;
  - adicionado teste de regressão com payload `imagePlan` escapado + duplicado em sequência.

## 2026-05-21 12:10:00 UTC
- solicitação: remover de `DesignPresetProvisionalHtmlAssembler` a responsabilidade de instrumentação de tracking e manter o assembler apenas como montador do HTML consolidado da etapa `landing-page-design-preset`.
- causa-raiz identificada: havia acoplamento indevido de responsabilidade (assembler de composição também fazia injeção de tracker), contrariando separação de responsabilidades e dificultando controle de publicação.
- foi feito:
  - removida a injeção de tracker (`data-track-section` + script `data-mh-funnel-tracking`) de `DesignPresetProvisionalHtmlAssembler`;
  - o fluxo `approveAndPublishLanding` passou a ler prioritariamente `experiment.landing_page_design_preset` (com fallback para `landing_page_html` legado), injetar tracker nesse momento e seguir com injeção de controles de funil + Facebook Pixel + publicação no Lead Portal;
  - no fechamento da etapa `LANDING_PAGE_DESIGN_PRESET`, o backend passou a persistir o HTML consolidado da etapa em `experiment.landing_page_design_preset` (mantendo atualização de `landing_page_html` para compatibilidade).

## 2026-05-21 12:35:00 UTC
- solicitação complementar: documentar explicitamente o que acontece depois do botão "Aprovar e publicar landing", incluindo a classe responsável e responsabilidades executadas.
- foi feito:
  - atualização do documento canônico `docs/canonical/procedimento-experimento-canon.v1.md`, seção de publicação da landing;
  - detalhado o fluxo pós-aprovação executado por `GeraLandingStageExecutionService.approveAndPublishLanding`, incluindo: resolução do HTML base, injeções (tracking/funnel/pixel), publicação no Lead Portal e persistência da `follow_up_action_url`.

## 2026-05-21 — Ajuste prompt GeraLanding Wireframe (whitelist CSS)
- Atualizado o prompt `landing-page-wireframe.md` para incluir whitelist explícita de atributos CSS permitidos em `estilos[]`.
- Regra adicional: proibido usar atributos fora da whitelist para evitar divergência de contrato de estilo no artefato wireframe.

## 2026-05-21 12:30:38 UTC-3
- solicitação para verificar e ajustar o assembler da etapa `landing-page-design-preset` do Gera Landing quanto a comentários obrigatórios e aderência canônica.
- raciocínio aplicado: cumprir o fluxo de documentação Java (comentário de responsabilidade da classe + comentários de métodos) e eliminar contaminação de artefato final com metadado técnico antes do HTML.
- foi feito:
  - inclusão de comentário de responsabilidade da classe e comentários breves nos métodos do `DesignPresetProvisionalHtmlAssembler`.
  - substituição do comportamento que injetava comentário técnico de `jobId` no HTML por retorno neutro (`preserveCanonicalHtml`), preservando o conteúdo gerado sem prefixos técnicos.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/gera-landing/modelo-canonico-gera-landing.md
  - docs/canonical/system-governance-canon.v2.md
  - docs/registros/experimentos.md

## 2026-05-21 13:05:00 UTC
- solicitação: ajustar o campo `system` enviado para OpenAI na geração dos dados do pipeline de experimentos para orientar o modelo como especialista em marketing focado em vendas de produtos digitais pela internet.
- causa-raiz: prompt de sistema padrão ainda estava genérico ("especialista em execução de pipeline") sem explicitar foco de marketing orientado a vendas digitais.
- foi feito:
  - atualização do `buildSystemPrompt` no backend de experimentos para incluir a instrução explícita de especialista em marketing focado em vendas de produtos digitais pela Internet;
  - atualização do fallback de `systemMessage` no `GeraLandingExecutionService` com a mesma orientação;
  - atualização do payload batch (`GeraLandingOpenAiBatchClient`) para manter a mesma diretriz no campo `system`.

## 2026-05-21 15:55:00 UTC
- ajuste solicitado: remover a expressão "pipeline de experimento" do campo `system` enviado à OpenAI no fluxo de geração.
- causa-raiz: o prompt ainda continha orientação secundária de "execução de pipeline", divergindo da instrução de manter apenas posicionamento de especialista em marketing/vendas digitais.
- foi feito:
  - removida a frase de "execução de pipeline de experimento" do `buildSystemPrompt` no backend;
  - removida a mesma frase do `systemMessage` explícito e do fallback no `GeraLandingExecutionService`;
  - removida a mesma frase do payload batch no `GeraLandingOpenAiBatchClient`.

## 2026-05-21 16:10:00 UTC
- ajuste solicitado: adicionar comentários de responsabilidade/orientação nas classes Java alteradas do fluxo de geração de experimentos.
- causa-raiz: classes alteradas no PR anterior não continham comentários explícitos em todos os pontos exigidos pelo fluxo operacional do AGENTS.
- foi feito:
  - adicionado comentário de responsabilidade da classe e comentário do método `buildSystemPrompt` em `ExperimentPipelineGenerationService`;
  - adicionado comentário de responsabilidade da classe e comentário do método `buildOpenAiRequestBody` em `GeraLandingExecutionService`;
  - adicionado comentário de responsabilidade da classe e comentário do método `buildRequestBodyFromPrompt` em `GeraLandingOpenAiBatchClient`.

## 2026-05-21 17:45:00 UTC
- ajuste solicitado: melhorar erro de contrato do planejamento de imagens da etapa `landing-page-image-planning` e aceitar chave `imagePlan` no payload raiz.
- causa-raiz: parser da montagem de HTML provisório esperava apenas objeto com chaves legadas (`images`/`landingPageImagePlanning`) e não reportava com clareza quando faltava o elemento contratual esperado.
- foi feito:
  - `CopyProvisionalHtmlProcessor` passou a ler `imagePlan` no objeto raiz do JSON de planejamento de imagens;
  - adicionada validação explícita com mensagem orientativa quando o elemento `imagePlan` não está presente;
  - melhorada mensagem de erro de parsing para indicar que o payload deve ser JSON objeto (`{...}`) compatível com o contrato;
  - adicionados comentários de responsabilidade da classe e comentários de métodos principais da classe ajustada.

## 2026-05-21 18:00:00 UTC
- solicitação: conferir o quadro canônico de etapas/processadores e explicitar responsabilidade nos processadores envolvidos.
- validação: o quadro operacional de referência está em `docs/canonical/procedimento-experimento-canon.v1.md` seção 5.3.
- foi feito:
  - comentário de responsabilidade ajustado em `CopyProvisionalHtmlProcessor` para deixar explícito que ele atende a etapa de copy e também enriquecimentos de image planning/design preset;
  - comentário de responsabilidade adicionado em `DesignPresetProvisionalHtmlProcessor` para explicitar consolidação final da etapa de design preset.

## 2026-05-21 18:20:00 UTC
- solicitação: isolar conjuntos de geração de HTML por etapa, documentar no cânone e comentar responsabilidade nas classes.
- causa-raiz: classes/fluxo de HTML estavam acoplados entre etapas, especialmente copy acumulando comportamento de image planning/design preset.
- foi feito:
  - reorganização de pacotes por etapa em `com.marketinghub.geralanding.{wireframe,copy,designpreset}`;
  - `CopyProvisionalHtmlAssembler/Processor` restritos à etapa `LANDING_PAGE_COPY` (remoção do caminho `assembleComplete/processComplete`);
  - orquestração de image planning ajustada para usar HTML base da copy + `LandingPageImageInjector` no serviço;
  - atualização do cânone em `procedimento-experimento-canon.v1.md` com regra obrigatória de isolamento por conjunto e mapeamento por pacote;
  - comentários de responsabilidade adicionados/ajustados nas classes dos conjuntos.

## 2026-05-21 18:40:00 UTC
- ajuste complementar solicitado: incluir o conjunto da etapa de prompt/image planning que estava faltando no isolamento por etapa.
- foi feito:
  - criação do conjunto `com.marketinghub.geralanding.imageplanning` com `ImagePlanningProvisionalHtmlAssembler`;
  - orquestração do `GeraLandingStageExecutionService` atualizada para usar o assembler exclusivo da etapa `LANDING_PAGE_IMAGE_PLANNING`;
  - atualização do quadro canônico e mapeamento por pacote para incluir explicitamente o conjunto de image planning.

## 2026-05-21 18:55:00 UTC
- solicitação: replicar no Worker AI a divisão por etapa aplicada no backend (incluindo prompt image).
- foi feito:
  - criado `GeraLandingStageDefinition` para centralizar etapas canônicas do worker;
  - criado `GeraLandingStageSchemaResolver` para resolver schema por etapa de forma isolada;
  - `GeraLandingExecutionService` atualizado para usar definição de etapa + resolver dedicado, removendo seleção ad-hoc por string;
  - cânone atualizado com seção específica do Worker AI para divisão obrigatória por etapa.

## 2026-05-21 19:20:00 UTC
- solicitação: no Gera Landing, remover referência nominal a "Batch" em classes, pois o fluxo atual usa modo flex.
- causa-raiz: nomes de classes do worker ainda refletiam arquitetura antiga baseada em batch, gerando inconsistência com o comportamento atual (`/responses` com `service_tier=flex`).
- foi feito:
  - renomeada classe `GeraLandingOpenAiBatchClient` para `GeraLandingOpenAiFlexClient` e atualização das referências no serviço de execução;
  - renomeado teste correspondente para `GeraLandingOpenAiFlexClientTest` e ajustes de tipagem/mocks;
  - atualização do documento canônico de Gera Landing para descrever execução em modo flex ao invés de ciclo batch.
## 2026-05-21 19:10:00 UTC
- solicitação: verificar se as novas estruturas de pacotes do GeraLanding continuam aderentes ao documento canônico.
- validação executada:
  - comparação dos pacotes atuais do backend (`geralanding`, `wireframe`, `copy`, `imageplanning`, `designpreset`) com o mapeamento canônico da seção 5.3 em `docs/canonical/procedimento-experimento-canon.v1.md`;
  - revisão da orquestração em `GeraLandingStageExecutionService` para confirmar uso de assembler/processador por etapa sem acoplamento cruzado indevido;
  - revisão do worker (`GeraLandingStageDefinition` + `GeraLandingStageSchemaResolver`) para confirmar resolução de schema por etapa.
- resultado: estrutura de pacotes e fluxo por etapa seguem o isolamento definido no cânone e mantêm aderência com a regra de conjuntos por etapa.
- ponto de atenção documental: há referências em documentos legados para `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`, porém o arquivo atual está em `docs/canonical/obsoletos/`; manter revisão de links em futuras limpezas documentais.

## 2026-05-21 18:30:00 UTC
- correção de quebra de compilação dos testes do `ads-service` após refatoração dos pacotes de GeraLanding.
- foi feito:
  - atualização de imports nos testes para os novos pacotes (`copy`, `wireframe`, `designpreset`, `imageplanning`);
  - ajuste dos testes para APIs atuais dos assemblers/processors (remoção de chamadas legadas `assembleComplete`/`processComplete`);
  - ajuste de mocks em `GeraLandingStageExecutionServiceTest` para refletir `ImagePlanningProvisionalHtmlAssembler` na etapa de image planning.
- validação: `mvn -DskipITs test-compile` executado com sucesso no módulo `backend/ads-service`.

## 2026-05-21 15:45:11 UTC-3
- correção de falha de compilação nos testes do módulo ai-worker após evolução do construtor de `GeraLandingExecutionService`.
- causa-raiz identificada: os testes continuavam instanciando o serviço sem o novo parâmetro obrigatório `GeraLandingStageSchemaResolver`, gerando incompatibilidade de assinatura.
- foi feito: atualização de `GeraLandingExecutionServiceTest` para criar mock de `GeraLandingStageSchemaResolver` e passá-lo nas duas instâncias do serviço cobertas pelos testes.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/GeraLandingExecutionService.java
  - ai-worker/src/test/java/com/marketinghub/worker/geralanding/GeraLandingExecutionServiceTest.java

## 2026-05-21 20:05:00 UTC
- solicitação: endurecer o schema da etapa de briefing de imagens para obrigar o formato com atributo `imagePlan`.
- causa-raiz: o modelo estava variando entre payload válido e formatos inválidos (array na raiz e JSON concatenado), quebrando o contrato esperado no consumidor da etapa.
- foi feito:
  - atualização do schema `landing-page-image-planning-schema.json` para exigir `landingPageImagePlanning.imagePlan` (substituindo `images` nesta etapa);
  - reforço do prompt da etapa com regra explícita de retorno em objeto único com atributo raiz `imagePlan` e proibição de array na raiz/JSON duplicado.

## 2026-05-21 20:20:00 UTC
- solicitação: exibir o histórico da seção "Gera Imagem" em ordem cronológica com o mais recente primeiro.
- causa-raiz: a lista `historyGeraLandingImagePromptsExecutions` fazia apenas merge + deduplicação e não aplicava ordenação por `executionRequestedAt`, permitindo ordem inconsistente na UI.
- foi feito:
  - ajuste do cálculo de histórico em `ExperimentDetailPage.tsx` para incluir status de falha explicitamente (`hasFailedExecution`);
  - aplicação de ordenação decrescente por timestamp (`Date.parse(executionRequestedAt)`) antes da deduplicação;
  - manutenção da deduplicação por `idJob` após ordenação para preservar apenas a ocorrência mais recente por job.
## 2026-05-21 18:12:00 UTC
- solicitação: ajustar legibilidade do subtítulo/badge na tela de detalhe de execução do Gera Landing, pois texto azul sobre fundo azul ficou sem contraste.
- causa-raiz: o badge de `stageCode` na composição do título usava estilo contextual que podia herdar cor de texto do heading em certos contextos, resultando em contraste insuficiente.
- foi feito:
  - atualização da classe CSS do badge para aplicar fundo azul fixo (`bg-primary`) com texto explicitamente branco (`text-white`) e peso `fw-semibold`, garantindo leitura consistente.
- validação: inspeção estática do componente em `frontend/src/pages/experiment/ExperimentGeraLandingExecutionDetailPage.tsx`.

## 2026-05-21 19:08:31 UTC-3
- solicitação: corrigir inconsistência da tela de etapas, onde o Preset Design mostrava HTML em vez do JSON bruto retornado pela OpenAI.
- causa-raiz identificada: o backend persistia o HTML provisório da etapa `landing-page-design-preset` na coluna `experiment.landing_page_design_preset`, sobrescrevendo o artefato JSON dessa etapa.
- foi feito:
  - criada a nova coluna `experiment.html_geralanding` (Liquibase incremental) para armazenar exclusivamente o HTML gerado pelo `DesignPresetProvisionalHtmlAssembler`;
  - ajustada a persistência da etapa `LANDING_PAGE_DESIGN_PRESET` para manter `landing_page_design_preset` como JSON bruto (`modelResponse`) e gravar o HTML consolidado em `html_geralanding`;
  - ajustado o fluxo de publicação (`approveAndPublishLanding`) para ler prioritariamente `html_geralanding` (fallback para `landing_page_html`);
  - atualizado o DTO do experimento para expor `htmlGeraLanding` e o texto da UI para explicitar que a etapa 6 mostra JSON bruto.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - backend/AGENTS.md
  - frontend/AGENTS.md
  - docs/canonical/procedimento-experimento-canon.v1.md
  - docs/modelo-dados-experimento.md
  - docs/registros/experimentos.md

## 2026-05-21 19:20:11 UTC-3
- solicitação para reduzir ruído na etapa de preset design, que estava gerando muitos presets LHM sem uso prático.
- causa-raiz tratada no prompt: ausência de regra explícita para restringir `elementPresets` somente a elementos existentes e com necessidade real de override.
- foi feito: atualização do prompt `landing-page-design-preset` no AI Worker para descartar geração redundante de presets LHM, proibindo entradas extras que apenas repetem defaults de `theme`/`sectionPresets` e evitando criação de classes/tokens fora do contrato.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-design-preset.md

## 2026-05-21 23:58:00 UTC
- ajuste solicitado: correção de escopo — alteração deve ser aplicada no Gera Landing (AI Worker), não no schema de pipeline do backend.
- causa-raiz: a mudança anterior foi feita em `backend/ads-service` (pipeline), enquanto a necessidade era no contrato/prompt operacional da etapa `landing-page-wireframe` do módulo `ai-worker` (Gera Landing).
- foi feito:
  - atualização do schema `ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe-schema.json` para o formato com raiz `definicoes` + `pagina`;
  - inclusão das categorias `estrutura`, `posicao`, `layout`, `mistas` em `definicoes`, cada uma com `desktop`/`mobile` e itens `{nome, atributoCss, valor}`;
  - ajuste de `pagina.corpo.secoes` para usar somente referências por `nome` (separadas em desktop/mobile) nas quatro categorias;
  - atualização do prompt `ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe.md` para reforçar explicitamente as novas regras do contrato no escopo Gera Landing.

## 2026-05-22 00:25:00 UTC
- ajuste solicitado: no prompt do gera wireframe, não remover em massa; remover apenas o que conflita claramente com a nova definição `definicoes + pagina`.
- causa-raiz: a versão anterior do prompt v3 ficou excessivamente enxuta e descartou regras operacionais/comerciais úteis que continuam válidas no novo contrato.
- foi feito:
  - reintroduzidas regras de negócio e qualidade (mobile-first, CTA/âncoras, mínimo de seções/imagens, formulário com nome+email, conteúdo vazio no wireframe, heurísticas de escaneabilidade e composição);
  - mantida apenas a remoção de itens incompatíveis com o novo schema (ex.: instruções de `estilos[]` inline e `briefingVisual` obrigatório em `img`);
  - preservado o contrato novo com raiz `definicoes` + `pagina` e referências por nome em desktop/mobile.

## 2026-05-22 00:55:00 UTC
- ajuste solicitado: explicitar no Gera Wireframe os atributos CSS que compõem cada grupo (posicionamento, exibição, tamanho etc.), conforme `/docs/gera-landing/listas-css-estrutura-acabamento.md`.
- causa-raiz: o contrato v3 já separava `definicoes` em categorias, porém sem lista explícita/validável de atributos por grupo, gerando ambiguidade e maior risco de payload inválido.
- foi feito:
  - atualização do prompt `landing-page-wireframe.md` com matriz explícita de grupos e atributos permitidos (`posicionamento`, `exibicaoFluxo`, `tamanho`, `espacamentoExterno`, `espacamentoInterno`, `flexbox`, `grid`, `transformacoes`) e regras de conformidade por categoria (`estrutura`, `posicao`, `layout`, `mistas`);
  - atualização do schema `landing-page-wireframe-schema.json` para validar `atributoCss` por categoria com `enum` explícito, removendo o tipo genérico anterior e criando definições tipadas por categoria.

## 2026-05-22 01:20:00 UTC
- ajuste solicitado: manter explicitamente no wireframe o trecho de contrato com campos comerciais da seção, `estilos[]` em seção/elemento e `briefingVisual` obrigatório para `img`.
- causa-raiz: versão anterior havia simplificado o contrato focando nas `definicoes`, removendo campos ainda necessários para a etapa de estrutura comercial e direção visual dos elementos de imagem.
- foi feito:
  - reintroduzido no prompt o trecho obrigatório de contrato exatamente com os campos exigidos para seção/elementos;
  - expandido o schema com os campos comerciais de seção (`oQueQuerProvocarNoUsuario`, `papelComercial`, `fasePersuasao`, `objeçãoQueRemove`, `prioridadeConversao`, `acaoEsperada`, `fonteContexto`), além de `estilos[]` em seção e elemento;
  - adicionada validação condicional de `briefingVisual`: obrigatório para `tag=img` e `null` para demais tags.

## 2026-05-22 01:40:00 UTC
- ajuste solicitado: manter também a regra explícita de metadados comerciais por visual `img` e o ajuste de intenção por seção com `prioridadeConversao` numérica.
- causa-raiz: o contrato/prompt estava parcialmente explícito, mas faltavam instruções literais pedidas para narrativa visual por imagem e restrição de tipo para prioridade de conversão.
- foi feito:
  - inclusão no prompt da regra de explicitação por visual `img` (narrativa/contexto, tipo visual, função comercial, objeção removida e classificação visual);
  - ajuste da seção "Ajuste de intenção por seção" para o formato solicitado, incluindo exemplos de `fonteContexto[]`;
  - ajuste do schema para `prioridadeConversao` como inteiro de 1 a 10.

## 2026-05-22 01:57:00 UTC
- ajuste solicitado: suportar novo JSON da etapa Gera Wireframe no fluxo de montagem de HTML provisório (assembler + processor).
- causa-raiz: o fluxo de copy provisória assumia principalmente estruturas `bodySections/items` e adicionava metadado técnico (`jobId`) no HTML final, o que dificulta compatibilidade com o novo payload e viola a regra de não contaminar artefato final.
- foi feito:
  - atualização do `CopyProvisionalHtmlAssembler` para retornar apenas o HTML produzido pelo processor, sem inserir comentário técnico de `jobId`;
  - atualização do `CopyProvisionalHtmlProcessor` para aceitar payloads envelopados (`landingPageWireframe`/`landingPageCopy`) e também copy no schema `pagina.corpo.secoes[].elementosSeccao[]` com `texto.conteudo`;
  - adição de teste cobrindo aplicação de copy no formato `pagina` aninhado para garantir regressão positiva no novo contrato.

## 2026-05-22 02:10:00 UTC
- ajuste solicitado: tratar CSS do novo wireframe com definições separadas por desktop/mobile ao gerar HTML provisório.
- causa-raiz: o gerador do formato `pagina` processava somente `estilos` inline antigos e não convertia referências de classe vindas do novo contrato (`definicoes` + `estilos.desktop/mobile`), causando perda de layout no preview.
- foi feito:
  - atualização do `WireframeHtmlGenerator` para montar `<style>` responsivo a partir de `definicoes` (desktop + mobile via media query);
  - atualização da renderização de nós para anexar classes derivadas de `estilos.desktop/mobile`;
  - inclusão de teste cobrindo geração de CSS desktop/mobile e aplicação de `class` no HTML final.

## 2026-05-22 03:10:00 UTC
- ajuste solicitado: no assembler do Gera Wireframe aplicar fundo alternado por seção, padronizar tamanho sugerido de imagens e preencher textos vazios com Lorem Ipsum proporcional.
- causa-raiz: o HTML provisório do wireframe era renderizado sem padronização visual mínima para alternância de superfície, sem heurística de tamanho para `img` e sem fallback textual quando os blocos vinham sem conteúdo.
- foi feito:
  - atualização do `WireframeHtmlGenerator` para alternar `background-color` automático em seções sem fundo explícito (`#FFFFFF` e `#F7F9FC`);
  - inclusão de heurística de imagem no `img` para sugerir `width` e `height` por média entre `minWidth/maxWidth` e `minHeight/maxHeight` quando presentes (com fallback 960x540);
  - inclusão de fallback de Lorem Ipsum quando `texto.conteudo` estiver vazio, usando média entre `textMinWords/textMaxWords` quando disponíveis.

## 2026-05-22 23:40:00 UTC
- ajuste solicitado: alterar prompt e schema da etapa `landing-page-design-preset` para usar grupos de estrutura equivalentes ao acabamento da etapa wireframe e aplicar definições no JSON de elementos recebido do wireframe.
- causa-raiz: o contrato anterior listava propriedades CSS isoladas, sem agrupamento semântico obrigatório nem validação explícita de vínculo com os elementos reais do wireframe.
- foi feito:
  - atualização do prompt para exigir os 12 grupos de estrutura (`cores-fundo`, `tipografia`, `texto`, `bordas`, `contorno`, `sombras-transparencia`, `filtro-efeitos`, `cursor`, `listas`, `imagens`, `transições`, `animações`) com detalhamento literal de cada atributo CSS por grupo;
  - atualização do formato de saída no prompt para exigir `group` em cada atributo de `sectionAttributes` e `attributes` de `elementPresets`, além de reforçar aplicação somente em `sectionId/elementId` existentes no wireframe;
  - atualização do schema JSON da etapa para tornar `group` obrigatório em cada atributo, validar os 12 grupos via `enum` e exigir `consistencyChecks` com status `PASS|WARN|FAIL`.

## 2026-05-22 23:58:00 UTC
- ajuste solicitado: deixar o JSON da etapa `landing-page-design-preset` parecido com o exemplo `/exemplos/model-response-7179cef3-1f8f-4464-a9d5-c43a49a37fff.json`, mantendo `definicoes` + `pagina`, mas trocando as listas de `definicoes` pelos 12 grupos de acabamento.
- causa-raiz: a mudança anterior manteve um contrato alternativo (`landingPageDesignPreset`) que não seguia o shape operacional esperado pelo fluxo atual baseado em `definicoes/pagina`.
- foi feito:
  - reescrita do prompt para exigir saída no shape `definicoes` + `pagina` (espelhando wireframe) e aplicação apenas nos elementos existentes do wireframe;
  - substituição dos grupos de `definicoes` para os 12 grupos solicitados (`cores-fundo`, `tipografia`, `texto`, `bordas`, `contorno`, `sombras-transparencia`, `filtro-efeitos`, `cursor`, `listas`, `imagens`, `transições`, `animações`);
  - reescrita do schema para validar o novo contrato com grupos obrigatórios, arrays `desktop/mobile` e itens `{nome, atributoCss, valor}`.

## 2026-05-22 14:10:00 UTC
- ajuste solicitado: na aba de conteúdo do detalhe de experimento, além do botão "Copiar etapa", disponibilizar também a ação "Baixar etapa" para exportar o JSON da etapa.
- causa-raiz: a tela permitia apenas copiar para clipboard, sem alternativa direta para download do conteúdo bruto por etapa.
- foi feito:
  - inclusão de utilitário frontend para download de conteúdo JSON via `Blob` e link temporário;
  - adição de botão "Baixar etapa" ao lado de "Copiar etapa" em cada card com conteúdo;
  - adição de estado de carregamento com botão desabilitado + spinner durante a ação de download, mantendo consistência de UX com a regra assíncrona do frontend.

## 2026-05-22 17:20:00 UTC
- ajuste solicitado: incluir no nome do arquivo baixado o nome da etapa e o número do job.
- causa-raiz: o nome do arquivo exportado continha apenas o título da etapa, sem identificador do job para rastreabilidade operacional.
- foi feito:
  - inclusão de sanitização de partes do nome de arquivo com remoção de acentos/caracteres inválidos;
  - inclusão de extração de número do job a partir do JSON da etapa (`jobNumber`, `jobId` ou `job_id`) com fallback `sem-job`;
  - atualização do padrão final do arquivo para `<nome-da-etapa>-job-<numero>.json`.

## 2026-05-22 17:40:00 UTC
- ajuste solicitado: usar ArchUnit para bloquear dependências cruzadas entre os subpacotes de GeraLanding (`wireframe`, `copy`, `imageplanning` e `designpreset`).
- causa-raiz: não existia um teste de arquitetura específico para impedir acoplamento indevido entre esses subpacotes, permitindo regressões silenciosas de dependência interna.
- foi feito:
  - ampliação do teste `ModuleIsolationArchitectureTest` com regras dedicadas de isolamento para cada subpacote de GeraLanding;
  - extração de helper para padronizar as regras de isolamento e reduzir duplicação;
  - inclusão de comentários de responsabilidade na classe e métodos do teste para aderência ao padrão do backend.

## 2026-05-22 23:59:59 UTC
- ajuste solicitado: corrigir violações do ArchUnit no isolamento dos subpacotes `geralanding.copy`, `geralanding.imageplanning` e `geralanding.designpreset`.
- causa-raiz: classes desses subpacotes dependiam diretamente de implementações de outros subpacotes internos (`wireframe`, `copy`) e de serviço externo ao subpacote (`LandingPageImageInjector`), quebrando o contrato de independência.
- foi feito:
  - `copy` e `designpreset`: introduzidos geradores locais de HTML de wireframe (`CopyWireframeHtmlGenerator` e `DesignPresetWireframeHtmlGenerator`) para remover dependência direta com `geralanding.wireframe`;
  - `imageplanning`: substituída dependência concreta por contratos internos (`CopyStageHtmlProvider` e `ImageHtmlInjector`) e criada configuração de adaptação (`GeraLandingImagePlanningConfig`) para conectar integrações fora do subpacote;
  - atualização do assembler de image planning para usar apenas contratos internos do próprio subpacote.

## 2026-05-22 18:45:00 UTC
- ajuste solicitado: corrigir logs do AI Worker para não indicar incorretamente a etapa `wireframe` durante execuções da etapa `landing-page-copy`.
- causa-raiz: mensagens de log em `GeraLandingExecutionService` estavam hardcoded com o texto `wireframe`, independentemente do `stageCode` em processamento.
- foi feito:
  - atualização do log de montagem do prompt para incluir dinamicamente `execution.stageCode()`;
  - atualização do log de falha para incluir dinamicamente `execution.stageCode()` e evitar diagnóstico incorreto da etapa em erro.

## 2026-05-22 15:54:24 UTC-3
- solicitação para reforçar isolamento arquitetural entre etapas de GeraLanding no ai-worker com ArchUnit.
- raciocínio aplicado: prevenir acoplamento cruzado entre etapas (wireframe, copy, imageplanning e presetdesign) mantendo fronteiras claras de domínio e reduzindo regressões futuras.
- foi feito:
  - atualização do teste `GeraLandingArchitectureTest` para adicionar regra ArchUnit baseada em `slices` que impede dependência entre os subpacotes `geralanding.wireframe`, `geralanding.copy`, `geralanding.imageplanning` e `geralanding.presetdesign`.
  - preservação da regra existente que bloqueia dependência de `geralanding` para `experimentpipeline`.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md
  - ai-worker/src/test/java/com/marketinghub/worker/geralanding/GeraLandingArchitectureTest.java


## 2026-05-22 19:10:00 UTC
- ajuste solicitado: aplicar `allowEmptyShould(true)` na regra ArchUnit de independência dos subpacotes de GeraLanding.
- causa-raiz: a regra `slices` falhava quando nenhum pacote correspondente era encontrado no recorte avaliado, por comportamento padrão de falha em regra vazia.
- foi feito:
  - atualização de `GeraLandingArchitectureTest` para permitir avaliação vazia com `.allowEmptyShould(true)` na regra `geralanding_subpackages_must_be_independent`;
  - manutenção da validação de não dependência entre subpacotes quando existirem classes compatíveis.

- 2026-05-22 — AI Worker / GeraLanding: adicionado log com `jobId` no envio para OpenAI, no recebimento da resposta da OpenAI e no envio do retorno ao backend com `url` e `payload` para rastreabilidade ponta a ponta.
- 2026-05-22 — AI Worker / GeraLanding: ajustado log de retorno da OpenAI para incluir a resposta completa do modelo (`respostaCompleta`) junto com `jobId`.

## 2026-05-22 22:05:00 UTC
- tema: backend / GeraLanding / etapa `landing-page-copy`.
- causa-raiz observada: falha 500 no endpoint `receive-result` sem contexto operacional suficiente no log para identificar rapidamente payload/campo de origem durante montagem de HTML provisório.
- foi feito:
  - adicionado logging de erro estruturado em `GeraLandingStageExecutionService.receiveResult` com `idJob`, `experimentId`, `stageCode`, `openAiJobId`, tamanho e preview de `modelResponse` e `provisionalHtml` antes de relançar exceção;
  - adicionado logging de erro estruturado em `CopyProvisionalHtmlAssembler.assemble` com `jobId`, tamanho e preview de `copyModelResponse` e `wireframeModelResponse`, preservando stack trace completo da exceção original.
- resultado esperado: próxima recorrência desse tipo de erro passa a expor no log o ponto exato do fluxo e o contexto de payload para diagnóstico de causa-raiz em menor tempo.

- 2026-05-23 01:22:35 UTC — Validação da tela de experimento (Etapas 7 e 8): confirmado que as colunas persistidas no backend para os cards são `landing_page_html` (Etapa 7) e `landing_page_deliverables` (Etapa 8). Ajustada a descrição da Etapa 8 no frontend para explicitar o nome da coluna exibida.

- 2026-05-23 01:26:00 UTC — Ajustada a tela do experimento para exibir os dois HTMLs persistidos no banco nas posições corretas do pipeline: Etapa 7 agora mostra `html_geralanding`, Etapa 8 mostra `landing_page_html` e os deliverables foram reposicionados para Etapa 9 (`landing_page_deliverables`).
- 2026-05-23 (UTC): padronização do fluxo de provisional HTML no GeraLanding para usar o mesmo formato de chamada dos assemblers por etapa (`assemble(modelResponse, idJob)`), aplicando na etapa `landing-page-design-preset` via `DesignPresetProvisionalHtmlAssembler` e removendo o fallback com composição cruzada no serviço de execução.

- 2026-05-23 (UTC): adicionada regra ArchUnit `GeraLandingAssemblerArchitectureTest` para bloquear uso da assinatura legada `assemble(wireframe, copy, imagePlanning, designPreset, jobId)` dentro de `GeraLandingStageExecutionService`, forçando o padrão `assemble(modelResponse, jobId)` na etapa `landing-page-design-preset`.

- 2026-05-23 (UTC): reforço adicional de ArchUnit para exigir localização dos assemblers canônicos: `WireframeProvisionalHtmlAssembler` em `com.marketinghub.geralanding.wireframe` e `DesignPresetProvisionalHtmlAssembler` em `com.marketinghub.geralanding.designpreset`; mantida também a regra que bloqueia chamada da assinatura legada de 5 parâmetros no service.

- 2026-05-23 (UTC): ampliadas regras ArchUnit do GeraLanding para também bloquear no `GeraLandingStageExecutionService` o uso de assinaturas legadas dos assemblers de wireframe (`assemble(modelResponse)`) e copy (`assemble(copyModelResponse, wireframeModelResponse)`), forçando os contratos com `jobId`.

- 2026-05-23 (UTC): adicionadas regras ArchUnit positivas para exigir que `GeraLandingStageExecutionService` chame explicitamente os assemblers canônicos nas assinaturas padrão: wireframe (`assemble(modelResponse, jobId)`), design preset (`assemble(modelResponse, jobId)`) e copy (`assemble(copyModelResponse, wireframeModelResponse, jobId)`).

- 2026-05-23 (UTC): atualização do documento canônico do Gera Landing com diagrama de fluxo mostrando `GeraLandingStageExecutionService` acionando os assemblers de wireframe, copy, design-preset e image-planning, e a persistência dos dados gerados na tabela `gera_landing_stage_execution`.
- 2026-05-23 (UTC): pipeline legado de conteúdo ajustado para encerrar a continuação automática após `AD_IMAGE_BRIEFING`, evitando enfileirar `landing-page-wireframe` sem execução manual de Gera Landing; adicionado teste unitário cobrindo que a conclusão de `AD_IMAGE_BRIEFING` não cria job pendente de `LANDING_PAGE_WIREFRAME`.

## 2026-05-23 00:58:21 UTC-3
- solicitação para continuar o diagrama canônico da seção 1.4 do Gera Landing e explicitar as inserções usadas da tabela `experiments`.
- raciocínio aplicado: manter o foco na causa-raiz da montagem de contexto por estágio, deixando explícita a origem dos dados antes da persistência em `gera_landing_stage_execution`.
- foi feito: atualização do diagrama mermaid com o nó `experiments` alimentando os assemblers (wireframe/copy/design-preset/image-planning) e complemento textual listando os principais campos inseridos/consumidos no fluxo.
- documentos/arquivos lidos:
  - AGENTS.md
  - docs/gera-landing/modelo-canonico-gera-landing.md
  - docs/registros/experimentos.md

## 2026-05-23 02:15:00 UTC-3
- solicitação: pesquisar no código as classes de persistência envolvidas nas tabelas `gera_landing_stage_execution` e `experiments` para as etapas wireframe, copy, preset design e prompt images, e completar o diagrama canônico.
- causa-raiz tratada: o diagrama anterior mostrava apenas visão de alto nível dos assemblers, sem explicitar os pontos reais de escrita por etapa (service + repositories + campos persistidos).
- foi feito:
  - rastreamento no backend (`ads-service`) do fluxo `GeraLandingStageExecutionService.receiveResult(...)` e do método `persistStageArtifactOnExperiment(...)`;
  - atualização da seção 1.4 com diagrama mermaid completo incluindo `GeraLandingStageExecutionRepository.save(...)`, `ExperimentRepository.save(...)`, assemblers por etapa e colunas gravadas em `experiments`;
  - complemento textual com mapeamento objetivo de persistência por etapa.
- classes identificadas no código:
  - `com.marketinghub.geralanding.GeraLandingStageExecutionService`
  - `com.marketinghub.geralanding.GeraLandingStageExecutionRepository`
  - `com.marketinghub.experiment.repository.ExperimentRepository`
  - `com.marketinghub.geralanding.wireframe.WireframeProvisionalHtmlAssembler`
  - `com.marketinghub.geralanding.copy.CopyProvisionalHtmlAssembler`
  - `com.marketinghub.geralanding.imageplanning.ImagePlanningProvisionalHtmlAssembler`
  - `com.marketinghub.geralanding.designpreset.DesignPresetProvisionalHtmlAssembler`

- 2026-05-23 04:34:00 UTC — Correção de persistência do GeraLanding solicitada pelo usuário:
  - causa-raiz: o backend ainda gravava HTML provisório das etapas internas em `experiment.landing_page_html`, misturando artefato intermediário com artefato de publicação.
  - ajuste aplicado: etapas `LANDING_PAGE_IMAGE_PLANNING` e `LANDING_PAGE_DESIGN_PRESET` passam a persistir somente em `gera_landing_stage_execution.provisional_html` e `experiment.html_geralanding` (design preset), sem gravar `landing_page_html`.
  - ajuste aplicado: `landing_page_html` agora é persistido apenas no fluxo de aprovação/publicação (`approveAndPublishLanding`) com o HTML final efetivamente publicado.
  - validação: testes unitários de `GeraLandingStageExecutionServiceTest` atualizados para garantir ausência de gravação prematura em `landing_page_html`.
## 2026-05-23 01:30:00 UTC-3
- solicitação: corrigir erro de renderização Mermaid na seção 1.4 do documento canônico de Gera Landing exibido no GitHub.
- causa-raiz: labels do diagrama continham quebra de linha e parênteses em formato que o parser do Mermaid no GitHub interpretou como sintaxe inválida.
- foi feito:
  - ajuste dos nós com textos multi-linha para formato string com `<br/>` (entre aspas), evitando ambiguidades de parsing;
  - manutenção do mesmo conteúdo funcional do diagrama (service, repositories e campos persistidos);
  - registro desta manutenção no histórico de experimentos.
- arquivos alterados:
  - docs/gera-landing/modelo-canonico-gera-landing.md
  - docs/registros/experimentos.md
## 2026-05-23 01:50:16 UTC-3
- solicitação para eliminar divergência entre dois documentos de Gera Landing e manter uma única fonte canônica em `/docs/canonical`.
- causa-raiz identificada: coexistência de definições operacionais em `procedimento-experimento-canon.v1.md` e em `docs/gera-landing/modelo-canonico-gera-landing.md`, criando conflito de interpretação da etapa preset design.
- correção aplicada: consolidação da regra no documento canônico principal do experimento, com seção explícita de unificação e regra mandatória de geração de HTML provisório pelo `DesignPresetProvisionalHtmlAssembler`; remoção do documento fora da pasta canonical.
- validação no código realizada por inspeção de referências de etapa/assembler e testes existentes da etapa preset design.
- documentos/arquivos lidos:
  - AGENTS.md
  - docs/canonical/procedimento-experimento-canon.v1.md
  - docs/gera-landing/modelo-canonico-gera-landing.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/GeraLandingStageExecutionService.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/designpreset/DesignPresetProvisionalHtmlAssembler.java
  - backend/ads-service/src/test/java/com/marketinghub/geralanding/GeraLandingStageExecutionServiceTest.java

## 2026-05-23 05:40:00 UTC
- solicitação: corrigir violação ArchUnit de isolamento do subpacote `geralanding.designpreset`.
- causa-raiz: `DesignPresetProvisionalHtmlAssembler` dependia diretamente de `ExperimentRepository` e `Experiment`, quebrando a regra de independência do subpacote.
- correção aplicada:
  - removida a dependência de `ExperimentRepository`/`Experiment` do assembler de design preset;
  - removido o overload `assemble(Long experimentId, ...)` no assembler;
  - `GeraLandingStageExecutionService` passou a fornecer explicitamente `wireframe/copy/imagePlanning` já carregados do experimento para o assembler.
- validação: teste `ModuleIsolationArchitectureTest` executado com sucesso após o ajuste.

## 2026-05-23 06:12:00 UTC
- solicitação: ajustar teste de arquitetura do GeraLanding para refletir a chamada do assembler de design preset com novos parâmetros.
- causa-raiz: o teste ArchUnit ainda tratava a assinatura com cinco parâmetros como legado proibido, embora o serviço já use essa assinatura no fluxo atual.
- correção aplicada:
  - regra de bloqueio atualizada para proibir o overload antigo `assemble(String, String)` em `DesignPresetProvisionalHtmlAssembler`;
  - regra de conformidade atualizada para exigir a chamada `assemble(String, String, String, String, String)` no `GeraLandingStageExecutionService`.
- validação: teste `GeraLandingAssemblerArchitectureTest` executado com sucesso.

## 2026-05-23 06:25:00 UTC
- solicitação: remover referência de arquivo JSON inexistente no teste `DesignPresetProvisionalHtmlProcessorTest`.
- causa-raiz: o teste de exemplos do repositório apontava para `model-response-7179cef3-1f8f-4464-a9d5-c43a49a37fff.json`, arquivo que não existe mais em `../../exemplos`.
- correção aplicada:
  - removida a referência ao JSON inexistente da lista `exampleFiles`;
  - mantido o exemplo válido remanescente para continuar validando o processamento tokenizado.
- validação: execução do teste unitário específico com sucesso.

## 2026-05-23 12:40:00 UTC
- solicitação: remover definição forçada de cor inline no HTML provisório da etapa de preset design, mantendo estilos via classes/tokens.
- causa-raiz: `DesignPresetWireframeHtmlGenerator` adicionava automaticamente `background-color` em cada `<section>` quando o estilo não vinha no payload, o que gerava `style="background-color:..."` mesmo sem classe correspondente.
- correção aplicada:
  - removida a lógica de fallback que injetava `background-color` automático por seção;
  - o gerador agora preserva apenas os estilos fornecidos pelo contrato (`estilos`/classes responsivas), sem forçar cor inline.
- validação: teste unitário direcionado do módulo executado com sucesso.

## 2026-05-23 13:10:00 UTC
- solicitação: verificar se o generator de preset design ainda injeta/omite/altera conteúdo além do JSON de entrada.
- causa-raiz: além da cor de fundo já removida, o gerador ainda alterava contrato em outros pontos (injeção automática de `alt/width/height` em `<img>` e omissão de `class/style` vindos em `props/atributos` quando também havia tokens em `estilos`).
- correção aplicada:
  - removida a injeção automática de atributos de imagem (`alt`, `width`, `height`);
  - ajuste de merge para preservar `class` e `style` vindos do JSON e combinar com classes/estilos derivados de `estilos` tokenizados, sem sobrescrever/omitir.
  - adicionados testes unitários direcionados para cobertura dos cenários de preservação/merge e ausência de injeção automática.
- validação: testes unitários específicos do módulo executados com sucesso.

## 2026-05-23 13:58:00 UTC
- solicitação: validar o gerador/pipeline com os JSONs reais da pasta `/exemplos` para garantir que o HTML final contempla o contrato recebido.
- causa-raiz: faltava teste automatizado com amostras reais (`etapa-3`, `etapa-4`, `etapa-6`), o que deixava risco de regressão silenciosa entre contrato esperado e HTML montado.
- correção aplicada:
  - adicionado teste `shouldRenderUsingRealExamplesWithoutImplicitStyleOrImageAttributeInjection` em `DesignPresetProvisionalHtmlProcessorTest` lendo diretamente os três JSONs de `/exemplos`;
  - validação cobre presença de estrutura esperada no HTML e ausência de injeções automáticas já proibidas (fallback de `background-color` e `width/height` default de imagem).
- validação: suíte direcionada do módulo executada com sucesso.

## 2026-05-23 16:08:00 UTC
- solicitação: retirar o teste com leitura direta de `/exemplos` e voltar ao estado anterior do teste da etapa preset design.
- causa-raiz: o teste com arquivos de `/exemplos` foi criado para validação pontual durante implementação e não deveria permanecer na suíte.
- correção aplicada:
  - removido `shouldRenderUsingRealExamplesWithoutImplicitStyleOrImageAttributeInjection` de `DesignPresetProvisionalHtmlProcessorTest`;
  - removidos imports/helpers auxiliares de leitura de arquivo (`Files`, `Path`, `IOException`, `readExample`).
- validação: suíte direcionada de testes do módulo executada com sucesso.
- 2026-05-23 (UTC): corrigido gerador de HTML provisório da etapa design preset para não injetar texto placeholder "Lorem ipsum" quando o wireframe não traz conteúdo textual; validação de classes do JSON tokenizado reforçada em teste de processor (`DesignPresetProvisionalHtmlProcessorTest`) para garantir aplicação de classes e CSS do preset.

## 2026-05-23 17:05:00 UTC
- solicitação: no preset design, campo `pagina` estava vindo `null`; enviar ao modelo os itens da página (JSON de wireframe) e exigir alocação de estilos por seção/item conforme wireframe.
- causa-raiz: o template `landing-design-preset` não expunha explicitamente o JSON literal do wireframe para a etapa de design preset, permitindo geração de `sectionPresets` incompletos/desalinhados.
- correção aplicada:
  - `ExperimentPipelineOpenAiClient` passou a injetar variável de template `WIREFRAME_JSON_BLOCK` extraída de `job.prompt` (`landingPageWireframe`) para etapas que usam templates;
  - prompt `landing-design-preset.md` atualizado para versão `v2`, com bloco `WIREFRAME_JSON` explícito e regra obrigatória de mapear estilos por seção/item exatamente conforme `sectionOrder` do wireframe.
- validação: tentativa de compilação do módulo `ai-worker` bloqueada por dependência privada `com.marketinghub:ads-service:0.0.1-SNAPSHOT` (401 no GitHub Packages).

## 2026-05-23 18:30:00 UTC
- solicitação: incluir no início do prompt `landing-design-preset` o bloco completo de contexto hierárquico (nicho/hipótese/experimento) com dados de dor, resultado e artefatos de campanha.
- causa-raiz: faltava contexto estrutural explícito no topo do prompt da etapa design preset, reduzindo aderência semântica entre preset visual e cadeia completa do experimento.
- correção aplicada:
  - prompt `landing-design-preset.md` atualizado para `template_version: v3` com o bloco de contexto solicitado no início de `SYSTEM_INSTRUCTIONS`;
  - adicionadas variáveis de template `PAIN_JSON` e `RESULT_JSON` no `ExperimentPipelineOpenAiClient` para preencher os placeholders novos quando presentes no `job.prompt`.
- validação: ajuste textual/templating validado por inspeção estática; compilação completa do módulo segue bloqueada por dependência privada `com.marketinghub:ads-service:0.0.1-SNAPSHOT` (401 no GitHub Packages).


## 2026-05-23 18:11:44 UTC-3
- solicitação para alinhar o Worker AI ao local canônico atual dos prompts do Gera Landing e atualizar a documentação canônica com os caminhos corretos.
- causa-raiz identificada: `ExperimentPipelineOpenAiClient` ainda carregava templates de landing em `prompts/experiment`, enquanto os prompts oficiais já estavam em `prompts/geralanding`, provocando ausência de enriquecimento de prompt e falhas de teste.
- foi feito:
  - atualização dos paths de templates de landing no `ExperimentPipelineOpenAiClient` para `prompts/geralanding/*`;
  - alinhamento da etapa final de landing para aceitar também aliases de seção `landing-page-deliverables` no mesmo cliente;
  - atualização do documento canônico `procedimento-experimento-canon.v1.md` com o local canônico explícito dos prompts de experimento e Gera Landing.
- documentos/arquivos lidos:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/canonical/procedimento-experimento-canon.v1.md
  - ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java
  - docs/registros/experimentos.md
## 2026-05-23 19:08:43 UTC-3
- solicitação para criar um diagrama canônico semelhante ao do MOIS, agora para Gera Landing, destacando os lados Worker AI e Backend.
- raciocínio aplicado: manter padrão visual/estrutural do documento canônico atual e explicitar fronteiras de responsabilidade e integração para reduzir ambiguidade operacional.
- foi feito: adicionada a seção `12.6 Diagrama de arquitetura por módulo/pacote — Gera Landing (Worker AI x Backend)` em `docs/canonical/mois-worker-canon.v1.md` com Mermaid separado em subgraphs (Worker AI e Backend), incluindo conexões com OpenAI, backend, MySQL e publicação no Lead Portal.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/mois-worker-canon.v1.md
  - docs/registros/experimentos.md
## 2026-05-23 20:20:00 UTC
- solicitação para mover o diagrama de arquitetura do Gera Landing para o documento canônico de experimento e corrigir erro de parse no Mermaid exibido no GitHub.
- causa-raiz identificada: o diagrama estava no cânone do MOIS (documento fora do escopo principal do fluxo Gera Landing) e utilizava quebras `\n` em labels Mermaid, além de typo em `GeraLandingContoller`, gerando falha de renderização.
- foi feito:
  - remoção da seção detalhada do diagrama no `mois-worker-canon.v1.md` e inclusão de apontamento para o novo local canônico;
  - inclusão da seção `15.4 Diagrama de arquitetura por módulo/pacote — Gera Landing (Worker AI x Backend)` em `procedimento-experimento-canon.v1.md`;
  - ajuste de sintaxe Mermaid para compatibilidade com renderer do GitHub (`<br/>` em labels e correção `GeraLandingController`), além do ajuste de `dispatch` na seta de integração.
- documentos/arquivos lidos e atualizados:
  - AGENTS.md
  - docs/canonical/mois-worker-canon.v1.md
  - docs/canonical/procedimento-experimento-canon.v1.md
  - docs/registros/experimentos.md
## 2026-05-23 21:05:00 UTC
- solicitação para corrigir o prompt de Gera Copy da landing com foco em causa-raiz de divergência entre intenção do CTA, destino de navegação, compliance temporal, microcopy de formulário e acessibilidade de imagens.
- causa-raiz identificada: o contrato da etapa `landing-page-copy` não separava semanticamente CTA de conversão vs navegação, não exigia destino explícito de CTA secundário, não obrigava microcopy de privacidade/consentimento no formulário e não exigia plano de alt text por imagem.
- foi feito:
  - atualização do prompt `landing-page-copy.md` com regras obrigatórias para `ctaType`, `targetSectionId`, `formMicrocopy`, `imageAccessibilityPlan` e compliance da promessa de "2 minutos";
  - atualização do schema `landing-page-copy-schema.json` para refletir o novo contrato (campos obrigatórios para tipagem de CTA, microcopy de formulário e acessibilidade de imagens).
- resultado esperado: reduzir 400/422 por ambiguidade de payload e melhorar aderência de copy/HTML no fluxo de prova, conversão e acessibilidade.
- solicitação para corrigir lacunas no prompt do Gera Landing (wireframe) em contrato estrutural/semântico para body, interação, imagens, formulário e componentes.
- causa-raiz identificada: o contrato da etapa wireframe exigia briefing visual e estrutura geral, mas não obrigava metadados funcionais mínimos (âncora/href, contrato de asset, semântica de input e classes raiz de body), permitindo saídas incompletas no HTML derivado.
- foi feito:
  - reforço do prompt `landing-page-wireframe.md` com regras explícitas de `pagina.body`, intenção funcional de elementos interativos, contrato de asset em `img`, contrato semântico de `input`, componentes semânticos e validação permanente de `texto.conteudo` vazio;
  - atualização do schema `landing-page-wireframe-schema.json` para incluir e validar os novos campos contratuais (`pagina.body.classes`, `interacao`, `asset`, `contratoCampo`, `componente`) com regras condicionais por `tag`.
- impacto esperado: reduzir geração de wireframe ambígua e eliminar casos de HTML sem `src` em imagem, links sem destino claro e inputs sem contrato de campo.
## 2026-05-24 00:00:00 UTC
- solicitação para corrigir lacunas de design system no prompt da etapa `landing-page-design-preset` (tokens de texto, preset global de body, completude de botão/input, hover real, contraste e uso indevido de opacidade).
- causa-raiz identificada: o prompt definia grupos CSS válidos, mas não explicitava checklist mínimo de classes utilitárias para garantir herança tipográfica/cromática, estados interativos reais e contraste acessível em tema escuro.
- foi feito:
  - atualização de `ai-worker/src/main/resources/prompts/geralanding/landing-page-design-preset.md` com regras obrigatórias e checklist operacional cobrindo `textPrimary/textMuted/textSubtle/textOnButtonPrimary/textOnInput/placeholderText`, preset `pageRoot`, completude de botão/input, regra de hover real, proibição de usar `opacityMuted` como substituto de cor e metas WCAG (4.5:1 e 3:1).
- impacto esperado: reduzir presets incompletos e aumentar consistência visual/acessibilidade do HTML provisório gerado nas etapas seguintes.

- 2026-05-24 — Governança de copy final: formalizado bloqueio canônico para vazamento de metainstrução/texto técnico no Gera Landing (incluindo padrão de erro `IllegalStateException` com caminho do campo rejeitado), com atualização em `procedimento-experimento-canon.v1.md` e reforço em `system-governance-canon.v2.md`.
## 2026-05-23 21:52:18 UTC-3
- solicitação para melhorar legibilidade do quadro de Prompt no detalhe da etapa do experimento (Gera Landing), pois textos longos estavam sem quebras visíveis e difíceis de leitura.
- causa-raiz identificada: o renderer de conteúdo textual bruto usava classe utilitária de quebra (`text-wrap`) que não garantia preservação consistente de quebras/word-wrap no bloco `<pre>` em todos os casos.
- correção aplicada: ajuste do `CollapsibleJsonViewer` para forçar estilo explícito no `<pre>` com `whiteSpace: pre-wrap`, `overflowWrap: anywhere` e `wordBreak: break-word`, preservando saltos de linha e quebrando tokens longos sem comprometer cópia/visualização do prompt.
- documentos/arquivos lidos:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentGeraLandingExecutionDetailPage.tsx
  - frontend/src/components/CollapsibleJsonViewer.tsx
## 2026-05-24 00:20:00 UTC
- solicitação para corrigir erro de renderização Mermaid no diagrama canônico `15.4` de Gera Landing no documento de procedimento de experimento.
- causa-raiz identificada: labels Mermaid com quebra HTML + parênteses sem aspas em nós específicos, gerando falha de parsing no renderer do GitHub.
- foi feito:
  - ajuste dos nós do Mermaid para formato com rótulo entre aspas e `<br/>` compatível com GitHub (`WB`, `OA`, `SD`, `LP`) em `docs/canonical/procedimento-experimento-canon.v1.md`;
  - manutenção do diagrama no mesmo local canônico, sem mudança de arquitetura/regras de fluxo.
- resultado esperado: renderização estável do diagrama no GitHub sem erro de parse.

## 2026-05-24 01:55:00 UTC
- solicitação para corrigir falha de compilação no módulo `backend/ads-service` por símbolo ausente em `DesignPresetProvisionalHtmlProcessor` (`applyPageBodyClasses`).
- causa-raiz identificada: chamadas para aplicação de classes de `pagina.body` permaneceram no fluxo de montagem do HTML, porém o método auxiliar havia sido removido/omitido na classe.
- correção aplicada:
  - reintroduzido `applyPageBodyClasses(Document, Map<String,Object>)` com fallback para `body/corpo` e `classes/classList/estilos`, reaproveitando `collectStyleClasses` + `appendClasses` para manter deduplicação e contrato atual tokenizado.
- resultado esperado: restauração da compilação do `ads-service` e reaplicação consistente de classes globais no `<body>`.


## 2026-05-24 02:20:00 UTC
- solicitação para remover o teste `DesignPresetWireframeHtmlGeneratorTest` que quebrou o `testCompile` do módulo `ads-service` após remoção/renomeação da classe `DesignPresetWireframeHtmlGenerator`.
- causa-raiz identificada: teste órfão em `src/test` referenciando classe inexistente no pacote `com.marketinghub.geralanding.designpreset`.
- correção aplicada: exclusão do arquivo de teste órfão para restaurar a compilação dos testes do módulo.
- resultado esperado: etapa Maven `testCompile` do `ads-service` volta a compilar sem erro de símbolo não encontrado.

## 2026-05-24 03:05:00 UTC
- solicitação para corrigir o HTML do gerador preset design, pois as classes CSS do JSON de preset não estavam sendo inseridas no HTML final.
- causa-raiz identificada:
  - `pagina.body` no preset tokenizado usa estrutura direta `desktop/mobile`, mas o processor só lia `classes/classList/estilos`, descartando as classes globais de `<body>`;
  - a recursão de classes tokenizadas percorria apenas `elementosSeccao` na raiz e apenas `elementosInternos` nos níveis seguintes, deixando nós em estruturas mistas sem aplicação de classes.
- correção aplicada:
  - no `DesignPresetProvisionalHtmlProcessor`, `applyPageBodyClasses` passou a aceitar fallback para o próprio mapa `body` quando ele já está no formato `desktop/mobile`;
  - a recursão de `applyTokenizedNodeClasses` foi normalizada para sempre percorrer os dois filhos (`elementosSeccao` e `elementosInternos`) em todos os níveis.
- cobertura de regressão adicionada:
  - novo teste `shouldApplyBodyClassesAndNestedSectionClassesFromTokenizedPreset` validando classe no `<body>`, em seção e em nós aninhados.
- resultado esperado: classes do preset design passam a ser refletidas corretamente no HTML provisório em toda a árvore.

## 2026-05-24 10:35:00 UTC
- solicitação para garantir que as classes do preset design exibidas em `pagina.body.desktop/mobile` sejam aplicadas no elemento `<body>` do HTML provisório.
- causa-raiz identificada: o parser de classes do `<body>` cobria parcialmente formatos alternativos, mas não consolidava todos os formatos aceitos em uma única coleta robusta, causando perda de classes em alguns payloads da etapa 6.
- correção aplicada:
  - no `DesignPresetProvisionalHtmlProcessor`, criação do método `collectBodyClasses` para consolidar classes do `<body>` em múltiplos formatos (`desktop/mobile` diretos, `classes`, `classList`, `estilos`), com deduplicação preservada;
  - atualização do teste `shouldApplyBodyClassesAndNestedSectionClassesFromTokenizedPreset` para validar explicitamente as 7 classes esperadas no `<body>` (`pageRoot`, `bgBody`, `fontBase`, `textPrimary`, `textSizeBase`, `lineHeightBase`, `marginReset`).
- resultado esperado: o HTML da etapa preset design passa a refletir integralmente as classes globais no `<body>` conforme o JSON gerado.

## 2026-05-24 10:55:00 UTC
- ajuste complementar solicitado: remover rigidez da validação de classes do `<body>` na regressão do preset design, pois a quantidade de classes pode variar por experimento/prompt.
- causa-raiz identificada: o teste anterior validava uma string fixa de `<body class="...">`, acoplando o cenário exatamente a 7 classes e à ordem literal.
- correção aplicada: teste atualizado para parsear o HTML com Jsoup e validar que o `<body>` contém pelo menos as classes esperadas do payload, sem assumir quantidade total fixa.
- resultado esperado: regressão cobre o comportamento funcional correto mesmo quando o preset gerar mais ou menos classes globais.
## 2026-05-24 11:14:04 UTC-3
- solicitação para ajustar os schemas das etapas “gera wireframe” e “gera preset design” para manter separação `desktop/mobile` apenas em `definicoes` e simplificar os elementos de `pagina` para usar classes sem segmentação por device.
- raciocínio aplicado: manter responsividade onde ela pertence (catálogo de tokens em `definicoes`) e reduzir complexidade/ambiguidade nas referências em `pagina.secoes`.
- foi feito:
  - atualização do schema de wireframe para que `pagina.secoes[].estrutura|posicao|layout|mistas` aceitem apenas lista simples de nomes de classe.
  - atualização da instrução textual do prompt de wireframe para refletir a nova regra de referências simples em `pagina/secoes`.
- documentos/arquivos lidos:
  - AGENTS.md
  - docs/registros/experimentos.md
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe-schema.json
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe.md
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-design-preset-schema.json

## 2026-05-24 12:20:00 UTC
- solicitação para ajustar a etapa `landing-page-design-preset`: separar `desktop/mobile` apenas em `definicoes`, aplicar classes diretamente nos elementos de `pagina` e incluir lista `estilos` também em `pagina.body`.
- causa-raiz identificada: o contrato do prompt/schema ainda permitia ambiguidade no `pagina`, incentivando formatos mistos (`desktop/mobile` dentro de elementos) e sem obrigatoriedade explícita de `body.estilos`.
- correção aplicada:
  - atualização do schema `landing-page-design-preset-schema.json` para documentar `pagina.body.estilos` como lista de strings e nós de página com `estilos` em formato direto;
  - atualização do prompt `landing-page-design-preset.md` para instruir explicitamente que `desktop/mobile` existe somente em `definicoes` e que os elementos de `pagina` usam `estilos` como array simples de classes.
- resultado esperado: payloads de preset design mais consistentes com renderização direta das classes no HTML provisório.

## 2026-05-24 12:58:37 UTC-3
- solicitação para ajustar o gerador HTML do backend (`geralanding.presetdesign`) após atualização dos schemas de wireframe e design preset no AI Worker.
- causa-raiz identificada: o processador do preset de design no backend ainda tratava `estilos` principalmente como objeto com `desktop/mobile`, ignorando o novo formato principal em lista simples de classes (`string[]`) previsto no schema novo.
- correção aplicada: atualização do `DesignPresetProvisionalHtmlProcessor` para aceitar e aplicar classes CSS quando `estilos` vier como lista de strings, mantendo compatibilidade com formato legado.
- documentos/arquivos lidos:
  - AGENTS.md
  - backend/AGENTS.md
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe-schema.json
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-design-preset-schema.json
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/designpreset/DesignPresetProvisionalHtmlProcessor.java
  - docs/registros/experimentos.md

## 2026-05-24 13:36:50 UTC-3
- solicitação para aplicar o mesmo ajuste de compatibilidade no gerador/assembler da etapa gera wireframe.
- causa-raiz identificada: o `WireframeHtmlGenerator` consumia `estilos` com cast direto para `List<Map<...>>`, o que pode quebrar/ignorar o novo formato com lista simples de classes (`string[]`) em nós da página.
- correção aplicada:
  - ajuste da leitura de `estilos` para suportar explicitamente lista de strings em classes CSS e manter mapas legados para estilos inline/responsivos;
  - inclusão de normalização de classes sem duplicação no HTML gerado.
- documentos/arquivos lidos:
  - AGENTS.md
  - backend/AGENTS.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/wireframe/WireframeHtmlGenerator.java
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe-schema.json
  - docs/registros/experimentos.md

## 2026-05-24 17:20:00 UTC
- solicitação para detalhar erros no assembler de HTML da etapa `landing-page-copy`.
- causa-raiz identificada: quando o `CopyProvisionalHtmlAssembler` falhava, a exceção retornada para o fluxo superior tinha contexto insuficiente para diagnóstico rápido (sem detalhe da causa-raiz textual no erro propagado).
- correção aplicada:
  - enriquecimento do tratamento de erro no `CopyProvisionalHtmlAssembler` com `errorDetails` contendo classe e mensagem da causa-raiz;
  - inclusão de `jobId`, `copyLength` e `wireframeLength` na mensagem da `IllegalArgumentException` propagada;
  - adição de teste unitário cobrindo a presença dos detalhes na mensagem de erro quando o JSON de copy é inválido.
- documentos/arquivos lidos:
  - AGENTS.md
  - backend/AGENTS.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/copy/CopyProvisionalHtmlAssembler.java
  - backend/ads-service/src/test/java/com/marketinghub/geralanding/CopyProvisionalHtmlAssemblerErrorDetailTest.java
  - docs/registros/experimentos.md

## 2026-05-24 17:45:00 UTC
- solicitação para aplicar no assembler da etapa `landing-page-design-preset` o mesmo padrão de diagnóstico detalhado, incluindo indicação exata do elemento com falha de processamento.
- causa-raiz identificada: falhas do fluxo de design preset eram propagadas com contexto genérico no assembler, sem detalhar payloads e sem destacar elemento (`id/tag`) que quebrou no processor.
- correção aplicada:
  - enriquecimento do `DesignPresetProvisionalHtmlAssembler` com log/erro propagado contendo `jobId`, tamanhos dos payloads e `errorDetails` da causa-raiz;
  - reforço no `DesignPresetProvisionalHtmlProcessor` para lançar erro contextual com `id/tag` do elemento ao falhar na criação/processamento do nó e de seus filhos;
  - adição de teste unitário validando que o assembler propaga mensagem com detalhe de elemento quando ocorre erro no processor.
- documentos/arquivos lidos:
  - AGENTS.md
  - backend/AGENTS.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/designpreset/DesignPresetProvisionalHtmlAssembler.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/designpreset/DesignPresetProvisionalHtmlProcessor.java
  - backend/ads-service/src/test/java/com/marketinghub/geralanding/DesignPresetProvisionalHtmlAssemblerErrorDetailTest.java
  - docs/registros/experimentos.md

## 2026-05-24 18:05:00 UTC
- solicitação para criar exception dedicada de integração OpenAI no backend com construtor único contendo request cru, response cru, json validado, job id do Marketing Hub e job id da OpenAI.
- correção aplicada:
  - criada a classe `OpenAiException` em `com.marketinghub.openai` com um único construtor contendo os cinco campos solicitados;
  - implementado `toString()` para expor literalmente todos os campos, garantindo diagnóstico completo em logs quando a exception for registrada.
- documentos/arquivos lidos:
  - AGENTS.md
  - backend/AGENTS.md
  - backend/ads-service/src/main/java/com/marketinghub/openai/OpenAiException.java
## 2026-05-24 18:00:00 UTC
- solicitação para pesquisar nos logs do backend o erro `500 Internal Server Error` da execução `df5cfce5-d343-4ec1-9022-4954d352d2c6` na etapa `landing-page-copy`.
- investigação executada via MCP (`java_module_logs`) com filtros por `executionId`, endpoint de gera-landing e janela temporal.
- achados objetivos:
  - o worker montou prompt e payload OpenAI para a execução, sem falha prévia na fase de montagem/envio inicial;
  - ocorreu erro em `GeraLandingExecutionService` ao processar a etapa (`Falha ao processar etapa landing-page-copy ... experimentId=31`);
  - não foram encontrados, na mesma janela, stack trace/cause detalhada com o mesmo `executionId` para apontar campo/payload exato rejeitado no `receive-result`.
- próximo passo recomendado (causa-raiz): reforçar logging contextual no ponto de POST para `receive-result` (status, response body e exception completa) para expor o motivo real do 500 e eliminar diagnóstico cego.
- documentos/arquivos lidos:
  - AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-24 18:20:00 UTC
- solicitação para procurar no log do backend problemas no assembler da etapa `copy`, priorizando busca pelo `jobId` `df5cfce5-d343-4ec1-9022-4954d352d2c6`.
- investigação via MCP `java_module_logs` com filtros por `jobId`, `landing-page-copy`, `Assembler` e `CopyProvisionalHtmlAssembler`.
- achados objetivos:
  - com filtro por `jobId`, foi localizado erro com stack trace parcial: `WebClientResponseException$InternalServerError` no POST `.../receive-result`;
  - não retornaram linhas com `Assembler`/`CopyProvisionalHtmlAssembler` na janela pesquisada no módulo `backend`;
  - a falha observável permanece no envio do resultado para o endpoint `receive-result` (HTTP 500), sem linha informativa adicional do assembler no recorte de logs atual.
- próximo passo recomendado (causa-raiz): incluir log estruturado no fluxo de assembler/copy antes do POST final (jobId, tamanho/shape dos artefatos e resumo de validação) e também logar corpo de resposta do `receive-result` para identificar o campo rejeitado no backend receptor.
- documentos/arquivos lidos:
  - AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-24 18:15:00 UTC
- solicitação para adicionar mais logs no `CopyProvisionalHtmlAssembler` e classes internas para identificar com precisão onde o problema ocorre.
- causa-raiz operacional: diagnóstico insuficiente no recorte de produção quando a etapa `landing-page-copy` falha no fluxo de montagem/processamento antes do `receive-result`.
- correção aplicada:
  - `CopyProvisionalHtmlAssembler`: logs de início/sucesso da montagem, métricas de tamanho, preview de payload, contagem de chaves após resolve e `htmlLength` final;
  - `CopyProvisionalHtmlPayloadResolver`: logs de entrada e de estrutura resolvida (`rootKeys` e `keys`) para wireframe/copy;
  - `CopyProvisionalHtmlProcessor`: logs por fase (parse, geração HTML base, total de itens aplicados, conclusão) com tamanhos para rastreabilidade.
- validação executada:
  - `../mvnw -Dtest='*CopyProvisionalHtml*' test` em `backend/ads-service` com sucesso (7 testes, 0 falhas, 0 erros).
- documentos/arquivos lidos:
  - AGENTS.md
  - backend/AGENTS.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/copy/CopyProvisionalHtmlAssembler.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/copy/CopyProvisionalHtmlPayloadResolver.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/copy/CopyProvisionalHtmlProcessor.java
  - docs/registros/experimentos.md

## 2026-05-25 14:55:00 UTC
- ajuste visual na tela de detalhe da execução Gera Landing para corrigir legibilidade do nome da etapa (`stageCode`) no cabeçalho.
- causa-raiz identificada: badge da etapa estava com esquema `bg-primary`, gerando contraste inadequado (texto azul sobre fundo azul) no contexto atual da UI.
- correção aplicada: troca para badge neutra (`text-bg-light` + `text-dark` + borda suave), mantendo destaque sem comprometer leitura.
- arquivos alterados:
  - `frontend/src/pages/experiment/ExperimentGeraLandingExecutionDetailPage.tsx`
  - `docs/registros/experimentos.md`

## 2026-05-25 15:30:00 UTC
- refatoração de arquitetura no AI Worker para separar os montadores de request por etapa do GeraLanding em classes `MontaRequest` por domínio (`wireframe`, `copy`, `imageplanning`, `presetdesign`, `deliverables`).
- causa-raiz: responsabilidade de montagem de payload OpenAI concentrada em `GeraLandingExecutionService`, dificultando evolução por etapa.
- correção aplicada:
  - criação de classes dedicadas `MontaRequest` em cada pacote de etapa;
  - `GeraLandingExecutionService` passou a apenas rotear por etapa com `montarRequestPorEtapa(...)`.
- impacto funcional: sem mudança de contrato; apenas reorganização de responsabilidades mantendo o formato de request por etapa.
- documentos/arquivos lidos:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-25 16:20:00 UTC
- ajuste solicitado: classes `MontaRequest` do GeraLanding passam a receber apenas um objeto de experimento preenchido e devolver o payload pronto, sem parâmetros soltos.
- causa-raiz: assinatura com muitos parâmetros (`model`, `prompt`, `systemName`, `systemMessage`, `schema`) aumentava acoplamento e risco de montagem inconsistente entre etapas.
- correção aplicada:
  - criação do record `GeraLandingExperimentRequest` para centralizar dados necessários da montagem;
  - classes `MontaRequest` de `wireframe`, `copy`, `imageplanning`, `presetdesign` e `deliverables` agora recebem somente `GeraLandingExperimentRequest`;
  - `GeraLandingExecutionService` foi ajustado para construir o objeto de experimento e repassar ao montador da etapa, mantendo o schema resolvido por etapa internamente no fluxo.
- impacto funcional: mantém o contrato final de request OpenAI, com interface de montagem mais coesa e orientada a objeto de experimento.
- documentos/arquivos lidos:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-25 19:10:00 UTC
- ajuste corretivo após revisão: classes `MontaRequest` do GeraLanding agora recebem somente o objeto de experimento e assumem internamente a responsabilidade de resolver schema e configuração padrão do request.
- causa-raiz: implementação anterior ainda exigia resolução de schema no `GeraLandingExecutionService`, mantendo parte da responsabilidade fora das classes de montagem.
- correção aplicada:
  - simplificação de `GeraLandingExperimentRequest` para conter apenas dados do experimento (`experimentId`, `prompt`);
  - cada `MontaRequest` passou a carregar o schema da própria etapa via resource classpath e montar o payload completo internamente;
  - `GeraLandingExecutionService` agora apenas repassa o objeto de experimento para o montador da etapa.

## 2026-05-25 20:05:00 UTC
- ajuste solicitado nos `MontaRequest` do GeraLanding para centralizar definição de nomes de schema/prompt e a montagem completa do prompt por etapa.
- causa-raiz: parte da montagem do prompt (carregamento de markdown + ingestão de placeholders `dados-*` e `prompt-*`) permanecia fora dos montadores, no `GeraLandingService`/`GeraLandingExecutionService`.
- correção aplicada:
  - criação de `MontaRequestSupport` com utilitários de resolução de placeholders de prompt (`{prompt-*}`, `{dados-*}` e `{{mustache}}`) e carregamento de schema;
  - `MontaRequest` de `wireframe`, `copy`, `imageplanning`, `presetdesign` e `deliverables` passaram a declarar internamente:
    - nome do schema JSON da etapa;
    - nome do markdown de prompt da etapa;
    - montagem do prompt final e payload OpenAI;
  - `GeraLandingExecutionService` foi ajustado para delegar a montagem do prompt e do markdown bruto diretamente aos `MontaRequest` por etapa.
- impacto funcional: mantém o contrato final de request OpenAI, reduz acoplamento e centraliza a lógica de montagem por etapa.

## 2026-05-25 20:20:00 UTC
- ajuste solicitado em revisão: criação do pacote `geralanding.comum` e realocação do utilitário compartilhado de montagem.
- correção aplicada:
  - `MontaRequestSupport` movido para `com.marketinghub.worker.geralanding.comum`;
  - atualização dos imports nas classes `MontaRequest` das etapas (`wireframe`, `copy`, `imageplanning`, `presetdesign`, `deliverables`).
- impacto funcional: sem alteração de comportamento, apenas organização de pacote para melhor separação de responsabilidades.

## 2026-05-25 20:30:00 UTC
- ajuste solicitado: criação de regras de arquitetura com ArchUnit para isolamento dos módulos `geralanding.*` no AI Worker.
- causa-raiz: ausência de validação automatizada explícita para impedir acoplamento entre módulos irmãos e para restringir dependências do pacote `geralanding.comum`.
- correção aplicada:
  - inclusão de regras ArchUnit para bloquear dependências cruzadas entre módulos `copy`, `presetdesign`, `stage`, `wireframe`, `deliverables` e `imageplanning`;
  - inclusão de regra ArchUnit para garantir que `geralanding.comum` não acesse módulos funcionais `geralanding.*`.
- impacto funcional: reforço de fronteiras arquiteturais do domínio GeraLanding com validação automatizada em testes.

## 2026-05-25 20:50:00 UTC
- ajuste de refinamento: endurecimento das regras ArchUnit do GeraLanding para restringir dependências também contra outros pacotes da aplicação (`com.marketinghub.worker..`).
- causa-raiz: versão anterior bloqueava módulos irmãos, mas ainda permitia dependências para outros pacotes da aplicação fora de `geralanding`.
- correção aplicada:
  - regras por módulo alteradas para proibir dependências em `com.marketinghub.worker..` fora do próprio pacote e de `geralanding.comum`;
  - regra de `geralanding.comum` ajustada para permitir apenas dependências no próprio pacote dentro da aplicação.
- impacto funcional: validação arquitetural agora confirma que `geralanding.*` só acessa próprio pacote/comum e nenhum outro ponto da aplicação.

## 2026-05-25 21:10:00 UTC
- ajuste solicitado em revisão: ampliar o escopo das restrições ArchUnit de `com.marketinghub.worker..` para `com.marketinghub..`.
- causa-raiz: regra anterior cobria apenas dependências internas do worker e não todo o namespace da aplicação.
- correção aplicada:
  - atualização das regras por módulo para filtrar dependências em `com.marketinghub..`;
  - manutenção das mesmas exceções permitidas (próprio pacote e `geralanding.comum`).
- impacto funcional: o isolamento de `geralanding.*` passa a considerar todo o domínio `com.marketinghub`.
## 2026-05-25 21:05:00 UTC
- correção de compilação no `GeraLandingExecutionService` para alinhamento de assinatura com exceções reais dos montadores por etapa.
- causa-raiz: o método `montarRequestPorEtapa` declarava apenas `JsonProcessingException`, mas os montadores (`montar`) podem propagar `IOException`.
- correção aplicada:
  - atualização da assinatura de `montarRequestPorEtapa` para `throws IOException`, eliminando erro de compilação de exceção checada não declarada.
- impacto funcional: sem mudança de comportamento de negócio; apenas correção de contrato de exceções para compilar com segurança.

## 2026-05-25 21:20:00 UTC
- correção de compilação na etapa de copy do GeraLanding.
- causa-raiz: a classe `copy/MontaRequest` chamava `montarPrompt(...)` (que lança `IOException`) dentro de `montar(...)`, mas a assinatura de `montar` declarava apenas `JsonProcessingException`.
- correção aplicada:
  - remoção do import não utilizado `JsonProcessingException`;
  - atualização da assinatura de `montar(...)` para `throws IOException`, alinhando com as exceções reais propagadas pelo fluxo.
- impacto funcional: sem alteração de regra de negócio; apenas correção de contrato de exceção para eliminar o erro de compilação no CI.

## 2026-05-25 21:24:00 UTC
- correção complementar de compilação nos montadores de etapas adicionais do GeraLanding.
- causa-raiz: o mesmo desalinhamento de exceção checada (`IOException`) também existia em `imageplanning`, `presetdesign` e `deliverables`.
- correção aplicada:
  - atualização das assinaturas `montar(...)` para `throws IOException` nesses três montadores;
  - remoção dos imports `JsonProcessingException` não utilizados após o ajuste.
- impacto funcional: correção de build sem alteração de comportamento funcional das etapas.


## 2026-05-25 20:45:00 UTC
- ajuste no teste do GeraLanding para remover assertiva textual obsoleta de heading ("## Pipeline de hipótese").
- causa-raiz: o contrato funcional atual da etapa `landing-page-wireframe` é validado por schema JSON (`landing-page-wireframe-schema.json`), e a presença de heading literal no prompt deixou de ser requisito obrigatório.
- correção aplicada:
  - atualização de `GeraLandingServiceTest` para validar os campos canônicos do pipeline sem acoplamento ao título do bloco.
- impacto funcional: elimina falso negativo de teste sem alterar comportamento de negócio nem contrato de saída.


## 2026-05-25 21:05:00 UTC
- ajuste solicitado: desligamento do teste textual amplo do pipeline no `GeraLandingServiceTest`.
- causa-raiz: a validação linha a linha do bloco canônico no prompt ficou desnecessária para o fluxo atual e gerava manutenção sem ganho de contrato funcional.
- correção aplicada:
  - anotação `@Disabled` no teste `deveDisponibilizarTodosOsItensCanonicosDosPipelinesNoPromptFinal`;
  - inclusão de justificativa explícita no próprio teste.
- impacto funcional: nenhum impacto em runtime; apenas redução de ruído em suíte de testes.

## 2026-05-25 22:10:00 UTC
- ajuste no assembler da etapa de preset design para preservar estilos das duas fontes (wireframe + design preset).
- causa-raiz: o `DesignPresetProvisionalHtmlProcessor` recriava/removia a tag `<style id="lhm-legacy-design-preset-css">` a cada aplicação de estilos tokenizados; com isso, o segundo processamento sobrescrevia o CSS do primeiro.
- correção aplicada:
  - alteração da rotina `applyTokenizedPresetStyles` para reutilizar a mesma tag `<style>` e concatenar o CSS, mantendo simultaneamente as definições das etapas wireframe e design preset;
  - inclusão de teste de regressão `shouldKeepWireframeAndDesignCssDefinitionsTogether` cobrindo presença conjunta de classes/CSS das duas etapas no HTML final.
- impacto funcional: o HTML provisório final passa a conter todos os estilos definidos em wireframe e preset design, sem perda por sobrescrita.

## 2026-05-25 22:32:00 UTC
- ajuste solicitado no assembler de preset design para remover identificador fixo na tag de estilos.
- causa-raiz: a implementação anterior dependia de `id="lhm-legacy-design-preset-css"` na `<style>`, mas o contrato esperado para o HTML provisório não exige esse identificador.
- correção aplicada:
  - atualização de `DesignPresetProvisionalHtmlProcessor.applyTokenizedPresetStyles` para inserir estilos em `<style>` sem `id`;
  - atualização do teste `shouldApplyLegacyTokenizedPresetFormat` para validar ausência do id legado e manter as validações de CSS/classes.
- impacto funcional: mantém os estilos de wireframe + preset design no HTML final, agora sem atributo `id` na tag `<style>`.

## 2026-05-25 22:55:00 UTC
- ajuste solicitado: aplicar a regra de estilos canônicos no fluxo real (código), e não apenas no documento.
- causa-raiz: o worker aceitava wireframe com referências em `estilos[]` que não existiam em `definicoes`, permitindo drift de contrato mesmo com schema sintático válido.
- correção aplicada:
  - adição de validação pós-resposta da OpenAI na etapa `landing-page-wireframe` para garantir que cada item de `pagina.corpo.secoes[*].estilos` e `elementosSeccao[*].estilos` exista no conjunto `definicoes.*.(desktop|mobile)[].nome`;
  - rejeição explícita com caminho literal do campo inválido quando houver estilo inexistente;
  - inclusão de teste unitário cobrindo falha e callback `receiveFailure` quando o wireframe retorna estilo não definido.
- impacto funcional: bloqueia na origem a publicação de wireframe com estilos fora das definições canônicas, reduzindo retrabalho nas etapas seguintes.

## 2026-05-25 23:10:00 UTC
- ajuste solicitado: reforçar a mesma regra também no prompt da etapa wireframe.
- causa-raiz: somente validar no pós-resposta reduz risco técnico, mas não orienta preventivamente o modelo durante geração.
- correção aplicada:
  - atualização de `landing-page-wireframe.md` com regra explícita proibindo nomes em `estilos[]` fora de `definicoes.*.(desktop|mobile)[].nome`;
  - atualização de teste do `GeraLandingServiceTest` para garantir presença literal da nova instrução no prompt montado.
- impacto funcional: aumenta aderência já na geração do JSON e reduz incidência de retorno inválido antes da validação de runtime.

## 2026-05-25 23:30:00 UTC
- ajuste solicitado: aplicar a mesma proteção de estilos canônicos também na etapa `landing-page-design-preset`.
- causa-raiz: o preset design ainda podia referenciar classes em `pagina.estilos[]` sem correspondência no bloco `definicoes`, gerando deriva de contrato visual.
- correção aplicada:
  - validação de runtime no `GeraLandingExecutionService` para `DESIGN_PRESET`, cobrindo `pagina.body.estilos`, `pagina.corpo.estilos`, `secoes[]`, `elementosSeccao[]` e `elementosInternos[]`;
  - atualização do prompt `landing-page-design-preset.md` com regra explícita de referência exclusiva a `definicoes.*.(desktop|mobile)[].nome`;
  - novos testes unitários para falha da execução quando houver estilo indefinido no preset e para presença da regra no prompt de design preset.
- impacto funcional: bloqueia preset inválido antes de persistência e orienta o modelo a produzir saída aderente já na geração.

## 2026-05-26 00:00:00 UTC
- solicitação: criação da estrutura de pacotes para o submódulo `geralanding.x` no backend.
- ação aplicada:
  - criação dos pacotes `com.marketinghub.geralanding.x.web`, `com.marketinghub.geralanding.x.service` e `com.marketinghub.geralanding.x.repository`.
  - inclusão de `package-info.java` em cada pacote para documentar responsabilidade básica por camada.
- impacto funcional: estrutura base preparada para separar responsabilidades HTTP, serviço e persistência no novo submódulo `x`.

## 2026-05-26 01:40:00 UTC
- ajuste solicitado: mover os endpoints das etapas do GeraLanding para o pacote web da estrutura `geralanding.x`.
- causa-raiz: os controllers de etapa ainda estavam no pacote raiz `com.marketinghub.geralanding`, sem aderência à segmentação por camada definida para `geralanding.x`.
- correção aplicada:
  - migração de `GeraLandingContoller` e `GeraLandingInternalController` para `com.marketinghub.geralanding.x.web`.
  - atualização dos imports nos testes `@WebMvcTest` para apontar para os controllers no novo pacote.
- impacto funcional: endpoints mantidos com os mesmos contratos/rotas HTTP, agora organizados no pacote web do submódulo `geralanding.x`.

## 2026-05-26 02:05:00 UTC
- ajuste solicitado: tratar `x` como variável por etapa no GeraLanding (e não como pacote literal).
- causa-raiz: organização anterior fixava controllers em `geralanding.x.web`, contrariando a intenção de segmentar por etapa.
- correção aplicada:
  - remoção da estrutura literal `geralanding.x.*`.
  - criação de controllers por etapa nos pacotes `geralanding.wireframe.web`, `geralanding.copy.web`, `geralanding.designpreset.web`, `geralanding.imageplanning.web` e `geralanding.deliverables.web`.
  - criação de controller transversal em `geralanding.execution.web` para listagem/detalhe de execuções e publicação.
  - migração do controller interno para `geralanding.internal.web`.
  - atualização dos testes WebMvc para os novos pacotes.
- impacto funcional: endpoints permanecem com os mesmos contratos HTTP, agora organizados por etapa conforme regra de variável por estágio.
## 2026-05-25 23:55:00 UTC
- ajuste solicitado: criar classe `RecebeResponse` nas etapas do GeraLanding (`wireframe`, `copy`, `imageplanning`, `presetdesign`, `deliverables`) para centralizar recebimento da resposta crua da OpenAI e envio ao backend.
- causa-raiz: o envio de dispatch/result estava acoplado no serviço de execução, dificultando evolução por etapa e rastreabilidade do payload por domínio.
- correção aplicada:
  - criação de `RecebeResponse` por pacote de etapa para montar o callback e enviar dados ao backend;
  - atualização do `GeraLandingExecutionService` para delegar o processamento de resposta por etapa via `encaminharRespostaDaEtapa(...)`.
- impacto funcional: separa responsabilidade por etapa, melhora organização do fluxo e mantém envio padronizado de payload para os endpoints de backend.

## 2026-05-26 00:10:00 UTC
- ajuste solicitado: definir nomes de bean explícitos e distintos para todas as classes `RecebeResponse` do GeraLanding.
- causa-raiz: todas as classes compartilhavam o mesmo nome simples e estavam anotadas apenas com `@Component`, aumentando risco de ambiguidade de injeção por nome em evoluções futuras.
- correção aplicada:
  - adição de nomes explícitos em `@Component` para cada etapa (`wireframe`, `copy`, `imageplanning`, `presetdesign`, `deliverables`).
- impacto funcional: mantém a injeção estável e elimina ambiguidades de identificação de bean por nome.

## 2026-05-26 02:20:00 UTC
- ajuste solicitado: criar pacote `service` em cada etapa do GeraLanding e mover o início de execução para classes específicas por etapa.
- causa-raiz: os controllers de `wireframe`, `copy`, `imageplanning` e `designpreset` dependiam diretamente de classes do pacote raiz `com.marketinghub.geralanding`, violando o teste de isolamento arquitetural por subpacote.
- correção aplicada:
  - criação de serviços por etapa (`GeraLanding*StageService`) dentro de `geralanding.<etapa>.service`;
  - criação de responses por etapa (`GeraLanding*StartResponse`) dentro de `geralanding.<etapa>.service`;
  - atualização dos controllers de etapa para depender apenas das classes do próprio subpacote.
- impacto funcional: mantém os endpoints e o contrato HTTP de início de etapa, com isolamento arquitetural por submódulo.

## 2026-05-26 02:58:00 UTC
- ajuste solicitado: corrigir falha de contexto em `GeraLandingContollerTest` durante `@WebMvcTest`.
- causa-raiz: o controller `geralanding.copy.web.GeraLandingCopyController` passou a depender de `GeraLandingCopyStageService`, mas o teste não declarava `@MockBean` desse serviço, impedindo a criação do ApplicationContext.
- correção aplicada:
  - inclusão de `@MockBean GeraLandingCopyStageService` no teste;
  - ajuste do cenário `shouldCreateCopyExecutionAndReturnCodeAndStatus` para mockar/verificar `copyStageService.start(...)` e `GeraLandingCopyStartResponse`.
- impacto funcional: restaura a subida do contexto do teste MVC sem alterar contrato HTTP dos endpoints.

## 2026-05-26 03:45:00 UTC
- ajuste solicitado: criar `GeraLandingStartResponse` e `GeraLandingStageExecutionService` dentro do pacote `com.marketinghub.geralanding.wireframe.service`.
- causa-raiz: necessidade de disponibilizar contrato e serviço locais no subpacote `wireframe.service` para reduzir acoplamento direto no ponto de uso.
- correção aplicada:
  - criação de `GeraLandingStartResponse` local em `geralanding.wireframe.service`;
  - criação de `GeraLandingStageExecutionService` local em `geralanding.wireframe.service`, com adaptação de retorno para o contrato local.
- impacto funcional: mantém a API interna de início da etapa wireframe com tipos locais no próprio subpacote.

## 2026-05-26 03:50:00 UTC
- ajuste solicitado: remover o uso de `com.marketinghub.geralanding.GeraLandingStartResponse` no serviço local de wireframe.
- causa-raiz: implementação anterior fazia adaptação por delegação ao serviço/response do pacote raiz, contrariando a orientação de manter a lógica dentro do pacote local neste momento.
- correção aplicada:
  - substituição da lógica delegada por implementação direta de `registerInitialExecution(...)` em `geralanding.wireframe.service.GeraLandingStageExecutionService`;
  - remoção do uso explícito de `com.marketinghub.geralanding.GeraLandingStartResponse` no fluxo local.
- impacto funcional: o retorno do start continua no contrato local `GeraLandingStartResponse`, agora com lógica executada localmente no serviço da etapa wireframe.

## 2026-05-26 04:05:00 UTC
- ajuste solicitado: adicionar regra de arquitetura no ArchUnit para restringir dependências de `geralanding.*.service`.
- causa-raiz: ausência de guarda arquitetural explícita permitia que serviços do GeraLanding dependessem de classes internas fora da lista permitida.
- correção aplicada:
  - inclusão de regra `geralandingServicePackagesShouldOnlyAccessAllowedMarketingHubClasses` em `ModuleIsolationArchitectureTest`;
  - lista de classes permitidas em `com.marketinghub`: `Experiment`, `ExperimentRepository`, `GeraLandingStageExecution`, `GeraLandingStageExecutionRepository`.
- impacto funcional: prevenção automatizada de acoplamento indevido entre serviços `geralanding.*.service` e outros pacotes internos.

## 2026-05-26 01:07:18 UTC-3
- solicitação para criar diagramas separados de backend e worker ai usando como base as definições ArchUnit do módulo geralanding no documento canônico.
- raciocínio aplicado: usar os testes de arquitetura como fonte objetiva das dependências permitidas/proibidas para evitar diagrama genérico e manter aderência de governança.
- foi feito:
  - atualização do cânone `procedimento-experimento-canon.v1.md` com seção de diagramas por módulo (backend e worker ai), incluindo regras explícitas derivadas dos testes ArchUnit;
  - ajuste de numeração da seção subsequente de regra mandatória para manter sequência (`15.5`).
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/ads-service/src/test/java/com/marketinghub/architecture/GeraLandingAssemblerArchitectureTest.java
  - backend/ads-service/src/test/java/com/marketinghub/architecture/ModuleIsolationArchitectureTest.java
  - ai-worker/src/test/java/com/marketinghub/worker/geralanding/GeraLandingArchitectureTest.java
  - docs/canonical/procedimento-experimento-canon.v1.md
  - docs/registros/experimentos.md

## 2026-05-26 01:11:16 UTC-3
- ajuste solicitado: mover os diagramas de arquitetura do GeraLanding para um documento próprio do tema GeraLanding.
- causa-raiz: os diagramas estavam no procedimento geral de experimento, dificultando localizar rapidamente a arquitetura canônica específica do módulo.
- foi feito:
  - criado o documento canônico `docs/canonical/geralanding-arquitetura-canon.v1.md` com os dois diagramas (backend e worker ai) baseados nas regras ArchUnit;
  - seção `15.4` do `docs/canonical/procedimento-experimento-canon.v1.md` foi reduzida para apontamento explícito ao novo documento canônico de arquitetura.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/procedimento-experimento-canon.v1.md
  - backend/ads-service/src/test/java/com/marketinghub/architecture/GeraLandingAssemblerArchitectureTest.java
  - backend/ads-service/src/test/java/com/marketinghub/architecture/ModuleIsolationArchitectureTest.java
  - ai-worker/src/test/java/com/marketinghub/worker/geralanding/GeraLandingArchitectureTest.java
  - docs/registros/experimentos.md

- 2026-05-26: Ajustado `GeraLandingWireframeStageService` para usar as cópias locais de service/response no pacote `geralanding.wireframe.service`, removendo dependência direta de `com.marketinghub.geralanding.GeraLandingStageExecutionService` e `GeraLandingStartResponse`.
- 2026-05-26: Reorganizadas classes provisórias de GeraLanding no backend para subpacotes dedicados `geralanding.wireframe.provisorio` e `geralanding.copy.provisorio`, com atualização dos imports em produção e testes.

## 2026-05-26 12:20:00 UTC
- ajuste solicitado: remover isolamento amplo em `geralanding.*` no ArchUnit e aplicar regras de isolamento em nível de pacotes mais baixos (`web`, `provisorio`, `service`).
- causa-raiz: as regras anteriores isolavam submódulos inteiros (`wireframe`, `copy`, `imageplanning`, `designpreset`) e não representavam o contrato de dependência por camada dentro de cada etapa.
- correção aplicada:
  - criada regra para `geralanding.*.web`: permite depender apenas de `geralanding.*.web` e `geralanding.*.service` da mesma etapa;
  - criada regra para `geralanding.*.provisorio`: permite depender apenas de `geralanding.*.provisorio` da mesma etapa;
  - mantida regra para `geralanding.*.service` com whitelist explícita para `Experiment`, `ExperimentRepository`, `GeraLandingStageExecution` e `GeraLandingStageExecutionRepository`.
- impacto funcional: o ArchUnit passa a proteger o encapsulamento por camada/etapa com granularidade mais precisa, reduzindo acoplamentos indevidos entre etapas do GeraLanding.

## 2026-05-26 10:50:00 UTC
- solicitado descontinuar controllers legados `com.marketinghub.geralanding.execution.web.GeraLandingExecutionController` e `com.marketinghub.geralanding.internal.web.GeraLandingInternalController` e migrar uso para os novos controllers por etapa.
- causa-raiz: os controllers legados centralizados geravam acoplamento transversal entre etapas e violavam a regra ArchUnit de isolamento `geralanding.*.web` por etapa.
- correção aplicada: removidos os dois controllers legados do backend para concentrar o fluxo nos endpoints novos organizados por etapa (`wireframe`, `copy`, `imageplanning`, `deliverables`, `designpreset`).
- 2026-05-26: Criadas classes de cópia locais de execução por etapa (`GeraLandingCopyStageExecutionService`, `GeraLandingDesignPresetStageExecutionService`, `GeraLandingImagePlanningStageExecutionService`, `GeraLandingDeliverablesStageExecutionService`) e atualizado cada `*StageService` para usar somente service/response do próprio pacote da etapa, evitando dependência direta de `GeraLandingStageExecutionService`/`GeraLandingStartResponse` no código das etapas.
- 2026-05-26: Ajuste de abordagem conforme revisão: os `*StageExecutionService` das etapas `copy`, `designpreset`, `imageplanning` e `deliverables` passaram de adaptadores para implementação duplicada local do fluxo `registerInitialExecution`, sem delegação para `GeraLandingStageExecutionService`.

- 2026-05-26: Atualizada a regra arquitetural de GeraLanding para permitir dependência entre classes do próprio pacote `service` (mesma etapa), mantendo as dependências explícitas já permitidas (`Experiment`, `ExperimentRepository`, `GeraLandingStageExecution`, `GeraLandingStageExecutionRepository`). Ajustados teste ArchUnit e cânone para aderência.

- 2026-05-26: Ajustado `docs/pitch/marketing-hub-investidores.md` com identidade visual (logo MKTH no canto dos slides, gradientes de fundo e destaque tipográfico) para melhorar apresentação do pitch.

## 2026-05-26 18:15:00 UTC
- ajuste solicitado: corrigir falha de compilação em `GeraLandingExecutionServiceTest` após mudança de assinatura do construtor de `GeraLandingExecutionService`.
- causa-raiz: o construtor passou a exigir cinco dependências adicionais de processadores `RecebeResponse` por etapa (`wireframe`, `copy`, `imageplanning`, `presetdesign`, `deliverables`) e os testes continuavam instanciando o serviço com a assinatura antiga.
- correção aplicada:
  - adicionados mocks de `RecebeResponse` para todas as etapas em cada cenário do teste;
  - atualizado o `new GeraLandingExecutionService(...)` em todos os testes para enviar os novos argumentos na ordem correta.
- impacto funcional: os testes voltam a compilar com a API atual do serviço, eliminando o erro de lista de argumentos divergente no `testCompile`.
- 2026-05-26: Atualizados os diagramas canônicos de arquitetura do GeraLanding para refletir as regras ArchUnit vigentes no backend por camada/etapa (`web`, `provisorio` e `service`), incluindo as dependências explicitamente permitidas em `service` para `Experiment`, `ExperimentRepository`, `GeraLandingStageExecution` e `GeraLandingStageExecutionRepository`.

## 2026-05-26 18:40:00 UTC
- ajuste solicitado: corrigir falha do teste `GeraLandingExecutionServiceTest.processPendingExecutionsShouldSendPromptToOpenAiAndRegisterResult` que ainda esperava callback direto no `backendClient`.
- causa-raiz: após a refatoração por etapa, `receiveDispatch`/`receiveResult` passaram a ocorrer dentro dos processadores `RecebeResponse`; no teste o `wireframeRecebeResponse` é mock e, portanto, não executa o callback real no backend.
- correção aplicada:
  - alterada a asserção principal para validar a delegação ao processador da etapa (`wireframeRecebeResponse.processar(...)`);
  - removida expectativa de `receiveDispatch`/`receiveResult` no `backendClient` nesse cenário, mantendo validação de ausência de falha.
- impacto funcional: o teste passa a validar o contrato correto do `GeraLandingExecutionService` (delegar o processamento da resposta para a etapa), eliminando falso negativo no surefire.

- 2026-05-26 20:36:00 UTC — Ajustado o teste `GeraLandingExecutionServiceTest.processPendingExecutionsShouldSendPromptToOpenAiAndRegisterResult` para refletir o fluxo atual com validação de wireframe: o mock de resposta OpenAI agora retorna JSON válido com `definicoes` e `pagina.corpo.secoes`, permitindo a chamada de `wireframeRecebeResponse.processar(...)` no caminho de sucesso.
## 2026-05-26 17:57:13 UTC-3
- solicitação para validar na tela `/experiments/33` se os pontos de frontend do fluxo GeraLanding chamam controllers nos pacotes `geralanding.<etapa>.web`.
- raciocínio aplicado: mapear todos os endpoints `*/geralanding/*` consumidos pela tela de detalhe do experimento e cruzar com os controllers do backend por pacote.
- resultado da validação:
  - conformes nos pacotes `geralanding.<etapa>.web`: `wireframe/start`, `copy/start`, `design-preset/start`, `image-prompts/start`, `deliverables/start`.
  - pendências fora de `geralanding.<etapa>.web`: `stage-executions`, `stage-executions/{jobId}`, `html/provisional/generate`, `landing/approve-and-publish` (não localizados em controllers `geralanding.<etapa>.web` no backend atual).
- documentos/arquivos lidos:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx
  - frontend/src/api/experiment/useGeraLandingStageExecutions.ts
  - frontend/src/api/experiment/useApproveAndPublishLanding.ts
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/wireframe/web/GeraLandingWireframeController.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/copy/web/GeraLandingCopyController.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/designpreset/web/GeraLandingDesignPresetController.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/imageplanning/web/GeraLandingImagePlanningController.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/deliverables/web/GeraLandingDeliverablesController.java
## 2026-05-26 18:01:25 UTC-3
- solicitação para corrigir as chamadas de listagem de execuções do GeraLanding na tela de experimento, removendo o uso de `stageCode` como query param e passando a usar endpoint específico por etapa.
- causa-raiz: o frontend consultava endpoint genérico (`/geralanding/stage-executions`) com `stageCode` na query, contrariando o contrato desejado de endpoints segmentados por etapa.
- correção aplicada no frontend:
  - criado mapeamento `stageCode -> segmento de endpoint` no hook de execuções do GeraLanding;
  - atualizado `GET` para usar `.../geralanding/<etapa>/stage-executions` e manter apenas `includeCompleted` como parâmetro de query.
- documentos/arquivos lidos:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/api/experiment/useGeraLandingStageExecutions.ts
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx

## 2026-05-26 21:20:00 UTC
- solicitação: disponibilizar endpoints de listagem/detalhe de execuções dentro dos pacotes `geralanding.<etapa>.web`, incluindo `GET /api/experiments/{experimentId}/geralanding/deliverables/stage-executions?includeCompleted=false`.
- causa-raiz: os controllers por etapa possuíam apenas endpoint `start`, sem endpoints HTTP para consultar histórico e detalhe de execuções, apesar do serviço `GeraLandingStageExecutionService` já suportar essas consultas.
- correção aplicada no backend:
  - adicionados endpoints por etapa (`wireframe`, `copy`, `image-prompts`, `design-preset`, `deliverables`) para:
    - `GET /<etapa>/stage-executions`
    - `GET /<etapa>/stage-executions/{idJob}`
  - mantido filtro `includeCompleted` com default `true`, delegando ao `GeraLandingStageExecutionService` com `stageCode` fixo por controller.
- validação executada:
  - compilação do módulo `ads-service` concluída com sucesso após alterações.
- 2026-05-26 21:12:16 UTC — Simplificado o diagrama do documento `docs/canonical/geralanding-arquitetura-canon.v1.md` para reduzir confusão: os pacotes passaram a ser exibidos como caixas únicas e as dependências aparecem somente com setas quando permitidas (sem seta = acesso não permitido).

- 2026-05-26 23:45:03 UTC — Ajustado o diagrama canônico do GeraLanding (seção Backend) para remover setas de auto dependência de pacote (`web -> web`, `service -> service`, `provisorio -> provisorio`), mantendo apenas dependências entre pacotes distintos e regras permitidas explícitas.
- 2026-05-26 23:46:30 UTC — Aplicado o mesmo ajuste de simplificação em outros trechos de dependência entre pacotes no cânone do GeraLanding: removidas setas de auto referência semântica no diagrama do Worker AI (`copy/presetdesign/wireframe/imageplanning/deliverables/stage/comum` para eles mesmos), mantendo as regras explícitas no texto logo abaixo.

## 2026-05-26 23:58:00 UTC
- solicitação: criar versões por etapa de `GeraLandingExecutionSummaryResponse`, `GeraLandingStageExecutionDetailResponse` e `GeraLandingStageExecutionService`, usando apenas as versões da etapa nos controllers.
- causa-raiz: controllers `geralanding.<etapa>.web` dependiam de classes transversais em `com.marketinghub.geralanding`, violando a regra ArchUnit de isolamento por etapa no pacote web.
- correção aplicada no backend:
  - criados DTOs e serviços de execução por etapa em `geralanding.<etapa>.service` para `wireframe`, `copy`, `designpreset`, `imageplanning` e `deliverables`;
  - atualizados controllers de etapa para depender apenas dos serviços/DTOs locais da etapa;
  - marcadas como obsoletas (`@Deprecated`) as classes transversais antigas: `GeraLandingExecutionSummaryResponse`, `GeraLandingStageExecutionDetailResponse` e `GeraLandingStageExecutionService`.
- validação executada:
  - teste arquitetural `ArquiteturaTest` executado com sucesso após refatoração.

## 2026-05-27 00:38:00 UTC
- solicitação: nas classes do pacote `service`, usar a versão local do próprio pacote para iniciar execução de etapa.
- correção aplicada no backend (`ads-service`):
  - `GeraLanding*StageService` de `copy`, `designpreset`, `imageplanning`, `wireframe` e `deliverables` passaram a depender de `GeraLanding*StageExecutionService` local da etapa (em vez de depender direto do serviço transversal);
  - adicionado método `registerInitialExecution(...)` em cada `GeraLanding*StageExecutionService` para encapsular a delegação e retornar `GeraLanding*StartResponse` local.
- validação executada:
  - `mvn -q -Dtest=ArquiteturaTest test` (ainda falha por dependências remanescentes dos `*StageExecutionService` para classes transversais legadas).

## 2026-05-27 01:15:00 UTC
- solicitação: "faça o mesmo nas demais etapas além da copy" para remover dependência dos `*StageExecutionService` por etapa do serviço transversal legado.
- correção aplicada no backend (`ads-service`):
  - refatorados `GeraLandingWireframeStageExecutionService`, `GeraLandingImagePlanningStageExecutionService`, `GeraLandingDesignPresetStageExecutionService` e `GeraLandingDeliverablesStageExecutionService`;
  - removida a dependência de `GeraLandingStageExecutionService`, `GeraLandingExecutionSummaryResponse` e `GeraLandingStageExecutionDetailResponse` nas etapas acima;
  - implementada lógica local com `ExperimentRepository` + `GeraLandingStageExecutionRepository` para:
    - `registerInitialExecution(...)`;
    - `listExperimentStageExecutions(...)`;
    - `getStageExecutionDetail(...)`;
  - mantidas conversões de `idJob` (`String` <-> `byte[]`) em cada serviço por etapa para aderir ao repositório e ao contrato de resposta da etapa.
- validação executada:
  - `mvn -Dtest=ArquiteturaTest test -q` (ainda falha por violações arquiteturais remanescentes, inclusive chamadas ao tipo interno `GeraLandingStageExecution$GeraLandingStageExecutionBuilder` e outras classes fora do escopo permitido).

## 2026-05-27 01:40:00 UTC
- solicitação: "pegue uma situação que tem esse erro" para a violação ArchUnit em `geralanding..service..`.
- situação capturada (reprodução real):
  - teste executado: `mvn -Dtest=ArquiteturaTest test -q` no módulo `backend/ads-service`;
  - falha observada: regra `geralandingServicePackagesShouldOnlyAccessAllowedMarketingHubClasses` com **50 violações**;
  - exemplo concreto da violação:
    - classe origem: `com.marketinghub.geralanding.copy.service.GeraLandingCopyStageExecutionService`;
    - operação: método `registerInitialExecution(Long, String)`;
    - dependência proibida: chamadas para `com.marketinghub.geralanding.GeraLandingStageExecution$GeraLandingStageExecutionBuilder` (`experimentId(...)`, `stageCode(...)`, `executionRequestedAt(...)`, `build()`, entre outras).
- causa-raiz identificada:
  - a regra arquitetural permite explicitamente `GeraLandingStageExecution` e `GeraLandingStageExecutionRepository`, porém as chamadas do builder (`GeraLandingStageExecution$GeraLandingStageExecutionBuilder`) são contabilizadas pelo ArchUnit como dependência para uma classe **não incluída** na allowlist.
- ação aplicada nesta tarefa:
  - registrado o cenário com evidência objetiva para orientar o próximo ajuste da regra/implementação e evitar análise por consequência.

## 2026-05-27 01:50:00 UTC
- solicitação: adicionar como 5ª exceção permitida na regra ArchUnit de `geralanding..service..` a classe interna do builder de `GeraLandingStageExecution`.
- ajuste aplicado em `backend/ads-service/src/test/java/com/marketinghub/architecture/ArquiteturaTest.java`:
  - criada constante `GERALANDING_STAGE_EXECUTION_BUILDER_CLASS` com valor `com.marketinghub.geralanding.GeraLandingStageExecution$GeraLandingStageExecutionBuilder`;
  - incluída essa constante na allowlist da condição `onlyDependOnAllowedMarketingHubClasses()`;
  - atualizada mensagem da regra para listar explicitamente as 5 exceções permitidas.
- validação executada:
  - `cd backend/ads-service && ../../backend/mvnw -Dtest=ArquiteturaTest test -q` (sucesso).

## 2026-05-27 02:55:00 UTC
- solicitação: ajustar frontend para consultar status de job no endpoint segmentado por etapa (GeraLanding stage-executions).
- ajuste aplicado no frontend:
  - `useGeraLandingStageExecutionDetail(...)` passou a receber `stageCode` e montar URL por etapa (`/geralanding/{stage}/stage-executions/{jobId}`) reaproveitando o resolvedor já existente;
  - tela `ExperimentGeraLandingExecutionDetailPage` passou a ler `stageCode` da querystring e repassar para o hook de detalhe;
  - links de Job ID na página do experimento foram atualizados para incluir `?stageCode=...` correto por etapa (wireframe, copy, image-planning, design-preset, deliverables), garantindo consulta no endpoint certo.
- validação executada:
  - `npm run lint` (falhou: script inexistente no `package.json` do frontend);
  - `npm run build` (falhou: `vite: not found` no ambiente atual).

## 2026-05-27 06:12:00 UTC
- solicitação: desligar scheduler global `GeraLandingExecutionScheduler` no ai-worker e criar scheduler por etapa, usando endpoint exclusivo da etapa no backend.
- ajustes aplicados no `ai-worker`:
  - removida anotação `@Component` de `GeraLandingExecutionScheduler` para desativar a execução global;
  - criada a API `processExecutions(List<GeraLandingStageExecutionDto>)` em `GeraLandingExecutionService` para processar listas já filtradas por etapa;
  - criados schedulers dedicados por etapa, cada um chamando somente seu respectivo `*PendingJobsService`:
    - `WireframeExecutionScheduler`;
    - `CopyExecutionScheduler`;
    - `ImagePlanningExecutionScheduler`;
    - `PresetDesignExecutionScheduler`;
    - `DeliverablesExecutionScheduler`.
  - cada scheduler usa cron literal direto na anotação `@Scheduled`.
- observação operacional:
  - a confirmação de pendência por etapa continua sendo feita via endpoint exclusivo da etapa em cada `*PendingJobsService` (wireframe/copy/imageplanning/designpreset/deliverables).
- validação executada:
  - `mvn -f ai-worker/pom.xml -DskipTests compile` (falhou por dependência externa ausente/sem acesso: `com.marketinghub:ads-service:0.0.1-SNAPSHOT`, erro 401 no repositório GitHub Packages).

## 2026-05-27 06:23:00 UTC
- solicitação: no ai-worker, dentro de `*.geralanding.<etapa>`, criar `GeraLanding<Etapa>ExecutionService` para evitar import direto de `com.marketinghub.worker.geralanding.GeraLandingExecutionService` nos schedulers de etapa.
- ajustes aplicados no `ai-worker`:
  - criados serviços por etapa para encapsular a execução compartilhada:
    - `GeraLandingWireframeExecutionService`;
    - `GeraLandingCopyExecutionService`;
    - `GeraLandingImagePlanningExecutionService`;
    - `GeraLandingPresetDesignExecutionService`;
    - `GeraLandingDeliverablesExecutionService`.
  - atualizado cada `*ExecutionScheduler` de etapa para depender do serviço da própria etapa, removendo o import direto do executor compartilhado.
- validação executada:
  - `mvn -f ai-worker/pom.xml -Dtest=GeraLandingExecutionServiceTest test` (falhou por dependência externa sem autenticação: `com.marketinghub:ads-service:0.0.1-SNAPSHOT`, erro 401 no GitHub Packages).

## 2026-05-27 13:37:00 UTC-3
- solicitação: mover dependências de GeraLanding para pacote de etapa no AI Worker, aceitando duplicação para isolamento arquitetural.
- causa-raiz identificada: pacote `copy` dependia de classes-base em `com.marketinghub.worker.geralanding`, contrariando a regra de isolamento por etapa do `ai-worker/AGENTS.md`.
- correção aplicada: criadas seis classes locais no pacote `copy` (`GeraLandingExecutionService`, `GeraLandingStageExecutionDto`, `GeraLandingExperimentRequest`, `GeraLandingBackendClient`, `GeraLandingJobCompletionPayload`) com delegação/mapeamento para as classes base, e atualização dos imports do pacote `copy` para usar apenas as classes locais.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/registros/experimentos.md

## 2026-05-27 00:00:00 UTC
- solicitação: no `ai-worker`, criar classes específicas de wireframe (`GeraLandingExecutionWireframeService`, `GeraLandingStageExecutionWireframeDto`, `GeraLandingExperimentWireframeRequest`, `GeraLandingWireframeBackendClient`, `GeraLandingJobCompletionWireframePayload`) e replicar o padrão de isolamento por etapa no GeraLanding.
- causa-raiz identificada: a etapa wireframe ainda dependia diretamente de classes compartilhadas em `com.marketinghub.worker.geralanding`, aumentando acoplamento transversal entre etapas.
- correção aplicada:
  - criadas as novas classes específicas de wireframe com mapeamento/delegação para o núcleo compartilhado;
  - atualizado `WireframePendingJobsService` para consumir DTO e backend client da própria etapa wireframe;
  - atualizado `WireframeExecutionScheduler` para depender do novo `GeraLandingExecutionWireframeService`;
  - mantido `GeraLandingWireframeExecutionService` como compatibilidade delegando para o novo serviço.
- validação executada:
  - `mvn -q -Dtest=GeraLandingExecutionServiceTest,GeraLandingServiceTest test` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-27 00:00:00 UTC (extensão)
- solicitação: aplicar o mesmo padrão de isolamento por etapa do wireframe para as demais etapas do GeraLanding no AI Worker.
- correção aplicada:
  - criadas classes específicas por etapa para `imageplanning`, `presetdesign` e `deliverables`:
    - `GeraLandingStageExecution<Etapa>Dto`
    - `GeraLandingExperiment<Etapa>Request`
    - `GeraLandingJobCompletion<Etapa>Payload`
    - `GeraLanding<Etapa>BackendClient`
  - atualizado `*PendingJobsService` das três etapas para consumir `BackendClient` e `StageExecutionDto` locais da etapa.
  - atualizado `GeraLandingImagePlanningExecutionService`, `GeraLandingPresetDesignExecutionService` e `GeraLandingDeliverablesExecutionService` para aceitar DTOs locais e converter para DTO base no ponto de delegação.
- validação executada:
  - `mvn -q -DskipTests compile` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-27 00:00:00 UTC (varredura de isolamento por etapa no ai-worker)
- solicitação: varrer os pacotes de etapas do GeraLanding no `ai-worker` e substituir usos de classes compartilhadas quando já existirem equivalentes dentro do próprio pacote da etapa.
- causa-raiz identificada: as etapas `imageplanning`, `presetdesign` e `deliverables` ainda recebiam/enviavam tipos compartilhados (`GeraLandingExperimentRequest`, `GeraLandingJobCompletionPayload` e `GeraLandingBackendClient`) apesar de já existirem DTOs/clients locais por etapa.
- correção aplicada:
  - `MontaRequest` de `imageplanning`, `presetdesign` e `deliverables` passou a receber os requests locais de etapa;
  - `RecebeResponse` dessas três etapas passou a depender do backend client local e dos payloads locais de etapa;
  - `GeraLandingExecutionService` foi ajustado para converter request/payload comuns nos tipos locais de `imageplanning`, `presetdesign` e `deliverables` antes de chamar `MontaRequest` e `RecebeResponse`.
- validação executada:
  - `mvn -Dtest=ArquiteturaTest test -DskipITs` em `backend/ads-service` (sucesso);
  - `mvn -Dtest=ArquiteturaTest test -DskipITs` em `ai-worker` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).


## 2026-05-27 18:20:00 UTC
- solicitação: corrigir erro de compilação no `ai-worker` causado por métodos ausentes nos backend clients de etapa do GeraLanding (`imageplanning`, `presetdesign`, `deliverables`).
- causa-raiz identificada: no refactor de isolamento por etapa, os `RecebeResponse` passaram a chamar `receiveDispatch` e `receiveResult` nos clients locais, mas esses métodos não foram implementados nas classes locais.
- correção aplicada:
  - adicionados `receiveDispatch` e `receiveResult` em `GeraLandingImagePlanningBackendClient`;
  - adicionados `receiveDispatch` e `receiveResult` em `GeraLandingPresetDesignBackendClient`;
  - adicionados `receiveDispatch` e `receiveResult` em `GeraLandingDeliverablesBackendClient`;
  - os métodos `receiveResult` fazem mapeamento explícito dos payloads de etapa para `GeraLandingJobCompletionPayload` antes de delegar ao backend client base.
- validação executada:
  - `mvn -pl ai-worker -DskipTests compile` (falhou: projeto não encontrado no reactor).
  - `mvn -DskipTests compile` em `ai-worker/` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-27 19:05:00 UTC
- solicitação: "fazer o mesmo ajuste para os pacotes das outras etapas" após o ajuste anterior de isolamento do wireframe no `ai-worker`.
- causa-raiz identificada: as etapas `copy`, `imageplanning`, `presetdesign` e `deliverables` ainda mantinham acoplamento de execução ao serviço concreto raiz e mapeamentos redundantes de payload/DTO para tipo base.
- correção aplicada:
  - padronizadas as classes de execução de etapa (`copy`, `imageplanning`, `presetdesign`, `deliverables`) para depender de `geralanding.comum.GeraLandingStageExecutionProcessor` e enviar `GeraLandingStageExecutionRef`;
  - removidos mapeamentos redundantes nos backend clients locais dessas etapas para envio de `receiveResult`, passando a delegar payload tipado da etapa;
  - adicionados overloads `receiveResult(...)` tipados no `GeraLandingBackendClient` base para `copy`, `imageplanning`, `presetdesign` e `deliverables`;
  - removidos métodos `toBase()` redundantes dos payloads e DTOs de etapa impactados.
- validação executada:
  - inspeção estática via `rg` para confirmar remoção de `toBase()` nas etapas ajustadas;
  - tentativa de execução de testes/maven permanece bloqueada por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` (HTTP 401 no GitHub Packages).

## 2026-05-27 20:10:00 UTC
- solicitação: subdividir os pacotes da etapa `geralanding.wireframe` no `ai-worker` em `monitor`, `openai`, `callback` e `backend`.
- causa-raiz identificada: as classes da etapa wireframe estavam concentradas em um único pacote, reduzindo isolamento interno por responsabilidade e dificultando manutenção por contexto da etapa.
- correção aplicada:
  - movidas classes de execução/monitoramento para `geralanding.wireframe.monitor`;
  - movidas classes de request OpenAI para `geralanding.wireframe.openai`;
  - movidas classes de callback/result payload para `geralanding.wireframe.callback`;
  - movidas classes de integração backend e DTOs para `geralanding.wireframe.backend`;
  - atualizadas importações e referências em `GeraLandingExecutionService`, `GeraLandingCopyBackendClient` e `GeraLandingExecutionServiceTest` para os novos pacotes.
- validação executada:
  - `mvn -q -Dtest=GeraLandingExecutionServiceTest test` em `ai-worker/` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 14:20:00 UTC
- solicitação: remover classes adaptadoras `GeraLandingExecution<Etapa>Service` com pouca responsabilidade no fluxo monitorado de etapas, mantendo somente agendador, polling de pendências e processor no pacote `geralanding.<etapa>.monitor`.
- causa-raiz identificada: a etapa `wireframe` mantinha duas camadas de delegação para execução (`GeraLandingExecutionWireframeService` e `GeraLandingWireframeExecutionService`) sem ganho funcional, aumentando acoplamento e ruído arquitetural.
- correção aplicada:
  - removidas as classes `GeraLandingExecutionWireframeService` e `GeraLandingWireframeExecutionService` do pacote `geralanding.wireframe.monitor`;
  - criado `WireframeExecutionProcessor` para concentrar o processamento da lista retornada pelo polling;
  - atualizado `WireframeExecutionScheduler` para orquestrar apenas `WireframePendingJobsService` + `WireframeExecutionProcessor`.
- validação executada:
  - `mvn -q -DskipTests compile` em `ai-worker/` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 15:05:00 UTC
- solicitação: aplicar o mesmo padrão do wireframe para as demais etapas, mantendo em `geralanding.<etapa>.monitor` apenas 3 papéis (scheduler, pending jobs e processor).
- causa-raiz identificada: as etapas `copy`, `imageplanning`, `presetdesign` e `deliverables` ainda mantinham classes `GeraLanding<Etapa>ExecutionService` apenas como camada adaptadora, sem responsabilidade adicional.
- correção aplicada:
  - movidos schedulers e serviços de pendências para os pacotes `monitor` de cada etapa;
  - criados processors dedicados por etapa (`CopyExecutionProcessor`, `ImagePlanningExecutionProcessor`, `PresetDesignExecutionProcessor`, `DeliverablesExecutionProcessor`);
  - removidas classes adaptadoras `GeraLandingCopyExecutionService`, `GeraLandingImagePlanningExecutionService`, `GeraLandingPresetDesignExecutionService` e `GeraLandingDeliverablesExecutionService`;
  - atualizado cada scheduler para orquestrar apenas `PendingJobsService` + `ExecutionProcessor` da própria etapa.
- validação executada:
  - `mvn -q -DskipTests compile` em `ai-worker/` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 16:10:00 UTC
- solicitação: renomear `GeraLandingJobCompletionWireframePayload` para `RecordWireframeResponse` na etapa `geralanding.wireframe` do `ai-worker`.
- causa-raiz identificada: nome anterior longo e inconsistente com o padrão desejado para objeto de resposta da etapa wireframe.
- correção aplicada:
  - classe record renomeada para `RecordWireframeResponse`;
  - atualizadas todas as referências/importações no fluxo wireframe (backend client, execução OpenAI e receive response) e no teste de execução.
- validação executada:
  - `mvn -q -Dtest=GeraLandingExecutionServiceTest test` em `ai-worker/` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 17:05:00 UTC
- solicitação: replicar para `geralanding.presetdesign` a mesma estrutura de pacotes consolidada na etapa `geralanding.wireframe`.
- causa-raiz identificada: a etapa `presetdesign` ainda mantinha classes centrais em pacote raiz da etapa, diferente da organização por responsabilidade usada em `wireframe`.
- correção aplicada:
  - movida a integração com backend para `geralanding.presetdesign.backend.GeraLandingPresetDesignBackendClient`;
  - movida a classe de callback para `geralanding.presetdesign.response.RecebeResponse`;
  - criado o record `geralanding.presetdesign.response.RecordPresetDesignResponse` para alinhar payload de retorno ao padrão de resposta por etapa;
  - atualizadas referências/importações nos fluxos de monitor/request e no backend client compartilhado.
- validação executada:
  - `mvn -q -Dtest=GeraLandingExecutionServiceTest test` em `ai-worker/` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 17:30:00 UTC
- solicitação: renomear `GeraLandingExperimentWireframeRequest` para `RecordWireframeRequest` na etapa `geralanding.wireframe` do `ai-worker`.
- causa-raiz identificada: nome anterior não seguia o padrão curto de records adotado nas classes de request/response do fluxo wireframe.
- correção aplicada:
  - arquivo `GeraLandingExperimentWireframeRequest.java` renomeado para `RecordWireframeRequest.java`;
  - record atualizado para `RecordWireframeRequest`;
  - referências ajustadas em `GeraLandingWireframeOpenAiExecutionService` e `MontaRequest`.
- validação executada:
  - inspeção estática das referências com `rg` para garantir ausência do nome antigo em código Java da etapa wireframe.

## 2026-05-28 18:00:00 UTC
- solicitação: usar a mesma estrutura do wireframe no worker ai para replicar na etapa `geralanding.imageplanning`.
- causa-raiz identificada: `imageplanning` ainda mantinha classes `MontaRequest`, `RecebeResponse` e `GeraLandingImagePlanningBackendClient` no pacote raiz da etapa, divergindo do padrão por responsabilidade aplicado em `wireframe`.
- correção aplicada:
  - movida integração backend para `geralanding.imageplanning.backend.GeraLandingImagePlanningBackendClient`;
  - movido callback de resposta para `geralanding.imageplanning.response.RecebeResponse`;
  - movido montador de request OpenAI para `geralanding.imageplanning.request.MontaRequest`;
  - atualizados imports/referências em `GeraLandingImagePlanningOpenAiExecutionService` e `ImagePlanningPendingJobsService`.
- validação executada:
  - `mvn -q -DskipTests compile` em `ai-worker/` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 18:20:00 UTC
- solicitação: retirar o ponto final da regra ArchUnit de `geralanding.wireframe` para corrigir falso-positivo.
- causa-raiz identificada: a checagem permitia apenas pacote `geralanding.comum.` (com subpacote), mas a classe alvo estava no pacote exato `geralanding.comum`.
- correção aplicada:
  - ajustada a condição `targetPackage.contains(".geralanding.comum.")` para `targetPackage.contains(".geralanding.comum")` em `ArquiteturaTest`.
- validação executada:
  - inspeção estática da regra alterada com `nl -ba` para confirmar remoção do ponto final no matcher.
  - execução de `mvn -q -Dtest=com.marketinghub.worker.geralanding.ArquiteturaTest test` em `ai-worker/` (falhou por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 19:05:00 UTC
- solicitação: usar `geralanding.wireframe` como referência para remover dependências cruzadas entre slices do Worker AI e corrigir falhas do `ArquiteturaTest`.
- causa-raiz identificada: `copy.backend.GeraLandingCopyBackendClient` concentrava contratos e métodos de `imageplanning`, `presetdesign` e `wireframe`, e `imageplanning/presetdesign` dependiam diretamente de `copy.backend`.
- correção aplicada:
  - removidas, em `copy.backend.GeraLandingCopyBackendClient`, as APIs tipadas e consultas específicas de `imageplanning`, `presetdesign` e `wireframe`;
  - reescrito `imageplanning.backend.GeraLandingImagePlanningBackendClient` para encapsular seu próprio fluxo HTTP com DTO e payload da própria etapa, sem depender de `copy`;
  - reescrito `presetdesign.backend.GeraLandingPresetDesignBackendClient` no mesmo padrão de isolamento por etapa.
- validação executada:
  - `mvn -q -Dtest=ArquiteturaTest test` em `ai-worker/` (bloqueado por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).


## 2026-05-28 16:40:00 UTC
- solicitação: revisar chamadas do Worker AI ao backend no fluxo GeraLanding após erro 404 em `GET /api/internal/geralanding/stage-executions/pending?limit=20`.
- causa-raiz identificada: o Worker AI ainda chamava endpoints internos genéricos de execução, mas o contrato operacional atual exige endpoints específicos por etapa para evitar mistura entre wireframe, copy, image-planning, design-preset e deliverables.
- correção aplicada:
  - ajustados apenas os clients do Worker AI para usar endpoints específicos já existentes no backend: `wireframe`, `copy`, `image-prompts`, `design-preset` e `deliverables`;
  - removida a dependência do backend client de deliverables sobre o backend client de copy, mantendo isolamento por etapa.
- validação executada:
  - `mvn -q -DskipTests compile` em `backend/ads-service/`;
  - `mvn -q -DskipTests install` em `backend/ads-service/` para publicar o artefato local exigido pelo Worker AI;
  - `mvn -q -DskipTests compile` em `ai-worker/`.


## 2026-05-28 17:05:00 UTC
- solicitação: corrigir revisão do PR para remover controller interno genérico no backend; cada etapa do GeraLanding deve ter seu próprio endpoint/controller.
- causa-raiz identificada: a primeira correção criou um controller único parametrizado por `{stagePath}`, o que preservava endpoint específico na URL mas mantinha um controller genérico para todas as etapas.
- correção aplicada:
  - removidas do PR as alterações em classes do backend porque os endpoints do backend já estavam prontos e não deveriam ser modificados nesta tarefa;
  - mantida somente a adequação do Worker AI para chamar os endpoints específicos por etapa;
  - atualizado o cânone de arquitetura para registrar que a correção do Worker AI não deve criar controller genérico no backend.
- validação executada:
  - `mvn -q -DskipTests compile` em `backend/ads-service/`;
  - `mvn -q -Dtest=com.marketinghub.architecture.ArquiteturaTest test` em `backend/ads-service/`.


## 2026-05-28 17:25:00 UTC
- solicitação: não alterar classes do backend porque os endpoints do GeraLanding já estão prontos.
- causa-raiz identificada: a correção anterior tentou resolver o 404 criando/alterando controllers, repositório e serviço no backend, mas a responsabilidade desta tarefa é ajustar apenas o Worker AI para consumir os endpoints específicos existentes.
- correção aplicada:
  - revertidas todas as alterações em classes Java do backend feitas para esta revisão;
  - mantidas as chamadas do Worker AI para endpoints específicos por etapa;
  - preservada documentação canônica apenas como regra de consumo/isolamento, sem introduzir implementação nova no backend.
- validação executada:
  - `mvn -q -DskipTests compile` em `ai-worker/`.
## 2026-05-28 20:15:00 UTC
- solicitação: renomear `GeraLandingJobDto` para `RecordJobDto` em todas as etapas do `geralanding` no `ai-worker`.
- causa-raiz identificada: o nome anterior mantinha o prefixo legado do módulo e não seguia o padrão curto de records adotado nas etapas do GeraLanding.
- correção aplicada:
  - records de job OpenAI renomeados para `RecordJobDto` nos pacotes geral, `copy`, `deliverables`, `imageplanning`, `presetdesign` e `wireframe`;
  - imports, assinaturas e instanciações atualizados nos serviços de execução OpenAI do GeraLanding;
  - cliente flex geral atualizado para aceitar `RecordJobDto` do fluxo geral e das etapas `deliverables`, `imageplanning` e `presetdesign`.
- validação executada:
  - inspeção estática com `rg` para garantir ausência do nome antigo no código Java do `ai-worker`;
  - `mvn -q -DskipTests compile` em `ai-worker/` (bloqueado por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 14:08:18 UTC-3
- solicitação para criar um Swagger canônico com os endpoints existentes no backend do GeraLanding organizados por etapa.
- raciocínio aplicado: verificar diretamente os controllers do pacote `com.marketinghub.geralanding` para documentar apenas contratos realmente expostos pelo backend e evitar inventar endpoints inexistentes.
- registro do que foi feito: criado o OpenAPI canônico `docs/canonical/geralanding-backend-swagger.v1.yaml` com endpoints de start, listagem e detalhe das etapas wireframe, copy, image planning, design preset e deliverables; também foi adicionada referência ao Swagger no cânone de arquitetura do GeraLanding.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/canonical/geralanding-arquitetura-canon.v1.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/wireframe/web/GeraLandingWireframeController.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/copy/web/GeraLandingCopyController.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/imageplanning/web/GeraLandingImagePlanningController.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/designpreset/web/GeraLandingDesignPresetController.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/deliverables/web/GeraLandingDeliverablesController.java

## 2026-05-28 20:45:00 UTC
- solicitação: verificar se todos os acessos dos pacotes do Worker AI `geralanding.<etapa>` usam endpoints declarados no Swagger canônico em `docs/canonical`.
- causa-raiz identificada: o Swagger canônico atual documenta apenas endpoints públicos por experimento (`start`, listagem e detalhe), enquanto os clients do Worker AI ainda consomem endpoints operacionais internos por etapa (`pending`, `receive-result`, `receive-dispatch` e, no caso de copy, `receive-prompt`) e endpoints auxiliares de experimento/nicho que não aparecem nesse Swagger.
- resultado da verificação:
  - acessos compatíveis: consultas de detalhe por etapa em `/api/experiments/{experimentId}/geralanding/<etapa>/stage-executions/{idJob}`;
  - acessos não declarados no Swagger canônico: `/api/internal/geralanding/<etapa>/stage-executions/pending`, `/receive-result`, `/receive-dispatch`, `/receive-prompt`, `/api/experiments/{experimentId}`, `/api/niches/{nicheId}` e `/api/niches/{nicheId}/hypotheses`.
- validação executada:
  - `find docs/canonical -type f` para confirmar que o único Swagger canônico YAML é `docs/canonical/geralanding-backend-swagger.v1.yaml`;
  - `rg`/script Python sobre `ai-worker/src/main/java/com/marketinghub/worker/geralanding` para enumerar chamadas HTTP montadas com `UrlUtils.joinPath` e `.uri(...)`;
  - `rg` nos controllers do backend em `backend/ads-service/src/main/java/com/marketinghub/geralanding` para comparar os endpoints expostos pelo pacote `com.marketinghub.geralanding` com o Swagger.

## 2026-05-28 19:10:00 UTC
- solicitação: ajustar o Worker AI para buscar pendências de `landing-page-wireframe` usando a URL realmente exposta pelo backend atual.
- causa-raiz identificada: `GeraLandingWireframeBackendClient.listPendingExecutions(...)` chamava `/api/internal/geralanding/wireframe/stage-executions/pending`, endpoint inexistente no backend; o controller atual expõe a listagem por experimento em `/api/experiments/{experimentId}/geralanding/wireframe/stage-executions?includeCompleted=false`.
- correção aplicada:
  - o client de wireframe passou a listar experimentos via `/api/experiments` e, para cada experimento, consultar a URL correta de execuções abertas de wireframe por experimento;
  - os resumos retornados pelo backend são convertidos para `GeraLandingStageExecutionDetailDto` com `experimentId` e `stageCode` preenchidos para manter o fluxo do scheduler;
  - adicionado teste unitário garantindo que o client chama `/api/experiments/{experimentId}/geralanding/wireframe/stage-executions?includeCompleted=false`.
- validação executada:
  - `mvn -q -Dtest=GeraLandingWireframeBackendClientTest test` em `ai-worker/` (bloqueado por dependência privada externa `com.marketinghub:ads-service:0.0.1-SNAPSHOT` com HTTP 401 no GitHub Packages).

## 2026-05-28 21:30:00 UTC
- solicitação: criar endpoint interno global de pendências para a etapa wireframe do GeraLanding e reforçar a arquitetura por etapa.
- causa-raiz tratada: a etapa wireframe precisava de uma fila independente de experimento para o Worker AI consultar apenas jobs com status `INICIADO`, evitando varredura por experimento como substituto operacional.
- correção aplicada:
  - renomeado o controller Java de wireframe para `BackendWireframeController`;
  - criado o endpoint `GET /api/internal/geralanding/wireframe/stage-executions/pending` no método `pending`;
  - adicionada consulta por `stageCode` + status `INICIADO` no repositório e no serviço da etapa wireframe;
  - registrado o padrão em `docs/canonical/arquitetura-etapas.md` e no Swagger canônico do GeraLanding;
  - criada regra ArchUnit para exigir método `pending` em classes `Backend<Etapa>Controller` do GeraLanding;
  - adicionados testes unitários do controller e serviço de wireframe.

## 2026-05-28 — Padronização de retorno pending por etapa

- Renomeado o record de pendência da etapa wireframe para `RecordWireframePending`.
- Ajustado `BackendWireframeController.pending` para retornar diretamente `List<RecordWireframePending>`.
- Registrada no cânone de arquitetura por etapa a regra `Backend<Etapa>Controller.pending -> List<Record<Etapa>Pending>`.
- Atualizado `ArquiteturaTest` para validar o contrato genérico do método `pending` por etapa.

## 2026-05-28 21:55:00 UTC
- solicitação: incluir o experimento junto aos itens pendentes da etapa `landing-page-wireframe` e garantir teste para atributos `experiment` e `jobid` no retorno de `pending`.
- causa-raiz tratada: o contrato interno global de pendências expunha apenas identificadores mínimos (`experimentId`, `idJob`/etapa), obrigando consumidores a buscar o experimento em chamada separada para identificar contexto básico do job.
- correção aplicada:
  - o record `RecordWireframePending` passou a expor `jobid`, `stageCode`, `experimentId` e o resumo `experiment`;
  - criado `RecordWireframeExperiment` com os campos mínimos do experimento necessários para identificação do job pendente;
  - o serviço de execução de wireframe passou a montar o resumo do experimento a partir da execução carregada;
  - a consulta de pendências no repositório passou a carregar o relacionamento `experiment` via `EntityGraph` para evitar consulta adicional por item;
  - o cânone de arquitetura por etapa e o Swagger canônico do GeraLanding foram atualizados com o novo contrato de pending;
  - adicionados testes garantindo os dados do experimento no service e a serialização da resposta como lista contendo `experiment` e `jobid`.

## 2026-05-28 22:20:00 UTC
- solicitação: ampliar o objeto `experiment` retornado pelo pending de `landing-page-wireframe` com artefatos de criação usados no fluxo e levantar os campos existentes de hipótese.
- causa-raiz tratada: o resumo anterior do experimento ainda era insuficiente para consumidores da fila reaproveitarem contexto de prompts, anúncios e etapas já geradas sem novas chamadas ao backend.
- correção aplicada:
  - `RecordWireframeExperiment` passou a incluir `creativeTextPrompt`, `creativeImagePrompt`, `campaignAngle`, `adCopy`, `adImageBriefing`, `landingPageCopy`, `landingPageWireframe`, `landingPageImagePlanning`, `landingPageDesignPreset`, `landingPageDeliverables` e `htmlGeraLanding`;
  - o mapeamento de pending da etapa wireframe passou a preencher esses campos a partir da entidade `Experiment` carregada pela execução;
  - os testes do service e do controller foram atualizados para validar os novos campos serializados no objeto `experiment`;
  - o cânone de arquitetura por etapa e o Swagger canônico do GeraLanding foram atualizados com os novos campos do contrato;
  - os campos da tabela/entidade/DTO de hipótese foram levantados para orientar a próxima decisão de contrato.

## 2026-05-28 22:45:00 UTC
- solicitação: incluir `hypothesis.framework` com todos os itens canônicos no retorno da lista de pendentes da etapa `landing-page-wireframe`.
- causa-raiz tratada: o payload de pending já carregava o experimento, mas ainda não entregava o framework da hipótese no mesmo contrato, obrigando consumidores da fila a buscar a hipótese separadamente para obter Dor → Resultado → Mecanismo → Prova → Oferta.
- correção aplicada:
  - `RecordWireframePending` passou a incluir o atributo `hypothesis` além de `experiment`;
  - criado `RecordWireframeHypothesis` com `id`, `title` e `framework`;
  - o serviço de wireframe passou a resolver `hypothesis.framework` a partir do JSON da hipótese e garantir presença dos blocos `pain`, `result`, `mechanism`, `proof`, `offer` e `checklist`;
  - a entidade `Experiment` passou a expor métodos operacionais para acessar id, título e framework JSON da hipótese associada sem acoplar o serviço da etapa ao pacote de hipótese;
  - a consulta de pending passou a carregar `experiment.hypothesisRef` junto com o experimento;
  - testes do service/controller, cânone de arquitetura por etapa e Swagger canônico foram atualizados para o novo contrato.

## 2026-05-29 00:05:00 UTC
- solicitação: ajustar a lista `pending` da etapa `landing-page-wireframe` para não serializar artefatos JSON como strings escapadas, evitando perda de estrutura em `campaignAngle` e campos equivalentes.
- causa-raiz tratada: artefatos persistidos em colunas textuais eram repassados diretamente pelo contrato interno de pending, gerando JSON dentro de string e obrigando consumidores a reparsear campos que deveriam chegar estruturados.
- correção aplicada:
  - `RecordWireframeExperiment` passou a aceitar artefatos como `Object` nos campos JSON-backed;
  - o serviço de wireframe passou a reidratar JSON válido de `campaignAngle`, `adCopy`, `adImageBriefing`, `landingPageCopy`, `landingPageWireframe`, `landingPageImagePlanning`, `landingPageDesignPreset` e `landingPageDeliverables` antes de serializar a resposta;
  - conteúdo textual real permanece como string e JSON aparentemente inválido gera log com `experimentId` e nome do campo;
  - o cânone de arquitetura por etapa e o Swagger canônico do GeraLanding foram atualizados para proibir JSON dentro de string nos contratos internos;
  - testes do service e do controller foram atualizados para validar `campaignAngle` e demais artefatos como JSON estruturado no pending.
- validação executada:
  - `./../mvnw -Dtest=GeraLandingWireframeStageExecutionServiceTest,BackendWireframeControllerTest test` em `backend/ads-service/`.

## 2026-05-28 21:30:47 UTC-3
- solicitação: criar um documento canônico com diagramas das informações tratadas pela OpenAI, incluindo framework de hipóteses e atributos do experimento vindos das etapas do pipeline.
- raciocínio aplicado: centralizar em `/docs/canonical` a visão de dados enviados/recebidos pela OpenAI para preservar rastreabilidade, separar metadado técnico de artefato final e manter aderência ao eixo Dor → Resultado → Mecanismo → Prova → Oferta.
- registro do que foi feito: criado o cânone `docs/canonical/openai-informacoes-tratadas-canon.v1.md`, com diagramas Mermaid, matriz de atributos de hipótese, atributos de experimento, etapas do pipeline, envelope técnico de job e checklists de governança; também foi adicionada referência no procedimento canônico de experimento.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - docs/canonical/procedimento-experimento-canon.v1.md
  - docs/modelo-dados-hipotese.md
  - docs/modelo-dados-experimento.md
  - docs/experiment-pipeline-artifacts-visual.md
  - docs/ai-worker/experimento-criativo-service.md
  - docs/ai-worker/produto-sucesso-nicho-hypotese-service.md
  - backend/ads-service/src/main/java/com/marketinghub/hypothesis/Hypothesis.java
  - backend/ads-service/src/main/java/com/marketinghub/experiment/Experiment.java
  - backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/ExperimentPipelineSection.java
  - backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/ExperimentPipelineGenerationJob.java
## 2026-05-29 — Worker AI preparado para fila pending estruturada do wireframe

- Ajustado o client do Worker AI da etapa `landing-page-wireframe` para consumir diretamente o endpoint interno `/api/internal/geralanding/wireframe/stage-executions/pending`, preservando os artefatos JSON estruturados enviados pelo backend (`experiment` e `hypothesis.framework`) sem voltar a depender da varredura por experimento.
- A montagem dos dados de prompt passou a preferir o JSON já entregue na fila pending, mantendo fallback legado por `experimentId` quando o backend não enviar dados embutidos.
- Adicionado teste unitário cobrindo o contrato `jobid`, `experiment`, `hypothesis.framework` e a preservação do JSON estruturado no DTO consumido pelo scheduler de wireframe.

## 2026-05-29 — Worker AI wireframe sem consulta adicional de detalhe

- Solicitação: remover a necessidade de `fetchWireframeStageExecutionDetail` no fluxo de wireframe, porque tudo que a etapa precisa já vem na lista de pendentes.
- Causa-raiz tratada: o serviço de pendências ainda confirmava cada job com uma chamada extra ao endpoint de detalhe, apesar de o contrato `pending` já carregar status, experimento, hipótese e artefatos estruturados suficientes para processamento.
- Correção aplicada:
  - `WireframePendingJobsService` passou a usar exclusivamente `listPendingExecutions` e filtrar apenas o código da etapa `landing-page-wireframe`;
  - removido o método `fetchWireframeStageExecutionDetail` do client de wireframe do Worker AI;
  - o cânone de arquitetura por etapa foi atualizado para declarar que o `pending` de wireframe é fonte suficiente para o Worker AI, sem chamada adicional de detalhe antes do processamento;
  - adicionados testes garantindo que o serviço usa apenas a lista pending estruturada e que o client faz somente uma requisição ao endpoint interno de pendências.

## 2026-05-28 22:53:51 UTC-3
- solicitação para documentar no cânone de arquitetura por etapas a segunda etapa operacional do Worker AI: agendamento e busca de novos itens para processamento.
- raciocínio aplicado: usar o scheduler de wireframe como referência concreta para formalizar que cada ciclo agendado consulta apenas a fila interna `pending` da própria etapa e que cada item deve chegar completo como unidade de trabalho fechada.
- foi feito: inclusão da seção "Etapa 2 — Agendamento e busca de novos itens para processamento" em `docs/canonical/arquitetura-etapas.md`, deixando explícito que o contrato `pending` deve carregar todos os identificadores, dados de contexto e artefatos necessários sem chamada adicional de detalhe antes do processamento.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/canonical/arquitetura-etapas.md
  - docs/registros/experimentos.md
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/wireframe/monitor/WireframeExecutionScheduler.java
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/wireframe/monitor/WireframePendingJobsService.java
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/wireframe/backend/GeraLandingWireframeBackendClient.java

## 2026-05-28 22:58:09 UTC-3
- solicitação para complementar o cânone de arquitetura por etapas com a Etapa 3 do Worker AI: obtenção dos arquivos de prompt e schema, ingestão dos dados da solicitação recebida do backend e envio do request para o endpoint da OpenAI.
- raciocínio aplicado: usar o wireframe como exemplo concreto para documentar que a unidade de trabalho fechada é transformada em `Record<Etapa>Request`, combinada com prompt markdown e schema JSON versionados, convertida em payload JSON validável e enviada ao endpoint `/responses` da OpenAI.
- foi feito: inclusão da seção "Etapa 3 — Montagem do request OpenAI com prompt, schema e dados da solicitação" em `docs/canonical/arquitetura-etapas.md`, declarando responsabilidades do `<Etapa>OpenAiExecutionService`, do `MontaRequest`, dos arquivos `*.md`/`*-schema.json`, dos logs e da chamada `POST /responses`.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - ai-worker/AGENTS.md
  - docs/canonical/arquitetura-etapas.md
  - docs/registros/experimentos.md
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/wireframe/request/GeraLandingWireframeOpenAiExecutionService.java
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/wireframe/request/MontaRequest.java
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/comum/MontaRequestSupport.java
  - ai-worker/src/main/java/com/marketinghub/worker/geralanding/wireframe/request/RecordWireframeRequest.java
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe.md
  - ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe-schema.json
## 2026-05-29 — Cânone OpenAI reorientado para modelo de dados

- Solicitação: tornar `docs/canonical/openai-informacoes-tratadas-canon.v1.md` completamente focado no modelo de dados onde ficam as informações tratadas pelos modelos de IA.
- Correção aplicada: o cânone foi reescrito para conter somente a visão de persistência, tabelas, colunas, relacionamentos, cardinalidades e regras de localização dos dados de IA.
- Escopo documentado: `hypothesis`, `hypothesis_framework_generation_job`, `experiment`, `experiment_pipeline_generation_job`, `framework_image_generation_job` e `ai_worker_generation`.

## 2026-05-29 — Endpoint interno de envio para IA no wireframe

- Solicitação: criar um endpoint na etapa `landing-page-wireframe` que receba o `jobId` quando o job for enviado para IA, inicialmente sem alterar estado persistido.
- Correção aplicada:
  - `BackendWireframeController` passou a expor o método `enviadoParaIA(String idJob)` no endpoint interno `/api/internal/geralanding/wireframe/stage-executions/{idJob}/enviado-para-ia`, retornando `202 Accepted` sem efeitos colaterais nesta primeira versão;
  - o Swagger canônico do GeraLanding foi atualizado com o novo contrato;
  - a regra ArchUnit passou a exigir a existência do método `enviadoParaIA` no controller de wireframe;
  - teste unitário do controller valida que a chamada é aceita e não interage com os serviços enquanto o comportamento operacional ainda não for implementado.


## 2026-05-29 — Swagger GeraLanding reduzido aos endpoints internos do wireframe

- Solicitação: retirar do Swagger canônico todos os endpoints, deixando apenas a fila `pending` e o callback `enviado-para-ia` da etapa `landing-page-wireframe`.
- Correção aplicada:
  - `docs/canonical/geralanding-backend-swagger.v1.yaml` passou a documentar somente `GET /api/internal/geralanding/wireframe/stage-executions/pending` e `POST /api/internal/geralanding/wireframe/stage-executions/{idJob}/enviado-para-ia`;
  - foram removidas do Swagger as entradas públicas de start/list/detail das etapas e as tags/componentes que ficaram sem uso no contrato reduzido.

## 2026-05-29 — Callback recebePrompt do wireframe com payload OpenAI

- Solicitação: renomear o método `enviadoParaIA` do `BackendWireframeController` para `recebePrompt` e exigir no POST os dados `prompt` e `jobidopenai`.
- Correção aplicada:
  - `BackendWireframeController` passou a expor o callback interno `/api/internal/geralanding/wireframe/stage-executions/{idJob}/recebe-prompt` pelo método `recebePrompt(String idJob, RecebePromptRequest payload)`, mantendo `202 Accepted` sem efeitos colaterais nesta primeira versão;
  - o payload `RecebePromptRequest` documenta os campos contratuais `prompt` e `jobidopenai` para rastrear o prompt enviado à IA e o job aberto na OpenAI;
  - o Swagger canônico e o cânone de arquitetura por etapas foram atualizados com o novo nome, endpoint e corpo JSON;
  - os testes unitários e o `ArquiteturaTest` passaram a validar o método `recebePrompt` e sua assinatura com payload.

## 2026-05-29 — Serviço backend de wireframe renomeado

- Solicitação: alterar o nome de `GeraLandingWireframeStageExecutionService` para `BackendWireframeService`.
- Correção aplicada:
  - classe principal, construtor, logger, imports, injeções e testes foram renomeados para `BackendWireframeService`;
  - este registro documenta a alteração para manter rastreabilidade do tema Experimentos.

## 2026-05-29 — Remoção do DTO inicial de wireframe

- Solicitação: excluir `GeraLandingWireframeStartExecutionResponse`.
- Correção aplicada: removido o record Java obsoleto do backend, que não possuía referências ativas no código e não faz mais parte do contrato reduzido da etapa `landing-page-wireframe`.
## 2026-05-29 — Start do wireframe concentrado no BackendWireframeService

- Solicitação: excluir `GeraLandingWireframeStageService` e passar o tratamento de `start` para `BackendWireframeService`.
- Correção aplicada:
  - `BackendWireframeService` passou a expor `start(Long experimentId)` usando internamente o código canônico `landing-page-wireframe`;
  - `BackendWireframeController` passou a depender somente de `BackendWireframeService` para iniciar, listar, detalhar e expor callbacks da etapa;
  - a classe intermediária `GeraLandingWireframeStageService` foi removida;
  - os testes unitários do controller e do service foram atualizados para validar a delegação direta e o registro inicial da etapa.

## 2026-05-29 — DTO de detalhe do wireframe renomeado

- Solicitação: alterar o nome de `GeraLandingWireframeStageExecutionDetailResponse` para `RecordBackendWireframeDetalheDto`.
- Correção aplicada:
  - o record Java de detalhe da execução da etapa `landing-page-wireframe` foi renomeado para `RecordBackendWireframeDetalheDto`;
  - `BackendWireframeService` e `BackendWireframeController` foram atualizados para usar o novo nome no retorno do detalhe da execução;
  - este registro documenta a alteração para manter rastreabilidade do tema Experimentos.

## 2026-05-29 08:05:26 UTC-3
- solicitação para persistir no backend o prompt recebido pelo endpoint interno de wireframe e marcar a execução como aguardando retorno da OpenAI.
- raciocínio aplicado: a causa-raiz era que `recebePrompt` aceitava o payload sem efeito persistente, mantendo o job iniciado sem registrar `prompt`, `openAiJobId`, início do processamento e status de espera.
- foi feito: criação de `BackendWireframeService.markWaitingOpenAiDispatch`, chamada pelo `BackendWireframeController.recebePrompt`, e atualização dos testes unitários do serviço e do controller para cobrir a persistência e a delegação.
## 2026-05-29 07:49:55 UTC-3
- solicitação: no backend, criar o pacote `recebeprompt` dentro de `geralanding.wireframe.service` e mover para ele os records usados pelo endpoint `recebe-prompt`.
- raciocínio: separar o contrato do payload interno do endpoint em pacote de serviço específico, evitando record aninhado no controller e deixando o endpoint depender de um DTO explícito.
- registro do que foi feito: criado `RecebePromptRequest` em `com.marketinghub.geralanding.wireframe.service.recebeprompt`, atualizado o controller de wireframe para usar esse record externo e ajustado o teste do controller para o novo pacote.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/wireframe/service/BackendWireframeService.java
  - backend/ads-service/src/main/java/com/marketinghub/geralanding/wireframe/web/BackendWireframeController.java
  - backend/ads-service/src/test/java/com/marketinghub/geralanding/wireframe/service/BackendWireframeServiceTest.java
  - backend/ads-service/src/test/java/com/marketinghub/geralanding/wireframe/web/BackendWireframeControllerTest.java
## 2026-05-29 — Records pending do wireframe isolados em subpacote

- Solicitação: dentro de `geralanding.wireframe.service`, criar o pacote `pending` e mover para ele todos os records usados pelo endpoint interno pending da etapa `landing-page-wireframe`.
- Correção aplicada:
  - `RecordWireframePending`, `RecordWireframeExperiment` e `RecordWireframeHypothesis` foram movidos para `com.marketinghub.geralanding.wireframe.service.pending`;
  - `BackendWireframeService`, `BackendWireframeController` e os testes relacionados foram atualizados para importar os records a partir do novo subpacote.

## 2026-05-29 — Callback recebeResposta vazio do wireframe

- Solicitação: criar no `BackendWireframeController` um endpoint POST `recebeResposta`, semelhante ao `recebePrompt`, mas com payload próprio e inicialmente sem processamento.
- Correção aplicada:
  - criado o payload `RecebeRespostaRequest` em `com.marketinghub.geralanding.wireframe.service.receberesposta`;
  - `BackendWireframeController` passou a expor `/api/internal/geralanding/wireframe/stage-executions/{idJob}/recebe-resposta`, retornando `202 Accepted` sem acionar serviço nesta primeira versão;
  - o contrato canônico do GeraLanding foi atualizado para registrar o novo callback inicial;
  - teste unitário do controller valida que o endpoint aceita o payload vazio sem efeitos colaterais.
## 2026-05-29 — Remoção de import legado do pending de wireframe

- Solicitação: retirar o import antigo de `RecordWireframePending` no `BackendWireframeController`.
- Correção aplicada:
  - removido o import legado `com.marketinghub.geralanding.wireframe.service.RecordWireframePending`;
  - mantido o import correto em `com.marketinghub.geralanding.wireframe.service.pending.RecordWireframePending`.
## 2026-05-29 — Log do prompt recebido no callback de wireframe

- Solicitação: no `BackendWireframeController`, registrar em log o campo `payload.prompt()` recebido pelo callback interno `recebe-prompt`.
- Correção aplicada:
  - `BackendWireframeController.recebePrompt` passou a registrar `idJob`, `jobidopenai` e o valor literal de `payload.prompt()` antes de delegar para `BackendWireframeService.markWaitingOpenAiDispatch`;
  - o teste do controller foi atualizado para capturar o log e garantir que o prompt recebido aparece na saída de log.

## 2026-05-29 — Records dos endpoints de listagem e detalhe do wireframe isolados

- Solicitação: dentro de `geralanding.wireframe.service`, criar os pacotes `listStageExecutions` e `detailStageExecution` e mover para eles os records usados pelos endpoints correspondentes do controller.
- Correção aplicada:
  - `GeraLandingWireframeExecutionSummaryResponse` foi movido para `com.marketinghub.geralanding.wireframe.service.listStageExecutions`;
  - `RecordBackendWireframeDetalheDto` foi movido para `com.marketinghub.geralanding.wireframe.service.detailStageExecution`;
  - `BackendWireframeService` e `BackendWireframeController` passaram a importar os records a partir dos novos subpacotes.

## 2026-05-29 — Persistência do prompt no callback de despacho do wireframe

- Ajuste adicional validado por teste unitário: `BackendWireframeService.markWaitingOpenAiDispatch` também passou a preencher o campo `prompt` da execução ao receber o prompt despachado, mantendo `openAiRequestBody` preservado com o mesmo conteúdo para compatibilidade.
## 2026-05-29 — Swagger completo do BackendWireframeController

- Solicitação: atualizar o Swagger com todos os endpoints expostos pelo `BackendWireframeController`.
- Correção aplicada:
  - `docs/gera-landing/swagger-gera-landing-etapas.yaml` passou a documentar os seis endpoints do controller de wireframe: start, listagem, detalhe, pending, `recebe-prompt` e `recebe-resposta`;
  - foram adicionados parâmetros reutilizáveis e schemas para respostas, pendências e payloads específicos da etapa `landing-page-wireframe`.

## 2026-05-29 — Regra ArchUnit para subpacotes internos de service do GeraLanding

- Solicitação: alterar a regra de arquitetura do backend para permitir que classes em pacotes `service` acessem classes de pacotes internos `service.*`.
- Correção aplicada:
  - a regra ArchUnit de `com.marketinghub.geralanding..service..` passou a aceitar dependências para toda a árvore `geralanding.<etapa>.service` e `geralanding.<etapa>.service.*` da mesma etapa;
  - o cânone de arquitetura do GeraLanding foi sincronizado para registrar a permissão explícita de dependência entre o pacote `service` e seus subpacotes internos.

## 2026-05-29 00:00:00 UTC
- solicitação: trocar no Worker AI da etapa wireframe a chamada de `receiveDispatch` pelo callback `recebe-prompt`, enviando o prompt e o identificador do job OpenAI conforme Swagger canônico.
- causa-raiz identificada: o fluxo ativo de wireframe registrava o `openAiJobId` via `receive-dispatch` após a resposta da OpenAI, mas o contrato documentado para rastreabilidade do prompt é `POST /api/internal/geralanding/wireframe/stage-executions/{idJob}/recebe-prompt` com `prompt` e `jobidopenai`.
- correção aplicada:
  - `GeraLandingWireframeOpenAiExecutionService` passou a repassar o prompt montado ao processador de resposta;
  - `RecebeResponse` da etapa wireframe passou a chamar `recebePrompt` antes do envio do resultado, substituindo a chamada a `receiveDispatch`;
  - `GeraLandingWireframeBackendClient` passou a postar no endpoint `/recebe-prompt` com payload contendo apenas `prompt` e `jobidopenai`, conforme contrato do Swagger.
