# Recuperação da preparação comercial Opala v1 — 16/09/2026

## Escopo preservado

- Produto Vega, ID 4, tipo oficial `PDE` (Opala).
- Cadeia 14, processo `opala-commercial-preparation-v1` v1, definição 77.
- Ciclo 2, experimento 92, versão
  `musa-pde-entry-v12-primeiro-ajuste-aplicavel`.
- Execução automática 8 e tentativa histórica 428 preservadas.

## Estado observado antes da correção

Consulta read-only da tela, banco e logs pelo MCP confirmou três fatos independentes:

1. a execução 8 estava na fila porque a execução raiz 4 em `ERROR` ainda reservava o produto;
2. o experimento 92 não possuía `follow_up_action_url`, mas havia exatamente uma candidata v8
   do mesmo experimento e versão, com `https://v8.clubemusa.com.br` e contrato v12 em rascunho;
3. o contrato publicado do produto continuava na v7, como deve ocorrer até a promoção, e a URL
   v8 publicada externamente ainda respondia com os diagnósticos da v7.

Não houve escrita em produção, retentativa paga, publicação, cobrança ou liberação de mídia.

## Alternativas avaliadas

| Alternativa | Benefício | Risco | Esforço | Decisão |
|---|---|---|---|---|
| Preencher manualmente a URL do experimento e alterar a execução 4 | Recuperação rápida deste registro | Repete intervenção nos próximos ciclos, ignora contrato da candidata e perde a causa da fila | Baixo | Rejeitada |
| Pedir ao LLM que escolha uma URL do histórico | Pouca alteração no backend | Escolha não determinística, risco de usar predecessor/preview e novo consumo sem progresso | Baixo | Rejeitada |
| Resolver candidata exata no backend e liberar estados terminais da fila | Corrige o caso e as próximas execuções sem enfraquecer gates | Exige regressão de fila, identidade, ambiguidade e versão | Médio | Escolhida |

O backend, e não o modelo, passa a distinguir dado explícito, inferência determinística e
ausência. Isso segue a ideia de *Context Sufficiency Gate* descrita em
`pesquisas/agentes-inteligentes/2026-09-13-agentes-inteligentes.md`, seção 1, e o ciclo
*propose–probe–commit* resumido em
`pesquisas/agentes-inteligentes/2026-09-12-agentes-inteligentes.md`, seção 1. São referências
de desenho; o ganho local é comprovado somente pelos testes abaixo.

## Matriz definida antes da homologação

| Área | Cenários obrigatórios | Critério |
|---|---|---|
| Caminho feliz | erro terminal anterior + execução nova; candidata única com contrato v12 e produto publicado v7 | fila liberada, Dédalo recebe URL/contrato v12 e o callback vincula a URL sem publicar |
| Validações | URL explícita divergente; duas candidatas; candidata pausada/retirada; versão diferente | bloqueio claro e zero substituição silenciosa |
| Retomada e histórico | tentativa #428 preservada; novo identificador sintético; replay do callback | nova ocorrência independente, idempotência e custos anteriores intactos |
| Integrações | backend, Catálogo Vivo, quatro workers, PDE v8 e checkout simulado | contratos compatíveis; nenhum gasto, cobrança ou chamada paga |
| Observabilidade | contexto com `destinationSource`, diário e custo | origem explícita e correlação por produto/ciclo/experimento/versão |
| Métricas | eventos de QA segregados e ausência de alegação de venda | testes técnicos não contabilizados como receita ou venda |
| Interface | processo e retorno ao pai em desktop, iPhone 15 Pro e Pixel 7 | carregamento sem erro, numeração 5.2.x e comandos coerentes |
| Publicação | diagnóstico local v8 e gate produtivo | v8 local serve v12; produção permanece bloqueada até PR, deploy seletivo e nova homologação |

## Resultados

### Causa e candidata aceita

A correção fecha duas causas independentes, sem exceção para os IDs atuais:

1. `ERROR` era terminal na fila de conciliação, mas continuava reservando o produto na consulta
   de raízes ativas. A execução 4 permanecia preservada e impedia a execução 8 de avançar.
2. O contexto do agente expunha a candidata v12 na lista, mas buscava a URL somente no experimento
   e o contrato somente no produto publicado v7. A própria candidata exata não podia satisfazer a
   atividade `entry`.

A candidata aceita usa a identidade produto + experimento + versão do ciclo. Ela vincula a URL
somente no callback aceito, preserva o contrato público v7 e bloqueia URL divergente, duplicidade,
slot pausado ou retirado. A repetição do callback não duplica a escrita.

