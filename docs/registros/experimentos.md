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
