# Vega — reserva de fila por projeção de medição

## Causa e contexto confirmados

Em 03/10/2026, a tela e o MCP confirmaram Vega #4, tipo `PDE`, cadeia #14,
ciclo #2 aberto em `MEASUREMENT`, experimento #92 e execução #42 do processo
`operacao-otimizacao-experimento`, definição #123 v8 publicada. A reserva é
a raiz #4 do processo `pde-sales-delivery-learning`, definição #75 v6 retirada.
Não há ficha nem vínculo de execução do produto que autorize a versão retirada.
O backend servido é `50d9b575a75a33a945fe149f339db31f8ff0120b`.

A #4 permanece em `WAITING_ACTIVITY` desde 19/09, na atividade `optimization`,
sem tarefa, instância ou filho. `SalesFlowResolver` projeta `IN_PROGRESS` durante
a medição e a leitura conserva `stateEvidence=SALES_FLOW_EVENT`. O coordenador
interpretava esse estado como trabalho em curso antes de avaliar a autorização
da versão. A mesma projeção impedia drenar a reserva. Os processos retirados
#22 de Vega e #32/#37/#38 de Capella já encerraram corretamente: a lacuna é a
projeção sem execução real, não a ausência de conciliação nem toda retirada.

A #42 tem requisito próprio: estratégia de Atena com contrato pronto para
operação. Liberar a fila não comprova esse requisito nem autoriza chamar modelo,
reativar mídia ou herdar a janela encerrada de 17–22/09. O #92 e seus custos,
resultados, aprovações e limites são preservados. Não migrar o ciclo nem criar
ficha para autorizar retrospectivamente a versão antiga.

## Alternativas e decisão

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Pausar manualmente a raiz antiga | Liberação operacional pontual | A projeção também bloqueia a pausa; causa compartilhada permanece | Não resolve a origem |
| Fixar a versão retirada em uma ficha | Permite continuar o contrato histórico | Cria decisão de produto inexistente e autorização retroativa | Recusada |
| Distinguir projeção de tarefa/instância real | Encerramento e delegação seguem o contrato existente | Ajuste pequeno; exige proteger callbacks e trabalho registrado | Escolhida |

## Matriz definida antes dos testes

| Dimensão | Critério de aceite local |
| --- | --- |
| Caso original | Versão retirada sem ficha, projeção `SALES_FLOW_EVENT` ativa sem tarefa/instância: `CLOSED`, nunca sucesso; fila seguinte liberada |
| Outros casos | Identificadores segregados, estados `PENDING` e `IN_PROGRESS`, ciclo encerrado e pausa sem execução real |
| Trabalho real | Tarefa ou instância ativa e delegações em curso continuam reservadas até callback; nada é cancelado |
| Versão autorizada | Projeção sem tarefa não impede chamada ao subprocesso; reutiliza filho existente e retorna a prova ao pai sem duplicação |
| Gates | Processo seguinte mostra requisito próprio; nenhuma tarefa paga antes da entrada válida, nenhuma aprovação fabricada |
| Persistência | HTTP, JPA e MySQL 5.7: encerramento único, histórico/provas/custos preservados; consulta não muta |
| Recuperação | Worker pelo `pending`, callback, reinício e leitura posterior conservam estado e identidade |
| Tela | Chromium desktop, iPhone 15 Pro e Pixel 7: reserva removida, causa própria visível, histórico consultável, sem overflow |
| Métricas e isolamento | Fixtures sem dados produtivos ou credenciais reais; nenhum teste conta como venda, nenhum provedor pago |

O harness existente foi ampliado com a projeção que faltava nas fixtures, sem
novo serviço, agente, prompt pago ou exceção por ID. Comparação inclui o caso
original, retornos anteriormente funcionais e produtos independentes. Não há
medição disponível de custo/latência por homologação ou aumento de vendas.

## Validação local e limite comercial

