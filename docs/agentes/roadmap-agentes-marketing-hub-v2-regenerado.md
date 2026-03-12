# Roadmap Técnico por Sprint — Marketing Hub Agents v2

## 1. Objetivo

Este documento define o plano técnico por sprint para implementar uma camada de agentes sobre o projeto **Marketing Hub**, aproveitando a base já existente do produto e adicionando:

- orquestração agentic;
- catálogo formal de tools;
- approvals humanos;
- tracing e observabilidade;
- memória operacional e de aprendizado;
- evals e melhoria contínua;
- mecanismos de promoção, rollback e especialização dos agentes.

A proposta assume que o sistema atual **já possui boa parte da execução operacional pronta** e que o foco agora é transformar essa base em uma plataforma orientada por agentes, cada vez mais precisa, segura e valiosa.

---

## 2. Premissas do plano

### 2.1 Premissas sobre a base existente

Com base no repositório e no modelo de dados atual, o projeto já aparenta ter:

- backend e frontend principais;
- workers especializados para IA, Facebook Ads, email, watermark e zip;
- serviços de lead portal e pagamento;
- estrutura de nichos, hipóteses, experimentos e jornadas;
- geração de criativos e amostras;
- execução de campanhas em Meta/Facebook;
- coleta de leads;
- envio de emails;
- compra e entrega premium;
- histórico de eventos suficiente para aprendizagem com dados reais.

### 2.2 Diretriz de arquitetura

Os agentes **não devem substituir** o que já existe. Eles devem operar como uma camada superior de:

- decisão;
- coordenação;
- leitura de contexto;
- geração de artefatos;
- análise;
- melhoria contínua.

### 2.3 Agentes-alvo

A arquitetura final considera 4 agentes principais:

1. **Estrategista**
2. **Criativo**
3. **Operador**
4. **Analista**

Todos compartilham uma infraestrutura comum de:

- tools;
- memória;
- tracing;
- evals;
- approvals;
- versionamento;
- segurança.

---

## 3. Princípios de implementação

1. **Reaproveitar a base existente antes de criar novos serviços.**
2. **Encapsular capacidades reais como tools estáveis.**
3. **Separar ações de leitura, sugestão e execução.**
4. **Exigir aprovação humana nas ações de maior impacto.**
5. **Versionar prompts, artefatos, decisões e resultados.**
6. **Tratar cada erro relevante como futuro caso de teste.**
7. **Tratar cada acerto relevante como padrão reutilizável.**
8. **Melhorar agentes continuamente com dados reais do funil.**

---

## 4. Arquitetura de alto nível

### 4.1 Camadas

#### Camada A — Sistemas existentes
- backend
- frontend
- ai-worker
- facebook-ads-worker
- email-service
- image-watermark-service
- image-zipper-service
- lead-portal
- lead-portal-payments-service
- market-research-service

#### Camada B — Agent Control Layer
Novo serviço responsável por:
- registrar tools;
- chamar serviços existentes;
- padronizar contratos;
- aplicar autenticação e autorização;
- registrar traces;
- controlar approvals;
- consolidar logs e lineage.

#### Camada C — Agentes
- agente estrategista
- agente criativo
- agente operador
- agente analista

#### Camada D — Aprendizado contínuo
- datasets de erro/acerto;
- rubricas e evals;
- biblioteca de decisões;
- promotion/rollback de prompts;
- rotinas de análise de performance;
- especialização de agente por tarefa.

---

## 5. Planejamento por sprint

Este plano usa **10 sprints de 2 semanas**.

- **Sprints 1–3:** fundação agentic sobre a base existente
- **Sprints 4–6:** agentes operacionais
- **Sprints 7–10:** aprendizado contínuo, precisão crescente e escala

---

# Sprint 1 — Descoberta técnica e Tool Layer

## Objetivo
Criar a camada técnica mínima que permita aos agentes operar sobre os serviços já existentes de forma segura e rastreável.

## Escopo
- mapear os serviços e endpoints já existentes;
- catalogar capacidades por domínio;
- definir convenção única de tools;
- criar o serviço `agent-control`;
- implementar autenticação entre `agent-control` e os serviços internos;
- criar `trace_id` único por execução agentic;
- definir política de approval para ações críticas.

## Entregáveis
- catálogo inicial de tools;
- documento de contratos de tool;
- diagrama de integração entre agentes e serviços atuais;
- middleware de tracing;
- camada de autorização e permissões;
- fluxo mínimo de aprovação humana.

