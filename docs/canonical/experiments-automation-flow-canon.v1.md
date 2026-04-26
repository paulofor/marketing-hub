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
7. `landingPageDesignPreset` (Preset visual da Landing)

Após `landingPageDesignPreset`, a etapa `landingPageHtml` **não** deve ser enfileirada automaticamente.
Ela passa a ser uma escolha manual do usuário na UI, com duas opções válidas:

- `Gerar com LHM` (determinístico);
- `Gerar com IA` (não determinístico, sujeito ao contrato de validação do backend).

Regra canônica:

- uma etapa só pode iniciar quando a etapa anterior estiver em `COMPLETED`.
- avanço fora de ordem é drift e deve ser bloqueado no backend.

### 3.1 Desvio controlado obrigatório entre planejamento e preset visual

Entre as etapas `landingPageImagePlanning` e `landingPageDesignPreset`, existe um
**desvio controlado obrigatório** para geração das imagens finais da landing.

Fluxo canônico:

1. `landingPageImagePlanning` conclui em `COMPLETED`;
2. sistema dispara a geração de imagens planejadas (`framework images`);
3. fila entra em `WAITING_DEPENDENCY` enquanto houver item em `PLANNED`,
   `PENDING` ou `PROCESSING`;
4. após concluir (ou consolidar erro tratável) os itens de imagem, a fila
   retorna ao trilho principal e só então libera `landingPageDesignPreset`.

Regras mandatórias:

- `landingPageDesignPreset` não pode iniciar enquanto a geração de imagens estiver em
  andamento.
- o retorno ao trilho principal deve ser automático até `landingPageDesignPreset`
  (sem exigir ação manual quando não houver bloqueio).
- se houver falha na geração de imagens, a UI deve apresentar causa e ação
  recomendada antes de permitir retomada.
- após `landingPageDesignPreset`, a continuidade para `landingPageHtml` exige
  comando manual explícito do usuário (LHM ou IA).

## 4. Estados da fila automática

Estados permitidos para execução da fila:

- `IDLE`: fila automática desligada, sem execução em curso;
- `RUNNING`: fila automática ativa com etapa em processamento;
- `WAITING_DEPENDENCY`: aguardando pré-condição externa (worker, artefato obrigatório ou sincronização);
- `BLOCKED`: execução interrompida por erro de regra, contrato ou validação;
- `COMPLETED`: todas as etapas automáticas concluídas para o experimento
  (termina em `landingPageDesignPreset`).

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

## 6.1 Regra canônica de isolamento entre experimentos (obrigatória)

No pipeline de experimento, **as informações de um experimento não podem, em hipótese alguma, influenciar outro experimento**.

Regras mandatórias:

- toda leitura de artefato para compor prompt, contexto, validação ou retomada deve ser filtrada por `experimentId`;
- outputs de etapas anteriores só podem ser reaproveitados quando forem fatos persistidos do **mesmo** `experimentId`;
- campos legados, snapshots antigos, cache em memória, retries e retomadas não podem “vazar” conteúdo de outro experimento;
- qualquer tentativa de processar etapa com contexto de `experimentId` diferente deve ser tratada como erro de domínio e bloqueio (`BLOCKED`);
- frontend, backend e workers devem preservar o mesmo escopo de isolamento por `experimentId` em toda transição de estado.
- a classificação de exclusividade de artefatos deve seguir a regra global descrita em `system-governance-canon.v2.md`.

Critério de conformidade:

- se não houver artefato predecessor concluído no mesmo `experimentId`, o pipeline deve operar sem esse contexto (nunca buscar em outro experimento).

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
- quando houver desvio controlado para geração de imagens, explicitar que o
  fluxo saiu temporariamente de `landingPageImagePlanning`, está gerando imagens
  e retornará automaticamente para `landingPageHtml`.

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

### 8.5 Fluxo canônico simplificado de publicação da landing (obrigatório)

Para experimentos com tráfego direcionado para landing própria, o processo oficial
de publicação deve ser simplificado em **3 passos**:

1. **Geração da landing pela IA**
   O pipeline gera o HTML final da landing para o experimento.
   A composição final deve ser executada pelo **LHM (Landing HTML Module)**,
   responsável por consolidar wireframe aprovado, copy aprovada, design preset
   aprovado e URLs de imagens aprovadas em um único `htmlDocument`.
2. **Aprovação única do usuário**
   O usuário aprova a landing uma única vez na interface administrativa.
3. **Publicação automática pelo sistema**
   Após a aprovação, o backend/sistema:
   - cria/publica a URL final da landing;
   - aplica automaticamente o pixel do nicho na landing publicada.

Regras mandatórias:

- não exigir etapa manual adicional entre aprovação e publicação da URL final;
- não exigir etapa manual adicional para inserção do pixel do nicho;
- manter o backend como fonte única de verdade para URL publicada e vínculo de pixel;
- exibir feedback claro de sucesso/erro da aprovação e do resultado da publicação.

### 8.6 Nome canônico do módulo de composição de landing

Para padronizar comunicação técnica e documentação entre backend, frontend e
worker, o módulo de composição final da landing passa a ser nomeado
oficialmente como:

- **LHM (Landing HTML Module)**.

Definição operacional do LHM:

- recebe como entradas canônicas: `wireframe`, `copy`, `designPreset` e `imagens (URLs)`;
- entrega como saída canônica: `htmlDocument` final da landing;
- roda no backend como fonte única de verdade para aplicação de contratos e
  validações de publicação.
- a UI pode oferecer o comando `Gerar com LHM` como alternativa explícita ao
  `Gerar com IA` na etapa `landing-page-html`.

### 8.7 Aba canônica de destino da campanha (Landing)

A navegação principal do experimento deve expor a aba `Landing` para suportar o
fluxo simplificado definido na seção 8.5.

Regra mandatória de posicionamento da ação de aprovação:

- o comando primário de aprovação/publicação da landing deve estar na aba `Landing`;
- o rótulo recomendado é `Aprovar e publicar landing` (ou equivalente semântico direto);
- é proibido deslocar a ação principal de aprovação para outra aba como único ponto de execução;
- controles auxiliares (pré-visualização, diagnóstico e reprocessamento) podem existir em outras abas, desde que a aprovação oficial permaneça acessível na aba `Landing`.

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

### 9.1 Regra canônica para quebra de contrato de saída de modelo (AI Worker)

Quando o Worker IA identificar quebra de contrato no output esperado do modelo
(por exemplo: seção que exige `HTML puro` e retorna payload incompatível, JSON
inválido, ou ausência de campo obrigatório no artefato), é obrigatório registrar
no log do Spring:

- `experimentId`;
- `section`/`stepKey`;
- motivo explícito da quebra do contrato;
- resposta retornada pelo modelo (texto bruto, com truncamento seguro quando necessário).

Regras mandatórias:

- o log deve ocorrer **antes** do job ser marcado como falho;
- o motivo precisa permitir diagnóstico operacional sem depender de reprodução manual;
- toda falha por contrato de schema/formato deve manter a resposta do modelo auditável nos logs.

## 10. Referências normativas

- `docs/canonical/system-governance-canon.v2.md`
- `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
