# Vega — recuperação da continuidade do processo 6

Data: 12/09/2026. Produto 4, cadeia 14/v14, processo 75/v6, ciclo 2, experimento 92,
versão `musa-pde-entry-v12-primeiro-ajuste-aplicavel`.

## Evidência e diagnóstico

A tela e o MCP confirmaram a execução #4 em `WAITING_ACTIVITY`, sem tarefas pendentes.
O ciclo permanece em `ADJUSTMENT`, revisão 6. O gate multiagente #258 está concluído
em `experiment:92`, com produto 4, versão v12 e cinco provas independentes. A integração
privada #270 está concluída e declara checkout simulado, tráfego `AGENT_VALIDATION`,
sem autorização de publicação, campanha ou cobrança. O processo 4 está concluído.

A orientação do ciclo ainda apresenta `prototypeCorrection` quando todos os trabalhos
delegados terminaram. O consumidor de homologação consulta exclusivamente
`product:4@agent-validation-v1`, descartando o gate do experimento. O histórico #1,
os contratos legados e os loops `LOOP-BPM-CICLO-SEM-CHAMADA-DO-PAI` e
`LOOP-BPM-DISPARO-MANUAL-SEM-CONTROLE-DE-PROCESSO` foram consultados.
Logs MCP retornaram zero linhas para os filtros `ProcessRun` e `/automation/v1`;
a consulta sem filtro confirmou acesso e logs atuais, sem evidência de falha de worker.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo/esforço | Decisão |
| --- | --- | --- | --- |
| Registrar apenas a conclusão do ajuste pela tela | Pequeno esforço; utiliza comando existente | A próxima homologação continua sem reconhecer a prova vigente | Insuficiente isoladamente |
| Executar novamente os agentes de homologação | Produz avaliação atualizada quando há mudança real | Custo e repetição sem mudança de entrada; não corrige o consumidor | Não indicada para este impedimento |
| Corrigir consumidor e orientação e registrar provas pelo fluxo oficial | Preserva a linhagem e evita trabalho pago repetido | Esforço moderado com regressões de identidade e legado | Escolhida |

Não se transforma preparação privada em prontidão comercial. Orçamento proposto não é
autorização; operação, entrega e resultado dependem de publicação e fatos comerciais reais.

## Matriz definida antes dos testes

| Área | Critério de aprovação |
| --- | --- |
| Homologação | Gate vigente do próprio experimento aparece na seleção e é aceito pelo comando; referência legada válida continua compatível |
| Rejeições | Outro produto/experimento/versão, prova com fonte divergente, gate anterior ao ajuste ou reprovação posterior não liberam homologação |
| Continuidade | Trabalho pendente conserva processo/cadeia/ciclo; trabalhos comprovadamente concluídos direcionam ao registro de evidência, sem mandar refazer correção antiga |
| Estado desconhecido | Processo ausente ou sem atividade atual não equivale a todos os objetivos comprovados |
| Integração e persistência | REST e MySQL 5.7 locais: ajuste → briefing → dois vídeos → revisão → homologação → autorização pendente; idempotência, revisão e histórico preservados |
| Gates comerciais | Sem confirmação humana não há autorização; sem publicação não há operação, venda, entrega ou conclusão do processo 6 |
| Observabilidade | Eventos registram referências e autoria real do registro; tentativas e custos anteriores preservados; nenhum agente pago repetido |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7: formulário, gate, erro/retry e retorno ao processo pai no mesmo ciclo |
| Segregação | Somente fixtures locais; nenhuma venda, receita ou observação humana inferida de testes; integrações externas simuladas |
| Entrega | Testes relevantes, suíte backend completa, revisão do diff, JAR e imagem correspondentes ao código validado; coordenação antes da intervenção |

Após a última correção, executar duas rodadas locais completas e consecutivas sem falhas.
Os resultados e a situação publicada serão registrados após execução, sem antecipar sucesso.

## Ampliação do diagnóstico antes da validação final

O subprocesso 76/v4 inclui as etapas VIDEO_BRIEF, CAMPAIGN_VIDEO, PDE_ENTRY_VIDEO e
VIDEO_APPROVAL. O MCP contou **zero ativos de vídeo no experimento 92**. O planejamento
persistido (evento 5) declara que teto técnico e custo variável são hipóteses, não autorização
de gasto. O cadastro do experimento tem `media_spend_limit = NULL` e estado `PLANNED`.
Essas pendências não serão removidas para concluir artificialmente o processo.

Na fronteira de identidade foram comparados três caminhos: aceitar somente `experiment:*`
(quebra o contrato privado legado), unir fontes e procurar qualquer aprovação (risco de usar
outra passagem) e reutilizar o resolvedor canônico da execução (baixo esforço e fonte única).
A terceira opção foi aplicada: seleção e comando usam o mesmo contrato do produtor, sem
fallback entre experimentos; provas contextuais também precisam declarar sua fonte no JSON.


## Revisão dos executores e limite de continuidade

