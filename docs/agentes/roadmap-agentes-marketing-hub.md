# Roadmap Técnico por Sprint — Camada de Agentes do Marketing Hub

## 1. Objetivo

Este documento define um plano técnico por sprint para implementar uma **camada de agentes baseada em IA + sandbox** sobre o projeto **Marketing Hub**, aproveitando a base já existente do sistema.

A meta não é reconstruir o produto, e sim:

- encapsular os serviços atuais como **tools**;
- introduzir uma camada de **orquestração agentic**;
- habilitar **aprovação humana** para ações sensíveis;
- fechar o ciclo de **experimentação → execução → medição → melhoria contínua**.

---

## 2. Estado atual do projeto

O Marketing Hub já possui uma base funcional relevante. Pelo repositório e pelo modelo de dados atual, o sistema já cobre boa parte da operação de marketing e fulfillment:

### 2.1 Módulos já presentes no repositório

- `backend`
- `frontend`
- `ai-worker`
- `facebook-ads-worker`
- `email-service`
- `image-watermark-service`
- `image-zipper-service`
- `lead-portal`
- `lead-portal-payments-service`
- `market-research-service`
- `institutional-site`
- `deploy`
- `docs`
- `AGENTS.md`

### 2.2 Capacidades já evidenciadas

- gestão de nichos, hipóteses e experimentos;
- geração e gestão de criativos;
- publicação assistida em Meta Ads;
- lead portal com perguntas configuráveis;
- geração de pacotes de imagens personalizadas;
- aplicação de marca d’água;
- compactação/entrega de pacotes;
- e-mail transacional;
- pagamento e webhooks;
- analytics de campanha;
- workers especializados.

### 2.3 Evidências do modelo de dados

O snapshot atual do banco indica operação real, com registros em entidades como:

- `experiment`
- `hypothesis`
- `creative`
- `facebook_ads_campaign`
- `facebook_ads_ad_set`
- `facebook_ads_ad`
- `flow_submissions`
- `flow_submission_image_package`
- `lead_portal_purchase`
- `lead_portal_premium_delivery`
- `email_log`
- `mercadopago_webhook_log`
- `ai_worker_generation`

---

## 3. Diretrizes de arquitetura

### 3.1 Estratégia principal

A implementação será feita como uma **camada de agentes sobre os serviços existentes**, e não como uma nova aplicação paralela.

### 3.2 Base recomendada

- **Responses API** como interface principal para orquestração de chamadas, tools nativas e function calling.
- **Agents SDK** para estruturar agentes especializados, handoffs e tracing.
- **Tools customizadas** para encapsular as capacidades atuais do Marketing Hub.
- **Sandbox** para tarefas determinísticas: análise de dados, composição de arquivos, processamento de lotes, avaliação e geração de artefatos auxiliares.

### 3.3 Princípios operacionais

1. **Reaproveitar antes de reconstruir**.
2. **Tudo importante vira tool**.
3. **Toda ação crítica exige aprovação humana**.
4. **Todo output precisa de lineage**:
   - hipótese
   - experimento
   - prompt
   - modelo
   - tool
   - artefato gerado
5. **Toda melhoria precisa ser mensurável**.
6. **Nenhum agente publica em produção sem guardrails**.

### 3.4 Ações com aprovação humana obrigatória

- publicar campanha paga;
- alterar orçamento;
- alterar preço;
- disparar email em massa;
- liberar entrega final ao cliente;
- reemitir entrega paga;
- emitir reembolso;
- sobrescrever prompts ativos vencedores;
- ativar mudanças globais em produção.

---

## 4. Arquitetura-alvo dos agentes

Serão implantados 4 agentes especializados.

### 4.1 Agente Estrategista
Responsável por:
- analisar nicho;
- gerar ou revisar hipótese;
- propor promessa, mecanismo e diferenciação;
- montar plano de experimento;
- definir metas e métricas.

### 4.2 Agente Criativo
Responsável por:
- gerar copy;
- gerar prompts de imagem;
- gerar criativos;
- gerar landing e email draft;
- montar amostras personalizadas.

