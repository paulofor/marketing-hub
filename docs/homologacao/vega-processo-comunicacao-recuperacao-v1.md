# Recuperação da comunicação do Vega e navegação entre processos

Solicitação de 12/09/2026: executar o processo 63, produto 4, cadeia 14, ciclo 2,
experimento 92; preservar o contexto copiável e padronizar links entre pai e filhos.

## Evidências iniciais

Tela pública e MCP confirmaram execução #1 bloqueada em `communicationContract`.
As tarefas #400 e #401 bloquearam por ausência de checkout comercial e ativos finais.
O contexto recebido é `LEARNING_CYCLE_PRIVATE`, com gate multiagente vigente e sem
autorização de cobrança/publicação. A constituição do worker exige checkout sem distinguir
a atividade de planejamento da composição final. A integração da tela também informa
posição histórica incompatível: o executor consultava a posição global, em vez dos
predecessores e da versão do ciclo aberto. A correção preserva os requisitos reais de
slot publicado e checkout para a integração comercial final.

## Decisões comparadas

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Concluir previamente toda configuração comercial com dados reais | Contrato completo para a futura publicação | Anteciparia decisões e trabalho que a preparação privada ainda não exige | Não escolhida para esta etapa |
| Criar uma versão exclusiva do pipeline de comunicação privada | Isola completamente as políticas | Amplia manutenção, migração e homologação de dois fluxos | Reservada para uma mudança integral futura |
| Corrigir requisitos por atividade e escopo; preservar gates e evidências | Remove dependência circular e mantém qualidade | Exige regressões de contratos e integração | Escolhida |

| Navegação | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Rotas aninhadas com pai e filho validados no backend | Contexto explícito no endereço | Migração de links e mais parâmetros em todos os pontos de entrada | Não escolhida |
| Endpoint próprio para relações do catálogo | Reutilização fora da execução | Consulta adicional e conciliação com a versão já delegada | Alternativa viável |
| Relações oficiais na projeção da execução e na prévia sem experimento | Retorno e andamento compartilham a mesma identidade | Contrato e testes moderados | Escolhida |

## Matriz definida antes da implementação e dos testes

| Área | Critérios |
| --- | --- |
| Íris | Contexto privado íntegro pode planejar mensagem; falta de estratégia, produto ou gate bloqueia; composição comercial continua exigindo suas provas |
| Sequência | Tarefa, callback, confirmação do objetivo, subprocessos, integração e retorno; nenhuma tarefa encerrada artificialmente |
| Falhas | Motivo real preservado; retomada sem duplicar tarefa ativa; dependências e falhas dos filhos aparecem no pai |
| Isolamento | Produto, definição, cadeia, ciclo e referência; links não mudam de experimento; históricos preservados |
| Navegação | Pai identifica cada filho; filho retorna à atividade chamadora antes e depois de concluir; chamadas múltiplas explícitas |
| Interface e cópia | Loading, erros, dados ausentes, confirmação real da cópia, contexto e links completos |
| Integrações | Backend e executor locais; banco local e test doubles; callbacks reais nos contratos alterados |
| Observabilidade | Estado, contagens, tarefas, causa e evidências; dados de teste sem eventos comerciais, e-mail ou gasto |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; teclado, toque e ausência de overflow |
| Qualidade | Testes dos módulos alterados, TypeScript/build, contratos relevantes e revisão do diff |
| Publicação autorizada | Imagens pelos Dockerfiles do repositório, coordenação ACTIVE, saúde, identidade e conferência visual |

Após a última correção: duas rodadas locais completas e consecutivas sem falhas.
Resultados e limitações serão registrados ao executar; esta matriz não declara aprovação prévia.

## Diagnóstico local e limites comprovados

