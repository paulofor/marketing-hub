# Vega — retomada privada do Estúdio em 05/11/2026

## Contexto conferido

Produto 4, projeto 14, perfil 69, cadeia 27, ciclo comercial 21 e experimento 114.
O aceite continua sendo um MP4 privado de 32 segundos/oito cortes, com voz,
legendas e revisões independentes do mesmo arquivo. Storyboard não é vídeo.
O recibo 128 representa US$ 15 novos totais; não autoriza o histórico, outro
vídeo, recarga, campanha ou publicação comercial.

Às 20:33 UTC, o MCP confirmou backend no SHA
`01720b5c789b1ceb16f75e9fd429d25da5c57d54`, do PR 5792. UI e banco confirmaram
perfil DRAFT, nenhum job para 69 e ciclos 42–46 exclusivamente de preflight.
O PR 5793 permanece draft, sem revisão e com conflito no cânone; sua base e
alterações independentes devem ser preservadas. Nenhuma geração paga ocorre
nos testes desta entrega.

## Escolhas de implementação

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Consultar o ciclo no navegador e enviar seu contrato ao endpoint existente | Reutiliza o render e seus gates | Mais leituras, acoplamento financeiro na UI e possibilidade de estado desatualizado | Não adotada |
| Usar um comando de projeto com contexto persistido no servidor | Reutiliza decisões, reserva e job com lock e menor alteração | Exige regressões de UI/backend | Adotada |
| Encadear automaticamente a produção no aceite do processo 4.2 | Reduz a operação manual futura | Amplia a mudança de BPM e exige homologação de todos os gates e responsáveis | Preservada para demanda própria |

Para recuperação foram comparadas três opções: consultar somente a task e baixar
seu resultado ainda disponível (menor persistência, mas dependente da validade da
URL); preservar imediatamente os bytes no storage existente e seu recibo/hash
(adotada, resistente ao reinício e à expiração); ou separar toda a montagem em
um novo tipo de job referenciando os clipes (isolamento maior, mas mudança mais
ampla no pipeline). Repetir geração não é recuperação. Timeout de envio sem ID
conhecido permanece incerto e bloqueia novo POST; timeout de polling consulta a
mesma task.

## Matriz definida antes da execução

| Área | Critério de aceite local |
| --- | --- |
| UI e backend | Comando Runway usa ciclo canônico; contexto elegível cria/reutiliza um único job; outro tenant/projeto/produto/perfil é recusado |
| Bootstrap financeiro | Registrar as sete parcelas e fontes pela UI, reavaliar sem resetar teto ou criar outro ciclo; ausência, vencimento e divergência bloqueiam |
| Teto compartilhado | Recibo, custos, pendências e reservas são consultados no servidor antes do consumo; não presumir que metadata declarada implementa limite |
| Recuperação | Falha/reinício depois dos clipes reutiliza IDs/bytes/hash sem POST pago; timeout com resultado incerto preserva reservas e exige conciliação |
| Mídia | Montagem sintética preserva 32s e streams; nenhuma alegação de qualidade premium baseada em fixture |
| Regressões | Caso original, outro produto/contexto e caminho antes válido; repetição e concorrência sem duplicação |
| Observabilidade | IDs, decisões, erros, custos e recibos persistidos; fonte desconhecida não vira zero |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 com doubles locais, sem mídia paga |
| Entrega | Diff e testes locais antes de commit; PR 5793 reutilizado; revisão independente real, checks do HEAD e deploys/saúde comprovados antes da operação |

## Aprendizado e limites

A lacuna de harness observada é a validação do planner isolado sem exercitar o
payload do botão real, o bootstrap financeiro e a perda de estado entre render
e montagem. As regressões desta entrega precisam comprovar esses percursos.
Métrica verificável: ausência de POST pago duplicado e preservação de contexto,
arquivo e teto. Ganho comercial ainda não foi medido.

## Resultado local

O botão real passou a chamar o comando do projeto sem inventar metadata
financeira. O servidor valida tenant, produto, experimento, perfil e decisão
persistida e reutiliza o mesmo job sob lock. Os testes cobrem o contexto original,
outra identidade e caminhos anteriormente válidos. O formulário de custos registra
as sete parcelas e suas evidências antes de Plutus, no mesmo ciclo e teto.