### 4.3 Agente Operador
Responsável por:
- acionar serviços existentes;
- criar rascunhos de campanha;
- publicar fluxos aprovados;
- enviar email de amostra;
- criar checkout;
- acionar fulfillment após pagamento.

### 4.4 Agente Analista
Responsável por:
- consolidar métricas do funil;
- comparar variantes;
- detectar gargalos;
- sugerir novo experimento;
- alimentar backlog de melhoria contínua.

---

## 5. Estratégia de entrega

### Duração sugerida
- **5 sprints**
- **2 semanas por sprint**
- total estimado: **10 semanas**

### Milestones
- **Fim do Sprint 1:** camada inicial de tools e governança
- **Fim do Sprint 2:** agente Estrategista funcional
- **Fim do Sprint 3:** agente Criativo funcional
- **Fim do Sprint 4:** agente Operador funcional com aprovação humana
- **Fim do Sprint 5:** agente Analista + loop de melhoria contínua

---

# 6. Plano por sprint

## Sprint 1 — Tooling Layer, governança e tracing

### Objetivo
Transformar os módulos existentes do Marketing Hub em uma **camada de tools consistente**, com observabilidade, autenticação e aprovação humana.

### Problema que este sprint resolve
Hoje o sistema já executa partes importantes do fluxo, mas os agentes ainda não têm uma interface formal e confiável para operar sobre essas capacidades.

### Escopo
- criar o serviço ou módulo `agent-control`;
- definir o catálogo inicial de tools;
- padronizar contrato de entrada e saída das tools;
- implementar autenticação/autorização entre agente e serviços;
- implementar `trace_id` por execução;
- implementar fila de aprovação humana;
- padronizar logging de tool call, resultado, erro e custo.

### Épicos

#### Épico 1 — Registro de tools
Criar um catálogo central com:
- nome da tool;
- domínio;
- serviço de destino;
- schema de input;
- schema de output;
- nível de risco;
- necessidade de aprovação.

#### Épico 2 — Gateway agentic
Criar uma camada que receba a decisão do agente e encaminhe a chamada para o serviço correto.

#### Épico 3 — Aprovação humana
Criar um fluxo para:
- pendenciar ações críticas;
- mostrar diff/contexto;
- aprovar ou rejeitar;
- registrar auditoria.

#### Épico 4 — Tracing e observabilidade
Garantir rastreabilidade de:
- agente;
- ferramenta usada;
- inputs;
- outputs;
- custo;
- latência;
- erro.

### Backlog sugerido
- [ ] Definir contrato base `ToolDefinition`
- [ ] Criar tabela ou storage de catálogo de tools
- [ ] Criar `tool_call_log`
- [ ] Criar `agent_run`
- [ ] Criar `approval_request`
- [ ] Criar `approval_decision`
- [ ] Criar middleware de autenticação entre agentes e serviços
- [ ] Adicionar `trace_id` em requisições internas
- [ ] Padronizar resposta de erro
- [ ] Criar dashboard operacional de execuções

### Tools desta fase
- `get_niche`
- `get_hypothesis`
- `get_experiment`
- `get_experiment_metrics`
- `get_creatives`
- `get_lead_portal_flow`
- `get_submissions`
- `get_image_packages`
- `get_purchases`
- `request_human_approval`

### Entregáveis
- gateway de tools funcional;
- catálogo versionado de tools;
- auditoria de execuções;
- aprovação humana operacional;
- tracing ponta a ponta para chamadas agentic.

### Critérios de aceite
- um agente consegue chamar pelo menos 5 tools reais;
- cada execução recebe `trace_id` único;
- uma ação crítica não executa sem aprovação humana;
- falhas ficam registradas com contexto suficiente para diagnóstico.

### Dependências
- acesso aos serviços existentes;
- definição de autenticação interna;
- disponibilidade de ambiente de homologação.

### Riscos
- contratos inconsistentes entre microserviços;
- ausência de ids correlacionáveis entre serviços;
- ações críticas sem interface clara de revisão.

