# Continuidade após aprovação dos vídeos

## Diagnóstico e escopo

Em 14/09/2026, UI `/videos` e MCP `db_query` confirmaram os ativos #41 (AD) e #42
(LANDING_HERO), experimento #92, aprovados por `Marketing Hub` às 22:46:04 e
22:46:02 UTC. O ciclo #2, revisão 11, permaneceu em VIDEO_APPROVAL. O PATCH de
aprovação grava o ativo, sem coordenar a passagem para integração. O formulário
exige criativo, slot PDE e transcrição manual de provas.

O #92 não possui URL comercial nem slot produtivo. A prova #270 identifica o
destino privado v12 já aprovado, no próprio ciclo. A experiência histórica #91
possui criativo e URL comerciais, contexto distinto. Exigir o contrato produtivo
na preparação privada repete LOOP-DESTINO-CONDICIONAL-LANDING-OBRIGATORIA e
LOOP-BPM-DECISAO-HUMANA-COMO-EXECUCAO. A consulta de logs `video-assets` pelo MCP
respondeu HTTP 206, com zero linhas; o diagnóstico é confirmado por UI, dados e código.

Preservar produto 4, cadeia 14/v14, processo 75/v6, run 4, ciclo 2, experimento 92,
versão musa-pde-entry-v12-primeiro-ajuste-aplicavel, mídias e aprovações existentes.

## Alternativas

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Preencher manualmente apenas Vega | Menor alteração | Repete trabalho humano e não resolve o contrato privado; baixa aderência | Descartada |
| Automatizar cliques/formulários no frontend | Menos cliques aparentes | Orquestração frágil, duplica decisões, depende de navegador aberto | Descartada |
| Integrar e coordenar pelo backend com provas persistidas | Reutiliza aprovações, recupera ciclos ativos e atende execuções futuras | Esforço maior, exige testar concorrência e invalidar somente provas afetadas | Escolhida |

## Matriz definida antes dos testes

| Dimensão | Critérios de aceitação |
| --- | --- |
| Caminho feliz | Duas aprovações → integração do destino correto → fila técnica → revisões independentes → registro automático → autorização comercial separada |
| Validações | Uma aprovação pendente, rejeição, mídia alterada, versão/experimento/produto divergentes e destino ambíguo bloqueiam; nenhuma evidência é fabricada |
| Falhas e recuperação | Repetição, concorrência, reinício, callback repetido, falha técnica e retomada preservam histórico e não duplicam tarefas/consumo |
| Integrações | Backend e MySQL 5.7 locais; consumidores reais com integrações externas simuladas; experiência privada e contrato de catálogo separados |
| Observabilidade | Evento de integração, identidades, decisões, tarefas, motivos e custos consultáveis; ausência de custo não vira zero; leitura sem escrita |
| Métricas e segregação | QA_INTERNAL/AGENT_VALIDATION; sem campanha, cobrança, contato externo, receita simulada no ledger comercial ou nova geração paga |
| Desktop/mobile | Chromium desktop, iPhone 15 Pro e Pixel 7: acompanhamento sem formulário duplicado; vídeo opcional, reprodução e falha com CTA e retomada preservados |
| Regressão | Produtos/ciclos diferentes, histórico encerrado, mídia revogada, STOP/pausa e gates humanos comerciais mantidos |
| Entrega | Suítes pertinentes, duas rodadas completas consecutivas após última correção, build e revisão do diff antes de qualquer publicação |

## Resultados

Homologação local concluída em 15/09/2026, rodadas `final2` e `final3`, completas,
consecutivas e sem falhas. A rodada anterior detectou a incompatibilidade TypeScript;
a conferência da imagem detectou a dependência de codecs antes da publicação.

| Verificação por rodada | Resultado |
| --- | --- |
| Backend completo, inclusive arquitetura | 3.006 executados, 8 opcionais ignorados |
| Psique Java | 106 executados, 1 opcional ignorado |
| Têmis Java | 91 executados, 1 opcional ignorado |
| Contratos do navegador e worker privado | 22 aprovados |
| Frontend relacionado | 32 aprovados |
| Preservação do plano operacional | 2 testes aprovados, incluindo os cinco alvos |
| Total contabilizado | **3.259 por rodada; 6.518 nas duas** |

