# Produção autônoma de vídeo por Apolo e Plutus v1

## Objetivo

Apolo é o executor criativo dos vídeos do Estúdio. Plutus é o gate financeiro independente de cada ciclo. O backend é a fonte de verdade da fila, da decisão, do custo e do avanço.

## Fluxo canônico

1. Um usuário solicita pelo Estúdio um ciclo associado a produto, planejamento, projeto e perfil de vídeo.
2. O backend persiste o ciclo como `PENDING_PROVIDER_PREFLIGHT` e publica a pendência de consulta da conta e das rotas candidatas para o executor de vídeo.
3. O executor realiza somente leituras e simulações sem cobrança, e reporta ao backend o snapshot sanitizado de saldo, quota, elegibilidade e custo previsto.
4. Com o snapshot vigente, o backend move o ciclo para `PENDING_FINANCIAL_REVIEW` e abre na mesa de Plutus uma tarefa de gate `VIDEO_PROVIDER_COST_BENEFIT_APPROVAL`, com Apolo como solicitante.
5. Plutus decide pelo contrato formal do gate; alterar apenas o status da tarefa não libera provider nem consumo.
6. Nenhum job pago de provider existe antes da decisão financeira.
7. Somente a identidade técnica `financial-agent` pode registrar `APPROVED` ou `REJECTED`, sempre com motivo auditável.
8. Um preflight integralmente apto reserva preventivamente a soma dos tetos duros por geração — nunca apenas a estimativa otimista — sem criar job ou consumir o provider. A aprovação de Plutus usa essa reserva ainda vigente e cria em modo `TEST` o job do executor oficial de vídeo; uma rejeição libera a reserva e termina em `FINANCIAL_BLOCKED`.
9. Apolo planeja, gera, inspeciona e devolve o candidato. O provider não decide próxima etapa.
10. O custo conhecido deve ser conciliado no ledger. Novo consumo é bloqueado ao atingir o teto ou quando o custo do ciclo atual estiver desconhecido. Cobertura histórica incompleta deve gerar pendência de conciliação, mas não bloqueia sozinha um ciclo de descoberta com teto explícito, ledger segregado e custo incremental rastreável.
11. QA independente decide qualidade. Apolo não aprova o próprio trabalho.
12. Quando o job vinculado terminar em falha, o backend deve preservar no ciclo o job, código, detalhe e horário da falha antes de reconciliar uma nova tentativa. O painel deve exibir a falha anterior e o novo job separadamente; o estado `QUEUED_FOR_APOLLO` nunca pode ocultar um job terminal falho.

## Preflight de conta e rota

Por decisão comercial de 2026-09-03, o gate de Plutus deve receber antes da decisão financeira um
snapshot operacional sem custo produzido pelo executor de vídeo. O snapshot separa modelo,
agregador, conta de créditos e rota, e comprova saldo, quota, concorrência, preço vigente,
elegibilidade do payload e custo previsto. A especificação completa está em
`plutus-model-provider-pricing-canon.v1.md`.

O executor consulta a plataforma porque já possui o segredo operacional; o backend somente publica
a pendência e persiste o resultado; Plutus compara e decide; Apolo executa após aprovação. Plutus
não deve receber segredo nem chamar o provider de render diretamente. Falta de saldo pode gerar uma
recomendação de recarga ao usuário, mas nunca compra automática, autobilling ou criação antecipada
de job pago.

O payload faturável deve ser estruturalmente equivalente ao payload do dry run, exceto pela ausência de
`dryRun`. Receitas oficiais que não documentem `dryRun` só podem entrar pelo mesmo fluxo quando a
versão estiver fixada, o payload exato estiver congelado e o custo máximo puder ser recalculado
deterministicamente no backend a partir de duração, proporção e tabela oficial versionada, sem chamar
o endpoint faturável no preflight. Cada rota persiste modelo ou receita, fabricante, agregador, conta, configuração, preferência,
estimativa e teto. A resposta faturável deve repetir modelo, fabricante, configuração, preferência e
teto; divergência interrompe as cenas restantes e preserva a task já aceita para conciliação. A
reserva precisa estar vigente no início de cada cena, reservas vencidas sem consumo devem ser
liberadas sob lock da conta e nenhum retry pago automático é permitido.

Configurações Runway são recursos externos previamente provisionados. O código referencia slugs
imutáveis, mas não cria ou altera allowlists automaticamente. Somente modelos `ACTIVE` com adapter,
preço, licença comercial e QA verificados no catálogo do Marketing Hub podem superar o gate; modelo
novo escolhido pelo Router permanece bloqueado até homologação explícita.

