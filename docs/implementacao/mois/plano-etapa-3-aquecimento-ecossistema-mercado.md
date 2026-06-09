# Plano de implementação — MOIS Etapa 3: Aquecimento e Ecossistema de Mercado

## 1. Objetivo de negócio

Transformar a Biblioteca de Páginas de Vendas do MOIS em uma ferramenta de decisão comercial, adicionando uma etapa posterior à análise da página para responder:

> Existe um ecossistema ativo aquecendo esse mercado e preparando o público para comprar uma solução parecida?

A etapa deve identificar, por produto/página analisada, sinais públicos de demanda e aquecimento em canais como YouTube, Instagram, TikTok, Google/web aberta, comunidades, reviews, concorrentes e afiliados.

O resultado esperado é priorizar produtos e mercados com maior chance de gerar vendas, mantendo o eixo:

```text
Dor → Resultado → Mecanismo → Prova → Oferta
```

## 2. Nome da etapa

**Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado**

Nome operacional sugerido:

```text
MARKET_WARMUP_RESEARCH
```

Slug sugerido:

```text
market-warmup-research
```

## 3. Contexto atual

Hoje o pipeline oficial da Biblioteca de Páginas de Vendas do MOIS possui duas etapas operacionais:

1. **Obtenção de HTML** — captura HTML bruto útil da página.
2. **Análise Comercial da Página** — usa o HTML capturado para gerar score e diagnóstico comercial.

A Etapa 3 deve começar somente depois de existir análise comercial concluída, pois a pesquisa deve usar a dor, promessa, mecanismo, prova, público e categoria identificados na página, e não apenas o nome bruto do produto.

## 4. Princípios obrigatórios da etapa

1. **Pesquisa orientada por evidência**: nenhuma conclusão sem fonte pública rastreável.
2. **Separação entre produto e mercado**: diferenciar presença do produto específico de aquecimento do mercado/dor.
3. **Recência acima de vaidade**: canal ativo recentemente vale mais do que canal grande abandonado.
4. **Intenção acima de volume**: comentários com dúvida, objeção e intenção de compra valem mais do que visualizações soltas.
5. **Risco de saturação explícito**: mercado aquecido demais pode exigir ângulo novo.
6. **Sem JSON dentro de JSON**: campos estruturados devem ser armazenados como registros próprios ou JSON de contrato direto, nunca JSON serializado dentro de texto funcional.
7. **Sem contaminação do artefato final**: não inserir marcadores técnicos, debug ou metadados internos nos resumos exibidos ao usuário.

## 5. Perguntas que a Etapa 3 deve responder

### 5.1 Produto específico

- O produto tem presença própria fora da página de venda?
- Existe produtor, especialista ou marca identificável?
- Há canal no YouTube, perfil no Instagram, TikTok ou comunidade oficial?
- Existem reviews, depoimentos, comparações, reclamações ou afiliados promovendo?
- A presença está ativa ou abandonada?

### 5.2 Mercado/dor

- Existem criadores produzindo conteúdo recente sobre a dor?
- Existem comentários recentes com sofrimento, desejo, urgência ou objeção?
- O público já entende o problema ou precisa ser educado?
- Quais promessas são repetidas no mercado?
- Quais mecanismos são mais usados?
- Quais provas aparecem com frequência?
- Há concorrentes fortes ou excesso de ofertas semelhantes?

### 5.3 Decisão comercial

- O mercado está quente, promissor, morno, frio ou saturado?
- Vale priorizar esse produto/mercado para experimento?
- Qual ângulo inicial de experimento parece mais promissor?
- Qual canal parece mais adequado para aquecer ou vender?

## 6. Score proposto

Criar um **Market Warm-up Score** de 0 a 100.

| Dimensão | Peso | Critério |
|---|---:|---|
| Atividade recente | 20 | Fontes/canais com publicação ou interação recente. |
| Densidade de criadores | 15 | Quantidade e variedade de criadores falando da dor. |
| Engajamento real | 20 | Comentários, perguntas, objeções e intenção comercial. |
| Dor explícita | 15 | Evidências de sofrimento, esforço, urgência ou desejo. |
| Maturidade de oferta | 10 | Concorrentes, produtos, bônus, reviews e afiliados. |
| Prova social disponível | 10 | Casos, depoimentos, antes/depois, autoridade ou demonstrações. |
| Risco de saturação | -10 | Promessas repetidas, desconfiança, excesso de ofertas ou risco regulatório. |

