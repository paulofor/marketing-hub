# Especificação — Centro de Decisão de Experimentos no frontend

## 1. Objetivo

Criar uma experiência administrativa que permita ao usuário acompanhar, compreender e decidir sobre experimentos comerciais sem depender de logs técnicos, consultas ao banco ou interpretação manual de JSONs.

A interface deve responder, em ordem:

1. O que está sendo testado?
2. Esta execução chegou validamente ao mercado?
3. Os dados são íntegros e atuais?
4. Onde está o maior gargalo?
5. Qual foi o resultado econômico?
6. Já existe evidência suficiente para decidir?
7. O que as regras determinísticas indicam?
8. O que o modelo recomenda e por quê?
9. Qual ação o usuário pode executar agora?
10. O que acontecerá após a ação?

O frontend é uma camada de apresentação e comando. Ele não calcula métricas de negócio, não decide transições de pipeline e não orquestra módulos.

---

## 2. Princípios obrigatórios

### 2.1 Verdade única no backend

Toda informação apresentada deve vir de DTOs/read models do backend:

- estratégia;
- run atual;
- preflight;
- publicação;
- métricas;
- economia;
- qualidade do dado;
- comparação;
- recomendação;
- decisão humana;
- comandos permitidos.

O frontend pode formatar moeda, percentual e datas, mas não deve recomputar:

- taxas de conversão;
- margem;
- nível de evidência;
- prontidão;
- vencedor;
- validade do run;
- causa-raiz;
- próxima ação.

### 2.2 Informação progressiva

A tela deve priorizar decisão e esconder detalhes técnicos em níveis progressivos:

1. resumo executivo;
2. diagnóstico e evidências;
3. detalhes operacionais;
4. payloads brutos somente para perfis autorizados.

Não apresentar grandes JSONs como interface principal.

### 2.3 Zero, ausente e não aplicável são diferentes

A UI deve diferenciar:

- `0`: houve medição e o resultado foi zero;
- `—`: dado ainda não recebido ou indisponível;
- `N/A`: etapa não pertence à estratégia;
- `Atrasado`: dado existente fora da janela de atualização;
- `Bloqueado`: dado não pode ser interpretado por falha crítica.

### 2.4 Recomendações não são comandos

A recomendação do modelo deve aparecer separada de:

- decisão humana;
- comando operacional;
- resultado do comando.

O botão de aceitar recomendação não pode publicar, pausar ou elevar orçamento automaticamente. Aceitar registra a decisão; a ação seguinte exige comando explícito quando aplicável.

### 2.5 Poucos comandos, sempre contextuais

Exibir somente comandos permitidos pelo backend no estado atual. Cada comando precisa informar:

- objetivo;
- impacto;
- pré-condições;
- efeitos esperados;
- possibilidade de reversão;
- confirmação quando houver impacto comercial.

---

## 3. Arquitetura de informação

## 3.1 Rotas principais

```text
/experiments
/experiments/:experimentId
/experiments/:experimentId/runs/:runId
/experiment-comparisons
/experiment-comparisons/new
/experiment-comparisons/:comparisonId
/experiment-comparisons/:comparisonId/decision-runs/:decisionRunId
```

### Compatibilidade

A página existente de detalhe do experimento permanece. A evolução deve adicionar componentes e abas gradualmente, sem duplicar as telas atuais de criativos, landing, público e conteúdo.

## 3.2 Navegação de alto nível

### Lista de experimentos

Adicionar filtros:

- estratégia;
- rota de entrada;
- status agregado;
- validade da última execução;
- comparação vinculada;
- nicho;
- hipótese;
- período;
- presença de bloqueio;
- decisão pendente.

Cards/linhas devem mostrar:

- código do experimento;
- hipótese;
- rota;
- run atual;
- estado;
- validade;
- gasto;
- compras;
- margem;
- alerta principal;
- próximo passo.

### Lista de comparações

Mostrar:

- código e pergunta de negócio;
- braços;
- variável alterada;
- métrica primária;
- status;
- nível de evidência;
- líder atual sem declarar vencedor prematuramente;
- qualidade dos dados;
- decisão pendente;
- última atualização.

---

## 4. Detalhe do experimento

## 4.1 Cabeçalho persistente

Mostrar:

- código/nome;
- nicho e hipótese;
- rota comercial;
- oferta;
- variável primária;
- métrica primária;
- run atual;
- status do run;
- validade da evidência;
- comparação vinculada;
- data da última atualização.

Ações globais devem vir de `allowedCommands`.

## 4.2 Abas propostas

```text
Visão geral
Preparação e execução
Ativos
Público
Funil
Economia
Aprendizado e decisão
Histórico
```

Abas existentes podem ser incorporadas ou manter atalhos, mas a nomenclatura final deve evitar sobreposição.

### `Visão geral`

