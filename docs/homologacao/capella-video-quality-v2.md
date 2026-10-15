# Capella: qualidade audiovisual v2 — 2026-10-11

## Falha observada

Os vídeos #55/#56, rejeitados pelo operador, ampliavam uma captura longa da prova
técnica #685/#584 e repetiam ilustrações sintéticas de teste. O movimento sobre a
mesma tela não demonstrava a qualidade da entrega. Os pareceres de imagem não eram
pareceres do MP4 final. A main já contém a revisão temporal de Psique; esta correção
reutiliza esse mecanismo, sem criar outro gate.

## Escolha

Animar os placeholders preservaria a falha. Gerar todas as artes novamente consumiria
orçamento sem corrigir a seleção da fonte. A opção adotada é selecionar amostras da
biblioteca aprovada do próprio produto, com direitos e hash, e compor cenas variadas.
Amostra de catálogo não será chamada de captura da versão privada nem prova de vendas.
O pacote, preço e condições permanecem os mesmos. A seleção anterior continua rejeitada.

## Matriz local de aceite, antes de publicar

| Área | Verificação |
|---|---|
| Caminho feliz | Escolher fontes aprovadas pela tela, compor anúncio e demonstração, preservar voz auditada, encaminhar MP4 novo à revisão existente. |
| Qualidade visual | Artes reais da biblioteca, alternância de composição, mudanças de conteúdo e movimento perceptível; nenhum placeholder ou página longa reduzida. |
| Clareza | Oferta fiel, preço e condições legíveis, linguagem da cliente, sem prometer agenda/clientes garantidos. |
| Integração | Backend resolve fontes e identidade; executor confere bytes e compõe; resposta preserva manifesto e momentos de revisão. |
| Falhas | Rejeitar fonte de outro produto/tenant, não aprovada, sem direitos/hash, adulterada, repetição sem variedade e composição sem fonte. |
| Recuperação e custo | Voz idêntica reutilizada; contratos inválidos bloqueiam antes de TTS; sem publicar ou herdar aceite humano. |
| Regressões | Outro produto/execução e finalização anterior sem amostras continuam funcionando. |
| Dispositivos | Chromium desktop, iPhone e Pixel emulados; áudio, legendas e área segura. |
| Observabilidade | IDs, hashes, origem, tempos e resposta persistidos; testes segregados; custo desconhecido permanece desconhecido. |
| Resultado comercial | Só aferível após autorização e homologação; reprodução e testes locais não comprovam vendas. |

## Hipótese de aprendizado

Apolo deve selecionar uma fonte visual adequada ao que está sendo vendido, em vez de
usar automaticamente a primeira prova técnica disponível. Psique deve reprovar
monotonia ou artes inadequadas no MP4, mesmo quando o arquivo e a captura são íntegros.
Critério de adoção: regressões locais aprovadas e parecer independente do novo arquivo.
Critério de reversão: mistura de identidade, consumo duplicado ou perda de fidelidade.
Impacto em conversão e margem ainda não medido.

## Resultado local

- Backend: seleção da biblioteca e hashes legados, DTO HTTP, contexto de produto/tenant,
  metadados da montagem, compatibilidade V1/V2/V3 e revisão final testados. ArchUnit
  preserva a fachada existente; nenhuma alteração nas regras de arquitetura.
- Vídeo: renderizações completas de 27 s e 45 s com seis artes aprovadas de Capella,
  oito trechos de voz recuperados em cada vídeo, nenhum TTS novo. Arte e legenda têm
  áreas separadas; títulos curtos destacam benefício e R$ 67. Composição recalcula
  os intervalos a partir da voz real e registra fontes/momentos de revisão.
- Teste real identificou e corrigiu mistura indevida do áudio da fonte após substituir
  sua camada visual. Camadas estáticas são preparadas uma vez, evitando decodificar
  imagens grandes em todos os frames. Recuperação conserva custos pendentes.
- Navegação local com APIs simuladas: seleção pela tela, payload com IDs/títulos sem
  proveniência fornecida pelo cliente e sem autorizar nova locução. Desktop, iPhone
  15 Pro e Pixel 7 reproduziram ambos os MP4s 1080×1920, com áudio decodificado e sem erro.
