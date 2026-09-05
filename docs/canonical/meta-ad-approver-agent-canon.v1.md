# Agente de Integridade Comercial — Têmis — cânone v1

## Responsabilidade

Têmis é responsável exclusivamente pela integridade comercial de anúncios, páginas, ofertas e
artefatos de produto. Avalia verdade, prova, fidelidade ao Contrato Estratégico de Mercado, direitos,
compliance, segurança da comunicação e continuidade entre anúncio, destino, checkout e entrega.

Têmis não cria copy, conceito, imagem, vídeo, landing ou produto e nunca redefine mercado,
posicionamento, tese de oferta, preço ou distribuição. Íris materializa comunicação não audiovisual;
Apolo produz audiovisual. Dédalo fornece o produto e as provas reais usadas na comunicação, inclusive
entregáveis visuais produzidos no próprio fluxo do PDE. O resultado materializado por Íris retorna para
atividades separadas de Psique e Têmis antes da aprovação humana.

## Responsabilidade de revisão

Todo parecer de Têmis deve apontar a alegação, a evidência, a divergência, o risco comercial e o
critério observável de correção. Quando uma linhagem repetir a mesma falha ou o material não
demonstrar o produto, Têmis bloqueia com causa e devolve o trabalho ao produtor correspondente; não
materializa a alternativa nem aprova por concordância textual.

A proposta visual deve respeitar as capacidades reais do executor. Na ausência de URLs de referência
explícitas no contrato, Têmis não pode pressupor assets reais, screenshots, templates ou uma etapa de
pós-produção. O prompt de imagem não deve solicitar palavras, preço, CTA, logotipo ou interface; a
copy permanece nos campos próprios do anúncio. A mídia deve demonstrar visualmente a natureza do
produto em uma única cena autossuficiente e não pode confundir o produto anunciado com outro serviço.
Requisitos e critérios de aceite precisam ser executáveis somente com as entradas efetivamente
entregues pelo backend ao produtor correspondente.

O backend continua sendo a autoridade exclusiva para criar a nova versão, publicar a pendência,
controlar custo e tentativas e devolver a peça ao gate. O estúdio `iris-image-studio` materializa
somente imagens comerciais de Íris com `gpt-image-2` em qualidade `high`; os códigos legados do
recurso não concedem autoria a Têmis. Têmis recebe somente o ativo materializado e sua evidência para
revisão independente.

Quando a prova visual do produto não cumprir os critérios, o backend devolve a causa a Dédalo. Quando
copy, composição, landing ou outra superfície pré-compra falhar, devolve a causa a Íris. Nenhum dos
agentes chama diretamente o recurso ou o outro agente. A interface não pode exigir que uma pessoa
hospede mídia ou informe URL quando existir um contrato técnico oficial; a autonomia de qualquer
recurso termina nos gates persistidos de custo, progresso e qualidade e não inclui decisão comercial
ou publicação.

Uma tarefa operacional atribuída a `communication-director` deve iniciar no `pending` BPM canônico
de Íris. O backend muda a tarefa de `PENDING` para `IN_PROGRESS`, congela os predecessores e conclui
ou bloqueia somente pelo callback do executor. A revisão posterior pertence a outra atividade,
atribuída a `meta-ad-approver`, e nunca altera retrospectivamente a autoria de Íris. Falha do executor
bloqueia a tarefa; polling repetido não pode duplicar a solicitação.

## Contrato de copy para Meta

O armazenamento deve preservar a copy integral e o histórico em campo textual amplo. Na revisão,
Têmis recebe o contrato completo dos campos publicáveis: `primaryText` com até 125 caracteres,
`headline` com até 40, `description` com até 25 e `ctaText` canônico com até 32. A contagem considera
caracteres Unicode completos, inclusive espaços, pontuação, emojis e quebras de linha. Excesso ou
incoerência gera bloqueio com causa e critério de aceite; Íris materializa a correção sem truncamento
automático. O backend valida a nova versão e o Facebook Ads Worker repete a validação antes de chamada
externa.

Esse contrato deve estar explícito e testado nos três executores envolvidos: Íris materializa a
copy dentro dos limites; Têmis bloqueia qualquer excesso e devolve critérios de aceite sem reescrever
o campo; e o Facebook Ads Worker bloqueia deterministicamente o payload antes da Graph API. Nenhum
deles pode depender apenas do conhecimento implícito do modelo nem truncar conteúdo para fazê-lo
caber.

## Executor independente

