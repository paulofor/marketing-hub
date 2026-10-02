# Número do processo no título da ficha

## Evidência e aceite

Em 02/10/2026, a consulta de Mira/produto 10, subprocesso 58, cadeia 24, foi
conferida no banco pelo MCP e na tela administrativa. A quarta atividade do
Processo 5 chama a homologação técnica: o título deve ser
**Mira · Processo 5.4 — Homologação técnica de experimento**.

O frontend já formata `chainPosition.sequenceLabel`. O resolvedor backend
descartava qualquer consulta sem ciclo, embora o grafo da cadeia fosse suficiente.
A posição global só contém medições de subprocessos iniciados; por isso não
substitui a posição do subprocesso selecionado.

A consulta também retornou HTTP 500. Os logs MCP mostraram ausência do plano
comercial governante, convertida em bloqueio pelo executor, seguida de
`UnexpectedRollbackException`: a avaliação transacional já havia marcado a
consulta para rollback. A correção deve preservar o bloqueio financeiro e a
legibilidade da ficha, sem autorizar execução ou gasto.

## Alternativas

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Fixar 5.4 no frontend | Alteração pequena | Incorreta em outras cadeias e versões | Descartada |
| Criar outra consulta de numeração | Contrato específico | Duplica o resolvedor e adiciona chamadas | Descartada |
| Corrigir o resolvedor e a leitura transacional existentes | Fonte canônica única, sem consultas extras | Exige regressões de contexto e transação | Escolhida |

## Matriz local

| Cenário | Verificação |
| --- | --- |
| Homologação técnica sem ciclo | Backend entrega `5.4`; título usa o mesmo número |
| Rota tipada sem ciclo | Preparação comercial mantém `5.1` |
| Ciclo com adesão Opala histórica | Preserva `5.2` somente para a identidade adotada |
| Nós declarados fora de ordem e retorno REWORK | Ordem causal preserva `5.4` |
| Cadeia ausente ou identidade divergente | Não inventa número ou reaproveita posição de outro contexto |
| Plano financeiro ausente | Consulta transacional devolve bloqueio; não grava nem libera execução |
| Desktop, iPhone 15 Pro e Pixel 7 em Chromium | Título completo visível e sem overflow horizontal |
| Observabilidade e segregação | Fixtures locais, sem comandos produtivos, eventos comerciais ou mídia |

## Harness

Os testes anteriores usavam apenas ciclos presentes e o serviço sem proxy
transacional. As regressões devem exercitar ausência de ciclo, BPM embaralhado e
a transação Spring real, preservando o mecanismo existente sem infraestrutura nova.

## Resultados locais

- Antes da correção, três cenários de posição falharam e a consulta transacional
  reproduziu `UnexpectedRollbackException`.
- Após a correção, os 64 testes das seis classes relacionadas passaram; os 19
  testes da ficha no frontend, o typecheck e o build também passaram.
- A suíte completa do backend encerrou com 3.773 cenários, zero falhas e zero
  erros. Os 23 cenários condicionados ou desabilitados pelo projeto não foram
  executados nessa rodada; nenhum faz parte do aceite desta correção. Spotless
  validou os quatro arquivos Java alterados.
- Desktop, iPhone 15 Pro e Pixel 7 emulados no Chromium exibiram o título 5.4,
  sem erros JavaScript, overflow horizontal ou requisições de escrita.
- As seis definições reais da cadeia 24 foram conferidas por leitura MCP: seus
  grafos progressivos possuem ordem causal válida. O Swagger foi validado como YAML.
