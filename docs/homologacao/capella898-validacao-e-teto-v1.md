# Capella 898 — validação e teto antes da primeira tarefa

## Evidência e alternativas

Em 04/11/2026, produto 7, cadeia 27, ciclo 20, experimento 113, execução 97:
Atena 898 produziu APPROVE, mas a validação recusou “não impor comparação documental
de entradas, REFERENCE/REDUCED ou matrizes adicionais”. A saída preservada não exige
a comparação. A tarefa custou US$ 2,155494 estimados. O ciclo está sem recibo próprio;
o guard só verificava tarefas repetidas e deixou passar a primeira execução.
Banco, painel e logs do executor confirmaram ambas as causas. Nenhuma autorização
retroativa decorre desta correção; o custo e os fatos anteriores são preservados.

Alternativas: repetir Atena acrescentaria custo sem corrigir o validador; desativar
validação ou herdar tetos removeria proteções; reconhecer proibições explícitas,
revalidar a mesma resposta e conferir o teto antes de toda nova tarefa elimina as
causas com os mecanismos existentes. A terceira alternativa foi escolhida.

## Matriz local de aceite, definida antes dos testes

| Caso | Aceite |
| --- | --- |
| Resposta 898 e transporte originais | Validador completo aceita os mesmos bytes, sem inferência |
| Proibição explícita e outra identidade | Worker e backend concordam; nove combinações preservadas |
| Obrigação em outra frase/campo, contraste ou dupla negação | Comparação indevida continua bloqueada |
| Comparação implementada e contrato histórico válido | Fluxos válidos preservados |
| Primeira tarefa sem recibo, retomar repetido e polling | PAUSED persistido; zero tarefas e zero consumo |
| Recibo próprio suficiente, sucessor e outra identidade | Uma passagem; nenhum teto herdado |
| Resultado já produzido e pausa financeira | Callback recuperável; próxima tarefa continua protegida |
| Histórico sem consumo e custos desconhecidos | Ausência de tarefas não vira dívida; custo ausente não vira zero |
| Interface desktop, iPhone 15 Pro e Pixel 7 | Motivo financeiro e link para mesma planilha legíveis |

Testes usam modelos/dependências simulados e identificadores de QA. Não há tráfego,
compra, mídia ou receita de teste na produção. A validação comprova comportamento,
não ganho de vendas. A métrica de adoção é ausência de falsa rejeição e de tarefa
sem recibo; regressão em obrigações reais ou identidade exige revisão da mudança.

## Evidências adicionais

A resposta completa preservada de 898 foi aceita pelo validador integral do executor,
com o transporte extraído da auditoria (campos ocultados mantidos como desconhecidos),
sem inferência, callback ou alteração de estado externo durante o teste. SHA-256 da
resposta: `a1eedb8418b38146621532d1c8e0e8218dbf96f45a2cb0aae2d2b47cde278a78`.
O replay de callback existente foi testado com custo/tokens imutáveis e idempotência.
O handoff do serviço da tela cobre Capella v4, outra identidade e versões válidas.

O ledger consultado via MCP registra somente o evento preparatório 125 no ciclo 20,
sem `AUTHORIZE_PREPARATION_BUDGET`. A execução 97 iniciou 898 e só então registrou
`CORRECTION_BUDGET_REQUIRED`. A correção protege novos disparos; não reclassifica
esse gasto como autorizado nem fabrica um recibo.

O teste responsivo existente aprovou card, planilha, gráficos e recibos em desktop,
iPhone 15 Pro e Pixel 7. A atestação Mira v73 preserva a v72 e revalida apenas a
compatibilidade do código compartilhado; a seleção de publicação PDE confirmou
`has-deployment=false`, sem nova versão comercial ou autorização de publicação.

Na suíte ampliada, a sandbox encerrou a JVM com código 137 e registrou `oom_kill=1`.
A continuação local limita Maven a 512 MiB e o cache de contextos de teste a quatro;
executa somente suítes restantes e os testes ajustados. A fixture privada independente
passou a injetar o guard real, como a aplicação, sem criar autorização fictícia.

## Resultado da homologação local

Concluída em 04/11/2026: relatórios consolidados do backend com 5.802 casos,
zero falhas/erros e 66 skips condicionais de integração; continuação terminou
com código zero. Executor Atena: 111 casos, zero falhas/erros/skips. O replay
local integral de 898 também passou sem inferência. Construtor de evidências:
17 casos aprovados. Interface: desktop e dois dispositivos móveis aprovados.
Nenhum changelog foi alterado; as integrações MySQL seguem no CI do PR.
