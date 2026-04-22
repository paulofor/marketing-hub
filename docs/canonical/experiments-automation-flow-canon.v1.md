# Experiments Automation Flow Canon v1

## 1. Propósito

Definir o contrato canônico do **fluxo automático do pipeline de experimento** (fila automática), incluindo ordem de etapas, estados, gatilhos e critérios de progressão.

Este documento existe para impedir drift entre backend, frontend e workers quando a execução automática estiver ativa.

## 2. Escopo

Este cânone cobre:

- ordem obrigatória das etapas automáticas;
- estados operacionais da fila automática;
- eventos mínimos de transição;
- critérios de conclusão, bloqueio e retomada;
- regras de UX para manter o usuário informado e evitar ações que prejudiquem o fluxo.

Este cânone não substitui:

- schema dos artefatos (mantido em `modelo-canonico-artefatos-pipeline-experimento.md`);
- governança global (mantida em `system-governance-canon.v2.md`).

## 3. Ordem canônica das etapas automáticas

Quando a fila automática estiver ativa, o pipeline deve respeitar a sequência abaixo:

1. `campaignAngle` (Ângulo da Campanha)
2. `adCopy` (Texto do Anúncio)
3. `imagePrompt` (Prompt da Imagem)
4. `landingPageCopy` (Texto da Landing)
5. `landingPageWireframe` (Layout da Landing)
6. `landingPageImagePlanning` (Planejamento de Imagens da Landing)
7. `landingPageHtml` (HTML da Landing)

Regra canônica:

- uma etapa só pode iniciar quando a etapa anterior estiver em `COMPLETED`.
- avanço fora de ordem é drift e deve ser bloqueado no backend.

## 4. Estados da fila automática

Estados permitidos para execução da fila:

- `IDLE`: fila automática desligada, sem execução em curso;
- `RUNNING`: fila automática ativa com etapa em processamento;
- `WAITING_DEPENDENCY`: aguardando pré-condição externa (worker, artefato obrigatório ou sincronização);
- `BLOCKED`: execução interrompida por erro de regra, contrato ou validação;
- `COMPLETED`: todas as etapas automáticas concluídas para o experimento.

Regra canônica:

- somente o backend promove transições de estado.
- frontend e workers exibem/projetam estado, mas não redefinem regra de transição.

## 5. Contrato mínimo de progresso por etapa

Para cada etapa da sequência automática, registrar no domínio:

- `stepKey`: identificador canônico da etapa;
- `status`: `PENDING | RUNNING | COMPLETED | FAILED`;
- `startedAt` e `finishedAt` (quando aplicável);
- `attempt`: número da tentativa atual;
- `artifactId`: artefato canônico produzido ao concluir;
- `message`: resumo operacional para auditoria e UI;
- `userGuidance`: orientação objetiva do próximo comportamento esperado do usuário.

## 6. Critérios de progressão

A etapa atual só pode transicionar para `COMPLETED` quando:

1. o artefato obrigatório da etapa foi persistido;
2. o artefato passou nas validações canônicas mínimas;
3. o vínculo com `experimentId` está consistente.

Se algum critério falhar:

- etapa vai para `FAILED`;
- fila vai para `BLOCKED`;
- próxima etapa não inicia.

## 7. Critérios de retomada

Uma fila em `BLOCKED` só pode voltar para `RUNNING` quando:

- a causa do bloqueio foi corrigida;
- houve comando explícito de retentativa no backend;
- a retomada reinicia da primeira etapa não concluída.

## 8. Regras de UX e prevenção de erro do usuário

Durante `RUNNING` e `WAITING_DEPENDENCY`, a UI deve proteger o fluxo automático com travas de interação.

### 8.1 Informação contínua na tela

A UI deve exibir, no mínimo:

- status global da fila (`RUNNING`, `WAITING_DEPENDENCY`, `BLOCKED`, `COMPLETED`);
- etapa atual, próximas etapas e etapa concluída mais recente;
- mensagem de progresso em linguagem clara (`message`);
- orientação objetiva de ação/inação (`userGuidance`), por exemplo: “aguarde a conclusão desta etapa”.

### 8.2 Bloqueios obrigatórios de ação

Enquanto a fila estiver ativa, a UI deve:

- desabilitar comandos que alterem manualmente artefatos de etapas já em execução;
- impedir avanço manual para etapa futura sem autorização explícita do backend;
- exigir confirmação textual para ações destrutivas (cancelar fila, resetar etapa, sobrescrever artefato);
- exibir motivo de bloqueio quando botão/comando estiver desabilitado.

### 8.3 Segurança contra conflito de edição

Se houver edição manual concorrente ao fluxo automático:

- backend deve rejeitar gravação conflitante com erro de domínio explícito;
- UI deve apresentar aviso claro e oferecer somente ações seguras (`voltar`, `atualizar estado`, `retentar quando aplicável`);
- alterações não aplicadas não podem ser descartadas silenciosamente.

### 8.4 Regras para estado `BLOCKED`

Quando `BLOCKED`:

- destacar visualmente a etapa que bloqueou;
- exibir causa técnica resumida + ação recomendada;
- disponibilizar apenas comandos necessários para destravar (`retentar`, `corrigir`, `pausar`).

## 9. Observabilidade mínima

Cada transição de estado da fila e de etapa deve gerar log com:

- `experimentId`;
- `stepKey`;
- `fromStatus` e `toStatus`;
- `timestamp`;
- `correlationId` quando existir.

Logs de bloqueio devem incluir também:

- `blockingReasonCode`;
- `userImpact` (mensagem curta para orientar a UI).

## 10. Referências normativas

- `docs/canonical/system-governance-canon.v2.md`
- `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