O teste de recuperação usa quatro tomadas válidas de 10/10/10/2 segundos para
montar 32 segundos; tomadas e oito cortes editoriais são contratos diferentes.
Uma falha real de FFmpeg ocorre depois dos uploads. Outra instância remonta os
mesmos bytes por task, asset, URL e SHA-256: continuam exatamente quatro POSTs,
quatro polls e quatro uploads. Hash ou contexto divergente é recusado. O teste
é parametrizado com jobs diferentes e não avalia qualidade premium da candidata.
Outra regressão comprova que o processor reutiliza o plano do backend sem chamar
novamente a IA. Timeout HTTP 408 permanece incerto após reinício e não repete
POST; timeout de polling consulta a task aceita e conclui sem nova submissão.

A revisão do replay identificou que um novo aviso de aceite podia retirar uma
cobrança já liquidada do ledger. O registrador compartilhado agora é idempotente:
preserva data, contexto e custo da primeira task e recusa outro contexto ou valor.
Clipes já preservados não repetem eventos financeiros. As regressões cobrem dois
jobs, repetição e divergência sem alterar a conciliação original.

A suíte ampla de backend terminou com 5.873 casos registrados, zero falhas/erros
e 66 casos condicionais ignorados. O teste específico de migração/aplicação/
reaplicação do contrato foi executado separadamente com MySQL 5.7 real e passou;
a topologia temporária foi removida. No executor, os relatórios combinados somam
318 casos, zero falhas/erros e quatro testes condicionais dependentes de mídias
externas ignorados; os cenários locais opcionais de FFmpeg foram executados.
No frontend passaram 78 testes focados, verificação de tipos e build. O navegador
passou seis percursos na UI real/controller real em desktop, iPhone 15 Pro e Pixel 7,
com persistência e executores explicitamente simulados. Não houve geração paga.

Uma primeira invocação ampla de Maven usou execução no mesmo processo a partir
da raiz e excedeu a memória do cgroup, além de alterar a base dos caminhos relativos
das fixtures. A execução complementar preservou relatórios válidos, usou o fork
normal no diretório do módulo e cache de quatro contextos Spring. A única falha
funcional encontrada foi uma dependência direta entre módulos: ela foi corrigida
por uma porta obrigatória de política financeira do domínio, sem ampliar a regra
ArchUnit. A regressão arquitetural passou com 93 regras. A nova regressão do
formulário financeiro foi incluída no job existente de CI do Estúdio; os três
workflows alterados passaram no Actionlint.

O pacote real de evidências comerciais e seus testes passaram. Uma sucessora v77
reconcilia a base v75/main e v76/PR sem alterar atestações antigas; o resolvedor
continua indicando nenhuma republicação do frontend PDE. Scripts shell alterados
ou investigados passaram em `bash -n` e ShellCheck. O diff não tem erros de espaços;
classes e métodos Java alterados têm comentários de responsabilidade em português.

## Estado do limite no HEAD 8303130 — histórico superado pela continuação abaixo

O recibo 128 continua válido como autorização de US$ 15 novos totais. Entretanto,
`FinancialCodexRunner.reviewVideoCycle` executa o subprocesso Codex e lê o uso de
tokens depois que ele termina; `AgentTaskService` também aplica o custo depois
da resposta. O wrapper OAuth protege sessão, não dólares. A reserva integral
do Estúdio não limita essas inferências nem as revisões futuras. Não foi encontrada
prova de um limite prévio compartilhado em todos os executores envolvidos.

Por isso, o backend persiste `SINGLE_VIDEO_SHARED_LIMIT_NOT_ENFORCED` e impede
iniciar Plutus ou o render direto para recibos desse escopo. Essa contenção foi
testada com identidades diferentes e não depende de configuração ou metadata
enviada pelo navegador. Ela não equivale a implementar o executor limitado:
o caminho pago e o aceite final de um vídeo real continuam pendentes.

Não há prova de insuficiência de saldo da conta inteira. US$ 3,84/4,80 do preflight
46 são um snapshot parcial vencido. Nenhum zero sintético ou custo histórico foi
tratado como saldo disponível do novo vídeo. Em nova leitura MCP, perfil 69 segue
DRAFT e ciclos 42–46 seguem sem tarefa ou job. Nenhuma inferência, geração ou
revisão paga foi disparada nesta continuação.

