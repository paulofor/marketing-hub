# Matriz de homologação — Runway Product UGC premium v1

## Objetivo comercial

Produzir no Estúdio um criativo vertical premium e reutilizável para o experimento 91, demonstrando
o PDE MUSA como experiência digital com IA e conduzindo ao diagnóstico, sem repetir as causas da
reprovação do ativo 37.

## Gargalo, métrica e decisão

- Gargalo real: o ativo 37 foi rejeitado porque a legenda divergia da locução, houve tremor e a
  composição da personagem com celular diante do espelho ficou irreal.
- Métrica esperada: um candidato de 15 segundos em 1080 × 1920, com conteúdo falado e legenda
  equivalentes, no máximo quatro planos estáveis, custo integralmente reconciliado e
  revisão independente pendente.
- Continuar: preflight recente, rota apta, reserva de no máximo 648 créditos, arquivo aprovado pelos
  gates técnicos de Apolo e registrado como candidato pendente para o subprocesso independente de
  Psique, Têmis e decisão humana. O metadado de revisores exigidos não equivale a parecer executado.
- Ajustar: falha visual ou de comunicação com arquivo preservado; corrigir somente referência,
  conceito ou pós-produção causal antes de novo ciclo explícito.
- Parar: saldo/quota/preço indeterminável, direitos ausentes, teto excedido, payload divergente,
  segunda regeneração automática, promessa indevida ou qualquer tentativa de publicação.

## Alternativas avaliadas

1. Repetir a montagem editorial sem custo de provider: menor custo, mas repete a linguagem que já
   falhou em demonstrar o PDE e não resolve a referência irreal.
2. Gerar vários clipes pelo Model Router: flexível, porém multiplica custo e risco de quebra de
   personagem, ambiente e movimento entre cenas.
3. Usar a receita Product UGC com apresentadora licenciada e uma imagem limpa do PDE: uma única
   requisição faturável com poucos planos sociais estáveis, custo determinístico, duas referências
   explícitas e melhor aderência a Reels.

A terceira alternativa é a rota escolhida. O vídeo do provider será silencioso; voz e legenda vêm
da mesma fonte textual na pós-produção determinística. Cada trecho de voz é medido e define a janela
real da respectiva legenda; o áudio maior que o vídeo bloqueia, e a peça premium não usa o antigo tom
senoidal como falsa trilha. A referência da experiência digital é gerada pelo script versionado
`pde-platform/frontend/scripts/generate-musa-product-ugc-reference.mjs`, sem espelho, aparelho ou
produto físico inventado; o preset aponta para esse arquivo público e para a personagem sintética
já aprovada no catálogo do produto.

O build instala `rsvg-convert` explicitamente e o gerador usa FFmpeg somente como fallback fora do
container. Assim, a imagem não depende do decoder SVG opcional da distribuição do FFmpeg.

Para a copy de 15 segundos, foram comparados manter 29 palavras, acelerar artificialmente a voz ou
encurtar a frase central sem perder dor, mecanismo e CTA. A terceira opção foi escolhida. A locução
de fallback medida fisicamente caiu para uma margem segura dentro dos 15 segundos, enquanto a
pós-produção continua bloqueando qualquer voz real que ultrapasse o arquivo.

## Orçamento de créditos

- Receita fixada: `product_ugc` versão `2026-06`.
- Final: 15 segundos, `1080:1920`, áudio nativo desligado.
- Custo contratual: 208 créditos pelos primeiros 4 segundos + 40 por segundo adicional = 648
  créditos (US$ 6,48).
- Saldo informado antes da execução: 2.020 créditos.
- Saldo mínimo preservado após uma execução aprovada: 1.372 créditos.
- Nenhuma retentativa paga é automática. Novo consumo exige outro ciclo, preflight e decisão de
  Plutus.

## Casos de homologação