Objetivo: permitir leitura em menos de um minuto.

Blocos:

1. **O que está sendo testado**;
2. **Execução atual**;
3. **Resultado principal**;
4. **Maior gargalo**;
5. **Economia**;
6. **Qualidade do dado**;
7. **Recomendação atual**;
8. **Próxima ação permitida**.

### `Preparação e execução`

Usar a especificação de `ExperimentRun`.

Seções:

- qualidade upstream;
- desenho experimental;
- ativos;
- teste ponta a ponta;
- publicação Meta;
- mensuração;
- linha do tempo;
- tentativas e retries;
- histórico de runs.

### `Ativos`

Reunir somente leitura executiva e links para editores existentes:

- anúncio/criativo;
- landing;
- Instant Form;
- amostra;
- oferta;
- checkout;
- entregável.

Mostrar coerência de mensagem:

```text
Dor → promessa → CTA → captura → entrega → oferta
```

Divergências devem vir do backend como diagnóstico estruturado.

### `Público`

Mostrar:

- público estratégico planejado;
- targeting publicado;
- diferenças;
- estimativa de alcance;
- qualidade/aderência;
- fontes do público;
- status da resolução Meta;
- alertas de amplitude ou estreitamento.

### `Funil`

O funil deve ser específico da estratégia.

Exemplo Instant Form + personalização:

```text
Impressão
Clique/abertura
Lead capturado
Perfil iniciado
Perfil concluído
Amostra entregue
Amostra vista
Oferta vista
Checkout
Compra
Entrega paga
```

Exemplo venda direta:

```text
Impressão
Clique
Página de vendas
Checkout
Compra
Entrega
```

Cada etapa mostra:

- quantidade;
- únicos;
- conversão versus etapa anterior;
- conversão desde a entrada;
- custo por avanço;
- última ocorrência;
- freshness;
- status da fonte;
- intervalo de comparação.

O frontend não deve somar eventos nem derivar etapas.

### `Economia`

Mostrar:

- gasto de mídia;
- custo de IA;
- custo de personalização;
- taxas de pagamento;
- reembolsos;
- receita bruta;
- receita líquida;
- margem de contribuição;
- margem por clique elegível;
- margem por lead;
- margem por compra;
- custo por etapa.

Deve existir detalhamento do ledger por origem, com filtros e exportação.

### `Aprendizado e decisão`

Separar visualmente:

1. **Leitura determinística**;
2. **Recomendação do modelo**;
3. **Decisão humana**;
4. **Comando executado**;
5. **Resultado posterior**.

### `Histórico`

Timeline unificada:

- mudanças de estratégia;
- criação de runs;
- preflight;
- publicação;
- pausas;
- falhas;
- recomendações;
- decisões humanas;
- comandos;
- relatórios;
- criação de experimentos derivados.

---

## 5. Detalhe da comparação

## 5.1 Cabeçalho

Mostrar:

- pergunta de negócio;
- hipótese;
- variável alterada;
- variáveis controladas;
- métrica primária;
- janela;
- política de parada;
- nível de evidência;
- qualidade do dado;
- status.

## 5.2 Resumo dos braços

Cada braço deve apresentar:

- papel (`CONTROL`/`CHALLENGER`);
- experimento;
- rota;
- oferta/preço;
- run comercial usado;
- tráfego recebido;
- gasto;
- compras;
- margem;
- métrica primária;
- integridade;
- desvios de comparabilidade.

## 5.3 Matriz de comparabilidade

Tabela:

| Dimensão | Controle | Desafiante | Classificação |
|---|---|---|---|
| Público | ... | ... | Controlado / alerta |
| Criativo | ... | ... | Igual / variável |
| Rota | ... | ... | Variável esperada |
| Oferta | ... | ... | Igual |
| Preço | ... | ... | Igual |
| Janela | ... | ... | Compatível |

Classificações:

```text
EXPECTED_DIFFERENCE
CONTROLLED_EQUIVALENCE
COMPARABILITY_WARNING
COMPARABILITY_BLOCKER
```

## 5.4 Funil lado a lado

Para rotas com etapas diferentes, usar:

- etapas equivalentes semanticamente;
- etapas específicas abaixo do caminho comum;
- `N/A` para etapas inexistentes;
- explicação da normalização feita pelo backend.

Não forçar um único funil visual que transforme ausência de etapa em zero.

## 5.5 Gráficos

Gráficos úteis:

- métrica primária por dia;
- margem acumulada;
- gasto acumulado;
- conversões acumuladas;
- distribuição de tráfego;
- freshness de fontes.

Regras:

- sempre mostrar valores tabulares acessíveis;
- indicar períodos sem dados;
- não extrapolar tendência futura;
- não desenhar linha de vencedor antes de `DECISION_READY`;
- apresentar intervalo de incerteza quando fornecido pelo backend.

