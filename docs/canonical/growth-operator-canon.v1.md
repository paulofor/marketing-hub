# Operador de Crescimento Canonico v1

## Objetivo

O Operador de Crescimento transforma meta, gargalo e evidencias persistidas do planejamento comercial em diagnostico auditavel. A v1 opera continuamente em modo `READ_ONLY_DIAGNOSIS`, seguindo o ciclo de sandbox do exemplo `/exemplos/aih6`.

## Autoridade

- O backend cria a pendencia, congela o contexto e persiste o resultado.
- O worker consome somente o endpoint `pending/claim` e nunca acessa o banco.
- O Codex roda com sandbox `read-only`, identidade ChatGPT persistida em volume proprio e repositorio montado sem escrita.
- Como o repositorio e montado de outro host e pertence a um UID diferente, o comando aceita explicitamente essa arvore com `--skip-git-repo-check`; essa opcao nao amplia permissoes e o sandbox `read-only` permanece obrigatorio.
- O worker mantem o polling operacional, mas o backend so cria novo ciclo quando o fingerprint das evidencias muda. A passagem de 30 minutos, isoladamente, nunca justifica novo consumo de IA.
- A investigacao consulta APIs oficiais e documentacao publica. A unica mutacao autonoma permitida e solicitar pausa preventiva; o backend valida gates deterministas, registra auditoria e aciona o worker da Meta. Retomada apenas registra pedido para aprovacao humana.
- A v1 nao altera plano, codigo, campanha, preco, orcamento, publicacao, comunicacao ou dados comerciais.
- Toda recomendacao que exija mutacao deve retornar `WAIT_FOR_APPROVAL`.
- O backend nunca aplica automaticamente a proxima acao recomendada.

## Contrato do diagnostico

Cada execucao deve persistir objetivo, gargalo, snapshot de evidencias, exatamente tres alternativas, causa-raiz, metrica esperada, criterios de continuar/ajustar/parar, decisao, proxima acao, resposta bruta, modelo, custo e falha quando houver.

O fingerprint exclui a memoria acumulada e inclui apenas evidencias operacionais atuais. Assim, o proprio relatorio anterior nao cria artificialmente uma mudanca. Cada execucao tambem persiste as chamadas MCP realmente observadas, permitindo auditar quais ferramentas fundamentaram a conclusao.

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
vinculado ao planejamento do job, apresenta ferramentas tipadas para consultar
planejamento, funil, sessoes, campanhas Meta, estrategia de videos e memoria historica. O catalogo nao expoe ferramenta
generica HTTP nem banco. As unicas mutacoes sao a pausa governada e o pedido de retomada que nunca
reativa diretamente. Cada resposta inclui ferramenta, planejamento, rota de
origem e horario da consulta para a evidencia permanecer auditavel no resultado do modelo. Para
aprofundar ou atualizar a leitura durante um ciclo, o backend expoe
`GET /api/growth-operator/v1/internal/commercial-plans/{planId}/session-intelligence?eventLimit=2000`,
com o mesmo contrato detalhado, anonimizado e limitado do snapshot. Essa consulta nao autoriza
acesso direto ao banco nem metodos HTTP de mutacao.

A ferramenta `consultar_estrategia_videos` consolida pelo experimento a hipotese registrada no
Estudio, funcao no funil, custo comercial, progressao dos videos, acoes posteriores, vendas e
aprendizados. O Operador deve comparar pecas do mesmo `strategyGroupKey`, distinguir custo de
producao de gasto de campanha e somente confirmar aprendizado quando houver eventos humanos
atribuidos.

Estrategias de outro produto ou experimento nunca devem ser vinculadas automaticamente apenas por compartilharem tema, personagem ou grupo narrativo. Ausencia de estrategia compativel e uma lacuna valida; vinculo incorreto e contaminacao de evidencia.

## Imagem e operacao

- A imagem de producao e criada exclusivamente pelo `Dockerfile` e pelo Compose versionados em `growth-operator-worker`.
- Toda alteracao na `main` deve acionar o workflow dedicado `growth-operator-worker-ci.yml`, que testa, constroi, sincroniza a revisao versionada no VPS, recria o container e confirma que ele ficou em execucao, possui `CODEX_HOME` gravavel pelo usuario do worker e reconhece a identidade ChatGPT. Assim, o agente analisa sempre a fonte mais recente do Marketing Hub. Atualizacao manual do codigo ou da imagem no VPS nao e um fluxo de publicacao valido.
- O repositorio sincronizado pelo workflow e montado como somente leitura no container, garantindo que o diagnostico use a mesma revisao que acionou o deploy.
- O endereco do registry/IP entra por `GROWTH_OPERATOR_IMAGE`; nenhuma imagem manual fora do repositorio e aceita.
- `COMMERCIAL_PLAN_ID` define o plano ativo acompanhado pelo loop e `MARKETING_HUB_URL` define a origem oficial consultada.
- A credencial Codex/ChatGPT fica no volume dedicado e persistente `/opt/growth-operator/codex-home`, montado em `CODEX_HOME`, sem ser gravada na imagem ou no repositorio. O workflow nao pode substituir esse volume por um diretorio vazio ou sem permissao do usuario do container.

Decisoes permitidas: `CONTINUE`, `ADJUST`, `STOP` e `WAIT_FOR_APPROVAL`.

`RUN`, retomada automatica, aumento de orcamento e publicacao permanecem proibidos.

## Metas e gates de gasto

- Cada ciclo recebe separadamente o teto mensal e os objetivos da semana comercial vigente.
- A semana vigente tem precedencia na decisao operacional; o teto mensal nunca representa autorizacao de gasto.
- O menor gate monetario do criterio de parada e preventivo. Quando ele for atingido sem receita comprovada, o backend bloqueia `CONTINUE` e registra `WAIT_FOR_APPROVAL`.
- Objetivo semanal ausente ou contraditorio deve ser tratado como lacuna de planejamento, nunca preenchido por suposicao do modelo.
- Quando o prazo do proprio plano termina na semana vigente, o objetivo comercial persistido no plano e uma meta semanal efetiva e deve ser enviado com a origem `PLAN_DEADLINE_IN_CURRENT_WEEK`; isso nao cria nem inventa outra meta.

## Gates de ampliacao

A autonomia somente pode ser ampliada por nova versao e decisao explicita do usuario depois de pelo menos dez diagnosticos confirmados por eventos posteriores, sem violacao de autoridade. Preco, gasto, campanha, publicacao, comunicacao em massa e PR permanecem sujeitos a aprovacao humana.