Classificação sugerida:

| Score | Classificação | Decisão sugerida |
|---:|---|---|
| 80–100 | Quente | Priorizar experimento. |
| 60–79 | Promissor | Priorizar com refinamento de ângulo. |
| 40–59 | Morno | Pesquisar mais antes de criar oferta. |
| 0–39 | Frio | Baixa prioridade. |
| Qualquer score com saturação alta | Saturado | Só avançar com ângulo diferenciado. |

## 7. Tipos de ecossistema

A etapa deve classificar o ecossistema principal encontrado:

1. **Aquecido por especialistas** — autoridade técnica, profissional ou institucional.
2. **Aquecido por creators** — influenciadores, canais de dicas, vídeos curtos, histórias pessoais.
3. **Aquecido por dor recorrente** — muitas perguntas, fóruns, comunidades e reclamações.
4. **Aquecido por concorrentes** — muitas ofertas, afiliados e páginas similares.
5. **Frio ou pouco educado** — poucas fontes recentes, pouca conversa e baixa intenção.
6. **Saturado** — muita promessa repetida, desconfiança ou competição comoditizada.

## 8. Fontes de pesquisa

### 8.1 Fontes da V1

Na primeira versão, usar pesquisa web pública e rastreável:

- Google/web aberta;
- YouTube público;
- páginas públicas de Instagram quando acessíveis;
- páginas públicas de TikTok quando acessíveis;
- reviews, blogs, fóruns, comunidades abertas e páginas concorrentes.

### 8.2 Fontes futuras

Em versões posteriores, avaliar integrações formais:

- YouTube Data API;
- Meta/Instagram Graph API, quando houver permissão e caso de uso compatível;
- APIs de social listening;
- bases de anúncios e criativos;
- coletores dedicados por plataforma.

## 9. Modelo de dados proposto

### 9.1 `mois_sales_page_market_warmup_job`

Controla uma execução de pesquisa de aquecimento.

Campos mínimos:

- `id`
- `sales_page_id`
- `workspace_id`
- `status`
- `attempt`
- `score_total`
- `market_temperature`
- `ecosystem_type`
- `recommendation`
- `started_at`
- `finished_at`
- `error_category`
- `error_message`
- `created_at`
- `updated_at`

### 9.2 `mois_sales_page_market_warmup_source`

Guarda cada fonte pública encontrada.

Campos mínimos:

- `id`
- `job_id`
- `sales_page_id`
- `platform`
- `source_type`
- `source_url`
- `source_title`
- `author_name`
- `published_at`
- `last_activity_at`
- `followers_or_subscribers`
- `views_count`
- `likes_count`
- `comments_count`
- `recency_score`
- `engagement_score`
- `evidence_summary`
- `created_at`
- `updated_at`

### 9.3 `mois_sales_page_market_warmup_signal`

Guarda sinais extraídos das fontes.

Campos mínimos:

- `id`
- `job_id`
- `source_id`
- `sales_page_id`
- `signal_type`
- `signal_strength`
- `signal_text`
- `business_interpretation`
- `created_at`

Tipos iniciais de sinal:

- `PAIN_EXPLICIT`
- `BUYING_INTENT`
- `OBJECTION`
- `SOCIAL_PROOF`
- `CREATOR_AUTHORITY`
- `COMPETITOR_OFFER`
- `COMMUNITY_ACTIVITY`
- `CONTENT_RECENCY`
- `SATURATION_RISK`
- `CHANNEL_FIT`

### 9.4 `mois_sales_page_market_warmup_summary`

Resumo final da execução.

Campos mínimos:

- `job_id`
- `sales_page_id`
- `score_total`
- `market_temperature`
- `ecosystem_type`
- `main_pains`
- `main_objections`
- `main_promises`
- `main_channels`
- `main_competitors`
- `saturation_risk`
- `opportunity_recommendation`
- `next_experiment_suggestion`
- `created_at`
- `updated_at`

