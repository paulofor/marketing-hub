# Procedimento Canônico de Execução de Experimento v1

## 1. Objetivo

Padronizar o funcionamento do experimento de ponta a ponta (criação, geração de artefatos, aprovação e publicação), garantindo rastreabilidade operacional, alinhamento com o código vigente e foco no resultado comercial do sistema.

Este documento consolida:
- a ordem operacional nas telas;
- as etapas do pipeline de experimento;
- as etapas do pipeline Gera Landing;
- a relação entre Worker AI, OpenAI (modo batch), Lead Portal e publicação final;
- observabilidade de entradas/saídas e custos por experimento.

## 2. Escopo funcional

Aplica-se ao fluxo de experimentos no Marketing Hub, especialmente:
- Backend (`backend/ads-service`);
- Worker AI (`ai-worker`);
- Frontend administrativo de Experimentos;
- publicação final de landing no Lead Portal.

## 3. Fluxo macro do experimento (visão operacional)

### 3.1 Criação do experimento
1. O usuário cria o experimento pela tela, preenchendo os campos obrigatórios do formulário.
2. Após salvar, o experimento passa a ter contexto para geração de ativos do pipeline.

### 3.2 Execução no detalhe do experimento
1. O usuário entra no detalhe do experimento.
2. A execução acontece por etapas guiadas, com geração e validação progressiva dos artefatos.

## 4. Pipeline de experimento (núcleo inicial)

A sequência canônica de seções do pipeline inclui:
1. `CAMPAIGN_ANGLE` (ângulo da campanha)
2. `AD_COPY` (ad copy)
3. `AD_IMAGE_BRIEFING` (briefing da imagem do anúncio)

Essas seções e suas dependências fazem parte do enum oficial do backend.

### 4.1 Prompts dessas etapas
Os prompts do pipeline ficam versionados no repositório, em `resources` do Worker AI.

Local canônico vigente:
- pipeline de experimento (núcleo inicial): `ai-worker/src/main/resources/prompts/experiment`;
- pipeline de landing no Worker AI: prompts e schemas devem ser resolvidos por configuração tipada da etapa em `openai.core.<etapa>`; o caminho físico em `resources/prompts/<dominio>` é detalhe de recurso versionado e não define namespace Java do Worker AI.

## 5. Pipeline Gera Landing (núcleo da landing)

No fluxo atual, a geração da landing segue as etapas:
1. gerar wireframe (`LANDING_PAGE_WIREFRAME`);
2. gerar copy (`LANDING_PAGE_COPY`);
3. gerar planejamento/prompt de imagens (`LANDING_PAGE_IMAGE_PLANNING`) e gerar imagens, materializando `experiment.landing_page_image_assets` com as URLs finais;
   - ao reexecutar manualmente `LANDING_PAGE_IMAGE_PLANNING`, o backend deve zerar o planejamento anterior, o manifesto `experiment.landing_page_image_assets` e os jobs de imagem já associados ao experimento antes de registrar o novo job;
4. gerar preset de design (`LANDING_PAGE_DESIGN_PRESET`);
5. gerar entregável HTML da landing (`LANDING_PAGE_HTML`).

### 5.1 Observação obrigatória — HTML provisório por etapa
Durante o pipeline de Gera Landing, existe produção incremental/provisória de conteúdo para permitir evolução etapa a etapa. No estágio de design preset é consolidada a base visual e ocorre a etapa usada para ingestão do pixel no fluxo atual.

### 5.2 Instrumentação obrigatória de funil no assembler do design preset
Para a etapa `LANDING_PAGE_DESIGN_PRESET`, o assembler de HTML provisório deve injetar instrumentação mínima de comportamento para diagnóstico de avanço de funil na landing:
1. disparo de `page_view` no carregamento da página;
2. marcação explícita das seções monitoráveis (`data-track-section` derivado de `data-section-id`/`id`);
3. medição de tempo de visualização por seção usando `IntersectionObserver` com critério de visibilidade (>= 50%);
4. emissão de evento consolidado por seção (`section_view_time`) com `sectionId` e `elapsedMs` sempre que a seção deixa de ficar visível, quando a aba fica oculta e no `beforeunload`.

Regras complementares:
- a instrumentação deve ser idempotente (não pode ser injetada em duplicidade no mesmo HTML);
- o payload publicado no artefato final deve manter apenas campos/eventos previstos em contrato canônico, sem metadado técnico fora do escopo funcional.