Por decisão de 2026-09-04, `RUNWAY_PRODUCT_UGC` é a primeira rota de receita. Ela usa
`product_ugc@2026-06`, uma imagem licenciada da apresentadora, uma captura limpa do PDE, no máximo 15
segundos e áudio nativo desligado. Plutus reserva o custo contratual integral antes de Apolo; a
pós-produção deriva locução, legenda queimada e VTT da mesma sequência de palavras. Falha de
estabilidade, divergência entre texto e voz, ausência de duração por trecho ou narração maior que o
vídeo bloqueia o candidato e jamais abre retentativa paga automática. Os limites das legendas
premium vêm das durações físicas dos próprios trechos narrados, não de divisões iguais ou estimadas.
Para a locução natural, o executor usa o snapshot versionado `gpt-4o-mini-tts-2025-12-15` e a voz
`marin`, mantendo o texto aprovado como fonte única. Cada chamada por trecho persiste o request sem
segredo e o binário bruto da resposta como ativo de áudio ligado ao job, com tipo, tamanho, request
ID quando existir e SHA-256. Como o endpoint de Speech não devolve uso por request, o custo fica
`PENDING_PROVIDER_RECONCILIATION`; tabela pública não autoriza estimar débito como zero. O vídeo
exibe de forma legível que a voz foi gerada por IA. O endpoint não aceita `service_tier`, e essa
exceção funcional deve constar na auditoria em vez de enviar campo não suportado.

O limite genérico de dez segundos das rotas Runway de clipe não se aplica à receita fixada
`RUNWAY_PRODUCT_UGC`, cujo contrato aceita até quinze segundos. A validação de duração do backend
deve resolver primeiro a identidade da receita e somente depois aplicar o fallback genérico do
agregador; assim, uma aprovação financeira íntegra não é desfeita durante a criação transacional do
job.

Quando a rota paga depender dessa finalização premium, o preflight deve confirmar previamente que
pós-produção, TTS natural, modelo, voz e credencial estão configurados. A ausência de qualquer item
bloqueia antes da reserva e da chamada Runway; concluir o vídeo visual sem capacidade de produzir o
áudio aprovado criaria um custo irrecuperável e não é permitido.

Por decisão operacional de 2026-09-03, o Estúdio também deve oferecer um preflight isolado. Esse
comando consulta saldo e quota e executa o `dryRun`, mas encerra o ciclo sem criar reserva, tarefa de
Plutus ou job de Apolo, ainda que a conta possua saldo. O teto informado nesse modo é somente um
limite analítico para avaliar o Router e não constitui autorização financeira. Produção e preflight
isolado devem usar endpoints e estados distintos para impedir que uma verificação avance por engano.

O parecer de Plutus deve registrar prompt, resposta bruta, modelo e uso antes do callback funcional.
Se o callback falhar, a próxima leitura reutiliza a resposta auditada e não consome uma segunda
interação de IA.

A identidade da recomendação não é texto livre do modelo: o executor financeiro deve copiar
`recommendedAggregator` do agregador persistido no preflight e `recommendedRoute` do `batchRouteId`
exato das rotas selecionadas. Para Model Router, a rota começa com `RUNWAY_ROUTER:`; para a receita
Product UGC, começa com `RUNWAY_PRODUCT_UGC:`. A resposta bruta de Plutus permanece auditada, mas o
callback funcional usa a identidade canônica do snapshot para impedir divergência de prefixo,
repetição de modelo e reserva parada.

## Contrato financeiro da fase de descoberta

Enquanto o produto ainda estiver produzindo materiais para descobrir qual combinação de mensagem, formato e provider funciona, Plutus deve avaliar o ciclo como investimento de aprendizado, e não exigir retorno ou venda prévia como condição de aprovação.

- Retorno igual a zero, ausência de vendas e ROI ainda não comprovado são esperados antes de existir material testável e não constituem, isoladamente, motivo para rejeição.
- Plutus deve aprovar gasto incremental controlado quando existir objetivo de aprendizado explícito, teto autorizado pelo usuário, ledger segregado do ciclo e capacidade de interromper novo consumo ao atingir o limite.
- Para o grupo inicial `musa-two-video-funnel-v1`, o teto autorizado é de US$ 20 no total, distribuído em até US$ 10 por vídeo; esse limite não autoriza excedente nem compra de créditos.
- Custos históricos conhecidos sem atribuição continuam como dívida obrigatória de conciliação, mas não devem ser somados ficticiamente ao novo ciclo nem bloquear sozinhos a descoberta quando o custo incremental novo puder ser medido desde a primeira tentativa. Por decisão comercial de 2026-08-12, custos anteriores a 2026-08-13 que permanecem irrecuperavelmente desconhecidos são assumidos em USD 0 com evidência auditável; esse fechamento histórico não autoriza estimar como zero qualquer consumo novo.
- Plutus deve bloquear quando faltar rastreabilidade do custo novo, houver risco de ultrapassar o teto, o objetivo não produzir aprendizado verificável ou o ciclo tentar publicar/consumir mídia sem autorização.
- A métrica operacional é material tecnicamente válido por dólar consumido. Deve-se continuar enquanto houver aprendizado dentro do teto, ajustar provider ou abordagem quando qualidade/custo falhar e parar ao atingir US$ 20, perder rastreabilidade ou concluir os dois candidatos válidos.

