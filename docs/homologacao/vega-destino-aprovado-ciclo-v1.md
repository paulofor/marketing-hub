# Vega — destino aprovado do ciclo e conclusão da comunicação

## Diagnóstico confirmado em 12/09/2026

A tela do produto 4, cadeia 14, ciclo 2, processo 63 mostra duas atividades concluídas.
O MCP confirmou as tarefas 409 e 410 bloqueadas antes do modelo: `approvedLandingAssets`
está vazio no contexto privado. A homologação 395 e os pareceres 396–399 aprovam o
protótipo MUSA v12; a comunicação 402 e o criativo 406 apontam para essa mesma experiência
privada. Psique 407 e Têmis 408 aprovaram a peça nesse escopo. O operador registrou a
decisão sobre o criativo na ocorrência 266; isso não cria checkout ou campanha.

A definição do pai determina usar o subprocesso de landing **quando esse for o destino
aprovado**. A implementação chamava o filho incondicionalmente. Além disso, a integração
procurava plano e slot comerciais, embora o sucessor tenha contratos privados próprios.
O histórico de landing comercial (por exemplo, tarefa 243) pertence a outro contrato;
não comprova a existência desses pré-requisitos no experimento 92.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Criar uma landing privada adicional | Permite testar outra apresentação | Introduz página e variável adicionais; nova produção e revisão | Reservada para hipótese que a exija |
| Adaptar todo o pipeline de landing comercial ao modo privado | Reutiliza suas oito etapas | Amplia contratos de HTML, checkout, revisão e publicação sem necessidade atual | Fora do caminho deste ciclo |
| Resolver o destino no backend e reutilizar o PDE já homologado | Preserva hipótese, URL, provas e versão; evita produção duplicada | Exige decisão persistida, invalidação e tratamento do filho já aberto | Adotada |

Uma experiência privada aprovada não vira página comercial. A conclusão deverá significar
comunicação e jornada privadas preparadas, com checkout simulado, sem autorização de campanha,
cobrança ou tráfego comercial. A geração de landing continua disponível quando exigida pelo
contrato comercial. Tentativas antigas permanecem bloqueadas no histórico; não serão aprovadas.

## Matriz definida antes dos testes

| Área | Critérios de aceite local |
| --- | --- |
| Caminho feliz | Contrato e criativos concluídos → destino privado comprovado → integração → pai concluído |
| Caminho comercial | Continua chamando o subprocesso canônico de landing; não usa prova privada como comercial |
| Validações | Recusar produto, referência, ciclo, versão, URL, hash e gate divergentes; recusar predecessor ausente ou substituído |
| Integração | Usar contratos reais de acesso, retomada, checkout simulado e eventos já homologados; não exigir plano/slot histórico |
| Recuperação | Filho indevido já bloqueado é encerrado com motivo; preservar tarefas, custos e links; aguardar qualquer trabalho em curso |
| Concorrência | Repetição idempotente, locks por produto, isolamento entre produtos/ciclos, pausa e reinício |
| Persistência das consultas | Repositório JPA real deve ler prontidão e prova arquivada em transação somente leitura; reserva de escrita continua com lock |
| Observabilidade | Prova persistida da escolha e integração, motivo claro na tela e histórico auditável |
| Métricas | Nenhuma venda, receita, consentimento ou prova humana inferida dos testes; sem chamadas pagas na sandbox |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; retorno pai/filho, contexto copiável e ausência de overflow |
| Empacotamento | Suíte backend completa, frontend/worker conforme impacto, MySQL 5.7, JAR correspondente às classes testadas e imagem |

Após a última correção, duas rodadas locais completas e consecutivas devem passar antes da
aplicação autorizada. O runtime deve ser coordenado por `begin`/`execute`; imagens derivadas
dos arquivos do repositório, conferidas por conteúdo. Não criar commit, push ou PR nesta tarefa.
Resultados, limitações e aplicação serão acrescentados somente após sua execução.

## Reprodução do caso real e revisão operacional