### 5.3 Quadro operacional — etapas, assembler de HTML e persistência de HTML provisório

| Etapa | Classe que faz assembler do HTML da etapa | Campo(s) de tabela onde grava HTML provisório |
|---|---|---|
| `LANDING_PAGE_WIREFRAME` | `WireframeProvisionalHtmlAssembler` | `gera_landing_stage_execution.provisional_html` |
| `LANDING_PAGE_COPY` | `CopyProvisionalHtmlAssembler` | `gera_landing_stage_execution.provisional_html` |
| `LANDING_PAGE_IMAGE_PLANNING` | `ImagePlanningProvisionalHtmlAssembler` (usa internamente `CopyProvisionalHtmlAssembler` + `LandingPageImageInjector` apenas para esta etapa). | `gera_landing_stage_execution.provisional_html` (não persiste em `experiment.landing_page_html` nesta etapa). |
| `LANDING_PAGE_DESIGN_PRESET` | `DesignPresetProvisionalHtmlAssembler` + `LandingPageImageInjector.injectImages(...)` | `gera_landing_stage_execution.provisional_html`, `experiment.landing_page_design_preset` (JSON bruto da resposta do modelo) e `experiment.html_geralanding` (HTML consolidado da etapa). `experiment.landing_page_html` só é persistido na aprovação/publicação. |

### 5.4 Regra de isolamento por conjunto (obrigatória)

Cada conjunto de montagem de HTML deve atuar **exclusivamente** na sua etapa canônica, com pacote dedicado dentro de `com.marketinghub.geralanding`:

- `com.marketinghub.geralanding.wireframe` → etapa `LANDING_PAGE_WIREFRAME`
  - `WireframeProvisionalHtmlAssembler`
  - `WireframeHtmlGenerator`
- `com.marketinghub.geralanding.copy` → etapa `LANDING_PAGE_COPY`
  - `CopyProvisionalHtmlPayloadResolver`
  - `CopyProvisionalHtmlProcessor`
  - `CopyProvisionalHtmlAssembler`
- `com.marketinghub.geralanding.presetdesign.provisorio` → HTML provisório da etapa `LANDING_PAGE_DESIGN_PRESET`
  - `DesignPresetProvisionalHtmlProcessor`
  - `DesignPresetProvisionalHtmlAssembler`
- `com.marketinghub.geralanding.imageplanning` → etapa `LANDING_PAGE_IMAGE_PLANNING`
  - `ImagePlanningProvisionalHtmlAssembler`

Regras:
1. Um conjunto de etapa não pode consolidar regras de outra etapa.
2. Enriquecimentos transversais (ex.: injeção de URLs finais de imagem) devem ocorrer em serviço auxiliar dedicado e orquestrados pelo serviço da etapa, sem transferir a responsabilidade de etapa entre processadores.
3. A etapa de geração de imagens deve persistir o manifesto consolidado `experiment.landing_page_image_assets`; a etapa de preset design deve consumir esse manifesto para substituir placeholders/URLs provisórias por URLs finais antes de persistir o HTML.

### 5.5 Worker AI — divisão equivalente por etapa (obrigatória)

No `ai-worker`, a mesma divisão por etapa deve ser mantida para evitar acoplamento entre execução,
prompt e schema. A arquitetura canônica vigente para qualquer etapa de landing no Worker AI é o núcleo
`com.marketinghub.worker.openai.core.<etapa>`. Não existe modelo canônico ativo em namespace Java
específico de GeraLanding dentro do Worker AI.

- Cada etapa deve residir em `com.marketinghub.worker.openai.core.<etapa>` e usar `StageWorker`,
  `StageBackendPort`, `StagePromptBuilder`, `StageResponseValidator`, `StageResponseHandler` e
  `OpenAiClientPort`.
- Scheduler, propriedades, adapters de backend, prompt builder, validador e handler devem ser beans
  declarados pela configuração explícita da própria etapa, sem depender de services genéricos do antigo
  fluxo de landing.
- O antigo namespace Java de landing do Worker AI é legado, não deve receber novas classes e não
  deve ser usado como referência arquitetural; qualquer remanescente deve ser tratado como débito de
  migração para `openai.core.<etapa>`.
- Cada etapa do `openai.core` deve resolver seu próprio prompt/schema por configuração tipada e por
  contratos de input/output próprios.