- A primeira compilação integral detectou um import ausente no teste novo; corrigido antes de publicar.
- A rodada diagnóstica seguinte passou 2.724 testes de backend (sete exclusões preexistentes),
  mas identificou no teste de ciclo uma suposição antiga: conclusão do filho e disparo seguinte
  no mesmo tick. A matriz passou a exigir primeiro a prova persistida e depois o novo disparo.
- A revalidação exigiu também confirmar o estado terminal do filho depois do callback do gate.
  O navegador de teste agora aguarda as requisições interceptadas antes de encerrar cada contexto.
- A revisão do provedor de alvo encontrou outro desvio causal: `creative-production-approval`
  não recebia o alvo do ciclo e caía na versão global histórica. O provedor passou a preservar
  a identidade privada também na produção e revisão dos criativos; dez testes locais passaram.
- Os 21 testes do worker passaram sem exclusão, incluindo a entrada privada produzida pelo
  backend local. O Dockerfile do worker também construiu sem erro.
- A revisão cruzada dos textos detectou uma instrução geral que ainda mandava registrar
  dependências futuras em `evidenceGaps`, contrariando a atividade e o validador de conclusão.
  O teste de contrato reproduziu a falha; a instrução agora usa `nextHandoff` e o teste passou.
  A rodada `vega-recovery-release-3`, já iniciada nessa descoberta, permanece diagnóstica;
  somente rodadas completas iniciadas após a correção contam para o aceite final.
- A navegação do simulador usa respostas HTTP locais independentes do contexto do navegador
  e conclui toda interceptação, inclusive quando a tela cancela uma consulta. O diagnóstico
  passou em desktop, iPhone e Pixel sem ocultar falhas de integração.
- A rodada `vega-recovery-release-4` aprovou backend, frontend, MySQL, reinício, navegação,
  Íris e os 96 cenários de cópia do processo. A regressão antiga de cópia da atividade
  ainda recusava a consulta de navegação sem referência; sua fixture passou a representar
  a prévia `UNAVAILABLE`, validando identidade e mantendo todos os comandos indisponíveis.
  O diagnóstico posterior passou os 60 cenários dessa regressão, com zero escritas.
  Portanto, essa rodada também não conta como aceite completo; a contagem reinicia na rodada 5.
- MCP confirmou apenas a execução automática #1, produto 4, processo 63, `experiment:92`.
  O plano comercial #3 pertence ao #91, e não existe vínculo desse plano com o #92.
  A prova visual #196 é da versão histórica MUSA v7 e não tem revisão independente registrada;
  ela não foi promovida a prova do ciclo privado atual.
- A homologação #395 contém cinco capturas da v12 (evidências #118–122). O conteúdo móvel
  #119 foi lido pelo endpoint oficial e seu SHA-256 corresponde ao registro persistido:
  `2d8034b8d8e32e0d5b63377c52cfdeab347641e1c5b12183e981cfa5b2acc12f`.
  A inspeção mostra ajuste aplicável, resultado salvo e continuidade simulada sem cobrança;
  isso comprova a superfície privada, sem convertê-la em validação humana ou comercial.
- O deploy consultado na conferência inicial, Actions #34674517265, terminou com sucesso na revisão `0a79bc8c`.
  Os checksums dos scripts APP e do Compose remoto são iguais aos arquivos do repositório.

## Publicação solicitada pelo usuário

Foram comparadas publicação via PR imediato, alteração direta de arquivos no host e entrega das
imagens validadas pelos scripts existentes. A terceira opção respeita a exceção explícita desta
solicitação e preserva rollback, configuração e proveniência. PR não foi solicitado nesta etapa.
Qualquer publicação requer duas rodadas finais aprovadas e coordenação ACTIVE dos escopos APP
e do workflow `communication-agent-worker-ci.yml`. A pausa só pode ser liberada após integração
da correção na main, conforme o cânone de retomada. Nenhuma campanha, cobrança ou prova humana
é autorizada por essa publicação técnica.

