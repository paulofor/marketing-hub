# Continuidade dos tetos financeiros entre ciclos — v1

## Evidência e escolha

Em 10/11/2026, o MCP confirmou Capella: ciclo 20/experimento 113,
predecessor 19/112, e Vega: ciclo 22/115, predecessor 21/114. Os recibos de
preparação continuam nos predecessores (Capella US$ 120, reserva US$ 20;
Vega US$ 200, reserva explícita zero). A consulta agregava custos por produto,
mas procurava recibos apenas no ciclo selecionado. A versão executável também
muda nos sucessores. Painel, agentes, contratos e consulta de logs foram
preservados em `artifacts/cumulative-ceilings` (sem credenciais). O MCP de logs
respondeu normalmente, sem linhas correspondentes ao filtro financeiro.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Copiar aprovações ao criar ciclos | Mudança pequena | Duplica recibos e aparenta novo orçamento; não recupera sucessores existentes | Rejeitada |
| Novo cadastro global de limites | Centraliza | Migração e mecanismo paralelo sem necessidade comprovada | Rejeitada |
| Resolver recibos pela linhagem persistida | Recupera sucessores, conserva auditoria e um teto único | Validar identidade, ordem e histórico; esforço proporcional | Adotada |

## Matriz local de aceite

| Cenário | Aceite esperado |
| --- | --- |
| Sucessor de Capella e produto com IDs diferentes | Mesmo recibo/teto/reserva; nova versão executável não solicita aprovação monetária |
| Vários predecessores e substituição explícita | Última decisão de cada grupo prevalece; valores não se somam |
| Consumo anterior, reserva e nova tarefa | Saldo é teto menos consumo menos reserva, cada parcela uma vez; guard permanece ativo |
| Limite exato e ultrapassado | Aviso de atenção e revisão na planilha; leitura não inicia gasto |
| Custo desconhecido | Ausência explícita; não inventar zero nem afirmar cobertura |
| Produto/cadeia/linhagem divergentes e corrupção | Recusa segura; nenhuma aprovação alheia |
| Replay e revisão concorrente | Comando repetido idempotente; revisão antiga recusada |
| Consulta de ciclo encerrado | Não lê decisões de descendentes nem oferece retomada |
| HTTP → serviço → recibo → pending/guard | Contratos reais com dependências locais; nenhuma inferência paga |
| Desktop Chromium, iPhone 15 Pro e Pixel 7 emulados | Origem do teto, saldo e alerta legíveis; revisão acessível na própria planilha |
| Regressões existentes | Aprovações, fluxo de autorização e arquitetura passam; mídia/vídeo/STOP preservados |

## Melhoria reutilizável e limites

A lacuna do harness era validar apenas um ciclo isolado. A regressão executa
predecessores e sucessores nos serviços reais, conferindo o recibo nativo que
chega ao executor e o saldo consultado antes da tarefa. Critério de adoção:
nenhuma nova aprovação apenas por troca de ciclo, sem reiniciar saldo ou
perder reservas. Testes locais não demonstram aumento de vendas. Autorizações
pontuais de um vídeo, homologações e publicação mantêm seus gates.

A navegação local revelou ainda um fixture de browser desatualizado: a página atual
consulta o recibo de vídeo único e os projetos do Estúdio, ausentes na simulação.
O teste foi atualizado para esses contratos consultivos e agora registra qualquer
mutação inesperada, preservando cleanup mesmo quando falta uma rota simulada. Não
foi alterada a autorização de vídeo único. Este ajuste valida a página real completa,
sem esconder integrações novas nem usar publicação como diagnóstico.

## Validação local concluída

- 190 testes Java aprovados (98 de contratos/financeiro e 92 de arquitetura), sem falhas ou skips.
- 29 testes de frontend aprovados; TypeScript e build de produção aprovados.
- Browser Chromium: desktop, iPhone 15 Pro e Pixel 7 emulados; navegação real, recibo
  do predecessor, reserva, alerta e link de revisão conferidos. Nenhum POST inesperado.
- Consulta JPA real em H2 confere filtro por linhagem e ordenação por data/ID,
  inclusive empate e revisões locais diferentes. Nenhum changelog foi alterado.