Os requests e resultados persistidos de Dédalo, Psique, Têmis e Íris preservam versão,
fonte, auditoria e papéis independentes. A incompatibilidade está no consumo do gate pelo
ciclo; repetir as mesmas avaliações não elimina essa causa. O contrato compartilhado e
os testes protegem a passagem sem alterar os critérios dos agentes.

O monitor pós-deploy e os slots também foram consultados: nenhum slot comercial tem
`sourceExperimentId=92`; o slot v7 ativo pertence à versão histórica e ao experimento 90.
A URL privada v12 não deve ser confundida com esse destino comercial.

A produção audiovisual posterior deverá mostrar uma microação real da v12, usando itens
que a cliente já possui. Proposta para avaliação: vídeo AD apresenta situação cotidiana,
mostra o primeiro ajuste e convida a experimentar; vídeo LANDING_HERO demonstra como aplicar,
a ocasião e o critério de autoavaliação, preservando acesso ao CTA sem reprodução obrigatória.
São orientações propostas, não peças produzidas nem briefing aprovado por Íris. A métrica
principal permanece primeiro resultado por início, seguida de checkout, vendas líquidas,
entrega e contribuição; visualizações de vídeo são auxiliares. Não há resultado comercial
medido neste sucessor nem atribuição causal válida com a amostra histórica de quatro sessões.

A publicação excepcional será restrita ao backend, após os testes, usando o Dockerfile e
Compose do repositório. A pré-checagem somente leitura confirmou Compose idêntico no host,
seleção de um único serviço e preservação de configuração. A imagem anterior é mantida para
retorno. A autorização excepcional não será usada para render pago, mídia ou cobrança.


O monitor do #92 retorna `PDE_ANALYTICS_SLOT_REQUIRED`: falta o slot comercial desse
experimento, não há evidência de indisponibilidade do protótipo privado. Gasto, receita e
vendas desse sucessor não devem ser apresentados como zero medido enquanto a fonte não
existe. O predecessor mantém suas quatro sessões e zero vendas reconciliadas, separadamente.


## Escolha da aplicação operacional

Foram comparadas a publicação ampla de backend/frontend/worker (inclui serviços sem alteração),
a recriação manual a partir de `docker inspect` (duplica o Compose) e a aplicação restrita do
backend pelo Compose versionado (preserva configuração, serviços adjacentes e retorno).
A terceira foi escolhida. `infra/testing/vega-process6-recovery/apply-backend.py` exige o
hash do Compose, a imagem desta recuperação e o serviço/container esperado. A configuração
sensível existe apenas em memória; três testes locais cobrem preservação e rejeições.
A pré-checagem no host retornou somente `backend`, sem fazer alterações.


## Ajustes na preparação local

A compilação inicial identificou um import ausente no helper compartilhado; o import foi
incluído antes da matriz. O teste novo de orientação também precisou fornecer a cadeia ao
service real. A fixture de homologação foi alinhada ao produtor: usa a referência canônica
do ciclo e permite simular prova divergente e produto alheio. As 32 verificações focadas
passaram antes das rodadas completas. Nenhuma publicação ocorreu durante esses ajustes.

## Resultado das duas rodadas locais

As rodadas `round1` e `round2` terminaram com `RODADA COMPLETA APROVADA` e retorno zero.
Cada rodada aprovou 2.774 testes backend (oito ignorados preexistentes), 629 frontend e
33 do executor Atena, além de 19 controles REST/MySQL do ciclo e seis de decisão.
As matrizes de navegação de ciclo, cadeia, histórico, decisão e venda passaram em Chromium
desktop, iPhone 15 Pro e Pixel 7. MySQL 5.7 aprovou migração, reaplicação e idempotência.
Os containers, a rede e os volumes de teste foram removidos em cada rodada.

O empacotamento posterior conferiu identidade das 3.934 classes testadas, 414 recursos e
inicialização do catálogo com 209 cartões. Os três testes do aplicador operacional passaram
duas vezes. Não houve alteração funcional entre as rodadas ou depois delas.
Evidências brutas em `artifacts/vega-processo6-recovery/round1` e `round2`;
manifesto do código validado: `7306e2b51fed49b91cdb464838463f255421e3f2e64fed523eea359ca714116d`.

A consulta a `experiment_budget` e `experiment_financial_decision` não encontrou orçamento
ou autorização do experimento 92. O teto proposto do planejamento não supre esse registro.
A homologação técnica está concluída; o processo comercial completo ainda tem requisitos reais.

## Retorno sem ciclo identificado na conferência publicada

Antes de aplicar qualquer imagem, a verificação visual encontrou o `parentUrl` do catálogo
sem `learningCycleId`. O histórico da API anterior já continha essa omissão. O teste local
verificava produto/cadeia e o retorno, mas não exigia a ocorrência no URL. O ciclo 2 foi
registrado pela UI com as provas existentes (evento 10, revisão 7, VIDEO_BRIEF); a imagem
anterior permanece em execução enquanto uma publicação Actions já iniciada está na fila.

Alternativas: acrescentar apenas o ciclo aberto no backend (simples, mas troca o histórico),
montar o contexto na interface (menor esforço, duplica a identidade) ou consultar o catálogo
com a ocorrência selecionada (custo moderado, valida produto/cadeia e preserva o BPM real).
Foi escolhida a terceira, com o retorno oficial contendo `learningCycleId`.