| Área | Caso | Resultado obrigatório |
|---|---|---|
| Caminho feliz | Projeto com perfil, duas imagens HTTPS, direitos, roteiro e teto de US$ 6,48 ou maior | Snapshot, cálculo de 648 créditos, reserva, job único em `TEST` e pós-produção automática |
| Preflight isolado | Mesmos dados sem intenção de produção | Consulta saldo/quota e calcula custo, sem reserva, Plutus ou job |
| Referência | Imagem da apresentadora ausente ou tipo diferente de `image` | Bloqueio antes da Runway |
| Referência | Imagem do PDE ausente, não HTTPS, sem direitos ou URL que devolve HTML | Bloqueio antes de consultar a conta Runway |
| Contrato | Versão `unsafe-latest`, duração fora de 4–15 ou proporção diferente das permitidas | Bloqueio determinístico |
| Planejamento | Planejador antigo exige cinco chamadas numa receita faturada uma única vez | Apolo valida a receita pinada sem IA, limita seus cortes internos e preserva os cartões realmente usados |
| Projeto existente | O projeto #3 ainda contém o plano do ativo rejeitado | O preset Vega #91 fica disponível na edição, preenche a rota premium e só persiste após salvar |
| Finanças | Saldo disponível menor que 648, quota ausente ou teto menor que US$ 6,48 | Bloqueio e orientação de Plutus, sem geração |
| Expiração financeira | Reserva vence enquanto o parecer auditado de Plutus ainda aguarda aplicação | Backend libera a reserva, encerra ciclo e gate como bloqueados e a fila segue sem repetir modelo ou criar job |
| Expiração financeira | Reserva do primeiro ciclo ainda está vigente | Reconciliação preserva a autorização e Plutus continua a decisão normal |
| Voz premium | TTS natural, modelo, voz ou credencial ausente | Bloqueio no preflight, antes da reserva e da chamada Runway |
| Integridade | Tipo, bytes, dimensões ou SHA-256 da referência divergem entre preflight e job | Novo download e bloqueio antes da chamada paga |
| Integração | Runway aceita a task | Request e response sanitizados, task ID, custo contratual e correlação persistidos |
| Falha externa | HTTP 401/403/402/429, moderação, timeout ou task falha | Causa explícita, custo conciliado e nenhuma repetição automática |
| Texto e voz | Legenda usa palavras diferentes da locução | Pós-produção bloqueada antes do TTS |
| Texto e voz | Legenda e locução têm as mesmas palavras, com separadores de timing | Cada trecho é narrado, medido e usado como limite do ASS/VTT; CTA permanece até o fim |
| Texto e voz | Soma dos trechos narrados ultrapassa a duração física do vídeo | Bloqueio sem cortar voz ou inventar timestamps |
| Áudio | Product UGC premium finalizado sem trilha licenciada | Voz natural normalizada, sem tom senoidal apresentado como música |
| Auditoria TTS | Speech devolve MP3 sem usage por request | Request sanitizado, resposta binária em ativo `AUDIO_AUDIT`, SHA-256 e custo pendente, nunca zero inferido |
| Disclosure | A peça usa voz sintética | Texto “Voz gerada por IA” queimado no vídeo durante toda a locução |
| Continuidade | Tomada realmente única contém corte detectado | Candidato bloqueado com causa de continuidade própria |
| Continuidade | Product UGC possui até quatro cortes editoriais limpos e planos individualmente estáveis | Transições são auditadas e excluídas do cálculo de tremor |
| Continuidade | Tremor, salto ou correção abrupta dentro de qualquer plano | Candidato bloqueado antes do upload final |
| Continuidade | Movimento suave com câmera estável dentro dos planos | Gate registra métricas e permite revisão independente |
| Copy visual | Provider preserva o layout, mas inventa palavras ou acentuação na tela | Pós-produção substitui integralmente os planos de produto pela referência aprovada no preflight |
| Concorrência | Dois workers recebem o mesmo job durante o mesmo polling | Claim atômico entrega uma única lease e o segundo worker recebe conflito |
| Callback tardio | Worker antigo falha depois que outro já concluiu | Estado `VIDEO_READY` e seus assets permanecem monotônicos |
| Composição | Espelho, celular refletido ou interface ilegível no conceito | Briefing bloqueado; Product UGC usa apresentadora frontal e referência digital limpa gerada pelo build do PDE |
| Build da referência | FFmpeg da imagem Alpine não possui decoder SVG | `rsvg-convert` recria PNG e manifesto dentro da imagem Docker versionada |
| Tipografia da referência | Fonte pedida pelo SVG não existe na imagem Alpine | Build instala DejaVu Sans; gerador exige essa família em todos os textos e bloqueia glyphs ausentes |
| Observabilidade | Job concluído ou falho | Request, response, versão, refs, hash, créditos, custo, duração e gate técnico auditáveis sem segredo |
| Relógio distribuído | Executor observa o saldo até 5 minutos antes ou depois do relógio do backend | Backend preserva o horário observado para auditoria, usa o próprio recebimento para o TTL e aceita o callback sem ampliar a validade |
| Relógio distribuído | Diferença absoluta entre executor e backend maior que 5 minutos | Callback bloqueado como desvio anormal, sem reserva ou geração paga |
| Revisão | Arquivo tecnicamente apto | Fica pendente; Psique e Têmis devem produzir pareceres reais no subprocesso criativo, e a aprovação humana continua obrigatória |
| Métricas | Ativo aprovado para experimento | Medir 3s, 25/50/75/100%, CTA, diagnóstico, checkout, pagamento, reembolso e custo |
| Segregação | Homologação local e experimento 91 | Tráfego/testes não entram como humanos, vendas ou receita |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | Formulário, preflight, relatório e player sem overflow ou erro de console |