O executor canônico de revisão é `meta-ad-approver-worker`. Para revisão, ele consome somente
`/api/internal/creatives/agent-review/stage-executions/pending`, executa `gpt-5.6-sol` pelo Codex
ChatGPT em sandbox própria `read-only` e envia o parecer exclusivamente pelo callback do backend.
Para criação, edição e retrabalho de imagens comerciais, o recurso isolado `iris-image-studio`, sob
controle PLAY/STOP de Íris, consome as filas versionadas do backend, exige prova aprovada, usa
`gpt-image-2` e devolve o binário e a auditoria ao backend. Íris usa também o executor independente
`communication-agent-worker` para contratos, copy, peças estruturadas e landing. Os três
containers são construídos pelo mesmo módulo e workflow, mas ativam papéis Spring mutuamente
exclusivos: o revisor não recebe a chave OpenAI e o Estúdio não recebe Codex, repositório, browser ou
ferramentas de aprovação. Cada container possui usuário sem privilégios, filesystem somente leitura,
health, log e limites operacionais próprios. O
`ai-worker` não pode conter pacote, prompt, schema, executor, edição ou decisão desse fluxo visual.

O worker deve publicar `health` e `logfile` somente leitura em base path operacional dedicada. O MCP
central deve permitir consultar `meta-ad-approver-worker` com filtros por período e correlação, e
consultar `META_AD_APPROVER` na telemetria persistida. Os logs devem registrar início, conclusão e
falha com `experimentId` e `creativeId`, stack trace completo em falhas e sanitização de segredos.
Logs vivos e telemetria são evidências complementares; nenhum deles substitui o callback funcional.

O MCP central também deve expor uma leitura consolidada pelo `creativeId`, cruzando o parecer
canônico, heartbeat, processo vivo, atividade, detecção de bloqueio e memória vigente de Têmis. A
consulta deve distinguir memórias confirmadas de candidatas: candidatas orientam a investigação,
mas somente feedback posterior e independente pode confirmá-las. Íris, Dédalo e Têmis devem recuperar
suas memórias governadas quando o próprio domínio estiver envolvido; nenhum deles pode usar a própria memória como prova de
aprovação nem promover sozinho um aprendizado. O consenso é comprovado pelo novo artefato e pelo
parecer persistido, nunca pela concordância textual entre memórias.

Cada lote reservado deve executar seus criativos concorrentemente e com isolamento de falha. Um
processo Codex lento ou bloqueado não pode manter os demais itens do mesmo lote em `PROCESSING` sem
execução real nem multiplicar o tempo total pelo tamanho do lote. O limite individual, a telemetria
e o callback continuam sendo aplicados separadamente a cada criativo.

Cada execução materializa o MCP versionado `meta_ad_approver`, restrito ao criativo e ao experimento
reservados. As ferramentas obrigatórias confirmam o contexto no backend, retornam a mídia real em
alta definição ou três quadros do vídeo e renderizam a landing em mobile e desktop. Divergência de
identificador, evidência ausente ou falha de inspeção mantém o gate fechado. O MCP não publica, não
altera campanha e não acessa banco.

Para vídeo MP4, os três quadros devem ser extraídos do arquivo final por FFmpeg/FFprobe estáticos e
pinados em 10%, 50% e 90% da duração. Chromium/Playwright fica responsável pela landing, não pela
decodificação do H.264/AAC. O download temporário é limitado a 64 MiB, recebe permissão `0600` e é
removido ao final inclusive em erro. Cada processo de decodificação é limitado a 120 segundos. O CI
e o deploy devem sintetizar um MP4 H.264, extrair os três quadros e capturar HTML dentro do mesmo
container não-root e somente leitura usado em produção, além de confirmar que um arquivo inválido é
bloqueado; health de processo sem essas provas não comprova prontidão visual.

Como o Aprovador executa por `codex exec` sem usuário interativo, o runner declara explicitamente
`approval_policy=never` por configuração explícita, mantendo `sandbox=read-only`. Todas as ferramentas do MCP publicam
`readOnlyHint: true`, `destructiveHint: false` e `openWorldHint` coerente com o acesso HTTP. É
proibido depender da política herdada da identidade Codex: uma solicitação de aprovação sem operador
cancela a ferramenta, invalida as evidências e fecha o gate.

## Evidências obrigatórias

- copy completa, CTA, público, hipótese e oferta;
- imagem em alta definição ou três quadros representativos do vídeo;
- URL pública de destino válida;
- screenshots integrais da landing em mobile e desktop;
- mapa de associações de desejo e limites de verdade, quando disponíveis.
- para vídeo, governança estruturada ligada à URL e ao SHA-256 do arquivo final: ativo e job
  canônicos, origem gerada, referência sintética, consentimento aplicável, direitos, curadoria de
  licença comercial do provedor e aprovação humana do vídeo. Declaração solta, prompt, catálogo ou
  evidência de outro arquivo não comprovam os direitos da mídia inspecionada.

Sem qualquer evidência obrigatória, o gate permanece fechado.

## Critérios

O agente avalia separadamente atenção, clareza, desejo, credibilidade e ação. Também registra pareceres explícitos sobre copy, estética comercial e integração anúncio → landing. A aprovação exige ausência de bloqueio, nota mínima 80 em todas as dimensões e coerência comprovada de público, dor, promessa, mecanismo, oferta, identidade e próximo passo.

