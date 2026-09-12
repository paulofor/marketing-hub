# Atividade: pacote não audiovisual

Materialize a copy completa e peças não audiovisuais previstas pelo contrato de comunicação:
anúncios estáticos, carrosséis, mensagens, e-mails ou briefings de renderização. Cada peça deve ter
formato, conteúdo publicável, prova real associada e correspondência explícita com a página de
destino. Respeite os limites do canal presentes no contexto. Para Meta, preserve `primaryText` até
125 caracteres, `headline` até 40, `description` até 25 e `ctaText` até 32, sem truncamento.

Preencha `functionalOutput.copy` e ao menos um `staticAssets`. Um vídeo necessário deve aparecer
somente como `audiovisualBrief`; Apolo produzirá a mídia final. Não gere URL fictícia nem marque o
pacote como publicado.

Preserve os formatos congelados no `COMMUNICATION_PACKAGE` predecessor. Quando esse contrato
contiver `audiovisualBrief`, detalhe-o para Apolo; quando for `null`, mantenha `null`. Se uma
inconsistência exigir mudar os formatos, bloqueie indicando a correção do contrato de comunicação;
não introduza silenciosamente vídeo ou áudio depois da resolução técnica de formatos.

Quando a entrada for `LEARNING_CYCLE_PRIVATE`, materialize as peças para avaliação privada.
O CTA pode conduzir à experiência privada aprovada, sem alegar que se trata de checkout ou
autorizar distribuição pública. A ausência de checkout comercial impede CTA de compra, mas
não impede copy e composição de demonstração do primeiro resultado gratuito. Use referências
reais das provas do protótipo e identifique a natureza sintética na auditoria; não fabrique
preferência humana, venda ou URL de peça renderizada. Briefing de renderização é briefing,
não uma imagem pronta. Registre a necessidade de renderização e aprovação independente antes
do uso comercial. Os avaliadores seguintes conservam seus próprios critérios de qualidade.
Registre as dependências de publicação em `nextHandoff`; reserve `evidenceGaps` para
lacunas que impedem comprovar a peça solicitada nesta atividade.


## Imagem final obrigatória — PROOF_CARD_V1

Quando `blockedActivities` contiver parecer posterior de Psique ou Têmis com decision=ADJUST, corrija cada requiredChange na nova peça e explique a mudança. Preserve a estratégia; não repita o mesmo briefing sem aplicar o parecer. BLOCKED por evidência essencial continua sendo impedimento real.

A atividade só termina depois que o executor renderiza e persiste cada peça. Briefing sozinho não é peça final.
Use `approvedVisualInputs` e a imagem anexada pelo executor; são pixels aprovados da mesma versão.
Em cada `staticAssets[]`, preencha `renderSpec` com templateVersion `PROOF_CARD_V1`, sourceArtifactId e sourceSha256 exatos da entrada e crop inteiro em **pixels originais** (x, y, width, height). Selecione um detalhe útil e legível da interface real; não invente tela nem resultado. O recorte é registrado e seus pixels são copiados sem redesenho. Prefira um único detalhe que prove a promessa. A imagem é 1080 × 1350; a área da prova mede 952 × 550. O recorte não pode exigir escala menor que 0,7; escolha no máximo 1360 × 785 pixels e preserve o contexto necessário. Não use a página inteira se perder legibilidade.

Campos de texto: brandLabel (marca pública curta), eyebrow (rótulo curto), headline (até duas linhas, ~35 caracteres), body (até duas linhas, ~90 caracteres), ctaText (uma linha curta, ~35 caracteres), footer (limite factual curto). Cores backgroundColor e accentColor em #RRGGBB, com alto contraste: fundo claro, destaque escuro. Use texto comercial claro e fiel ao briefing aprovado; sem prometer compra, oferta ou resultado que o protótipo não entrega.

Em validação privada o executor imprime os marcadores “EXPERIÊNCIA PRIVADA” e “Demonstração sintética · sem compra ou cobrança”. Não confunda esse trabalho com publicação de anúncio. O PNG, hash, URL privada e linhagem são acrescentados pelo executor em `functionalOutput.renderedAssets`; não invente esses campos na resposta do modelo. Inclua em `nextHandoff` a revisão da **imagem final** por Psique e Têmis. Campos não aplicáveis nos demais tipos de saída continuam vazios; não produza staticAssets fora da atividade de produção.
