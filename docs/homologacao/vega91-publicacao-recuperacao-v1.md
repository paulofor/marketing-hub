# Vega #91 — recuperação da publicação Meta

## Evidência e decisão

Em 06/09/2026, a tela, os endpoints e o MCP confirmaram o experimento #91 em
`FAILED`, liberado às 17:55:17 UTC. O job
`472907ba7b5bdbf4d3dd3ed1adb4b31c472fb438f7941114f3e9658fed9fa569` foi entregue
pelo backend e bloqueado pelo publicador às 17:58:36 UTC antes de criar objetos
Meta: o anúncio #526 tinha 202 caracteres no texto principal, contra 125 do
contrato comercial canônico. Nenhuma campanha do #91 estava persistida.
O #88 possui publicação histórica completa, descartando indisponibilidade geral
do publicador. A lista administrativa excluía `FAILED` inclusive ao filtrar esse
status. O parecer e a aprovação humana não repetiam a validação determinística
dos limites que já existia no worker.

Alternativas: corrigir somente a copy (menor esforço, recorrência intacta),
relaxar o limite (contraria o cânone vigente) ou alinhar os gates e a recuperação
(esforço moderado, preserva mídia e aprovações auditáveis). Escolhida a terceira,
com revisão da copy em nova versão e preservação do vídeo aprovado.

Objetivo operacional: uma campanha completa vinculada ao #91, `RUNNING` somente
após callback real, R$ 20/dia e teto de R$ 100. Publicação e impressões não são
vendas. Continuar com rastreabilidade e limites íntegros; ajustar o que falhar;
parar gasto no teto ou diante dos gates comerciais persistidos.

## Matriz definida antes da homologação

| Dimensão | Critério de aceite local |
| --- | --- |
| Lista administrativa | `FAILED` visível, pesquisável e filtrável; estados encerrados preservados no histórico. |
| Copy e aprovação | 125/40/25 caracteres Unicode; excesso não recebe aprovação do agente nem humana; armazenamento não trunca histórico. |
| Readiness e fila | Copy incompatível aparece como bloqueio explicável antes da liberação e do consumo; correção válida desbloqueia. |
| Recuperação | Nova revisão mantém vídeo e origem, recebe parecer próprio; retry autorizado mantém experimento e teto. |
| Publicação | Backend e Meta simulados; campanha, conjunto, anúncio, vídeo, destino, orçamento, período e IDs coerentes. |
| Falhas e idempotência | Callback incompleto, erro Meta, orçamento e período inválidos, repetição e duplicidade não produzem falso `RUNNING`. |
| Observabilidade e métricas | Causa e job persistidos; QA local não cria campanhas reais, eventos comerciais, compras ou gasto. |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulação mobile; lista, bloqueios, correção e navegação sem overflow. |
| Regressão | Testes dos módulos alterados, suíte do publicador, typecheck, build, diff e comentários Java revisados. |

Dados locais são doubles/fixtures identificados; nenhum parecer local pode ser
usado como evidência produtiva de Têmis. Após defeito corrigido, executar duas
rodadas completas consecutivas sem falhas. Qualquer limitação externa deve ser
registrada com sua evidência concreta.

## Defeitos encontrados na própria homologação