## Critério de encerramento local

Após qualquer defeito encontrado e corrigido, executar duas rodadas locais completas e consecutivas
sem falha, incluindo backend, executor, frontend, contratos, MySQL 5.7, imagem Docker e jornadas nos
três perfis de navegador. A API produtiva da Runway não é chamada na homologação local; saldo,
quota, task e arquivo são simulados com test doubles que preservam o contrato oficial.

## Evidência executada em 2026-09-04

Depois de identificar que uma URL de imagem podia devolver o HTML de fallback da SPA, o preflight
passou a baixar, decodificar e congelar as duas referências antes de consultar a conta Runway; o job
repete a inspeção e bloqueia qualquer mudança de bytes ou dimensões antes da chamada paga. A
calibração posterior com o ativo rejeitado mediu variação média de 1,751 e pico de 83,068 contra
limites de 1,25 e 12. Ela também revelou que palavras iguais ainda eram divididas em janelas iguais,
sem prova de sincronismo temporal. A inspeção visual seguinte encontrou a tipografia substituída por
quadrados no PNG do container e reiniciou a homologação depois da inclusão explícita de DejaVu Sans.
Após a última correção, duas rodadas completas e consecutivas foram executadas sem falhas:

- 2.289 testes do backend, com zero falhas e zero erros, por rodada;
- 121 testes do executor de vídeo, com zero falhas e zero erros, por rodada;
- 139 arquivos e 474 testes do frontend, com zero falhas, por rodada;
- typecheck e builds do frontend administrativo e do PDE por rodada;
- Liquibase aplicado, retomado e reaplicado fisicamente no MySQL 5.7 por rodada;
- quatro imagens Docker construídas por rodada: backend, executor, frontend e PDE;
- filtros `vidstabdetect` e `vidstabtransform` confirmados na imagem final do executor;
- PNG 1080 × 1920 reproduzido pelo Dockerfile do PDE, com textos portugueses legíveis, fonte
  DejaVu Sans e SHA-256 registrados no manifesto;
- projeto #3 aberto e preset Vega #91 aplicado, sem persistência, em desktop, iPhone 15 Pro e Pixel
  7, com Product UGC, referências, copy única, gates contra a rejeição anterior, zero overflow e zero
  erro de console.

A referência sintética da personagem respondeu como PNG 941 × 1672. Após a publicação do preset, a
referência limpa do PDE passou a responder como `image/png` em 1080 × 1920. Nenhuma geração real foi
iniciada e nenhum dos 2.020 créditos informados foi consumido durante essa homologação.

## Execução produtiva do ciclo 11 em 2026-09-05

O Estúdio criou uma única task Runway Product UGC de 15 segundos para o experimento 91. A task
`e83b5648-a2ab-4248-8848-c59b63fd2c3c` concluiu e o ledger liquidou exatamente 648 créditos
(US$ 6,48), sem segunda geração. O MP4 bruto tem 1080 × 1920, 24 fps, 15,042 segundos, 9.432.580
bytes e SHA-256 `c372c1774f2d6982e83b1b2ce4ad2b7acadae2fc3733fa1985e38c30988f4ec0`.

