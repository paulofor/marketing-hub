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

## 2. Documentos

### Documento mestre

Seguir como roadmap principal:

`docs/implementacao/experimentos/plano-mestre-evolucao-funis-produtos-personalizacao.md`

Esse documento define:

- diagnóstico do estado atual;
- decisões arquiteturais;
- modelo de domínio;
- jornadas comerciais suportadas;
- contratos e APIs;
- mensuração e economia unitária;
- fases de implementação;
- sequência de pull requests;
- migração e compatibilidade;
- critérios globais de aceite.

### Especificação obrigatória de frontend

Usar para construir as telas de acompanhamento e decisão:

`docs/implementacao/experimentos/especificacao-centro-de-decisao-frontend.md`

Esse documento detalha:

- arquitetura de informação;
- telas e abas;
- DTOs de leitura;
- comparação entre braços;
- funil, economia e qualidade de dados;
- apresentação das recomendações dos modelos;
- comandos permitidos ao usuário;
- estados vazios, falhas e bloqueios;
- critérios de usabilidade e aceite.

### Especificação obrigatória de IA

Usar para implementar o apoio à decisão por modelos:

`docs/implementacao/experimentos/especificacao-pipeline-decisao-ia-v1.md`

Esse documento detalha:

- separação entre cálculo determinístico, análise do modelo e decisão humana;
- pipeline versionado `experimentdecision.v1`;
- estágios, contratos, prompts e schemas;
- evidências e confiança;
- políticas de autorização;
- auditoria de request/response, tokens e custos;
- regras que impedem o modelo de inventar métricas ou tomar ações de alto impacto sozinho.

## 3. Decisão estrutural principal

O registro atual `Experiment` continuará representando **um teste comercial atômico**, com uma rota, uma promessa, uma oferta e uma variável primária.

A comparação A/B não será implementada colocando várias variantes dentro do mesmo `Experiment`. Será criada uma camada superior, chamada neste plano de `ExperimentComparison`, que agrupa dois ou mais experimentos comparáveis.

Hierarquia alvo:

```text
Nicho
  └── Hipótese
       └── Comparação de experimentos
            ├── Experimento A — controle
            └── Experimento B — desafiante
```

Essa decisão:

- preserva o contrato atual de `Experiment`;
- mantém Instant Form, Lead Portal, campanha, landing e jornada com vínculo inequívoco;
- evita misturar eventos e custos de braços diferentes;
- permite rollout incremental e compatibilidade com experimentos existentes;
- mantém a regra canônica de uma única hipótese comercial por experimento.

## 4. Regra de decisão

A ordem de precedência será:

```text
Qualidade e integridade dos dados
  → regras determinísticas de negócio/estatística
    → recomendação estruturada do modelo
      → decisão humana ou política previamente aprovada
```

O modelo pode:

- interpretar sinais;
- explicar gargalos;
- classificar hipóteses;
- sugerir o próximo teste;
- indicar riscos e lacunas;
- recomendar manter, pausar, iterar ou encerrar.

O modelo não pode, por padrão:

- inventar métricas;
- declarar vencedor com dados insuficientes;
- publicar campanhas;
- elevar orçamento;
- mudar oferta, preço ou promessa;
- encerrar definitivamente um experimento;
- usar dados pessoais do lead sem necessidade funcional.

Ações com impacto comercial continuam exigindo comando humano, salvo política automática explícita, versionada, auditável e aprovada.

## 5. Ordem recomendada de implementação

1. **Fase 0 — cânone, baseline e feature flags**.
2. **Fase 1 — estratégia comercial do experimento, produto, oferta e personalização**.
3. **Fase 2 — comparação de experimentos e contratos administrativos**.
4. **Fase 3 — ingestão idempotente de Instant Forms e identidade do lead**.
5. **Fase 4 — funil unificado, atribuição e economia unitária**.
6. **Fase 5 — Centro de Decisão no frontend sem IA**.
7. **Fase 6 — pipeline `experimentdecision.v1` no AI Worker e backend**.
8. **Fase 7 — personalização, geração, entrega e fallback genérico**.
9. **Fase 8 — testes controlados, calibração e automação gradual**.

Não iniciar a camada de decisão por IA antes de consolidar identidade, atribuição, custos e qualidade dos dados. Um modelo sofisticado sobre dados incompletos apenas produzirá recomendações convincentes e pouco confiáveis.

## 6. Primeiro incremento recomendado

O primeiro incremento implementável deve conter:

1. `experiment_strategy` em relação 1:1 com `experiment`;
2. enums de rota de entrada, amostra, produto e objetivo de conversão;
3. backfill seguro para experimentos existentes;
4. endpoints de leitura e atualização da estratégia;
5. card de estratégia na visão geral do experimento;
6. validações que preservem uma única rota e uma única variável primária;
7. testes de domínio, API e frontend;
8. nenhuma mudança ainda na publicação Meta ou no processamento de leads.

Resultado esperado: o Marketing Hub passa a declarar explicitamente **qual funil está sendo testado**, sem alterar o comportamento operacional atual.

## 7. Definição global de concluído

A evolução estará concluída quando:

- Instant Form, landing e venda direta forem rotas de primeira classe;
- produtos e ofertas genéricos/personalizados forem representados sem ambiguidade;
- um lead puder ser identificado e acompanhado entre Meta, Portal, e-mail, checkout e entrega;
- eventos forem idempotentes e atribuídos a um único experimento;
- comparações mostrarem variável alterada e variáveis controladas;
- margem de contribuição e métricas de funil forem calculadas no backend;
- o frontend informar estado, dados, qualidade, recomendação, riscos e próximo comando;
- recomendações de IA tiverem evidências, confiança, limitações e auditoria;
- decisões humanas e ações executadas forem registradas;
- todos os fluxos respeitarem a arquitetura backend como fonte de verdade;
- workers executarem apenas por contratos oficiais e reportarem resultados ao backend;
- o sistema conseguir recomendar e executar o próximo teste sem perder o histórico do aprendizado anterior.