- Os testes condicionais que exigem serviços externos não foram usados. Renderização
  FFmpeg, reprodução e fixtures de integração necessárias foram executadas localmente.
  Não houve escrita em produção nem nova chamada de IA durante a homologação local.
- Limite: frames e reprodução não comprovam naturalidade da voz, aceite da cliente ou
  conversão. O áudio foi preservado; Psique e a seleção humana continuam vinculados ao
  hash novo. A oferta permanece em preparação, sem autorização comercial decorrente
  desta correção.

## Largura do formulário na conferência publicada

A conferência detectou expansão da página de 1440 para 2983 px. A hipótese inicial
sobre descrições das artes foi descartada: o problema vinha do evento carregado
assincronamente, com JSON e identificadores extensos sem quebra no estado da
pós-produção. A correção permite quebra de texto e reduz o tamanho mínimo do bloco,
preservando a auditoria completa. Truncar os dados perderia evidência; refazer o
formulário seria desproporcional.

O teste existente `frontend/e2e/product-video-authoring-responsive.mjs` passa a
incluir pós-produção anterior e evento com URL/hash extensos, imagens carregadas,
seleção de artes e envio sem repetir locução. Mede overflow em desktop, iPhone e
Pixel com IDs segregados e nenhuma requisição externa. O cenário anterior não
fornecia eventos ao formulário e por isso não reproduzia a falha. Executar com
`FRONTEND_TEST_URL` apontando ao Vite local.

Regressão comprovada: sem a correção, o caso local expandiu 1440 para 6334 px;
com ela, passou nos três dispositivos, preservando também roteiro e importação.
A conferência visual publicada com o mesmo ajuste apenas no navegador retornou
1440 px, sem alterar dados ou enviar geração. Typecheck e build foram executados.

## Passagem para a revisão independente

A montagem publicada criou vídeos 65 (27 s) e 66 (45 s), com bytes e hashes
conferidos, 16 segmentos de voz reutilizados e nenhuma nova síntese. Reprodução
com áudio decodificado e controles passou em desktop, iPhone e Pixel.

A retomada do subprocesso criativo revelou fila circular com a espera de seleção.
Alternativas: pausar manualmente o processo de vendas, liberar toda concorrência ou
reconhecer a dependência exata. A última preserva o acompanhamento e custa menos
manutenção, sem liberar contextos independentes ou tarefas em curso.

Matriz local adicional: o caso original e outro produto devem ganhar vez para
revisão; produto/cadeia/ciclo/experimento diferentes, ciclo encerrado, pausa, falha,
etapa errada e tarefa real devem manter o bloqueio. A preparação comercial já
válida continua permitida. HTTP/JPA deve preservar espera humana, revisão, custos e
histórico, sem criar tarefa durante leitura. Os dois casos positivos falharam
antes da correção; nenhuma inferência ou cobrança é usada nos testes.

Validação: 328 testes das 15 suítes de execução, revisão final e arquitetura sem
falhas ou skips. A fixture HTTP foi corrigida para usar a chave do ciclo real em
vez da chave sem ciclo; depois passou em duas identidades. Spotless e revisão do
diff confirmaram comentários de responsabilidade em todas as classes/métodos
alterados. A correção não muda banco, orçamento, peça nem seleção humana.

## Revalidação sob origem concluída

Após a publicação, a fila mostrou `WAITING_PARENT`: a execução criativa 65 tem
origem 64, concluída em 09/10. O evento 2196 comprova o novo comando de revalidação
em 11/10. O controlador comparava a raiz de comunicação à dependência de revisão;
além disso, uma raiz concluída não aparece na lista de reservas ativas.

Desvincular o filho perderia o retorno; reabrir a origem sem condição enfraqueceria
a pausa e a autoridade. A correção escolhida reaproveita a validação dos ancestrais
e avalia o subprocesso autorizado para a prioridade. Só reabre a espera do pai,
depois de todas as reservas permitirem a revisão e sem tarefa conflitante.