## 10. Contratos de API propostos

### 10.1 Criar pesquisa para uma página

```http
POST /api/mois/sales-library/pages/{pageId}/market-warmup:request
```

Uso: solicitar a Etapa 3 para uma página específica.

### 10.2 Consultar resumo da pesquisa

```http
GET /api/mois/sales-library/pages/{pageId}/market-warmup
```

Uso: exibir na tela de detalhe da página.

### 10.3 Listar fontes encontradas

```http
GET /api/mois/sales-library/pages/{pageId}/market-warmup/sources
```

Uso: auditoria e revisão humana.

### 10.4 Listar sinais encontrados

```http
GET /api/mois/sales-library/pages/{pageId}/market-warmup/signals
```

Uso: entender por que o score foi atribuído.

### 10.5 Claim interno do worker

```http
POST /api/mois/sales-library/market-warmup/jobs:claim
```

Uso: worker reservar próxima pesquisa pendente.

### 10.6 Concluir job interno

```http
POST /api/mois/sales-library/market-warmup/jobs/{jobId}:complete
```

Uso: worker enviar fontes, sinais e resumo final.

### 10.7 Falhar job interno

```http
POST /api/mois/sales-library/market-warmup/jobs/{jobId}:fail
```

Uso: registrar falha terminal ou retentável.

## 11. Worker proposto

Nome sugerido:

```text
mois-market-warmup-worker
```

Responsabilidades:

1. Reservar job pendente no backend.
2. Buscar a página e sua análise comercial concluída.
3. Gerar queries qualificadas a partir de dor, promessa, público, mecanismo e categoria.
4. Executar coleta de fontes públicas.
5. Extrair sinais das fontes.
6. Calcular score e recomendação.
7. Persistir fontes, sinais e resumo final no backend.
8. Registrar logs com payload bruto recebido da fonte quando houver ingestão de dados.

## 12. UI proposta

### 12.1 Tela de detalhe da página

Adicionar bloco/aba:

```text
Aquecimento do Mercado
```

Exibir:

- score;
- temperatura;
- tipo de ecossistema;
- recomendação;
- principais canais;
- principais dores;
- principais objeções;
- principais concorrentes;
- fontes públicas com link;
- sinais que justificam a pontuação;
- botão “Executar pesquisa de aquecimento”.

### 12.2 Tela de biblioteca

Adicionar filtros/colunas futuras:

- aquecimento;
- ecossistema;
- prioridade;
- risco de saturação;
- data da última pesquisa.

### 12.3 Tela de pipeline

Adicionar card:

```text
Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado
```

Contadores:

- páginas analisadas aptas à Etapa 3;
- pesquisas pendentes;
- pesquisas em execução;
- pesquisas concluídas;
- falhas;
- mercados quentes/promissores/frios/saturados.

## 13. Critérios de qualidade

A etapa só deve ser considerada concluída quando:

1. Toda recomendação tiver fontes rastreáveis.
2. O score puder ser explicado por sinais salvos.
3. A UI permitir revisar fontes e sinais.
4. Falhas ficarem visíveis por página e por job.
5. O processo não bloquear a análise comercial existente.
6. O backend continuar sendo o único módulo com acesso ao banco.
7. O worker conversar somente com o backend.
8. Os contratos estiverem documentados no Swagger.
9. Houver testes unitários para regras de score, status e endpoints.
10. Changelogs Liquibase seguirem MySQL 5.7 e evitarem erro 1093.

## 14. Etapas de implementação em prompts executáveis

Cada item abaixo foi dimensionado para caber em um prompt de desenvolvimento independente.

### Fase 1 — Canonizar a Etapa 3

**Objetivo:** registrar a decisão nos documentos canônicos e no contrato de pipeline.

Escopo:

- Atualizar `docs/canonical/mois-worker-canon.v1.md` com a Etapa 3.
- Atualizar a definição oficial do pipeline MOIS para incluir `MARKET_WARMUP_RESEARCH` como posição 3.
- Atualizar testes relacionados ao pipeline oficial.
- Registrar a tarefa em `docs/registros/mois1.md`.