## Tools previstas
- `get_niche`
- `get_hypothesis`
- `get_experiment`
- `get_creative`
- `get_campaign`
- `get_flow`
- `get_submission`
- `get_purchase`
- `request_human_approval`

## Critérios de aceite
- um agente consegue chamar 3 serviços reais via `agent-control`;
- cada chamada registra `trace_id`, input, output, duração e erro;
- uma ação crítica é bloqueada até aprovação humana.

## Riscos tratados
- acoplamento direto entre agente e microserviços;
- falta de observabilidade;
- ausência de controle de execução.

---

# Sprint 2 — Contexto, memória e recuperação útil

## Objetivo
Fazer os agentes operarem com contexto histórico do produto, e não apenas com a tarefa atual.

## Escopo
- consolidar acesso a nichos, hipóteses, experimentos, prompts e assets;
- criar memória operacional por experimento;
- indexar histórico de execuções relevantes;
- salvar “aprendizados” de forma estruturada;
- implementar busca por padrões vencedores e perdedores.

## Entregáveis
- repositório de contexto por nicho;
- memória de experimentos anteriores;
- biblioteca versionada de prompts;
- repositório de padrões vencedores;
- modelo de armazenamento de aprendizados.

## Tools previstas
- `get_past_experiments`
- `search_prompt_library`
- `search_asset_library`
- `get_winner_patterns`
- `get_failure_patterns`
- `save_learning`
- `get_learning_by_niche`

## Estrutura mínima do aprendizado salvo
Cada aprendizado deve conter:
- domínio: campanha, página, email, checkout, personalização;
- contexto: nicho, hipótese, público, tipo de oferta;
- decisão tomada;
- resultado observado;
- classificação: acerto, erro, inconclusivo;
- confiança;
- recomendação futura.

## Critérios de aceite
- o agente consegue responder o que já foi testado por nicho;
- o agente recupera exemplos de prompts/artefatos com bom resultado;
- o agente registra um aprendizado após execução real.

---

# Sprint 3 — Agente Estrategista

## Objetivo
Implementar o agente responsável por transformar nicho em hipótese, hipótese em experimento e experimento em plano mensurável.

## Escopo
- geração de hipótese comercial;
- estruturação de promessa, mecanismo e objeções;
- criação de experimento;
- seleção de jornada/template;
- definição de metas e métricas de sucesso;
- backlog de testes sugeridos.

## Entregáveis
- agente Estrategista funcional;
- scorecard de hipótese;
- formato padrão de experimento;
- geração de plano de teste por nicho.

## Tools previstas
- `create_hypothesis`
- `score_hypothesis`
- `map_objections`
- `select_journey_template`
- `create_experiment_plan`
- `define_success_metrics`
- `prioritize_test_matrix`

## Critérios de aceite
- dado um nicho, o agente gera 3 hipóteses consistentes;
- o agente escolhe a melhor hipótese por rubric;
- o agente cria um experimento compatível com o schema atual.

## Aprendizado contínuo acoplado
Nesta fase já devem ser salvos:
- hipóteses aprovadas;
- hipóteses rejeitadas;
- motivo da rejeição;
- padrões de nicho com melhor aderência.

---

# Sprint 4 — Agente Criativo

## Objetivo
Automatizar a criação dos principais artefatos do processo comercial.

## Escopo
- geração de copy de anúncio;
- geração de prompt para criativo;
- geração de headline e blocos da lead page;
- geração de email de amostra;
- geração de prompts de personalização;
- versionamento de prompt e output.

## Entregáveis
- agente Criativo funcional;
- pipeline de criação de artefatos por experimento;
- biblioteca de prompts versionados;
- lineage entre experimento, prompt e asset.

## Tools previstas
- `generate_ad_copy`
- `generate_headline_variants`
- `generate_landing_page_copy`
- `generate_email_subjects`
- `generate_email_body`
- `generate_creative_prompt`
- `generate_personalized_sample_prompt`
- `fork_prompt`
- `compare_prompt_versions`

## Critérios de aceite
- a partir de uma hipótese aprovada, o agente gera:
  - copy de anúncio;
  - prompt de criativo;
  - copy de lead page;
  - email com prova de valor;
  - prompt da amostra personalizada.