Regra operacional: a seleção de schema/prompt por etapa no worker deve ocorrer por definição de etapa
explícita, sem ifs ad-hoc espalhados no serviço de execução. Etapas novas ou remanescentes devem entrar
diretamente no padrão `openai.core.<etapa>`.

## 6. Geração e aprovação de anúncios

Após os artefatos de base:
1. gera anúncios com IA;
2. usuário realiza aprovação operacional dos anúncios.

## 7. Aba Landing: visualização, aprovação e URL de campanha

Com a landing gerada:
1. o usuário acessa a aba **Landing** no experimento;
2. faz visualização e aprovação/publicação;
3. nesse ponto é consolidada a URL final usada na campanha.

## 8. Regras de integração com OpenAI

### 8.1 Worker AI como camada obrigatória
Toda chamada para OpenAI no contexto deste fluxo é mediada pelo Worker AI. O frontend e demais módulos não devem chamar OpenAI diretamente para essas etapas.

### 8.2 Modo de processamento OpenAI
No fluxo de **Gera Landing**, a integração canônica com OpenAI deve usar **Flex processing** na API de `responses`, definindo `service_tier=flex` em cada requisição do Worker AI.

Para outros fluxos que ainda usam lote assíncrono (ex.: alguns jobs de criativos/imagem), o modo batch continua permitido quando o contrato operacional exigir processamento em arquivo JSONL com polling.

Regras obrigatórias do modo Flex no Gera Landing:
- usar endpoint síncrono `/v1/responses` com `service_tier=flex`;
- configurar timeout de cliente compatível com latência maior do Flex;
- tratar indisponibilidade de capacidade (`429`) como falha de integração com contexto operacional em log.

## 9. Publicação da landing

A publicação do HTML final da landing ocorre no Lead Portal, com integração feita pelo fluxo de backend/worker.

### 9.1 O que acontece depois de clicar em "Aprovar e publicar landing"

Classe responsável no backend: `GeraLandingStageExecutionService` (método `approveAndPublishLanding`).

Fluxo obrigatório executado após a aprovação:
1. carregar o experimento e resolver o HTML base para publicação (priorizando `experiment.html_geralanding`; fallback legado para `experiment.landing_page_html`);
2. injetar instrumentação de tracking comportamental no HTML publicado (`data-track-section` + script `data-mh-funnel-tracking`);
3. injetar controles de funil (`data-mh-funnel-controls`);
4. resolver `facebookPixelId` do nicho do experimento e injetar snippet do Facebook Pixel quando elegível;
5. publicar o flow no Lead Portal via `PUT /api/flows/{slug}` com payload contendo `slug`, `name`, `description` e `customFormHtml`;
6. resolver URLs finais de publicação (`iframe` e `standalone`) e persistir no experimento a `follow_up_action_url`.

Regras adicionais:
- a injeção de tracking deve ser idempotente (não duplicar quando já existir no HTML);
- falhas de contrato na publicação para Lead Portal devem ser tratadas pela exception canônica de violação de contrato do GeraLanding.

## 10. Custos e mensuração por experimento

As solicitações OpenAI do fluxo são mensuradas, registradas e totalizadas no contexto do experimento, permitindo acompanhamento financeiro por geração.

## 11. Transparência operacional para o usuário

A interface de Experimentos deve manter abas/visões que permitam acompanhar, de forma organizada:
- o que foi enviado para geração;
- o que foi recebido;
- status das etapas;
- facilidade de copiar/baixar artefatos.

## 12. Checklist canônico de execução

1. Criar experimento na tela.
2. Entrar no detalhe e executar etapas do pipeline inicial (`campaign angle` → `ad copy` → `ad image briefing`).
3. Executar etapas do Gera Landing (`wireframe` → `copy` → `image planning` → `image generation` → `design preset` → `landing html`).
4. Gerar anúncios com IA.
5. Aprovar anúncios.
6. Ir para aba Landing, revisar/aprovar/publicar landing e confirmar URL final para campanha.
7. Validar custos/telemetria da geração no experimento.

## 13. Fonte de verdade técnica (código)

- Ordem/seções do pipeline no backend: `ExperimentPipelineSection`.
- Clientes OpenAI do pipeline e Gera Landing no Worker AI.
- Prompts versionados no Worker AI em `src/main/resources/prompts/...`.
- Informações tratadas pela OpenAI, incluindo framework de hipótese e atributos do experimento: `docs/canonical/openai-informacoes-tratadas-canon.v1.md`.
- Endpoint de aprovação/publicação de landing no módulo Gera Landing do backend.

