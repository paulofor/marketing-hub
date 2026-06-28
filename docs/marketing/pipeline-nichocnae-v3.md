# Pipeline NichoCNAE v3 — objetivo e etapas

## Objetivo do pipeline

O pipeline **NichoCNAE v3** existe para transformar um CNAE em um perfil funcional validado de **persona, rotina, tarefas diárias, dores operacionais e evidências públicas**, sem ainda gerar oferta, campanha, landing page ou promessa comercial.

O objetivo de negócio é descobrir, com segurança, onde existe dor real em uma rotina de MEI ou profissional autônomo para que o Marketing Hub possa, em etapas posteriores, criar produtos digitais com maior chance de venda porque atacam esforço, perda de tempo, retrabalho, confusão, controle manual ou dificuldade recorrente.

O pipeline trabalha com o limite explícito de público:

- foco em **MEI, donos-operadores e profissionais autônomos**;
- não analisar funcionários CLT contratados diretamente;
- não gerar oferta antes de validar rotina e evidências;
- preservar dados auditáveis suficientes para explicar ao usuário por que o pipeline avançou ou bloqueou.

## Resultado esperado

Ao final, quando todas as etapas forem aprovadas, o pipeline deve entregar um perfil materializável contendo:

- CNAE e descrição;
- persona operacional priorizada;
- resumo da rotina;
- tarefas diárias ou recorrentes;
- dores operacionais associadas;
- sinais de compra ou busca por facilidade;
- fontes/evidências usadas;
- alavancas de facilidade que podem orientar produto futuro;
- candidato de nicho pronto para persistência pelo backend.

## Ordem das etapas

A ordem operacional atual é:

1. `cnae-intake`
2. `persona-candidate-generator`
3. `persona-tournament`
4. `routine-query-planner`
5. `source-searcher`
6. `source-fetcher`
7. `routine-signal-extractor`
8. `daily-tasks-synthesizer`
9. `quality-gate`
10. `persona-routine-materializer`

---

## 1. `cnae-intake`

### Objetivo

Receber o CNAE de entrada e preparar o contexto mínimo do pipeline.

### O que faz

- Valida se o payload possui `cnaeCode`.
- Valida se o payload possui `cnaeDescription`.
- Define o recorte de público como MEI e profissionais autônomos não CLT.
- Registra que o pipeline não deve analisar funcionários CLT contratados diretamente.
- Registra que esta versão não deve gerar oferta, campanha ou landing page.
- Direciona a próxima etapa para `persona-candidate-generator`.

### Saída principal

- CNAE recebido;
- descrição do CNAE;
- tipo de público alvo;
- fronteira de emprego;
- próxima etapa.

### Critério de bloqueio

Bloqueia se faltar CNAE ou descrição do CNAE.

---

## 2. `persona-candidate-generator`

### Objetivo

Gerar personas candidatas plausíveis para aquele CNAE, com foco em rotina operacional real.

### O que faz

- Recebe CNAE, descrição e contexto persistido.
- Monta uma requisição para geração de personas candidatas.
- Usa o cliente de geração com IA para produzir personas estruturadas.
- Completa campos técnicos de rastreabilidade, como `jobId`, `stageExecutionId`, `stage` e `status`.
- Valida se foram geradas personas suficientes.
- Direciona a próxima etapa para `persona-tournament`.

### Saída principal

- lista de personas candidatas;
- quantidade de personas;
- descrição, rotina, tarefas, interações, ferramentas e necessidades de validação de cada persona;
- próxima etapa.

### Critério de bloqueio

Bloqueia se a IA não retornar pelo menos 3 personas candidatas estruturadas.

---

## 3. `persona-tournament`

### Objetivo

Escolher a persona mais útil para investigar rotina, dor e oportunidade de produto.

### O que faz

- Lê as personas candidatas.
- Calcula uma pontuação para cada candidata.
- Prioriza personas com mais sinais de:
  - tarefas diárias;
  - dores operacionais;
  - sinais de compra;
  - perfil dono-operador, MEI, autônomo ou familiar.
- Penaliza personas que parecem funcionário CLT, auxiliar, estoquista, empregado ou cargo de retaguarda.
- Ordena o ranking.
- Seleciona a persona vencedora.
- Registra a justificativa da escolha.
- Direciona a próxima etapa para `routine-query-planner`.

### Saída principal

- persona vencedora;
- nome da persona vencedora;
- ranking das personas;
- pontuação e decomposição da pontuação;
- justificativa de seleção.

### Critério de bloqueio

Bloqueia se não houver `candidatePersonas` na entrada.

---

## 4. `routine-query-planner`

### Objetivo

Planejar buscas públicas para validar se a rotina, as tarefas, as dores e os sinais de compra da persona vencedora existem fora da geração inicial da IA.

### O que faz