Durante a homologação, cinco arquivos de pesquisa foram integrados por outro fluxo à main
`b3d30f744bc925cac87bf796d4e4b3f192ed6d2a`. Foram sincronizados integralmente dessa revisão,
sem alteração de seu conteúdo, para preservar a biblioteca empacotada no backend. O manifesto
registra o commit de origem e os hashes; esses arquivos não são uma nova pesquisa desta tarefa.

A intervenção `f3221756d77344dd94a10c02816956fe` foi aberta nos escopos `app` e `temis`
(este último corresponde ao publicador de `communication-agent-worker`). A drenagem preserva
as execuções iniciadas e o run da main atual `34680679956`; somente itens antigos com zero jobs
podem ser retirados pelo comando canônico `discard-unstarted`. Esse registro descreve a preparação da intervenção.

## Aceite local da correção consolidada

As rodadas `vega-recovery-release-5` e `vega-recovery-release-6` terminaram completas e
consecutivas, sem falhas e sem alteração funcional entre elas. Cada uma aprovou:

- 2.725 testes de backend, com sete exclusões preexistentes e nenhuma nova exclusão;
- 622 testes de frontend, TypeScript e build; seis testes do executor de processos;
- 21 testes de Íris, incluindo entrada privada exportada pelos testes reais do backend;
- 18 cenários HTTP e seis cenários de ciclo com MySQL 5.7 real, além do reinício;
- navegação desktop/iPhone/Pixel com pausa, retomada, bloqueio, conclusão, filho e retorno;
- 96 cenários de cópia do processo e 60 de cópia da atividade, em HTTP/contexto seguro;
- imagem do executor, imagem de Íris, contratos de deploy/Liquibase e revisão do diff.

Os testes usaram agentes e integrações simulados, sem produzir métricas, mensagens, campanhas
ou tarefas de teste em produção. A captura #119 foi apenas lida, sem nova sessão do produto.
O pacote do backend foi conferido: classes novas, textos atuais de Íris no catálogo e os três
Markdown recentes de pesquisa estão presentes e idênticos às fontes.

O manifesto histórico `artifacts/vega-process-recovery/release-manifest-initial.json` identifica a entrega
`vega-processos-18e923ae93f1`, as três imagens e os hashes dos arquivos. Os logs de cada
rodada ficam em `artifacts/vega-process-recovery/`, `artifacts/process-automation/` e
`artifacts/process-context-copy/`. O run da main `34680679956` terminou com sucesso e
o coordenador confirmou `ACTIVE`, cinco publicadores protegidos e nenhuma execução na fila.
A conferência MCP anterior à troca não encontrou tarefa de Íris em andamento.


## Continuação: peça final e diagnóstico preservado

A retomada real concluiu a tarefa 402 (contrato de comunicação) e a 403 (produção não audiovisual). A tarefa 404 revelou incompatibilidade entre produção e revisão: 403 entregou `META_INSTAGRAM_STATIC_4_5_RENDER_BRIEF`, explicitamente ainda não renderizado; Psique pediu o arquivo final, mas o prompt mandava usar SINGLE_CREATIVE mesmo sem pixels e o validador recusou o parecer sem guardá-lo. A resposta original foi conferida na sessão do executor, via leitura SSH autorizada. Não existe imagem final produzida nessa tentativa.

Alternativas comparadas: (1) novo passo BPM de renderização — fronteira explícita, porém requer migração/versionamento e aumenta o número de handoffs; (2) criação manual da imagem na UI — reduz implementação, mas mantém a causa recorrente e intervenção humana; (3) materialização determinística dentro da produção de Íris — mantém uma entrega completa, preserva os pixels aprovados e custa apenas CPU local. Escolhida 3, com template versionado, upload governado, gate de derivação e revisão independente com anexos reais. O executor de geração por IA existente continua responsável por geração bitmap quando necessária; o novo compositor não gera provas, telas ou depoimentos.

