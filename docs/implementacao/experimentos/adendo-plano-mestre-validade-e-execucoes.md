# Adendo vinculante ao plano mestre — validade e execuções de experimentos

## 1. Status e precedência

Este documento complementa e, em caso de conflito, **substitui** a ordem de implementação, o primeiro incremento e a sequência de pull requests definidos em:

`docs/implementacao/experimentos/plano-mestre-evolucao-funis-produtos-personalizacao.md`

A revisão tornou-se necessária após a análise dos experimentos 37–40, consolidada em:

`docs/implementacao/experimentos/analise-historico-experimentos-sem-sucesso.md`

Os demais conceitos do plano mestre continuam válidos, especialmente:

- experimento comercial atômico;
- estratégia explícita;
- comparação como camada superior;
- produto separado de oferta;
- personalização versionada;
- identidade e consentimento do lead;
- eventos canônicos e economia unitária;
- frontend orientado à decisão;
- apoio por modelos com evidências e decisão humana.

---

## 2. Mudança central

Antes de adicionar novas rotas comerciais, o sistema precisa provar que cada tentativa realmente chegou ao mercado e gerou dados interpretáveis.

A hierarquia revisada é:

```text
Nicho
  └── Hipótese
       └── Comparação
            ├── Experimento A
            │    ├── Run 1 — teste técnico
            │    ├── Run 2 — publicação falha
            │    └── Run 3 — execução comercial válida
            └── Experimento B
                 └── Run 1 — execução comercial válida
```

### Regras

- `Experiment` representa uma pergunta comercial e uma variável primária.
- `ExperimentRun` representa uma tentativa operacional.
- Correção técnica cria novo run do mesmo experimento.
- Mudança comercial relevante cria novo experimento.
- Somente runs comercialmente válidos alimentam comparação e decisão de mercado.

A especificação completa fica em:

`docs/implementacao/experimentos/especificacao-experiment-run-preflight.md`

---

## 3. Ordem revisada das fases

## Fase 0 — Cânone, baseline e feature flags

### Objetivo

Formalizar as novas regras sem mudar o comportamento produtivo.

### Entregas

- atualizar o cânone de experimentos;
- registrar `ExperimentRun`, validade e modos `TEST`/`PRODUCTION`;
- registrar diferença entre novo run e novo experimento;
- definir política de compatibilidade com status legados;
- criar feature flags;
- transformar experimentos 37–40 em fixtures de regressão;
- documentar rollback.

### Saída

Contrato de domínio aprovado e nenhuma mudança operacional ainda ativa.

## Fase 1 — `ExperimentRun`, preflight e validade

### Objetivo

Impedir que falhas técnicas sejam registradas como rejeição comercial.

### Entregas

- `experiment_run`;
- `experiment_run_gate_result`;
- `experiment_run_step`;
- máquina de estados;
- taxonomia de falhas;
- validade da evidência;
- modo de teste separado;
- read model de run;
- card e aba de preparação no frontend;
- migração de runs legados como `NOT_EVALUATED`.

### Saída

Cada tentativa possui estado, gates, evidências, falhas e validade auditáveis.

## Fase 2 — Publicação e E2E vinculados ao run

### Objetivo

Confirmar tecnicamente a execução antes de iniciar a janela comercial.

### Entregas

- publicação Meta vinculada ao run/job;
- passos de campanha, ad set, criativo e anúncio;
- confirmação do estado efetivo;
- primeira exposição verificada;
- teste ponta a ponta de formulário, lead, e-mail, portal e checkout aplicável;
- exclusão de eventos `TEST` das métricas;
- início da janela comercial somente após exposição comprovada.

### Saída

O sistema sabe distinguir “não publicado”, “publicado sem exposição” e “em execução comercial”.

## Fase 3 — Estratégia, produto, oferta e personalização

### Objetivo

Declarar formalmente qual funil e qual oferta cada experimento testa.

### Entregas

- `experiment_strategy`;
- rotas de entrada;
- modos de captura, amostra e produto;
- `commercial_offer`;
- vínculo experimento-oferta;
- definição de personalização;
- validações por estratégia;
- card de estratégia no frontend.

### Saída

O sistema deixa de inferir o funil por combinação de campos.

## Fase 4 — Comparação de experimentos

### Objetivo

Comparar experimentos atômicos usando apenas runs válidos.

### Entregas

- `experiment_comparison`;
- braços;
- comparabilidade;
- variável alterada e controles;
- política de parada;
- seleção do run válido de cada braço;
- readiness;
- telas de lista/criação/detalhe.

### Saída

Comparações causais ficam separadas de reconstruções exploratórias.

## Fase 5 — Instant Forms, identidade, consentimento e jornada

### Objetivo

Transformar captura Meta em uma entrada confiável para personalização e venda.

### Entregas

- inbox idempotente;
- pipeline `metaleadingestion.v1`;
- identidade do lead;
- contatos protegidos;
- consentimentos por finalidade;
- magic link;
- perfil progressivo;
- enrollment de jornada pelo backend;
- observabilidade no frontend.

