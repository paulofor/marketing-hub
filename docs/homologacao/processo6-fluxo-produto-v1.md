# Processo 6 — fluxo por produto e experimento

## Diagnóstico confirmado em 09/09/2026

A tela do histórico do Vega aponta 6.1 por tarefas de agente, enquanto a tela de atividades
aponta 6.4 por ciclo. MCP confirmou tarefas #356/#357 bloqueadas no experimento #91, adoção
histórica no ciclo #1 e evento automático `MEASURE` avançando para `DECISION`. O backend de
posição ignora o ledger do ciclo. O diagrama v5 inicia operação e entrega simultaneamente, sem
explicitar a entrada histórica nem a condição de ausência de vendas. Não há motivo para
reativar o #91, concluir Hermes retroativamente ou atribuir entrega a uma venda inexistente.
O MCP respondeu à consulta de logs, mas não encontrou linhas para `cycleId=1` na retenção atual.
As evidências determinantes são os eventos persistidos e a divergência reproduzida no navegador.

Rastreio: o histórico usa `GET /api/products/value-chain-positions/{productId}` e o
`ProductSubprocessPositionResolver`; as atividades usam
`GET /api/business-processes/{processId}/products/{productId}/activity-executions` e
`BusinessProcessActivityExecutionService`/`LearningCycleActivityProjection`. A primeira leitura
priorizava `agent_task` e suas instâncias; a segunda já considerava `learning_sales_cycle_v1`.
A correção compartilha a leitura de ciclo e `learning_sales_cycle_event_v1` entre ambas.

[Evidências de produção, somente leitura](evidencias/processo6-vega-2026-09-09.json).

## Alternativas

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Trocar somente o rótulo 6.1 | Baixo esforço | Mantém fontes divergentes e comandos fora de ordem | Não |
| Forçar quatro atividades estritamente sequenciais | Fácil de explicar | Atrasa entrega até encerrar operação; inventa conclusões históricas | Não |
| Fluxo compartilhado por produto/experimento com gateways e eventos | Uma posição verificável, entrega durante operação, retornos auditáveis | Exige contrato, BPM versionado e regressão integrada | Sim |

Preservar as quatro atividades. Operação e entrega são ramos explícitos; a entrega acompanha
cada venda. Uma referência histórica entra na consolidação com limitações registradas.
Consolidação válida libera decisão. Cada retorno informa origem, destino, motivo, responsável,
data e evidência. Versões e tentativas anteriores são história, não posição atual.

## Matriz de homologação definida antes dos testes

| Área | Critérios |
| --- | --- |
| Vega histórico | #91 adotado e conciliado aponta 6.4 em histórico e atividades; #357 permanece bloqueada no histórico, sem aprovação retroativa |
| Caminho normal | Operação e entrega condicionada a vendas; consolidação antes de decisão; encerramento preserva evidências |
| Retornos | Correção da medição, nova coleta, ajuste/sucessor e escala com destinos explícitos e registro dos eventos; sem regressão por tarefa antiga |
| Segregação | Produto, experimento e cadeia corretos; sucessor tem contexto próprio; cadeia histórica conserva sua versão; outro produto não herda progresso |
| Validações/falhas | Ausência ou erro de fonte não vira zero nem conclusão; comando fora de fase bloqueado; leitura não cria tarefa/ciclo ou movimenta campanha |
| Persistência | Backend real e MySQL 5.7 local; incremento, rollback e reaplicação de BPM sem reescrever definições antigas |
| Observabilidade | Mesma fonte de posição; transições com evidência e motivo; métricas comerciais preservadas; doubles identificados e sem tráfego produtivo |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; histórico → atividade → subprocesso → retorno; legibilidade e contexto |
| Regressão | Testes relevantes backend/frontend, tipagem, build, formatação, contratos e revisão do diff |

Uma rodada local sem defeitos encerra a matriz. Se houver correção durante a matriz, executar
duas rodadas completas consecutivas sem falhas depois da última correção. Nenhuma publicação
será usada como teste. Emulação Chromium não comprova Safari nativo.

## Resultado

**Duas rodadas completas consecutivas aprovadas após a última correção**, com a mesma
implementação nas duas execuções. A revisão final de causa-raiz inclui a fonte comum da posição,
o contexto do experimento ao entrar em subprocessos e a proteção de tarefas antigas na fila;
não se limita a trocar o número exibido na tela.

| Verificação | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Controles do runner | 23/23 | 23/23 |
| Seleção de testes backend | 330 aprovados; 2 condicionais ignorados | 330 aprovados; 2 condicionais ignorados |
| Analytics PDE em MySQL 5.7 | 15/15 | 15/15 |
| Testes frontend | 528/528 | 528/528 |
| Cenários REST com persistência real | 18/18 | 18/18 |
| Jornadas de navegador | Desktop, iPhone 15 Pro e Pixel 7 | Desktop, iPhone 15 Pro e Pixel 7 |
| Migração, rollback, reaplicação e idempotência | Aprovados | Aprovados |
| Tipagem, build, Spotless, contrato temporal e diff | Aprovados | Aprovados |
| Remoção da topologia temporária | Concluída | Concluída |

As **92 verificações de `ArquiteturaTest`** também passaram, sem alteração da implementação
entre a verificação e a segunda rodada. Os dois testes condicionais preexistentes pertencem
a `VideoCreativeControllerTest`, dependem de `videoCreative.browser` e
`publicationRecovery.browser` e não integram o escopo desta mudança. As jornadas do ciclo,
incluindo seus contratos de vídeo, foram executadas pelos runners locais da matriz.

Foram comprovados: posição 6.4 após a adoção e conciliação histórica; entrega não aplicável sem
vendas; posição 6.2 com venda sem entrega comprovada; retorno à decisão após conciliação da
entrega; bloqueio de operação antiga e de referência legada de plano; passagem entre versões
da cadeia; sucessor sem herdar métricas/aprovações; ausência de efeitos de escrita ao navegar;
e segregação entre dois produtos de teste. O atalho do histórico abre a atividade atual, que
chama o subprocesso com o identificador do ciclo e do experimento corretos.

Os testes usam backend, controllers, JPA, eventos e MySQL 5.7 reais na sandbox. Integrações de
agentes, campanha, pagamento e fontes de métricas são simuladas; nenhuma venda, gasto ou
publicação produtiva foi executada. Os dados locais usam os produtos de teste 91001/91002.
A emulação mobile usa Chromium e não comprova comportamento nativo do Safari/WebKit.

- [Resultados, controles e assinaturas dos arquivos testados](evidencias/processo6-fluxo-produto-v1/resultado.json).
- [Captura da tela de atividades na sandbox](evidencias/processo6-fluxo-produto-v1/atividades-desktop-local.png).
- Logs completos desta sessão: `/tmp/learning-sales-cycle-round-bnvcET` e
  `/tmp/learning-sales-cycle-round-oIcc0v`.

### Reprodução local

Executar o runner com o projeto Compose exclusivo autorizado para a sandbox e o host da engine
local. Nesta sessão:

```bash
LEARNING_CYCLES_COMPOSE_PROJECT=aihub-62305253-32c8-49d3-bcb2-69ea697a34f6-b719af74d7 \
LEARNING_CYCLES_DB_HOST=sandbox-docker \
bash backend/ads-service/scripts/homologate-learning-cycles-local.sh --video-matrix
```

A migração acrescenta Processo 6 v6 e cadeia v14, preserva definições e ciclos anteriores e
permite rollback sem apagar o histórico. As alterações permanecem locais, prontas para o fluxo
de PR/deploy do repositório; nenhum PR, commit ou publicação foi realizado nesta solicitação.