### Definição de pronto
- tool catalog publicado;
- logs e approvals funcionando;
- integração piloto com serviços reais validada.

---

## Sprint 2 — Agente Estrategista

### Objetivo
Construir o agente responsável por transformar nicho + repertório existente em **hipóteses e experimentos operáveis**.

### Problema que este sprint resolve
O projeto já possui nichos, hipóteses, templates de jornada e produtos de sucesso, mas falta uma camada que sintetize esses insumos e proponha experimentos novos de forma consistente.

### Escopo
- criar o agente Estrategista;
- conectar o agente às entidades de nicho, hipótese e jornada;
- gerar hipótese estruturada;
- gerar experimento com metas;
- associar métrica preset e template de jornada;
- registrar justificativa da decisão do agente.

### Épicos

#### Épico 1 — Contexto estratégico
Ler e consolidar dados vindos de:
- `market_niche`
- `niche_detailed_description`
- `hypothesis`
- `success_product`
- `journey_template`
- `metric_preset`

#### Épico 2 — Geração de hipótese
Gerar:
- persona;
- problema;
- promessa;
- mecanismo;
- diferenciação;
- regra de sucesso.

#### Épico 3 — Geração de experimento
Gerar:
- nome do experimento;
- objetivo;
- KPI alvo;
- volume de criativos;
- volume de emails;
- volume de entregáveis;
- template de jornada;
- fluxo previsto.

#### Épico 4 — Score de hipótese
Criar heurística inicial para ranqueamento com base em:
- clareza da promessa;
- tangibilidade da solução;
- facilidade de personalização;
- aderência ao nicho;
- viabilidade operacional.

### Backlog sugerido
- [ ] Criar `create_hypothesis`
- [ ] Criar `score_hypothesis`
- [ ] Criar `create_experiment_plan`
- [ ] Criar `select_journey_template`
- [ ] Criar `define_success_metrics`
- [ ] Criar `explain_strategy_choice`
- [ ] Salvar rationale do agente
- [ ] Permitir revisão humana da hipótese antes da execução

### Tools desta fase
- `search_niches`
- `get_niche_context`
- `get_success_products`
- `create_hypothesis`
- `score_hypothesis`
- `create_experiment_plan`
- `select_journey_template`
- `define_success_metrics`

### Entregáveis
- agente Estrategista funcional;
- hipótese estruturada pronta para revisão;
- experimento criado no formato do sistema atual;
- score de priorização.

### Critérios de aceite
- dado um nicho, o agente propõe pelo menos 3 hipóteses;
- cada hipótese traz promessa, mecanismo e critério de sucesso;
- o agente consegue criar um experimento compatível com o schema atual;
- o usuário consegue aprovar a hipótese escolhida.

### Dependências
- Sprint 1 concluído;
- tools de leitura e gravação disponíveis;
- modelos e instruções iniciais aprovados.

### Riscos
- hipótese bonita, mas pouco operacional;
- pouca consistência na escolha do journey template;
- falta de versionamento do rationale.

### Definição de pronto
- uma hipótese aprovada consegue seguir para geração de artefatos no sprint seguinte.

---

## Sprint 3 — Agente Criativo + versionamento de prompts e artefatos

### Objetivo
Automatizar a criação dos artefatos do experimento usando o que já existe no projeto: criativos, prompts, pacotes de imagem, emails de amostra e fluxos do lead portal.

### Problema que este sprint resolve
O sistema já possui geração por IA e assets, mas ainda não existe uma camada agentic responsável por criar, comparar e versionar sistematicamente os ativos de venda.

### Escopo
- criar o agente Criativo;
- formalizar versionamento de prompts;
- gerar copy de anúncio;
- gerar prompt de imagem;
- gerar criativo draft;
- gerar landing/flow draft;
- gerar email de amostra;
- gerar pacote de amostras com marca d’água.

### Épicos