A prévia local usa a captura técnica 118 da tarefa 395, cujo SHA-256 foi verificado, somente para homologação visual. Não é aprovação comercial. Os testes automatizados usam IDs 910xxx e HTTP/storage simulados, sem escrever dados de teste na produção.

Matriz ampliada antes da validação final: bytes PNG e dimensões; recorte exato/fora de limites; texto legível/overflow; hash divergente; arquivo ausente; origem/versão/produto distintos; upload indisponível; preservação da resposta bruta; retorno por ADJUST; revalidação após nova peça; captura anexada a Psique/Têmis; preview em desktop/iPhone/Pixel; aprovação humana e gastos permanecem fechados. As duas rodadas anteriores 5/6 validam a publicação inicial; a ampliação exige duas novas rodadas completas após o último ajuste.

A leitura do acompanhamento também foi reduzida a projeções funcionais e filtros SQL, evitando recarregar megabytes de prompts por consulta. A rodada diagnóstica 7 detectou duas falhas do mesmo teste mockado que ainda interceptava o método antigo do repositório; a fixture foi atualizada para o contrato SQL novo, sem relaxar a verificação de histórico/erro mostrado na tela.

A rodada diagnóstica 9 passou em backend, frontend, integrações, revisores e navegação, mas detectou que a matriz ampliada tentava montar a imagem de Psique sem preparar `review-evidence`. Os workflows oficiais já possuem esse passo. A homologação e a construção autorizada agora executam o mesmo gerador versionado `scripts/build-commercial-review-evidence.mjs`, com seus testes de contrato, antes de montar as imagens dos revisores. Nenhuma imagem dessa rodada foi publicada. A contagem das duas rodadas finais foi reiniciada.

A verificação visual foi ampliada para abrir o build real com o PNG renderizado dentro da auditoria de Íris. O teste `infra/testing/vega-process-recovery/creative-browser.cjs` confere desktop, iPhone 15 Pro e Pixel 7, decodificação de 1080 × 1350, hash, ausência de overflow, ausência de erro JavaScript e bloqueio de todas as escritas/conexões externas. A montagem inicial do teste precisou alinhar a definição da fixture e interceptar a URL canônica do backend antes de bloquear conexões; nenhuma mudança de código funcional foi necessária. A rodada 11 completa incluiu essa conferência e passou.

Antes da atualização de desempenho, os endpoints exatos usados pela UI responderam em 7,602 s para histórico sem prompts e 28,424 s para contexto do ciclo; a consulta curta da execução respondeu em 0,369 s. São medições pontuais, não uma promessa de latência. Evidências: `artifacts/vega-process-recovery/performance-ui-before.json` e `performance-before.json`.

## Aceite da produção e revisão com imagens reais

As rodadas `vega-recovery-release-11` e `vega-recovery-release-12` terminaram completas e consecutivas sem falhas, sobre as mesmas 52 fontes de runtime. Cada uma passou 2.735 testes de backend (sete exclusões preexistentes), 623 de frontend, 26 de Íris, 107 de Psique (uma exclusão preexistente), 92 de Têmis (uma exclusão preexistente) e seis do executor de processos. Também passaram os cenários MySQL/HTTP, reinício, concorrência, navegação, 96 cenários de cópia do processo, 60 de cópia de atividades, PNG na interface em três dispositivos, TypeScript/build, contratos e imagens Docker dos três agentes. Os agentes locais usaram test doubles; as revisões de IA reais pertencem à execução operacional posterior, com seus próprios resultados persistidos.

O manifesto atual identifica `vega-processos-de46b407adf0`, o conjunto de fontes validado e o pacote de evidências gerado pelo script versionado. A main consultada permaneceu em `b3d30f744bc925cac87bf796d4e4b3f192ed6d2a`. Na conferência anterior à aplicação, não havia tarefas em execução em Íris, Psique ou Têmis; três tarefas antigas de Íris permaneciam pendentes e foram preservadas.