Também não há revisão independente do PR 5793: a identidade autenticada é a autora
`paulofor`, única colaboradora listada no repositório. Não é permitido usar sua
própria aprovação como revisão independente. O merge, deploy e qualquer produção
dependente permanecem sem execução enquanto não houver revisão válida e controle
de consumo efetivamente comprovado. Não prometer retomada automática sem mecanismo
ativo e elegível. Adotar as correções técnicas somente após os checks e essa
revisão; não declarar prontidão premium, venda ou ganho comercial a partir dos testes.

## Ajuste após o primeiro CI

O frontend PDE falhou por vencimento da fixture documental compartilhada às 21:13 UTC,
depois de uma execução anterior bem-sucedida às 18:48. As fontes oficiais foram
reconsultadas; uma fixture nova e a atestação sucessora v78 preservam o histórico e
o gate de frescor, acrescentando diagnóstico de URL/prazo e cinco regressões de
integridade/datas. A matriz local real de 18 percursos, histórico e cenários passou
com MySQL 5.7 e zero provedores externos. O construtor passou com 798 arquivos/207
manifestos, e o resolvedor segue sem publicação PDE. Ver o
[registro específico](mira-fixture-documental-retomada-2026-11-05.md).
Esse ajuste de CI não comprova o teto integral nem libera gerar o vídeo de Vega.

## Continuação da conferência — 06/11/2026

Aceite preservado: **um** MP4 privado de 32 s/oito cortes no projeto 14/perfil 69,
produto 4, cadeia 27/ciclo comercial 21/experimento 114; recibo 128 de US$15 novos
totais. Teste de software, storyboard, gravação de fonte e montagem sintética não
são o vídeo premium pedido. Nenhuma aprovação de qualidade ou venda é inferida.

A conferência reabriu a preparação local porque o HEAD 8303130 ainda bloqueava
corretamente inferências sem limite compartilhado, mas não implementava o transporte
finito. O próximo HEAD deve conter a correção completa e a documentação deste
comportamento antes de solicitar revisão independente. Main/runtime 01720b5c já
contém PR5792; não republicar esse lote.

Matriz da rodada local, definida antes da validação completa desta continuação:

| Área | Aceite determinístico | Evidência prevista |
|---|---|---|
| Autorização | Mesmo recibo/contexto; outro produto, versão, tenant, STOP ou autorização vencida não inicia consumo | SingleVideoSpendingTest, VideoProductRecordingServiceTest |
| Concorrência e história | Lock comercial único, três executores não ultrapassam US$15, incerteza retida, MySQL5.7 reapply/ENUM/JPA/BLOB sem perder histórico | SingleVideoSpendMysql57Test |
| Íris e entrada DRAFT | Briefing privado do pedido não fabrica estratégia de Atena; uma tarefa por ciclo; FALA_FINAL literal; PRIVATE_READY sem approvedBy | SingleVideoIrisInputTest, PrivateStudioVideoInputTest, VideoPrivateBriefPreparationTest, VideoProductionCycleServiceTest |
| Previsão | Frontend registra sete grupos/fontes, reavalia o mesmo ciclo e preserva teto; reserva de clipes entra no ledger comum sem dupla contagem | VideoProviderFinancialPreflightServiceTest, SingleVideoSpendingTest, continuation-browser.cjs |
| Transporte de agentes | Corpo imutável, Flex, sem tools, max_output_tokens efetivo, reserva/begin antes do POST; callback perdido é replay sem inferência | single-video-bounded-agent.test.mjs e testes dos quatro workers |
| Recuperação Apolo | IDs/respostas/bytes/hashes duráveis; falha de montagem, perda de callback e timeout incerto nunca criam outro POST pago | BoundedVideoSpendingTest, RunwayAcceptedTaskReplayTest, RunwayVideoProviderTest |
| Prova funcional | Software real local, dados sintéticos e uma operação; entrada/resultado/salvar/reabrir datados; upload privado pelo front sem fila paga; codec/hash/duração verificados antes de gerar | capture-vega-functional-recording.mjs, ProductRecordingUpload.test.tsx, FunctionalRecordingOverlayTest |
| Dispositivos | Página real em desktop, iPhone15Pro e Pixel7 com UI/backend locais; recusas de tenant e reenvio idempotente | continuation-browser.cjs |
| Mídia | Montagem FFmpeg de 32s/oito cortes, locução limitada sem fallback, trilha original sem API, preservação de áudio/legendas e relógio real da prova | RunwayMontageDurationTest, OriginalMusicBedTest, FunctionalRecordingOverlayTest, suíte de pós-produção |
| Pareceres | Psique/Têmis recebem o mesmo hash do MP4; Têmis anexa frames reais datados sem chave do produtor; storyboard não supre material | CreativeFinalVideoEvidenceTest, CreativeReviewImagesTest, video-review-inputs.test.mjs, TemisContainerIsolationContractTest |
| Observabilidade e dados | Requests, contagem, máximo retido, respostas brutas, tiers observados, custos pendentes e revisão separados; nada disso entra como venda | ledger/relatório do ciclo, SingleVideoSpendingSummary.test.tsx |