A primeira rodada completa executou 2.369 testes backend e revelou uma falha na
edição mobile: o rodapé do modal podia ficar fora da área visível. O modal da nova
versão agora mantém as ações visíveis, com rolagem no corpo. O teste verifica o
botão inteiramente no viewport antes de clicar. A automação também captura
conjuntamente a interação e a resposta para preservar o diagnóstico de falhas.
O diagnóstico posterior identificou interceptação de ponteiro no iPhone: a
automação agora usa toque nos dispositivos móveis e capturas do viewport, sem
clique forçado e sem alterar o enquadramento entre as ações. O botão precisa
estar inteiramente visível; a evidência registra o elemento atingido pelo toque.
A recorrência no iPhone também exigiu corrigir o ambiente de automação: o
Playwright global 1.54.2 declarava Chromium 139 em seu manifesto, mas a sandbox
executava Chromium 152. O projeto passou a declarar `@playwright/test` 1.63.0,
com suporte aos navegadores atuais, e cada jornada registra a versão real usada.
Foram comparados manter o binário antigo compatível, instalar outro navegador
isolado ou atualizar o driver para o Chromium disponível. Escolhida a atualização
do driver, preservando a emulação e sem ignorar validações de interação.
Referência: [compatibilidade de navegadores do Playwright](https://playwright.dev/docs/browsers).
Após o ajuste, a jornada precisa passar novamente nos três dispositivos.
Na revisão subsequente, o teste histórico de liberação revelou que o comando apagava até campanha ativa, contrariando a unicidade já canônica. A correção bloqueia a nova liberação quando existe campanha vinculada, torna repetição de clique idempotente e preserva os eventos e o início da mensuração no retry de `FAILED`. O teste de integração foi atualizado para comprovar essas três situações. As rodadas finais foram reiniciadas depois dessa última correção.

## Resultado

As rodadas iniciais `aprovada-1` e `aprovada-2`, concluídas em 06/09/2026, passaram
consecutivamente após a última correção. Cada uma executou:

- backend: 2.369 testes, zero falhas, zero erros, três ignorados pela suíte;
- frontend: 488 testes, typecheck e build;
- Facebook Ads Worker: 120 testes, incluindo publicação com Meta simulada;
- seis jornadas de navegador: recuperação da publicação e fluxo de vídeo,
  cada um em desktop, iPhone 15 Pro e Pixel 7.

O harness de recuperação usa o frontend compilado e o backend Spring real com
H2; somente integrações externas e dados de QA são simulados. Nenhum teste local
usa callback de revisão para aprovar dados produtivos. Não houve alteração de
changelog nem execução de MySQL nesta homologação.

O empacotamento e as imagens de backend/frontend foram construídos pelos
Dockerfiles versionados. O smoke Docker confirmou a rota SPA, health do frontend
e Java 21 no backend; não representa um boot integrado com MySQL. O projeto
Compose exclusivo foi encerrado com volumes e órfãos removidos. Logs, capturas,
resultados e arquivos das imagens permanecem em
`artifacts/vega91-publication-recovery/` (não versionados).

## Execução operacional

O diagnóstico oficial da conta Meta confirmou `ads_management` e `ads_read`
concedidos, conta acessível e biblioteca de vídeos acessível. A tela criou a
revisão #527 a partir de #526, com 87 caracteres, preservando o vídeo #40 e sua
governança verificada. Têmis aprovou a revisão na fila real; a aprovação de portfólio
foi executada pelo frontend com autorização do usuário. O #526 foi substituído
e a liberação foi executada, conforme as evidências do segundo bloqueio abaixo.
Não se presume `RUNNING` por haver solicitação.

Acesso SSH ao worker `191.252.210.83` e backend `191.252.181.168` retornou
`Permission denied (publickey,password)`. Esta sandbox não contém a chave
privada da execução anterior; a chave pública informada no histórico não
permite reconstruí-la. Foi solicitado o caminho protegido da credencial.
MCP e frontend estão acessíveis. Nenhum código foi publicado por SSH, commit,
push ou PR nesta execução.

## Segundo bloqueio confirmado na Meta

A revisão #527 foi aprovada por Têmis às 22:15:14 UTC e pelo frontend às
22:17:37 UTC. O #526 foi marcado como substituído, preservando mídia e histórico.
A liberação pela tela às 22:18:59 UTC gerou o job
`f00f013da99b5ef9d0c3bd4437ffd6f2e2b5680f533f7804bd270d8d8445e631`.
O upload real do vídeo foi aceito (`video_id=1978283913145425`). A criação da
campanha foi recusada às 22:22:23 UTC, passo #803, HTTP 400/código 100/subcódigo
2446307: a Meta exige ao menos R$ 300 nesse campo `spend_cap` para a conta BRL.
Nenhuma campanha foi criada e o experimento voltou a `FAILED`.

O mesmo request revelou `OUTCOME_LEADS` apesar do objetivo `SALES` persistido:
a heurística de recompensa gratuita precedia o objetivo explícito. A correção
local preserva vendas e otimização Purchase para PDE com degustação.

Alternativas financeiras: elevar o teto (exige nova decisão e triplica a
exposição autorizada), depender somente de pausa posterior (risco de excedente)
ou usar orçamento vitalício do conjunto (proteção nativa e sem elevar teto).
Escolhida a terceira, somente para a recusa explícita conhecida. A verba fica
limitada ao teto e aos dias restantes: R$ 20 no último dia do #91. Antes de criar
anúncios, a releitura confirma campanha, teto e término; divergência interrompe
a publicação e limpa os objetos incompletos. O callback persistirá orçamento
vitalício pelo contrato existente. O SDK oficial confirma `lifetime_budget` no
contrato de ad sets: https://github.com/facebook/facebook-python-business-sdk/blob/main/facebook_business/adobjects/adset.py.
A referência HTML da Meta retornou HTTP 429; o mínimo de R$ 300 provém da
resposta real persistida, não de inferência sobre uma tabela universal de moedas.

A matriz foi ampliada para recusa do mínimo, confirmação divergente, orçamento
sem acúmulo dos dias perdidos e preservação do objetivo SALES com degustação.
A fixture inicial do teste novo tinha copy sem relação com sua recompensa e foi
corretamente bloqueada; a fixture foi corrigida, sem relaxar o gate. A suíte do
worker passou em seguida com 129 testes. As duas novas rodadas completas
`final-1` e `final-2` passaram consecutivamente após a última correção: em cada
rodada, 2.369 testes backend (três ignorados pela suíte), 488 frontend e 129 do
publicador, além de typecheck, build e seis jornadas de navegador. Os términos
das rodadas foram 22:39:54 e 22:45:56 UTC. Não houve nova alteração funcional
após essas rodadas.

## Confirmação externa sem publicação e entrega local

Com a credencial operacional obtida pelo contrato do backend, `POST /adsets`
com `execution_options=["validate_only"]` retornou HTTP 200/`success=true`
para orçamento vitalício de R$ 20, Purchase, pixel do #91, placements exclusivos
do Instagram e término em 07/09/2026 02:59:59 UTC. A consulta referenciou a
campanha histórica #88, já `PAUSED` e com objetivo SALES; nenhuma campanha ou
conjunto foi alterado. O número de conjuntos permaneceu um antes e depois.
Isso valida os parâmetros na Meta e não equivale a publicar o #91.
A primeira consulta com a credencial de leitura da sandbox havia sido recusada
por permissão; a confirmação usa a mesma fonte de credencial do publicador.
Resultado sanitizado: `artifacts/vega91-publication-recovery/meta-budget-validation.json`.

A Meta também confirmou `video_status=ready` para o vídeo 1978283913145425.
Upload, processamento e disponibilização da mídia estão completos; a campanha
do #91 continua inexistente. O anúncio #527 permanece `READY/APPROVED`, o #526
`REJECTED/APPROVED` com justificativa de substituição, e o #91 permanece `FAILED`.
O teto cadastrado segue R$ 100 e o diário R$ 20; não houve gasto de mídia do #91.
O #90 não foi alterado.

As três imagens finais foram construídas pelos Dockerfiles do repositório,
verificadas em Compose isolado e exportadas com SHA-256 para
`artifacts/vega91-publication-recovery/images/`. O smoke confirmou frontend,
Java 21 e FFmpeg do publicador. Containers, volumes, rede e referências das
imagens temporárias foram removidos; os arquivos exportados permanecem prontos
para uma única publicação autorizada. Não ocorreu deploy de código: a chave
privada SSH necessária não foi disponibilizada nesta execução.

O aceite operacional completo continua pendente: instalar as três imagens,
liberar pela UI dentro de uma janela autorizada, confirmar campanha/conjunto/
anúncio na Meta e callback real que coloque o #91 em `RUNNING`. Não é necessário
um novo ciclo de investigação ou PR por defeito; a entrega local reúne todas as
correções identificadas nesta execução.