Além da contagem: TypeScript, builds, recursos empacotados, cinco imagens, cinco
cenários do harness real, seis inspeções de tela, MySQL 5.7, cinco integrações
concorrentes com recibo único, revogação de mídia, retomada após reinício do banco
e `git diff --check`. Os 20 arquivos Java alterados têm comentário de responsabilidade.
Nenhum changelog produtivo novo foi necessário; a fixture aplica o changelog Vega
existente, com include relativo explícito.

As imagens contêm o código identificado pelo digest de fontes
`ae63251932c2086e08d6c93dc9bbd6997469a2cd58a32e6323032847b6911579`, sobre a base
`5f77fcd6cc1c735d06cd852e03ba8c892ed617bb`. Os ajustes posteriores restringiram-se
ao verificador de homologação (identificador de sessão e reprodução com filesystem
somente leitura); nenhum código incorporado às imagens mudou entre as rodadas.
O JAR do backend e os bundles foram comparados com os artefatos locais; prompts e
harness foram conferidos dentro das imagens. Chrome oficial reproduziu também os
bytes reais de #41 e #42: 15 segundos, 1080 × 1920 e áudio decodificado em ambos.

O reinício inicial encontrou falta de espaço na engine efêmera. Foram removidos
artefatos descartados desta homologação e cache de build antigo, sem remover imagens
ativas ou dados produtivos. O reinício passou nas duas rodadas finais.

## Recuperação publicada e comprovação

Aplicação excepcional autorizada pelo usuário, sem PR, coordenada pela intervenção
`0161d37ffc2344bda4b409ebd9f9bfda`, escopos `app` e `pde`. O coordenador confirmou
`ACTIVE`, publicadores pausados e fila vazia antes da operação. Transferência e
troca passaram por `execute`. Todos os serviços ficaram saudáveis; as camadas,
plataforma, comandos, ambiente e usuário efetivo das imagens foram comparados com
os candidatos locais. Cada serviço conserva uma imagem de retorno com sufixo
`vega-approved-rollback-ae63251932c2`, além dos arquivos Compose originais.

Sem novo comando humano, o backend registrou:

- **Evento #15**, 15/09/2026 00:41:52 UTC: aprovação das peças reutilizada e
  integração privada concluída. IDs, hashes, responsável e horários originais
  de #41/AD e #42/LANDING_HERO foram preservados.
- **Tarefas #423–#427**, uma ocorrência de cada revisão afetada, todas
  `COMPLETED / APPROVED`: homologação técnica, Psique aderente, recuperação,
  segurança e Têmis. Nenhuma tentativa paga foi repetida e nenhum vídeo foi
  gerado novamente.
- **Gate #289 e evento #16**, 00:56:37 UTC: homologação concluída automaticamente,
  com o mesmo fingerprint `f8dcfafecab160d1af259f7367ad77039fa0e79c8b67781ebc2526120b4d1c69`.
- Ciclo **2**, revisão **13**, experimento **92**, produto **4**, cadeia **14** e
  versão **v12** preservados. Nova etapa: **AUTHORIZATION**, orçamento e janela.
  O processo comercial #75/v6 continua em andamento; vendas, entrega e resultados
  comerciais não foram fabricados nem declarados concluídos.

O harness produtivo comprovou os 13 critérios anteriores e os quatro novos:
identidade dos vídeos, reprodução com áudio, uso opcional e recuperação da falha
de mídia. As leituras de teste ficaram em `AGENT_VALIDATION`; não houve campanha,
cobrança ou gasto de mídia. A tela pública foi novamente inspecionada em desktop
e iPhone emulado, exibindo a continuidade automática e depois o gate comercial.

Custos estimados registrados dos quatro pareceres: **USD 2,464248**. A tarefa
técnica não informou custo próprio e as inferências internas de primeiro ajuste
preservam seus requests/responses e tokens, mas ainda não têm custo conciliado.
Portanto, esse subtotal não é custo total nem confirmação de saldo integral do teto.
Os custos continuam consultáveis nas tarefas do subprocesso; o resumo do processo
pai contabiliza somente suas próprias execuções e não agrega essas revisões.

Links operacionais:

- [Ciclo #2 / experimento #92](http://191.252.181.168:5173/business-process-chains/learning-cycles?chainId=14&productId=4&cycleId=2).
- [Processo pai #75/v6](http://191.252.181.168:5173/products/4/value-chain-history/processes/75/activities?chainId=14&learningCycleId=2).
- [Provas das revisões no subprocesso](http://191.252.181.168:5173/products/4/value-chain-history/processes/63/activities?chainId=14&learningCycleId=2).

## Aprendizado de operação

Transcrever IDs e repetir a aprovação dos mesmos arquivos não acrescentava prova
de qualidade. Essa passagem foi substituída por validação dos dados persistidos e
integração automática. As revisões conservam sua responsabilidade e produzem provas
do conjunto efetivamente entregue. A hipótese de melhoria é reduzir o tempo e o
abandono entre aprovação e prontidão comercial; receita e conversão ainda precisam
ser medidas em operação comercial autorizada.

## Decisões de implementação

- **Destino:** promover a experiência privada a slot comercial anteciparia um gate;
  gravar URLs globais no produto perderia o vínculo com o ciclo. Foi escolhido
  um recibo versionado no diário do ciclo, consumido pela experiência privada e
  pelos executores. O adaptador entregue atende `/vega-private`, cuja tela e
  harness implementam este contrato. Outros destinos conservam seu fluxo até
  terem adaptador e provas próprios; o tipo de produto sozinho não concede suporte.
- **Revisões:** reaproveitar o gate antigo ignoraria a nova mídia; reproduzir os
  vídeos inteiros repetiria o custo principal. Reutilizam-se os arquivos e as
  aprovações humanas, com uma ocorrência nova de cada revisão afetada, após a
  integração. Falha atual não cria retry automático. O teto da versão é exigido
  antes do despacho; custos ausentes não são declarados como zero ou conciliados.
- **Identidade:** a comparação do JSON precisa tolerar a leitura de um número
  inteiro como `Integer` em vez de `Long`, mantendo valor e todos os campos.
  O teste de round-trip detectou e corrigiu essa divergência antes da publicação.
- **Controle:** a continuidade roda antes da espera da atividade ativa, respeitando
  pausa, ancestrais pausados, contexto encerrado, prioridade entre processos e STOP.
  O executor somente executa e devolve provas; a transição permanece no backend.

## Reprodução local

`infra/testing/video-continuation/run-round.sh` executa a matriz. A fixture
`LearningCycleLocalApplication` une ciclo, diário e experiência privada em MySQL
5.7; os metadados do provedor de vídeo são test doubles explícitos. Os MP4s locais
são produzidos com ffmpeg e inspecionados com ffprobe. A geração do primeiro ajuste
usa o worker real contra o mock OpenAI versionado; nenhum teste exige provedor pago.

O coordenador de tarefas é exercitado com callbacks simulados, inclusive duplicação,
falha, STOP e aprovação comercial separada. O harness real executa cinco cenários;
as telas são conferidas em Chromium desktop e emulações iPhone 15 Pro/Pixel 7.
Emulação não comprova Safari nativo nem avaliação humana de compreensão.

Os bytes dos ativos produtivos #41 e #42 foram consultados sem mutação e conferidos
com os hashes persistidos. Ambos permaneciam `APPROVED`, sem redirecionamento do
MP4. Não houve reprodução paga nem alteração da aprovação para esta inspeção.

## Falhas adicionais descobertas antes da publicação

- A checagem TypeScript do frontend privado recusou `replaceAll` pelo alvo de
  compilação existente. A substituição de separadores passou a usar expressão
  regular compatível, mantendo o texto e o alvo do projeto.
- O MP4 H.264/AAC tocou no Chromium da sandbox, mas falhou com
  `NotSupportedError` tanto no headless shell quanto no Chromium completo da
  imagem Playwright. A [documentação oficial de codecs](https://playwright.dev/docs/browsers#media-codecs)
  confirma que o Chromium aberto não inclui todos os codecs dos navegadores oficiais.
  Alternativas: dispensar a prova de reprodução enfraqueceria o gate; transcodificar
  só para o teste deixaria de testar o arquivo aprovado; usar Chrome oficial na imagem
  testa os mesmos bytes. Escolhida a terceira opção, com Chrome for Testing
  `153.0.8010.36`, URL oficial e SHA-256 fixados no Dockerfile. O restante dos
  observadores mantém seu navegador; o harness audiovisual seleciona o executável
  compatível. Essa correção reinicia a exigência das duas rodadas completas.
