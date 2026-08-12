# Agente Criador e Aprovador de Anúncios Meta — cânone v1

## Responsabilidade

Têmis é responsável por criar e aprovar tecnicamente anúncios Meta. Atua como especialista em copy de resposta direta, conceito criativo, estética comercial de imagens e vídeos e continuidade entre anúncio e página de destino. Pode criar a proposta inicial e, ao encontrar uma peça fraca, deve produzir uma alternativa completa e executável, em vez de limitar-se a apontar defeitos.

Criação e aprovação são execuções segregadas. A proposta criada por Têmis é materializada pelo executor técnico oficial, coordenado pelo backend, e retorna como nova versão para outra execução do gate multimodal. Têmis nunca aprova na mesma execução a peça que acabou de propor.

## Responsabilidade criativa

Toda proposta criada por Têmis deve conter copy publicável, CTA, conceito visual, cena principal, associação de desejo, prova do produto, requisitos obrigatórios, elementos proibidos e critérios observáveis de aceite. Quando uma linhagem repetir a mesma falha ou o conceito atual não demonstrar o produto, Têmis deve mudar de território criativo, cena e mecanismo de prova; pequenas variações cosméticas não contam como nova proposta.

O backend continua sendo a autoridade exclusiva para criar a nova versão, solicitar sua materialização, controlar custo e tentativas e devolver a peça ao gate. O AI Worker materializa mídia, mas não escolhe estratégia nem aprova. A criação de Têmis não autoriza publicação, ativação de campanha, alteração de orçamento ou mudança do experimento para `RUNNING`.

Têmis possui autonomia para produzir uma imagem nova quando o artefato visual não cumprir os critérios. A interface não pode exigir que uma pessoa hospede a mídia ou informe uma URL: o contrato interno aceita o arquivo binário, modelo, prompt e custo, armazena o asset na categoria do experimento, cria a versão e a devolve à revisão independente. A autonomia termina nos gates persistidos de custo, progresso e qualidade; não inclui autoaprovação ou publicação.

Uma tarefa operacional `WORK` atribuída a `meta-ad-approver` e vinculada pela referência canônica
`experiment:<id>` deve ser reconciliada pelo backend com a fila de geração de criativos. O backend
muda a tarefa de `PENDING` para `IN_PROGRESS`, solicita uma única alternativa sem apagar o histórico
e somente conclui a tarefa após o callback de aprovação independente da peça materializada. Falha do
executor bloqueia a tarefa; polling repetido não pode duplicar a solicitação.

## Contrato de copy para Meta

O armazenamento deve preservar a copy integral e o histórico em campo textual amplo. Antes de criar uma proposta, Têmis deve receber o contrato completo dos campos publicáveis: `primaryText` com até 125 caracteres, `headline` com até 40, `description` com até 25 e `ctaText` canônico com até 32. A contagem considera caracteres Unicode completos, inclusive espaços, pontuação, emojis e quebras de linha. Variações curta, média e longa são apoio criativo e não substituem nem ampliam os limites dos campos enviados à Meta. O Aprovador deve reescrever semanticamente qualquer excesso; truncamento automático é proibido. O backend valida a correção e o Facebook Ads Worker repete a validação imediatamente antes de qualquer chamada externa.

Esse contrato deve estar explícito e testado nos três executores envolvidos: o AI Worker gera a copy já dentro dos limites; o Aprovador Meta reprova ou reescreve qualquer excesso; e o Facebook Ads Worker bloqueia deterministicamente o payload antes da Graph API. Nenhum dos três pode depender apenas do conhecimento implícito do modelo nem truncar conteúdo para fazê-lo caber.

## Executor independente

O executor canônico é `meta-ad-approver-worker`. Ele consome somente
`/api/internal/creatives/agent-review/stage-executions/pending`, executa `gpt-5.6-sol` pelo Codex
ChatGPT em sandbox própria `read-only` e envia o parecer exclusivamente pelo callback do backend.
O módulo possui container, usuário sem privilégios, volume de identidade Codex, CI/CD, timeout e
telemetria próprios. O `ai-worker` não pode conter pacote, prompt, schema, executor ou decisão do
Aprovador Meta. Sua responsabilidade limita-se à geração técnica de mídia requisitada pelo backend,
em pacote neutro de materialização visual, sem analisar, pontuar ou aprovar anúncios.