## 14. Governança de evolução deste cânone

Quando houver alteração de regra operacional (ordem de etapas, critérios de aprovação, publicação, modo OpenAI, custos ou observabilidade), este documento deve ser atualizado imediatamente junto com:
- o documento canônico de artefatos aplicável;
- testes e contratos do backend/worker afetados.

## 15. Unificação canônica do Gera Landing (vigente em 2026-05-23)

Este documento passa a ser a **única fonte canônica** para definição operacional do Gera Landing e do procedimento de experimento.

- Documento descontinuado: `docs/gera-landing/modelo-canonico-gera-landing.md` (mantido fora de uso e removido da referência ativa).
- Toda nova regra de etapa, contrato, máquina de estados, publicação e auditoria do Gera Landing deve ser registrada somente em `/docs/canonical`.

### 15.1 Ordem canônica Total Gera Landing

1. `landing-page-wireframe`
2. `landing-page-copy`
3. `landing-page-image-planning`
4. `landing-page-image-generation`
5. `landing-page-design-preset`
6. `landing-page-deliverables`

### 15.2 Regra mandatória para HTML provisório da etapa preset design

Para a etapa `landing-page-design-preset`, o HTML provisório **deve** ser gerado pelo `DesignPresetProvisionalHtmlAssembler` e persistido na execução da etapa.

Persistência esperada na conclusão da etapa:
- `gera_landing_stage_execution.provisional_html` recebe o HTML provisório da etapa;
- `experiment.landing_page_design_preset` recebe o JSON de resposta da etapa;
- `experiment.html_geralanding` recebe o HTML consolidado da etapa com imagens resolvidas a partir de `experiment.landing_page_image_assets` quando disponível.

`experiment.landing_page_html` permanece reservado para fluxo de aprovação/publicação final.

### 15.3 Regra de validação de implementação

Qualquer correção de falha onde o preset design não gerar HTML provisório deve validar, no código, estes pontos mínimos:
1. orquestração da etapa `landing-page-design-preset` no `GeraLandingStageExecutionService`;
2. uso explícito do `DesignPresetProvisionalHtmlAssembler` na montagem do HTML;
3. persistência do resultado em `provisional_html` da execução e em `experiment.html_geralanding`;
4. cobertura por teste unitário da etapa garantindo que o HTML é produzido/persistido.



### 15.4 Diagramas arquiteturais por módulo (base ArchUnit) — Gera Landing

Os diagramas canônicos de arquitetura do GeraLanding (backend e worker ai), derivados das regras ArchUnit, ficam centralizados em:

- `docs/canonical/geralanding-arquitetura-canon.v1.md`

> Observação: manter este procedimento como referência de fluxo e o documento acima como referência primária de arquitetura do módulo GeraLanding.

### 15.5 Regra mandatória — bloqueio de metainstrução na copy final

É obrigatório bloquear a publicação quando qualquer campo textual final da landing contiver metainstrução ou texto técnico operacional (ex.: instruções de montagem, placeholders, notas para operador/IA como "preciso do targetSectionId real").

Critérios mínimos de bloqueio:
1. validar campos de texto final (incluindo `bodySections[*].items[*].texto`) antes da persistência/publicação;
2. lançar erro explícito de contrato com caminho do campo e conteúdo literal rejeitado;
3. tratar a falha como causa-raiz de geração (prompt/mapper/validador), proibindo correção manual ad hoc no payload publicado.

Mensagem padrão recomendada: `IllegalStateException: Copy inválida: vazamento de metainstrução/texto técnico detectado em <campo>=<valor>`.

### 15.6 Regra mandatória — pouco esforço na copy da landing

A etapa `landing-page-copy` deve aplicar o princípio de pouco esforço em todos os campos textuais finais: o usuário não quer trabalhar para entender a comunicação da página. A copy deve ser clara em leitura rápida, respeitar os limites `tamMinimo`/`tamMaximo` definidos pelo wireframe, evitar explicações longas sem necessidade, não multiplicar escolhas de ação e conduzir naturalmente para o CTA ou próximo passo previsto na estrutura do wireframe.

Essa regra não autoriza criar novos blocos, seções, FAQs, CTAs ou metadados: o wireframe permanece a única fonte de verdade estrutural, e o princípio de pouco esforço deve ser aplicado somente dentro dos textos que o wireframe já solicitou.

