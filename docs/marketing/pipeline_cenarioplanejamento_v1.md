# Pipeline CenarioPlanejamento v1

## Objetivo

O pipeline **CenarioPlanejamento v1** tem como objetivo transformar um contexto de negócio, nicho ou hipótese em cenários estruturados de planejamento, permitindo comparar caminhos possíveis antes de avançar para decisões comerciais mais caras, como criação de oferta, campanha, landing page, produção de ativos ou mídia paga.

A função principal do pipeline é reduzir risco e esforço operacional: em vez de escolher uma ação diretamente, o sistema cria alternativas, avalia impacto, custo, velocidade, risco e potencial de aprendizado, e recomenda o cenário mais adequado para o próximo movimento do Marketing Hub.

## Explicação geral

Um cenário de planejamento é uma possibilidade de execução organizada em torno de uma tese operacional. Cada cenário descreve o que fazer, por que fazer, quais recursos usar, quais riscos existem, quais evidências sustentam a escolha e qual resultado esperado pode ser medido.

Este pipeline deve ser pequeno, objetivo e auditável. Ele não substitui pipelines de pesquisa, hipótese, oferta, campanha ou landing page. Ele atua como uma camada intermediária de decisão para escolher o melhor caminho antes de acionar esses fluxos posteriores.

O pipeline deve persistir entradas, saídas, decisões, justificativas, riscos, evidências e próximos passos para que o frontend consiga explicar ao usuário o raciocínio do planejamento sem depender de logs técnicos.

## Quando usar

Use este pipeline quando o sistema já possuir contexto suficiente para planejar, mas ainda precisar decidir entre caminhos possíveis, por exemplo:

- escolher entre diferentes abordagens comerciais para um nicho;
- comparar cenários de baixo custo, alta velocidade ou maior qualidade de aprendizado;
- decidir se vale avançar para oferta, campanha, landing ou experimento;
- organizar um plano inicial antes de gastar mídia;
- transformar uma hipótese ampla em alternativas práticas de execução.

## Quando não usar

Não use este pipeline para:

- pesquisar a realidade inicial de um nicho;
- definir CNAE, subnicho ou público executor do zero;
- materializar hipótese final validada;
- criar diretamente campanha, copy, landing page ou oferta completa;
- tomar decisões automáticas de gasto sem gate comercial posterior.

## Contrato operacional

- **Nome do pipeline:** `cenario-planejamento`
- **Versão:** `v1`
- **Tipo:** pipeline pequeno de decisão e planejamento
- **Entrada principal:** contexto de nicho, hipótese ou objetivo comercial
- **Saída principal:** cenário recomendado com justificativa, riscos e próximo passo
- **Executor sugerido:** módulo responsável pelo fluxo comercial que solicitar o planejamento
- **Backend:** fonte de verdade para persistência, status, relatório e callbacks
- **IA:** usada para geração, análise e síntese dos cenários
- **Modelo OpenAI sugerido:** modelo configurável por etapa, com padrão operacional em modelo de raciocínio/copy estratégico disponível no catálogo do sistema
- **Modo OpenAI:** usar `service_tier: "flex"` por padrão quando o provedor for OpenAI

## Etapas do pipeline

### 1. Preparação do contexto

#### Objetivo

Consolidar as informações disponíveis antes de gerar cenários, garantindo que o planejamento parta de dados reais e não de suposições soltas.

#### Entradas

- Identificador do job ou ciclo.
- Nicho ou subnicho.
- Público-alvo conhecido.
- Objetivo do planejamento.
- Contexto operacional já persistido.
- Evidências disponíveis.
- Restrições informadas pelo usuário ou pelo sistema.
- Histórico de decisões anteriores, quando existir.

#### Processamento

- Buscar no backend os dados persistidos do contexto.
- Separar fatos, hipóteses e lacunas.
- Identificar restrições relevantes: orçamento, prazo, canal, risco, capacidade operacional e maturidade do nicho.
- Montar um resumo estruturado para as próximas etapas.

#### Saídas

- Contexto consolidado.
- Lista de fatos confirmados.
- Lista de hipóteses ainda não validadas.
- Lista de lacunas de informação.
- Restrições operacionais do planejamento.

#### Recursos usados

