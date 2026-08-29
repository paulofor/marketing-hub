# Telemetria de execução dos agentes Codex — cânone v1

## Objetivo

Distinguir um job apenas reservado de um processo Codex efetivamente ativo, sem depender de logs técnicos.

## Contrato obrigatório

- Cliente, Financeiro, Operador de Crescimento, Estrategista, Aprovador de Anúncios Meta e Gerador de Landing enviam heartbeat a cada 15 segundos durante a execução.
- O backend é a fonte de verdade e persiste agente, execução, PID, processo vivo, quantidade de eventos, bytes de saída, última atividade e encerramento.
- Uma execução `RUNNING` sem heartbeat por mais de dois minutos é apresentada como possivelmente presa.
- Eventos representam linhas observadas na saída do processo; não equivalem necessariamente a interações do modelo.
- Tokens de entrada e saída só podem ser persistidos quando forem informados de forma estruturada pelo Codex. É proibido estimar ou converter ausência em zero.
- Falha na telemetria não transforma uma execução funcional em falha, mas deve ser registrada no log do worker.
- O timeout configurado representa o limite de inatividade observável, não a duração total de um trabalho que continua produzindo saída. Uma execução ativa pode avançar até três janelas operacionais, quando então o teto absoluto encerra o processo.
- Dédalo e Atena preservam a lease após timeout por inatividade para uma única retomada automática com a mesma entrada congelada. Nova expiração encerra a execução como falha; é proibido repetir indefinidamente.
- Falha em uma fila auxiliar de um agente não pode impedir o consumo das demais filas independentes. Em especial, indisponibilidade da fila de vídeo não bloqueia a fila financeira de Plutus.

## Visualização

Os painéis dos agentes exibem última atividade, processo ativo, eventos, bytes e tokens disponíveis. A API canônica é `GET /api/codex-agent-telemetry/v1/{agentType}/executions/{executionId}`. Para o Aprovador Meta, `agentType=META_AD_APPROVER`; para o Gerador de Landing, `agentType=LANDING_GENERATOR` e a correlação operacional é o experimento reservado.

A Mesa do Agente deve apresentar separadamente estado do processo, início, último heartbeat,
quantidade de eventos, bytes produzidos, tipo do último evento e atraso superior a dois minutos.
Esses sinais comprovam atividade técnica sem expor raciocínio interno, credenciais ou logs brutos.
O histórico operacional deve, porém, mostrar o prompt integral efetivamente enviado e o tipo de
raciocínio configurado (`high`, `xhigh` ou equivalente), segregados pela própria tarefa. O tipo
configurado não representa nem autoriza expor cadeia de pensamento do modelo. Zero tokens só pode
ser apresentado como consumo quando o provedor tiver informado a medição; ausência continua
explicitamente desconhecida.

O monitor administrativo `GET /api/agents/work-monitor` consolida por agente os tokens de entrada e saída das execuções iniciadas no dia comercial (`America/Sao_Paulo`). O contador é recalculado a cada consulta e a tela o atualiza no mesmo polling de 15 segundos do estado operacional. Ausência de telemetria reportada é exibida como zero, sem estimativa local.

O mesmo monitor deve expor a prontidão do executor por uma prova ativa e auditável. Cada
executor reporta sua versão implantada, acesso ao backend e autenticação Codex validada por
`codex login status`, sem executar prompt, gerar conteúdo ou consumir tokens. A leitura vence em
dez minutos. A reconexão OAuth administrativa deve ser iniciada pelo Marketing Hub, persistida
como comando auditável e executada no worker pelo Codex App Server. O frontend pode receber apenas
URL, código temporário e estado; tokens e `auth.json` nunca transitam pelo backend ou pela tela.
Ausência ou vencimento deve aparecer como `UNKNOWN`, nunca como executor saudável.
Versão divergente, backend inacessível ou autenticação inválida deve aparecer como `BLOCKED` com
causa acionável. O request interno canônico é
`POST /api/internal/agents/executor-health`.

A versão reportada pelo Compose de cada executor deve acompanhar a versão corrente do agente
ativada pelo backend. Em implantação, `buildReference` deve receber o SHA imutável do commit pelo
workflow; o fallback `local` é permitido somente em desenvolvimento. Teste de contrato no próprio
worker deve bloquear divergência de versão e deploy sem referência auditável de build.

## Critério operacional

Continuar quando heartbeat e saída avançarem; investigar quando o processo estiver vivo sem mudança de saída; considerar a execução presa quando o backend marcar `stale=true`. Ao recuperar uma lease presa, reutilizar a entrada congelada e preservar a correlação; após uma segunda expiração, parar e expor a dificuldade para decisão externa.

Toda nova tarefa terminal, bloqueada ou concluída, deve declarar seu modo de execução. Quando houver
modelo, modelo efetivo, tipo de raciocínio configurado e prompt integral são obrigatórios. Todo
bloqueio deve acrescentar categoria, orientação concreta e ao menos um link seguro. URLs realmente
abertas por Psique devem ser distinguidas de URLs apenas recebidas no contexto e vinculadas somente
ao `taskId`; parâmetros de credencial não podem ser persistidos nem mostrados.
