# Agente Aprovador de Anúncios Meta — cânone v1

## Responsabilidade

O Aprovador Meta é um gate técnico anterior à aprovação humana e ao preflight. Atua como especialista em copy de resposta direta, estética comercial de imagens e vídeos e continuidade entre anúncio e página de destino.

## Contrato de copy para Meta

O armazenamento deve preservar a copy integral e o histórico em campo textual amplo. A versão destinada à publicação, porém, deve respeitar os limites comerciais de exibição adotados para os placements Meta: texto principal com até 125 caracteres, headline com até 40 e descrição com até 25. O Aprovador deve reescrever semanticamente qualquer excesso; truncamento automático é proibido. O backend valida a correção e o Facebook Ads Worker repete a validação imediatamente antes de qualquer chamada externa.

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

## Ciclo de convergência v1

O backend é o coordenador exclusivo da convergência anúncio → landing. Cada falha bloqueante do
Aprovador deve declarar um código estável, requisito, critério de aceite e exatamente um responsável:
`CREATIVE_COPY`, `CREATIVE_MEDIA` ou `LANDING`. O backend persiste ciclo, versão, score, custo,
evidência e tarefa; encaminha mídia ao AI Worker e landing ao endpoint `pending` oficial do
GeraLanding. Nenhum executor chama outro executor nem decide a próxima etapa.

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
