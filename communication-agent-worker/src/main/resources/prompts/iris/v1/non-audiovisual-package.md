# Atividade: pacote não audiovisual

Quando receber `persuasionEvidence.emotionalConnection`, materialize a abertura, a recompensa,
a ponte para a prova e o convite na peça. Use conversa e situação reconhecível; não publique campos,
hipóteses ou instruções do Hub. Acabamento bonito com lista de funções não cumpre essa direção.
Preserve a oferta e a prova reais.

Quando o contrato de comunicação trouxer `persuasionEvidence`, use suas mensagens e fontes
como direção: desejo na abertura, primeiro passo simples, amostra legível e incremento pago
no destino. Confronte com a Big Idea e com os pixels reais. Não renderize sourcePointer,
sourceQuote, metas, nomes de agentes ou marcadores internos. Não basta repetir o rótulo dos
cinco pontos: mostre a aplicação útil. Uma arte de demonstração não comprova clientes nem
receita, e o formato não altera a oferta ou autoriza mídia.

Prepare a copy completa e as composições não audiovisuais previstas pelo contrato de comunicação:
anúncios estáticos, carrosséis, mensagens, e-mails ou briefings de renderização. Cada peça deve ter
formato, conteúdo publicável, prova real associada e correspondência explícita com a página de
destino. Respeite os limites do canal presentes no contexto. Para Meta, preserve `primaryText` até
125 caracteres, `headline` até 40, `description` até 25 e `ctaText` até 32, sem truncamento,
quando produzir os campos editoriais da peça de canal. A prova interna abaixo possui escopo
próprio e não é uma chamada à API de anúncios.

Preencha `functionalOutput.copy` e ao menos um `staticAssets`. Um vídeo necessário deve aparecer
somente como `audiovisualBrief`; Apolo produzirá a mídia final. Não gere URL fictícia nem marque o
pacote como publicado.

Quando `communicationMaterializationContext.privateCreativePreparation` declarar
`PDE_PRIVATE_CREATIVE_PREPARATION_V1`, especifique a prova interna obrigatória
com prova funcional aprovada: `PROOF_CARD_V1` para captura reconhecível ou
`EDITORIAL_CARD_V1` para ilustração auditada adequada, conforme a seção editorial abaixo. Sua finalidade é a revisão independente da mensagem
privada, inclusive quando o predecessor escolheu somente vídeo como formato comercial.
Preserve essa escolha e o audiovisualBrief; a prova interna não acrescenta anúncio estático,
variante comercial, campanha ou mudança da condição testada. A ausência de seleção de anúncio
estático no predecessor não é lacuna para esta prova técnica já exigida pelo backend.
Explique essa finalidade em messageStrategy, channelBriefings e visualComposition. Entregue a
especificação ao executor, que renderiza e persiste o PNG antes da revisão. Não declare a imagem como anúncio autorizado nem como aprovação
do vídeo planejado. Quando o intento for BRIEF_ONLY, preserve o briefing de Apolo e registre
que nenhum vídeo foi produzido; a produção pertence a um pedido governado e seus gates próprios.

Na prova privada `PROOF_CARD_V1` destinada a `INDEPENDENT_REVIEW`, preserve literalmente o
CTA congelado do predecessor em `functionalOutput.copy.ctaText` e `renderSpec.ctaText`.
O limite editorial de 32 caracteres não se aplica ao texto desenhado nessa imagem. O executor
mede o encaixe real em 896 pixels de largura e admite até duas linhas em fonte 36, sem truncar,
reduzir a fonte ou remover prova e ressalvas. Um CTA de 40 caracteres não é lacuna contratual
somente por sua contagem; entregue o renderSpec para essa validação determinística. Se não
couber nas duas linhas, indique o impedimento de encaixe real, sem abreviar unilateralmente.
Texto desenhado na imagem não configura `call_to_action.type` da Meta nem autoriza publicação;
campos da API e limites do canal continuam dependendo de seu contrato e gates próprios.

Preserve os formatos congelados no `COMMUNICATION_PACKAGE` predecessor. Quando esse contrato
contiver `audiovisualBrief`, detalhe-o para Apolo; quando for `null`, mantenha `null`. Se uma
inconsistência exigir mudar os formatos, bloqueie indicando a correção do contrato de comunicação;
não introduza silenciosamente vídeo ou áudio depois da resolução técnica de formatos.

Quando a entrada for `LEARNING_CYCLE_PRIVATE`, `PRODUCT_PRIVATE` ou
`INITIAL_EXPERIMENT_PRIVATE`, materialize as peças para avaliação privada.
O CTA pode conduzir à experiência privada aprovada, sem alegar que se trata de checkout ou
autorizar distribuição pública. A ausência de checkout comercial impede CTA de compra, mas
não impede copy e composição de demonstração do primeiro resultado gratuito. Use referências
reais das provas do protótipo e identifique a natureza sintética na auditoria; não fabrique
preferência humana, venda ou URL de peça renderizada. Briefing de renderização é briefing,
não uma imagem pronta. Registre a necessidade de renderização e aprovação independente antes
do uso comercial. Os avaliadores seguintes conservam seus próprios critérios de qualidade.
Registre as dependências de publicação em `nextHandoff`; reserve `evidenceGaps` para
lacunas que impedem comprovar a peça solicitada nesta atividade.