A matriz passa a exigir também o retorno de um predecessor encerrado quando já existe
sucessor aberto, mantendo os IDs no link, na consulta e na chegada ao pai; produto/cadeia
divergentes e ciclo inexistente precisam ser recusados. São verificações REST/MySQL e
desktop/iPhone/Pixel. As duas rodadas anteriores são preliminares: após esta correção serão
executadas duas novas rodadas completas. Backend e frontend serão aplicados juntos, após
a homologação final, pelo Dockerfile e Compose versionados.

A pré-checagem somente leitura do frontend identificou que o Compose integral exige
variáveis do backend mesmo quando a aplicação seleciona apenas frontend. O aplicador
passou a resolver o Compose com os valores existentes dos dois serviços em memória,
priorizando os do serviço selecionado e sem transportá-los para o outro container.
Cinco testes do aplicador e a nova pré-checagem passaram. A rodada 3 é diagnóstica;
as duas rodadas finais deverão começar depois deste último ajuste operacional.

A publicação anterior `34717183163` terminou com sucesso em 12/09/2026. O coordenador
`a47006925d7b4e2e9b9b735a3dd9437d` alcançou ACTIVE com quatro publicadores APP pausados,
fila vazia e sem cancelamento. Nenhuma imagem desta recuperação foi aplicada até aqui.

A revisão final preservou a precedência da cadeia explicitamente informada na URL.
O catálogo recusa ciclo/cadeia divergentes, em vez de trocar silenciosamente a cadeia.
A inferência pela cadeia do ciclo permanece somente quando a URL não informa cadeia.

A rodada diagnóstica 3 confirmou os 20 contratos REST/MySQL, mas a asserção nova de
navegação esperava a mensagem anterior à criação do sucessor. A API e a interface mostraram
o predecessor correto concluído e “Sucessor vinculado”, conforme `SalesFlowResolver`.
A expectativa foi corrigida para esse estado, preservando a checagem do ID histórico,
a navegação de ida e volta e a ausência de alteração dos dados. Não houve defeito funcional
nessa mensagem nem publicação de imagem para descobrir o comportamento.

O executor Node também foi conferido. A ferramenta MCP de logs Java não inclui esse módulo
em seu catálogo, nem os alvos Docker permitidos para logs; a leitura do container pelo
`sandbox-ssh` confirmou chamadas de conciliação do `runId=4`, com retorno WAITING_ACTIVITY.
Os seis testes do `process-execution-worker` aprovam pending, correlação, concorrência,
falha isolada e heartbeat. Não há evidência de paralisação ou necessidade de reexecutar IA.

## Sugestões comerciais, sem execução nesta recuperação

| Prioridade | Proposta | Evidência e limite | Validação | Impacto / esforço |
| --- | --- | --- | --- | --- |
| 1 | Demonstrar uma microação real logo no início dos dois vídeos e manter CTA para experimentar | Íris #402 já aprova essa mensagem; não há efeito comercial medido no #92 | Primeiro resultado por início; depois checkout e vendas líquidas | Alto potencial / baixo esforço de roteiro |
| 2 | Usar salvamento e retomada como continuidade do mesmo resultado, sem exigir nova geração | v12 e gate #258 comprovam a função tecnicamente; adesão real ainda não observada | Retomadas que chegam a primeiro resultado, checkout e uso após entrega | Médio potencial / baixo esforço de comunicação |
| 3 | Dimensionar aquisição pela contribuição líquida e entrega após autorizações próprias | #91 tem só quatro sessões, zero vendas e contribuição negativa; não comprova causa ou público vencedor | Vendas líquidas entregues, CAC observado e contribuição por coorte, excluindo testes | Alto potencial / esforço médio de medição |

São hipóteses de melhoria e critérios futuros. Nenhuma afirmação de ganho medido, campanha
ou mudança simultânea de público, canal, preço e oferta foi produzida nesta solicitação.

## Aceite local final

As rodadas **4 e 5** passaram integralmente e consecutivamente, depois da última correção,
com o mesmo manifesto `9b2c6ea9d2c884a8103954dae219d353bde5d014b936ef8d163f2c8b7e3a2fc1`.
Cada rodada inclui 2.775 testes backend (oito ignorados preexistentes), 630 frontend,
33 Atena, seis do executor de processos e cinco do aplicador. Os 20 controles REST/MySQL,
seis de decisão e todas as matrizes de navegador passaram, incluindo o predecessor
encerrado com sucessor aberto em desktop, iPhone 15 Pro e Pixel 7. Migração, reaplicação,
idempotência, formatação e revisão do diff aprovadas; topologias temporárias removidas.

O escopo final da aplicação é **backend e frontend**, ambos com seus Dockerfiles e o
Compose do repositório. Os executores não precisam de nova imagem. A transferência foi
verificada localmente contra checksum, duplicação, truncamento e excesso de bytes.
O registro de aplicação e os resultados publicados serão acrescentados abaixo.