## Escopo operacional de Apolo v2

Apolo possui acesso operacional completo ao Estúdio de Áudio e Vídeo depois da aprovação financeira: roteiro, storyboard, imagens mestre aprovadas, geração de cenas, seleção de provider por cena, continuidade, narração, trilha, montagem, legendas, HLS, inspeção técnica e iteração causal. Esse acesso não remove os gates de Plutus, o ledger nem o QA independente.

O financeiro deve separar custo estimado, custo contratual calculado pela tabela vigente e débito confirmado pelo provedor. Status de sucesso ou falha da task, sem valor financeiro retornado pelo provider, não comprova débito real nem saldo oficial. Tentativas sem evidência permanecem desconhecidas e bloqueiam nova geração automática quando puderem comprometer o teto aprovado.

A primeira missão v2 é concluir os dois projetos persistidos do grupo `musa-two-video-funnel-v1`: vídeo de qualificação da campanha e vídeo hero de conversão do PDE. Cada vídeo mantém objetivo, métrica e CTA próprios; a conclusão exige montagem narrativa, áudio pt-BR, legendas mobile, HLS, inspeção técnica e entrega ao QA.

Por decisão comercial de 2026-08-13, Apolo não pode selecionar Luma para o MUSA, inclusive quando um projeto legado ainda a mencionar. O padrão é `RUNWAY_SEEDANCE_2_5`, com Runway Gen-4 como alternativa explícita quando Seedance não atender ao contrato técnico ou ao QA. Vídeos acima da duração direta do modelo devem ser produzidos por cenas auditáveis e montagem; nunca por truncamento silencioso. A troca de provider preserva o teto já aprovado, mas não autoriza compra de créditos, publicação ou mídia.

A duração máxima gerada pelo provider não define a duração dos cortes exibidos. Antes de consumir créditos, Apolo deve persistir no Estúdio um plano de cortes com função comercial, duração e objetivo visual por tomada; o executor agrupa esses cortes em clipes compatíveis com a capacidade específica do modelo e a pós-produção realiza a montagem. Texto, legenda, interface, preço e CTA são overlays determinísticos de pós-produção e não podem ser delegados ao modelo de vídeo. A tela deve mostrar separadamente quantidade de clipes cobrados, duração máxima por clipe e quantidade de cortes editoriais.

Por decisão comercial de 2026-08-13, todo render pago de ciclo autônomo deve passar por um planejador de IA de Apolo antes do provider. A IA atua como diretora criativa: transforma o contexto persistido em storyboard estruturado, mas não possui autoridade para aprovar custo, repetir geração ou publicar. O executor valida deterministicamente quantidade e duração dos cortes, diversidade visual, cobertura de dor, resultado, mecanismo e CTA, ausência de texto embutido e custo previsto contra o teto aprovado por Plutus. Ausência de credencial, resposta inválida, redundância ou orçamento excedido bloqueia o provider. Request e response brutos, modelo, plano, custo previsto e decisão do gate devem permanecer auditáveis no job e visíveis no storyboard do Estúdio.

Por decisão comercial de 2026-08-14, novo crédito só pode ser consumido depois de roteiro aprovado com gancho e CTA, duração compatível com a capacidade do modelo, plano de pelo menos cinco cortes e arco narrativo progressivo de gancho até CTA. Cada corte deve declarar fase narrativa e âncora de continuidade; o gate bloqueia retrocesso da história, ausência de prova, quebra de continuidade ou retorno ao plano legado de clipes fixos de dez segundos. A qualidade do modelo de planejamento não substitui esse gate e nenhuma sessão ou API de IA pode repetir gasto automaticamente.

## Autoridade e segurança

- Aprovar um ciclo não autoriza publicação, campanha, mudança de preço ou compra de créditos.
- O metadata do job deve declarar `publicationAllowed: false`.
- Um teto é limite, nunca meta de gasto.
- Estimativas, jobs e aprovações não são vendas.
- A primeira operação do MUSA v7 permanece assistida e em `TEST` até comprovar custo, qualidade audiovisual, legibilidade mobile e callback completo.

## Homologação

A matriz cobre: caminho feliz, projeto sem perfil, decisão por identidade indevida, rejeição, decisão duplicada, provider indisponível, custo desconhecido, teto excedido, callback idempotente, QA independente, desktop, iPhone e Android. Providers reais não devem ser chamados na homologação local.
# Conciliação financeira por task do provider

Cada cena aceita por um provider deve possuir identidade própria e liquidação financeira auditável.
O aceite registra a estimativa; o desfecho registra créditos e custo cobrados ou reembolsados,
com a origem da evidência. Saldo calculado por recargas menos liquidações deve ser apresentado
como reconciliado, nunca como saldo oficial do provider quando a API externa não expuser saldo.