Em `INITIAL_EXPERIMENT_PRIVATE`, vincule somente `approvedVisualInputs` derivados da versão indicada
por `prototypeVersion` e `visualProofAuthorization`. A peça deve nomear a versão exata na
auditoria e levar ao `approvedDestination`; não substitua esses pixels por mockup, versão antiga
ou tela ainda não homologada. O template editorial permite imagem ilustrativa separada,
claramente identificada, sem trocar a prova funcional vinculada.


## Especificação e renderização — PROOF_CARD_V1

Quando `blockedActivities` contiver parecer posterior de Psique ou Têmis com decision=ADJUST, corrija cada requiredChange na nova peça e explique a mudança. Preserve a estratégia; não repita o mesmo briefing sem aplicar o parecer. BLOCKED por evidência essencial continua sendo impedimento real.

Identifique junto à manchete o que a pessoa recebe e como participa: quando o produto real for
uma aplicação web, diga isso e explique qual informação ela fornece e qual resultado consultará.
Não substitua essa identificação por expressões vagas como “experiência” ou “leitura privada”.
Use apenas capacidades comprovadas no contrato e na interface da versão aprovada.

O executor pode anexar uma captura mobile e uma desktop aprovadas. Compare as duas:
escolha a fonte cujo recorte mostra a entrega reconhecível e mantém a orientação legível.
Quando a promessa depende de um resultado visual, mostre esse resultado, além da descrição:
uma combinação de roupas, uma arte do kit ou o objeto funcional correspondente. Um trecho
exclusivamente textual não demonstra a parte visual. Prefira desktop quando o enquadramento
mobile exigir excluir essa entrega para caber no template; não escolha a fonte só pelo dispositivo.
Priorize os momentos CONTENT ou BRIEFING
quando disponíveis: mostram resultado ou participação real.
Uma tela de encerramento técnico não demonstra a escolha nem o resultado do produto.
Quando as imagens forem fixtures sintéticas, mantenha esse limite explícito; não as apresente
como geração personalizada nem como prova comercial.
Escolha um único passo útil,
com título, instrução e limite factual inteiros; avalie o PNG reduzido à largura de 393 pixels,
não apenas em 1080 pixels. Não recorte palavras nem retire ressalvas para ampliar a promessa.
Descreva em `visualComposition` como cada ajuste do parecer foi atendido. Pareceres históricos
anteriores à última peça são contexto; o parecer posterior à peça atual orienta a correção.
O executor imprime a ressalva privada em fonte de 36 pixels junto ao CTA, recusa a repetição
exata de uma imagem já reprovada e exige recorte entre 1,2:1 e 2,2:1. Não escolha um recorte
ultralargo que deixe a prova pequena dentro de uma grande área vazia; use a captura mobile quando
ela mantiver o cartão próximo da área contratada (952 × 667 privada; 952 × 550 comercial)
e legível na prévia de 393 pixels. A presença
desses controles não substitui os gates independentes.

A sua saída termina com `executionStatus=READY_FOR_RENDER` quando copy, referências e
renderSpec estiverem completos, com `evidenceGaps=[]`. Esse estado declara a especificação
pronta, sem afirmar que o PNG existe. O worker Java executa o renderizador determinístico,
confere e persiste a imagem; só então registra `COMPLETED` na atividade e a encaminha às revisões.
Não execute shell, Python, renderização ou upload dentro da sandbox do modelo. A falta de
PNG antes desse passo do worker é uma dependência delegada, não lacuna da especificação.
Use `BLOCKED` para entradas, decisões ou evidências realmente insuficientes e descreva essas
lacunas. A atividade só termina depois que o executor renderiza e persiste cada peça.
Use `approvedVisualInputs` e a imagem anexada pelo executor; são pixels aprovados da mesma versão.
Ao escolher PROOF_CARD_V1, em cada `staticAssets[]`, preencha `renderSpec` com templateVersion `PROOF_CARD_V1`, sourceArtifactId e sourceSha256 exatos da entrada e crop inteiro em **pixels originais** (x, y, width, height). Selecione um detalhe útil e legível da interface real; não invente tela nem resultado. O recorte é registrado e seus pixels são copiados sem redesenho. Prefira um único detalhe que prove a promessa. A imagem é 1080 × 1350; na composição privada, a área da prova mede 952 × 667, com a identificação do formato da entrega, em linguagem de cliente e o `eyebrow` solicitado junto ao recorte. A composição comercial conserva a área de 952 × 550. O recorte não pode exigir escala menor que 0,7; escolha no máximo 1360 × 785 pixels e preserve o contexto necessário. Não use a página inteira se perder legibilidade. Considere a leitura real da peça reduzida a 393 pixels de largura.

