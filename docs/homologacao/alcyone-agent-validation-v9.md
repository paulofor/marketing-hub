# Alcyone — encerramento auditável da recuperação condicional v9

Data: 01/10/2026

## Limite da evolução

Esta evolução corrige a projeção final do Processo 3 sem alterar as provas aprovadas no gate. Ela
não usa participantes humanos, não afirma preferência ou demanda, não habilita checkout, não cria
campanha e não autoriza gasto. A tentativa cancelada continua auditável e não é convertida em
sucesso.

## Evidência produtiva que revelou a lacuna

- O backend no SHA `1b80c41a0707b4049b70c1fde5d8682d78725a72` publicou a política que impede
  a tarefa cancelada #582 de substituir a correção válida #580.
- Pela tela oficial, Alcyone passou de `STOP` para `PLAY` e a execução #34 aceitou a retomada com
  HTTP 200.
- A projeção confirmou `technicalHomologation`, os três cenários de Psique, Têmis e
  `agentValidationGate` como `COMPLETED`; os cinco requisitos detalhados do gate estavam
  satisfeitos.
- Mesmo assim, o run voltou a `BLOCKED`, com dez atividades concluídas e uma restante: a ocorrência
  condicional #466/tarefa #582, cancelada depois que a falha foi atribuída ao executor.

## Causa-raiz

O gate e a prontidão de retrabalho já compartilhavam a seleção correta da correção aplicável. A
projeção global do processo, porém, mantinha toda atividade condicional que possuísse histórico como
atividade selecionada. Como `CANCELLED` não é uma dispensa explícita, a tentativa inerte continuava
contada como objetivo pendente mesmo depois que todos os destinos de `remediatesActivities` haviam
sido comprovados.

## Alternativas avaliadas

1. Contar qualquer cancelamento como conclusão: encerraria Alcyone, mas esconderia cancelamentos de
   atividades obrigatórias.
2. Remover a atividade condicional do histórico: corrigiria a contagem, porém perderia a auditoria
   de #582 e de sua instância BPM.
3. Projetar como `RECORDED` somente a tentativa `CANCELLED`, não acionável, declarada como
   `ON_FUNCTIONAL_REJECTION` e com todos os destinos resolvidos: preserva a evidência, não fabrica
   objetivo e remove apenas a falsa pendência.

A terceira alternativa foi adotada.

## Contrato preventivo

- A definição precisa declarar `activationMode=ON_FUNCTIONAL_REJECTION` e ao menos um destino em
  `remediatesActivities`.
- A ocorrência precisa estar `CANCELLED` e sem comando disponível.
- Todos os destinos precisam estar concluídos, históricos ou explicitamente não aplicáveis.
- A projeção derivada passa a `RECORDED`, com `objectiveAchieved=false`; tarefa, instância, custo e
  evidências originais permanecem intactos.
- Cancelamento de atividade obrigatória, recuperação ainda acionável ou qualquer destino pendente
  continua bloqueando o processo.
- JSON de definição inválido falha fechado e conserva o estado anterior.

## Matriz de homologação

| Área | Caminho feliz | Validação/falha | Resultado esperado |
|---|---|---|---|
| Recuperação | cancelada e todos os destinos comprovados | tentativa e instância permanecem no histórico | `RECORDED`, sem objetivo fabricado |
| Segurança | destino `BLOCKED`, `PENDING` ou `CANCELLED` | recuperação ainda pode ser necessária | continua `CANCELLED` e pendente |
| Escopo | atividade obrigatória cancelada | não possui ativação condicional | permanece bloqueante |
| Comando | recuperação ainda disponível | gatilho funcional vigente | não é reclassificada |
| Contrato | metadado JSON inválido ou destino ausente | definição não comprovável | falha fechada |
| Processo | dez objetivos concluídos e uma recuperação registrada | contagem separa conclusão de dispensa | zero restante, uma omitida, processo concluído |
| Fronteira comercial | somente agentes e testes determinísticos | humano, checkout, campanha ou mídia | nenhum efeito externo autorizado |

## Critério de conclusão

A evolução termina somente com testes locais direcionados e regressão completa, PR revisado e
integrado, deploy saudável, retomada pela interface, execução #34 em `COMPLETED`, Alcyone novamente
em `STOP` e comprovação de ausência de humanos, checkout, campanha e gasto.

## Validação local concluída

- Testes direcionados da projeção, execução BPM, recuperações e gate: 127 aprovados.
- Regressão integral do backend: 3.754 testes, zero falhas e zero erros; 23 cenários explicitamente
  ignorados pela configuração das próprias suítes.
- Spotless e as regras ArchUnit de arquitetura e automação de processos foram aprovados.
- O diff não contém erro de whitespace e nenhuma alteração de schema ou Liquibase foi necessária.
- O construtor de evidência aprovou 15 testes e materializou localmente o pacote real com 309
  arquivos e 60 manifestos.
- As suítes dos consumidores do pacote aprovaram 150 testes de Psique e 108 de Têmis; dois e um
  cenários, respectivamente, permaneceram ignorados pelas condições explícitas das próprias suítes.

## Sucessão da evidência comercial

As recuperações automáticas da Psique no run `36798401553` e de Têmis no run `36798407520`
reexecutaram os 14 testes anteriores do construtor com sucesso, mas bloquearam antes do build
porque a atestação v7 fixava o hash do diário global `docs/registros/loops.md`. O registro preventivo
desta própria correção alterou legitimamente esse diário. Portanto, repetir os workflows não
resolveria a divergência.

Foram avaliadas três alternativas:

1. apagar o novo registro do diário, o que recuperaria o hash à custa de perder a prevenção;
2. substituir o hash dentro da v7, o que reescreveria uma atestação histórica;
3. preservar a v7 e criar a v8 com esta evidência específica e versionada de Alcyone.

A terceira alternativa preserva a cadeia de auditoria. O construtor também passa a rejeitar
`docs/registros/loops.md` em qualquer coleção da atestação vigente, sem invalidar referências de
manifestos históricos. Novas evoluções devem usar relatórios próprios e versionados do produto.
