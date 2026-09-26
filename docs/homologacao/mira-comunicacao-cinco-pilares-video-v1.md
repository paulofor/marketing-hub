# Homologação — comunicação de Mira e decisão de vídeo v1

Data: 2026-09-26. Escopo: produto Mira #10, experimento #93, comunicação #95 e produção criativa
#64. O usuário autorizou teto audiovisual de R$ 80 e aprovou o ciclo; isso não autoriza campanha,
publicação, compra de créditos ou mídia. O teto operacional será convertido pela PTAX registrada e
arredondado para baixo. Cotação de venda PTAX de 25/09/2026: R$ 5,199100 por US$ 1; teto
operacional conservador: US$ 15,38 (o quociente integral é US$ 15,387278...). Fonte:
`https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarDia(dataCotacao=@dataCotacao)`.

## Critérios comerciais

| Critério | Entregável vigente | Evidência de aceite | Métrica futura |
|---|---|---|---|
| Desejo reconhecido | Organizar produtos de skincare já disponíveis com menos esforço e clareza | `DESEJO_RECONHECIDO` na tarefa #502, dossiê #36 e estratégia V3 | impressão humana atribuída → primeira interação → início |
| Primeiro passo fácil | Entrada móvel em três passos, progresso, retomada e bloqueio explicável | `PRIMEIRO_PASSO_FACIL`, prova real `mira-private-v3` | início → conclusão → primeiro valor; abandono e tempo |
| Valor antes do compromisso | Demonstração identificada com pixels autorizados da versão real | `VALOR_ANTES_DO_COMPROMISSO`, tarefa técnica #371 e artefatos 95–99 | demonstração vista → CTA → início; uso separado de exibição |
| Continuidade paga | Rotina completa, preço hipotético único de R$ 49, duas tentativas máximas e limites | `CONTINUIDADE_PAGA`; checkout real, pagamento e reembolso continuam como gates | oferta vista → CTA → checkout → compra → entrega → uso |
| Repetição com margem | Uma variável: cartão estático versus vídeo curto; teto audiovisual autorizado de R$ 80, sem confundir com o custo máximo de R$ 14 por resultado útil | `REPETICAO_COM_MARGEM`, Plutus, autorização original em BRL e plano #8 | compras líquidas, CAC, custo integral, reembolso, contribuição e margem |

## Alternativas de vídeo

1. Somente cartão estático: baixo custo e prova direta, mas menor clareza do percurso.
2. Geração externa paga: maior riqueza visual, mas exige autorização, orçamento e aumenta risco de
   representar prova inexistente.
3. Demonstração editorial curta com capturas reais: esforço moderado, alta fidelidade, custo externo
   visual esperado de zero e comparação limpa com o estático. Escolhida por Íris e adotada porque
   preserva margem; Apolo só executa após preflight persistido, Plutus e autorização humana.

## Matriz técnica local

| Caso | Resultado esperado |
|---|---|
| Construção com `harness.audiovisualRequired=false` | conclusão determinística, custo e provider zero |
| Comunicação com harness falso e rota criativa verdadeira | prevalece a rota; bloqueio `AUTHORIZATION_REQUIRED` sem provider |
| Autorização de R$ 80 convertida para USD | valor/moeda, cotação, fonte e data persistidos; teto USD nunca supera a conversão arredondada para baixo |
| Movimento editorial local | preflight `LOCAL_EDITORIAL`, reserva e custo visual zero, sem chamada de storyboard por IA |
| Prova privada homologada | leitura pelo backend com tenant e SHA-256; divergência bloqueia antes do ffmpeg |
| Ciclo concluído da mesma versão | recibo mínimo retorna à tarefa #505; Apolo não regenera a peça |
| Referência de experimento com contrato geral V2 e protótipo aceito V3 | comunicação usa versão, URL e contexto V3; checkout histórico não é exposto |
| Rota ausente, incompleta, histórica ou de outra definição | `MISSING_CONTRACT`; nenhuma inferência |
| Resposta especializada de Apolo | somente identidade e decisão mínima; sem contexto histórico amplo |
| Callback BPM | caminho do contrato, IDs e custo conhecido do ciclo; nenhuma nova chamada externa durante o callback |
| Retomada da tarefa | no máximo uma execução determinística por tarefa; nenhuma chamada de modelo |
| Tela desktop, iPhone e Pixel | cinco pilares, estado e bloqueio legíveis, sem ação automática de mídia |

Dados de QA, agentes, bots e duplicidades não contam como cliente, compra ou receita. O aceite
técnico desta matriz não comprova vendas nem lucro.

## Evidência operacional da primeira tentativa

- Perfil #61, projeto #6 e ciclo #23 foram criados pela interface com teto original de R$ 80,
  PTAX de R$ 5,199100 e limite operacional conservador de US$ 15,38.
- Plutus aprovou a rota `LOCAL_EDITORIAL:editorial_motion@v1`; o job #21245 foi criado somente
  depois da aprovação do roteiro #562.
- O job falhou antes de materializar um ativo porque o descritor isolado de produção não habilitava
  o bean `EDITORIAL_MOTION`. O ledger liquidou a reserva com zero crédito e US$ 0 de custo.
- A correção adiciona a variável ao Compose efetivamente usado no host e um teste de contrato que
  renderiza esse descritor. Uma nova tentativa deve referenciar o ciclo #23, preservar o mesmo teto
  agregado de R$ 80 e não interpretar a falha técnica como aprendizado comercial.

## Evidência operacional do ativo final e retorno à cadeia

- O ciclo substituto #24 preservou o teto de R$ 80, a PTAX registrada e a rota editorial local. O
  job fonte #21246 e o acabamento final #21249 produziram o ativo #47, aprovado por revisão humana,
  sem nova geração visual externa.
- O vídeo final tem 24 segundos, 1080 × 1920, H.264/AAC, legendas VTT e HLS. A comunicação começa
  pelo desejo de organizar os produtos já disponíveis, mostra a tela real antes da oferta e explica
  o pacote individualizado de R$ 49, suporte e uma correção técnica.
- A tarefa #521 expôs uma falha de identidade antes do retorno: a comunicação do experimento ainda
  recebia `mira-private-v2`, enquanto projeto, prova e vídeo pertenciam a `mira-private-v3`. A
  correção faz os processos pré-comerciais partirem do protótipo privado aceito e recusa o checkout
  histórico até sua homologação.

## Ajuste do criativo estático após revisão independente

- Psique pediu que o cartão diga explicitamente “aplicação web privada”, apresente a entrega como
  rotina digital consultável e preserve legibilidade no feed móvel. A correção automática #524
  aplicou copy e fonte visual novas, mas o renderizador publicado ainda substituía o rótulo correto
  por “experiência privada”.
- Foram comparadas três opções: trocar somente a copy, com risco de o template repetir o erro;
  substituir o PNG manualmente, quebrando a linhagem; ou corrigir o template determinístico e
  regenerar pelo processo oficial. A terceira foi escolhida por eliminar a causa-raiz, preservar
  hash, origem e revisão independente e não exigir nova geração audiovisual paga.
- O contrato do renderizador e o prompt passam a declarar “APLICAÇÃO WEB PRIVADA” e a recusar
  recortes ultralargos que reduzem a prova a uma faixa pequena cercada por espaço vazio. A nova
  peça precisa continuar mostrando pixels autorizados de `mira-private-v3`, ser conferida em
  393 px e voltar aos gates de Psique e Têmis antes de qualquer publicação.