## Aplicação técnica da versão com PNG

A versão `vega-processos-de46b407adf0` foi aplicada a backend, frontend, Íris, Psique e Têmis pelo coordenador ativo, após as duas rodadas finais. Todas as imagens foram verificadas por digest do config e camadas RootFS no host; os serviços ficaram saudáveis. As configurações e os mounts das sessões foram preservados. O serviço separado `iris-image-studio` permaneceu intacto. Os cinco publicadores continuam protegidos até integração na main.

Na medição pontual após a aplicação, o contexto do ciclo respondeu em 16,027 s (antes 28,424 s); o histórico sem prompts respondeu em 8,247 s (antes 7,602 s). Houve melhora no contexto, sem ganho demonstrado no histórico. Não se trata de benchmark de carga ou garantia de latência. A continuidade real e seus resultados serão registrados a seguir.

## Correção da conferência de empacotamento do backend

A afirmação de correspondência de código do backend na aplicação `de46b407adf0` foi invalidada: a imagem transportou corretamente os bytes locais, mas o JAR local ainda era o pacote anterior. Frontend e três agentes foram construídos a partir das fontes novas; o backend permaneceu funcionalmente na revisão anterior. Por isso, as medições de latência acima comparam a mesma revisão e não demonstram efeito da otimização.

O Dockerfile incorpora um JAR pronto e a preparação local não repetiu package depois da ampliação. A investigação reproduziu localmente a ausência das classes novas e o verificador passou a recusar esse pacote antes da imagem. A retomada da tela criou #405, bloqueada por falta da imagem final; seu histórico foi preservado. Não houve produção de peça nem aprovação de parecer nessa tentativa.

Foram adicionados empacotamento à matriz, comparação integral das classes testadas com o JAR e inspeção do JAR real nas camadas Docker. Os contratos reproduzem JAR anterior, classe ausente/extra/divergente e imagem sem JAR ou que o substitui/remove em camada posterior. A pré-validação conferiu 3.929 classes, 401 recursos externos e 199 cartões do catálogo, além do JAR incorporado à imagem temporária. A contagem de aceite reinicia nas rodadas 13/14; as rodadas 11/12 continuam como evidência dos testes funcionais, sem satisfazer a nova conferência completa de empacotamento.

## Aceite final com correspondência do pacote

As rodadas `vega-recovery-release-13` e `vega-recovery-release-14` terminaram completas e consecutivas, sem falhas. Mantiveram as contagens funcionais das rodadas 11/12 e incluíram empacotamento, nove contratos de recursos/classes, cinco contratos da imagem, catálogo inicializado a partir do JAR e conferência dos bytes efetivamente incorporados ao Docker. Os testes completos de Spring também inicializam o grafo real de dependências, com H2 local. As 52 fontes de runtime permaneceram idênticas durante as duas rodadas.

O manifesto congelou a entrega `vega-processos-0b060f35e0ed`, com JAR SHA-256 `3dc44524c2591c138fd006a6b0550f47c758840d693cf7cb9748ba661adfd8b2`. A construção reutiliza exatamente esse pacote aprovado; recusa qualquer alteração de bytes. Íris, Psique e Têmis preservam as imagens corretas `de46b407adf0`; a correção de empacotamento não exige reenviar seus executores.

Os contratos de navegação e das entradas visuais também foram atualizados em `docs/swagger/process-automation-v1-swagger.yaml` e `docs/swagger/agent-tasks-v1-swagger.yaml`, com YAML e referências internas conferidos localmente. Essa atualização documental não altera o runtime homologado.

## Consulta de histórico sem corte temporal

A entrega `0b060f35e0ed` foi aplicada pelo coordenador e o SHA-256 do JAR foi conferido dentro do container em execução. A saúde canônica respondeu UP e o novo endpoint de entradas visuais passou a recusar corretamente uma tarefa não reservada, com HTTP 409. A consulta funcional da tela, porém, manteve a produção 403 como concluída e Psique em espera por uma imagem.