## Aprendizado contínuo acoplado
Começar a registrar:
- quais tipos de copy são mais aprovados por humano;
- quais estilos criativos são mais escolhidos;
- quais prompts geram menos retrabalho;
- quais saídas falham em rubric de clareza, aderência e valor percebido.

---

# Sprint 5 — Personalização, watermark e kit de amostras

## Objetivo
Fazer o agente produzir amostras personalizadas realmente utilizáveis no funil de vendas.

## Escopo
- ler dados do lead/formulário;
- extrair atributos do negócio;
- montar brief de personalização;
- gerar imagens amostra;
- aplicar watermark;
- montar sample pack;
- registrar qualidade do resultado.

## Entregáveis
- pipeline de personalização orientado por agente;
- geração de amostras para envio por email;
- aplicação de watermark integrada;
- score de qualidade da personalização.

## Tools previstas
- `parse_lead_profile`
- `extract_business_attributes`
- `generate_personalization_brief`
- `render_personalized_samples`
- `apply_watermark`
- `assemble_sample_pack`
- `validate_personalization_quality`

## Critérios de aceite
- dado um lead real, o agente cria um kit de amostras coerente;
- o watermark é aplicado corretamente;
- o kit fica pronto para email ou checkout;
- os resultados ficam associados ao experimento de origem.

## Aprendizado contínuo acoplado
Salvar:
- quais inputs do formulário aumentam a qualidade da personalização;
- quais elementos visuais geram mais clique;
- quais tipos de sample pack convertem mais;
- causas de falha na geração ou composição do pacote.

---

# Sprint 6 — Agente Operador

## Objetivo
Conectar os agentes à execução real do fluxo, mantendo proteção por approvals.

## Escopo
- publicação de campanha draft;
- criação/atualização de lead page;
- envio de email com amostra;
- criação de checkout;
- liberação do produto final após pagamento;
- pausa e rollback operacional.

## Entregáveis
- agente Operador funcional;
- execução assistida por approval;
- guardrails de orçamento e preço;
- linha de auditoria completa.

## Tools previstas
- `publish_campaign_draft`
- `pause_campaign`
- `publish_landing_page`
- `send_sample_email`
- `create_checkout`
- `release_final_assets`
- `resend_delivery`
- `rollback_last_change`

## Ações que exigem approval obrigatório
- publicar anúncio;
- aumentar orçamento;
- mudar preço;
- enviar email em massa;
- liberar entrega final em produção;
- emitir reembolso.

## Critérios de aceite
- um experimento consegue ir de artefatos aprovados até compra;
- o produto final pode ser entregue automaticamente após pagamento confirmado;
- cada passo crítico fica auditável e revertível.

## Aprendizado contínuo acoplado
Salvar:
- quais aprovações humanas mais bloqueiam o agente;
- quais decisões do agente mais frequentemente são aceitas;
- quais ações geram mais rollback;
- padrões de risco operacional.

---

# Sprint 7 — Agente Analista e score de decisão

## Objetivo
Fazer o sistema diagnosticar gargalos e sugerir melhorias baseadas em resultado real do funil.

## Escopo
- consolidar métricas por experimento;
- medir performance por variante;
- diagnosticar gargalos;
- explicar quedas de conversão;
- sugerir próximo teste com base em evidência.

## Entregáveis
- agente Analista funcional;
- dashboards por experimento;
- score por hipótese/copy/criativo/email/checkout;
- ranking de próximos testes.

## Tools previstas
- `get_funnel_report`
- `get_variant_performance`
- `get_niche_performance`
- `diagnose_bottleneck`
- `propose_next_test`
- `compare_periods`
- `detect_anomaly`

## Score de decisão
Cada decisão relevante do agente deve ganhar score em pelo menos 4 dimensões:
- qualidade percebida;
- eficiência operacional;
- impacto em conversão;
- impacto em receita.

## Critérios de aceite
- o sistema responde qual variante performou melhor;
- o sistema identifica a maior queda do funil;
- o sistema sugere a próxima mudança com justificativa baseada em dados.

## Aprendizado contínuo acoplado
Nesta fase começa o ciclo formal:
- decisão;
- resultado;
- score;
- aprendizado salvo;
- reutilização em contexto futuro.

---

# Sprint 8 — Evals estruturadas e regressão de qualidade

## Objetivo
Transformar erros e acertos em testes repetíveis para evitar regressões e melhorar a precisão dos agentes.

