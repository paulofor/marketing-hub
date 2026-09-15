# Autorização com dois valores

## Decisão

Alternativas: esconder campos e enviar metadados pelo navegador (menor esforço, auditoria
frágil); criar outro fluxo financeiro (maior custo e risco de divergência); adaptar o comando
canônico com uma entrada administrativa de dois valores (escolhida: mantém gates, lock,
histórico e idempotência). Não altera campanha nem a autorização final de ativação.

## Matriz definida antes dos testes

- Sugestões por teto e janela restante; edição de diário/total e confirmação explícita.
- Valores nulos, negativos, centavos inválidos, diário maior que total, janela expirada.
- Persistência exata no experimento, metadados automáticos e mesma revisão/idempotência.
- Bloqueios comerciais preservados; outros comandos permanecem disponíveis fora do formulário.
- Casos sintéticos de outros ciclos/produtos, canal direto e legado sem diário informado.
- Desktop, iPhone e Pixel: dois campos, envio e ausência de transbordamento.
- Sem mídia/IA real, dados produtivos ou receita atribuída aos testes.

## Histórico consultado

Imagem anexada e LOOP-CICLO-AUTORIZACAO-SEM-SUPERFICIE-COMERCIAL:
a divergência de teto já tem correção versionada no materializador. A nova entrada deve
reutilizá-lo, aceitando o diário escolhido sem retornar ao erro de comparação prévia.

## Contrato implementado

`POST /api/business-process-chains/learning-cycles/v1/products/{productId}/{cycleId}/budget-authorization`
recebe `requestKey`, `expectedRevision`, `dailyBudgetBrl` e `budgetLimitBrl`.
Os dois primeiros são controles automáticos. Os dois últimos são os únicos campos humanos.
O backend deriva a versão do ciclo e a referência do aceite, usa a identidade autenticada
quando disponível e identifica apenas a origem administrativa quando não existe identidade.
A mesma transação de comando confere homologação/prontidão, aplica limites e registra evento.
A API de consulta sugere `authorizationReview.dailyBudgetBrl` pelos dias restantes em Brasília.
Janela vencida não é prorrogada silenciosamente. O teto total continua independente do diário.
O comando legado permanece compatível. Não houve migração de banco nem mudança de agentes/IA.

## Escopo da evidência

Testes Java verificam entrada HTTP, validações, tradução para comando canônico, aplicação
no experimento e bloqueio de homologação revogada, além das regressões existentes do ciclo.
A navegação usa o componente real e o cliente HTTP real com resposta simulada e identificadores
sintéticos (produto 400, ciclo 200, experimento 920); não acessa Meta nem envia autorização real.
A suíte de navegador própria é `frontend/e2e/budget-authorization-responsive.mjs`.
As duas suítes antigas de jornada também foram adaptadas ao novo contrato; a matriz desta
alteração não executa suas topologias MySQL completas. Não há alegação de validação produtiva,
receita medida ou teste novo de concorrência física no MySQL.

## Resultado local — 15/09/2026

Duas rodadas consecutivas concluídas com sucesso após os ajustes:

| Verificação por rodada | Resultado |
| --- | --- |
| Java: `LearningCycle*Test,SalesFlowResolverTest` | 248 testes, zero falhas/erros |
| Frontend: páginas e API de learningCycle | 31 testes aprovados |
| TypeScript | Sem erros |
| Chromium desktop, iPhone 15 Pro e Pixel 7 emulados | Três perfis aprovados |

Cada perfil verificou sugestões, edição, diário acima do total, falha simulada de envio,
reenvio com a mesma chave e montantes, confirmação e botão bloqueado por janela encerrada.
Screenshots inspecionadas e logs locais em `artifacts/budget-authorization/`.
Spotless aplicado aos Java alterados, scripts verificados e `git diff --check` sem erros.

Revisão da causa: apenas esconder os campos manteria o diário calculado contra a escolha do
operador. O materializador agora recebe explicitamente o diário aprovado; o fluxo legado
continua usando rateio. Não existem exceções por ID nas fontes produtivas alteradas.
Nenhum commit, PR, deploy, alteração de dados produtivos ou autorização de gasto foi realizado.