- A fotografia histórica de uma versão invalidada permanece consultável, com recibo
  inelegível para execução. Nenhuma autorização pontual de vídeo foi transferida.
- Arquivos de evidência declarados em `.codex/delivery-artifacts.json`; nenhuma credencial
  faz parte da entrega. Dados fictícios permanecem separados do estado comercial.

O aceite técnico não constitui venda ou contribuição realizada. Custos incompletos e
outros gates permanecem identificados; esta alteração não inicia tarefas nem campanhas.

## Bloqueio externo observado na publicação

Ao conferir os checks do PR #5814, o workflow preexistente `zx`
(`.github/workflows/ze7dbha.yml`) executou no push. Seu código carregava segredos,
codificava valores e tentava gravá-los em outras branches deste repositório público.
O fluxo foi desativado no GitHub e removido desta entrega para impedir repetição.
A consulta somente de metadados confirmou arquivos nos destinos, sem baixar,
decodificar ou reproduzir seus conteúdos. Não foram apagadas branches nem reescrito
histórico. O run observado foi #42857764287; seu sucesso não é validação financeira.

Remover o workflow não revoga credenciais já expostas. A publicação permanece
condicionada à confirmação da revogação/substituição por canal seguro nos provedores
e no GitHub. Essa ação externa pode afetar serviços e não foi executada por inferência.
A orientação oficial é [revogar ou rotacionar segredos expostos](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository).
Nenhum segredo deve ser enviado na conversa, no PR, nas evidências ou nos logs.
Os CIs legítimos do PR apareceram posteriormente e são acompanhados separadamente.

## Complemento após o CI real

O CI completo executou 5.948 testes e encontrou uma única falha: a configuração
explícita `LearningCycleLocalApplication` não importava o novo auxiliar financeiro.
O mesmo motivo impedia o bootstrap do job MySQL. A aplicação normal usa descoberta
de componentes; a falha estava no harness. O import foi corrigido e a verificação
existente de dependências da fixture passou a incluir os auxiliares financeiros.

Validação adicional na sandbox, antes de atualizar o PR:

- Nove testes de fixture, continuidade e consulta JPA aprovados.
- MySQL 5.7 real com projeto Compose exclusivo: API inicializada, 18 testes de analytics,
  22 cenários REST (incluindo teto/recibo/reserva entre versões e ciclos), continuidade
  entre processos em dois produtos, 14 verificações de recuperação e segregação e
  rollback/reaplicação/idempotência de migração aprovados. Nenhuma chamada externa paga.
- O daemon da sandbox é remoto; o host de banco foi `sandbox-docker`, conforme o
  contrato já suportado. A primeira tentativa em localhost foi descartada por erro
  de conexão, sem alterar produção. Containers, rede e volumes temporários encerrados.
- `bash -n` e ShellCheck aprovados nos scripts shell pertinentes. Os blocos do workflow
  removido foram analisados estaticamente, nunca executados; seus achados foram
  eliminados com a remoção integral do fluxo inseguro.

As evidências adicionais ficam em `artifacts/cumulative-ceilings/mysql-fixed`.
A confirmação da recuperação de credenciais continua pendente e impede publicação.

A revisão final preservou a prioridade de decisões operacionais explícitas de vídeos e
mídia sobre um teto de planejamento mais antigo. Duas regressões verificam valores
diferentes e impedem atribuir ao recibo herdado a decisão operacional atual. A rodada
final relevante teve **195 testes Java aprovados**, sem falhas ou skips; frontend e
persistência já validados não foram repetidos sem alteração relacionada.

A seleção também é definitiva por substituição: a invalidação contextual da última
decisão bloqueia a cobertura, sem ressuscitar teto antigo maior. A regressão usa
redução explícita de US$ 200 para US$ 100 e comprova histórico preservado e nenhum
recibo vigente após invalidar o contexto da última decisão. Rodada relevante final:
**196 testes Java aprovados**, sem falhas ou skips. A mudança não altera consultas,
persistência ou os cenários MySQL já conferidos; valida somente a seleção e o aviso.