## Escopo
- definir rubricas de qualidade por agente;
- criar datasets de casos bons e ruins;
- montar suíte de evals;
- rodar evals em CI/CD;
- introduzir gating de promoção de prompt/agente.

## Entregáveis
- suíte inicial de evals;
- repositório de casos de teste;
- rubricas versionadas;
- pipeline de regressão de qualidade;
- política de promoção e rollback.

## Tipos de eval
### Eval de componente
- qualidade da copy;
- qualidade do criativo;
- qualidade da personalização;
- clareza de oferta;
- alinhamento com nicho.

### Eval de workflow
- o agente seguiu a ordem correta de tools;
- o agente pediu approval quando necessário;
- o agente registrou rastros completos;
- o agente não repetiu ações indevidas.

### Eval de negócio
- impacto em CTR;
- impacto em lead rate;
- impacto em email click;
- impacto em checkout rate;
- impacto em receita por lead.

### Eval de segurança
- exposição de PII;
- uso indevido de orçamento;
- alteração de preço sem approval;
- execução indevida de publish/send.

## Tools previstas
- `evaluate_copy`
- `evaluate_creative`
- `evaluate_personalization`
- `evaluate_offer_clarity`
- `run_regression_eval`
- `approve_if_score_above_threshold`

## Critérios de aceite
- uma mudança de prompt não pode ser promovida sem passar nos evals mínimos;
- um erro recorrente consegue virar caso de teste em até 1 sprint;
- uma melhoria comprovada pode ser promovida com segurança.

---

# Sprint 9 — Memória útil e auto-otimização assistida

## Objetivo
Fazer os agentes reutilizarem aprendizados com mais precisão e menor repetição de erros.

## Escopo
- separar memória episódica, semântica e operacional;
- adicionar score de confiança por aprendizado;
- recuperar aprendizados por similaridade de contexto;
- construir “playbooks vivos” por nicho;
- permitir auto-otimização assistida de prompts e decisões.

## Tipos de memória
### Memória episódica
Registra execuções específicas.
Ex.: “No experimento X para personal trainers, a promessa Y gerou CTR alto, mas checkout baixo.”

### Memória semântica
Registra padrões mais duradouros.
Ex.: “Promessas de economia de tempo funcionam melhor que promessas genéricas de crescimento para nichos de serviços locais.”

### Memória operacional
Registra como fazer.
Ex.: “Para montar sample pack de dentistas, usar template Z e 4 variações visuais.”

### Memória de avaliação
Registra o que costuma falhar/aprovar em rubricas.
Ex.: “Este estilo de copy tende a reprovar por excesso de abstração.”

## Entregáveis
- sistema de memória segmentado;
- recuperação de aprendizado por contexto;
- ranking de recomendações por confiança;
- suggestions automáticas de prompt improvement.

## Tools previstas
- `save_episode`
- `save_semantic_learning`
- `get_similar_cases`
- `get_playbook_for_niche`
- `suggest_prompt_improvement`
- `suggest_next_best_action`

## Critérios de aceite
- o agente passa a citar e reutilizar aprendizados relevantes automaticamente;
- o número de erros repetidos cai em tarefas recorrentes;
- o retrabalho humano em aprovação diminui.

---

# Sprint 10 — Especialização, distillation e escala controlada

## Objetivo
Levar os agentes de “bons generalistas” para “especialistas úteis” em tarefas repetidas e valiosas.

## Escopo
- identificar tarefas de alto volume e alto padrão;
- selecionar tarefas candidatas a especialização;
- testar fine-tuning, distillation ou prompt optimizer;
- criar agentes especializados por domínio;
- implantar rollout progressivo com monitoramento.

## Candidatos naturais à especialização
- copy de anúncio por nicho;
- estrutura de lead page por tipo de oferta;
- email com amostra;
- composição de sample pack;
- diagnóstico de gargalo do funil;
- classificação de qualidade de artefato.

## Entregáveis
- matriz de especialização por tarefa;
- prova de conceito de especialização;
- benchmark contra agente generalista;
- rollout controlado por nicho.

## Tools previstas
- `collect_training_candidates`
- `export_high_quality_examples`
- `benchmark_specialized_agent`
- `route_task_to_specialist`
- `fallback_to_generalist`

## Critérios de aceite
- o agente especializado supera o generalista em pelo menos uma tarefa crítica;
- o custo/latência/qualidade ficam mensurados;
- o fallback para generalista funciona sem perda operacional.

