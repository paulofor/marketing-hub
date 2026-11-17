# Vega — prova complementar e continuidade da cadeia

## Caso e causa confirmada

Produto 4, cadeia 27, ciclo 22, experimento 115, processo 117/execução 102.
Têmis #921 bloqueou a integridade por falta de cobertura determinística, declarando
explicitamente a preservação da mesma implementação e dos pareceres #918–920.
O banco preserva #917 APPROVED. O motor encaminha à correção funcional e a regra
`PdeFunctionalCorrectionCycleReadinessProvider` exige sucessor apenas pela categoria
FUNCTIONAL_ADJUSTMENT. A orientação ainda acrescenta nova versão incondicionalmente.
Isso contradiz o parecer e o mecanismo existente de suplemento de prova versionado.

## Alternativas consideradas

1. Criar uma ocorrência técnica complementar independente e vincular o resultado: boa
   separação de execução, mas duplicaria o catálogo de evidências já implementado.
2. Acrescentar uma operação administrativa de anexação de suplemento: flexível, mas exige
   nova superfície e governança de integridade desnecessárias para esta correção.
3. Reutilizar o catálogo de suplementos e vincular a retentativa ao parecer e hash exatos:
   menor alteração, preserva auditoria, revisões independentes e transições existentes.

Adotada a terceira. Mudança funcional continua exigindo sucessor; prova adicional não
se torna aprovação automática e não autoriza novo gasto, publicação comercial ou vídeo.

## Matriz de aceite definida antes da execução

| Área | Verificação |
| --- | --- |
| Diagnóstico | Prova complementar da mesma implementação não orienta sucessor nem nova versão; mudança funcional continua protegida |
| Identidade | Rejeitar suplemento antigo, adulterado, de outro produto/ciclo/versão ou sem vínculo ao parecer; repetir com outra identidade |
| Prontidão | Sem suplemento não retentar; prova válida libera somente a revisão afetada; novo bloqueio posterior não repete tarefa |
| MUSA/MySQL 5.7 | Preferência revisada muda a orientação; chave conflitante e revisão antiga recusadas; concorrência consome uma operação; teto e isolamento comprovados em dois contextos |
| Estados/navegador | Encerramento em INPUT, resultado, bloqueio e falha recuperável: persistência, cartão, mensagem literal e recarga em desktop/iPhone/Pixel Chromium; emulação não comprova Safari físico |
| Gate/transições | Reutilizar matriz e pareceres compatíveis, exigir nova aprovação de Têmis, concluir Processo 3 e disponibilizar Processo 4 no mesmo contexto; preservar estados financeiros e comerciais |
| Recuperação | Retomar execução existente sem duplicar tarefa, inferência, custo ou saída; impedir reexecução com entradas iguais |
| Observabilidade | Suplemento com origem, hash, resultados e fronteira local explícitos; pendência, dono e ação vêm do backend |
| Histórico | Não reescrever tarefas, pareceres, custos ou autorizações; testes sintéticos não entram em métricas comerciais |
| Entrega | Testes locais, diff revisado, PR/revisão/main, workflows e saúde publicados antes da retomada visual |

## Harness e aprendizado

Lacuna observada: o relatório da jornada contínua não fornece ao catálogo a cobertura
exigida por sua arquitetura, e a orientação genérica transforma ausência de prova em
mudança de produto. A melhoria reutiliza o suplemento existente e regressões por identidade.
Medir recorrência do bloqueio e tarefas repetidas; não há prova de aumento de vendas.

## Provas locais executadas

Os 14 testes MySQL 5.7 produziram 14 critérios de controle em duas identidades
independentes (28 observações). A comparação de preferência mantém ocasião e peças
iguais e comprova mudança da ação, preservando a escolha anterior. Os 12 casos
terminais cobrem os quatro estados aplicáveis nos três perfis Chromium. A falha de
transporte não é convertida em estado FAILED fictício do contrato síncrono.
Persistência após encerramento, escolhas confirmadas, recusa de acesso, mensagem literal
e recarga foram conferidas. O suplemento reúne 26 critérios, sem chamadas de provedor.

A primeira rodada foi interrompida por OOM (exit 137). A configuração local posterior
usou um único JVM limitado e os controles passaram. Duas expectativas do teste foram
corrigidas: preferência altera a ação no contexto previsto pela regra (videochamada),
e acesso revogado responde 401. No navegador, o painel diagnóstico BLOCKED foi separado
da orientação utilizável e a captura passou a aguardar o alerta real. Repetidos somente
os casos afetados; as provas anteriores compatíveis foram preservadas.

O contrato de saída foi conferido na [documentação oficial OpenAI](https://developers.openai.com/api/docs/guides/structured-outputs):
propriedades de objetos estritos precisam constar em required. O teste recursivo do
schema impede repetir o erro de contrato já observado na tarefa histórica #823.

O teste de compatibilidade encontrou vínculos antigos de fontes alteradas. Foram criadas
atestação Mira v85 e preparação Vega v15, preservando seus predecessores e declarando
que nenhuma implementação de produto, aprovação ou autorização comercial foi renovada.

## Aceite antes da publicação

346 testes relacionados do backend aprovados, incluindo 92 de arquitetura. A rodada final
focada confirmou 235 testes após os ajustes; não houve repetição da matriz visual completa.
Têmis: 141 testes aprovados, com uma integração legada condicionada a diretório externo não
fornecido, fora do aceite desta candidata. O transporte real com modelo simulado e o gate
com as respostas originais da Vega foram executados. O empacotador conferiu 850 arquivos,
220 manifestos e seus testes de contrato. A topologia Compose exclusiva foi encerrada com
volumes e sem limpeza global.

A regressão usa #915–921 preservadas, bloqueia o gate antes da nova revisão e, somente com
um parecer independente simulado localmente, aprova o gate e o consumo por Íris. Nenhuma
aprovação produtiva foi fabricada. O motor comprovou a passagem 2→3→4 com persistência e
retomadas concorrentes, sem refazer o planejamento pago. Uma aprovação de outra atividade,
versão ou identidade não supera a rejeição funcional.

Consulta financeira: US$ 113,3324472 estimados, teto cumulativo US$ 200, dez custos antigos
sem valor. Recuperado recibo de tokens da #709 com identidade e modelo exatos; ainda não
lançado no ledger. #454/#455 não possuem recibo de uso nos arquivos preservados consultados.
Os demais custos não foram presumidos zero. Por exigência anterior de conciliar antes de
gastar, a execução 102 foi pausada pela interface, sem cancelar tarefas; novas revisões
pagas dependem da decisão financeira apresentada nesta conversa. Publicação técnica não
significa que Têmis aprovou ou que a cadeia produtiva já avançou.

As capturas e resultados sanitizados estão em `artifacts/vega-continuity-20261117`, com
manifesto de arquivos e hashes e declaração em `.codex/delivery-artifacts.json`.
