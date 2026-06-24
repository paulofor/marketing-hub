# Implementação — evolução comercial de Experimentos

## 1. Objetivo

Este diretório organiza a evolução do Marketing Hub para suportar, de forma comparável e auditável:

- captura por **Meta Instant Form**;
- captura por **landing própria**;
- venda direta de produto genérico low-ticket;
- amostra genérica ou personalizada;
- enriquecimento progressivo do perfil do lead;
- produto genérico ou gerado por lead;
- comparação entre rotas comerciais;
- recomendações produzidas por modelos de IA;
- decisão humana apoiada por dados, evidências e regras determinísticas.

O objetivo principal permanece o mesmo do Marketing Hub: **gerar vendas de produtos digitais que entreguem valor real**, preservando Dor → Resultado → Mecanismo → Prova → Oferta.

## 2. Documentos e precedência

### 2.1 Diagnóstico histórico obrigatório

Ler antes de qualquer implementação:

`docs/implementacao/experimentos/analise-historico-experimentos-sem-sucesso.md`

Esse documento consolida os experimentos 37–40 e demonstra que o status final não pode ser interpretado isoladamente como rejeição de mercado. Ele define requisitos adicionais de:

- separação entre experimento e execução (`ExperimentRun`);
- validade da evidência comercial;
- preflight ponta a ponta;
- confirmação de exposição real;
- taxonomia de falha técnica, estratégica, de medição e comercial;
- qualidade dos artefatos vindos de nicho e hipótese;
- distinção entre correção técnica e novo experimento comercial.

### 2.2 Documento mestre

Roadmap funcional e arquitetural amplo:

`docs/implementacao/experimentos/plano-mestre-evolucao-funis-produtos-personalizacao.md`

Esse documento define:

- diagnóstico do estado atual;
- decisões arquiteturais;
- modelo de domínio;
- jornadas comerciais suportadas;
- contratos e APIs;
- mensuração e economia unitária;
- migração e compatibilidade;
- critérios globais de aceite.

### 2.3 Adendo vinculante do plano mestre

Usar como fonte de verdade para ordem das fases, primeiro incremento e sequência de PRs:

`docs/implementacao/experimentos/adendo-plano-mestre-validade-e-execucoes.md`

O adendo substitui as partes conflitantes do plano mestre após incorporar o aprendizado dos experimentos 37–40. A prioridade passa a ser provar a validade da execução antes de ampliar rotas, comparações e decisões por IA.

### 2.4 Especificação obrigatória de `ExperimentRun`

Usar para implementar tentativas operacionais, preflight e validade da evidência:

`docs/implementacao/experimentos/especificacao-experiment-run-preflight.md`

Esse documento detalha:

- quando criar novo run ou novo experimento;
- modelo de dados;
- máquina de estados;
- gates de qualidade, desenho, ativos, E2E, Meta e mensuração;
- modo `TEST` versus `PRODUCTION`;
- validade da evidência;
- contratos administrativos e internos;
- read model para o frontend;
- migração, testes e sequência de PRs.

### 2.5 Especificação obrigatória de frontend

Usar para construir as telas de acompanhamento e decisão:

`docs/implementacao/experimentos/especificacao-centro-de-decisao-frontend.md`

Esse documento detalha:

- arquitetura de informação;
- telas e abas;
- DTOs de leitura;
- comparação entre braços;
- execução atual e histórico de runs;
- funil, economia e qualidade de dados;
- apresentação das recomendações dos modelos;
- comandos permitidos ao usuário;
- estados vazios, falhas e bloqueios;
- critérios de usabilidade e aceite.

### 2.6 Especificação obrigatória de IA

Usar para implementar o apoio à decisão por modelos:

`docs/implementacao/experimentos/especificacao-pipeline-decisao-ia-v1.md`

Esse documento detalha:

- separação entre cálculo determinístico, análise do modelo e decisão humana;
- pipeline versionado `experimentdecision.v1`;
- estágios, contratos, prompts e schemas;
- catálogo e referências de evidência;
- políticas de autorização;
- auditoria de request/response, tokens e custos;
- regras que impedem o modelo de inventar métricas ou tomar ações de alto impacto sozinho.

### 2.7 Regra de precedência

Em caso de conflito:

1. o cânone vigente governa regras já aprovadas do sistema;
2. o diagnóstico histórico governa os problemas que precisam ser prevenidos;
3. o adendo governa ordem de execução e prioridade;
4. a especificação de `ExperimentRun` governa validade e preflight;
5. o plano mestre governa o desenho funcional amplo;
6. as especificações de frontend e IA governam suas respectivas implementações.

Antes de implementar mudanças de regra, o documento canônico correspondente deve ser atualizado.

## 3. Decisão estrutural principal

O registro atual `Experiment` continuará representando **um teste comercial atômico**, com uma rota, uma promessa, uma oferta e uma variável primária.