### Saída

Um lead pode ser acompanhado da Meta ao Portal sem duplicidade ou perda de contexto.

## Fase 6 — Eventos, atribuição e economia unitária

### Objetivo

Criar a fonte de verdade comercial para funil e decisão.

### Entregas

- fato comercial canônico;
- atribuição;
- projeção de funil por estratégia;
- ledger de custos;
- receita/reembolso;
- margem de contribuição;
- freshness e flags de qualidade;
- dual-write e reconciliação com estruturas legadas.

### Saída

O backend fornece métricas e margem por experimento, run e braço.

## Fase 7 — Centro de Decisão determinístico

### Objetivo

Permitir decisão humana completa antes da IA.

### Entregas

- overview executivo;
- preparação e execução;
- funil adaptativo;
- economia;
- comparação lado a lado;
- qualidade do dado;
- nível de evidência;
- causa-raiz determinística;
- comandos contextuais;
- histórico.

### Saída

O usuário consegue decidir sem logs, banco ou JSON bruto.

## Fase 8 — Pipeline de IA `experimentdecision.v1`

### Objetivo

Interpretar os dados confiáveis e apoiar a decisão humana.

### Entregas

- snapshot imutável;
- catálogo de evidências;
- `SIGNAL_DIAGNOSIS`;
- `ALTERNATIVE_GENERATION`;
- `RECOMMENDATION_REVIEW`;
- prompts/schemas versionados;
- auditoria de modelo/tokens/custo;
- validadores finais;
- decisão humana separada de comando;
- modo sombra.

### Saída

Recomendação estruturada, explicável e incapaz de agir sozinha.

## Fase 9 — Personalização, entrega e fallback

### Objetivo

Operar amostras e produtos genéricos ou personalizados.

### Entregas

- campos versionados de personalização;
- perfil parcial/completo;
- geração de amostra;
- oferta personalizada;
- fulfillment pago;
- fallback genérico;
- SLA;
- custo por geração;
- acompanhamento no frontend.

### Saída

As rotas originalmente discutidas ficam operacionais e comparáveis.

## Fase 10 — Calibração e automação gradual

### Objetivo

Avaliar recomendações e liberar somente automações de baixo risco.

### Entregas

- feedback recomendação versus decisão humana;
- resultado posterior;
- métricas de qualidade do pipeline;
- calibração de prompts/thresholds;
- políticas automáticas versionadas;
- rollback.

### Saída

Automação baseada em evidência, sem entregar ao modelo decisões comerciais irreversíveis.

---

## 4. Sequência revisada de pull requests

1. **PR 1 — cânone, fixtures históricas e feature flags**.
2. **PR 2 — persistência e API de `ExperimentRun`**.
3. **PR 3 — gates, validade e taxonomia de falha**.
4. **PR 4 — frontend de run/preflight**.
5. **PR 5 — publicação Meta vinculada ao run**.
6. **PR 6 — preflight funcional e separação TEST/PRODUCTION**.
7. **PR 7 — estratégia comercial do experimento**.
8. **PR 8 — produto, oferta e personalização**.
9. **PR 9 — comparação de experimentos**.
10. **PR 10 — ingestão Meta idempotente**.
11. **PR 11 — identidade, consentimento e magic link**.
12. **PR 12 — eventos e atribuição**.
13. **PR 13 — economia unitária**.
14. **PR 14 — Centro de Decisão determinístico**.
15. **PR 15 — backend do pipeline de decisão v1**.
16. **PR 16 — AI Worker do pipeline de decisão v1**.
17. **PR 17 — recomendação e decisão humana no frontend**.
18. **PR 18 — personalização e fallback**.
19. **PR 19 — calibração e automação segura**.

Cada PR deve possuir rollback independente e não misturar mudança estrutural, migração e automação comercial de alto impacto no mesmo pacote.

---

## 5. Primeiro incremento definitivo

O primeiro incremento de código não é mais `experiment_strategy`.

Ele deve entregar:

- persistência de `ExperimentRun`;
- modo `TEST` e `PRODUCTION`;
- status e validade;
- criação sequencial por experimento;
- migração segura de legado;
- endpoint de leitura/criação;
- card de execução atual;
- histórico mínimo;
- testes de domínio e API.

Não deve ainda:

- alterar publicação Meta;
- criar comparação;
- mudar captação de leads;
- ativar IA;
- remover status existentes.

---

## 6. Critérios adicionais de concluído

Além dos critérios do plano mestre:

- [ ] toda tentativa operacional pertence a um run;
- [ ] eventos de teste e produção são separados;
- [ ] janela comercial começa após exposição comprovada;
- [ ] falha técnica não reprova hipótese;
- [ ] somente runs válidos entram em comparação;
- [ ] o frontend explica o motivo da validade/invalidez;
- [ ] o modelo recebe runs inválidos somente como aprendizado operacional;
- [ ] os experimentos 37–40 permanecem como testes de regressão do processo decisório.