Alternativas da prova temporal: PNG preserva estado, mas não mostra o gesto;
reabrir sessão histórica encerrada quebraria a autoridade; gravar o software real
na sandbox com dados sintéticos permite demonstrar o funcionamento sem migrar história.
Adotada a terceira opção. O registro privado limitado no banco existente evita disparar
a fila genérica de análise e não exige contratar storage ou uma nova infraestrutura.
A montagem preserva a velocidade dos gestos e informa LOCAL_SANDBOX/dados de teste.
O roteiro e a produção mantêm a versão audiovisual alvo; a origem filmada continua
identificada como a experiência v17 herdada, sem ativar outra versão do produto.

Para o teto compartilhado foram comparados: contabilizar OAuth depois da execução
(não limita consumo), usar só timeout (não limita tokens nem ferramentas) e admitir
requests finitos com reserva transacional anterior ao envio. Adotada a terceira opção,
com custo de implementação maior e comportamento comprovável por HTTP local/concorrência.
Ela mantém o máximo retido após incerteza; não declara custo faturado nem promete que
US$15 bastarão para todas as etapas. A cobertura integral e as credenciais ainda devem
ser revalidadas no contexto real antes de qualquer despesa.


### Resultado da preparação local desta conferência

O transporte limitado foi implementado nos quatro workers de agentes e em Apolo:
contagem do corpo real, reserva e admissão transacionais precedem um único POST;
resposta bruta, pedido, IDs, hashes e estado incerto sobrevivem ao reinício. O mesmo
lock do ciclo e recibo cobre inferência, áudio, clipes e voz. Resposta conhecida
não vira custo faturado nem libera automaticamente o máximo retido. Executores
antigos, STOP, contexto divergente e resultado incerto permanecem sem novo envio.

A conferência do vínculo observou que resolver apenas o experimento admitiria uma
atividade alheia ao único vídeo autorizado. A regra agora exige a tarefa de Íris
registrada pelo ciclo, o gate real de Plutus registrado naquele pedido ou a revisão
criativa da mídia READY do render fixado no recibo (incluindo sua pós-produção).
Áudio de outro asset é recusado. O teste usa o contrato persistido `GATE_DECISION`
com `gateStatus=PENDING`; o nome hipotético `GATE` não era compatível com o backend.
As regressões cobrem vínculo elegível, outro render/tenant/asset, tarefa alheia no
mesmo experimento e caminho legado sem recibo. Essa alteração limita o gasto e
não concede ao agente autoridade para aprovar sua própria entrega.

Para oito cortes foram comparadas: oito chamadas de quatro segundos ao provedor
(mais chamadas e necessidade de outra cotação/plano); quatro tomadas válidas com
oito reenquadramentos editoriais determinísticos (escolhida, sem POST adicional);
outra distribuição de fontes cinematográficas e funcionais (exigiria replanejar
o contrato congelado). O reenquadramento usa os mesmos pixels, sem alegar oito
ângulos gerados. A primeira tentativa de split/concat produziu zero mudanças nas
fronteiras do teste estático; o filtro foi corrigido para o relógio dos frames.
A regressão decodifica pixels e prova sete mudanças entre oito partes em 32 s,
com áudio e legendas preservados. A prova funcional fica entre 16 e 28 s, em
velocidade real; não ocupa a abertura cinematográfica nem o CTA final.