MCP confirmou as três tarefas da referência. A consulta usada pelos provedores exigia `created_at >= NULL` quando não havia corte de data, retornando histórico vazio. A regressão local reproduziu duas falhas com JPA real, incluindo o próprio provedor de retorno à produção; após a correção, os quatro testes de persistência e os testes dos dois serviços criativos passaram.

Alternativas: (1) informar uma data artificial em cada consumidor, baixo esforço mas contrato implícito e recorrência provável; (2) criar outra consulta exclusiva sem data, contrato explícito mas duplicação de projeção e consumidores; (3) aplicar o filtro temporal somente quando informado, mantendo definição e referência obrigatórias, consistente com a consulta já existente por código de processo. Escolhida a terceira, com testes usando o repositório real para eliminar a divergência dos mocks.

O subprocesso foi pausado pela UI, com um único POST de pausa autorizado, sem tarefa de agente em curso e sem criar nova tentativa. O link de retorno ao pai e a cópia do contexto funcionaram na conferência. As rodadas 15/16 passam a ser o aceite da correção consolidada; nenhuma dessas rodadas é declarada aprovada antecipadamente.

Durante essa rodada, a main recebeu `14900978a929900abdef7413780b164ab51ae29a`, contendo exclusivamente uma nova pesquisa de agentes inteligentes. Ela permanece preservada na main e fora desta correção do runtime já congelado; nenhum arquivo dessa atualização foi removido ou sobrescrito. A retomada posterior dos publicadores continua exigindo a integração das correções na main.

As rodadas 15/16 terminaram completas e consecutivas sem falhas, com 2.736 testes de backend em cada uma, mantendo as sete exclusões anteriores; as demais contagens e verificações completas permaneceram iguais às rodadas 13/14. O manifesto congelou vega-processos-c79453d6f337, com JAR SHA-256 cdfc29c320ce84d8df231b1ed89d6faa1db3b4d6da9e5fe557bfa25ca3bf14e0. A construção e a aplicação devem usar exatamente esse pacote aprovado. A versão anterior 0b foi preservada em release-manifest-before-null-since.json com o problema funcional identificado, sem invalidar sua conferência de empacotamento.

## Resultado da execução real e decisão pendente

A versão `vega-processos-c79453d6f337` foi aplicada ao APP pelo coordenador em
12/09/2026, concluindo às 12:03 UTC. O SHA-256 do JAR foi novamente conferido dentro
do container em execução e a saúde canônica respondeu UP. Os três agentes preservam
as imagens corretas `vega-processos-de46b407adf0`. Após a atualização, a consulta real
reabriu a produção que tinha somente briefing. A retomada foi feita pela interface,
uma vez, e o backend encadeou produção e pareceres automaticamente.

| Entrega do ciclo #2 / experimento #92 | Execução real | Resultado persistido |
| --- | --- | --- |
| Contrato de comunicação | Íris #402 | Concluído |
| Peça estática final | Íris #406 | Concluída, PNG #126 |
| Avaliação do cliente simulado | Psique #407 | APPROVED, sem alterações obrigatórias |
| Revisão de integridade comercial | Têmis #408 | APPROVED, restrita à avaliação privada |
| Decisão humana de uso | Atividade `human`, definição #646 | Aguardando decisão; não executada pelo modelo |

O PNG #126 tem 1080 × 1350 e 144.632 bytes, com SHA-256
`b5c09c56a4eb22306ca429341e879319ae83aed74f3aa129b69fbca122fabb19`.
Foi baixado pelo endpoint oficial, inspecionado visualmente e comparado com o registro
persistido. O mesmo hash está nos dois pareceres e nos logs MCP de recebimento dos
revisores. A origem é a captura #118 da homologação #395, versão MUSA v12, com recorte
declarado e sem redesenhar a prova do produto. O endpoint servido pelo frontend também
retorna os mesmos bytes PNG, com HTTP 200 e `Content-Type: image/png`.