- Recebe a persona vencedora.
- Extrai tarefas diárias, dores operacionais e sinais de compra.
- Gera até 8 queries planejadas.
- Cada query recebe:
  - prioridade;
  - intenção;
  - objetivo;
  - evidências esperadas.
- Registra perguntas que a busca deve responder.
- Registra critérios mínimos de aceitação de fonte.
- Registra critérios de descarte.
- Direciona a próxima etapa para `source-searcher`.

### Saída principal

- foco da persona;
- objetivo da busca;
- lista de queries planejadas;
- perguntas de validação;
- critérios de aceitação e descarte.

### Critério de bloqueio

Bloqueia se não houver `winnerPersona` na entrada.

### Observação importante

Esta etapa ainda não valida a qualidade das fontes. Ela apenas planeja as buscas. A validação acontece na etapa seguinte.

---

## 5. `source-searcher`

### Objetivo

Buscar fontes públicas e selecionar apenas aquelas que sustentam evidência real de rotina.

### O que faz

- Recebe as queries planejadas.
- Para cada query, gera variações simplificadas e operacionais.
- Busca fontes públicas usando o cliente de busca configurado.
- Qualifica cada resultado bruto.
- Calcula sinais como:
  - evidência de rotina;
  - aderência ao Brasil;
  - evidência de profissional autônomo;
  - atualidade;
  - risco comercial;
  - risco de linguagem de solução;
  - risco de desvio para empresa estruturada.
- Descarta fontes sem URL, duplicadas, comerciais/contaminadas, com evidência insuficiente ou qualidade insuficiente.
- Registra as tentativas em `searchAttempts`.
- Se encontrar fontes suficientes, direciona para `source-fetcher`.
- Se não encontrar, bloqueia o pipeline com motivo persistido.

### Saída principal

- queries planejadas;
- fontes encontradas;
- fontes selecionadas;
- tentativas de busca;
- quantidade bruta de resultados;
- quantidade de fontes qualificadas;
- quantidade de fontes rejeitadas;
- motivos de rejeição;
- decisão de bloqueio ou avanço.

### Critério de aceitação atual

Uma fonte só avança se:

- for classificada como evidência de rotina;
- tiver pontuação mínima de evidência de rotina;
- tiver pontuação mínima de qualidade;
- não for duplicada;
- não apresentar risco comercial ou risco de solução prematura.

### Critério de bloqueio

Bloqueia quando nenhuma fonte pública qualificada é encontrada.

### Observação crítica

Esta etapa é hoje o principal ponto de fragilidade do pipeline. A lógica atual usa muitos sinais lexicais e pontuação por termos. Isso é útil como filtro inicial, mas é limitado para decidir se uma fonte realmente comprova rotina, dor e esforço. O ideal é evoluir esta etapa para classificação semântica mais forte, com mais diversidade de queries, mais resultados por query e relatório melhor de rejeições.

---

## 6. `source-fetcher`

### Objetivo

Transformar fontes selecionadas em snapshots auditáveis para extração posterior de sinais.

### O que faz

- Recebe `selectedSources` ou `foundSources`.
- Valida se existe ao menos uma fonte.
- Para cada fonte, cria um snapshot funcional.
- Preserva:
  - URL;
  - título;
  - trecho de evidência;
  - tipo da fonte;
  - intenção da fonte;
  - pontuações de qualificação;
  - riscos identificados.
- Registra critérios de coleta.
- Direciona a próxima etapa para `routine-signal-extractor`.

### Saída principal

- lista de snapshots;
- quantidade de snapshots;
- campos capturados;
- critérios de coleta.

### Critério de bloqueio

Bloqueia se não houver fonte selecionada ou se nenhuma fonte puder virar snapshot auditável.

---

## 7. `routine-signal-extractor`

### Objetivo

Extrair sinais de rotina, dor e compra a partir dos snapshots coletados.

### O que faz

- Recebe `sourceSnapshots`.
- Para cada snapshot, lê o trecho de evidência.
- Gera um sinal de rotina contendo:
  - tarefa observada;
  - dor operacional inferida;
  - sinal de compra inferido;
  - URL da fonte;
  - texto de evidência;
  - confiança.
- Classifica dores como:
  - risco de erro ou retrabalho;
  - perda de tempo;
  - controle operacional manual;
  - dor operacional a validar.
- Classifica sinais de compra como:
  - procura por ferramenta ou modelo;
  - procura por ajuda especializada;
  - sinal a validar.
- Direciona a próxima etapa para `daily-tasks-synthesizer`.

### Saída principal

- lista de sinais de rotina;
- quantidade de sinais;
- regras de extração;
- próxima etapa.

### Critério de bloqueio

Bloqueia se não houver `sourceSnapshots`.

---

## 8. `daily-tasks-synthesizer`

### Objetivo