- Backend principal para leitura do contexto.
- Banco de dados via backend.
- Artefatos persistidos de pipelines anteriores.
- Sem necessidade obrigatória de IA, salvo quando for preciso resumir contexto longo.

---

### 2. Geração de cenários

#### Objetivo

Criar alternativas práticas de planejamento que representem caminhos diferentes de execução.

#### Entradas

- Contexto consolidado da etapa anterior.
- Objetivo do planejamento.
- Restrições operacionais.
- Critérios mínimos de criação de cenário.
- Quantidade desejada de cenários, preferencialmente entre 2 e 5.

#### Processamento

- Usar IA para propor cenários distintos e comparáveis.
- Evitar cenários duplicados ou apenas variações superficiais.
- Garantir que cada cenário tenha uma tese clara de execução.
- Classificar cada cenário por orientação principal, por exemplo:
  - baixo custo;
  - maior velocidade;
  - maior potencial de venda;
  - maior aprendizado;
  - menor risco;
  - maior diferenciação.

#### Saídas

Para cada cenário gerado:

- Nome do cenário.
- Tese central.
- Público ou recorte priorizado.
- Ação principal proposta.
- Canal sugerido, quando aplicável.
- Hipótese comercial associada.
- Resultado esperado.
- Principais riscos.
- Evidências ou sinais que sustentam o cenário.
- Dependências para execução.

#### Recursos usados

- Modelo OpenAI configurado para planejamento estratégico.
- Prompt versionado no módulo executor responsável.
- Schema JSON versionado para validar a saída.
- Registro auditável do request enviado e response bruto recebido.

---

### 3. Avaliação dos cenários

#### Objetivo

Comparar os cenários por critérios objetivos para evitar decisão baseada apenas em preferência subjetiva.

#### Entradas

- Cenários gerados.
- Contexto consolidado.
- Critérios de avaliação.
- Restrições comerciais e operacionais.

#### Processamento

- Atribuir pontuação ou classificação para cada cenário.
- Avaliar cada cenário nos critérios mínimos:
  - potencial de venda;
  - velocidade de execução;
  - custo estimado;
  - risco operacional;
  - risco comercial;
  - clareza para o público;
  - facilidade de validação;
  - qualidade do aprendizado gerado;
  - dependência de ativos ainda inexistentes.
- Separar falhas técnicas de decisões comerciais.
- Identificar cenários inviáveis ou que exigem aprovação humana.

#### Saídas

Para cada cenário avaliado:

- Pontuações por critério.
- Diagnóstico resumido.
- Forças do cenário.
- Fraquezas do cenário.
- Riscos principais.
- Condições de execução.
- Status recomendado: `APROVAVEL`, `REVISAR`, `BLOQUEADO` ou `DESCARTADO`.

#### Recursos usados

- Modelo OpenAI para análise comparativa, quando houver avaliação qualitativa.
- Regras determinísticas para critérios objetivos conhecidos.
- Backend para persistência da avaliação.
- Schema JSON para garantir saída estruturada.

---

### 4. Seleção do cenário recomendado

#### Objetivo

Escolher o melhor cenário para o próximo movimento, com justificativa clara e auditável.

#### Entradas

- Cenários avaliados.
- Pontuações e diagnósticos.
- Objetivo do planejamento.
- Restrições do usuário ou do sistema.
- Critérios de bloqueio ou aprovação.

#### Processamento

- Comparar os cenários aprováveis.
- Selecionar o cenário mais alinhado ao objetivo principal.
- Justificar por que ele venceu os demais.
- Registrar por que os outros cenários não foram escolhidos.
- Se nenhum cenário for seguro, recomendar revisão, coleta adicional ou decisão humana.

#### Saídas

- Cenário recomendado.
- Justificativa da recomendação.
- Cenários alternativos mantidos.
- Cenários descartados.
- Motivos de descarte.
- Riscos do cenário vencedor.
- Condições mínimas para avançar.
- Próximo passo recomendado.

#### Recursos usados

- Modelo OpenAI para síntese decisória, quando necessário.
- Regras de gate para impedir avanço inseguro.
- Backend para persistência da decisão.

---

### 5. Gate de decisão

#### Objetivo

Impedir que o planejamento avance para execução quando houver risco comercial, falta de contexto, custo injustificado ou ausência de cenário minimamente validável.

#### Entradas