#### Épico 1 — Prompt lineage
Todo artefato precisa apontar para:
- prompt usado;
- modelo usado;
- input estruturado;
- output gerado;
- custo estimado.

#### Épico 2 — Geração de copy
Criar variações para:
- headline;
- primary text;
- CTA;
- copy da lead page;
- assunto e corpo do email.

#### Épico 3 — Geração de amostras
Usar os serviços e estruturas já existentes para:
- montar prompts personalizados;
- gerar imagens;
- aplicar marca d’água;
- compor pacote de amostra.

#### Épico 4 — Revisão e aprovação criativa
Criar etapa de aprovação para:
- criativo;
- email;
- flow/perguntas;
- pacote de amostra.

### Backlog sugerido
- [ ] Criar `prompt_version`
- [ ] Criar `artifact_lineage`
- [ ] Criar `generate_ad_copy`
- [ ] Criar `generate_creative_prompt`
- [ ] Criar `generate_sample_email`
- [ ] Criar `generate_lead_portal_flow`
- [ ] Criar `render_personalized_samples`
- [ ] Criar `apply_watermark`
- [ ] Criar `approve_artifact`
- [ ] Criar preview consolidado do experimento

### Tools desta fase
- `generate_ad_copy`
- `generate_creative_prompt`
- `generate_creative_variant`
- `generate_sample_email`
- `generate_lead_portal_flow`
- `generate_lead_portal_questions`
- `render_personalized_samples`
- `apply_watermark`
- `zip_sample_package_preview`

### Entregáveis
- agente Criativo funcional;
- prompts versionados;
- criativos draft por experimento;
- email de amostra draft;
- flow draft do lead portal;
- amostras personalizadas com marca d’água.

### Critérios de aceite
- a partir de uma hipótese aprovada, o agente gera todos os artefatos-base do experimento;
- cada artefato aponta para um prompt e um modelo;
- outputs podem ser revisados e aprovados antes de publicação;
- o custo por geração fica registrado.

### Dependências
- Sprint 2 concluído;
- integração funcional com `ai-worker` e serviços de imagem;
- acesso a storage de assets.

### Riscos
- prompts sem versionamento consistente;
- baixa previsibilidade na qualidade visual;
- forte dependência de ajustes manuais se a rubric criativa estiver fraca.

### Definição de pronto
- um experimento aprovado produz um pacote completo de artefatos prontos para operação assistida.

---

## Sprint 4 — Agente Operador

### Objetivo
Conectar a camada agentic à execução real do funil: campanha, flow, email, checkout e fulfillment, mantendo aprovação humana em ações sensíveis.

### Problema que este sprint resolve
Os artefatos podem ser criados, mas ainda falta um operador capaz de acionar o que já existe no ecossistema Marketing Hub para executar o funil ponta a ponta.

### Escopo
- criar o agente Operador;
- publicar criativos e drafts de campanha;
- acionar publicação de lead portal flow;
- acionar envio de email de amostra;
- criar checkout/pagamento;
- reagir à confirmação de pagamento;
- disparar entrega final.

### Épicos

#### Épico 1 — Operação de campanha
- criar rascunhos de campanha/adset/ad;
- subir assets aprovados;
- publicar inicialmente em modo seguro;
- permitir pausar/reativar.

#### Épico 2 — Operação de lead portal
- publicar flow aprovado;
- ativar formulário/perguntas;
- associar experimento ao fluxo.

#### Épico 3 — Operação de email
- renderizar email final com amostras;
- enviar email transacional;
- registrar request id, status e erros.

#### Épico 4 — Operação de pagamento e entrega
- criar checkout;
- aguardar confirmação de pagamento;
- remover restrições de entrega;
- gerar zip final;
- enviar entrega premium.

### Backlog sugerido
- [ ] Criar `publish_campaign_draft`
- [ ] Criar `pause_campaign`
- [ ] Criar `publish_lead_portal_flow`
- [ ] Criar `send_sample_email`
- [ ] Criar `create_checkout`
- [ ] Criar `get_payment_status`
- [ ] Criar `release_final_assets`
- [ ] Criar `resend_delivery`
- [ ] Criar guardrail de orçamento
- [ ] Criar guardrail de preço