Converter sinais extraídos em um mapa claro de tarefas diárias, dores e alavancas de facilidade.

### O que faz

- Recebe `routineSignals`.
- Para cada sinal, cria uma tarefa diária estruturada.
- Preserva:
  - tarefa;
  - dor;
  - sinal de compra;
  - texto de evidência;
  - URL da fonte.
- Traduz a dor em uma alavanca de facilidade, como:
  - economizar tempo;
  - reduzir erro e retrabalho;
  - simplificar controle operacional;
  - reduzir esforço percebido.
- Registra uma leitura comercial sem gerar oferta.
- Direciona a próxima etapa para `quality-gate`.

### Saída principal

- lista de tarefas diárias;
- quantidade de tarefas;
- dores associadas;
- sinais de compra;
- alavancas de facilidade;
- leitura comercial preliminar.

### Critério de bloqueio

Bloqueia se não houver `routineSignals`.

---

## 9. `quality-gate`

### Objetivo

Decidir se há evidência suficiente para materializar o perfil final.

### O que faz

- Recebe `dailyTasks`.
- Verifica se existem ao menos duas tarefas sintetizadas.
- Verifica se ao menos uma tarefa tem fonte rastreável.
- Reforça a ausência de oferta, campanha ou landing prematura.
- Se aprovado, direciona para `persona-routine-materializer`.
- Se reprovado, bloqueia e recomenda correção em `source-searcher`.

### Saída principal

- aprovação ou bloqueio;
- quantidade de tarefas com evidência;
- critérios do gate;
- motivo da decisão;
- etapa recomendada para correção.

### Critério de aprovação

Aprova somente se:

- houver pelo menos 2 tarefas sintetizadas;
- ao menos uma tarefa tiver URL de fonte rastreável.

### Critério de bloqueio

Bloqueia quando faltam tarefas suficientes ou fonte rastreável.

---

## 10. `persona-routine-materializer`

### Objetivo

Materializar o perfil funcional final aprovado pelo gate de qualidade.

### O que faz

- Verifica se o `quality-gate` aprovou a execução.
- Recebe persona vencedora e tarefas diárias.
- Consolida o perfil materializado com:
  - nome da persona;
  - descrição;
  - resumo da rotina;
  - tarefas diárias;
  - principais dores operacionais;
  - sinais de compra;
  - fontes de evidência;
  - alavancas de facilidade;
  - aprovação pelo gate.
- Monta um candidato de nicho para o backend persistir.
- Finaliza o pipeline sem próxima etapa.

### Saída principal

- perfil materializado;
- candidato de nicho de mercado;
- readiness de materialização;
- evidências e tarefas aprovadas.

### Critério de bloqueio

Bloqueia se:

- o quality gate não estiver aprovado;
- faltar persona vencedora;
- faltar tarefas diárias.


---

## Exemplo real — execução do CNAE 4781400 em 2026-06-28

Execução usada como referência: `http://191.252.181.168:5173/oprm/cnaes/4781400/pipeline-v3`.

### Contexto da execução

- **Job:** `e38fdc95-8ce4-4405-b800-9f73ea7648b5`.
- **CNAE:** `4781400`.
- **Descrição:** Comércio varejista de artigos do vestuário e acessórios.
- **Recorte correto:** MEI, dono-operador e profissional autônomo, sem analisar funcionário CLT.
- **Fronteira comercial preservada:** não gerar oferta, campanha ou landing antes de validar rotina e evidência pública.

### Caminho percorrido

| Etapa | Resultado real | Observação |
| --- | --- | --- |
| `cnae-intake` | `COMPLETED` / `CNAE_RECEBIDO` | Recebeu CNAE e descrição corretamente. |
| `persona-candidate-generator` | `COMPLETED` / `PERSONAS_CANDIDATAS` | Gerou 4 personas candidatas operacionais. |
| `persona-tournament` | `COMPLETED` / `PERSONA_PRIORIZADA` | Escolheu a persona mais aderente ao recorte dono-operador. |
| `routine-query-planner` | `COMPLETED` / `QUERIES_PLANEJADAS` | Gerou 8 buscas para validar tarefas reais da rotina. |
| `source-searcher` | `COMPLETED` / `FONTES_NAO_COLETADAS` | Bloqueou corretamente por não encontrar fonte pública qualificada. |
| `source-fetcher` em diante | Não executadas | O pipeline parou antes porque não havia fonte segura para buscar snapshot. |

### Personas reais geradas

A etapa de geração produziu quatro possibilidades coerentes com o CNAE:

1. **Dono-operador de loja de roupas e acessórios (varejo físico)** — score `86`.
2. **MEI de moda com venda por WhatsApp e retirada/entrega local** — score `83`.
3. **Autônomo de banca/box em feira ou galeria** — score `78`.
4. **Dono-operador de ateliê/pequena confecção com venda direta de peças e ajustes** — score `71`.