- Cenário recomendado.
- Avaliação dos cenários.
- Riscos identificados.
- Lacunas do contexto.
- Próximo passo proposto.

#### Processamento

- Verificar se o cenário recomendado possui clareza suficiente para execução.
- Verificar se existem riscos que exigem aprovação humana.
- Verificar se o próximo passo envolve gasto, publicação, exposição de marca ou criação de ativo comercial sensível.
- Bloquear avanço automático quando a decisão exigir validação humana ou contrato posterior completo.

#### Saídas

- Decisão do gate: `APROVADO`, `APROVADO_COM_RESSALVAS`, `BLOQUEADO` ou `REQUER_DECISAO_HUMANA`.
- Motivo da decisão.
- Impacto comercial esperado.
- Causa-raiz do bloqueio, quando houver.
- Ação recomendada para desbloqueio.

#### Recursos usados

- Regras determinísticas de bloqueio.
- Modelo OpenAI apenas como apoio de análise, nunca como única fonte de decisão para gasto ou publicação.
- Backend para registrar decisão e motivo.

---

### 6. Consolidação do plano de cenário

#### Objetivo

Gerar uma saída final simples, útil e legível para o usuário, pronta para alimentar o próximo fluxo do Marketing Hub.

#### Entradas

- Cenário recomendado.
- Decisão do gate.
- Justificativas.
- Riscos.
- Evidências.
- Próximo passo recomendado.

#### Processamento

- Montar resumo executivo do planejamento.
- Separar conteúdo funcional de metadados técnicos.
- Preparar payload estruturado para frontend e para pipelines posteriores.
- Registrar artefatos finais e auditoria.

#### Saídas

- Resumo executivo.
- Cenário escolhido.
- Objetivo do cenário.
- Plano de ação inicial.
- Riscos e mitigação.
- Evidências usadas.
- Próximo pipeline ou etapa recomendada.
- Status final do planejamento.

#### Recursos usados

- Backend para persistência da saída consolidada.
- Modelo OpenAI para redação do resumo executivo, quando necessário.
- Schema JSON de saída final.

## Saída final esperada

A saída final do pipeline deve permitir que o usuário entenda rapidamente:

- quais cenários foram considerados;
- qual cenário foi recomendado;
- por que ele foi recomendado;
- quais riscos ainda existem;
- qual decisão foi tomada pelo gate;
- qual é o próximo passo mais simples e eficaz.

## Dados mínimos a persistir

Cada execução deve persistir, no mínimo:

- `jobId` ou identificador equivalente da execução;
- versão do pipeline;
- etapa atual;
- status da etapa;
- horários de início e fim;
- entrada estruturada;
- saída estruturada;
- cenários gerados;
- avaliações por cenário;
- decisão do gate;
- riscos;
- evidências;
- erros técnicos, quando existirem;
- custos de IA, quando disponíveis;
- modelo usado;
- request enviado ao modelo;
- response bruto recebido do modelo.

## Recursos de IA

Quando usar OpenAI, cada etapa com IA deve seguir estas regras:

- prompt operacional em arquivo versionado do módulo executor;
- schema JSON em arquivo versionado do módulo executor;
- request e response brutos registrados de forma auditável;
- modelo, tokens, custo, status e erro persistidos quando disponíveis;
- uso de `service_tier: "flex"` por padrão;
- validação da resposta antes de aplicar resultado no backend;
- nenhuma resposta inválida deve ser tratada como sucesso técnico.

## Relação com outros pipelines

Este pipeline pode consumir saídas de pipelines anteriores, como pesquisa de nicho, análise de rotina, hipótese ou biblioteca de evidências. Ele também pode alimentar pipelines posteriores, como:

- hipótese;
- oferta;
- campanha;
- landing page;
- experimento pago;
- plano de conteúdo;
- revisão humana.

O CenarioPlanejamento v1 não deve decidir sozinho por publicação, gasto ou execução externa. Quando o próximo passo envolver risco comercial, mídia paga, exposição pública ou criação de ativo final, a decisão deve passar por gate ou fluxo específico posterior.

## Critério de sucesso

O pipeline é considerado bem-sucedido quando entrega uma recomendação clara de cenário, com justificativa compreensível, riscos explícitos, evidências registradas e próximo passo acionável.

O pipeline deve falhar de forma controlada quando não houver contexto suficiente para comparar cenários com segurança.