A peça apresenta uma microação aplicável com itens já disponíveis, ocasião,
autoavaliação, resultado salvo e CTA privado. Sua identificação de demonstração sintética
e ausência de compra/cobrança foram preservadas. Os pareceres são avaliações de agentes;
não constituem validação humana, resultado de campanha ou evidência de conversão.

A leitura MCP às 12:33 UTC confirmou: execução pai #1, processo #63 v7, em
`WAITING_SUBPROCESS`, uma atividade concluída e três restantes; execução filha #2,
processo #64 v8, em `WAITING_HUMAN`, quatro concluídas, uma restante e uma audiovisual
não aplicável ao formato planejado. O processo de landing #65 v6 ainda não foi iniciado.
As versões dos processos são independentes e os links preservam suas definições reais.
As tentativas antigas bloqueadas permanecem no histórico; nenhuma foi convertida em sucesso.

Foi solicitada ao usuário uma decisão concreta sobre a peça já revisada: uso apenas na
validação privada, preparação da fase comercial sem publicação ou ajustes na peça.
Não houve resposta até esta conferência. Por isso, não foi registrado consentimento humano,
nem concluído artificialmente o subprocesso ou o processo pai. O experimento #92 permanece
PLANNED, sem campanha iniciada, cobrança ou envio externo. O plano #3 e a prova #196,
pertencentes ao experimento #91, não foram usados como aprovação do ciclo atual.

Evidências locais: `artifacts/vega-process-recovery/actual-406/`, `actual-407/`,
`actual-408/`, `runtime/`, `release-manifest.json` e `running-backend-package.json`.
Os JSON de resultado bruto, o PNG e os registros de execução foram preservados nesses
diretórios. O conteúdo operacional pode ser consultado na tela do subprocesso:
`/products/4/value-chain-history/processes/64/activities?learningCycleId=2&chainId=14#activity-human`.

## Navegação publicada e encerramento técnico

A conferência publicada em desktop, iPhone 15 Pro e Pixel 7 abriu o subprocesso pelo
link do painel pai, copiou o contexto de ambos e voltou ao pai pelo link do filho.
Produto #4, cadeia #14 e ciclo #2 foram preservados em todos os percursos, sem escrita,
erro JavaScript ou overflow horizontal. Os painéis identificam os subprocessos e suas
versões; os testes locais também cobrem o retorno após conclusão do filho. A execução
real permanece aguardando a decisão acima, portanto não foi declarada uma conclusão
publicada que ainda não aconteceu.

As evidências dessa navegação ficam nos diretórios
`artifacts/vega-process-recovery/public-read-{desktop,iphone,pixel}-63-*`, incluindo
contexto copiado, respostas do backend, capturas e URLs percorridas.

A imagem final da tarefa #406 também foi aberta dentro da interface publicada em cada
um dos três dispositivos. O navegador decodificou o PNG de 1080 × 1350 pela rota real
do frontend, sem erro JavaScript, escrita ou overflow. Capturas e resultados estão em
`artifacts/vega-process-recovery/public-creative-406/`. O seletor inicial do script de
conferência alcançava também os resumos de JSON internos; foi delimitado ao cabeçalho
direto da tarefa, sem alteração da aplicação. A verificação final passou nos três casos.

A topologia temporária da homologação foi removida com `down --volumes --remove-orphans`
usando exclusivamente o projeto Compose `aihub-61305900-a7f7-4f32-8e38-a86180060461-1597a0b836`.
Nenhum commit, push ou PR foi feito. Os cinco publicadores da intervenção
`f3221756d77344dd94a10c02816956fe` continuam pausados e sem execuções pendentes para
preservar a correção aplicada. Sua retomada exige integrar esta correção na main,
conforme o cânone; o encerramento da sandbox não libera essa proteção.