- Antes da correção, seis dos sete casos de regressão reproduziram a espera
  indevida; a proteção da instância real já funcionava. Depois, os oito casos
  passam, incluindo preservação dos contratos ativos que não são projeções.
- A suíte completa do backend e as regressões finais registraram 3.855 casos:
  3.831 executados sem falhas/erros e 24 desabilitados ou condicionais já existentes.
  Os 120 casos finais do motor, persistência, grafo e arquitetura passaram.
- Integração local HTTP/JPA/MySQL 5.7: 19 cenários de API e 22 de ciclo de vida
  passaram. Os cinco novos cenários incluem projeção `PENDING` e `IN_PROGRESS`,
  retirada, pausa, ciclo encerrado e delegação/retorno sem duplicação.
- Build do backend e do frontend, 23 testes do painel, seis do executor, contratos
  do CI/runner, Spotless, `bash -n`, ShellCheck e revisão do diff passaram.
- O navegador Chromium em desktop, iPhone 15 Pro e Pixel 7 comprovou histórico,
  encerramento com pendências e o gate próprio da candidata, sem mutações por
  consulta, chamadas pagas, erro JavaScript ou rolagem horizontal.
- Após reiniciar o backend local com a mesma base, os casos projetados conservaram
  encerramento único, histórico e gate próprio; nova conciliação não duplicou
  evento nem tarefa. A rodada final de navegador confirmou os estados persistidos.

O parecer de Atena #16 está concluído e declara `MARKET_STRATEGY_V2`, mas seu
plano #3 é o **MUSA v7**, com experimento principal #91 e portfólio #90/#91.
A consulta ao backend e ao banco não encontrou vínculo do #92 com esse plano.
O contrato não pode ser copiado para satisfazer o gate da #42: versões e escopo
são distintos. O experimento #92 segue `INVALIDATED`, com janela encerrada.
Não foi solicitado novo parecer pago nem reativada campanha. A solução local
comprova a correção da fila; a publicação e o estado produtivo serão verificados
pelo PR, pelo build servido, pelo banco e pela tela antes de declarar a entrega.

## Isolamento confirmado pela sequência do harness

O primeiro CI do PR #5486 confirmou o motor, mas revelou interferência entre
cenários: a nova projeção usava o produto de teste #92039, já utilizado pelo
navegador da fila. A candidata com gate legítimo do cenário novo reservava o
produto e impedia a tarefa esperada pelo cenário antigo, gerando callback sem
tarefa. Os testes inicialmente executados de forma separada não cobriam essa
interação. O artefato do run `37164042281` e a reprodução local confirmaram a causa.

Foram comparadas três opções: reiniciar a base por família (isolamento forte,
mas recomposição cara e perda das provas para o navegador após reinício), alterar
o identificador do teste anterior (pequeno, mas desloca a dependência para outro
consumidor) e reservar identidades próprias para os novos cenários (pequeno,
preserva os contratos anteriores e a persistência exigida). A terceira foi escolhida:
as projeções usam #92041–#92045, fora da faixa anterior #92001–#92040.

A validação final inclui API e ciclo de vida na mesma base, reinício e as quatro
famílias de navegadores na ordem do runner, incluindo a fila anterior e a projeção.
A falha original do CI permanece no histórico; a atualização do PR depende dessa
validação local integrada, sem mudança adicional no comportamento produtivo.

Rodada integrada final aprovada: 19 cenários de API, 22 de ciclo de vida,
reinício, interface geral, versões encerradas, reserva anterior e medição
projetada, nessa ordem. As quatro famílias passaram em desktop/iPhone/Pixel,
incluindo histórico, retorno, segregação e nenhuma mutação por consulta.
Prettier, contratos do runner e revisão do diff também passaram. A falta de
espaço temporário na sandbox foi resolvida com remoção de cache de builds não
utilizado há mais de 24 horas; nenhum dado produtivo foi afetado.