### Tools desta fase
- `publish_campaign_draft`
- `pause_campaign`
- `publish_lead_portal_flow`
- `send_sample_email`
- `create_checkout`
- `get_payment_status`
- `release_final_assets`
- `resend_delivery`
- `request_human_approval`

### Entregáveis
- agente Operador funcional;
- campanha draft/publicável;
- email de amostra operacional;
- checkout operacional;
- fulfillment acionado após pagamento;
- reprocessamento assistido de falhas.

### Critérios de aceite
- um experimento aprovado pode ser executado ponta a ponta em homologação;
- nenhuma ação crítica ocorre sem approval;
- pagamento aprovado gera entrega final;
- falhas de envio ou zip ficam observáveis e reprocessáveis.

### Dependências
- Sprint 3 concluído;
- integrações com Meta, email e pagamento acessíveis em homologação;
- webhooks funcionando.

### Riscos
- inconsistência entre status internos e status do provedor externo;
- falha em webhooks sem mecanismo de retry;
- operação real sem guardrails fortes.

### Definição de pronto
- o sistema roda um funil completo com participação dos agentes e aprovação humana.

---

## Sprint 5 — Agente Analista + loop de melhoria contínua

### Objetivo
Fechar o ciclo do Marketing Hub como sistema de melhoria contínua: **medir → diagnosticar → sugerir nova ação → alimentar próximo experimento**.

### Problema que este sprint resolve
Hoje existem sinais de analytics e métricas operacionais, mas falta um agente que consolide tudo isso e transforme dados em decisão de próxima iteração.

### Escopo
- criar o agente Analista;
- consolidar métricas por experimento;
- ler métricas por criativo, campanha, email, checkout e compra;
- detectar gargalo principal;
- sugerir nova hipótese ou nova variante;
- criar backlog de melhoria.

### Épicos

#### Épico 1 — Visão unificada do funil
Consolidar pelo menos:
- impressões;
- cliques;
- CPL;
- submissões;
- amostras geradas;
- emails enviados/abertos;
- checkout iniciado;
- pagamento aprovado;
- entrega concluída.

#### Épico 2 — Diagnóstico de gargalos
Regras iniciais:
- CTR baixo → campanha/criativo/mensagem;
- submissão baixa → landing/flow/pergunta;
- abertura boa e clique baixo → email/amostra;
- checkout iniciado e compra baixa → oferta/checkout;
- compra boa e entrega problemática → fulfillment.

#### Épico 3 — Priorização de melhoria
Gerar sugestões de:
- nova hipótese;
- novo criativo;
- novo flow;
- novo email;
- novo preço ou novo posicionamento.

#### Épico 4 — Evals e qualidade
Criar avaliação automática para:
- copy;
- criatividade;
- aderência à hipótese;
- consistência com o nicho;
- força da personalização.

### Backlog sugerido
- [ ] Criar `get_funnel_report`
- [ ] Criar `get_variant_performance`
- [ ] Criar `get_experiment_diagnosis`
- [ ] Criar `propose_next_test`
- [ ] Criar `evaluate_copy`
- [ ] Criar `evaluate_creative`
- [ ] Criar `evaluate_personalization`
- [ ] Criar `create_iteration_backlog`
- [ ] Criar relatório executivo por experimento
- [ ] Criar rotina semanal de revisão assistida

### Tools desta fase
- `get_funnel_report`
- `get_variant_performance`
- `get_campaign_metrics`
- `get_email_metrics`
- `get_checkout_metrics`
- `diagnose_bottleneck`
- `propose_next_test`
- `evaluate_copy`
- `evaluate_creative`
- `evaluate_personalization`

### Entregáveis
- agente Analista funcional;
- relatório de performance por experimento;
- diagnóstico automático de gargalo;
- backlog de melhoria automática;
- evals básicas operando.