O prompt auditado da tarefa 402 contém o contexto congelado do ciclo. Esse contexto, as tarefas
395/402/406–410 e as ocorrências exportadas por SELECT via MCP alimentaram um replay offline.
O teste `PrivateCommunicationJourneyReplayTest` executa somente quando `PRIVATE_JOURNEY_REPLAY`
aponta para esse arquivo local; não consulta produção, não altera os registros de origem e não
envia eventos de teste ao funil. A suíte normal conserva fixtures sintéticas independentes.

O replay confirmou os mesmos bytes de comunicação, peça, pareceres e decisão, gerando novas
ocorrências apenas no repositório simulado. O contrato adicional `approvedDestination` é o
mesmo emitido pelo contexto backend corrigido, sem inventar evidência de landing comercial.

Para a aplicação autorizada foram comparadas três opções: o script amplo de aplicação (testado,
mas recria serviços adicionais e limpa tags), recriar containers por configuração de `inspect`
(preserva o runtime, porém duplica a definição operacional) e Compose versionado com troca
restrita das duas imagens (preserva configuração e permite rollback). A terceira foi escolhida.
O SHA-256 do Compose no host é igual ao do repositório. Credenciais serão herdadas em memória,
sem exposição; nenhuma dependência ou secret novo é necessário. A intervenção anterior já
protege os escopos APP e Íris e não deve ser liberada antes da integração desta correção.

## Ajustes encontrados antes das rodadas finais

- A preparação da sandbox exigiu instalar o lockfile do frontend. O runner ampliado também
  precisava receber a origem PNG; passou a validar pré-requisitos antes da suíte longa e a
  definir uma prévia separada por rodada automaticamente. A origem 118 foi baixada somente
  pelo endpoint oficial e conferida pelo SHA-256 persistido.
- A revisão preservou o link de consulta do subprocesso após sua conclusão, sem habilitar
  uma nova execução por esse motivo.
- Dois testes reproduziram a perda indevida de objetivos após encerramento/avanço de fase.
  Foram comparadas três alternativas: congelar todas as conclusões inclusive na preparação
  ativa (perderia invalidação), ampliar o contexto de execução para ciclos fechados (misturaria
  autorização e auditoria) e consultar a prova arquivada fora da preparação, mantendo
  revalidação durante a fase ativa (adotada). O histórico não depende da permissão para novo trabalho.
- Após a correção, os 47 testes focados passaram: 17 do contrato privado, 1 replay dos dados
  exportados e 29 do serviço de execução. As rodadas anteriores são diagnósticas; as duas
  rodadas finais recomeçam depois dessa última correção.

## Homologação local anterior à validação publicada

As rodadas `private-verified-1` e `private-verified-2` passaram integralmente, consecutivas,
sem alteração dos arquivos da entrega entre elas. A validação publicada revelou depois a
lacuna de persistência descrita abaixo; essas rodadas deixam de ser a homologação final.

| Verificação por rodada | Resultado |
| --- | --- |
| Backend completo, incluindo replay do caso real | 2.748 executados, zero falhas/erros; sete testes opcionais preexistentes ignorados |
| Frontend | 624 testes, 161 arquivos, zero falhas |
| Executor de processos | Seis testes unitários e 27 cenários HTTP/MySQL 5.7, incluindo reinício |
| Interfaces | Desktop, iPhone 15 Pro e Pixel 7 emulados; conclusão, destino aprovado, histórico pai/filho e cópia do contexto |
| Integrações de criativos | Íris: 26; Psique: 106; Têmis: 91 testes executados sem falhas; um teste opcional ignorado em cada revisor |
| Artefatos | PNG de origem conferido pelo hash; renderização e visualização local nos três dispositivos |
| Entrega | Compilação, tipos, formatação, contratos de pacote/imagem, bytes do JAR e recursos empacotados aprovados |

Os sete testes opcionais do backend e os dois dos revisores não representam critérios
pendentes da matriz. A execução dos agentes e integrações externas foi simulada localmente;
o replay usou contratos persistidos reais, sem chamada paga e sem escrita no banco publicado.