## 5.6 Decisão

Bloco final:

```text
Nível de evidência
Leitura determinística
Recomendação do modelo
Evidências citadas
Limitações
Riscos
Alternativas consideradas
Decisão humana
Próximo comando
```

---

## 6. Componentes propostos

```text
ExperimentStrategyCard
ExperimentRunSummaryCard
ExperimentPreflightPanel
ExperimentRunTimeline
ExperimentValidityBadge
DataQualityPanel
FunnelStageTable
FunnelBottleneckCard
ExperimentEconomicsCard
ExperimentCostLedgerTable
ComparisonArmSummaryCard
ComparisonMatrix
ComparisonFunnelTable
EvidenceLevelCard
DeterministicDecisionCard
AiRecommendationCard
EvidenceReferenceList
HumanDecisionPanel
AllowedCommandsPanel
ActionImpactDialog
DecisionHistoryTimeline
```

Componentes devem consumir DTOs tipados e não conhecer regras de domínio.

---

## 7. Read models do backend

## 7.1 Overview do experimento

```json
{
  "experiment": {},
  "strategy": {},
  "currentRun": {},
  "primaryOutcome": {},
  "funnelSummary": {},
  "economicsSummary": {},
  "dataQuality": {},
  "latestRecommendation": {},
  "latestHumanDecision": {},
  "allowedCommands": [],
  "lastUpdatedAt": ""
}
```

Endpoint sugerido:

```text
GET /api/experiments/{experimentId}/decision-dashboard
```

## 7.2 Dashboard da comparação

```json
{
  "comparison": {},
  "readiness": {},
  "comparability": {},
  "arms": [],
  "normalizedFunnel": {},
  "economics": {},
  "evidenceLevel": {},
  "deterministicAssessment": {},
  "latestRecommendation": {},
  "latestHumanDecision": {},
  "allowedCommands": [],
  "dataFreshness": []
}
```

Endpoint:

```text
GET /api/experiment-comparisons/{comparisonId}/dashboard
```

## 7.3 Evidence reference

```json
{
  "evidenceId": "metric:arm-a:purchase-rate",
  "type": "METRIC",
  "title": "Taxa de compra por clique",
  "value": "1,8%",
  "period": "...",
  "source": "PAYMENT_AND_AD_EVENTS",
  "freshnessStatus": "CURRENT",
  "drillDownUrl": "/..."
}
```

A UI não deve interpretar referências livres. Tipos e URLs precisam ser validados pelo backend.

## 7.4 Comando permitido

```json
{
  "code": "REQUEST_DECISION_ANALYSIS",
  "label": "Solicitar análise",
  "description": "Gera uma recomendação usando o snapshot atual.",
  "style": "PRIMARY",
  "requiresConfirmation": false,
  "disabled": false,
  "disabledReason": null,
  "impact": {
    "summary": "Cria uma execução de análise sem alterar campanhas.",
    "reversible": true
  }
}
```

---

## 8. Recomendação do modelo

## 8.1 Resumo

Mostrar:

- recomendação;
- confiança qualitativa;
- nível de evidência determinístico;
- horário;
- modelo;
- versão do pipeline;
- snapshot analisado;
- status da revisão crítica.

## 8.2 Evidências

Cada afirmação importante deve ter uma referência clicável.

Exemplo:

```text
“A landing é o maior gargalo”
  ↳ 114 cliques
  ↳ 100 visualizações
  ↳ 0 envios
  ↳ preflight funcional não aprovado
```

## 8.3 Limitações

Sempre mostrar:

- dados ausentes;
- flags críticas;
- amostra insuficiente;
- janela incompleta;
- problemas de comparabilidade;
- inferências feitas pelo modelo.

## 8.4 Alternativas

A UI deve mostrar alternativas consideradas e o motivo de não serem a recomendação principal.

## 8.5 Auditoria técnica

Em `<details>` para usuários autorizados:

- job;
- etapas;
- modelo;
- tokens;
- custo;
- prompt version;
- schema version;
- request/response brutos mascarados.

---

## 9. Decisão humana

Formulário mínimo:

```text
Decision:
- ACCEPT_RECOMMENDATION
- REJECT_RECOMMENDATION
- MODIFY_RECOMMENDATION
- DEFER_DECISION

Rationale: obrigatório
SelectedAction: opcional
RiskAcknowledgement: obrigatório para ações de alto impacto
```

A decisão deve mostrar:

- usuário;
- data/hora;
- recomendação considerada;
- snapshot considerado;
- justificativa;
- ação escolhida;
- divergência em relação ao modelo.

Editar decisão não sobrescreve histórico; cria nova versão/supersessão.

---

## 10. Comandos e diálogos de impacto

Comandos iniciais:

```text
CREATE_RUN
RUN_PREFLIGHT
REQUEST_PUBLICATION
PAUSE_RUN
RESUME_RUN
STOP_RUN
REQUEST_DECISION_ANALYSIS
REGISTER_HUMAN_DECISION
CREATE_DERIVED_EXPERIMENT_DRAFT
CREATE_COMPARISON
MARK_INCONCLUSIVE
```

### Ações de alto impacto

Exigem diálogo com resumo:

- pausar campanha;
- encerrar run;
- declarar vencedor;
- criar experimento derivado já vinculado;
- trocar orçamento/preço/oferta;
- ativar automação.

O diálogo deve exibir o que muda e o que não muda.

---

## 11. Estados de interface

## 11.1 Sem estratégia

Mensagem:

```text
Este experimento ainda não declara qual rota comercial está testando.
```

Ação: configurar estratégia.

## 11.2 Sem run

Mensagem:

```text
A hipótese comercial está configurada, mas nenhuma execução foi iniciada.
```

Ação: criar run de teste.

## 11.3 Preflight bloqueado

Mostrar:

- número de bloqueios;
- primeiro bloqueio;
- lista por grupo;
- ação de correção;
- evidências.

## 11.4 Publicação falha

Não mostrar “experimento reprovado”. Mostrar:

```text
A execução não chegou validamente ao mercado.
```

## 11.5 Dados insuficientes

Mostrar progresso para os critérios mínimos, sem barra enganosa de “sucesso”.

## 11.6 Dados atrasados

Banner persistente indicando:

- fonte;
- última sincronização;
- impacto sobre decisão;
- ação de reprocessar quando permitida.

## 11.7 Falha da IA

A tela determinística continua funcionando. Mostrar:

```text
A recomendação automática está indisponível; os dados e diagnósticos determinísticos permanecem válidos.
```

---

## 12. Permissões

Papéis mínimos:

```text
VIEWER
OPERATOR
DECISION_MAKER
ADMIN
```

Exemplo:

- `VIEWER`: leitura;
- `OPERATOR`: preflight, publicação, pausa;
- `DECISION_MAKER`: aceitar/rejeitar recomendação e declarar conclusão;
- `ADMIN`: políticas, correções e ações excepcionais.

O backend aplica autorização. O frontend apenas adapta a apresentação.

---

## 13. Acessibilidade e responsividade

- navegação completa por teclado;
- labels e descrições em inputs;
- estado não comunicado apenas por cor;
- tabelas com cabeçalho e resumo;
- gráficos com alternativa textual;
- foco controlado em diálogos;
- mensagens de erro associadas aos campos;
- mobile com resumo antes das tabelas;
- valores importantes legíveis sem hover.

---

## 14. Estratégia de implementação

### Etapa 1 — run e preflight

- `ExperimentRunSummaryCard`;
- aba de preparação;
- gates;
- timeline;
- comandos de run.

### Etapa 2 — estratégia e funil adaptativo

- card de estratégia;
- definição de funil vinda do backend;
- atualização da aba atual de funil.

### Etapa 3 — economia e qualidade

- cards econômicos;
- ledger;
- freshness;
- flags.

### Etapa 4 — comparação

- lista;
- criação;
- detalhe;
- matriz;
- funil lado a lado.

### Etapa 5 — decisão determinística

- nível de evidência;
- diagnóstico;
- comandos.

### Etapa 6 — recomendação por IA

- painel;
- evidências;
- limitações;
- decisão humana;
- histórico.

---

## 15. Testes obrigatórios

### Unitários

- formatadores;
- mapeamento de status;
- N/A versus zero;
- renderização de comandos;
- agrupamento de gates;
- evidências e limitações.

### Integração frontend

- overview completo;
- run bloqueado;
- publicação falha;
- dados atrasados;
- comparação com etapas diferentes;
- recomendação sem evidências;
- decisão humana;
- permissão insuficiente.

### E2E

1. criar run de teste;
2. executar preflight;
3. corrigir bloqueio;
4. solicitar publicação;
5. observar exposição;
6. acompanhar funil;
7. solicitar análise;
8. registrar decisão;
9. criar rascunho derivado.

---

## 16. Critérios de aceite

- [ ] usuário entende o que está sendo testado;
- [ ] falha técnica não aparece como falha de mercado;
- [ ] run e validade estão visíveis;
- [ ] nenhum KPI principal é calculado no navegador;
- [ ] N/A, zero e ausente são distintos;
- [ ] funil respeita a estratégia;
- [ ] margem e custos são visíveis;
- [ ] qualidade/freshness aparecem antes da recomendação;
- [ ] recomendação cita evidências;
- [ ] limitações ficam visíveis;
- [ ] decisão humana é separada do comando;
- [ ] comandos disponíveis vêm do backend;
- [ ] histórico explica quem decidiu e o que aconteceu;
- [ ] a operação comum não exige consulta a logs ou JSON bruto.