A captura do software real local mostrou entrada, resultado, salvar e reabrir,
com backend/MySQL5.7 e uma única operação sintética. MP4 de origem: 10,92 s,
540x1170/H.264/sem áudio, SHA-256
`c56b5459170d4707e244d78044da20096ab0ae32fc68ffdb74d4858978642993`.
As duas fontes funcionais filmadas coincidem byte a byte com o frontend v17
publicado e o service na main 01720b5c. A gravação continua identificada como
LOCAL_SANDBOX/AGENT_VALIDATION, sem prova comercial ou renovação de parecer antigo.
O frontend administrativo enviou os bytes reais e o manifesto pelo controller,
com leitura pelo mesmo SHA e recusa de outro tenant nos três dispositivos.
Foi corrigido o harness de navegador: multipart passa pelo proxy HTTP nativo do
Vite, porque `route.fetch` naquele ambiente perdia os bytes do arquivo.

A composição local completa passou: 32 s, 1080x1920, H.264 e AAC, oito cortes,
prova funcional, trilha PCM original sem API e legendas. FFmpeg decodificou o
arquivo integral sem erro; Chromium desktop e iPhone reproduziram e buscaram
posição com controles nativos, duração correta e nenhum erro. A voz desse teste
é um sinal de 440 Hz, identificado como teste; não comprova pronúncia nem qualidade
premium. A rota real mantém voz HD explícita limitada por caracteres, sem fallback
silencioso; Psique deve ouvir o MP4 real e Têmis deve revisar os mesmos bytes.

Relatórios locais combinados após os ajustes: backend 5.901 casos, vídeo 326,
Íris 158, Plutus 90, Psique 257 e Têmis 138; zero falhas/erros. Casos condicionais
ignorados: 67, 6, 5, 0, 13 e 1, respectivamente; MySQL5.7 e os cenários reais locais
de FFmpeg foram executados separadamente. A suíte de frontend de CI passou 336
casos em 31 arquivos, typecheck e build. Passaram 11 testes HTTP do transporte
nativo, um teste de frames reais de Têmis, três dispositivos no fluxo real local,
93 regras de arquitetura do backend e a regressão final de 17 casos financeiros.
Scripts passaram em `bash -n`, ShellCheck e Actionlint na versão fixada do repositório;
Node da imagem passou no healthcheck. O pacote comercial passou com 799 arquivos/
208 manifestos; a sucessora Mira v79 preserva atestações anteriores e continua sem
republicação PDE. Nenhum teste chamou inferência, voz ou geração paga.

Oportunidade reutilizável: evitar cobrança duplicada, consumo fora do pedido e
aprovação visual sobre storyboard. Hipótese comercial: reduzir espera e desperdício
para chegar a uma peça útil com revisões dos pixels/áudio reais. Comportamentos
locais comprovados: zero POST extra na recuperação, máximo compartilhado retido,
recusa de tarefa/asset indevidos e revisão vinculada ao SHA. Aumento de vendas e
qualidade premium do vídeo final permanecem desconhecidos. Adotar somente após
revisão independente, checks atuais, deploy e conferência do contexto real; caso
falhe um gate, preservar a pausa e a auditoria, sem mudar o teto nem repetir geração.

### Limites externos conferidos, sem produção

A credencial efetivamente montada em Apolo respondeu HTTP 200 na consulta de
modelos; o antigo teste de outro arquivo com HTTP 401 não comprova bloqueio do
módulo nem da conta. Psique também tem uma credencial efetiva válida. Em Têmis,
o arquivo dedicado previsto para o transporte limitado está ausente; a chave do
produtor não foi compartilhada com a revisora. Consultar modelos não gera mídia,
não comprova saldo nem aprova um modelo para este produto. A configuração segura
dessa credencial de revisão continua necessária antes de sua inferência.

GitHub foi consultado novamente nesta conferência: PR5793 aberto, autora e
identidade autenticada `paulofor`, sem revisão independente. A proteção explícita
do pedido permanece mesmo quando a branch não exige revisão por configuração.
Não realizar autoaprovação, bypass, merge ou deploy enquanto esse requisito não
for atendido. Main/runtime 01720b5c já integrado não deve ser republicado. O estado
atual continua perfil69 DRAFT, zero jobs e nenhum MP4 premium desta solicitação;
nenhuma tarefa ou despesa paga foi iniciada na preparação. Recibo128, histórico,
custos pendentes e reservas anteriores foram preservados. Cotação/cobertura
integral do pedido real ainda serão reavaliadas pelo fluxo protegido após a
publicação autorizada; o snapshot vencido 3,84/4,80 não foi tratado como saldo.