Cada tentativa operacional será representada por um `ExperimentRun`. Correções técnicas que não alteram a variável comercial criam novo run do mesmo experimento. Mudanças de dor, promessa, isca, preço, oferta, rota ou público primário criam um novo experimento.

A comparação A/B não será implementada colocando várias variantes dentro do mesmo `Experiment`. Será criada uma camada superior, chamada neste plano de `ExperimentComparison`, que agrupa dois ou mais experimentos comparáveis.

Hierarquia alvo:

```text
Nicho
  └── Hipótese
       └── Comparação de experimentos
            ├── Experimento A — controle
            │    ├── Run técnico
            │    └── Run comercial válido
            └── Experimento B — desafiante
                 └── Run comercial válido
```

Essa decisão:

- preserva o contrato atual de `Experiment`;
- mantém Instant Form, Lead Portal, campanha, landing e jornada com vínculo inequívoco;
- evita misturar eventos e custos de braços diferentes;
- impede que falha de publicação ou formulário reprove a hipótese comercial;
- permite rollout incremental e compatibilidade com experimentos existentes;
- mantém a regra canônica de uma única hipótese comercial por experimento.

## 4. Regra de decisão

A ordem de precedência será:

```text
Validade da execução
  → qualidade e integridade dos dados
    → regras determinísticas de negócio/estatística
      → recomendação estruturada do modelo
        → decisão humana ou política previamente aprovada
```

O modelo pode:

- interpretar sinais;
- explicar gargalos;
- classificar causas prováveis;
- sugerir o próximo teste;
- indicar riscos e lacunas;
- recomendar manter, pausar, iterar ou encerrar.

O modelo não pode, por padrão:

- inventar métricas;
- usar runs tecnicamente inválidos como evidência de mercado;
- declarar vencedor com dados insuficientes;
- publicar campanhas;
- elevar orçamento;
- mudar oferta, preço ou promessa;
- encerrar definitivamente um experimento;
- usar dados pessoais do lead sem necessidade funcional.

Ações com impacto comercial continuam exigindo comando humano, salvo política automática explícita, versionada, auditável e aprovada.

## 5. Ordem recomendada de implementação

1. **Fase 0 — cânone, baseline e feature flags**.
2. **Fase 1 — `ExperimentRun`, preflight, validade e taxonomia de falha**.
3. **Fase 2 — publicação e E2E vinculados ao run**.
4. **Fase 3 — estratégia comercial, produto, oferta e personalização**.
5. **Fase 4 — comparação de experimentos**.
6. **Fase 5 — Instant Forms, identidade, consentimento e jornada**.
7. **Fase 6 — eventos, atribuição e economia unitária**.
8. **Fase 7 — Centro de Decisão determinístico no frontend**.
9. **Fase 8 — pipeline `experimentdecision.v1`**.
10. **Fase 9 — personalização, entrega e fallback genérico**.
11. **Fase 10 — calibração e automação gradual**.

Não iniciar a camada de decisão por IA antes de consolidar validade do run, identidade, atribuição, custos e qualidade dos dados. Um modelo sofisticado sobre dados incompletos apenas produzirá recomendações convincentes e pouco confiáveis.

## 6. Primeiro incremento recomendado

O primeiro incremento implementável deve conter:

1. `experiment_run` ligado ao experimento;
2. modo `TEST` e `PRODUCTION`;
3. estados de preflight/publicação/execução;
4. classificação da validade da evidência;
5. taxonomia inicial de falhas;
6. checklist de readiness persistido no backend;
7. linha do tempo do run no frontend;
8. separação de eventos de teste e produção;
9. nenhuma mudança ainda na estratégia comercial ou IA.

Resultado esperado: o Marketing Hub deixa de confundir falha de execução com rejeição de mercado.

O incremento seguinte vincula publicação e teste E2E ao run. Somente depois entra `experiment_strategy` para declarar explicitamente qual funil está sendo testado.

## 7. Definição global de concluído

A evolução estará concluída quando:

- cada tentativa operacional estiver associada a um run auditável;
- somente runs comercialmente válidos alimentarem decisão de mercado;
- Instant Form, landing e venda direta forem rotas de primeira classe;
- produtos e ofertas genéricos/personalizados forem representados sem ambiguidade;
- um lead puder ser identificado e acompanhado entre Meta, Portal, e-mail, checkout e entrega;
- eventos forem idempotentes e atribuídos a um único experimento/run;
- comparações mostrarem variável alterada e variáveis controladas;
- margem de contribuição e métricas de funil forem calculadas no backend;
- o frontend informar estado, validade, dados, qualidade, recomendação, riscos e próximo comando;
- recomendações de IA tiverem evidências, confiança, limitações e auditoria;
- decisões humanas e ações executadas forem registradas;
- todos os fluxos respeitarem a arquitetura backend como fonte de verdade;
- workers executarem apenas por contratos oficiais e reportarem resultados ao backend;
- o sistema conseguir recomendar e criar o próximo teste sem perder o histórico do aprendizado anterior.