Critério de aceite:

- Pipeline oficial passa a ter três etapas.
- Etapa 3 tem nome, código, posição, responsabilidade e aliases.
- Testes de pipeline passam.

### Fase 2 — Criar schema Liquibase da Etapa 3

**Objetivo:** criar tabelas operacionais de job, fontes, sinais e resumo.

Escopo:

- Criar changelog YAML incremental MySQL 5.7.
- Incluir tabelas:
  - `mois_sales_page_market_warmup_job`;
  - `mois_sales_page_market_warmup_source`;
  - `mois_sales_page_market_warmup_signal`;
  - `mois_sales_page_market_warmup_summary`.
- Adicionar índices por `workspace_id`, `sales_page_id`, `status`, `score_total` e `platform`.
- Incluir FK para `mois_sales_page` e entre tabelas da etapa.
- Atualizar master changelog.

Critério de aceite:

- Changelog válido e idempotente.
- Sem `UPDATE/DELETE` com subconsulta na mesma tabela-alvo.
- Aplicação sobe com schema novo.

### Fase 3 — Backend: contratos DTO e Swagger

**Objetivo:** definir o contrato HTTP antes da implementação do fluxo.

Escopo:

- Criar DTOs da Etapa 3 no pacote MOIS da biblioteca.
- Documentar endpoints no Swagger do módulo.
- Definir enums de status, temperatura, tipo de ecossistema, plataforma e tipo de sinal.
- Garantir que o contrato não use JSON serializado dentro de campo texto funcional.

Critério de aceite:

- Swagger descreve request, response e erros.
- DTOs têm comentários em português.
- Sem endpoint consumido pelo frontend antes de existir no backend.

### Fase 4 — Backend: service/repository via JdbcTemplate/JPA no padrão atual

**Objetivo:** implementar persistência e leitura da Etapa 3.

Escopo:

- Criar service com responsabilidade única para warm-up.
- Criar métodos para:
  - solicitar pesquisa;
  - listar resumo por página;
  - listar fontes;
  - listar sinais;
  - reservar job;
  - concluir job;
  - falhar job.
- Criar repositories no pacote permitido `com.marketinghub.repository`, se necessário.
- Adicionar testes unitários do service.

Critério de aceite:

- Somente backend acessa banco.
- Classes Java e métodos têm comentários em português.
- Catch de exceções críticas registra log com contexto e stack trace.

### Fase 5 — Backend: controller da Etapa 3

**Objetivo:** expor endpoints públicos e internos da Etapa 3.

Escopo:

- Criar endpoints no controller MOIS da Biblioteca de Páginas de Vendas ou controller dedicado no mesmo pacote de módulo.
- Garantir escopo do módulo MOIS.
- Adicionar testes de controller.
- Validar payloads e respostas de erro.

Critério de aceite:

- Endpoints respondem conforme Swagger.
- Contratos internos do worker não expõem dados sensíveis.
- Testes unitários passam.

### Fase 6 — Worker V1 com pesquisa assistida por busca web pública

**Objetivo:** criar worker mínimo que processa jobs pendentes e monta dossiê inicial.

Escopo:

- Criar módulo `mois-market-warmup-worker` ou pacote equivalente, conforme decisão arquitetural.
- Implementar claim, processamento, complete e fail via backend.
- Gerar queries a partir da análise comercial da página.
- Fazer coleta inicial por busca web pública configurável.
- Salvar fontes e sinais básicos.
- Registrar logs do payload bruto recebido das fontes.

Critério de aceite:

- Worker não acessa banco diretamente.
- Worker usa apenas backend principal.
- Falhas são persistidas com categoria e mensagem operacional.

### Fase 7 — Motor de score e recomendação

**Objetivo:** transformar fontes e sinais em score comercial explicável.

Escopo:

- Implementar cálculo do Market Warm-up Score.
- Implementar classificação de temperatura e tipo de ecossistema.
- Implementar recomendação objetiva: `PRIORIZAR`, `OBSERVAR`, `PESQUISAR_MAIS`, `DESCARTAR`, `SATURADO_EXIGE_ANGULO`.
- Criar testes unitários com cenários de mercado quente, promissor, frio e saturado.