### Critérios de aceite
- o sistema consegue apontar o maior gargalo do funil;
- o sistema sugere ao menos uma próxima ação coerente;
- o diagnóstico fica rastreado por experimento;
- a sugestão pode ser reaproveitada pelo Estrategista no próximo ciclo.

### Dependências
- Sprint 4 concluído;
- métricas mínimas confiáveis;
- eventos e identificadores correlacionáveis.

### Riscos
- métricas incompletas ou desalinhadas entre módulos;
- pouca granularidade para explicar a performance;
- sugestão automática sem critério claro de prioridade.

### Definição de pronto
- o Marketing Hub passa a operar como sistema agentic de melhoria contínua.

---

# 7. Backlog transversal

Estes itens devem acompanhar todos os sprints.

## 7.1 Segurança e governança
- moderação de texto/imagem quando aplicável;
- validação de PII;
- controle de permissões;
- auditoria de prompts;
- approval logs;
- budget guardrails;
- rollback de ações críticas.

## 7.2 Qualidade de software
- testes automatizados por tool;
- smoke tests de fluxo;
- testes de contrato entre serviços;
- idempotência em ações críticas;
- retries controlados para webhooks e entregas.

## 7.3 Observabilidade
- tracing;
- custo por execução;
- latência por tool;
- taxa de falha por serviço;
- dashboard operacional do ecossistema agentic.

## 7.4 Dados e lineage
- versionamento de prompts;
- lineage de artefatos;
- vinculação a experimento;
- vinculação a campanha e compra;
- relatórios por ciclo de teste.

---

# 8. Ordem de prioridade real

## Prioridade máxima
- Sprint 1
- Sprint 2
- Sprint 3

Esses sprints constroem a base de decisão e geração dos artefatos.

## Prioridade alta
- Sprint 4

Aqui o sistema passa a operar ponta a ponta.

## Prioridade estratégica
- Sprint 5

Aqui o sistema começa a aprender e orientar a próxima rodada de melhoria.

---

# 9. Resultado esperado ao final do roadmap

Ao final dos 5 sprints, o Marketing Hub deverá ter:

- uma camada formal de tools sobre os serviços existentes;
- agentes especializados por função;
- aprovação humana em ações sensíveis;
- tracing e auditoria de ponta a ponta;
- versionamento de prompts e artefatos;
- operação assistida de campanha, flow, email, checkout e entrega;
- análise automática do funil;
- recomendações de melhoria contínua.

Em termos práticos, o sistema deixa de ser apenas um conjunto de módulos operacionais e passa a funcionar como um **motor de experimentação comercial assistido por agentes**.

---

# 10. Próximos passos recomendados

1. Validar o escopo do Sprint 1.
2. Listar quais endpoints/serviços atuais virarão tools primeiro.
3. Escolher o formato do gateway agentic:
   - interno no backend atual; ou
   - microserviço dedicado.
4. Definir o primeiro nicho piloto.
5. Definir o primeiro experimento que rodará com intervenção parcial dos agentes.

---

# 11. Anexo — Primeiro conjunto de tools sugeridas

## Estratégia
- `get_niche_context`
- `get_success_products`
- `create_hypothesis`
- `score_hypothesis`
- `create_experiment_plan`
- `select_journey_template`

## Criação
- `generate_ad_copy`
- `generate_creative_prompt`
- `generate_creative_variant`
- `generate_sample_email`
- `generate_lead_portal_flow`
- `render_personalized_samples`
- `apply_watermark`

## Operação
- `publish_campaign_draft`
- `pause_campaign`
- `publish_lead_portal_flow`
- `send_sample_email`
- `create_checkout`
- `get_payment_status`
- `release_final_assets`

## Análise
- `get_funnel_report`
- `get_variant_performance`
- `diagnose_bottleneck`
- `propose_next_test`
- `evaluate_copy`
- `evaluate_creative`
- `evaluate_personalization`

## Governança
- `request_human_approval`
- `check_permissions`
- `write_audit_log`
- `rollback_last_action`
- `budget_guardrail`
- `price_guardrail`

