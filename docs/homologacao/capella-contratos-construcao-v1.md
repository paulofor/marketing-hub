# Contratos de construção de Capella — 06/10/2026

## Evidência e causa

Após o PR #5515, o backend registrou a passagem da execução 46 para 47, no ciclo 5,
experimento 98. A tarefa 595 de jornada retornou `BLOCKED`; seu resultado e custo
estimado de USD 0,2930976 foram preservados. A arquitetura 594 havia sido aprovada
com a identidade completa: produto Capella, nome comercial Agenda Cheia Nail Design,
tipo de catálogo Quartzo. O contexto seguinte perdeu o tipo estruturado, preservando
somente sua menção em texto livre. O agente interpretou a menção como outro produto.

O mesmo resultado tentou ler o workspace e relatou erro de namespace. O executor
usa sandbox somente leitura e devolve JSON; não implementa o protótipo. O cânone e
`PdeTechnicalHomologationReadinessProvider` já distinguem especificação de prova real,
mas o núcleo do prompt mandava bloquear genericamente por prova ausente. A tarefa
antecipou para a jornada os requisitos de imagem, URL, manifesto e testes posteriores.

Histórico comparado: arquitetura 594 aprovada com contexto completo; jornada 595
bloqueada após perda de identidade; contratos anteriores de Mira e Vega mostram que
uma especificação concluída não prova implementação. Preservar os gates documentados
em `LOOP-DEDALO-CONTRATO-MARCADO-COMO-PROTOTIPO` é critério obrigatório desta correção.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Repetir a tarefa com a mesma entrada | Pouca alteração | Repete ambiguidade e consumo sem mudança causal | Descartada |
| Permitir edição/shell ao agente | Poderia permitir investigação local | Amplia autoridade, não fornece integração de implementação nem prova de entrega | Descartada |
| Preservar identidade e limitar a atividade ao seu contrato | Corrige a entrada compartilhada e evita tentativa de shell inútil | Exige regressões do worker e do gate; implementação real continua pendente | Adotada |

O backend transporta produto e tipo separadamente. O worker restringe shell nas três
atividades de especificação e declara o significado de `READY`. Correção de protótipo,
homologação técnica e avaliações independentes mantêm seus requisitos. Nenhuma saída
`BLOCKED` será reclassificada ou substituída por parecer fabricado.

## Matriz definida antes dos testes

| Área | Aceite |
| --- | --- |
| Capella e outro produto | Identidades e tipos vêm do catálogo sem mistura ou exceção por ID |
| Ausências | Tipo desconhecido permanece nulo; URL e aceite não são inventados |
| Worker real | Pending → auditoria → processo local simulado → callback nas três especificações |
| Ferramentas | Shell desabilitado nessas atividades, sandbox read-only preservada; outros contratos não mudam |
| Divergência | Produto estruturado divergente é recusado antes da inferência |
| Falha funcional | Resultado bloqueado preserva saída, causa e consumo; não vira sucesso |
| Homologação | Contrato completo sem implementação continua bloqueado; versão aceita permanece válida |
| Auditoria | Prompt efetivo, modelo, tokens e resultado seguem o contrato existente |
| Catálogo | Novo limite versionado aparece no harness existente com conteúdo e hash |
| Interface | Sem mudança visual; verificar retomada, tarefa e bloqueio pelo admin publicado |
| Comercial | Sem chamada de geração paga, mídia, cobrança, envio externo ou métricas humanas de QA |

Modelos locais simulados comprovam passagem de contrato e configuração, não a decisão
futura do agente real nem prontidão do produto. A confirmação operacional ocorre após
publicação pelo PR. O resultado anterior permanece auditável.

Referência da configuração CLI: [features.shell_tool](https://learn.chatgpt.com/docs/config-file/config-reference#configtoml).

## Validação local concluída

- 170 testes do backend aprovados, incluindo 92 regras de arquitetura, contexto do alvo,
  identidade de dois produtos, ausência de tipo e bloqueio da homologação sem implementação.
- 93 testes do worker aprovados. Os oito casos do percurso real foram executados também
  com os JSONs exportados pelo backend: três atividades, duas identidades, bloqueio funcional
  preservado e divergência recusada antes da inferência. Nenhum modelo pago foi chamado.
- O catálogo passou com o novo recurso, sem mecanismo paralelo. A configuração read-only
  e a ausência de alteração no contrato de arquitetura anterior foram verificadas.
- Spotless nos Java alterados, sintaxe do executável simulado, `bash -n`/ShellCheck do helper
  consultado e `git diff --check` aprovados. Uma chamada inicial usou o getter de tipo
  inexistente; a compilação local identificou e a implementação passou a usar a relação real.
- Sem alteração de interface, persistência SQL ou autorização comercial. As evidências
  operacionais posteriores ao deploy devem ser vinculadas ao PR e ao SHA efetivo.