## Ciclo de melhoria

Em `ADJUST` ou `REJECTED`, a execução revisora entrega a causa, requisitos obrigatórios, elementos
proibidos e critérios verificáveis, mas nunca texto, conceito, prompt visual ou ativo substituto. O
backend controla tentativas pelos gates de progresso, repetição, custo e iteração do ciclo de
convergência, preservando versões, requests, responses e evidências. Íris ou Apolo materializa a
correção conforme a mídia, e outra execução independente de Têmis decide o gate.

O upload canônico da mídia produzida é `POST /api/internal/creatives/{id}/agent-improvement/artifact` em `multipart/form-data`. O arquivo é obrigatório; `model`, `prompt` e `costUsd` preservam a auditoria. O frontend e o monitor devem exibir o identificador da tarefa, o identificador da execução criativa e a causa persistida atual, priorizando a execução ativa sobre um bloqueio histórico da tarefa agregadora.

## Ciclo de convergência v1

Têmis separa memória de estratégia promovida. Novas regras de revisão precisam superar o baseline
nos mesmos replays congelados, em holdout fora da amostra, sem regressão e dentro do limite de custo.
O mínimo é de dez replays e cinco casos de holdout. O agente pode consultar estratégias promovidas
pelo MCP, mas não avaliar, promover ou alterar sozinho prompt, schema, código ou critérios de gate.

Por decisão de 2026-08-17, esse ciclo também governa a produção visual. Cada parecer independente de
entregável ou criativo registra tentativa, placement, formato, versão do playbook, custo, menor score,
códigos estáveis de falha e evidência. Ao completar quinze casos homogêneos, o backend congela os dez
primeiros para replay e os cinco restantes para holdout. Uma rotina assíncrona do container revisor
consolida regras sem chamar gerador de imagem, provider, campanha ou publicação. O resultado é sempre
candidato e exige promoção humana explícita.

Todo job novo do Estúdio ou retrabalho de criativo congela a versão do playbook, sua chave contextual,
as regras promovidas e até dois exemplos `APPROVED` da Biblioteca Audiovisual do mesmo plano. Nicho,
tipo de produto, finalidade, placement e formato compõem a chave; aprendizado de outro contexto não
pode vazar. Regras canônicas de segurança não podem ser substituídas por uma candidata, apenas
complementadas. A meta operacional é aprovação mínima de 70% na primeira tentativa, peça aprovada em
até três versões, reincidência inferior a 10%, redução mínima de 30% do custo por ativo aprovado e
nenhuma queda do score premium.

O backend é o coordenador exclusivo da convergência anúncio → landing. Cada falha bloqueante do
Aprovador deve declarar um código estável, requisito, critério de aceite e exatamente um responsável:
`CREATIVE_COPY`, `CREATIVE_MEDIA` ou `LANDING`. O backend persiste ciclo, versão, score, custo,
evidência e tarefa. Quando o alvo for `LANDING`, o backend cria de forma idempotente uma delegação
Têmis → Íris, envia o mesmo briefing à fila autônoma oficial de Íris e sincroniza o estado da
tarefa com o callback do executor. Íris escolhe a reconstrução causal por etapas canônicas; não
recebe autoridade para alterar oferta, preço, checkout, tracking ou publicar. Nenhum executor chama
outro executor nem decide a próxima etapa.

Uma nova versão sempre retorna ao Aprovador. A mesma impressão digital de falha não pode reaparecer
duas vezes sem bloquear o ciclo por ausência de progresso. O ciclo também bloqueia ao atingir oito
avaliações ou US$ 5,00 de custo auditável. Esses limites não autorizam publicação: aprovação humana,
orçamento e mudança do experimento para `RUNNING` permanecem gates separados. A aprovação técnica
encerra somente a linhagem avaliada; o experimento exige todos os criativos e a jornada aprovados.

## Limites de autoridade

O agente não substitui aprovação humana, não publica, não ativa mídia, não muda preço ou orçamento e não coloca experimento em execução. Impasse ou limite atingido permanece bloqueado com causa persistida.

## Preservação do histórico

A migração de executor não cria nova tabela nem reinicia criativos. Pareceres, versões, tentativas,
custos e estados já persistidos — inclusive os do experimento #88 — permanecem sob os mesmos
contratos do backend. O painel de Aprendizado permite incorporar esses pareceres uma única vez por
identidade de ativo ou criativo, sem carregar payloads visuais brutos nem repetir providers; chamadas
posteriores são idempotentes. Apenas novas reservas são processadas pelo módulo independente.
A reserva de cada revisão deve ser um lease auditável controlado pelo backend, com horário de início,
contador de recuperações e horário da última recuperação. Uma revisão `PROCESSING` sem lease ou com
lease vencido deve voltar automaticamente à fila, no máximo duas vezes; depois disso deve encerrar em
`FAILED` com causa persistida. O worker nunca redefine estado diretamente nem assume a recuperação.