Matriz complementar: duas identidades com pai concluído → comando novo → revisão
→ parecer → retorno único, mantendo a seleção humana e os custos. Comando ausente
ou anterior à conclusão, pai pausado, STOP, outro ciclo, tarefa em execução e outra
reserva devem impedir disparo. Os dois casos positivos reproduziram localmente
`WAITING_PARENT` antes da correção. Os testes usam a política real de precedência,
persistência local e agentes simulados, sem novas chamadas pagas.

Resultado: 337 testes das 15 suítes relacionadas, sem falhas, erros ou skips,
incluindo retorno único ao pai e proteção das reservas. Spotless e diff conferidos.


## Correspondência entre narração e demonstração — parecer 731

Psique aprovou a evolução das artes, mas pediu ajuste dos MP4s 65/66: a narração
falava de briefing, textos, calendário e download enquanto a montagem só mostrava
posts. A abertura ainda permitia confundir o kit com serviço de manicure. Não é
falha da fonte aprovada; o compositor impedia combinar amostras com ações reais.

Alternativas avaliadas: simplificar somente a copy perderia a demonstração; voltar
à tela inteira repetiria a monotonia; combinar arte e recortes reais preserva o
apelo visual e comprova o uso. A terceira foi escolhida, reutilizando voz, capturas
homologadas e catálogo do mesmo produto. Não muda o produto, preço ou autorização.

Matriz adicional, definida antes dos testes:

- Backend e tela aceitam as duas fontes, resolvidas separadamente pelo contrato
  existente, com identidade e direitos. V4 impede workers antigos de ignorar uma
  camada. V1/V2/V3 continuam compatíveis com suas montagens anteriores.
- O harness técnico captura briefing preenchido e conteúdo expandido além dos nove
  percursos existentes. Momentos têm vínculo com cenário, dispositivo e sessão;
  não há compra, envio, nova composição externa nem prova comercial.
- O render final mantém abertura clara, artes dentro da área visível, demonstração
  no trecho narrado, legendas legíveis e encerramento próprio. Catálogo ilustrativo
  e interface real com dados fictícios continuam identificados separadamente.
- Dois produtos, desktop/iPhone/Pixel, recuperação da mesma voz, bloqueio de fontes
  adulteradas e testes antigos válidos. A revisão recebe hashes e momentos das
  duas fontes. Não herda aprovação do MP4 anterior.
- Adoção depende de regressões locais e novo parecer independente. O parecer 731
  custou US$ 0,560812 estimados; a locução original continua sem conciliação.
  Sem alegação de aumento de conversão ou margem antes de medição comercial.


A rodada local detectou duas capturas de conteúdo com a mesma chave. A chamada
repetida foi removida e o harness agora recusa duplicação antes de produzir relatório.
Os dois contextos passaram com nove percursos e quinze capturas cada; o consumidor
validou também vínculos e rejeições de momentos ausentes/dispositivo divergente.

Houve saturação real do limite compartilhado de 512 processos/threads da sandbox:
Chromium ficou sem renderer e FFmpeg recusou decoder com `Resource temporarily
unavailable`. Node persistente permaneceu saudável. A validação foi retomada com
afinidade local de duas CPUs e os decoders da composição passaram a declarar uma
thread por entrada, preservando conteúdo e reduzindo disputa. Não foi alterada a
instalação global nem utilizada publicação como teste.

Validação da composição combinada: 211 testes de backend nas oito suítes afetadas,
mais o consumidor da prova técnica real; 182 testes do worker de Psique (9 condicionais
sem serviço externo) e 292 do executor audiovisual (4 condicionais), sem falhas após
a correção dos decoders. Os testes de FFmpeg incluem duas identidades e modalidades
anteriores. Os MP4s completos de 27/45 s passaram em Chromium desktop, iPhone e Pixel,
com decodificação de áudio, controles e identidade. 28 testes de interface, typecheck,
formulário combinado nos três dispositivos e análise do script também passaram.

A rodada de integração comprovou 18 percursos técnicos, seis callbacks de cenário,
dois retornos de integridade/gate e nove controles privados. Após corrigir a chave
duplicada, os 18 percursos e o consumidor de quinze capturas foram revalidados; a
segunda rodada de callbacks foi interrompida pela saturação de processos, sem falha
funcional nova. O modo de cenário permanece sem mudança de contrato. A topologia
temporária foi removida. Nenhuma inferência paga foi usada na validação local.