O gate antigo reprovou média 1,306 e pico 54,705 porque calculou movimento através das transições.
A inspeção histórica do arquivo localizou cortes nos quadros 132, 183 e 258; todos os seis deltas
acima de oito pixels estavam no corte ou no quadro imediato. Separadas essas transições, os 354
deltas internos resultaram em média 0,544 e pico 5,112, abaixo dos limites 1,25 e 12. A política
passou a limitar quatro cortes e medir tremor somente dentro dos planos, preservando uma política
distinta e estrita quando tomada única for realmente exigida.

A pós-produção do job #21234 gerou voz, legendas e MP4, porém a execução produtiva e a execução local
haviam assumido o mesmo job com um segundo de diferença. O claim antigo apenas registrava evento e
não alterava estado; por isso a falha tardia do worker antigo sobrescreveu temporariamente a
conclusão. A correção torna a reserva atômica, renova a lease por heartbeat e impede regressão de
estado terminal.

A inspeção visual do primeiro MP4 final ainda encontrou palavras inventadas na interface e
`Más intenção` no lugar de `Mais intenção`. Sem consumir outro crédito Runway ou refazer a voz, os
planos 2 e 4 foram substituídos pela referência que o preflight havia aprovado, SHA-256
`5978ccbb9f70c69c27f9b23a5d982dee1aa8848541b02f01fbace1d19aba527e`. O ativo final #2780 tem
1080 × 1920, H.264 a 24 fps, áudio AAC, 15,084 segundos e SHA-256
`caa926f67cb514bdd58184148aff12dfd217b98130b2a57945fdd117c7748961`. O ativo de experimento
#38 está `READY`, ligado ao job #21234, com copy visual exata, legenda sincronizada e reprodução
aprovada em desktop, iPhone 15 Pro e Pixel 7. Campanha, mídia e publicação permanecem bloqueadas;
HLS e revisão humana são gates posteriores e separados da produção do arquivo.

## Retomada operacional do experimento 91 em 2026-09-04

O comando de preflight isolado foi executado pela tela do projeto #3 e criou o ciclo #9 com teto de
US$ 6,48. A integração real consultou a organização Runway, confirmou saldo de 2.020 créditos,
calculou 648 créditos para a receita `product_ugc` e validou as duas referências. O callback,
entretanto, permaneceu pendente porque o relógio do executor estava 78 segundos adiantado em relação
ao backend e o contrato publicado recusava qualquer diferença futura superior a 60 segundos.

O banco produtivo confirmou o ciclo `PENDING_PROVIDER_PREFLIGHT_ONLY`, preflight `PENDING`, custo
conhecido zero, nenhum `sales_video_job_id`, nenhuma reserva e saldo reservado zero. O backend passou
a usar seu próprio recebimento como início do TTL, preservando o horário observado para auditoria e
recusando somente diferenças anormais superiores a cinco minutos. O teste da tela que aguardava
apenas o título estático também passou a esperar o link funcional do MP4, removendo a intermitência
observada sob carga paralela.

Depois dessas correções, duas rodadas locais completas e consecutivas passaram:

- 2.299 testes do backend, com zero falhas e zero erros, por rodada;
- 138 testes do executor de vídeo, com zero falhas e zero erros, por rodada;
- 139 arquivos e 474 testes do frontend, com zero falhas, por rodada;
- Spotless, TypeScript, build administrativo e Actionlint canônico por rodada;
- Liquibase aplicado, retomado e reaplicado fisicamente no MySQL 5.7 por rodada;
- quatro imagens Docker construídas por rodada: backend, executor, frontend e PDE;
- referência do PDE reproduzida dentro da imagem em 1080 × 1920 e filtros `vidstabdetect` e
  `vidstabtransform` confirmados no executor;
- projeto #3 aberto sem erro ou overflow em Chromium desktop, iPhone 15 Pro e Pixel 7 por rodada.

A correção ainda precisa passar pelo fluxo de PR e deploy antes de o ciclo #9 poder concluir. Até
lá, não deve ser criado um ciclo de produção: isso manteria outra pendência e não autorizaria gasto.
