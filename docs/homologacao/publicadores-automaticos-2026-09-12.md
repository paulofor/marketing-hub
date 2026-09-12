# Retomada automática dos publicadores — 12/09/2026

## Evidência e decisão

O GitHub informou todos os workflows ativos. O merge #5176, revisão `06c8b1b5`,
gerou automaticamente o run `34713482384` do publicador central. O registro remoto
`f3221756d77344dd94a10c02816956fe` está `RELEASED`, desde 18:47 UTC.
Logo, o gatilho normal de push funciona. A recorrência vem do encerramento manual
da pausa e da ausência de recuperação dos eventos perdidos, confirmados no código de
`Coordinator.resume` e no histórico do botão Prompt para AIHUB.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Reativar manualmente a cada incidente | Ajuste operacional pequeno | Repete a causa e depende de nova solicitação | Rejeitada |
| Liberar por timeout ou fim da sessão | Automação simples | Pode sobrescrever homologação ou correção não integrada | Rejeitada |
| Registrar homologação concluída e reconciliar após merge | Recuperação auditável sem nova solicitação | Exige estado persistido, exclusão e contratos dos publicadores | Adotada |

## Matriz definida antes dos testes

GitHub e SSH de mutação serão simulados localmente, com o armazenamento e locks reais
em diretórios temporários. Nenhum deploy, commit, PR, mensagem, IA paga ou dado comercial
participa da validação. Navegadores/dispositivos não se aplicam a este controle de CI.

| Área | Critérios |
| --- | --- |
| Caminho feliz | Pausa, homologação concluída, integração, retomada e publicação recuperada |
| Bloqueios | Commit não integrado, evidência ausente, comando interrompido e pausa sem preparação preservados |
| Concorrência | Lock entre operadores, retomada parcial, desconexão e avanço da main tratados |
| Idempotência | Execução existente reutilizada; dispatch aceito/incerto não duplicado; falhas não geram loop de builds |
| Dependências | Aplicação antes dos agentes, mesma revisão e sucesso comprovado; proxy emergencial excluído |
| Escopo | Somente publicadores previamente ativos; estado anterior e proteção de novas intervenções preservados |
| Integração | CLI e armazenamento reais com GitHub simulado; contratos de todos os workflows e Actionlint |
| Observabilidade | Motivo de espera, revisão, run e falhas persistidos; resumo do Actions sem credenciais |
| Métricas e isolamento | Evidências locais por rodada; nenhuma alteração de métricas ou dados de produto |

Executar a matriz completa localmente. Após qualquer defeito corrigido, exigir duas rodadas
completas consecutivas sem falhas depois da última correção.

## Resultados

As primeiras validações identificaram dois ajustes necessários: retornar automaticamente da
drenagem após retomada parcial e reconhecer pelo ID o dispatch cujo evento capturou uma
nova revisão da main. O segundo caso foi reproduzido antes da correção em
`artifacts/publisher-recovery/main-drift-before-fix.log`. A proteção no workflow rejeita a revisão
divergente antes de publicar; o reconciliador reconhece esse recibo e recupera a revisão atual.

As duas rodadas finais são registradas em `artifacts/publisher-recovery/release1` e `release2`.
Nenhum publicador remoto foi alterado nesta implementação.

**Resultado: duas rodadas completas consecutivas aprovadas**, após a última correção,
com hashes idênticos das fontes entre as rodadas. Em cada uma:

- 33 testes do coordenador existente, 35 testes da recuperação automática e 35 testes Node
  de coordenação dos agentes/transporte de imagens: **103 testes aprovados**.
- Contratos SSH dos nove publicadores e do controlador administrativo preservados.
- Deploy transacional, recuperação de eventos perdidos, fila compartilhada e resiliência
  aprovados, incluindo os cenários de falha esperada dos serviços locais.
- Actionlint aprovou todos os workflows e a revisão do diff não encontrou erros.

Os relatórios `matrix.json` registram os 11 grupos, resultados, duração e hashes das fontes.
A topologia temporária foi removida com o projeto Compose exclusivo da sandbox; nenhum
container desse projeto permaneceu. A última consulta externa encontrou **43 workflows
ativos e nenhum desativado**, além do deploy automático `34713482384` bem-sucedido.

## Comportamento implementado

- `prepare-resume` registra commit validado, evidência e fim da homologação. A intervenção
  passa a `AWAITING_MERGE`; comandos de runtime ficam encerrados nesse registro.
- `reconcile-publishers.yml` permanece ativo durante a pausa. É acionado por merge/push,
  conclusão dos publicadores, agendamento de 15 minutos e comando manual de recuperação.
- O controle comprova integração, usa o mesmo lock e o comando `resume`, restaura apenas
  workflows anteriormente ativos e recupera publicações da main pelo catálogo versionado.
- A aplicação precede Argos, Psique e Íris. Continuação sem job de publicação aprovado não
  comprova entrega. Os demais publicadores mantêm seus testes, filas e rollback existentes.
- Cada workflow confere `recovery_sha` antes de testes, imagens e publicação. Avanço concorrente
  da main não publica uma revisão diferente da solicitada. O recibo da API permite reconciliar
  esse caso sem interpretar a rejeição esperada como defeito funcional da nova revisão.
- Falha real de build interrompe a recuperação; não há repetição automática do mesmo erro.
  Pedido sem recibo permanece auditável e não é duplicado por timeout. Nova intervenção
  preserva o histórico anterior e interrompe decisões da recuperação anterior.
- Estados anteriores desativados e comandos emergenciais de proxy permanecem fora do dispatch
  automático. O PDE usa seu contrato de produção com superfícies versionadas e proxy isolado.

O CLI e os dois transportes foram exercitados em processos reais contra GitHub/SSH simulados,
incluindo pausa, preparação, retomada, um único dispatch e confirmação final. API GitHub real
foi usada somente para leitura: versão `2026-03-10`, filtro por revisão/data e paginação
confirmaram o run `34713482384` concluído com sucesso para `06c8b1b5`.

SSH de controle reutiliza a credencial existente do publicador central e a chave pública
Ed25519 fixada do host administrativo, conferida pelo helper autorizado. O reconciliador
executa somente o armazenamento de coordenação no host; imagens continuam no fluxo versionado
de PR/Actions. Não instala daemon, cron ou scripts de publicação pelo SSH.

## Ativação

A mudança permanece na sandbox e será ativada ao integrar seu PR. Intervenções históricas
sem `prepare-resume` não são liberadas retroativamente. A intervenção observada nesta sessão
já estava `RELEASED` e todos os workflows ativos; não foi necessário reativá-los novamente.

Fontes oficiais: [API de workflows](https://docs.github.com/en/rest/actions/workflows)
e [gatilhos com GITHUB_TOKEN](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow).