Evidências locais: `artifacts/process-automation/private-verified-{1,2}/`,
`artifacts/process-context-copy/private-verified-{1,2}/` e
`artifacts/vega-process-recovery/private-verified-{1,2}/`.
O manifesto arquivado `artifacts/vega-landing-recovery/source-manifest-d51c5bfffedb.json` fixa os 26 arquivos
de código, testes e infraestrutura pela soma SHA-256 agregada
`d51c5bfffedbf12cff47bf3ef028921ec7fa3411a7edde42c473951ed23ae929`.
As imagens foram construídas pelos Dockerfiles versionados sobre a revisão
`4eebe408bf0fa58a03f6bdb5207adacd45ba01e6`, com as alterações locais identificadas pela
tag `vega-destination-d51c5bfffedb`; essa tag não representa um commit integrado à main.

## Verificação da transferência

A sandbox usa Docker 27.5.1 e o host usa Docker 29.3.0 com descritor OCI. A primeira
comparação de `image inspect.Id` interrompeu a aplicação antes de trocar containers:
o ID local era o hash da configuração, enquanto o host devolveu o hash do manifesto.
As camadas eram idênticas. Foi então exportada e examinada a imagem carregada no host:
os bytes da configuração têm exatamente o hash local, e o manifesto OCI referencia
esse mesmo hash. A configuração funcional também coincide; só campos vazios/padrão
de `inspect` eram omitidos na versão nova, comportamento descrito na
[documentação Docker](https://docs.docker.com/engine/deprecated/).

| Imagem | Configuração homologada na sandbox | Manifesto conferido no host |
| --- | --- | --- |
| Backend | `e319f82dd7bf77972602ac7d4df4ab52725c228e9aed7bc3eedb210f8b5d69bb` | `f9c835ea7fadad1c03aac1214b71b7e1749deb079859dc69748cb88055897e8e` |
| Frontend | `f6ec64e57165a6e3e385ae12a9e778ed7633b964d016e1d2f01ba40b0dc1b5d6` | `ea8b22b5d298df3927c4a35770619cb6080a55d527839cdafbb0520437560ab3` |

A evidência dessa transferência é `artifacts/vega-landing-recovery/transfer-verification-d51c5bfffedb.json`. Não houve
reconstrução nem alteração do código após as rodadas aprovadas. A conferência do
transporte passou a distinguir os dois tipos de digest em vez de aceitar IDs divergentes
sem comprovação dos bytes. O JAR homologado tem SHA-256
`4130f0fb44f087821410c6a25f2c8819afcb89a57c184465fce198013a373bef`.

A primeira aplicação usou `compose up --wait`; a janela inicial do healthcheck terminou
antes da inicialização completa do Spring. O MCP registrou a aplicação iniciada em 92
segundos e, em seguida, seu encerramento pela restauração operacional. A versão anterior
também passou por `unhealthy` durante o bootstrap, depois respondeu `UP`, confirmando que
o sinal transitório não era uma falha do novo contrato. O processo permaneceu pausado.
Foi mantida a configuração do container e adotada a espera limitada com confirmação
de estabilidade, conforme `deploy/bin/backend-health.sh`, em vez de encerrar a tentativa
na primeira janela de saúde. Nenhum arquivo da aplicação nem imagem foi alterado por esse
ajuste operacional. As tentativas estão em `release-apply.log` e `release-apply-2.log`.

## Falha de consulta reproduzida e corrigida na sandbox

Após a aplicação, os GETs `activity-executions` e `process-context` retornaram HTTP 500.
O MCP registrou MySQL 1792: `Cannot execute statement in a READ ONLY transaction`.
A tela ficou carregando o histórico e nenhum comando de retomada foi enviado. A versão
anterior foi restaurada pelo coordenador, mantendo o processo pausado durante a correção.

O método histórico `findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc`
possui `PESSIMISTIC_WRITE`. O novo leitor de prontidão havia reutilizado esse método em
transação somente leitura. Os doubles dos repositórios não reproduziam esse contrato físico.
O teste novo usa as três entidades e o repositório JPA reais, transações Spring e MySQL 5.7:
antes da correção, reproduziu o mesmo erro 1792 em duas consultas; a reserva de escrita passou.

Foram comparadas três alternativas: remover o lock global (perderia serialização de comandos),
permitir escrita em todas as consultas (misturaria responsabilidades) ou criar uma consulta
sem lock e manter a reserva exclusivamente no comando de conclusão (adotada, escopo mínimo).
A prontidão e a prova arquivada passam a usar a consulta; a gravação conserva a reserva.
O novo teste também examina o SQL emitido, roda na suíte comum com H2 e obrigatoriamente
com MySQL 5.7 em cada rodada integral. O schema `private_journey_local` é descartável,
separado da matriz de automação e nunca utiliza credenciais nem dados de produção.

Evidência da reprodução: `artifacts/vega-landing-recovery/persistence-before-mysql.log`.
As duas rodadas finais devem recomeçar depois desta correção, incluindo a matriz ampliada.

Na rodada `private-mysql-final-1`, os 2.751 testes de backend, 624 de interface, os 27
cenários HTTP/MySQL e os três testes JPA/MySQL passaram. O navegador revelou uma corrida
no driver da homologação: após clicar em retomar, ele conciliava o processo antes de receber
a confirmação HTTP; em seguida, simulava resultado para uma tarefa ainda não criada.
O erro `Tarefa não encontrada` também foi reproduzido no desktop introduzindo 350 ms de
latência somente na rota local de retomada (`browser-race-before/browser.log`).

Esperar um tempo fixo seria instável; consultar a tarefa até aparecer esconderia uma falha
do comando; aguardar a resposta HTTP e confirmar `QUEUED` preserva o contrato (adotado).
O driver agora aguarda essa confirmação nas duas retomadas e mantém a latência simulada
nos três dispositivos. Nenhuma regra de execução da aplicação foi modificada por esse ajuste.
Essa rodada também é diagnóstica: as duas rodadas completas reiniciam após a correção do driver.

## Homologação final aprovada

As rodadas `private-release-final-1` e `private-release-final-2` passaram completas e
consecutivas, sem alteração do código entre elas. Cada rodada executou:

- 2.751 testes de backend sem falhas ou erros, incluindo o replay do caso real; sete
  testes opcionais preexistentes foram ignorados.
- 624 testes de frontend, tipos e compilação; seis testes do executor de processos.
- 27 cenários HTTP com MySQL 5.7, recuperação após reinício e três testes adicionais
  do repositório JPA real com MySQL 5.7, inclusive a restrição de transação somente leitura.
- Navegação desktop, iPhone 15 Pro e Pixel 7 emulados, retomada com latência,
  conclusão do processo, destino aprovado, histórico pai/filho e cópia real do contexto.
- Suítes de Íris, Psique e Têmis, renderização e inspeção do criativo, contratos de
  empacotamento e conferência dos bytes do JAR dentro das imagens Docker.

O resumo está em `artifacts/vega-landing-recovery/release-final-rounds.log`; as evidências
estão nos diretórios `private-release-final-{1,2}` de `artifacts/process-automation/`,
`artifacts/process-context-copy/` e `artifacts/vega-process-recovery/`.
O manifesto final fixa os 29 arquivos de código, testes e infraestrutura pela soma
`7cfb9392ba65d61a7f7180cfcdcc1f107dfd6fe527cdee52420260f67abf7f5d`, com a tag
`vega-destination-7cfb9392ba65`. A base permanece
`4eebe408bf0fa58a03f6bdb5207adacd45ba01e6`; as alterações locais ainda não foram integradas.

As integrações externas foram simuladas localmente. O replay utilizou dados históricos
exportados por leitura; nenhum evento de teste foi enviado ao funil publicado. Os dois
celulares foram emulados no Chromium, sem alegação de teste em Safari ou aparelhos físicos.

## Aplicação autorizada e resultado persistido

As imagens finais foram transferidas e aplicadas pelo coordenador, na intervenção
`f3221756d77344dd94a10c02816956fe`, usando os Dockerfiles e o Compose versionados.
A aplicação terminou em `2026-09-12T16:44:36Z`, conforme o recibo do host. Backend,
frontend e executor de processos ficaram saudáveis; configuração, credenciais,
montagens e portas existentes foram preservadas. O executor não precisou de nova imagem.

| Imagem final | Configuração conferida na sandbox | Manifesto conferido no host |
| --- | --- | --- |
| Backend | `bea63433f7e14e933e53605f57e2fee8f213ec3ab3c2a0ed9d1020e7315befea` | `599ffc94e471353330e7d7107eeeeeb7d648aa32987927767b7e907d6626a000` |
| Frontend | `cd6cfc7781ad1a0b6e0aeb4a615b7c21608512a2b84dacc22c0ff80f3c0ad73d` | `219878813166ca9e6cfdd71ef29d0b6209bd72a29091ff9a94b7c47901eb231d` |

O JAR executado no host e o conferido na sandbox possuem o mesmo SHA-256:
`12998b403ec15bfb2131cbfe3395a495917ac45e42dba3bca3ed635b181c7d21`.
Recibo remoto: `/opt/marketinghub/containers/interventions/vega-destination/vega-destination-7cfb9392ba65/receipt.json`.
Manifesto, conferência de transferência, saúde e cópia do recibo estão em
`artifacts/vega-landing-recovery/`, nos arquivos `release-manifest.json`,
`transfer-verification.json`, `final-health.json` e `release-receipt-final.json`.

Antes da retomada, os endpoints `process-context`, `activity-executions` e `automation/v1`
responderam HTTP 200; o pai estava pausado, com duas atividades concluídas e o destino
aprovado disponível como comando do backend. O botão **Retomar processo** foi acionado
uma única vez pela interface. O controlador `ProcessRunController.resume` persistiu a
retomada pelo `ProcessRunService`; o executor consumiu a fila `pending` e chamou o
`reconcile` oficial. Os logs do executor registraram `WAITING_ACTIVITY` e `COMPLETED`;
o avanço e as conclusões ficaram registrados no backend.

| Registro confirmado por API e SELECT via MCP | Resultado |
| --- | --- |
| Produto 4, cadeia 14, ciclo 2, experimento 92, processo 63, execução 1 | `COMPLETED`, quatro concluídas, zero restantes e objetivo comprovado |
| Destino, atividade 639, ocorrência 269 | `PDE_COMMUNICATION_PRIVATE_DESTINATION_V1`, concluída com prova do ciclo |
| Integração, atividade 640, ocorrência 270 | `PDE_COMMUNICATION_PRIVATE_INTEGRATION_V1`, concluída com prova do ciclo |
| URL e versão | `https://v7.clubemusa.com.br/vega-private`, `musa-pde-entry-v12-primeiro-ajuste-aplicavel` |
| Escopo persistido nas duas provas | `checkoutMode: SIMULATED`, `publicationAuthorized: false` |
| Subprocesso 65, execução 3 | `CLOSED`, motivo de não necessidade registrado; suas oito etapas não foram declaradas concluídas |
| Histórico das tarefas | 44 tarefas no experimento antes e depois; maior ID 410; 409 e 410 permanecem `BLOCKED` |

Os eventos 87–94 documentam a retomada, os comandos de destino e integração, o fechamento
do filho e a conclusão do pai. Nenhuma nova tarefa de IA foi criada para essa conclusão;
não houve chamada paga, campanha, cobrança ou evento de teste no funil. Os custos e as
tentativas anteriores permanecem preservados.

A primeira conferência publicada passou no desktop, mas a abertura seguinte no iPhone
excedeu os 30 segundos do verificador. Uma consulta instrumentada independente abriu o
painel em 10.978 ms, sem erro HTTP, de rede ou JavaScript. A conferência completa seguinte,
somente leitura e sem novo clique de retomada, passou no desktop, iPhone e Pixel com os
mesmos timeouts e sem alteração da aplicação. Foram verificados progresso de 100%, link do
destino aprovado, motivo de encerramento do filho, retorno ao pai, ausência de overflow e
de erros JavaScript. Cancelamentos de requisições ao navegar entre páginas foram registrados.

Evidências publicadas: `published/resume-triggered.json`, `published-browser-final.log`,
`published-browser-readonly-final.log`, `published/*-completed.png`,
`published/*-child-history.png`, `published/*-audit.json`, `after-completion-*.json`,
`parent-events-final.json`, `child-events-final.json`, `child-final.json` e `worker-final.log`.

Nenhum commit, push ou PR foi criado. A conferência final do coordenador permanece
`ACTIVE`, sem publicação concorrente, com cinco publicadores pausados. Essa proteção só
deve ser liberada após a integração desta correção na `main`, conforme o cânone de retomada.
O resultado é a preparação privada concluída; não representa autorização comercial nem
validação de vendas.