Critério de aceite:

- Score reproduzível por testes.
- UI consegue explicar o score pelos sinais salvos.

### Fase 8 — Frontend: detalhe da página

**Objetivo:** permitir ao usuário consultar e solicitar pesquisa de aquecimento.

Escopo:

- Criar hooks frontend para endpoints da Etapa 3.
- Adicionar bloco “Aquecimento do Mercado” na tela de detalhe da página.
- Exibir score, temperatura, recomendação, fontes e sinais.
- Botão para solicitar pesquisa quando ainda não existir dossiê.

Critério de aceite:

- UI mostra informação comercial clara e sem excesso.
- Links externos abrem com `target="_blank"`.
- Estados de loading, erro e vazio são explícitos.

### Fase 9 — Frontend: biblioteca e pipeline

**Objetivo:** inserir a Etapa 3 na operação diária da biblioteca.

Escopo:

- Adicionar card da Etapa 3 na tela de pipeline.
- Adicionar contadores globais da Etapa 3.
- Adicionar filtros/colunas simples na biblioteca, se o backend já expuser os campos.

Critério de aceite:

- Usuário consegue saber quantas páginas já têm pesquisa de aquecimento.
- Usuário consegue priorizar páginas por score de aquecimento.

### Fase 10 — Observabilidade e saneamento operacional

**Objetivo:** evitar que a Etapa 3 pare silenciosamente.

Escopo:

- Adicionar logs de claim, complete, fail e fontes coletadas.
- Criar rotina de detecção de jobs `FETCHING` antigos.
- Criar endpoint/ação de reprocessamento quando necessário.
- Adicionar métricas resumidas no pipeline.

Critério de aceite:

- Jobs presos ficam visíveis.
- Operador consegue reexecutar sem mexer no banco.
- Falhas indicam causa-raiz provável.

### Fase 11 — Integração com priorização de oportunidades

**Objetivo:** usar a Etapa 3 para gerar decisão comercial.

Escopo:

- Criar endpoint de ranking de páginas por score comercial combinado:
  - score da página;
  - score de aquecimento;
  - risco de saturação;
  - recência da evidência.
- Sugerir próximo experimento ou próxima pesquisa OPRM/MDS.
- Exibir ranking na UI.

Critério de aceite:

- Biblioteca deixa claro quais mercados devem ser priorizados.
- Recomendação final é objetiva e vinculada às evidências.

## 15. Ordem recomendada de execução

1. Fase 1 — Canonizar a Etapa 3.
2. Fase 2 — Criar schema.
3. Fase 3 — Definir contratos e Swagger.
4. Fase 4 — Implementar service.
5. Fase 5 — Implementar controller.
6. Fase 8 — Criar UI de consulta manual.
7. Fase 6 — Criar worker V1.
8. Fase 7 — Refinar score.
9. Fase 9 — Integrar pipeline/listagem.
10. Fase 10 — Observabilidade.
11. Fase 11 — Ranking e priorização.

Essa ordem permite entregar valor incremental: primeiro registro e consulta manual, depois automação, depois priorização.

## 16. MVP recomendado

Para a primeira entrega útil, executar somente os prompts 1 a 8.

Resultado do MVP:

- Etapa 3 existe no contrato oficial.
- Banco suporta dossiês de aquecimento.
- Backend possui endpoints.
- Tela de detalhe permite solicitar e consultar pesquisa.
- Worker V1 consegue gerar um dossiê básico com fontes e sinais.

## 17. Resultado esperado para o usuário

Ao abrir uma página analisada, o usuário deve conseguir ver uma resposta objetiva:

```text
Este mercado está PROMISSOR.
Score de aquecimento: 72/100.
Existem canais ativos no YouTube e Instagram falando da dor.
As principais objeções são preço, tempo de resultado e confiança no método.
Há concorrentes, mas a saturação ainda é moderada.
Recomendação: priorizar experimento com ângulo X.
```

Isso transforma a análise de página em decisão prática de venda.