| Comparação | Baseline publicada | Candidata local |
|---|---|---|
| Reserva da fila | raiz 4 em `ERROR` retém o produto | `ERROR` preserva histórico/custo e libera a vez |
| Fonte da entrada | URL ausente + contrato v7 | slot único 8 fornece URL e contrato v12 |
| Segurança | nova tentativa paga repetiria o impedimento | ambiguidade, divergência e slot inativo bloqueiam antes do agente |
| Intervenção | preenchimento manual por ocorrência | regra compartilhada para novos IDs e versões |
| Custo da recuperação | tarefa 428 já consumiu USD 0,6152408 | zero chamada externa ou consumo adicional na homologação local |

### Duas rodadas completas consecutivas

As rodadas finais `accepted-1` e `accepted-2` passaram depois do último ajuste, sem defeito entre
elas. Em cada rodada:

- backend: 90 casos, 89 aprovados e 1 ignorado;
- Dédalo/landing worker: 71 aprovados;
- Plutus: 45 aprovados;
- Psique: 113 casos, 112 aprovados e 1 ignorado;
- Têmis: 98 casos, 97 aprovados e 1 ignorado;
- conciliador de processos: 6 aprovados;
- frontend: 53 aprovados, typecheck e build;
- Opala em MySQL 5.7: 31 aprovados, incluindo migração, reaplicação e rollback;
- PDE: 30 cenários em desktop, iPhone 15 Pro e Pixel 7;
- tela administrativa: processo pai → preparação → oito atividades concluídas na simulação →
  retorno ao pai, sem request de escrita;
- isolamento Docker: v7 permaneceu v7, v8 serviu a candidata v12 e a recriação de outro produto
  não trocou nenhum dos dois;
- Spotless, shell, validação estática Liquibase e `git diff --check` aprovados.

Total das duas rodadas: 1.068 casos aprovados e 6 ignorados previstos. Logs ficam em
`artifacts/opala-commercial/accepted-1` e `artifacts/opala-commercial/accepted-2`; as capturas
ficam em `artifacts/opala-commercial/browser/{desktop,iphone,pixel}.png`.

### Aperfeiçoamento dos agentes e atribuição

- **Dédalo:** recebeu um *context sufficiency gate* determinístico: destino, origem do destino e
  contrato vêm da candidata exata antes da chamada. Falta ou conflito bloqueia sem novo consumo.
- **Backend/orquestrador:** estado terminal deixa de monopolizar a fila e o backend continua sendo
  o único responsável pelo avanço.
- **Plutus, Psique e Têmis:** seus contratos, três cenários econômicos, revisões e gates passaram
  integralmente. Não foi encontrada causa nesses prompts; eles foram preservados para evitar nova
  chamada paga ou mudança sem evidência.

A decisão segue a orientação oficial de prompts de explicitar resultado, critérios de sucesso e
contratos estruturados ([OpenAI, GPT-5.5 prompting guide](https://developers.openai.com/api/docs/guides/latest-model?model=gpt-5.5)),
e usa das pesquisas apenas o padrão de separar fatos explícitos, inferência determinística e
ausência de contexto. As fontes consultadas foram
`pesquisas/agentes-inteligentes/2026-09-13-agentes-inteligentes.md`, seção **Context Sufficiency
Gate** (fonte original: [RCL](https://arxiv.org/abs/2609.11023)), e
`pesquisas/agentes-inteligentes/2026-09-12-agentes-inteligentes.md`, seções **Grounding Agent
Memory** (fonte original: [arXiv 2609.11060](https://arxiv.org/abs/2609.11060)) e **Interaction
Contracts** (fonte original: [arXiv 2609.11381](https://arxiv.org/abs/2609.11381)).

### Estado produtivo conferido após a homologação

Consulta somente leitura pelo MCP em 16/09/2026 confirmou:

- backend publicado no commit `ad5a0960a1a448d94ed4448536ed590dd5a89d5b`;
- execução raiz 4 ainda em `ERROR`, preservada;
- execução 8 ainda `QUEUED`, 0 de 8, custo histórico USD 0,6152408 com cobertura completa;
- experimento 92 ainda `PLANNED`, checkout Pepper preservado e destino ainda ausente;
- slot 8 `v8` ainda `CANDIDATE`, ligado ao experimento 92 e à versão v12 correta;
- `https://v8.clubemusa.com.br/version-diagnostics.json` ainda responde com imagem/contrato v7.

Portanto, a causa foi corrigida e o processo completo foi comprovado **localmente com dependências
simuladas**, mas a execução produtiva não foi reiniciada. Isso é intencional: repetir a tarefa 428
antes de backend corrigido e v8 seletivamente publicada apenas consumiria novo custo com o mesmo
impedimento. Também não se declara margem aprovada, venda ou receita; Plutus deve calcular os três
cenários com os dados atuais quando a execução produtiva alcançar `economics`.

Não houve commit, PR, deploy, publicação de v8, cobrança, campanha ou escrita em produção.