Spotless passou nos arquivos alterados do backend e do worker. O worker contém duas
violações de formatação preexistentes em FinalVideoReviewInputs, fora deste diff. O
check global do backend também aponta arquivos preexistentes fora desta alteração;
o check restrito aos arquivos alterados é a evidência usada. O build do frontend passou. O
executor audiovisual não configura Spotless e manteve seu estilo local.

## Rejeição humana e estabilidade de leitura — 15/10/2026

O banco confirmou a rejeição de #71 às 03:19:21Z e #69 às 03:20:05Z: encerramento
descritivo e artes tremendo. Os MP4s e seus metadados confirmam a frase narrada;
`DeliveryExamplesMontage.render` aplicava seno horizontal/vertical durante cada cena.
O parecer #736 havia aprovado os frames, declarando ausência de escuta. A escuta
publicada no PR #5600 não revisa retroativamente esses arquivos.

Foram comparadas três soluções: zoom contínuo lento (baixo esforço, mas altera a
escala do texto durante a leitura); novos planos da experiência real (boa prova,
mas exigem novas capturas e sincronização); entrada breve seguida de leitura
estável (reutiliza as artes e preserva a progressão entre cenas). A terceira foi
adotada para corrigir a causa compartilhada com menor esforço e risco de leitura.
O aviso de amostra continua explícito, sem repetir promessa negativa em toda cena.

Matriz: reproduzir deslocamento com FFmpeg real, medir estabilidade após 450 ms,
preservar entrada e troca de arte, verificar duas identidades, prova sincronizada,
narração recuperada, duração e reprodução desktop/iPhone/Pixel. Fontes e custos
históricos permanecem auditáveis; nenhum teste se mistura com métricas comerciais.

A regressão falhou antes da correção nos dois produtos: deslocamento horizontal
de 1,037 pixel na redução 108×192, superior à tolerância de 0,5 pixel. Depois da
correção, 290 testes do módulo executados sem falhas/erros, com sete testes opcionais
não habilitados; os quatro casos de renderização real foram habilitados e passaram.
A preparação de novos roteiros é distinta do aceite independente e da publicação.

A montagem completa com as fontes reais de Capella passou nos 14 testes da suíte
específica, incluindo a recuperação local das vozes e as capturas vinculadas por
hash. Os arquivos de 27/45 segundos dessa homologação conservam deliberadamente a
narração antiga: comprovam a correção visual, não são os novos vídeos comerciais.
A reprodução passou em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, com
controles, seek, encerramento e ausência de overflow horizontal. Não houve escuta
perceptual nesta conferência técnica. O workflow de publicação passou a habilitar
também os testes reais de estabilidade da entrega; sua execução equivalente local
passou com 292 testes, zero falhas/erros e quatro opcionais não habilitados.
O comando de CI passou em `bash -n` e ShellCheck, os 14 contratos de workflow
passaram e o Actionlint da revisão canônica do repositório validou o workflow.

Pelo Estúdio, os projetos #11 e #12 receberam os roteiros corrigidos (71 e 111
palavras), direção por cena, voz e critérios de revisão, em estado `DRAFT`.
São propostas operacionais para a correção pedida, sem parecer fabricado em nome
de Íris ou Psique. A oferta, o contexto e os limites permanecem os mesmos; os
arquivos #69/#71 e sua rejeição continuam preservados. Novas locuções, MP4s,
revisões com escuta, seleção humana e homologação comercial permanecem pendentes.

O ledger audiovisual de Capella mantém custos originais de locução desconhecidos
e recibos de reutilização com custo incremental zero. Isso não autoriza considerar
o custo original gratuito. A leitura do faturamento OpenAI retornou HTTP 403 por
ausência de `api.usage.read`, tanto com o acesso da sandbox quanto com o acesso do
executor audiovisual; não se altera o gate para contornar essa limitação.
Fonte: [API oficial de custos](https://developers.openai.com/api/reference/resources/admin/subresources/organization/subresources/usage/methods/costs).