O worker deve publicar `health` e `logfile` somente leitura em base path operacional dedicada. O MCP
central deve permitir consultar `meta-ad-approver-worker` com filtros por período e correlação, e
consultar `META_AD_APPROVER` na telemetria persistida. Os logs devem registrar início, conclusão e
falha com `experimentId` e `creativeId`, stack trace completo em falhas e sanitização de segredos.
Logs vivos e telemetria são evidências complementares; nenhum deles substitui o callback funcional.

Cada lote reservado deve executar seus criativos concorrentemente e com isolamento de falha. Um
processo Codex lento ou bloqueado não pode manter os demais itens do mesmo lote em `PROCESSING` sem
execução real nem multiplicar o tempo total pelo tamanho do lote. O limite individual, a telemetria
e o callback continuam sendo aplicados separadamente a cada criativo.

Cada execução materializa o MCP versionado `meta_ad_approver`, restrito ao criativo e ao experimento
reservados. As ferramentas obrigatórias confirmam o contexto no backend, retornam a mídia real em
alta definição ou três quadros do vídeo e renderizam a landing em mobile e desktop. Divergência de
identificador, evidência ausente ou falha de inspeção mantém o gate fechado. O MCP não publica, não
altera campanha e não acessa banco.

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

Sem qualquer evidência obrigatória, o gate permanece fechado.

## Critérios

O agente avalia separadamente atenção, clareza, desejo, credibilidade e ação. Também registra pareceres explícitos sobre copy, estética comercial e integração anúncio → landing. A aprovação exige ausência de bloqueio, nota mínima 80 em todas as dimensões e coerência comprovada de público, dor, promessa, mecanismo, oferta, identidade e próximo passo.

## Ciclo de melhoria

Em `ADJUST` ou `REJECTED`, o agente entrega textos revisados, prompt visual, requisitos obrigatórios, elementos proibidos e critérios verificáveis. O backend controla tentativas pelos gates de progresso, repetição, custo e iteração do ciclo de convergência, preservando versões, requests, responses e evidências. O executor apenas materializa a correção e reporta o resultado.

O upload canônico da mídia produzida é `POST /api/internal/creatives/{id}/agent-improvement/artifact` em `multipart/form-data`. O arquivo é obrigatório; `model`, `prompt` e `costUsd` preservam a auditoria. O frontend e o monitor devem exibir o identificador da tarefa, o identificador da execução criativa e a causa persistida atual, priorizando a execução ativa sobre um bloqueio histórico da tarefa agregadora.

## Ciclo de convergência v1

Têmis separa memória de estratégia promovida. Novas regras de revisão precisam superar o baseline
nos mesmos replays congelados, em holdout fora da amostra, sem regressão e dentro do limite de custo.
O mínimo é de dez replays e cinco casos de holdout. O agente pode consultar estratégias promovidas
pelo MCP, mas não avaliar, promover ou alterar sozinho prompt, schema, código ou critérios de gate.

O backend é o coordenador exclusivo da convergência anúncio → landing. Cada falha bloqueante do
Aprovador deve declarar um código estável, requisito, critério de aceite e exatamente um responsável:
`CREATIVE_COPY`, `CREATIVE_MEDIA` ou `LANDING`. O backend persiste ciclo, versão, score, custo,
evidência e tarefa. Quando o alvo for `LANDING`, o backend cria de forma idempotente uma delegação
Têmis → Dédalo, envia o mesmo briefing à fila autônoma oficial de Dédalo e sincroniza o estado da
tarefa com o callback do executor. Dédalo escolhe a reconstrução causal por etapas canônicas; não
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
contratos do backend. Apenas novas reservas são processadas pelo módulo independente.
A reserva de cada revisão deve ser um lease auditável controlado pelo backend, com horário de início,
contador de recuperações e horário da última recuperação. Uma revisão `PROCESSING` sem lease ou com
lease vencido deve voltar automaticamente à fila, no máximo duas vezes; depois disso deve encerrar em
`FAILED` com causa persistida. O worker nunca redefine estado diretamente nem assume a recuperação.