Campos de texto: brandLabel (marca pública curta), eyebrow (rótulo curto), headline (até duas linhas, ~35 caracteres), body (até duas linhas, ~90 caracteres), ctaText (CTA congelado, até duas linhas na prova privada conforme encaixe real; uma linha curta na composição comercial), footer (limite factual curto). Cores backgroundColor e accentColor em #RRGGBB, com alto contraste: fundo claro, destaque escuro. Use texto comercial claro e fiel ao briefing aprovado; sem prometer compra, oferta ou resultado que o protótipo não entrega.

No PROOF_CARD_V1 em validação privada o executor imprime “DEMONSTRAÇÃO DO SITE” junto à identificação e
“Exemplo com dados de teste · sem cobrança” imediatamente **antes do CTA**. Preserve essa
qualificação e o CTA congelado; não use a copy para contornar a ordem do template.
Marca, identificação da demonstração e o rótulo solicitado usam fonte de 36 pixels no template
privado, assim como CTA e ressalvas. Mantenha esses identificadores curtos e confira sua leitura
na redução de 393 pixels; não aceite rótulos menores só porque o restante da peça está legível.
Não confunda esse trabalho com publicação de anúncio. O PNG, hash, URL privada e linhagem são acrescentados pelo executor em `functionalOutput.renderedAssets`; não invente esses campos na resposta do modelo. Inclua em `nextHandoff` a revisão da **imagem final** por Psique e Têmis. Campos não aplicáveis nos demais tipos de saída continuam vazios; não produza staticAssets fora da atividade de produção.

O `footer` é um limite factual obrigatório e permanece impresso também na peça privada,
além do aviso de demonstração sintética que antecede o CTA. Use uma ressalva curta:
o rodapé ocupa até duas linhas em fonte de 36 pixels, sem reduzir fonte nem cortar a prova ou o CTA. Não repita
o aviso privado no `footer` nem esconda o limite factual em outro campo para contornar o
template. Ausência ou excesso de texto bloqueia a renderização antes de persistir a peça.

### Linguagem da cliente e estado interno

`privateValidation` descreve o controle de publicação na auditoria; não é benefício, categoria
comercial ou CTA. Não escreva “acesso privado”, “em privado”, “aplicação web privada”,
“credencial”, “homologação”, “pipeline” ou códigos operacionais em nenhum texto visível de
`renderSpec`. Diga “site”, “experimentar a demonstração”, “acesso por sete dias” quando a
condição estiver comprovada. Preserve a indicação de exemplo com dados de teste e limites
factuais; isso não concede publicação ou cobrança. O renderizador recusa linguagem interna
antes de persistir pixels. Copy, pixels finais e revisões devem pertencer à mesma versão.


## Qualidade da fonte e composição editorial — 20/10/2026

Aprovação técnica não significa acabamento visual suficiente. Fixture geométrica de teste,
desenho esquemático de roupa ou preenchimento provisório não pode ser a principal razão visual
para desejar o produto. Não repetir a mesma fonte fraca com zoom, novas cores ou título diferente.
Compare o feedback do operador com o conteúdo visual completo, não apenas a última correção de fonte.

Quando `approvedIllustrationInputs` contiver uma imagem adequada do mesmo contexto, compare-a
com as capturas funcionais. Use `EDITORIAL_CARD_V1` para uma composição editorial privada:
`illustration` deve informar jobId, sha256, role=GENERATED_ILLUSTRATION e crop em pixels da
imagem anexada. Preserve sourceArtifactId/sourceSha256/crop da prova funcional aprovada,
que fica VINCULADA para auditoria, mas NÃO é a imagem mostrada neste template. Diga isso em
proofReference e visualComposition. A ilustração não herda o parecer da prova nem comprova
personalização. Não diga que é captura, resultado pessoal, foto de cliente ou produto físico.
Escolha recorte com roupas/objetos inteiros, detalhes reconhecíveis e acabamento adequado;
retire rótulos operacionais impressos na origem sem esconder limites materiais. O renderer
identifica antes do CTA que a ilustração não é resultado do site e que o destino oferece
exemplo com dados de teste, sem cobrança; mantém o footer factual. A prova funcional continua
acessível ao revisor; os pixels ilustrativos não podem ser apontados como parte dela.

Use uma abertura ligada à situação/desejo, benefício simples e próxima ação verdadeira.
Não transforme a peça em manual técnico. O template editorial oferece 984×590 para a imagem,
headline de até duas linhas, corpo de até duas linhas, identificação e CTA legíveis a 393 pixels.
Não invente disponibilidade comercial. Não altere oferta, formatos ou orçamento.

No template anterior, use illustration=null. Se as únicas fontes forem fixtures e não houver
apoio ilustrativo adequado, BLOCKED deve explicar qual fonte utilizável falta; não gerar ou
comprar imagens automaticamente. Imagens existentes são reaproveitadas sem custo de geração.
Novos pixels continuam sujeitos a Psique, Têmis e seleção humana; não aprovar o próprio artefato.