---

## 6. Roadmap contínuo de aprimoramento dos agentes

Além dos sprints, o projeto deve operar um ciclo permanente.

### 6.1 Flywheel de melhoria
1. o agente executa;
2. o sistema registra o contexto completo;
3. o resultado é medido;
4. o resultado recebe score;
5. erros e acertos relevantes viram casos estruturados;
6. evals e memória são atualizados;
7. prompts/regras são promovidos ou revertidos;
8. o agente volta a operar com contexto melhor.

### 6.2 Fontes de aprendizado real
O sistema deve aprender a partir de:
- performance de campanha;
- taxa de envio e abertura de email;
- clique nas amostras;
- início de checkout;
- compra;
- suporte/reembolso;
- aprovação ou rejeição humana;
- retrabalho em prompts e ativos;
- falhas de geração e entrega.

### 6.3 O que conta como erro valioso
- copy aprovada pelo modelo, mas rejeitada por humano;
- criativo bonito, mas com CTR ruim;
- amostra boa, mas sem impacto em compra;
- decisão operacional correta, mas em momento errado;
- agente que faz mais passos do que o necessário;
- uso de tools inadequadas;
- publish/send sem necessidade;
- recuperação ruim de contexto.

### 6.4 O que conta como acerto valioso
- variante com performance superior consistente;
- prompt com menos retrabalho;
- decisão aceita por humano com frequência alta;
- melhoria que aumenta receita por lead;
- padrão reutilizável entre nichos semelhantes;
- resposta mais curta, precisa e operacionalmente útil.

---

## 7. Métricas de evolução dos agentes

## 7.1 Métricas de qualidade
- taxa de aprovação humana;
- score médio por rubric;
- taxa de retrabalho por artefato;
- taxa de erro repetido;
- taxa de aderência ao nicho.

## 7.2 Métricas operacionais
- tempo para gerar experimento;
- tempo para gerar kit completo de artefatos;
- número médio de tool calls por tarefa;
- taxa de rollback;
- custo por execução agentic.

## 7.3 Métricas de negócio
- CTR;
- CPL;
- taxa de lead;
- taxa de clique em email;
- taxa de checkout iniciado;
- purchase rate;
- receita por lead;
- ROAS.

## 7.4 Métricas de melhoria contínua
- percentual de casos relevantes convertidos em eval;
- tempo médio entre erro observado e proteção implementada;
- taxa de promoção de prompts vencedores;
- taxa de regressão após mudança;
- ganho incremental por versão do agente.

---

## 8. Backlog transversal obrigatório

Esses itens devem acompanhar todos os sprints:

- gestão de segredos;
- redaction de PII;
- controle de permissões;
- observabilidade e tracing;
- custo e orçamento por execução;
- documentação de tools;
- documentação de eventos do funil;
- política de approval;
- políticas de rollback;
- testes automatizados;
- smoke tests de integrações;
- moderação e segurança;
- versionamento de prompts e rubricas.

---

## 9. Ordem recomendada de implantação em produção

### Fase 1 — Assistência interna
Os agentes sugerem, mas não executam em produção sem humano.

### Fase 2 — Execução assistida
Os agentes executam fluxos específicos com approval obrigatório.

### Fase 3 — Autonomia limitada
Os agentes podem agir sozinhos em tarefas de baixo risco e alto padrão.

### Fase 4 — Otimização contínua controlada
Os agentes passam a operar com memória útil, evals, guardrails e especialização por domínio.

---

## 10. Resultado esperado ao final do roadmap

Ao fim do Sprint 10, o Marketing Hub deve ter:

- agentes especializados por papel;
- tools formalizadas e auditáveis;
- execução ponta a ponta com approvals;
- memória útil baseada em dados reais;
- evals e regressão de qualidade;
- score por decisão e por artefato;
- ciclo de melhoria contínua operacional;
- capacidade de aprender com erros e acertos reais do funil.

Em termos práticos, isso transforma o Marketing Hub de um conjunto de automações em um **sistema de experimentação comercial orientado por agentes**, com aprendizado acumulado e precisão crescente.

---

## 11. Próximo documento recomendado

A sequência ideal após este roadmap é criar um documento de execução com:

- épicos;
- histórias técnicas;
- critérios de aceite;
- dependências;
- estimativas;
- owners por squad;
- plano de rollout por ambiente.

