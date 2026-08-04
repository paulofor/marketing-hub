# Operador de Crescimento Canonico v1

## Objetivo

O Operador de Crescimento transforma meta, gargalo e evidencias persistidas do planejamento comercial em diagnostico auditavel. A v1 opera continuamente em modo `READ_ONLY_DIAGNOSIS`, seguindo o ciclo de sandbox do exemplo `/exemplos/aih6`.

## Autoridade

- O backend cria a pendencia, congela o contexto e persiste o resultado.
- O worker consome somente o endpoint `pending/claim` e nunca acessa o banco.
- O Codex roda com sandbox `read-only`, identidade ChatGPT persistida em volume proprio e repositorio montado sem escrita.
- Como o repositorio e montado de outro host e pertence a um UID diferente, o comando aceita explicitamente essa arvore com `--skip-git-repo-check`; essa opcao nao amplia permissoes e o sandbox `read-only` permanece obrigatorio.
- O worker mantem o loop operacional, mas solicita ao backend a criacao de cada novo ciclo; somente o backend decide se a cadencia de 30 minutos venceu.
- A investigacao pode consultar endpoints GET oficiais do Marketing Hub e documentacao publica na Internet. Metodos HTTP de mutacao ficam proibidos na v1.
- A v1 nao altera plano, codigo, campanha, preco, orcamento, publicacao, comunicacao ou dados comerciais.
- Toda recomendacao que exija mutacao deve retornar `WAIT_FOR_APPROVAL`.
- O backend nunca aplica automaticamente a proxima acao recomendada.

## Contrato do diagnostico

Cada execucao deve persistir objetivo, gargalo, snapshot de evidencias, exatamente tres alternativas, causa-raiz, metrica esperada, criterios de continuar/ajustar/parar, decisao, proxima acao, resposta bruta, modelo, custo e falha quando houver.

Cada ciclo tambem persiste numero sequencial, origem manual ou automatica e relatorio diario executivo. O snapshot do ciclo seguinte inclui memoria consolidada do planejamento: contagens de todo o historico e linha do tempo recente com conclusoes, evidencias, recomendacoes, falhas e metricas observadas em cada ciclo. A linha do tempo detalhada pode ser limitada para controlar contexto, mas deve declarar truncamento e manter as contagens integrais. Recomendacao deve ser identificada como nao confirmada ate que evidencia posterior comprove sua execucao e seu resultado. Atividade, recomendacao, impacto estimado e PR nunca contam como venda.

Quando o planejamento estiver associado a um experimento, o snapshot deve incluir a inteligencia
de sessoes completa disponivel na janela do funil: resumo, jornadas recentes e eventos individuais
em ordem temporal, incluindo secao, duracao, video, CTA, desempenho, dispositivo, origem e versao.
O snapshot separa `landingAnalytics` de `pdeAnalytics`, para que paginas GeraSalesPage e
experiencias PDE/MUSA sejam analisadas sem mistura de fontes ou versoes.
O contrato inclui ate 2.000 eventos por ciclo e declara `totalEventsAvailable`, `includedEvents` e
`truncated`, para o agente nunca confundir limite de contexto com ausencia de dados. Identificadores
de visitante e sessao devem ser pseudonimizados de forma estavel; IP, user-agent bruto, conteudo
digitado e identificadores publicos completos nao entram no snapshot. O worker continua sem acesso
direto ao banco e toda evidencia usada permanece congelada na execucao auditavel.

O agente nao depende das telas para investigar. Um servidor MCP local, dedicado ao Operador e
vinculado ao planejamento do job, apresenta ferramentas tipadas somente leitura para consultar
planejamento, funil, sessoes, campanhas Meta e memoria historica. O catalogo nao expoe ferramenta
generica HTTP, banco ou metodos mutaveis. Cada resposta inclui ferramenta, planejamento, rota de
origem e horario da consulta para a evidencia permanecer auditavel no resultado do modelo. Para
aprofundar ou atualizar a leitura durante um ciclo, o backend expoe
`GET /api/growth-operator/v1/internal/commercial-plans/{planId}/session-intelligence?eventLimit=2000`,
com o mesmo contrato detalhado, anonimizado e limitado do snapshot. Essa consulta nao autoriza
acesso direto ao banco nem metodos HTTP de mutacao.

## Imagem e operacao

- A imagem de producao e criada exclusivamente pelo `Dockerfile` e pelo Compose versionados em `growth-operator-worker`.
- Toda alteracao na `main` deve acionar o workflow dedicado `growth-operator-worker-ci.yml`, que testa, constroi, sincroniza a revisao versionada no VPS, recria o container e confirma que ele ficou em execucao, possui `CODEX_HOME` gravavel pelo usuario do worker e reconhece a identidade ChatGPT. Assim, o agente analisa sempre a fonte mais recente do Marketing Hub. Atualizacao manual do codigo ou da imagem no VPS nao e um fluxo de publicacao valido.
- O repositorio sincronizado pelo workflow e montado como somente leitura no container, garantindo que o diagnostico use a mesma revisao que acionou o deploy.
- O endereco do registry/IP entra por `GROWTH_OPERATOR_IMAGE`; nenhuma imagem manual fora do repositorio e aceita.
- `COMMERCIAL_PLAN_ID` define o plano ativo acompanhado pelo loop e `MARKETING_HUB_URL` define a origem oficial consultada.
- A credencial Codex/ChatGPT fica no volume dedicado e persistente `/opt/growth-operator/codex-home`, montado em `CODEX_HOME`, sem ser gravada na imagem ou no repositorio. O workflow nao pode substituir esse volume por um diretorio vazio ou sem permissao do usuario do container.

Decisoes permitidas: `CONTINUE`, `ADJUST`, `STOP` e `WAIT_FOR_APPROVAL`.

## Gates de ampliacao

A autonomia somente pode ser ampliada por nova versao e decisao explicita do usuario depois de pelo menos dez diagnosticos confirmados por eventos posteriores, sem violacao de autoridade. Preco, gasto, campanha, publicacao, comunicacao em massa e PR permanecem sujeitos a aprovacao humana.