A persona vencedora foi **Dono-operador de loja de roupas e acessórios (varejo físico)**, porque concentra rotina diária clara de atendimento, organização de loja, reposição, controle de recebimentos, contato com fornecedor e decisões pequenas do dono-operador.

### Exemplos reais de tarefas que viraram buscas

O `routine-query-planner` transformou a persona vencedora em 8 consultas de validação. Exemplos reais:

- atendimento no balcão/salão, apresentação de peças, sugestão de combinações e apoio à prova;
- reposição e organização de araras/prateleiras, separação por tamanho, cor e categoria;
- recebimento de mercadorias, conferência básica, etiquetagem e precificação;
- contagem pontual de itens e checagem de disponibilidade, principalmente tamanhos;
- registro de vendas, controle de recebimentos e separação de comprovantes;
- montagem ou ajuste de vitrine e exposição interna;
- contato com fornecedores para reposição e acompanhamento de entregas;
- emissão ou organização de documentos, notas, recibos, comprovantes e cadastro simples de clientes.

### Observação do bloqueio no gate atual

A execução está ficando bloqueada no **gate da etapa `source-searcher`**, antes de chegar ao `quality-gate` formal.

O motivo persistido foi:

> A etapa `source-searcher` não encontrou fontes públicas qualificadas de rotina; não é seguro avançar com queries ou fonte comercial.

Na prática, o bloqueio aconteceu porque a busca retornou resultados, mas nenhum resultado passou como evidência pública segura de rotina real. Os principais motivos de rejeição observados foram:

- **URLs duplicadas:** 42 rejeições.
- **Evidência de rotina insuficiente:** 16 rejeições.
- **Risco de contaminação comercial ou linguagem de solução:** 6 rejeições.

Exemplos reais de fontes rejeitadas:

- páginas de dicionário para o termo “dono”, como Dicio, Sinônimos e Michaelis, por não comprovarem rotina operacional;
- notícia do G1 sobre “dono” de outra empresa, por não ter relação com rotina de loja de vestuário MEI/autônoma;
- páginas comerciais como Mercado Livre, Magazine Luiza e Microsoft Store, por risco comercial ou ausência de evidência de rotina;
- páginas genéricas de agendamento ou governo, por não sustentarem tarefa diária do dono-operador de loja de roupas.

### Leitura de causa-raiz para evolução

O bloqueio é correto do ponto de vista de qualidade: o pipeline não deve avançar para `source-fetcher`, `routine-signal-extractor`, `daily-tasks-synthesizer`, `quality-gate` e materialização final sem fonte pública rastreável.

A causa operacional do bloqueio é que as consultas ainda estão muito presas à combinação literal **“dono-operador + MEI/autônomo + tarefa”**. Isso atrai resultados genéricos, dicionários, páginas comerciais ou conteúdos fora da rotina real. A próxima evolução deve gerar queries mais naturais e semânticas, por exemplo usando linguagem de busca como “rotina de loja de roupas pequena”, “como controlar estoque por tamanho em loja de roupa”, “dificuldade de atender cliente e organizar arara em loja de roupa”, sempre mantendo o filtro contra solução comercial prematura.

---

## Gates de negócio do pipeline

O pipeline possui três travas comerciais importantes:

1. **Não gerar oferta antes de evidência**  
   Todas as etapas carregam a fronteira `NAO_GERAR_OFERTA_CAMPANHA_LANDING`.

2. **Não avançar sem fonte pública qualificada**  
   O `source-searcher` bloqueia quando não encontra fonte confiável.

3. **Não materializar sem tarefas e fonte rastreável**  
   O `quality-gate` só aprova quando há tarefas suficientes e pelo menos uma fonte rastreável.

## Fragilidades conhecidas

A etapa `source-searcher` precisa evoluir porque hoje depende demais de:

- snippets de buscadores;
- lista de termos;
- pontuação lexical;
- poucas variações de query;
- volume limitado por consulta.

Isso pode rejeitar fontes úteis que não usam os termos esperados ou aceitar sinais fracos caso contenham palavras certas. Para tornar o pipeline mais robusto, a evolução recomendada é separar busca ampla de classificação semântica de evidência.

## Direção recomendada de evolução

Para aumentar a capacidade do pipeline de chegar ao final sem perder qualidade, recomenda-se:

1. aumentar a quantidade de resultados por query;
2. diversificar queries com linguagem mais próxima da dor real;
3. registrar mais exemplos de fontes rejeitadas por tentativa;
4. substituir o gate principal baseado em termos por classificação semântica de evidência;
5. diferenciar claramente fonte de rotina, fonte comercial, fonte genérica de setor e fonte de solução;
6. manter o bloqueio de qualidade para impedir avanço com evidência fraca.
