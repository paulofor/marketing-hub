# Continuidade do sucessor do Vega — 09/09/2026

## Escopo e evidência inicial

O ciclo produtivo #1, produto 4 e experimento #91, está `ADJUSTED`, revisão 2,
com aprovação de Atena registrada. A consulta via MCP confirmou o retorno ao processo
67, atividade `productArchitecture`. Na cadeia vigente v14, essa atividade é **2.3**.
O botão “Abrir atividade orientada” retornava `/experiments/91` e a orientação ainda
pedia aprovar a decisão concluída. O cadastro oferecido para o sucessor apontava para
`/experiments/manual/new`, que cria outro nicho e outra hipótese sem vincular o produto.
Não houve envio desses comandos inadequados.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Reabrir a execução histórica | Menos passos | Mistura iterações e viola o encerramento aprovado | Rejeitada |
| Criar novo nicho e hipótese pelo wizard manual | Cadastro disponível | Perde identidade do Vega e repete descoberta | Rejeitada |
| Criar experimento planejado do produto, vincular sucessor e carregar memória | Conserva aprendizado e separa métricas; usa contratos existentes | Ajustar navegação e validar vínculo ponta a ponta | Escolhida |

## Matriz local definida antes dos testes

| Dimensão | Critério |
| --- | --- |
| Caminho feliz | Ajuste encerrado oferece criação do sucessor; experimento pertence ao mesmo produto/nicho; novo ciclo herda evidências e destino; conclusão de aprendizado avança a planejamento |
| Navegação | Decisão encerrada não pede nova aprovação nem abre o experimento anterior como trabalho; sucessor existente abre sua ocorrência; ajuste aponta à atividade registrada |
| Validações e falhas | Recusar produto alheio, referência histórica como sucessor, duplicação, revisão vencida e comando no ciclo encerrado |
| Integração | Backend real, persistência MySQL 5.7 e frontend local; fontes externas e agentes simulados |
| Observabilidade | Conservar eventos, autoria declarada, predecessor, hipótese, destino BPM e motivos de bloqueio |
| Métricas e segregação | Dados sintéticos somente no banco local; não herdar vendas, métricas, aprovação ou autorização financeira |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; sem erro de JavaScript ou navegação externa na matriz local |
| Regressão | Testes do domínio de ciclos, contratos de execução, testes frontend afetados, tipagem, build, formatação e diff |

Como foram confirmados defeitos, a validação exige duas rodadas locais completas e
consecutivas após a última correção. Publicação não é mecanismo de teste.

## Resultados

**Duas rodadas locais completas e consecutivas aprovadas**, após a última correção:

- 149 testes backend, 25 de Atena e 33 frontend por rodada, sem falhas ou testes ignorados.
- 18 controles REST/MySQL por rodada, incluindo isolamento, idempotência, encerramento,
  vínculo do sucessor, memória, retorno e gates; zero chamadas produtivas na matriz.
- Cadastro do produto e ciclo histórico → sucessor → planejamento em Chromium desktop,
  iPhone 15 Pro e Pixel 7 emulados. O cadastro genérico usa um test double HTTP;
  a criação e a progressão dos ciclos usam o backend real e MySQL 5.7.
- Tipagem, build frontend, Spotless, Prettier e revisão do diff aprovados em ambas as rodadas.
- Seis controles adicionais da proposta de Atena também passaram, com worker pausado durante
  os testes de reserva concorrente. Em seguida o worker real consumiu a fixture com modelo simulado.

Na preparação da matriz, dois problemas da execução dos testes foram resolvidos localmente:
o cadastro emite um alerta de sucesso esperado, e o worker simulado não pode consumir a mesma
fila enquanto o teste verifica reservas concorrentes. Não foram tratados como falhas produtivas.

## Execução operacional pela interface

1. Criado **#92 — MUSA-H003-E003**, produto Vega (4), nicho 31 e hipótese MUSA-H003 existentes,
   tipo PDE, preço R$ 67 e status `PLANNED`. Sem orçamento diário, campanha ou gasto iniciados.
2. Criado **ciclo #2**, cadeia v14 e subprocesso v4, predecessor #1. A memória do #91 e o retorno
   ao processo 67/atividade `productArchitecture` foram preservados. Na cadeia atual é a atividade
   **2.3 — Projetar protótipo e harness PDE**, sob responsabilidade de Dédalo.
3. Concluída pela tela a atividade **Aprendizado e hipótese**, com fonte conciliada, limitação da
   amostra e explicação concorrente. O backend registrou `LEARNING → PLANNING`, revisão 1.
   A próxima atividade é **Planejar o experimento**, com Atena e Plutus, antes do ajuste.

Após a retomada, a navegação produtiva em desktop, iPhone e Pixel confirmou a entrada 6.4 →
ciclo #2 → planejamento → **2.1 — Selecionar para protótipo privado / Atena**, com botão de
execução disponível e sem erro de JavaScript. Nenhuma tarefa dessa próxima atividade foi
declarada concluída. A topologia local temporária foi removida com volumes e órfãos.

A versão `musa-pde-entry-v8-primeiro-ajuste-aplicavel` é o alvo a implementar. O teto de R$ 100
e a janela cadastrados no ciclo são proposta de planejamento; não constituem homologação,
autorização de mídia nem comprovação de melhoria. O #91 permanece `USER_STOPPED`.

Durante a criação do ciclo houve atualização externa do backend: o registro foi persistido,
mas a conexão fechou antes da resposta. O MCP confirmou shutdown e troca de build de
`da9239dfe8dd` para `d17be8bd5e4a`. A execução retomou o ciclo persistido sem duplicar
experimento ou ciclo e concluiu a atividade. Essa atualização não foi realizada por esta tarefa.

[Evidência estruturada](evidencias/vega-sucessor-continuidade-2026-09-09.json).
As correções de código permanecem locais; nenhuma publicação direta ou PR foi realizado nesta etapa.
