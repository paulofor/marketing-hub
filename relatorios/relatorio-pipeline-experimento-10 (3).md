# Relatório consolidado do pipeline de conteúdo do experimento

Experimento: Experimento 4
Hipótese vinculada: Agenda Cheia Sem Desconto (8 Semanas)

## Prompts e resultados produzidos por seção
### 1. Angulo da Campanha
- Data: 03/04, 06:52
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))
Metadados obrigatórios do experimento:
- primary_variable: Dor vs Resultado
- variant_id: variant-10
- stage: AD
- control_or_treatment: treatment
- asset_role: campaign-angle

Tarefa alvo: campaign-angle

Instruções extras do usuário:
Quantidade sugerida: 3

Responda exclusivamente em JSON válido e siga estritamente o schema da seção.
```

**Resultado**
```text
{"output_text":null,"id":"resp_0f0b0d0c959578560069cf8dc8832c8196bd24bc43bc1bfd0b","output":[{"tool_call":null,"tool_calls":null,"id":"msg_0f0b0d0c959578560069cf8dc9174c8196aad19b2d099f313f","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"campaignAngle\":{\"singleMindedPromise\":\"Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.\",\"primaryPain\":\"A conversa vira “quanto custa?” e os alunos somem nas primeiras semanas porque não enxergam valor rápido — aí sua agenda depende de indicação e o mês vira loteria.\",\"primaryPromise\":\"Ter agenda cheia com constância em até 8 semanas, sem desconto, com alunos que chegam mais qualificados (entendendo o valor) e permanecem engajados no início.\",\"cta\":\"Desbloquear o kit de “Agenda Cheia Sem Desconto (8 semanas)”\",\"tone\":\"Direto, prático e confiante (sem hype), com foco em clareza e controle do resultado.\",\"mechanismSummary\":\"Um kit de ativos gerados por IA com uma sequência pronta de comunicação + primeiros marcos de evolução (primeiras semanas) + check-ins automáticos para (1) posicionar seu diferencial antes do preço e (2) fazer o aluno sentir progresso e cuidado rápido.\",\"primaryCTA\":\"Desbloquear o kit\",\"landingMatchLine\":\"Desbloqueie o kit “Agenda Cheia Sem Desconto (8 semanas)” para atrair alunos que entendem seu valor e manter engajamento nas primeiras semanas — sem depender de indicação.\",\"funnelStage\":\"Topo/Meio (aquisição + pré-qualificação)\",\"proofSummary\":\"Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (reduz desistência, aumenta percepção de valor e melhora renovação), transformadas em sequência pronta e replicável em ativos digitais.\"},\"experimentMetadata\":{\"variant_id\":\"variant-10\",\"control_or_treatment\":\"treatment\",\"primary_variable\":\"Dor vs Resultado\",\"stage\":\"AD\",\"asset_role\":\"campaign-angle\"}}"}]}],"usage":{"input_tokens":982,"output_tokens":387,"prompt_tokens":null,"completion_tokens":null,"total_tokens":1369},"error":null,"status":"completed"}
```

### 2. Texto do Anuncio
- Data: 03/04, 06:53
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))
Metadados obrigatórios do experimento:
- primary_variable: Dor vs Resultado
- variant_id: variant-10
- stage: AD
- control_or_treatment: treatment
- asset_role: ad-copy

Tarefa alvo: ad-copy

Ângulo da campanha:
{"campaignAngle":{"singleMindedPromise":"Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.","primaryPain":"A conversa vira “quanto custa?” e os alunos somem nas primeiras semanas porque não enxergam valor rápido — aí sua agenda depende de indicação e o mês vira loteria.","primaryPromise":"Ter agenda cheia com constância em até 8 semanas, sem desconto, com alunos que chegam mais qualificados (entendendo o valor) e permanecem engajados no início.","cta":"Desbloquear o kit de “Agenda Cheia Sem Desconto (8 semanas)”","tone":"Direto, prático e confiante (sem hype), com foco em clareza e controle do resultado.","mechanismSummary":"Um kit de ativos gerados por IA com uma sequência pronta de comunicação + primeiros marcos de evolução (primeiras semanas) + check-ins automáticos para (1) posicionar seu diferencial antes do preço e (2) fazer o aluno sentir progresso e cuidado rápido.","primaryCTA":"Desbloquear o kit","landingMatchLine":"Desbloqueie o kit “Agenda Cheia Sem Desconto (8 semanas)” para atrair alunos que entendem seu valor e manter engajamento nas primeiras semanas — sem depender de indicação.","funnelStage":"Topo/Meio (aquisição + pré-qualificação)","proofSummary":"Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (reduz desistência, aumenta percepção de valor e melhora renovação), transformadas em sequência pronta e replicável em ativos digitais."},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"campaign-angle"}}

Texto do anúncio:
{"adCopy":{"primaryTextVariants":[{"label":"dor","openingHookType":"dor","headline":"Agenda cheia sem desconto (8 semanas)","placementHint":"feed","lengthVariants":{"curta":"A conversa com aluno vira “quanto custa?” e some na 2ª semana?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos entendendo seu valor e ficando engajados no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"A conversa vira “quanto custa?” cedo demais. O aluno entra empolgado… e some nas primeiras semanas. Aí sua agenda vira loteria de indicação e de preço.\n\nA ideia aqui é ter constância: em até 8 semanas, encher sua agenda sem dar desconto — com alunos chegando entendendo o valor do seu acompanhamento e permanecendo engajados no começo.\n\nPara isso, você desbloqueia uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto pra conduzir a conversa sem cair no preço cedo\n• marcos de progresso + check-ins das primeiras semanas\n\nSem call. Sem consultoria. Só ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"A conversa cai em “quanto custa?” e o aluno desengaja no começo?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e sentindo progresso logo nas primeiras semanas.\n\nDesbloqueie a prévia do Kit gerado por IA com mensagens prontas, roteiro de conversa e check-ins de onboarding.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA): mensagens + roteiro + check-ins.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"resultado","openingHookType":"resultado","headline":"Mais alunos certos. Menos preço.","placementHint":"stories/reels","lengthVariants":{"curta":"Agenda cheia em até 8 semanas — sem dar desconto.\n\nAlunos chegam entendendo seu valor e ficam engajados nas primeiras semanas.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Agenda cheia em até 8 semanas, sem dar desconto.\n\nA diferença não é “fazer mais”. É ter um ciclo simples para:\n1) o aluno entender seu valor antes de falar de preço\n2) o aluno sentir cuidado + progresso rápido nas primeiras semanas (quando mais desiste)\n\nDesbloqueie uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• convite e mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto de conversa (sem cair no preço cedo)\n• marcos iniciais + check-ins para segurar o engajamento\n\nSem call. Sem consultoria. Ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Em até 8 semanas, encha sua agenda sem dar desconto.\n\nCom uma sequência pronta, o aluno chega mais preparado (entende seu valor) e o começo já tem marcos + check-ins pra manter engajamento.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA) para captar e reter melhor.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"prova","openingHookType":"prova","headline":"Primeiros 30 dias: faz diferença","placementHint":"feed","lengthVariants":{"curta":"Os primeiros 30 dias costumam decidir se o aluno engaja ou some.\n\nEm até 8 semanas, encha sua agenda sem desconto com uma sequência que deixa seu valor claro + cria vitórias rápidas no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Os primeiros 30 dias costumam ser decisivos: quando o aluno tem clareza do caminho + pequenas vitórias logo no começo, a percepção de valor sobe e o engajamento tende a aumentar (boas práticas de onboarding e retenção).\n\nCom isso em mente, a promessa é simples: em até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas.\n\nDesbloqueie a prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto para conduzir a conversa antes do preço\n• marcos iniciais + check-ins para o aluno sentir progresso e cuidado\n\nSem call. Sem consultoria. Apenas ativos digitais.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Boas práticas de onboarding mostram: clareza + vitórias rápidas no começo aumentam a percepção de valor.\n\nEm até 8 semanas, encha sua agenda sem dar desconto com uma sequência pronta de comunicação + marcos + check-ins.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Estrutura com foco nos 30 primeiros dias (prévia).","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-copy"}}

Briefing da imagem:
{"adImageBriefing":{"briefings":[{"safeMargins":"Feed 1080x1350: manter 10% de margem de segurança (texto e elementos dentro da área central). Stories 1080x1920: evitar topo (UI) e base (CTA nativo); texto principal no terço central.","formatByPlacement":"Priorizar Feed (1080x1350) com leitura instantânea; adaptar para Stories/Reels (1080x1920) centralizando a metáfora visual e movendo CTA para acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação DOR do anúncio: dor = conversa vira “quanto custa?” cedo + aluno some nas primeiras semanas. Ainda assim, manter a promessa central igual à landing: agenda cheia sem desconto em até 8 semanas. CTA igual à landing: “Desbloquear a prévia/kit”.","imageTextMaxWords":9,"assetType":"estatico","hierarchy":"1) Dor (gatilho rápido) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy de compliance (se couber)","mustMatchAdVariant":"dor","visualAngle":"dor","complianceNotes":"Evitar garantias absolutas e resultados individuais. Não mencionar consultoria/call/gestão humana como entrega. “Gerado por IA” como característica, não milagre. Se usar “em até 8 semanas”, tratar como objetivo/estrutura (aspiracional). Microcopy opcional: “Sem call. Sem consultoria. Ativos digitais (IA).”","visualBriefing":"Conceito: ‘A conversa cai no preço + sumiço na semana 2’. Visual principal em split/contraste com dois balões de chat grandes no centro.\n\nComposição (Feed):\n- Fundo branco/cinza muito claro.\n- Metade esquerda (apagada/cinza): balão de chat com texto curto ‘Quanto custa?’ + ícone de pausa (||) ou ponto de interrogação.\n- Metade direita (ativa/azul escuro #0B1F3B): balão de chat com texto curto ‘Entendi como funciona’ + ícone de check verde #22C55E.\n- No topo, headline curta (máx. 1 linha): ‘Agenda cheia sem desconto’ (ou ‘Chega de briga de preço’ se couber sem perder promessa; manter promessa explícita em selo/linha 2).\n- Selo pequeno em verde: ‘8 SEMANAS’.\n- Rodapé com botão/box contrastante: ‘Desbloquear a prévia’.\n- Microcopy pequeno (se couber, 1 linha): ‘Ativos digitais gerados por IA’.\n\nTexto sugerido (<=9 palavras totais sobre imagem, priorizando):\n- Linha 1: ‘Agenda cheia sem desconto’ (4)\n- Selo: ‘em até 8 semanas’ (4) — se faltar espaço, usar apenas ‘8 semanas’ (2)\n- Botão: ‘Desbloquear a prévia’ (3)\n(Obs.: escolher combinações para respeitar o limite de palavras; preferir ‘Agenda cheia sem desconto’ + ‘8 semanas’ + ‘Desbloquear a prévia’).\n\nÍcones: chat, pause, check.\n\nAdaptação Stories: chats no terço central; headline acima; botão ‘Desbloquear a prévia’ logo abaixo dos chats (antes da área do CTA nativo)."},{"safeMargins":"Stories/Reels 1080x1920: texto e elementos críticos no terço central; evitar topo (UI) e base (CTA nativo). Feed 1080x1350: 10% de margem de segurança em todos os lados.","formatByPlacement":"Priorizar Stories/Reels (1080x1920) com layout vertical e timeline central; adaptar para Feed (1080x1350) mantendo a timeline com 3 marcos visuais e um selo ‘8 semanas’.","messageMatchNotes":"Deve bater com a variação RESULTADO do anúncio: ‘Agenda cheia em até 8 semanas — sem dar desconto’ + ‘alunos chegam entendendo seu valor e ficam engajados no começo’. Manter a mesma promessa do hero da landing e CTA de desbloqueio da prévia do kit.","imageTextMaxWords":10,"assetType":"story-vertical","hierarchy":"1) Resultado (agenda cheia sem desconto) 2) Prazo (8 semanas) 3) Benefício (alunos entendem valor + engajam) 4) CTA 5) Microcopy (IA / sem consultoria)","mustMatchAdVariant":"resultado","visualAngle":"resultado","complianceNotes":"Não prometer ‘agenda cheia garantida’. Usar linguagem aspiracional e de processo (‘com uma sequência pronta’). Não usar termos técnicos (funil/lead). Sem consultoria/call. Incluir ‘Ativos digitais (IA)’ quando possível.","visualBriefing":"Conceito: ‘8 semanas sob controle’ (timeline simples) para passar constância e previsibilidade.\n\nComposição (Stories):\n- Fundo branco com blocos/linhas finas em azul escuro (#0B1F3B) como moldura.\n- Centro: timeline vertical com 8 pontos (Sem 1 a Sem 8) e checks verdes (#22C55E) nos primeiros 3–4 para indicar progresso.\n- Ao lado de 3 pontos, mini-ícones:\n  - Sem 1: ícone chat (mensagens)\n  - Sem 2: ícone documento (roteiro)\n  - Sem 3: ícone check (check-ins)\n- Topo (texto grande): ‘Agenda cheia sem desconto’.\n- Logo abaixo (texto menor): ‘em até 8 semanas’.\n- Linha curta de benefício (se couber): ‘alunos entendem seu valor’.\n- Botão/box no terço inferior (mas acima da área do CTA nativo): ‘Desbloquear a prévia’.\n- Microcopy pequeno: ‘Ativos digitais (IA)’.\n\nTexto sugerido (<=10 palavras totais sobre imagem):\n- ‘Agenda cheia sem desconto’ (4)\n- ‘em até 8 semanas’ (4)\n- ‘Desbloquear a prévia’ (3)\n(Escolher 2 das 3 linhas conforme espaço para respeitar o limite; manter sempre promessa + CTA; o ‘8 semanas’ pode ir no selo).\n\nAdaptação Feed: timeline horizontal/compacta (8 marcadores) no meio; selo ‘8 semanas’; CTA no rodapé."},{"safeMargins":"Feed 1080x1350: 10% de margem de segurança. Stories 1080x1920: manter conteúdo principal no terço central e deixar respiro no topo/base para UI e CTA nativo.","formatByPlacement":"Priorizar Feed (1080x1350) com ‘card de prova’ + referência aos primeiros 30 dias; adaptar para Stories (1080x1920) mantendo o card central grande e CTA acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação PROVA do anúncio: ‘Primeiros 30 dias fazem diferença’ (boas práticas de onboarding) e conectar com a promessa de ‘agenda cheia sem desconto em até 8 semanas’. CTA deve ser ‘Desbloquear a prévia’ (equivalente ao ‘Desbloquear o Kit’ da landing).","imageTextMaxWords":11,"assetType":"carrossel","hierarchy":"1) Prova (primeiros 30 dias) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy (sem consultoria)","mustMatchAdVariant":"prova","visualAngle":"prova","complianceNotes":"Evitar ‘comprovado’/‘garantido’. Tratar como boas práticas/referência. Não usar logos de terceiros. Não prometer acompanhamento humano. Incluir ‘Ativos digitais gerados por IA’ como característica, não como promessa.","visualBriefing":"Formato: Carrossel 3 cards (1080x1350) — cada card com pouquíssimo texto e ícones claros.\n\nCard 1 (PROVA):\n- Headline: ‘Primeiros 30 dias’\n- Sub (curto): ‘decidem o engajamento’\n- Visual: mini-calendário 30 dias com 4 checks verdes (semanas) e uma seta apontando ‘continuidade’.\n\nCard 2 (CONEXÃO COM PROMESSA):\n- Headline: ‘8 semanas’\n- Badge: ‘SEM DESCONTO’\n- Sub (curto): ‘sequência pronta’\n- Visual: trilho/linha do tempo com 8 pontos + 2 ícones (chat + check).\n\nCard 3 (AÇÃO/PRODUTO):\n- Headline: ‘Desbloqueie a prévia’\n- Sub (curto): ‘mensagens + check-ins’\n- Micro (pequeno): ‘Ativos digitais (IA). Sem consultoria.’\n- Botão/box: ‘Desbloquear a prévia’.\n\nPaleta e estilo:\n- Fundo branco/cinza claro; textos em azul escuro (#0B1F3B); destaques em verde (#22C55E).\n- Tipografia sans bold para headline.\n\nControle de texto (<=11 palavras por card):\n- Card 1 exemplo: ‘Primeiros 30 dias decidem o engajamento’ (6)\n- Card 2 exemplo: ‘8 semanas. Sem desconto. Sequência pronta.’ (6)\n- Card 3 exemplo: ‘Desbloquear a prévia’ (3) + ‘Ativos digitais (IA)’ (3) (ajustar para não estourar).\n\nObservação: manter a promessa central explícita no Card 2 para garantir consistência com a landing."}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-image-briefing"}}

Textos da landing:
{"landingPageCopy":{"hero":{"headline":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","supportingCopy":"Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) com mensagens prontas, roteiro curto de conversa e check-ins iniciais para reduzir a briga por preço e aumentar engajamento no começo — sem call e sem consultoria.","eyebrow":"Agenda Cheia Sem Desconto (8 semanas)","ctaMatchNotes":"O CTA é idêntico ao do anúncio e leva para o desbloqueio da prévia do kit (ativos digitais gerados por IA).","microcopy":"Entrega 100% digital. Sem consultoria, sem call, sem gestão humana. Ativos gerados por IA para você adaptar e aplicar.","promise":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","subheadline":"Pare de depender de indicação e de “briga de preço”: tenha um processo simples, repetível e aplicável todo mês.","ctaUrl":"#desbloquear","proofBadge":"Baseado em boas práticas de onboarding e primeiros 30 dias (retenção e percepção de valor)."},"messageMatchNotes":"A promessa central e o CTA repetem literalmente o anúncio: agenda cheia sem desconto em até 8 semanas + desbloquear a prévia do kit gerada por IA.","ctaBlocks":[{"ctaSupport":"Desbloqueie a prévia e veja como ficam as mensagens, os marcos e os check-ins das primeiras semanas.","messageMatchNotes":"CTA no hero repete o mesmo texto do anúncio para continuidade imediata após o clique.","ctaVariant":"primary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"hero","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Se a conversa cai no preço cedo, use a sequência para deixar o valor claro antes do “quanto custa?”.","messageMatchNotes":"CTA no meio aparece logo após explicar mecanismo (sequência) e como aplicar sem depender de consultoria.","ctaVariant":"secondary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"mid","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Leve a prévia agora e adapte em minutos para sua rotina (WhatsApp/DM/e-mail).","messageMatchNotes":"CTA final reforça ação única (desbloquear a prévia) após prova + oferta para reduzir ceticismo.","ctaVariant":"primary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"final","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Prévia do kit (IA) para usar hoje: mensagens + roteiro + check-ins.","messageMatchNotes":"CTA sticky mantém a mesma ação do anúncio durante toda a leitura, sem mudar a intenção.","ctaVariant":"sticky","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"sticky","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"}],"consistencyChecks":[{"details":"CTA na página (hero/meio/final/sticky) usa exatamente ‘Desbloquear o Kit (receber a prévia gerada por IA)’, igual ao anúncio.","check":"CTA_MATCH","status":"PASS"},{"details":"Hero.headline, hero.promise e pageGoal repetem literalmente a single-minded promise fornecida no ângulo da campanha (sem variações criativas).","check":"PROMISE_MATCH","status":"PASS"},{"details":"Página com message match forte, hierarquia clara (dor→resultado→mecanismo→prova→ação), CTAs repetidos, microcopy de entrega e FAQs para reduzir objeções.","check":"GOOGLE_LANDING_BEST_PRACTICES","status":"PASS"}],"bodySections":[{"messageMatchNotes":"Conecta diretamente com a dor citada no anúncio (conversa vira ‘quanto custa?’ e aluno some no começo) e prepara o terreno para a promessa sem trocar o foco.","bullets":["A conversa cai no preço cedo demais — e você perde margem (ou perde o aluno).","O aluno entra animado… e some na 2ª semana porque não enxerga progresso rápido.","Sem um ‘primeiro mês’ bem guiado, o valor do acompanhamento fica invisível.","Resultado: agenda vira loteria de indicação, de mês a mês."],"ctaSupport":"Quer ver a sequência pronta que organiza isso em 8 semanas?","summary":"Se você não deixa claro o seu valor antes do preço e não cria vitórias rápidas no começo, o aluno desengaja — e sua agenda vira loteria.","sectionDependsOn":"primaryPromise","sectionId":"s1-pain-preco-engajamento","sectionType":"pain","copy":"Quando não existe um processo simples e repetível, duas coisas acontecem quase sempre:\n\n1) a conversa vira ‘quanto custa?’ cedo demais (antes do aluno entender o diferente do seu acompanhamento)\n2) nas primeiras semanas, o aluno não sente cuidado/progresso e começa a faltar, enrolar ou desistir\n\nAí o que deveria ser previsível vira estressante: você vive de indicação, de promoção e de negociação.","title":"O que te trava não é falta de aluno. É falta de processo."},{"messageMatchNotes":"Explica o mecanismo como suporte (sequência pronta de comunicação + marcos + check-ins), sem transformá-lo na promessa principal; mantém o match com o anúncio.","bullets":["Mensagens prontas para conduzir a conversa sem cair no preço cedo (WhatsApp/DM/e-mail).","Roteiro curto para apresentar seu ‘porquê’ e o que muda no acompanhamento.","Marcos das primeiras semanas para o aluno perceber evolução rápido.","Check-ins simples (prontos) para reforçar cuidado e consistência no início."],"ctaSupport":"Desbloqueie a prévia e veja exemplos prontos para adaptar ao seu serviço.","summary":"Você aplica uma sequência pronta para o aluno entender seu valor antes do preço e sentir progresso logo no começo.","sectionDependsOn":"mechanismSummary","sectionId":"s2-mechanism-sequencia-pronta","sectionType":"mechanism","copy":"A proposta é colocar sob controle o que normalmente fica “no improviso”:\n\n• Comunicação de entrada: mensagens e roteiro para o aluno entender o valor do seu acompanhamento antes do preço dominar a conversa.\n• Primeiras vitórias: marcos claros nas semanas iniciais para o aluno sentir progresso rápido.\n• Check-ins: pontos de contato prontos para manter engajamento no começo (quando a desistência é mais comum).\n\nTudo em formato de ativos digitais gerados por IA — para você copiar, ajustar e aplicar.","title":"O que muda: uma sequência pronta (com começo, meio e continuidade)"},{"messageMatchNotes":"Apoia a promessa com ‘prova’ em forma de boas práticas (onboarding/primeiros 30 dias) sem alegações absolutas; alinhado ao anúncio de prova e ao briefing.","bullets":["Clareza do caminho reduz ansiedade e aumenta a percepção de valor.","Vitórias rápidas no começo aumentam a chance de continuidade.","Check-ins consistentes reduzem ‘sumiço’ e melhoram engajamento."],"ctaSupport":"Quer ver como isso vira mensagens e check-ins prontos?","summary":"Os primeiros 30 dias são decisivos: clareza + vitórias rápidas tendem a aumentar percepção de valor e engajamento.","sectionDependsOn":"proofSummary","sectionId":"s3-proof-30-dias","sectionType":"proof","copy":"Em boas práticas de onboarding e retenção, o início costuma ser decisivo:\n\nQuando o aluno entende o caminho (o que vai acontecer e por quê) e percebe pequenos avanços logo nas primeiras semanas, a percepção de valor sobe — e o engajamento tende a acompanhar.\n\nPor isso o kit foca em organizar comunicação + marcos + check-ins justamente onde mais dá errado: no começo.","title":"Por que funciona: o começo (primeiros 30 dias) define o resto"},{"messageMatchNotes":"Oferta descrita como prévia do kit (ativos digitais gerados por IA), sem prometer consultoria/call; mantém a promessa central como headline/hero, e a oferta como meio para chegar lá.","bullets":["Prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA.","Mensagens prontas (WhatsApp/DM/e-mail) para antes e depois do ‘primeiro sim’.","Roteiro curto para conduzir a conversa e posicionar valor antes do preço.","Marcos das primeiras semanas + check-ins prontos para manter engajamento.","Modelos editáveis para adaptar ao seu estilo (copiar/colar/ajustar)."],"ctaSupport":"Clique para desbloquear a prévia e receber os exemplos gerados por IA.","summary":"Você recebe uma prévia do kit com os ativos essenciais para aplicar a sequência de 8 semanas na prática.","sectionDependsOn":"primaryCTA","sectionId":"s4-offer-previa-kit","sectionType":"offer","copy":"Ao desbloquear, você recebe uma prévia do kit com exemplos prontos para usar como base.\n\nImportante: é entrega digital (ativos), feita para escala — sem call, sem consultoria e sem acompanhamento manual.\n\nVocê adapta ao seu serviço e aplica no seu ritmo.","title":"O que você recebe na prévia (entrega digital)"}],"messageMatchSource":"Agenda cheia sem desconto (8 semanas)","pageGoal":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","primaryCTA":"Desbloquear o Kit (receber a prévia gerada por IA)","faq":[{"question":"Isso é consultoria, mentoria ou tem call?","answer":"Não. A entrega é 100% digital: ativos gerados por IA (mensagens, roteiro, marcos e check-ins) para você adaptar e aplicar. Não inclui call, consultoria, gestão humana ou acompanhamento manual.","objectionTag":"entrega_sem_consultoria"},{"question":"Serve para qualquer tipo de acompanhamento (treino, nutrição, estética, aulas, etc.)?","answer":"Sim, porque a lógica é de comunicação e onboarding: deixar o valor claro antes do preço e criar marcos iniciais + check-ins. Você só ajusta a linguagem e os marcos para sua realidade.","objectionTag":"serve_para_meu_nicho"},{"question":"E se eu já tenho alunos, mas eles desengajam nas primeiras semanas?","answer":"O kit foi pensado exatamente para esse ponto: organizar os primeiros marcos e os check-ins para o aluno sentir cuidado e progresso rápido no começo, reduzindo o ‘sumiço’ e aumentando a continuidade.","objectionTag":"retencao_engajamento"},{"question":"Isso garante agenda cheia em 8 semanas?","answer":"Não existe garantia de resultado individual. A promessa é um objetivo com base em um processo repetível (sequência de comunicação + marcos + check-ins) que costuma melhorar percepção de valor e engajamento quando aplicado de forma consistente.","objectionTag":"sem_garantia_absoluta"}],"complianceNotes":"A oferta é uma prévia de um kit de ativos digitais gerados por IA (mensagens, roteiro, marcos e check-ins). Não há promessa de consultoria, call, gestão humana ou acompanhamento manual. Evita garantias absolutas e resultados individuais; ‘em até 8 semanas’ é tratado como objetivo/estrutura baseada em processo."},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"landing-page-copy"}}

Diretriz específica para texto do anúncio:
Você cria ativos de campanha para o Marketing Hub.

Regras globais:
1. O anúncio e a landing devem ter a mesma promessa central.
2. O CTA do anúncio deve combinar com a ação principal da landing.
3. O material precisa caber no envelope real do produto:
   - pode entregar ativos digitais gerados por IA
   - não pode prometer consultoria, call, gestão humana ou acompanhamento manual
4. Priorize clareza comercial:
   DOR → RESULTADO → MECANISMO → PROVA → AÇÃO
5. Não transforme mecanismo em promessa principal.
6. Não use jargão técnico desnecessário.
7. O público é geral dentro do nicho, com baixa a moderada maturidade em marketing.
8. Sempre escreva pensando em alta escala e geração automatizada.
9. O anúncio deve ser rápido de entender.
10. A landing deve aprofundar a promessa e reduzir ceticismo.

Contexto do nicho: Profissionais da Área de Educação Física - Personal Trainer

Ângulo da campanha: {"campaignAngle":{"singleMindedPromise":"Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.","primaryPain":"A conversa vira “quanto custa?” e os alunos somem nas primeiras semanas porque não enxergam valor rápido — aí sua agenda depende de indicação e o mês vira loteria.","primaryPromise":"Ter agenda cheia com constância em até 8 semanas, sem desconto, com alunos que chegam mais qualificados (entendendo o valor) e permanecem engajados no início.","cta":"Desbloquear o kit de “Agenda Cheia Sem Desconto (8 semanas)”","tone":"Direto, prático e confiante (sem hype), com foco em clareza e controle do resultado.","mechanismSummary":"Um kit de ativos gerados por IA com uma sequência pronta de comunicação + primeiros marcos de evolução (primeiras semanas) + check-ins automáticos para (1) posicionar seu diferencial antes do preço e (2) fazer o aluno sentir progresso e cuidado rápido.","primaryCTA":"Desbloquear o kit","landingMatchLine":"Desbloqueie o kit “Agenda Cheia Sem Desconto (8 semanas)” para atrair alunos que entendem seu valor e manter engajamento nas primeiras semanas — sem depender de indicação.","funnelStage":"Topo/Meio (aquisição + pré-qualificação)","proofSummary":"Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (reduz desistência, aumenta percepção de valor e melhora renovação), transformadas em sequência pronta e replicável em ativos digitais."},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"campaign-angle"}}
Dor principal: A conversa vira “quanto custa?” e os alunos somem nas primeiras semanas porque não enxergam valor rápido — aí sua agenda depende de indicação e o mês vira loteria.
Promessa principal: Ter agenda cheia com constância em até 8 semanas, sem desconto, com alunos que chegam mais qualificados (entendendo o valor) e permanecem engajados no início.
Mecanismo resumido: Um kit de ativos gerados por IA com uma sequência pronta de comunicação + primeiros marcos de evolução (primeiras semanas) + check-ins automáticos para (1) posicionar seu diferencial antes do preço e (2) fazer o aluno sentir progresso e cuidado rápido.
Prova resumida: Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (reduz desistência, aumenta percepção de valor e melhora renovação), transformadas em sequência pronta e replicável em ativos digitais.

Promessa single-minded: Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.
CTA principal: Desbloquear o kit
Linha de match com landing: Desbloqueie o kit “Agenda Cheia Sem Desconto (8 semanas)” para atrair alunos que entendem seu valor e manter engajamento nas primeiras semanas — sem depender de indicação.

Objetivo do anúncio:
Gerar clique qualificado para a landing page.

Regras:
1. O texto do anúncio deve ser entendido em poucos segundos.
2. A primeira linha deve abrir com dor, consequência, resultado ou prova.
3. O mecanismo deve aparecer só depois do benefício principal.
4. O anúncio não pode parecer consultoria.
5. A promessa precisa ser compatível com ativos digitais gerados por IA.
6. Não usar jargão de tráfego pago.
7. Criar 3 variações:
   - V1 focada na dor
   - V2 focada no resultado
   - V3 focada na prova
8. Para cada variação, entregar 3 comprimentos de texto principal: curta, media e longa.
9. Definir openingHookType por variação com um valor entre: dor, consequência, resultado, prova.
10. Definir placementHint por variação com um valor entre: feed, stories/reels.
11. Aplicar trava de compliance em todas as variações:
    - sem garantia absoluta
    - sem promessa individual
    - sem linguagem de consultoria
12. O CTA deve combinar exatamente com a landing.
13. Entregar copy testável por placement e comprimento para Meta Ads.

Entregue apenas o JSON abaixo sem texto adicional:
{
  "adCopy": {
    "primaryTextVariants": [
    {
      "label": "dor",
      "openingHookType": "dor",
      "placementHint": "feed",
      "lengthVariants": {
        "curta": "",
        "media": "",
        "longa": ""
      },
      "headline": "",
      "description": "",
      "ctaText": "",
      "compliance": {
        "semGarantiaAbsoluta": true,
        "semPromessaIndividual": true,
        "semLinguagemDeConsultoria": true
      }
    },
    {
      "label": "resultado",
      "openingHookType": "resultado",
      "placementHint": "stories/reels",
      "lengthVariants": {
        "curta": "",
        "media": "",
        "longa": ""
      },
      "headline": "",
      "description": "",
      "ctaText": "",
      "compliance": {
        "semGarantiaAbsoluta": true,
        "semPromessaIndividual": true,
        "semLinguagemDeConsultoria": true
      }
    },
    {
      "label": "prova",
      "openingHookType": "prova",
      "placementHint": "feed",
      "lengthVariants": {
        "curta": "",
        "media": "",
        "longa": ""
      },
      "headline": "",
      "description": "",
      "ctaText": "",
      "compliance": {
        "semGarantiaAbsoluta": true,
        "semPromessaIndividual": true,
        "semLinguagemDeConsultoria": true
      }
    }
    ]
  },
  "experimentMetadata": {
    "primary_variable": "",
    "variant_id": "",
    "stage": "",
    "control_or_treatment": "",
    "asset_role": "ad-copy"
  }
}

Instruções extras do usuário:
Quantidade sugerida: 3

Responda exclusivamente em JSON válido e siga estritamente o schema da seção.
```

**Resultado**
```text
{"output_text":null,"id":"resp_04f4b21f0698e7010069cf8dfd447481968bcd00ea5be46f85","output":[{"tool_call":null,"tool_calls":null,"id":"msg_04f4b21f0698e7010069cf8dfe1ad8819682c142c4b3e3197d","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"adCopy\":{\"primaryTextVariants\":[{\"label\":\"dor\",\"openingHookType\":\"dor\",\"headline\":\"Agenda cheia sem desconto (8 semanas)\",\"placementHint\":\"feed\",\"lengthVariants\":{\"curta\":\"A conversa vira “quanto custa?” e o aluno some na 2ª semana?\\n\\nEncha sua agenda em até 8 semanas sem dar desconto — com alunos que chegam entendendo o valor do seu acompanhamento e seguem engajados no começo.\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\",\"longa\":\"A conversa vira “quanto custa?” cedo demais.\\nO aluno fecha, treina 1–2 semanas… e some porque não sente valor rápido.\\nNo fim, sua agenda depende de indicação e o mês vira loteria.\\n\\nA meta é ter constância: encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.\\n\\nComo você aplica isso:\\n• sequência pronta de mensagens (WhatsApp/DM/e-mail)\\n• roteiro curto pra conduzir a conversa sem cair no preço cedo\\n• marcos iniciais + check-ins para o aluno sentir cuidado e progresso no começo\\n\\nEntrega 100% digital, gerada por IA. Sem call e sem consultoria.\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\",\"media\":\"O aluno pergunta preço, fecha… e some no começo?\\n\\nEncha sua agenda em até 8 semanas sem dar desconto — com alunos entendendo seu valor e ficando engajados nas primeiras semanas.\\n\\nDesbloqueie a prévia do kit gerado por IA com mensagens prontas, roteiro de conversa e check-ins iniciais.\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\"},\"compliance\":{\"semPromessaIndividual\":true,\"semGarantiaAbsoluta\":true,\"semLinguagemDeConsultoria\":true},\"description\":\"Prévia do kit (IA): mensagens + roteiro + check-ins.\",\"primaryText\":\"\",\"ctaText\":\"Desbloquear o Kit (receber a prévia gerada por IA)\"},{\"label\":\"resultado\",\"openingHookType\":\"resultado\",\"headline\":\"Mais alunos certos. Menos briga de preço.\",\"placementHint\":\"stories/reels\",\"lengthVariants\":{\"curta\":\"Encha sua agenda em até 8 semanas sem dar desconto.\\n\\nAlunos chegam entendendo seu valor e seguem engajados no começo.\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\",\"longa\":\"Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.\\n\\nEm vez de depender de indicação, você usa uma sequência simples para:\\n1) deixar seu diferencial claro antes do preço\\n2) criar progresso perceptível logo no começo (quando muita gente desiste)\\n\\nDesbloqueie a prévia do kit gerado por IA:\\n• mensagens prontas (WhatsApp/DM/e-mail)\\n• roteiro curto de conversa\\n• marcos iniciais + check-ins das primeiras semanas\\n\\nEntrega digital. Sem call. Sem consultoria.\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\",\"media\":\"Encha sua agenda em até 8 semanas sem dar desconto.\\n\\nCom alunos que chegam entendendo seu valor e continuam engajados nas primeiras semanas.\\n\\nDesbloqueie a prévia do kit gerado por IA (mensagens + roteiro + check-ins).\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\"},\"compliance\":{\"semPromessaIndividual\":true,\"semGarantiaAbsoluta\":true,\"semLinguagemDeConsultoria\":true},\"description\":\"Prévia do kit (IA) para captar e engajar no início.\",\"primaryText\":\"\",\"ctaText\":\"Desbloquear o Kit (receber a prévia gerada por IA)\"},{\"label\":\"prova\",\"openingHookType\":\"prova\",\"headline\":\"O começo decide a permanência\",\"placementHint\":\"feed\",\"lengthVariants\":{\"curta\":\"Os primeiros 30 dias costumam decidir se o aluno engaja ou some.\\n\\nEncha sua agenda em até 8 semanas sem dar desconto — com valor claro antes do preço e marcos iniciais.\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\",\"longa\":\"Boas práticas de onboarding mostram um padrão: quando o aluno entende o caminho e tem pequenas vitórias no começo, a percepção de valor sobe e o engajamento tende a aumentar — especialmente nos primeiros 30 dias.\\n\\nPor isso a promessa é direta: encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.\\n\\nDesbloqueie a prévia do kit gerado por IA com:\\n• mensagens prontas (WhatsApp/DM/e-mail)\\n• roteiro curto para conduzir a conversa antes do preço\\n• marcos iniciais + check-ins para o aluno sentir progresso e cuidado\\n\\nEntrega digital. Sem call. Sem consultoria.\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\",\"media\":\"Primeiros 30 dias bem guiados costumam aumentar a percepção de valor.\\n\\nEncha sua agenda em até 8 semanas sem dar desconto — com uma sequência pronta de comunicação + marcos + check-ins.\\n\\nDesbloqueie a prévia do kit gerado por IA.\\n\\nDesbloquear o Kit (receber a prévia gerada por IA)\"},\"compliance\":{\"semPromessaIndividual\":true,\"semGarantiaAbsoluta\":true,\"semLinguagemDeConsultoria\":true},\"description\":\"Baseado em boas práticas dos primeiros 30 dias (prévia).\",\"primaryText\":\"\",\"ctaText\":\"Desbloquear o Kit (receber a prévia gerada por IA)\"}]},\"experimentMetadata\":{\"variant_id\":\"variant-10\",\"control_or_treatment\":\"treatment\",\"primary_variable\":\"Dor vs Resultado\",\"stage\":\"AD\",\"asset_role\":\"ad-copy\"}}"}]}],"usage":{"input_tokens":8927,"output_tokens":1318,"prompt_tokens":null,"completion_tokens":null,"total_tokens":10245},"error":null,"status":"completed"}
```

### 3. Prompt da Imagem
- Data: 06/04, 11:32
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Nome do experimento: Experimento 4
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))
Metadados obrigatórios do experimento:
- primary_variable: Dor vs Resultado
- variant_id: variant-10
- stage: AD
- control_or_treatment: treatment
- asset_role: ad-image-briefing

Tarefa alvo: ad-image-briefing

Ângulo da campanha:
{"campaignAngle":{"singleMindedPromise":"Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.","primaryPain":"A conversa vira “quanto custa?” e os alunos somem nas primeiras semanas porque não enxergam valor rápido — aí sua agenda depende de indicação e o mês vira loteria.","primaryPromise":"Ter agenda cheia com constância em até 8 semanas, sem desconto, com alunos que chegam mais qualificados (entendendo o valor) e permanecem engajados no início.","cta":"Desbloquear o kit de “Agenda Cheia Sem Desconto (8 semanas)”","tone":"Direto, prático e confiante (sem hype), com foco em clareza e controle do resultado.","mechanismSummary":"Um kit de ativos gerados por IA com uma sequência pronta de comunicação + primeiros marcos de evolução (primeiras semanas) + check-ins automáticos para (1) posicionar seu diferencial antes do preço e (2) fazer o aluno sentir progresso e cuidado rápido.","primaryCTA":"Desbloquear o kit","landingMatchLine":"Desbloqueie o kit “Agenda Cheia Sem Desconto (8 semanas)” para atrair alunos que entendem seu valor e manter engajamento nas primeiras semanas — sem depender de indicação.","funnelStage":"Topo/Meio (aquisição + pré-qualificação)","proofSummary":"Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (reduz desistência, aumenta percepção de valor e melhora renovação), transformadas em sequência pronta e replicável em ativos digitais."},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"campaign-angle"}}

Texto do anúncio:
{"adCopy":{"primaryTextVariants":[{"label":"dor","openingHookType":"dor","headline":"Agenda cheia sem desconto (8 semanas)","placementHint":"feed","lengthVariants":{"curta":"A conversa vira “quanto custa?” e o aluno some na 2ª semana?\n\nEncha sua agenda em até 8 semanas sem dar desconto — com alunos que chegam entendendo o valor do seu acompanhamento e seguem engajados no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA)","longa":"A conversa vira “quanto custa?” cedo demais.\nO aluno fecha, treina 1–2 semanas… e some porque não sente valor rápido.\nNo fim, sua agenda depende de indicação e o mês vira loteria.\n\nA meta é ter constância: encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.\n\nComo você aplica isso:\n• sequência pronta de mensagens (WhatsApp/DM/e-mail)\n• roteiro curto pra conduzir a conversa sem cair no preço cedo\n• marcos iniciais + check-ins para o aluno sentir cuidado e progresso no começo\n\nEntrega 100% digital, gerada por IA. Sem call e sem consultoria.\n\nDesbloquear o Kit (receber a prévia gerada por IA)","media":"O aluno pergunta preço, fecha… e some no começo?\n\nEncha sua agenda em até 8 semanas sem dar desconto — com alunos entendendo seu valor e ficando engajados nas primeiras semanas.\n\nDesbloqueie a prévia do kit gerado por IA com mensagens prontas, roteiro de conversa e check-ins iniciais.\n\nDesbloquear o Kit (receber a prévia gerada por IA)"},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do kit (IA): mensagens + roteiro + check-ins.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"resultado","openingHookType":"resultado","headline":"Mais alunos certos. Menos briga de preço.","placementHint":"stories/reels","lengthVariants":{"curta":"Encha sua agenda em até 8 semanas sem dar desconto.\n\nAlunos chegam entendendo seu valor e seguem engajados no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA)","longa":"Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.\n\nEm vez de depender de indicação, você usa uma sequência simples para:\n1) deixar seu diferencial claro antes do preço\n2) criar progresso perceptível logo no começo (quando muita gente desiste)\n\nDesbloqueie a prévia do kit gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto de conversa\n• marcos iniciais + check-ins das primeiras semanas\n\nEntrega digital. Sem call. Sem consultoria.\n\nDesbloquear o Kit (receber a prévia gerada por IA)","media":"Encha sua agenda em até 8 semanas sem dar desconto.\n\nCom alunos que chegam entendendo seu valor e continuam engajados nas primeiras semanas.\n\nDesbloqueie a prévia do kit gerado por IA (mensagens + roteiro + check-ins).\n\nDesbloquear o Kit (receber a prévia gerada por IA)"},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do kit (IA) para captar e engajar no início.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"prova","openingHookType":"prova","headline":"O começo decide a permanência","placementHint":"feed","lengthVariants":{"curta":"Os primeiros 30 dias costumam decidir se o aluno engaja ou some.\n\nEncha sua agenda em até 8 semanas sem dar desconto — com valor claro antes do preço e marcos iniciais.\n\nDesbloquear o Kit (receber a prévia gerada por IA)","longa":"Boas práticas de onboarding mostram um padrão: quando o aluno entende o caminho e tem pequenas vitórias no começo, a percepção de valor sobe e o engajamento tende a aumentar — especialmente nos primeiros 30 dias.\n\nPor isso a promessa é direta: encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.\n\nDesbloqueie a prévia do kit gerado por IA com:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto para conduzir a conversa antes do preço\n• marcos iniciais + check-ins para o aluno sentir progresso e cuidado\n\nEntrega digital. Sem call. Sem consultoria.\n\nDesbloquear o Kit (receber a prévia gerada por IA)","media":"Primeiros 30 dias bem guiados costumam aumentar a percepção de valor.\n\nEncha sua agenda em até 8 semanas sem dar desconto — com uma sequência pronta de comunicação + marcos + check-ins.\n\nDesbloqueie a prévia do kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)"},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Baseado em boas práticas dos primeiros 30 dias (prévia).","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-copy"}}

Textos da landing:
{"landingPageCopy":{"hero":{"headline":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","supportingCopy":"Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) com mensagens prontas, roteiro curto de conversa e check-ins iniciais para reduzir a briga por preço e aumentar engajamento no começo — sem call e sem consultoria.","eyebrow":"Agenda Cheia Sem Desconto (8 semanas)","ctaMatchNotes":"O CTA é idêntico ao do anúncio e leva para o desbloqueio da prévia do kit (ativos digitais gerados por IA).","microcopy":"Entrega 100% digital. Sem consultoria, sem call, sem gestão humana. Ativos gerados por IA para você adaptar e aplicar.","promise":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","subheadline":"Pare de depender de indicação e de “briga de preço”: tenha um processo simples, repetível e aplicável todo mês.","ctaUrl":"#desbloquear","proofBadge":"Baseado em boas práticas de onboarding e primeiros 30 dias (retenção e percepção de valor)."},"messageMatchNotes":"A promessa central e o CTA repetem literalmente o anúncio: agenda cheia sem desconto em até 8 semanas + desbloquear a prévia do kit gerada por IA.","ctaBlocks":[{"ctaSupport":"Desbloqueie a prévia e veja como ficam as mensagens, os marcos e os check-ins das primeiras semanas.","messageMatchNotes":"CTA no hero repete o mesmo texto do anúncio para continuidade imediata após o clique.","ctaVariant":"primary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"hero","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Se a conversa cai no preço cedo, use a sequência para deixar o valor claro antes do “quanto custa?”.","messageMatchNotes":"CTA no meio aparece logo após explicar mecanismo (sequência) e como aplicar sem depender de consultoria.","ctaVariant":"secondary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"mid","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Leve a prévia agora e adapte em minutos para sua rotina (WhatsApp/DM/e-mail).","messageMatchNotes":"CTA final reforça ação única (desbloquear a prévia) após prova + oferta para reduzir ceticismo.","ctaVariant":"primary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"final","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Prévia do kit (IA) para usar hoje: mensagens + roteiro + check-ins.","messageMatchNotes":"CTA sticky mantém a mesma ação do anúncio durante toda a leitura, sem mudar a intenção.","ctaVariant":"sticky","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"sticky","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"}],"consistencyChecks":[{"details":"CTA na página (hero/meio/final/sticky) usa exatamente ‘Desbloquear o Kit (receber a prévia gerada por IA)’, igual ao anúncio.","check":"CTA_MATCH","status":"PASS"},{"details":"Hero.headline, hero.promise e pageGoal repetem literalmente a single-minded promise fornecida no ângulo da campanha (sem variações criativas).","check":"PROMISE_MATCH","status":"PASS"},{"details":"Página com message match forte, hierarquia clara (dor→resultado→mecanismo→prova→ação), CTAs repetidos, microcopy de entrega e FAQs para reduzir objeções.","check":"GOOGLE_LANDING_BEST_PRACTICES","status":"PASS"}],"bodySections":[{"messageMatchNotes":"Conecta diretamente com a dor citada no anúncio (conversa vira ‘quanto custa?’ e aluno some no começo) e prepara o terreno para a promessa sem trocar o foco.","bullets":["A conversa cai no preço cedo demais — e você perde margem (ou perde o aluno).","O aluno entra animado… e some na 2ª semana porque não enxerga progresso rápido.","Sem um ‘primeiro mês’ bem guiado, o valor do acompanhamento fica invisível.","Resultado: agenda vira loteria de indicação, de mês a mês."],"ctaSupport":"Quer ver a sequência pronta que organiza isso em 8 semanas?","summary":"Se você não deixa claro o seu valor antes do preço e não cria vitórias rápidas no começo, o aluno desengaja — e sua agenda vira loteria.","sectionDependsOn":"primaryPromise","sectionId":"s1-pain-preco-engajamento","sectionType":"pain","copy":"Quando não existe um processo simples e repetível, duas coisas acontecem quase sempre:\n\n1) a conversa vira ‘quanto custa?’ cedo demais (antes do aluno entender o diferente do seu acompanhamento)\n2) nas primeiras semanas, o aluno não sente cuidado/progresso e começa a faltar, enrolar ou desistir\n\nAí o que deveria ser previsível vira estressante: você vive de indicação, de promoção e de negociação.","title":"O que te trava não é falta de aluno. É falta de processo."},{"messageMatchNotes":"Explica o mecanismo como suporte (sequência pronta de comunicação + marcos + check-ins), sem transformá-lo na promessa principal; mantém o match com o anúncio.","bullets":["Mensagens prontas para conduzir a conversa sem cair no preço cedo (WhatsApp/DM/e-mail).","Roteiro curto para apresentar seu ‘porquê’ e o que muda no acompanhamento.","Marcos das primeiras semanas para o aluno perceber evolução rápido.","Check-ins simples (prontos) para reforçar cuidado e consistência no início."],"ctaSupport":"Desbloqueie a prévia e veja exemplos prontos para adaptar ao seu serviço.","summary":"Você aplica uma sequência pronta para o aluno entender seu valor antes do preço e sentir progresso logo no começo.","sectionDependsOn":"mechanismSummary","sectionId":"s2-mechanism-sequencia-pronta","sectionType":"mechanism","copy":"A proposta é colocar sob controle o que normalmente fica “no improviso”:\n\n• Comunicação de entrada: mensagens e roteiro para o aluno entender o valor do seu acompanhamento antes do preço dominar a conversa.\n• Primeiras vitórias: marcos claros nas semanas iniciais para o aluno sentir progresso rápido.\n• Check-ins: pontos de contato prontos para manter engajamento no começo (quando a desistência é mais comum).\n\nTudo em formato de ativos digitais gerados por IA — para você copiar, ajustar e aplicar.","title":"O que muda: uma sequência pronta (com começo, meio e continuidade)"},{"messageMatchNotes":"Apoia a promessa com ‘prova’ em forma de boas práticas (onboarding/primeiros 30 dias) sem alegações absolutas; alinhado ao anúncio de prova e ao briefing.","bullets":["Clareza do caminho reduz ansiedade e aumenta a percepção de valor.","Vitórias rápidas no começo aumentam a chance de continuidade.","Check-ins consistentes reduzem ‘sumiço’ e melhoram engajamento."],"ctaSupport":"Quer ver como isso vira mensagens e check-ins prontos?","summary":"Os primeiros 30 dias são decisivos: clareza + vitórias rápidas tendem a aumentar percepção de valor e engajamento.","sectionDependsOn":"proofSummary","sectionId":"s3-proof-30-dias","sectionType":"proof","copy":"Em boas práticas de onboarding e retenção, o início costuma ser decisivo:\n\nQuando o aluno entende o caminho (o que vai acontecer e por quê) e percebe pequenos avanços logo nas primeiras semanas, a percepção de valor sobe — e o engajamento tende a acompanhar.\n\nPor isso o kit foca em organizar comunicação + marcos + check-ins justamente onde mais dá errado: no começo.","title":"Por que funciona: o começo (primeiros 30 dias) define o resto"},{"messageMatchNotes":"Oferta descrita como prévia do kit (ativos digitais gerados por IA), sem prometer consultoria/call; mantém a promessa central como headline/hero, e a oferta como meio para chegar lá.","bullets":["Prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA.","Mensagens prontas (WhatsApp/DM/e-mail) para antes e depois do ‘primeiro sim’.","Roteiro curto para conduzir a conversa e posicionar valor antes do preço.","Marcos das primeiras semanas + check-ins prontos para manter engajamento.","Modelos editáveis para adaptar ao seu estilo (copiar/colar/ajustar)."],"ctaSupport":"Clique para desbloquear a prévia e receber os exemplos gerados por IA.","summary":"Você recebe uma prévia do kit com os ativos essenciais para aplicar a sequência de 8 semanas na prática.","sectionDependsOn":"primaryCTA","sectionId":"s4-offer-previa-kit","sectionType":"offer","copy":"Ao desbloquear, você recebe uma prévia do kit com exemplos prontos para usar como base.\n\nImportante: é entrega digital (ativos), feita para escala — sem call, sem consultoria e sem acompanhamento manual.\n\nVocê adapta ao seu serviço e aplica no seu ritmo.","title":"O que você recebe na prévia (entrega digital)"}],"messageMatchSource":"Agenda cheia sem desconto (8 semanas)","pageGoal":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","primaryCTA":"Desbloquear o Kit (receber a prévia gerada por IA)","faq":[{"question":"Isso é consultoria, mentoria ou tem call?","answer":"Não. A entrega é 100% digital: ativos gerados por IA (mensagens, roteiro, marcos e check-ins) para você adaptar e aplicar. Não inclui call, consultoria, gestão humana ou acompanhamento manual.","objectionTag":"entrega_sem_consultoria"},{"question":"Serve para qualquer tipo de acompanhamento (treino, nutrição, estética, aulas, etc.)?","answer":"Sim, porque a lógica é de comunicação e onboarding: deixar o valor claro antes do preço e criar marcos iniciais + check-ins. Você só ajusta a linguagem e os marcos para sua realidade.","objectionTag":"serve_para_meu_nicho"},{"question":"E se eu já tenho alunos, mas eles desengajam nas primeiras semanas?","answer":"O kit foi pensado exatamente para esse ponto: organizar os primeiros marcos e os check-ins para o aluno sentir cuidado e progresso rápido no começo, reduzindo o ‘sumiço’ e aumentando a continuidade.","objectionTag":"retencao_engajamento"},{"question":"Isso garante agenda cheia em 8 semanas?","answer":"Não existe garantia de resultado individual. A promessa é um objetivo com base em um processo repetível (sequência de comunicação + marcos + check-ins) que costuma melhorar percepção de valor e engajamento quando aplicado de forma consistente.","objectionTag":"sem_garantia_absoluta"}],"complianceNotes":"A oferta é uma prévia de um kit de ativos digitais gerados por IA (mensagens, roteiro, marcos e check-ins). Não há promessa de consultoria, call, gestão humana ou acompanhamento manual. Evita garantias absolutas e resultados individuais; ‘em até 8 semanas’ é tratado como objetivo/estrutura baseada em processo."},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"landing-page-copy"}}

Diretriz específica para prompt da imagem:
Você cria ativos de campanha para o Marketing Hub.

Regras globais:
1. O anúncio e a landing devem ter a mesma promessa central.
2. O CTA do anúncio deve combinar com a ação principal da landing.
3. O material precisa caber no envelope real do produto:
   - pode entregar ativos digitais gerados por IA
   - não pode prometer consultoria, call, gestão humana ou acompanhamento manual
4. Priorize clareza comercial:
   DOR → RESULTADO → MECANISMO → PROVA → AÇÃO
5. Não transforme mecanismo em promessa principal.
6. Não use jargão técnico desnecessário.
7. O público é geral dentro do nicho, com baixa a moderada maturidade em marketing.
8. Sempre escreva pensando em alta escala e geração automatizada.
9. O anúncio deve ser rápido de entender.
10. A landing deve aprofundar a promessa e reduzir ceticismo.

Contexto do nicho: Profissionais da Área de Educação Física - Personal Trainer

Ângulo da campanha: {"campaignAngle":{"singleMindedPromise":"Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.","primaryPain":"A conversa vira “quanto custa?” e os alunos somem nas primeiras semanas porque não enxergam valor rápido — aí sua agenda depende de indicação e o mês vira loteria.","primaryPromise":"Ter agenda cheia com constância em até 8 semanas, sem desconto, com alunos que chegam mais qualificados (entendendo o valor) e permanecem engajados no início.","cta":"Desbloquear o kit de “Agenda Cheia Sem Desconto (8 semanas)”","tone":"Direto, prático e confiante (sem hype), com foco em clareza e controle do resultado.","mechanismSummary":"Um kit de ativos gerados por IA com uma sequência pronta de comunicação + primeiros marcos de evolução (primeiras semanas) + check-ins automáticos para (1) posicionar seu diferencial antes do preço e (2) fazer o aluno sentir progresso e cuidado rápido.","primaryCTA":"Desbloquear o kit","landingMatchLine":"Desbloqueie o kit “Agenda Cheia Sem Desconto (8 semanas)” para atrair alunos que entendem seu valor e manter engajamento nas primeiras semanas — sem depender de indicação.","funnelStage":"Topo/Meio (aquisição + pré-qualificação)","proofSummary":"Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (reduz desistência, aumenta percepção de valor e melhora renovação), transformadas em sequência pronta e replicável em ativos digitais."},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"campaign-angle"}}
Dor principal: A conversa vira “quanto custa?” e os alunos somem nas primeiras semanas porque não enxergam valor rápido — aí sua agenda depende de indicação e o mês vira loteria.
Promessa principal: Ter agenda cheia com constância em até 8 semanas, sem desconto, com alunos que chegam mais qualificados (entendendo o valor) e permanecem engajados no início.
Mecanismo resumido: Um kit de ativos gerados por IA com uma sequência pronta de comunicação + primeiros marcos de evolução (primeiras semanas) + check-ins automáticos para (1) posicionar seu diferencial antes do preço e (2) fazer o aluno sentir progresso e cuidado rápido.
Prova resumida: Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (reduz desistência, aumenta percepção de valor e melhora renovação), transformadas em sequência pronta e replicável em ativos digitais.

Promessa single-minded: Encha sua agenda em até 8 semanas sem dar desconto — com alunos que já chegam entendendo o valor do seu acompanhamento e seguem engajados nas primeiras semanas.
CTA principal: Desbloquear o kit
Linha de match com landing: Desbloqueie o kit “Agenda Cheia Sem Desconto (8 semanas)” para atrair alunos que entendem seu valor e manter engajamento nas primeiras semanas — sem depender de indicação.

Objetivo:
Gerar briefing visual testável por variável do experimento (não apenas imagem bonita).

Regras obrigatórias:
1. Gerar exatamente 3 briefings, um por variação do anúncio: dor, resultado e prova.
2. Em cada briefing, preencher visualAngle com: dor, resultado ou prova.
3. Em cada briefing, preencher mustMatchAdVariant com: dor, resultado ou prova.
4. Em cada briefing, definir assetType com um valor: estatico.
5. Em cada briefing, definir imageTextMaxWords (inteiro de 3 a 12) para limitar texto sobreposto.
6. Garantir coerência entre criativo, copy da variação e promessa da landing pós-clique.
7. Preservar hierarchy visual, safe margins e notas de compliance por peça.
8. Evitar claims absolutos e qualquer linguagem de consultoria.
9. Definir nicheCue: como o nicho será reconhecido visualmente em até 1 segundo.
10. Definir focalVisual: um único elemento principal que domina a peça.
11. Definir humanPresence com: obrigatoria, opcional ou proibida.
12. Se houver figura humana, ela deve representar o nicho em contexto real e não como rosto genérico decorativo.
13. O CTA visual não pode ser o elemento dominante da peça.


Instruções extras do usuário:
Quantidade sugerida: 4

Responda exclusivamente em JSON válido e siga estritamente o schema da seção.
```

**Resultado**
```text
{"output_text":null,"id":"resp_0ae28eb3657209810069d3c3e695e48197bcdfbba0069eebf5","output":[{"tool_call":null,"tool_calls":null,"id":"msg_0ae28eb3657209810069d3c3e7519081979d7d4f9f2f409ae3","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"adImageBriefing\":{\"briefings\":[{\"assetType\":\"estatico\",\"hierarchy\":\"1) Headline curta (DOR) no topo; 2) Visual principal (situação “preço cedo + sumiço”) ocupando 60–70%; 3) Linha de promessa (8 semanas sem desconto) abaixo; 4) Selo discreto “Kit IA • 100% digital” no rodapé; 5) CTA pequeno não-dominante: “Desbloquear o kit”.\",\"mustMatchAdVariant\":\"dor\",\"visualAngle\":\"dor\",\"complianceNotes\":\"Evitar garantias (ex.: “agenda cheia garantida”). Usar linguagem de objetivo/estrutura (“em até 8 semanas” como meta/processo). Não mencionar consultoria/call/mentoria. Não usar números de resultados (R$, alunos) ou depoimentos inventados. Não insinuar atendimento humano. Inserir microcopy “Entrega digital”/“Kit gerado por IA” sem prometer suporte.\",\"visualBriefing\":\"Conceito: ‘A conversa trava no preço e o aluno some no começo’.\\nCena/estilo: Foto realista (ou ilustração realista) de personal trainer em ambiente de academia/estúdio, olhando para o celular com expressão de frustração moderada. Na tela (sem marcas de apps), um balão de chat com ‘Quanto custa?’ e, abaixo, uma sequência de mensagens esmaecidas/sem resposta (‘visualizado’/silêncio) para representar sumiço.\\nComposição: Trainer à direita (meio corpo), celular em primeiro plano; fundo com equipamentos (halteres, anilhas) desfocado.\\nOverlay de texto (curto): no topo ‘Cansou do “quanto custa?”’ e abaixo ‘Sem desconto em 8 semanas’ (ou variação equivalente dentro do limite de palavras).\\nElementos de apoio: pequeno ícone de checklist/roteiro (bem discreto) próximo ao selo ‘Kit IA’.\",\"safeMargins\":\"Manter texto e selos dentro de 10% das bordas (safe area). Evitar colocar texto no extremo inferior (risco de UI do feed). Garantir contraste AA (texto branco sobre faixa escura ou texto escuro sobre faixa clara).\",\"formatByPlacement\":\"1:1 (1080x1080) prioritário para feed. Versão 4:5 (1080x1350) opcional, mantendo o trainer e o chat sempre no centro vertical e texto no terço superior.\",\"messageMatchNotes\":\"A peça reforça a dor do anúncio (‘quanto custa?’ + sumiço no começo) e conecta com a mesma promessa da landing: ‘em até 8 semanas sem dar desconto’, levando ao mesmo CTA ‘Desbloquear o kit’ (prévia gerada por IA).\",\"imageTextMaxWords\":8},{\"assetType\":\"estatico\",\"hierarchy\":\"1) Headline de RESULTADO no topo; 2) Visual principal (agenda/agenda lotada) 60–70%; 3) Sub-linha com ‘sem desconto’ + ‘8 semanas’ abaixo; 4) Selo discreto ‘Kit IA • mensagens + check-ins’; 5) CTA pequeno no rodapé: ‘Desbloquear o kit’.\",\"mustMatchAdVariant\":\"resultado\",\"visualAngle\":\"resultado\",\"complianceNotes\":\"Não usar ‘garantido’, ‘100%’, ‘infalível’. Não prometer resultado individual. Não sugerir consultoria/call. Evitar ‘antes e depois’ corporal. Evitar termos técnicos. Mostrar ‘kit’ como ativo digital (templates/roteiros) sem parecer serviço prestado.\",\"visualBriefing\":\"Conceito: ‘Mais alunos certos. Menos briga de preço.’\\nCena/estilo: Personal trainer em ambiente real (academia/estúdio) com um tablet ou notebook aberto mostrando uma agenda/calendário visual (blocos preenchidos) e uma lista curta de check-ins (ícones de check). Expressão confiante e prática (não exagerada).\\nFocal visual: Calendário/agenda preenchida (dominante) em primeiro plano.\\nNiche cue (1s): roupa de personal (camiseta dry-fit), prancheta/halteres ao fundo, ambiente de treino.\\nOverlay de texto: ‘Agenda cheia sem desconto’ (ou ‘Mais alunos certos’) + ‘em até 8 semanas’ (manter dentro do limite de palavras).\\nElementos de apoio: selo pequeno ‘Kit IA 100% digital’ no canto inferior.\",\"safeMargins\":\"Texto no terço superior; selo e CTA no terço inferior, ambos dentro de 10% de margem. Evitar texto sobre a área do calendário (para não perder legibilidade).\",\"formatByPlacement\":\"1:1 (1080x1080) para feed. Garantir que o calendário permaneça legível como ‘agenda cheia’ mesmo em miniatura (usar blocos grandes, sem texto minúsculo).\",\"messageMatchNotes\":\"A imagem prioriza o RESULTADO (agenda cheia com constância) sem transformar o mecanismo em promessa principal; apenas ancora com selo ‘Kit IA’. Mantém a promessa central e o CTA alinhados com a landing (‘Desbloquear o kit’/prévia IA).\",\"imageTextMaxWords\":7},{\"assetType\":\"estatico\",\"hierarchy\":\"1) Headline de PROVA (primeiros 30 dias) no topo; 2) Visual principal (timeline/checkpoints das primeiras semanas) 60–70%; 3) Linha de promessa ‘sem desconto • 8 semanas’ abaixo; 4) Selo discreto ‘Baseado em boas práticas de onboarding’; 5) CTA pequeno: ‘Desbloquear o kit’.\",\"mustMatchAdVariant\":\"prova\",\"visualAngle\":\"prova\",\"complianceNotes\":\"‘Boas práticas’/‘tende a’/‘costuma’ em vez de afirmar causalidade garantida. Sem citar estudo específico de forma enganosa; pode dizer ‘baseado em boas práticas dos primeiros 30 dias’. Sem depoimentos falsos. Sem consultoria/call. Evitar ‘método científico’ ou autoridade médica.\",\"visualBriefing\":\"Conceito: ‘O começo decide a permanência’.\\nCena/estilo: Foto/ilustração realista de um personal trainer em contexto real apontando para um quadro/folha/clipboard com uma linha do tempo simples: Semana 1, Semana 2, Semana 3, Semana 4, com 2–3 checkpoints (ex.: “marco”, “check-in”, “pequena vitória”) representados por ícones (check/estrela) — sem texto pequeno.\\nFocal visual: Timeline de 4 semanas com checkpoints grandes e claros.\\nNiche cue (1s): ambiente de academia + trainer com prancheta/quadro de treino.\\nOverlay de texto: ‘Primeiros 30 dias importam’ + ‘sem desconto’ (ou ‘8 semanas’) dentro do limite.\\nElementos de apoio: selo pequeno ‘Onboarding • retenção’ (sem números) e ‘Kit IA • 100% digital’.\",\"safeMargins\":\"Manter a timeline centralizada e grande. Headline no topo com 10% de margem. Rodapé com selo/CTA pequeno, sem encostar na borda inferior.\",\"formatByPlacement\":\"1:1 (1080x1080) para feed. Garantir leitura em miniatura: usar poucos elementos e ícones grandes; sem parágrafos na timeline.\",\"messageMatchNotes\":\"A prova visual conecta com a copy ‘primeiros 30 dias’ e sustenta a promessa da landing (agenda cheia em até 8 semanas sem desconto) mostrando o ‘porquê’ sem prometer garantia. CTA visual permanece secundário e igual à ação principal da landing: desbloquear a prévia do kit IA.\",\"imageTextMaxWords\":6}]},\"experimentMetadata\":{\"primary_variable\":\"Dor vs Resultado\",\"stage\":\"AD\",\"asset_role\":\"ad-image-briefing\",\"variant_id\":\"variant-10\",\"control_or_treatment\":\"treatment\"}}"}]}],"usage":{"input_tokens":6282,"output_tokens":1693,"prompt_tokens":null,"completion_tokens":null,"total_tokens":7975},"error":null,"status":"completed"}
```

### 4. Texto da Landing
- Data: 01/04, 13:50
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Nome do experimento: 
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Persona: teste
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))

Tarefa alvo: landing-page-copy

Ângulo da campanha:
{
  "primaryPromise": "Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e permanecendo engajados nas primeiras semanas.",
  "primaryPain": "A captação vira ‘loteria de indicação’ e, quando chega gente, a conversa cai rápido em ‘quanto custa?’, porque o aluno não entende o diferencial e não sente valor/progresso logo no começo — daí desengaja e some.",
  "mechanismSummary": "Um Kit de Comunicação + Onboarding de 8 semanas gerado por IA: mensagens prontas (DM/WhatsApp/e-mail), roteiro de pré-venda para posicionar valor antes do preço, marcos de progresso das primeiras semanas e check-ins automáticos para aumentar percepção de cuidado e reduzir desistência.",
  "proofUsed": "Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (referência: Glofox) — focando em dar clareza e vitórias rápidas para aumentar percepção de valor e engajamento.",
  "cta": "Desbloquear o Kit (receber a prévia gerada por IA)",
  "funnelStage": "Topo → Meio (descoberta do problema + consideração de uma solução pronta)",
  "tone": "Direto, claro e orientado a resultado; sem jargão, com linguagem de rotina (agenda, alunos, preço, desistência)."
}

Texto do anúncio:
{
  "primaryTextVariants": [
    {
      "label": "dor",
      "primaryText": "A conversa vira ‘quanto custa?’ e o aluno some nas primeiras semanas? Isso acontece quando ele não entende seu diferencial e não sente progresso rápido.\n\nEncha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e ficando engajados no começo.\n\nDesbloqueie uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA: mensagens prontas pro WhatsApp/DM/e-mail, roteiro de pré-venda, marcos de progresso e check-ins.\n\nClique e receba a prévia.",
      "headline": "Agenda cheia em até 8 semanas (sem desconto)",
      "description": "Prévia do Kit gerado por IA: mensagens + roteiro + check-ins.",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    },
    {
      "label": "resultado",
      "primaryText": "Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento (não só perguntando preço) e permanecendo engajados nas primeiras semanas.\n\nVocê recebe uma prévia de um Kit pronto, gerado por IA a partir de um briefing simples: convite claro pra atrair o aluno certo, roteiro de conversa no WhatsApp pra guiar a decisão e um começo que segura o aluno com marcos + check-ins.\n\nDesbloqueie a prévia do Kit.",
      "headline": "Mais alunos certos. Menos briga de preço.",
      "description": "Ciclo de 8 semanas pronto para usar (prévia).",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    },
    {
      "label": "prova",
      "primaryText": "Os primeiros 30 dias decidem se o aluno fica ou some. Quando ele tem clareza do caminho + vitórias rápidas, a percepção de valor sobe e o engajamento aumenta (boas práticas de onboarding e retenção).\n\nPor isso criamos o método “Agenda Cheia Sem Desconto (8 Semanas)”: um Kit de Comunicação + Onboarding gerado por IA com mensagens prontas, roteiro curto de WhatsApp, marcos de progresso e check-ins.\n\nDesbloqueie a prévia do Kit e veja como aplicar no seu acompanhamento.",
      "headline": "Baseado em boas práticas dos 30 primeiros dias",
      "description": "Prévia do Kit pronto (IA) para captar e reter melhor.",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    }
  ]
}

Briefing da imagem:
{
  "adImageBriefing": {
    "assetCount": 1,
    "formatPriority": [
      {
        "placement": "Feed",
        "size": "1080x1350",
        "safeMargins": "Manter 10% de margem de segurança (texto e elementos dentro da área central)."
      },
      {
        "placement": "Stories/Reels",
        "size": "1080x1920",
        "safeMargins": "Evitar topo (UI) e base (CTA). Texto principal no terço central."
      }
    ],
    "objective": "Fazer a promessa ser entendida em 2 segundos e gerar clique para ‘Desbloquear o Kit (receber a prévia gerada por IA)’.",
    "singleMindedPromise": "Agenda cheia em até 8 semanas sem dar desconto.",
    "supportingPoints": [
      "Alunos chegam entendendo seu valor (não só preço)",
      "Primeiras semanas com engajamento e progresso (menos desistência)",
      "Prévia de Kit pronto gerado por IA: mensagens + roteiro + marcos + check-ins"
    ],
    "primaryPainToVisualize": "A conversa vira ‘quanto custa?’ e o aluno some nas primeiras semanas.",
    "creativeConcept": {
      "conceptName": "8 Semanas sob controle",
      "idea": "Um visual de ‘plano de 8 semanas’ (timeline/checklist) que comunica processo simples e repetível. Reforça que é sem desconto e com aluno entendendo valor + começando bem."
    },
    "layout": {
      "structure": "Topo (promessa curta) → Meio (visual de timeline 8 semanas) → Rodapé (o que vem no kit + CTA)",
      "hierarchy": [
        "1) Promessa (grande)",
        "2) ‘Sem desconto’ (selo)",
        "3) Timeline 8 semanas (prova de método / clareza)",
        "4) Itens do kit (rápidos)",
        "5) CTA ‘Desbloquear a prévia’"
      ]
    },
    "onImageCopy": {
      "variantA": {
        "headline": "Agenda cheia em até 8 semanas",
        "badge": "SEM DESCONTO",
        "subhead": "com alunos chegando entendendo seu valor",
        "bullets": [
          "Mensagens prontas (Whats/DM/e-mail)",
          "Roteiro de pré-venda",
          "Marcos + check-ins (início)"
        ],
        "cta": "Desbloquear a prévia"
      }
    },
    "visualDirections": {
      "style": "Limpo, direto e ‘de rotina’ (agenda/semana/check).",
      "imagery": [
        "Timeline horizontal/vertical com 8 marcadores ‘Semana 1… Semana 8’",
        "Ícones simples: chat (mensagens), roteiro (documento), check (check-ins), alvo (aluno certo)"
      ],
      "avoid": [
        "Prometer consultoria, call ou acompanhamento humano",
        "Antes/depois agressivo ou ganhos garantidos",
        "Jargões (funil, lead magnet, etc.)"
      ]
    },
    "designSystem": {
      "colorPalette": {
        "primary": "Azul escuro (#0B1F3B) para confiança",
        "accent": "Verde (#22C55E) para ‘check/progresso’",
        "neutral": "Branco e cinza claro para leitura"
      },
      "typography": {
        "headline": "Sans bold (alta legibilidade)",
        "body": "Sans regular",
        "notes": "Máximo 2 famílias tipográficas"
      },
      "ctaButtonStyle": "Botão/box contrastante (verde ou branco com contorno), texto curto: ‘Desbloquear a prévia’."
    },
    "brandSafetyAndCompliance": {
      "claims": [
        "Usar ‘em até 8 semanas’ como promessa aspiracional; evitar garantias absolutas.",
        "Não afirmar resultados individuais; manter linguagem de ‘com um processo/kit’.",
        "‘Gerado por IA’ como característica do produto, não como milagre." 
      ],
      "finePrintOptional": "*Resultados variam conforme execução e contexto."
    },
    "adToLandingConsistency": {
      "promiseMatch": "Imagem reforça ‘Agenda cheia em até 8 semanas sem desconto’ e o porquê (alunos entendem valor + início engajante).",
      "ctaMatch": "CTA na imagem: ‘Desbloquear a prévia’ (equivalente a ‘Desbloquear o Kit / receber a prévia gerada por IA’)."
    },
    "productionNotes": {
      "export": [
        "PNG (alta) + versão editável",
        "Criar variação para 1080x1920 mantendo o mesmo texto e hierarquia"
      ],
      "readability": "Garantir leitura em tela pequena: headline até ~32 caracteres; bullets curtos."
    }
  }
}

Instruções extras do usuário:
Quantidade sugerida: 1

Responda exclusivamente em JSON válido no formato {"content":"..."}.
```

**Resultado**
```text
{"output_text":null,"id":"resp_02fbe669fbf347160069cd4cbad0dc8194b2fc15f434f211e4","output":[{"tool_call":null,"tool_calls":null,"id":"msg_02fbe669fbf347160069cd4cbb8b808194b6c09707b9ecaeac","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"content\":\"{\\\"hero\\\": {\\\"preheader\\\": \\\"Agenda cheia com constância — sem virar briga de preço\\\", \\\"headline\\\": \\\"Encha sua agenda em até 8 semanas sem dar desconto\\\", \\\"subheadline\\\": \\\"Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA — para atrair alunos que entendem seu valor e manter engajamento forte nas primeiras semanas.\\\", \\\"ctaPrimary\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\", \\\"ctaSecondary\\\": \\\"Ver o que vem na prévia\\\", \\\"microcopy\\\": \\\"Sem call. Sem consultoria. Você recebe ativos digitais prontos (gerados por IA).\\\"}, \\\"problemSection\\\": {\\\"title\\\": \\\"Se todo mês parece uma loteria, não é falta de esforço — é falta de sequência\\\", \\\"bullets\\\": [\\\"A captação depende de indicação e oscila.\\\", \\\"Quando aparece alguém, a conversa cai rápido em ‘quanto custa?’ — antes de você mostrar seu diferencial.\\\", \\\"Nas primeiras semanas o aluno ainda não tem hábito: se não sente progresso e cuidado, desengaja e some.\\\", \\\"Renovação vira tentativa e erro: alguns ficam, outros somem sem você entender o porquê.\\\"], \\\"transition\\\": \\\"A saída é um processo simples e repetível: posicionar valor antes do preço + criar vitórias rápidas logo no começo.\\\"}, \\\"promiseSection\\\": {\\\"title\\\": \\\"A promessa central\\\", \\\"content\\\": \\\"Em até 8 semanas, você passa a ter um ciclo claro para encher a agenda sem desconto — com alunos chegando mais preparados (entendendo seu valor) e permanecendo engajados nas primeiras semanas por sentirem progresso e cuidado desde o início.\\\"}, \\\"mechanismSection\\\": {\\\"title\\\": \\\"Como funciona (sem complicar)\\\", \\\"steps\\\": [{\\\"step\\\": \\\"1\\\", \\\"title\\\": \\\"Você responde um briefing rápido\\\", \\\"text\\\": \\\"Algumas perguntas simples sobre seu acompanhamento, público e como você trabalha.\\\"}, {\\\"step\\\": \\\"2\\\", \\\"title\\\": \\\"A IA gera sua prévia do Kit de 8 semanas\\\", \\\"text\\\": \\\"Você recebe mensagens e roteiros prontos para usar e adaptar (WhatsApp/DM/e-mail), além do plano de começo com marcos e check-ins.\\\"}, {\\\"step\\\": \\\"3\\\", \\\"title\\\": \\\"Você aplica a sequência no seu dia a dia\\\", \\\"text\\\": \\\"Usa o roteiro para conduzir conversas sem cair no preço cedo demais e usa o onboarding para criar progresso rápido nas primeiras semanas.\\\"}], \\\"note\\\": \\\"O Kit é o ‘mapa’ e os textos prontos. A execução depende de você e do seu contexto.\\\"}, \\\"whatsInsideSection\\\": {\\\"title\\\": \\\"O que vem na prévia do Kit (gerada por IA)\\\", \\\"items\\\": [\\\"Mensagens prontas para WhatsApp/DM/e-mail (captação + follow-up)\\\", \\\"Roteiro curto de pré-venda para posicionar valor antes do preço\\\", \\\"Sequência de onboarding das primeiras semanas (8 semanas)\\\", \\\"Marcos de progresso (o que o aluno precisa perceber e quando)\\\", \\\"Check-ins prontos (para aumentar percepção de cuidado e reduzir desistência)\\\"], \\\"formatNote\\\": \\\"Você recebe em formato copiável (texto), pronto para colar e ajustar.\\\"}, \\\"benefitsSection\\\": {\\\"title\\\": \\\"O que isso ajuda a destravar\\\", \\\"columns\\\": [{\\\"title\\\": \\\"Antes\\\", \\\"bullets\\\": [\\\"Conversa vira preço cedo\\\", \\\"Aluno entra e desengaja no início\\\", \\\"Agenda instável\\\", \\\"Renovação imprevisível\\\"]}, {\\\"title\\\": \\\"Com o Kit (aplicando a sequência)\\\", \\\"bullets\\\": [\\\"Mais clareza do seu diferencial antes do preço\\\", \\\"Primeiras semanas com ‘vitórias rápidas’\\\", \\\"Mais constância no fim do mês\\\", \\\"Mais alunos certos e menos desistência\\\"]}]}, \\\"proofSection\\\": {\\\"title\\\": \\\"Por que focar nas primeiras semanas funciona\\\", \\\"content\\\": \\\"Os primeiros 30 dias costumam ser decisivos para o aluno criar hábito e perceber valor. Boas práticas de onboarding e retenção indicam que clareza do caminho + vitórias rápidas aumentam engajamento e permanência. Este Kit foi estruturado com esse foco: deixar o valor claro e acelerar a percepção de progresso desde o começo.\\\", \\\"source\\\": \\\"Referência: boas práticas de onboarding e retenção (ex.: Glofox).\\\"}, \\\"objectionsSection\\\": {\\\"title\\\": \\\"Perguntas comuns (sem enrolação)\\\", \\\"faqs\\\": [{\\\"q\\\": \\\"Isso é consultoria ou acompanhamento?\\\", \\\"a\\\": \\\"Não. Você recebe ativos digitais gerados por IA (mensagens, roteiros, marcos e check-ins). Não inclui call, gestão humana ou acompanhamento manual.\\\"}, {\\\"q\\\": \\\"Funciona para qualquer tipo de acompanhamento?\\\", \\\"a\\\": \\\"Funciona melhor para serviços recorrentes com alunos/cliente que precisam de orientação e rotina (ex.: treinos, aulas, mentoria prática, acompanhamento profissional). A prévia já ajuda você a ver o encaixe.\\\"}, {\\\"q\\\": \\\"Vou precisar ‘entender marketing’?\\\", \\\"a\\\": \\\"Não. O material é escrito em linguagem de rotina (agenda, alunos, preço, começo). Você só aplica a sequência e adapta termos ao seu jeito.\\\"}, {\\\"q\\\": \\\"Em quanto tempo vou ver resultado?\\\", \\\"a\\\": \\\"A proposta é um ciclo de até 8 semanas. Resultados variam conforme sua execução, oferta e contexto.\\\"}, {\\\"q\\\": \\\"O que significa ‘gerado por IA’?\\\", \\\"a\\\": \\\"Significa que os textos e a estrutura do Kit são gerados automaticamente a partir do seu briefing, para você ganhar velocidade e consistência.\\\"}]}, \\\"ctaSection\\\": {\\\"title\\\": \\\"Desbloqueie a prévia do Kit agora\\\", \\\"subtitle\\\": \\\"Veja como fica um ciclo de 8 semanas pronto para captar melhor e segurar o aluno no começo — sem depender de indicação e sem desconto.\\\", \\\"ctaPrimary\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\", \\\"microcopy\\\": \\\"*Resultados variam conforme execução e contexto.\\\"}, \\\"footer\\\": {\\\"compliance\\\": [\\\"Sem garantias de resultado.\\\", \\\"Sem consultoria/call/gestão humana.\\\", \\\"Entrega de ativos digitais gerados por IA.\\\"]}}\"}"}]}],"usage":{"input_tokens":2556,"output_tokens":1362,"prompt_tokens":null,"completion_tokens":null,"total_tokens":3918},"error":null,"status":"completed"}
```

### 5. Texto da Landing
- Data: 01/04, 17:10
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Nome do experimento: 
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Persona: teste
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))

Tarefa alvo: landing-page-copy

Ângulo da campanha:
{
  "primaryPromise": "Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e permanecendo engajados nas primeiras semanas.",
  "primaryPain": "A captação vira ‘loteria de indicação’ e, quando chega gente, a conversa cai rápido em ‘quanto custa?’, porque o aluno não entende o diferencial e não sente valor/progresso logo no começo — daí desengaja e some.",
  "mechanismSummary": "Um Kit de Comunicação + Onboarding de 8 semanas gerado por IA: mensagens prontas (DM/WhatsApp/e-mail), roteiro de pré-venda para posicionar valor antes do preço, marcos de progresso das primeiras semanas e check-ins automáticos para aumentar percepção de cuidado e reduzir desistência.",
  "proofUsed": "Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (referência: Glofox) — focando em dar clareza e vitórias rápidas para aumentar percepção de valor e engajamento.",
  "cta": "Desbloquear o Kit (receber a prévia gerada por IA)",
  "funnelStage": "Topo → Meio (descoberta do problema + consideração de uma solução pronta)",
  "tone": "Direto, claro e orientado a resultado; sem jargão, com linguagem de rotina (agenda, alunos, preço, desistência)."
}

Texto do anúncio:
{
  "primaryTextVariants": [
    {
      "label": "dor",
      "primaryText": "A conversa vira ‘quanto custa?’ e o aluno some nas primeiras semanas? Isso acontece quando ele não entende seu diferencial e não sente progresso rápido.\n\nEncha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e ficando engajados no começo.\n\nDesbloqueie uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA: mensagens prontas pro WhatsApp/DM/e-mail, roteiro de pré-venda, marcos de progresso e check-ins.\n\nClique e receba a prévia.",
      "headline": "Agenda cheia em até 8 semanas (sem desconto)",
      "description": "Prévia do Kit gerado por IA: mensagens + roteiro + check-ins.",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    },
    {
      "label": "resultado",
      "primaryText": "Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento (não só perguntando preço) e permanecendo engajados nas primeiras semanas.\n\nVocê recebe uma prévia de um Kit pronto, gerado por IA a partir de um briefing simples: convite claro pra atrair o aluno certo, roteiro de conversa no WhatsApp pra guiar a decisão e um começo que segura o aluno com marcos + check-ins.\n\nDesbloqueie a prévia do Kit.",
      "headline": "Mais alunos certos. Menos briga de preço.",
      "description": "Ciclo de 8 semanas pronto para usar (prévia).",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    },
    {
      "label": "prova",
      "primaryText": "Os primeiros 30 dias decidem se o aluno fica ou some. Quando ele tem clareza do caminho + vitórias rápidas, a percepção de valor sobe e o engajamento aumenta (boas práticas de onboarding e retenção).\n\nPor isso criamos o método “Agenda Cheia Sem Desconto (8 Semanas)”: um Kit de Comunicação + Onboarding gerado por IA com mensagens prontas, roteiro curto de WhatsApp, marcos de progresso e check-ins.\n\nDesbloqueie a prévia do Kit e veja como aplicar no seu acompanhamento.",
      "headline": "Baseado em boas práticas dos 30 primeiros dias",
      "description": "Prévia do Kit pronto (IA) para captar e reter melhor.",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    }
  ]
}

Briefing da imagem:
{
  "adImageBriefing": {
    "assetCount": 4,
    "formatPriority": [
      {
        "placement": "Feed",
        "size": "1080x1350",
        "safeMargins": "Manter 10% de margem de segurança (texto e elementos dentro da área central)."
      },
      {
        "placement": "Stories/Reels",
        "size": "1080x1920",
        "safeMargins": "Evitar topo (UI) e base (CTA). Texto principal no terço central."
      }
    ],
    "objective": "Fazer a promessa ser entendida em 2 segundos e gerar clique para ‘Desbloquear o Kit (receber a prévia gerada por IA)’.",
    "singleMindedPromise": "Encha sua agenda em até 8 semanas sem dar desconto.",
    "adToLandingConsistency": {
      "promiseMatch": "Todas as imagens reforçam a mesma promessa do hero da landing: ‘Encha sua agenda em até 8 semanas sem dar desconto’ + motivo (alunos entendem valor e ficam engajados no começo).",
      "ctaMatch": "CTA em todas as variações: ‘Desbloquear a prévia’ (equivalente a ‘Desbloquear o Kit / receber a prévia gerada por IA’).",
      "complianceMatch": "Incluir microcopy quando couber: ‘Sem call. Sem consultoria. Ativos digitais gerados por IA.’"
    },
    "globalDesignSystem": {
      "style": "Limpo, direto e ‘de rotina’ (agenda/semana/check).",
      "colorPalette": {
        "primary": "#0B1F3B",
        "accent": "#22C55E",
        "neutral": "#FFFFFF / cinza claro"
      },
      "typography": {
        "headline": "Sans bold (alta legibilidade)",
        "body": "Sans regular",
        "rules": "Máx. 2 famílias tipográficas. Headline curta (até ~32 caracteres no feed)."
      },
      "iconography": "Ícones simples e sólidos: calendário/agenda, chat, check, documento/roteiro, alvo.",
      "ctaButtonStyle": "Box/botão contrastante (verde ou branco com contorno). Texto: ‘Desbloquear a prévia’.",
      "avoid": [
        "Prometer consultoria, call ou acompanhamento humano",
        "Antes/depois agressivo",
        "Ganhos garantidos",
        "Jargões (funil, lead magnet, etc.)",
        "Texto longo demais (ilegível no mobile)"
      ]
    },
    "brandSafetyAndCompliance": {
      "claims": [
        "Usar ‘em até 8 semanas’ como promessa aspiracional; evitar garantias absolutas.",
        "Não afirmar resultados individuais; manter linguagem de ‘com um processo/kit’.",
        "‘Gerado por IA’ como característica do produto, não como milagre."
      ],
      "finePrintOptional": "*Resultados variam conforme execução e contexto."
    },
    "variants": [
      {
        "id": "V1",
        "name": "8 Semanas sob controle (Timeline)",
        "concept": {
          "idea": "Timeline/checklist de 8 semanas para comunicar processo simples e repetível (sem desconto).",
          "primaryPainToVisualize": "‘Quanto custa?’ cedo + aluno some no começo.",
          "visualMetaphor": "Plano semanal com checks (controle e constância)."
        },
        "layout": {
          "structure": "Topo (promessa) → Meio (timeline 8 semanas) → Rodapé (itens do kit + CTA)",
          "hierarchy": [
            "1) Promessa grande",
            "2) Selo ‘SEM DESCONTO’",
            "3) Timeline ‘Semana 1… Semana 8’",
            "4) 3 bullets do que vem",
            "5) CTA"
          ]
        },
        "onImageCopy": {
          "headline": "Agenda cheia em até 8 semanas",
          "badge": "SEM DESCONTO",
          "subhead": "com alunos chegando entendendo seu valor",
          "bullets": [
            "Mensagens prontas (Whats/DM/e-mail)",
            "Roteiro de pré-venda",
            "Marcos + check-ins (início)"
          ],
          "cta": "Desbloquear a prévia",
          "microcopyOptional": "Sem call. Sem consultoria."
        },
        "visualDirections": {
          "imagery": [
            "Timeline vertical (feed) com 8 marcadores e checks verdes",
            "Ícones pequenos ao lado dos bullets (chat/documento/check)"
          ],
          "background": "Branco ou cinza muito claro; blocos em azul escuro; destaque verde para checks/CTA."
        },
        "productionNotes": {
          "readability": "Manter o subhead em 1 linha (ou 2 curtas). Bullets com 4–6 palavras.",
          "export": [
            "PNG 1080x1350",
            "PNG 1080x1920 (timeline centralizada no terço do meio)"
          ]
        }
      },
      {
        "id": "V2",
        "name": "Quebra de objeção (Sem briga de preço)",
        "concept": {
          "idea": "Contraste direto ‘Preço vs Valor’ para mostrar que o aluno chega mais preparado (sem parecer técnico).",
          "primaryPainToVisualize": "Conversa cai em ‘quanto custa?’ antes do diferencial.",
          "visualMetaphor": "Dois balões de chat: ‘Quanto custa?’ (apagado) vs ‘Entendi como funciona’ (ativo)."
        },
        "layout": {
          "structure": "Topo (promessa) → Meio (visual de chats comparativos) → Rodapé (o que vem + CTA)",
          "hierarchy": [
            "1) Promessa grande",
            "2) Prova visual (chats)",
            "3) ‘SEM DESCONTO’ como selo menor",
            "4) 2–3 bullets",
            "5) CTA"
          ]
        },
        "onImageCopy": {
          "headline": "Mais alunos certos. Menos briga de preço.",
          "subhead": "em até 8 semanas — sem desconto",
          "bullets": [
            "Roteiro pronto de WhatsApp",
            "Mensagens + follow-up",
            "Check-ins do começo"
          ],
          "cta": "Desbloquear a prévia",
          "microcopyOptional": "Ativos digitais gerados por IA"
        },
        "visualDirections": {
          "imagery": [
            "Balão de chat cinza: ‘Quanto custa?’ com ícone de ‘pause’",
            "Balão de chat azul: ‘Entendi o valor e o plano’ com ícone de check",
            "Selo ‘SEM DESCONTO’ em verde"
          ],
          "background": "Gradiente suave do branco para cinza claro; foco nos chats no centro."
        },
        "productionNotes": {
          "readability": "Headline até 38 caracteres. Evitar excesso de texto dentro dos balões.",
          "export": [
            "PNG 1080x1350",
            "PNG 1080x1920 (chats no centro; CTA acima da área do botão nativo)"
          ]
        }
      },
      {
        "id": "V3",
        "name": "Onboarding que segura (Primeiras semanas)",
        "concept": {
          "idea": "Foco em retenção: aluno não some porque sente cuidado + progresso rápido nas primeiras semanas.",
          "primaryPainToVisualize": "Aluno empolga na semana 1 e some na semana 2/3.",
          "visualMetaphor": "Calendário ‘Semana 1–4’ com checks e marcos; seta indicando continuidade até 8 semanas."
        },
        "layout": {
          "structure": "Topo (promessa) → Meio (calendário + marcos) → Rodapé (kit + CTA)",
          "hierarchy": [
            "1) Promessa grande",
            "2) Subhead ‘primeiras semanas’",
            "3) Calendário/marcos",
            "4) 3 itens do kit",
            "5) CTA"
          ]
        },
        "onImageCopy": {
          "headline": "Agenda cheia em até 8 semanas",
          "subhead": "com começo que mantém o aluno engajado",
          "badge": "SEM DESCONTO",
          "bullets": [
            "Marcos de progresso",
            "Check-ins prontos",
            "Mensagens de onboarding"
          ],
          "cta": "Desbloquear a prévia",
          "microcopyOptional": "Sem call. Sem consultoria."
        },
        "visualDirections": {
          "imagery": [
            "Mini calendário (4 semanas) com checks verdes",
            "2–3 ‘marcos’ em tags curtas (ex.: ‘Primeira vitória’, ‘Rotina firme’, ‘Feedback rápido’)"
          ],
          "background": "Branco; blocos em azul escuro; tags em cinza claro; checks em verde."
        },
        "productionNotes": {
          "readability": "Tags de marco com no máximo 2 palavras. Não prometer resultado físico/financeiro.",
          "export": [
            "PNG 1080x1350",
            "PNG 1080x1920"
          ]
        }
      },
      {
        "id": "V4",
        "name": "O que vem na prévia (Kit em 1 tela)",
        "concept": {
          "idea": "Reduz ceticismo mostrando ‘produto’ concreto: lista curta do que a pessoa recebe na prévia.",
          "primaryPainToVisualize": "Dúvida: ‘ok, mas o que eu vou receber?’",
          "visualMetaphor": "Card de checklist ‘Prévia do Kit (IA)’ com 4 itens e ícones."
        },
        "layout": {
          "structure": "Topo (promessa) → Meio (card checklist do kit) → Rodapé (CTA + microcopy)",
          "hierarchy": [
            "1) Promessa grande",
            "2) Card ‘Prévia do Kit’",
            "3) 4 itens (com ícones)",
            "4) CTA",
            "5) Microcopy ‘sem call/consultoria’"
          ]
        },
        "onImageCopy": {
          "headline": "Encha sua agenda em até 8 semanas",
          "badge": "SEM DESCONTO",
          "subhead": "Desbloqueie a prévia do Kit (IA)",
          "bullets": [
            "Mensagens (Whats/DM/e-mail)",
            "Roteiro de pré-venda",
            "Marcos de progresso",
            "Check-ins do início"
          ],
          "cta": "Desbloquear a prévia",
          "microcopyOptional": "Ativos digitais gerados por IA"
        },
        "visualDirections": {
          "imagery": [
            "Card central com borda sutil",
            "Ícones por item (chat/documento/flag/check)"
          ],
          "background": "Azul escuro bem leve (ou branco) com card branco para máximo contraste."
        },
        "productionNotes": {
          "readability": "Manter 4 bullets (não mais). Se necessário, reduzir para 3 no Stories.",
          "export": [
            "PNG 1080x1350",
            "PNG 1080x1920"
          ]
        }
      }
    ]
  }
}

Textos da landing:
{"hero": {"preheader": "Agenda cheia com constância — sem virar briga de preço", "headline": "Encha sua agenda em até 8 semanas sem dar desconto", "subheadline": "Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA — para atrair alunos que entendem seu valor e manter engajamento forte nas primeiras semanas.", "ctaPrimary": "Desbloquear o Kit (receber a prévia gerada por IA)", "ctaSecondary": "Ver o que vem na prévia", "microcopy": "Sem call. Sem consultoria. Você recebe ativos digitais prontos (gerados por IA)."}, "problemSection": {"title": "Se todo mês parece uma loteria, não é falta de esforço — é falta de sequência", "bullets": ["A captação depende de indicação e oscila.", "Quando aparece alguém, a conversa cai rápido em ‘quanto custa?’ — antes de você mostrar seu diferencial.", "Nas primeiras semanas o aluno ainda não tem hábito: se não sente progresso e cuidado, desengaja e some.", "Renovação vira tentativa e erro: alguns ficam, outros somem sem você entender o porquê."], "transition": "A saída é um processo simples e repetível: posicionar valor antes do preço + criar vitórias rápidas logo no começo."}, "promiseSection": {"title": "A promessa central", "content": "Em até 8 semanas, você passa a ter um ciclo claro para encher a agenda sem desconto — com alunos chegando mais preparados (entendendo seu valor) e permanecendo engajados nas primeiras semanas por sentirem progresso e cuidado desde o início."}, "mechanismSection": {"title": "Como funciona (sem complicar)", "steps": [{"step": "1", "title": "Você responde um briefing rápido", "text": "Algumas perguntas simples sobre seu acompanhamento, público e como você trabalha."}, {"step": "2", "title": "A IA gera sua prévia do Kit de 8 semanas", "text": "Você recebe mensagens e roteiros prontos para usar e adaptar (WhatsApp/DM/e-mail), além do plano de começo com marcos e check-ins."}, {"step": "3", "title": "Você aplica a sequência no seu dia a dia", "text": "Usa o roteiro para conduzir conversas sem cair no preço cedo demais e usa o onboarding para criar progresso rápido nas primeiras semanas."}], "note": "O Kit é o ‘mapa’ e os textos prontos. A execução depende de você e do seu contexto."}, "whatsInsideSection": {"title": "O que vem na prévia do Kit (gerada por IA)", "items": ["Mensagens prontas para WhatsApp/DM/e-mail (captação + follow-up)", "Roteiro curto de pré-venda para posicionar valor antes do preço", "Sequência de onboarding das primeiras semanas (8 semanas)", "Marcos de progresso (o que o aluno precisa perceber e quando)", "Check-ins prontos (para aumentar percepção de cuidado e reduzir desistência)"], "formatNote": "Você recebe em formato copiável (texto), pronto para colar e ajustar."}, "benefitsSection": {"title": "O que isso ajuda a destravar", "columns": [{"title": "Antes", "bullets": ["Conversa vira preço cedo", "Aluno entra e desengaja no início", "Agenda instável", "Renovação imprevisível"]}, {"title": "Com o Kit (aplicando a sequência)", "bullets": ["Mais clareza do seu diferencial antes do preço", "Primeiras semanas com ‘vitórias rápidas’", "Mais constância no fim do mês", "Mais alunos certos e menos desistência"]}]}, "proofSection": {"title": "Por que focar nas primeiras semanas funciona", "content": "Os primeiros 30 dias costumam ser decisivos para o aluno criar hábito e perceber valor. Boas práticas de onboarding e retenção indicam que clareza do caminho + vitórias rápidas aumentam engajamento e permanência. Este Kit foi estruturado com esse foco: deixar o valor claro e acelerar a percepção de progresso desde o começo.", "source": "Referência: boas práticas de onboarding e retenção (ex.: Glofox)."}, "objectionsSection": {"title": "Perguntas comuns (sem enrolação)", "faqs": [{"q": "Isso é consultoria ou acompanhamento?", "a": "Não. Você recebe ativos digitais gerados por IA (mensagens, roteiros, marcos e check-ins). Não inclui call, gestão humana ou acompanhamento manual."}, {"q": "Funciona para qualquer tipo de acompanhamento?", "a": "Funciona melhor para serviços recorrentes com alunos/cliente que precisam de orientação e rotina (ex.: treinos, aulas, mentoria prática, acompanhamento profissional). A prévia já ajuda você a ver o encaixe."}, {"q": "Vou precisar ‘entender marketing’?", "a": "Não. O material é escrito em linguagem de rotina (agenda, alunos, preço, começo). Você só aplica a sequência e adapta termos ao seu jeito."}, {"q": "Em quanto tempo vou ver resultado?", "a": "A proposta é um ciclo de até 8 semanas. Resultados variam conforme sua execução, oferta e contexto."}, {"q": "O que significa ‘gerado por IA’?", "a": "Significa que os textos e a estrutura do Kit são gerados automaticamente a partir do seu briefing, para você ganhar velocidade e consistência."}]}, "ctaSection": {"title": "Desbloqueie a prévia do Kit agora", "subtitle": "Veja como fica um ciclo de 8 semanas pronto para captar melhor e segurar o aluno no começo — sem depender de indicação e sem desconto.", "ctaPrimary": "Desbloquear o Kit (receber a prévia gerada por IA)", "microcopy": "*Resultados variam conforme execução e contexto."}, "footer": {"compliance": ["Sem garantias de resultado.", "Sem consultoria/call/gestão humana.", "Entrega de ativos digitais gerados por IA."]}}

Instruções extras do usuário:
Quantidade sugerida: 4

Responda exclusivamente em JSON válido no formato {"content":"..."}.
```

**Resultado**
```text
{"output_text":null,"id":"resp_026dacd8596740c20069cd7b9cdd9c8197be125a2d894c19da","output":[{"tool_call":null,"tool_calls":null,"id":"msg_026dacd8596740c20069cd7b9d879c8197a6c61bd0f947047b","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"content\":\"{\\n  \\\"landing-page-copy\\\": {\\n    \\\"version\\\": \\\"Experiment #10 — Agenda Cheia Sem Desconto (8 Semanas)\\\",\\n    \\\"primaryPromise\\\": \\\"Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e permanecendo engajados nas primeiras semanas.\\\",\\n    \\\"ctaPrimary\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\",\\n    \\\"ctaSecondary\\\": \\\"Ver o que vem na prévia\\\",\\n    \\\"sections\\\": [\\n      {\\n        \\\"id\\\": \\\"hero\\\",\\n        \\\"type\\\": \\\"hero\\\",\\n        \\\"preheader\\\": \\\"Agenda cheia com constância — sem virar briga de preço\\\",\\n        \\\"headline\\\": \\\"Encha sua agenda em até 8 semanas sem dar desconto\\\",\\n        \\\"subheadline\\\": \\\"Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA — para atrair alunos que entendem seu valor e manter engajamento forte nas primeiras semanas.\\\",\\n        \\\"ctaPrimary\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\",\\n        \\\"ctaSecondary\\\": \\\"Ver o que vem na prévia\\\",\\n        \\\"microcopy\\\": \\\"Sem call. Sem consultoria. Você recebe ativos digitais prontos (gerados por IA).\\\",\\n        \\\"trustBullets\\\": [\\n          \\\"Mensagens prontas (WhatsApp/DM/e-mail)\\\",\\n          \\\"Roteiro curto para não cair no preço cedo\\\", \\n          \\\"Onboarding com marcos + check-ins (8 semanas)\\\"\\n        ]\\n      },\\n      {\\n        \\\"id\\\": \\\"problem\\\",\\n        \\\"type\\\": \\\"problemSection\\\",\\n        \\\"title\\\": \\\"Se todo mês parece uma loteria, não é falta de esforço — é falta de sequência\\\",\\n        \\\"bullets\\\": [\\n          \\\"A captação depende de indicação e oscila.\\\",\\n          \\\"Quando aparece alguém, a conversa cai rápido em ‘quanto custa?’ — antes de você mostrar seu diferencial.\\\",\\n          \\\"Nas primeiras semanas o aluno ainda não tem hábito: se não sente progresso e cuidado, desengaja e some.\\\",\\n          \\\"Renovação vira tentativa e erro: alguns ficam, outros somem sem você entender o porquê.\\\"\\n        ],\\n        \\\"transition\\\": \\\"A saída é um processo simples e repetível: posicionar valor antes do preço + criar vitórias rápidas logo no começo.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"promise\\\",\\n        \\\"type\\\": \\\"promiseSection\\\",\\n        \\\"title\\\": \\\"A promessa central\\\",\\n        \\\"content\\\": \\\"Em até 8 semanas, você passa a ter um ciclo claro para encher a agenda sem desconto — com alunos chegando mais preparados (entendendo seu valor) e permanecendo engajados nas primeiras semanas por sentirem progresso e cuidado desde o início.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"mechanism\\\",\\n        \\\"type\\\": \\\"mechanismSection\\\",\\n        \\\"title\\\": \\\"Como funciona (sem complicar)\\\",\\n        \\\"steps\\\": [\\n          {\\n            \\\"step\\\": \\\"1\\\",\\n            \\\"title\\\": \\\"Você responde um briefing rápido\\\",\\n            \\\"text\\\": \\\"Algumas perguntas simples sobre seu acompanhamento, público e como você trabalha.\\\"\\n          },\\n          {\\n            \\\"step\\\": \\\"2\\\",\\n            \\\"title\\\": \\\"A IA gera sua prévia do Kit de 8 semanas\\\",\\n            \\\"text\\\": \\\"Você recebe mensagens e roteiros prontos para usar e adaptar (WhatsApp/DM/e-mail), além do plano de começo com marcos e check-ins.\\\"\\n          },\\n          {\\n            \\\"step\\\": \\\"3\\\",\\n            \\\"title\\\": \\\"Você aplica a sequência no seu dia a dia\\\",\\n            \\\"text\\\": \\\"Usa o roteiro para conduzir conversas sem cair no preço cedo demais e usa o onboarding para criar progresso rápido nas primeiras semanas.\\\"\\n          }\\n        ],\\n        \\\"note\\\": \\\"O Kit é o ‘mapa’ e os textos prontos. A execução depende de você e do seu contexto.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"whats-inside\\\",\\n        \\\"type\\\": \\\"whatsInsideSection\\\",\\n        \\\"title\\\": \\\"O que vem na prévia do Kit (gerada por IA)\\\",\\n        \\\"items\\\": [\\n          \\\"Mensagens prontas para WhatsApp/DM/e-mail (captação + follow-up)\\\",\\n          \\\"Roteiro curto de pré-venda para posicionar valor antes do preço\\\",\\n          \\\"Sequência de onboarding das primeiras semanas (8 semanas)\\\",\\n          \\\"Marcos de progresso (o que o aluno precisa perceber e quando)\\\",\\n          \\\"Check-ins prontos (para aumentar percepção de cuidado e reduzir desistência)\\\"\\n        ],\\n        \\\"formatNote\\\": \\\"Você recebe em formato copiável (texto), pronto para colar e ajustar.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"benefits\\\",\\n        \\\"type\\\": \\\"benefitsSection\\\",\\n        \\\"title\\\": \\\"O que isso ajuda a destravar\\\",\\n        \\\"columns\\\": [\\n          {\\n            \\\"title\\\": \\\"Antes\\\",\\n            \\\"bullets\\\": [\\n              \\\"Conversa vira preço cedo\\\",\\n              \\\"Aluno entra e desengaja no início\\\",\\n              \\\"Agenda instável\\\",\\n              \\\"Renovação imprevisível\\\"\\n            ]\\n          },\\n          {\\n            \\\"title\\\": \\\"Com o Kit (aplicando a sequência)\\\",\\n            \\\"bullets\\\": [\\n              \\\"Mais clareza do seu diferencial antes do preço\\\",\\n              \\\"Primeiras semanas com ‘vitórias rápidas’\\\",\\n              \\\"Mais constância no fim do mês\\\",\\n              \\\"Mais alunos certos e menos desistência\\\"\\n            ]\\n          }\\n        ]\\n      },\\n      {\\n        \\\"id\\\": \\\"proof\\\",\\n        \\\"type\\\": \\\"proofSection\\\",\\n        \\\"title\\\": \\\"Por que focar nas primeiras semanas funciona\\\",\\n        \\\"content\\\": \\\"Os primeiros 30 dias costumam ser decisivos para o aluno criar hábito e perceber valor. Boas práticas de onboarding e retenção indicam que clareza do caminho + vitórias rápidas aumentam engajamento e permanência. Este Kit foi estruturado com esse foco: deixar o valor claro e acelerar a percepção de progresso desde o começo.\\\",\\n        \\\"source\\\": \\\"Referência: boas práticas de onboarding e retenção (ex.: Glofox).\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"objections\\\",\\n        \\\"type\\\": \\\"objectionsSection\\\",\\n        \\\"title\\\": \\\"Perguntas comuns (sem enrolação)\\\",\\n        \\\"faqs\\\": [\\n          {\\n            \\\"q\\\": \\\"Isso é consultoria ou acompanhamento?\\\",\\n            \\\"a\\\": \\\"Não. Você recebe ativos digitais gerados por IA (mensagens, roteiros, marcos e check-ins). Não inclui call, gestão humana ou acompanhamento manual.\\\"\\n          },\\n          {\\n            \\\"q\\\": \\\"Funciona para qualquer tipo de acompanhamento?\\\",\\n            \\\"a\\\": \\\"Funciona melhor para serviços recorrentes com alunos/cliente que precisam de orientação e rotina (ex.: treinos, aulas, mentoria prática, acompanhamento profissional). A prévia já ajuda você a ver o encaixe.\\\"\\n          },\\n          {\\n            \\\"q\\\": \\\"Vou precisar ‘entender marketing’?\\\",\\n            \\\"a\\\": \\\"Não. O material é escrito em linguagem de rotina (agenda, alunos, preço, começo). Você só aplica a sequência e adapta termos ao seu jeito.\\\"\\n          },\\n          {\\n            \\\"q\\\": \\\"Em quanto tempo vou ver resultado?\\\",\\n            \\\"a\\\": \\\"A proposta é um ciclo de até 8 semanas. Resultados variam conforme sua execução, oferta e contexto.\\\"\\n          },\\n          {\\n            \\\"q\\\": \\\"O que significa ‘gerado por IA’?\\\",\\n            \\\"a\\\": \\\"Significa que os textos e a estrutura do Kit são gerados automaticamente a partir do seu briefing, para você ganhar velocidade e consistência.\\\"\\n          }\\n        ]\\n      },\\n      {\\n        \\\"id\\\": \\\"cta\\\",\\n        \\\"type\\\": \\\"ctaSection\\\",\\n        \\\"title\\\": \\\"Desbloqueie a prévia do Kit agora\\\",\\n        \\\"subtitle\\\": \\\"Veja como fica um ciclo de 8 semanas pronto para captar melhor e segurar o aluno no começo — sem depender de indicação e sem desconto.\\\",\\n        \\\"ctaPrimary\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\",\\n        \\\"microcopy\\\": \\\"*Resultados variam conforme execução e contexto.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"footer\\\",\\n        \\\"type\\\": \\\"footer\\\",\\n        \\\"compliance\\\": [\\n          \\\"Sem garantias de resultado.\\\",\\n          \\\"Sem consultoria/call/gestão humana.\\\",\\n          \\\"Entrega de ativos digitais gerados por IA.\\\"\\n        ]\\n      }\\n    ],\\n    \\\"recommendedQuantity\\\": 4\\n  }\\n}\"}"}]}],"usage":{"input_tokens":5275,"output_tokens":2010,"prompt_tokens":null,"completion_tokens":null,"total_tokens":7285},"error":null,"status":"completed"}
```

### 6. Texto da Landing
- Data: 02/04, 15:13
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Persona: teste
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))
Metadados obrigatórios do experimento:
- primary_variable: Dor vs Resultado
- variant_id: variant-10
- stage: AD
- control_or_treatment: treatment
- asset_role: landing-page-copy

Tarefa alvo: landing-page-copy

Ângulo da campanha:
{"experimentMetadata":{"variant_id":"variant-10","asset_role":"campaign-angle","stage":"AD","primary_variable":"Dor vs Resultado","control_or_treatment":"treatment"},"campaignAngle":{"primaryPain":"Você até consegue alunos, mas não tem um processo simples e repetível: a conversa cai rápido em “quanto custa?”, o aluno não percebe valor nas primeiras semanas e o engajamento despenca — aí a agenda vira loteria de indicação e de preço.","singleMindedPromise":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","proofSummary":"Estrutura baseada em boas práticas de onboarding e primeiros 30 dias para aumentar percepção de valor e retenção, com marcos iniciais claros e mensagens que antecipam e reforçam o “porquê” do seu acompanhamento.","funnelStage":"Frio → Morno (Topo/Meio)","landingMatchLine":"Agenda cheia sem desconto em 8 semanas: sequência pronta para o aluno entender seu valor e engajar nas primeiras semanas.","primaryCTA":"Desbloquear o kit","mechanismSummary":"Você recebe um kit de ativos digitais gerados por IA (sequência de mensagens + marcos das primeiras semanas + check-ins) para aplicar um processo de entrada e primeiras vitórias que reduz a briga por preço e aumenta engajamento.","tone":"Direto, claro e orientado a resultado, sem prometer “milagre” e sem jargão.","cta":"Desbloquear o kit","primaryPromise":"Agenda cheia sem desconto em até 8 semanas, com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas."}}

Texto do anúncio:
{"adCopy":{"primaryTextVariants":[{"label":"dor","openingHookType":"dor","headline":"Agenda cheia sem desconto (8 semanas)","placementHint":"feed","lengthVariants":{"curta":"A conversa com aluno vira “quanto custa?” e some na 2ª semana?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos entendendo seu valor e ficando engajados no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"A conversa vira “quanto custa?” cedo demais. O aluno entra empolgado… e some nas primeiras semanas. Aí sua agenda vira loteria de indicação e de preço.\n\nA ideia aqui é ter constância: em até 8 semanas, encher sua agenda sem dar desconto — com alunos chegando entendendo o valor do seu acompanhamento e permanecendo engajados no começo.\n\nPara isso, você desbloqueia uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto pra conduzir a conversa sem cair no preço cedo\n• marcos de progresso + check-ins das primeiras semanas\n\nSem call. Sem consultoria. Só ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"A conversa cai em “quanto custa?” e o aluno desengaja no começo?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e sentindo progresso logo nas primeiras semanas.\n\nDesbloqueie a prévia do Kit gerado por IA com mensagens prontas, roteiro de conversa e check-ins de onboarding.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA): mensagens + roteiro + check-ins.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"resultado","openingHookType":"resultado","headline":"Mais alunos certos. Menos preço.","placementHint":"stories/reels","lengthVariants":{"curta":"Agenda cheia em até 8 semanas — sem dar desconto.\n\nAlunos chegam entendendo seu valor e ficam engajados nas primeiras semanas.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Agenda cheia em até 8 semanas, sem dar desconto.\n\nA diferença não é “fazer mais”. É ter um ciclo simples para:\n1) o aluno entender seu valor antes de falar de preço\n2) o aluno sentir cuidado + progresso rápido nas primeiras semanas (quando mais desiste)\n\nDesbloqueie uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• convite e mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto de conversa (sem cair no preço cedo)\n• marcos iniciais + check-ins para segurar o engajamento\n\nSem call. Sem consultoria. Ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Em até 8 semanas, encha sua agenda sem dar desconto.\n\nCom uma sequência pronta, o aluno chega mais preparado (entende seu valor) e o começo já tem marcos + check-ins pra manter engajamento.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA) para captar e reter melhor.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"prova","openingHookType":"prova","headline":"Primeiros 30 dias: faz diferença","placementHint":"feed","lengthVariants":{"curta":"Os primeiros 30 dias costumam decidir se o aluno engaja ou some.\n\nEm até 8 semanas, encha sua agenda sem desconto com uma sequência que deixa seu valor claro + cria vitórias rápidas no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Os primeiros 30 dias costumam ser decisivos: quando o aluno tem clareza do caminho + pequenas vitórias logo no começo, a percepção de valor sobe e o engajamento tende a aumentar (boas práticas de onboarding e retenção).\n\nCom isso em mente, a promessa é simples: em até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas.\n\nDesbloqueie a prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto para conduzir a conversa antes do preço\n• marcos iniciais + check-ins para o aluno sentir progresso e cuidado\n\nSem call. Sem consultoria. Apenas ativos digitais.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Boas práticas de onboarding mostram: clareza + vitórias rápidas no começo aumentam a percepção de valor.\n\nEm até 8 semanas, encha sua agenda sem dar desconto com uma sequência pronta de comunicação + marcos + check-ins.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Estrutura com foco nos 30 primeiros dias (prévia).","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-copy"}}

Briefing da imagem:
{"adImageBriefing":{"briefings":[{"safeMargins":"Feed 1080x1350: manter 10% de margem de segurança (texto e elementos dentro da área central). Stories 1080x1920: evitar topo (UI) e base (CTA nativo); texto principal no terço central.","formatByPlacement":"Priorizar Feed (1080x1350) com leitura instantânea; adaptar para Stories/Reels (1080x1920) centralizando a metáfora visual e movendo CTA para acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação DOR do anúncio: dor = conversa vira “quanto custa?” cedo + aluno some nas primeiras semanas. Ainda assim, manter a promessa central igual à landing: agenda cheia sem desconto em até 8 semanas. CTA igual à landing: “Desbloquear a prévia/kit”.","imageTextMaxWords":9,"assetType":"estatico","hierarchy":"1) Dor (gatilho rápido) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy de compliance (se couber)","mustMatchAdVariant":"dor","visualAngle":"dor","complianceNotes":"Evitar garantias absolutas e resultados individuais. Não mencionar consultoria/call/gestão humana como entrega. “Gerado por IA” como característica, não milagre. Se usar “em até 8 semanas”, tratar como objetivo/estrutura (aspiracional). Microcopy opcional: “Sem call. Sem consultoria. Ativos digitais (IA).”","visualBriefing":"Conceito: ‘A conversa cai no preço + sumiço na semana 2’. Visual principal em split/contraste com dois balões de chat grandes no centro.\n\nComposição (Feed):\n- Fundo branco/cinza muito claro.\n- Metade esquerda (apagada/cinza): balão de chat com texto curto ‘Quanto custa?’ + ícone de pausa (||) ou ponto de interrogação.\n- Metade direita (ativa/azul escuro #0B1F3B): balão de chat com texto curto ‘Entendi como funciona’ + ícone de check verde #22C55E.\n- No topo, headline curta (máx. 1 linha): ‘Agenda cheia sem desconto’ (ou ‘Chega de briga de preço’ se couber sem perder promessa; manter promessa explícita em selo/linha 2).\n- Selo pequeno em verde: ‘8 SEMANAS’.\n- Rodapé com botão/box contrastante: ‘Desbloquear a prévia’.\n- Microcopy pequeno (se couber, 1 linha): ‘Ativos digitais gerados por IA’.\n\nTexto sugerido (<=9 palavras totais sobre imagem, priorizando):\n- Linha 1: ‘Agenda cheia sem desconto’ (4)\n- Selo: ‘em até 8 semanas’ (4) — se faltar espaço, usar apenas ‘8 semanas’ (2)\n- Botão: ‘Desbloquear a prévia’ (3)\n(Obs.: escolher combinações para respeitar o limite de palavras; preferir ‘Agenda cheia sem desconto’ + ‘8 semanas’ + ‘Desbloquear a prévia’).\n\nÍcones: chat, pause, check.\n\nAdaptação Stories: chats no terço central; headline acima; botão ‘Desbloquear a prévia’ logo abaixo dos chats (antes da área do CTA nativo)."},{"safeMargins":"Stories/Reels 1080x1920: texto e elementos críticos no terço central; evitar topo (UI) e base (CTA nativo). Feed 1080x1350: 10% de margem de segurança em todos os lados.","formatByPlacement":"Priorizar Stories/Reels (1080x1920) com layout vertical e timeline central; adaptar para Feed (1080x1350) mantendo a timeline com 3 marcos visuais e um selo ‘8 semanas’.","messageMatchNotes":"Deve bater com a variação RESULTADO do anúncio: ‘Agenda cheia em até 8 semanas — sem dar desconto’ + ‘alunos chegam entendendo seu valor e ficam engajados no começo’. Manter a mesma promessa do hero da landing e CTA de desbloqueio da prévia do kit.","imageTextMaxWords":10,"assetType":"story-vertical","hierarchy":"1) Resultado (agenda cheia sem desconto) 2) Prazo (8 semanas) 3) Benefício (alunos entendem valor + engajam) 4) CTA 5) Microcopy (IA / sem consultoria)","mustMatchAdVariant":"resultado","visualAngle":"resultado","complianceNotes":"Não prometer ‘agenda cheia garantida’. Usar linguagem aspiracional e de processo (‘com uma sequência pronta’). Não usar termos técnicos (funil/lead). Sem consultoria/call. Incluir ‘Ativos digitais (IA)’ quando possível.","visualBriefing":"Conceito: ‘8 semanas sob controle’ (timeline simples) para passar constância e previsibilidade.\n\nComposição (Stories):\n- Fundo branco com blocos/linhas finas em azul escuro (#0B1F3B) como moldura.\n- Centro: timeline vertical com 8 pontos (Sem 1 a Sem 8) e checks verdes (#22C55E) nos primeiros 3–4 para indicar progresso.\n- Ao lado de 3 pontos, mini-ícones:\n  - Sem 1: ícone chat (mensagens)\n  - Sem 2: ícone documento (roteiro)\n  - Sem 3: ícone check (check-ins)\n- Topo (texto grande): ‘Agenda cheia sem desconto’.\n- Logo abaixo (texto menor): ‘em até 8 semanas’.\n- Linha curta de benefício (se couber): ‘alunos entendem seu valor’.\n- Botão/box no terço inferior (mas acima da área do CTA nativo): ‘Desbloquear a prévia’.\n- Microcopy pequeno: ‘Ativos digitais (IA)’.\n\nTexto sugerido (<=10 palavras totais sobre imagem):\n- ‘Agenda cheia sem desconto’ (4)\n- ‘em até 8 semanas’ (4)\n- ‘Desbloquear a prévia’ (3)\n(Escolher 2 das 3 linhas conforme espaço para respeitar o limite; manter sempre promessa + CTA; o ‘8 semanas’ pode ir no selo).\n\nAdaptação Feed: timeline horizontal/compacta (8 marcadores) no meio; selo ‘8 semanas’; CTA no rodapé."},{"safeMargins":"Feed 1080x1350: 10% de margem de segurança. Stories 1080x1920: manter conteúdo principal no terço central e deixar respiro no topo/base para UI e CTA nativo.","formatByPlacement":"Priorizar Feed (1080x1350) com ‘card de prova’ + referência aos primeiros 30 dias; adaptar para Stories (1080x1920) mantendo o card central grande e CTA acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação PROVA do anúncio: ‘Primeiros 30 dias fazem diferença’ (boas práticas de onboarding) e conectar com a promessa de ‘agenda cheia sem desconto em até 8 semanas’. CTA deve ser ‘Desbloquear a prévia’ (equivalente ao ‘Desbloquear o Kit’ da landing).","imageTextMaxWords":11,"assetType":"carrossel","hierarchy":"1) Prova (primeiros 30 dias) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy (sem consultoria)","mustMatchAdVariant":"prova","visualAngle":"prova","complianceNotes":"Evitar ‘comprovado’/‘garantido’. Tratar como boas práticas/referência. Não usar logos de terceiros. Não prometer acompanhamento humano. Incluir ‘Ativos digitais gerados por IA’ como característica, não como promessa.","visualBriefing":"Formato: Carrossel 3 cards (1080x1350) — cada card com pouquíssimo texto e ícones claros.\n\nCard 1 (PROVA):\n- Headline: ‘Primeiros 30 dias’\n- Sub (curto): ‘decidem o engajamento’\n- Visual: mini-calendário 30 dias com 4 checks verdes (semanas) e uma seta apontando ‘continuidade’.\n\nCard 2 (CONEXÃO COM PROMESSA):\n- Headline: ‘8 semanas’\n- Badge: ‘SEM DESCONTO’\n- Sub (curto): ‘sequência pronta’\n- Visual: trilho/linha do tempo com 8 pontos + 2 ícones (chat + check).\n\nCard 3 (AÇÃO/PRODUTO):\n- Headline: ‘Desbloqueie a prévia’\n- Sub (curto): ‘mensagens + check-ins’\n- Micro (pequeno): ‘Ativos digitais (IA). Sem consultoria.’\n- Botão/box: ‘Desbloquear a prévia’.\n\nPaleta e estilo:\n- Fundo branco/cinza claro; textos em azul escuro (#0B1F3B); destaques em verde (#22C55E).\n- Tipografia sans bold para headline.\n\nControle de texto (<=11 palavras por card):\n- Card 1 exemplo: ‘Primeiros 30 dias decidem o engajamento’ (6)\n- Card 2 exemplo: ‘8 semanas. Sem desconto. Sequência pronta.’ (6)\n- Card 3 exemplo: ‘Desbloquear a prévia’ (3) + ‘Ativos digitais (IA)’ (3) (ajustar para não estourar).\n\nObservação: manter a promessa central explícita no Card 2 para garantir consistência com a landing."}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-image-briefing"}}

Textos da landing:
{
  "landing-page-copy": {
    "version": "Experiment #10 — Agenda Cheia Sem Desconto (8 Semanas)",
    "primaryPromise": "Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e permanecendo engajados nas primeiras semanas.",
    "ctaPrimary": "Desbloquear o Kit (receber a prévia gerada por IA)",
    "ctaSecondary": "Ver o que vem na prévia",
    "sections": [
      {
        "id": "hero",
        "type": "hero",
        "preheader": "Agenda cheia com constância — sem virar briga de preço",
        "headline": "Encha sua agenda em até 8 semanas sem dar desconto",
        "subheadline": "Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA — para atrair alunos que entendem seu valor e manter engajamento forte nas primeiras semanas.",
        "ctaPrimary": "Desbloquear o Kit (receber a prévia gerada por IA)",
        "ctaSecondary": "Ver o que vem na prévia",
        "microcopy": "Sem call. Sem consultoria. Você recebe ativos digitais prontos (gerados por IA).",
        "trustBullets": [
          "Mensagens prontas (WhatsApp/DM/e-mail)",
          "Roteiro curto para não cair no preço cedo", 
          "Onboarding com marcos + check-ins (8 semanas)"
        ]
      },
      {
        "id": "problem",
        "type": "problemSection",
        "title": "Se todo mês parece uma loteria, não é falta de esforço — é falta de sequência",
        "bullets": [
          "A captação depende de indicação e oscila.",
          "Quando aparece alguém, a conversa cai rápido em ‘quanto custa?’ — antes de você mostrar seu diferencial.",
          "Nas primeiras semanas o aluno ainda não tem hábito: se não sente progresso e cuidado, desengaja e some.",
          "Renovação vira tentativa e erro: alguns ficam, outros somem sem você entender o porquê."
        ],
        "transition": "A saída é um processo simples e repetível: posicionar valor antes do preço + criar vitórias rápidas logo no começo."
      },
      {
        "id": "promise",
        "type": "promiseSection",
        "title": "A promessa central",
        "content": "Em até 8 semanas, você passa a ter um ciclo claro para encher a agenda sem desconto — com alunos chegando mais preparados (entendendo seu valor) e permanecendo engajados nas primeiras semanas por sentirem progresso e cuidado desde o início."
      },
      {
        "id": "mechanism",
        "type": "mechanismSection",
        "title": "Como funciona (sem complicar)",
        "steps": [
          {
            "step": "1",
            "title": "Você responde um briefing rápido",
            "text": "Algumas perguntas simples sobre seu acompanhamento, público e como você trabalha."
          },
          {
            "step": "2",
            "title": "A IA gera sua prévia do Kit de 8 semanas",
            "text": "Você recebe mensagens e roteiros prontos para usar e adaptar (WhatsApp/DM/e-mail), além do plano de começo com marcos e check-ins."
          },
          {
            "step": "3",
            "title": "Você aplica a sequência no seu dia a dia",
            "text": "Usa o roteiro para conduzir conversas sem cair no preço cedo demais e usa o onboarding para criar progresso rápido nas primeiras semanas."
          }
        ],
        "note": "O Kit é o ‘mapa’ e os textos prontos. A execução depende de você e do seu contexto."
      },
      {
        "id": "whats-inside",
        "type": "whatsInsideSection",
        "title": "O que vem na prévia do Kit (gerada por IA)",
        "items": [
          "Mensagens prontas para WhatsApp/DM/e-mail (captação + follow-up)",
          "Roteiro curto de pré-venda para posicionar valor antes do preço",
          "Sequência de onboarding das primeiras semanas (8 semanas)",
          "Marcos de progresso (o que o aluno precisa perceber e quando)",
          "Check-ins prontos (para aumentar percepção de cuidado e reduzir desistência)"
        ],
        "formatNote": "Você recebe em formato copiável (texto), pronto para colar e ajustar."
      },
      {
        "id": "benefits",
        "type": "benefitsSection",
        "title": "O que isso ajuda a destravar",
        "columns": [
          {
            "title": "Antes",
            "bullets": [
              "Conversa vira preço cedo",
              "Aluno entra e desengaja no início",
              "Agenda instável",
              "Renovação imprevisível"
            ]
          },
          {
            "title": "Com o Kit (aplicando a sequência)",
            "bullets": [
              "Mais clareza do seu diferencial antes do preço",
              "Primeiras semanas com ‘vitórias rápidas’",
              "Mais constância no fim do mês",
              "Mais alunos certos e menos desistência"
            ]
          }
        ]
      },
      {
        "id": "proof",
        "type": "proofSection",
        "title": "Por que focar nas primeiras semanas funciona",
        "content": "Os primeiros 30 dias costumam ser decisivos para o aluno criar hábito e perceber valor. Boas práticas de onboarding e retenção indicam que clareza do caminho + vitórias rápidas aumentam engajamento e permanência. Este Kit foi estruturado com esse foco: deixar o valor claro e acelerar a percepção de progresso desde o começo.",
        "source": "Referência: boas práticas de onboarding e retenção (ex.: Glofox)."
      },
      {
        "id": "objections",
        "type": "objectionsSection",
        "title": "Perguntas comuns (sem enrolação)",
        "faqs": [
          {
            "q": "Isso é consultoria ou acompanhamento?",
            "a": "Não. Você recebe ativos digitais gerados por IA (mensagens, roteiros, marcos e check-ins). Não inclui call, gestão humana ou acompanhamento manual."
          },
          {
            "q": "Funciona para qualquer tipo de acompanhamento?",
            "a": "Funciona melhor para serviços recorrentes com alunos/cliente que precisam de orientação e rotina (ex.: treinos, aulas, mentoria prática, acompanhamento profissional). A prévia já ajuda você a ver o encaixe."
          },
          {
            "q": "Vou precisar ‘entender marketing’?",
            "a": "Não. O material é escrito em linguagem de rotina (agenda, alunos, preço, começo). Você só aplica a sequência e adapta termos ao seu jeito."
          },
          {
            "q": "Em quanto tempo vou ver resultado?",
            "a": "A proposta é um ciclo de até 8 semanas. Resultados variam conforme sua execução, oferta e contexto."
          },
          {
            "q": "O que significa ‘gerado por IA’?",
            "a": "Significa que os textos e a estrutura do Kit são gerados automaticamente a partir do seu briefing, para você ganhar velocidade e consistência."
          }
        ]
      },
      {
        "id": "cta",
        "type": "ctaSection",
        "title": "Desbloqueie a prévia do Kit agora",
        "subtitle": "Veja como fica um ciclo de 8 semanas pronto para captar melhor e segurar o aluno no começo — sem depender de indicação e sem desconto.",
        "ctaPrimary": "Desbloquear o Kit (receber a prévia gerada por IA)",
        "microcopy": "*Resultados variam conforme execução e contexto."
      },
      {
        "id": "footer",
        "type": "footer",
        "compliance": [
          "Sem garantias de resultado.",
          "Sem consultoria/call/gestão humana.",
          "Entrega de ativos digitais gerados por IA."
        ]
      }
    ],
    "recommendedQuantity": 4
  }
}

Instruções extras do usuário:
Quantidade sugerida: 4

Responda exclusivamente em JSON válido e siga estritamente o schema da seção.
```

**Resultado**
```text
{"output_text":null,"id":"resp_0e52aa2d0d4789250069ceb1b42e4c8194ab002e2fbd64d369","output":[{"tool_call":null,"tool_calls":null,"id":"msg_0e52aa2d0d4789250069ceb1b4d84c8194874024efd02df074","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"experimentMetadata\":{\"control_or_treatment\":\"treatment\",\"variant_id\":\"variant-10\",\"asset_role\":\"landing-page-copy\",\"stage\":\"AD\",\"primary_variable\":\"Dor vs Resultado\"},\"landingPageCopy\":{}}"}]}],"usage":{"input_tokens":6347,"output_tokens":56,"prompt_tokens":null,"completion_tokens":null,"total_tokens":6403},"error":null,"status":"completed"}
```

### 7. Texto da Landing
- Data: 02/04, 18:55
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))
Metadados obrigatórios do experimento:
- primary_variable: Dor vs Resultado
- variant_id: variant-10
- stage: AD
- control_or_treatment: treatment
- asset_role: landing-page-copy

Tarefa alvo: landing-page-copy

Ângulo da campanha:
{"experimentMetadata":{"variant_id":"variant-10","asset_role":"campaign-angle","stage":"AD","primary_variable":"Dor vs Resultado","control_or_treatment":"treatment"},"campaignAngle":{"primaryPain":"Você até consegue alunos, mas não tem um processo simples e repetível: a conversa cai rápido em “quanto custa?”, o aluno não percebe valor nas primeiras semanas e o engajamento despenca — aí a agenda vira loteria de indicação e de preço.","singleMindedPromise":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","proofSummary":"Estrutura baseada em boas práticas de onboarding e primeiros 30 dias para aumentar percepção de valor e retenção, com marcos iniciais claros e mensagens que antecipam e reforçam o “porquê” do seu acompanhamento.","funnelStage":"Frio → Morno (Topo/Meio)","landingMatchLine":"Agenda cheia sem desconto em 8 semanas: sequência pronta para o aluno entender seu valor e engajar nas primeiras semanas.","primaryCTA":"Desbloquear o kit","mechanismSummary":"Você recebe um kit de ativos digitais gerados por IA (sequência de mensagens + marcos das primeiras semanas + check-ins) para aplicar um processo de entrada e primeiras vitórias que reduz a briga por preço e aumenta engajamento.","tone":"Direto, claro e orientado a resultado, sem prometer “milagre” e sem jargão.","cta":"Desbloquear o kit","primaryPromise":"Agenda cheia sem desconto em até 8 semanas, com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas."}}

Texto do anúncio:
{"adCopy":{"primaryTextVariants":[{"label":"dor","openingHookType":"dor","headline":"Agenda cheia sem desconto (8 semanas)","placementHint":"feed","lengthVariants":{"curta":"A conversa com aluno vira “quanto custa?” e some na 2ª semana?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos entendendo seu valor e ficando engajados no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"A conversa vira “quanto custa?” cedo demais. O aluno entra empolgado… e some nas primeiras semanas. Aí sua agenda vira loteria de indicação e de preço.\n\nA ideia aqui é ter constância: em até 8 semanas, encher sua agenda sem dar desconto — com alunos chegando entendendo o valor do seu acompanhamento e permanecendo engajados no começo.\n\nPara isso, você desbloqueia uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto pra conduzir a conversa sem cair no preço cedo\n• marcos de progresso + check-ins das primeiras semanas\n\nSem call. Sem consultoria. Só ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"A conversa cai em “quanto custa?” e o aluno desengaja no começo?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e sentindo progresso logo nas primeiras semanas.\n\nDesbloqueie a prévia do Kit gerado por IA com mensagens prontas, roteiro de conversa e check-ins de onboarding.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA): mensagens + roteiro + check-ins.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"resultado","openingHookType":"resultado","headline":"Mais alunos certos. Menos preço.","placementHint":"stories/reels","lengthVariants":{"curta":"Agenda cheia em até 8 semanas — sem dar desconto.\n\nAlunos chegam entendendo seu valor e ficam engajados nas primeiras semanas.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Agenda cheia em até 8 semanas, sem dar desconto.\n\nA diferença não é “fazer mais”. É ter um ciclo simples para:\n1) o aluno entender seu valor antes de falar de preço\n2) o aluno sentir cuidado + progresso rápido nas primeiras semanas (quando mais desiste)\n\nDesbloqueie uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• convite e mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto de conversa (sem cair no preço cedo)\n• marcos iniciais + check-ins para segurar o engajamento\n\nSem call. Sem consultoria. Ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Em até 8 semanas, encha sua agenda sem dar desconto.\n\nCom uma sequência pronta, o aluno chega mais preparado (entende seu valor) e o começo já tem marcos + check-ins pra manter engajamento.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA) para captar e reter melhor.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"prova","openingHookType":"prova","headline":"Primeiros 30 dias: faz diferença","placementHint":"feed","lengthVariants":{"curta":"Os primeiros 30 dias costumam decidir se o aluno engaja ou some.\n\nEm até 8 semanas, encha sua agenda sem desconto com uma sequência que deixa seu valor claro + cria vitórias rápidas no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Os primeiros 30 dias costumam ser decisivos: quando o aluno tem clareza do caminho + pequenas vitórias logo no começo, a percepção de valor sobe e o engajamento tende a aumentar (boas práticas de onboarding e retenção).\n\nCom isso em mente, a promessa é simples: em até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas.\n\nDesbloqueie a prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto para conduzir a conversa antes do preço\n• marcos iniciais + check-ins para o aluno sentir progresso e cuidado\n\nSem call. Sem consultoria. Apenas ativos digitais.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Boas práticas de onboarding mostram: clareza + vitórias rápidas no começo aumentam a percepção de valor.\n\nEm até 8 semanas, encha sua agenda sem dar desconto com uma sequência pronta de comunicação + marcos + check-ins.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Estrutura com foco nos 30 primeiros dias (prévia).","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-copy"}}

Briefing da imagem:
{"adImageBriefing":{"briefings":[{"safeMargins":"Feed 1080x1350: manter 10% de margem de segurança (texto e elementos dentro da área central). Stories 1080x1920: evitar topo (UI) e base (CTA nativo); texto principal no terço central.","formatByPlacement":"Priorizar Feed (1080x1350) com leitura instantânea; adaptar para Stories/Reels (1080x1920) centralizando a metáfora visual e movendo CTA para acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação DOR do anúncio: dor = conversa vira “quanto custa?” cedo + aluno some nas primeiras semanas. Ainda assim, manter a promessa central igual à landing: agenda cheia sem desconto em até 8 semanas. CTA igual à landing: “Desbloquear a prévia/kit”.","imageTextMaxWords":9,"assetType":"estatico","hierarchy":"1) Dor (gatilho rápido) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy de compliance (se couber)","mustMatchAdVariant":"dor","visualAngle":"dor","complianceNotes":"Evitar garantias absolutas e resultados individuais. Não mencionar consultoria/call/gestão humana como entrega. “Gerado por IA” como característica, não milagre. Se usar “em até 8 semanas”, tratar como objetivo/estrutura (aspiracional). Microcopy opcional: “Sem call. Sem consultoria. Ativos digitais (IA).”","visualBriefing":"Conceito: ‘A conversa cai no preço + sumiço na semana 2’. Visual principal em split/contraste com dois balões de chat grandes no centro.\n\nComposição (Feed):\n- Fundo branco/cinza muito claro.\n- Metade esquerda (apagada/cinza): balão de chat com texto curto ‘Quanto custa?’ + ícone de pausa (||) ou ponto de interrogação.\n- Metade direita (ativa/azul escuro #0B1F3B): balão de chat com texto curto ‘Entendi como funciona’ + ícone de check verde #22C55E.\n- No topo, headline curta (máx. 1 linha): ‘Agenda cheia sem desconto’ (ou ‘Chega de briga de preço’ se couber sem perder promessa; manter promessa explícita em selo/linha 2).\n- Selo pequeno em verde: ‘8 SEMANAS’.\n- Rodapé com botão/box contrastante: ‘Desbloquear a prévia’.\n- Microcopy pequeno (se couber, 1 linha): ‘Ativos digitais gerados por IA’.\n\nTexto sugerido (<=9 palavras totais sobre imagem, priorizando):\n- Linha 1: ‘Agenda cheia sem desconto’ (4)\n- Selo: ‘em até 8 semanas’ (4) — se faltar espaço, usar apenas ‘8 semanas’ (2)\n- Botão: ‘Desbloquear a prévia’ (3)\n(Obs.: escolher combinações para respeitar o limite de palavras; preferir ‘Agenda cheia sem desconto’ + ‘8 semanas’ + ‘Desbloquear a prévia’).\n\nÍcones: chat, pause, check.\n\nAdaptação Stories: chats no terço central; headline acima; botão ‘Desbloquear a prévia’ logo abaixo dos chats (antes da área do CTA nativo)."},{"safeMargins":"Stories/Reels 1080x1920: texto e elementos críticos no terço central; evitar topo (UI) e base (CTA nativo). Feed 1080x1350: 10% de margem de segurança em todos os lados.","formatByPlacement":"Priorizar Stories/Reels (1080x1920) com layout vertical e timeline central; adaptar para Feed (1080x1350) mantendo a timeline com 3 marcos visuais e um selo ‘8 semanas’.","messageMatchNotes":"Deve bater com a variação RESULTADO do anúncio: ‘Agenda cheia em até 8 semanas — sem dar desconto’ + ‘alunos chegam entendendo seu valor e ficam engajados no começo’. Manter a mesma promessa do hero da landing e CTA de desbloqueio da prévia do kit.","imageTextMaxWords":10,"assetType":"story-vertical","hierarchy":"1) Resultado (agenda cheia sem desconto) 2) Prazo (8 semanas) 3) Benefício (alunos entendem valor + engajam) 4) CTA 5) Microcopy (IA / sem consultoria)","mustMatchAdVariant":"resultado","visualAngle":"resultado","complianceNotes":"Não prometer ‘agenda cheia garantida’. Usar linguagem aspiracional e de processo (‘com uma sequência pronta’). Não usar termos técnicos (funil/lead). Sem consultoria/call. Incluir ‘Ativos digitais (IA)’ quando possível.","visualBriefing":"Conceito: ‘8 semanas sob controle’ (timeline simples) para passar constância e previsibilidade.\n\nComposição (Stories):\n- Fundo branco com blocos/linhas finas em azul escuro (#0B1F3B) como moldura.\n- Centro: timeline vertical com 8 pontos (Sem 1 a Sem 8) e checks verdes (#22C55E) nos primeiros 3–4 para indicar progresso.\n- Ao lado de 3 pontos, mini-ícones:\n  - Sem 1: ícone chat (mensagens)\n  - Sem 2: ícone documento (roteiro)\n  - Sem 3: ícone check (check-ins)\n- Topo (texto grande): ‘Agenda cheia sem desconto’.\n- Logo abaixo (texto menor): ‘em até 8 semanas’.\n- Linha curta de benefício (se couber): ‘alunos entendem seu valor’.\n- Botão/box no terço inferior (mas acima da área do CTA nativo): ‘Desbloquear a prévia’.\n- Microcopy pequeno: ‘Ativos digitais (IA)’.\n\nTexto sugerido (<=10 palavras totais sobre imagem):\n- ‘Agenda cheia sem desconto’ (4)\n- ‘em até 8 semanas’ (4)\n- ‘Desbloquear a prévia’ (3)\n(Escolher 2 das 3 linhas conforme espaço para respeitar o limite; manter sempre promessa + CTA; o ‘8 semanas’ pode ir no selo).\n\nAdaptação Feed: timeline horizontal/compacta (8 marcadores) no meio; selo ‘8 semanas’; CTA no rodapé."},{"safeMargins":"Feed 1080x1350: 10% de margem de segurança. Stories 1080x1920: manter conteúdo principal no terço central e deixar respiro no topo/base para UI e CTA nativo.","formatByPlacement":"Priorizar Feed (1080x1350) com ‘card de prova’ + referência aos primeiros 30 dias; adaptar para Stories (1080x1920) mantendo o card central grande e CTA acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação PROVA do anúncio: ‘Primeiros 30 dias fazem diferença’ (boas práticas de onboarding) e conectar com a promessa de ‘agenda cheia sem desconto em até 8 semanas’. CTA deve ser ‘Desbloquear a prévia’ (equivalente ao ‘Desbloquear o Kit’ da landing).","imageTextMaxWords":11,"assetType":"carrossel","hierarchy":"1) Prova (primeiros 30 dias) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy (sem consultoria)","mustMatchAdVariant":"prova","visualAngle":"prova","complianceNotes":"Evitar ‘comprovado’/‘garantido’. Tratar como boas práticas/referência. Não usar logos de terceiros. Não prometer acompanhamento humano. Incluir ‘Ativos digitais gerados por IA’ como característica, não como promessa.","visualBriefing":"Formato: Carrossel 3 cards (1080x1350) — cada card com pouquíssimo texto e ícones claros.\n\nCard 1 (PROVA):\n- Headline: ‘Primeiros 30 dias’\n- Sub (curto): ‘decidem o engajamento’\n- Visual: mini-calendário 30 dias com 4 checks verdes (semanas) e uma seta apontando ‘continuidade’.\n\nCard 2 (CONEXÃO COM PROMESSA):\n- Headline: ‘8 semanas’\n- Badge: ‘SEM DESCONTO’\n- Sub (curto): ‘sequência pronta’\n- Visual: trilho/linha do tempo com 8 pontos + 2 ícones (chat + check).\n\nCard 3 (AÇÃO/PRODUTO):\n- Headline: ‘Desbloqueie a prévia’\n- Sub (curto): ‘mensagens + check-ins’\n- Micro (pequeno): ‘Ativos digitais (IA). Sem consultoria.’\n- Botão/box: ‘Desbloquear a prévia’.\n\nPaleta e estilo:\n- Fundo branco/cinza claro; textos em azul escuro (#0B1F3B); destaques em verde (#22C55E).\n- Tipografia sans bold para headline.\n\nControle de texto (<=11 palavras por card):\n- Card 1 exemplo: ‘Primeiros 30 dias decidem o engajamento’ (6)\n- Card 2 exemplo: ‘8 semanas. Sem desconto. Sequência pronta.’ (6)\n- Card 3 exemplo: ‘Desbloquear a prévia’ (3) + ‘Ativos digitais (IA)’ (3) (ajustar para não estourar).\n\nObservação: manter a promessa central explícita no Card 2 para garantir consistência com a landing."}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-image-briefing"}}

Textos da landing:
{"experimentMetadata":{"control_or_treatment":"treatment","variant_id":"variant-10","asset_role":"landing-page-copy","stage":"AD","primary_variable":"Dor vs Resultado"},"landingPageCopy":{}}

Diretriz específica para copy da landing:
Você cria ativos de campanha para o Marketing Hub.

Regras globais:
1. O anúncio e a landing devem ter a mesma promessa central.
2. O CTA do anúncio deve combinar com a ação principal da landing.
3. O material precisa caber no envelope real do produto:
   - pode entregar ativos digitais gerados por IA
   - não pode prometer consultoria, call, gestão humana ou acompanhamento manual
4. Priorize clareza comercial:
   DOR → RESULTADO → MECANISMO → PROVA → AÇÃO
5. Não transforme mecanismo em promessa principal.
6. Não use jargão técnico desnecessário.
7. O público é geral dentro do nicho, com baixa a moderada maturidade em marketing.
8. Sempre escreva pensando em alta escala e geração automatizada.
9. O anúncio deve ser rápido de entender.
10. A landing deve aprofundar a promessa e reduzir ceticismo.

Headline clicada no anúncio: Agenda cheia sem desconto (8 semanas)
CTA do anúncio: Desbloquear o Kit (receber a prévia gerada por IA)
Linha de match com landing: Agenda cheia sem desconto em 8 semanas: sequência pronta para o aluno entender seu valor e engajar nas primeiras semanas.

Objetivo:
Transformar o clique qualificado em Desbloquear o Kit (receber a prévia gerada por IA) mantendo a mesma promessa e CTA do anúncio.

Regras:
1. messageMatchSource deve citar exatamente qual headline do anúncio está sendo espelhada.
2. hero.headline, hero.promise e pageGoal precisam repetir a single-minded promise sem variações criativas.
3. hero.ctaLabel, primaryCTA e todos os ctaBlocks devem usar exatamente o mesmo texto do CTA do anúncio.
4. bodySections deve cobrir pelo menos dor, mecanismo, prova e oferta com sectionType explícito.
5. Cada bodySections[i] precisa preencher sectionDependsOn (primaryPromise, mechanismSummary, proofSummary ou primaryCTA) e messageMatchNotes descrevendo o vínculo.
6. ctaBlocks deve repetir o CTA no hero, meio e final com placement entre hero, mid, final, sticky ou inline.
7. faq precisa ter no mínimo 3 perguntas com objectionTag descrevendo a objeção atendida.
8. consistencyChecks deve listar pelo menos CTA_MATCH, PROMISE_MATCH e GOOGLE_LANDING_BEST_PRACTICES indicando status PASS/WARN/FAIL.
9. messageMatchNotes e messageMatchSource devem garantir continuidade literal da promessa do anúncio.
10. complianceNotes precisa reforçar que a oferta é entregue por ativos digitais (sem consultoria ou call).

Formato obrigatório:
- Preencher hero com eyebrow, headline, subheadline, promise, supportingCopy, proofBadge, microcopy e CTA.
- Preencher messageMatchSource com a headline real do anúncio usada como referência: Agenda cheia sem desconto (8 semanas).
- Preencher bodySections com sectionId único, sectionType, resumo, bullets e CTA de apoio quando existir.
- Preencher ctaBlocks com placement, ctaVariant, matchAdCta e messageMatchNotes descrevendo onde o CTA aparece na página.
- Preencher faq e consistencyChecks conforme regras acima.
- Em primaryCTA e ctaBlocks[].matchAdCta usar exatamente: Desbloquear o Kit (receber a prévia gerada por IA).

Instruções extras do usuário:
Quantidade sugerida: 4

Responda exclusivamente em JSON válido e siga estritamente o schema da seção.
```

**Resultado**
```text
{"output_text":null,"id":"resp_0ea7c331040dac9d0069cee5c0bba48195b5bdaab3f53c7c69","output":[{"tool_call":null,"tool_calls":null,"id":"msg_0ea7c331040dac9d0069cee5c15d1881959c108e3003c3bca5","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"landingPageCopy\":{\"hero\":{\"headline\":\"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"supportingCopy\":\"Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) com mensagens prontas, roteiro curto de conversa e check-ins iniciais para reduzir a briga por preço e aumentar engajamento no começo — sem call e sem consultoria.\",\"eyebrow\":\"Agenda Cheia Sem Desconto (8 semanas)\",\"ctaMatchNotes\":\"O CTA é idêntico ao do anúncio e leva para o desbloqueio da prévia do kit (ativos digitais gerados por IA).\",\"microcopy\":\"Entrega 100% digital. Sem consultoria, sem call, sem gestão humana. Ativos gerados por IA para você adaptar e aplicar.\",\"promise\":\"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.\",\"subheadline\":\"Pare de depender de indicação e de “briga de preço”: tenha um processo simples, repetível e aplicável todo mês.\",\"ctaUrl\":\"#desbloquear\",\"proofBadge\":\"Baseado em boas práticas de onboarding e primeiros 30 dias (retenção e percepção de valor).\"},\"messageMatchNotes\":\"A promessa central e o CTA repetem literalmente o anúncio: agenda cheia sem desconto em até 8 semanas + desbloquear a prévia do kit gerada por IA.\",\"ctaBlocks\":[{\"ctaSupport\":\"Desbloqueie a prévia e veja como ficam as mensagens, os marcos e os check-ins das primeiras semanas.\",\"messageMatchNotes\":\"CTA no hero repete o mesmo texto do anúncio para continuidade imediata após o clique.\",\"ctaVariant\":\"primary\",\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"placement\":\"hero\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaUrl\":\"#desbloquear\"},{\"ctaSupport\":\"Se a conversa cai no preço cedo, use a sequência para deixar o valor claro antes do “quanto custa?”.\",\"messageMatchNotes\":\"CTA no meio aparece logo após explicar mecanismo (sequência) e como aplicar sem depender de consultoria.\",\"ctaVariant\":\"secondary\",\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"placement\":\"mid\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaUrl\":\"#desbloquear\"},{\"ctaSupport\":\"Leve a prévia agora e adapte em minutos para sua rotina (WhatsApp/DM/e-mail).\",\"messageMatchNotes\":\"CTA final reforça ação única (desbloquear a prévia) após prova + oferta para reduzir ceticismo.\",\"ctaVariant\":\"primary\",\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"placement\":\"final\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaUrl\":\"#desbloquear\"},{\"ctaSupport\":\"Prévia do kit (IA) para usar hoje: mensagens + roteiro + check-ins.\",\"messageMatchNotes\":\"CTA sticky mantém a mesma ação do anúncio durante toda a leitura, sem mudar a intenção.\",\"ctaVariant\":\"sticky\",\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"placement\":\"sticky\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaUrl\":\"#desbloquear\"}],\"consistencyChecks\":[{\"details\":\"CTA na página (hero/meio/final/sticky) usa exatamente ‘Desbloquear o Kit (receber a prévia gerada por IA)’, igual ao anúncio.\",\"check\":\"CTA_MATCH\",\"status\":\"PASS\"},{\"details\":\"Hero.headline, hero.promise e pageGoal repetem literalmente a single-minded promise fornecida no ângulo da campanha (sem variações criativas).\",\"check\":\"PROMISE_MATCH\",\"status\":\"PASS\"},{\"details\":\"Página com message match forte, hierarquia clara (dor→resultado→mecanismo→prova→ação), CTAs repetidos, microcopy de entrega e FAQs para reduzir objeções.\",\"check\":\"GOOGLE_LANDING_BEST_PRACTICES\",\"status\":\"PASS\"}],\"bodySections\":[{\"messageMatchNotes\":\"Conecta diretamente com a dor citada no anúncio (conversa vira ‘quanto custa?’ e aluno some no começo) e prepara o terreno para a promessa sem trocar o foco.\",\"bullets\":[\"A conversa cai no preço cedo demais — e você perde margem (ou perde o aluno).\",\"O aluno entra animado… e some na 2ª semana porque não enxerga progresso rápido.\",\"Sem um ‘primeiro mês’ bem guiado, o valor do acompanhamento fica invisível.\",\"Resultado: agenda vira loteria de indicação, de mês a mês.\"],\"ctaSupport\":\"Quer ver a sequência pronta que organiza isso em 8 semanas?\",\"summary\":\"Se você não deixa claro o seu valor antes do preço e não cria vitórias rápidas no começo, o aluno desengaja — e sua agenda vira loteria.\",\"sectionDependsOn\":\"primaryPromise\",\"sectionId\":\"s1-pain-preco-engajamento\",\"sectionType\":\"pain\",\"copy\":\"Quando não existe um processo simples e repetível, duas coisas acontecem quase sempre:\\n\\n1) a conversa vira ‘quanto custa?’ cedo demais (antes do aluno entender o diferente do seu acompanhamento)\\n2) nas primeiras semanas, o aluno não sente cuidado/progresso e começa a faltar, enrolar ou desistir\\n\\nAí o que deveria ser previsível vira estressante: você vive de indicação, de promoção e de negociação.\",\"title\":\"O que te trava não é falta de aluno. É falta de processo.\"},{\"messageMatchNotes\":\"Explica o mecanismo como suporte (sequência pronta de comunicação + marcos + check-ins), sem transformá-lo na promessa principal; mantém o match com o anúncio.\",\"bullets\":[\"Mensagens prontas para conduzir a conversa sem cair no preço cedo (WhatsApp/DM/e-mail).\",\"Roteiro curto para apresentar seu ‘porquê’ e o que muda no acompanhamento.\",\"Marcos das primeiras semanas para o aluno perceber evolução rápido.\",\"Check-ins simples (prontos) para reforçar cuidado e consistência no início.\"],\"ctaSupport\":\"Desbloqueie a prévia e veja exemplos prontos para adaptar ao seu serviço.\",\"summary\":\"Você aplica uma sequência pronta para o aluno entender seu valor antes do preço e sentir progresso logo no começo.\",\"sectionDependsOn\":\"mechanismSummary\",\"sectionId\":\"s2-mechanism-sequencia-pronta\",\"sectionType\":\"mechanism\",\"copy\":\"A proposta é colocar sob controle o que normalmente fica “no improviso”:\\n\\n• Comunicação de entrada: mensagens e roteiro para o aluno entender o valor do seu acompanhamento antes do preço dominar a conversa.\\n• Primeiras vitórias: marcos claros nas semanas iniciais para o aluno sentir progresso rápido.\\n• Check-ins: pontos de contato prontos para manter engajamento no começo (quando a desistência é mais comum).\\n\\nTudo em formato de ativos digitais gerados por IA — para você copiar, ajustar e aplicar.\",\"title\":\"O que muda: uma sequência pronta (com começo, meio e continuidade)\"} ,{\"messageMatchNotes\":\"Apoia a promessa com ‘prova’ em forma de boas práticas (onboarding/primeiros 30 dias) sem alegações absolutas; alinhado ao anúncio de prova e ao briefing.\",\"bullets\":[\"Clareza do caminho reduz ansiedade e aumenta a percepção de valor.\",\"Vitórias rápidas no começo aumentam a chance de continuidade.\",\"Check-ins consistentes reduzem ‘sumiço’ e melhoram engajamento.\"],\"ctaSupport\":\"Quer ver como isso vira mensagens e check-ins prontos?\",\"summary\":\"Os primeiros 30 dias são decisivos: clareza + vitórias rápidas tendem a aumentar percepção de valor e engajamento.\",\"sectionDependsOn\":\"proofSummary\",\"sectionId\":\"s3-proof-30-dias\",\"sectionType\":\"proof\",\"copy\":\"Em boas práticas de onboarding e retenção, o início costuma ser decisivo:\\n\\nQuando o aluno entende o caminho (o que vai acontecer e por quê) e percebe pequenos avanços logo nas primeiras semanas, a percepção de valor sobe — e o engajamento tende a acompanhar.\\n\\nPor isso o kit foca em organizar comunicação + marcos + check-ins justamente onde mais dá errado: no começo.\",\"title\":\"Por que funciona: o começo (primeiros 30 dias) define o resto\"} ,{\"messageMatchNotes\":\"Oferta descrita como prévia do kit (ativos digitais gerados por IA), sem prometer consultoria/call; mantém a promessa central como headline/hero, e a oferta como meio para chegar lá.\",\"bullets\":[\"Prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA.\",\"Mensagens prontas (WhatsApp/DM/e-mail) para antes e depois do ‘primeiro sim’.\",\"Roteiro curto para conduzir a conversa e posicionar valor antes do preço.\",\"Marcos das primeiras semanas + check-ins prontos para manter engajamento.\",\"Modelos editáveis para adaptar ao seu estilo (copiar/colar/ajustar).\"],\"ctaSupport\":\"Clique para desbloquear a prévia e receber os exemplos gerados por IA.\",\"summary\":\"Você recebe uma prévia do kit com os ativos essenciais para aplicar a sequência de 8 semanas na prática.\",\"sectionDependsOn\":\"primaryCTA\",\"sectionId\":\"s4-offer-previa-kit\",\"sectionType\":\"offer\",\"copy\":\"Ao desbloquear, você recebe uma prévia do kit com exemplos prontos para usar como base.\\n\\nImportante: é entrega digital (ativos), feita para escala — sem call, sem consultoria e sem acompanhamento manual.\\n\\nVocê adapta ao seu serviço e aplica no seu ritmo.\",\"title\":\"O que você recebe na prévia (entrega digital)\"}],\"messageMatchSource\":\"Agenda cheia sem desconto (8 semanas)\",\"pageGoal\":\"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.\",\"primaryCTA\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"faq\":[{\"question\":\"Isso é consultoria, mentoria ou tem call?\",\"answer\":\"Não. A entrega é 100% digital: ativos gerados por IA (mensagens, roteiro, marcos e check-ins) para você adaptar e aplicar. Não inclui call, consultoria, gestão humana ou acompanhamento manual.\",\"objectionTag\":\"entrega_sem_consultoria\"},{\"question\":\"Serve para qualquer tipo de acompanhamento (treino, nutrição, estética, aulas, etc.)?\",\"answer\":\"Sim, porque a lógica é de comunicação e onboarding: deixar o valor claro antes do preço e criar marcos iniciais + check-ins. Você só ajusta a linguagem e os marcos para sua realidade.\",\"objectionTag\":\"serve_para_meu_nicho\"},{\"question\":\"E se eu já tenho alunos, mas eles desengajam nas primeiras semanas?\",\"answer\":\"O kit foi pensado exatamente para esse ponto: organizar os primeiros marcos e os check-ins para o aluno sentir cuidado e progresso rápido no começo, reduzindo o ‘sumiço’ e aumentando a continuidade.\",\"objectionTag\":\"retencao_engajamento\"},{\"question\":\"Isso garante agenda cheia em 8 semanas?\",\"answer\":\"Não existe garantia de resultado individual. A promessa é um objetivo com base em um processo repetível (sequência de comunicação + marcos + check-ins) que costuma melhorar percepção de valor e engajamento quando aplicado de forma consistente.\",\"objectionTag\":\"sem_garantia_absoluta\"}],\"complianceNotes\":\"A oferta é uma prévia de um kit de ativos digitais gerados por IA (mensagens, roteiro, marcos e check-ins). Não há promessa de consultoria, call, gestão humana ou acompanhamento manual. Evita garantias absolutas e resultados individuais; ‘em até 8 semanas’ é tratado como objetivo/estrutura baseada em processo.\"},\"experimentMetadata\":{\"variant_id\":\"variant-10\",\"control_or_treatment\":\"treatment\",\"primary_variable\":\"Dor vs Resultado\",\"stage\":\"AD\",\"asset_role\":\"landing-page-copy\"}}"}]}],"usage":{"input_tokens":5765,"output_tokens":2612,"prompt_tokens":null,"completion_tokens":null,"total_tokens":8377},"error":null,"status":"completed"}
```

### 8. Layout da Landing
- Data: 01/04, 17:11
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Nome do experimento: 
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Persona: teste
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))

Tarefa alvo: landing-page-wireframe

Ângulo da campanha:
{
  "primaryPromise": "Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e permanecendo engajados nas primeiras semanas.",
  "primaryPain": "A captação vira ‘loteria de indicação’ e, quando chega gente, a conversa cai rápido em ‘quanto custa?’, porque o aluno não entende o diferencial e não sente valor/progresso logo no começo — daí desengaja e some.",
  "mechanismSummary": "Um Kit de Comunicação + Onboarding de 8 semanas gerado por IA: mensagens prontas (DM/WhatsApp/e-mail), roteiro de pré-venda para posicionar valor antes do preço, marcos de progresso das primeiras semanas e check-ins automáticos para aumentar percepção de cuidado e reduzir desistência.",
  "proofUsed": "Baseado em boas práticas de onboarding e retenção nos primeiros 30 dias (referência: Glofox) — focando em dar clareza e vitórias rápidas para aumentar percepção de valor e engajamento.",
  "cta": "Desbloquear o Kit (receber a prévia gerada por IA)",
  "funnelStage": "Topo → Meio (descoberta do problema + consideração de uma solução pronta)",
  "tone": "Direto, claro e orientado a resultado; sem jargão, com linguagem de rotina (agenda, alunos, preço, desistência)."
}

Texto do anúncio:
{
  "primaryTextVariants": [
    {
      "label": "dor",
      "primaryText": "A conversa vira ‘quanto custa?’ e o aluno some nas primeiras semanas? Isso acontece quando ele não entende seu diferencial e não sente progresso rápido.\n\nEncha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e ficando engajados no começo.\n\nDesbloqueie uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA: mensagens prontas pro WhatsApp/DM/e-mail, roteiro de pré-venda, marcos de progresso e check-ins.\n\nClique e receba a prévia.",
      "headline": "Agenda cheia em até 8 semanas (sem desconto)",
      "description": "Prévia do Kit gerado por IA: mensagens + roteiro + check-ins.",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    },
    {
      "label": "resultado",
      "primaryText": "Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento (não só perguntando preço) e permanecendo engajados nas primeiras semanas.\n\nVocê recebe uma prévia de um Kit pronto, gerado por IA a partir de um briefing simples: convite claro pra atrair o aluno certo, roteiro de conversa no WhatsApp pra guiar a decisão e um começo que segura o aluno com marcos + check-ins.\n\nDesbloqueie a prévia do Kit.",
      "headline": "Mais alunos certos. Menos briga de preço.",
      "description": "Ciclo de 8 semanas pronto para usar (prévia).",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    },
    {
      "label": "prova",
      "primaryText": "Os primeiros 30 dias decidem se o aluno fica ou some. Quando ele tem clareza do caminho + vitórias rápidas, a percepção de valor sobe e o engajamento aumenta (boas práticas de onboarding e retenção).\n\nPor isso criamos o método “Agenda Cheia Sem Desconto (8 Semanas)”: um Kit de Comunicação + Onboarding gerado por IA com mensagens prontas, roteiro curto de WhatsApp, marcos de progresso e check-ins.\n\nDesbloqueie a prévia do Kit e veja como aplicar no seu acompanhamento.",
      "headline": "Baseado em boas práticas dos 30 primeiros dias",
      "description": "Prévia do Kit pronto (IA) para captar e reter melhor.",
      "ctaText": "Desbloquear o Kit (receber a prévia gerada por IA)"
    }
  ]
}

Briefing da imagem:
{
  "adImageBriefing": {
    "assetCount": 4,
    "formatPriority": [
      {
        "placement": "Feed",
        "size": "1080x1350",
        "safeMargins": "Manter 10% de margem de segurança (texto e elementos dentro da área central)."
      },
      {
        "placement": "Stories/Reels",
        "size": "1080x1920",
        "safeMargins": "Evitar topo (UI) e base (CTA). Texto principal no terço central."
      }
    ],
    "objective": "Fazer a promessa ser entendida em 2 segundos e gerar clique para ‘Desbloquear o Kit (receber a prévia gerada por IA)’.",
    "singleMindedPromise": "Encha sua agenda em até 8 semanas sem dar desconto.",
    "adToLandingConsistency": {
      "promiseMatch": "Todas as imagens reforçam a mesma promessa do hero da landing: ‘Encha sua agenda em até 8 semanas sem dar desconto’ + motivo (alunos entendem valor e ficam engajados no começo).",
      "ctaMatch": "CTA em todas as variações: ‘Desbloquear a prévia’ (equivalente a ‘Desbloquear o Kit / receber a prévia gerada por IA’).",
      "complianceMatch": "Incluir microcopy quando couber: ‘Sem call. Sem consultoria. Ativos digitais gerados por IA.’"
    },
    "globalDesignSystem": {
      "style": "Limpo, direto e ‘de rotina’ (agenda/semana/check).",
      "colorPalette": {
        "primary": "#0B1F3B",
        "accent": "#22C55E",
        "neutral": "#FFFFFF / cinza claro"
      },
      "typography": {
        "headline": "Sans bold (alta legibilidade)",
        "body": "Sans regular",
        "rules": "Máx. 2 famílias tipográficas. Headline curta (até ~32 caracteres no feed)."
      },
      "iconography": "Ícones simples e sólidos: calendário/agenda, chat, check, documento/roteiro, alvo.",
      "ctaButtonStyle": "Box/botão contrastante (verde ou branco com contorno). Texto: ‘Desbloquear a prévia’.",
      "avoid": [
        "Prometer consultoria, call ou acompanhamento humano",
        "Antes/depois agressivo",
        "Ganhos garantidos",
        "Jargões (funil, lead magnet, etc.)",
        "Texto longo demais (ilegível no mobile)"
      ]
    },
    "brandSafetyAndCompliance": {
      "claims": [
        "Usar ‘em até 8 semanas’ como promessa aspiracional; evitar garantias absolutas.",
        "Não afirmar resultados individuais; manter linguagem de ‘com um processo/kit’.",
        "‘Gerado por IA’ como característica do produto, não como milagre."
      ],
      "finePrintOptional": "*Resultados variam conforme execução e contexto."
    },
    "variants": [
      {
        "id": "V1",
        "name": "8 Semanas sob controle (Timeline)",
        "concept": {
          "idea": "Timeline/checklist de 8 semanas para comunicar processo simples e repetível (sem desconto).",
          "primaryPainToVisualize": "‘Quanto custa?’ cedo + aluno some no começo.",
          "visualMetaphor": "Plano semanal com checks (controle e constância)."
        },
        "layout": {
          "structure": "Topo (promessa) → Meio (timeline 8 semanas) → Rodapé (itens do kit + CTA)",
          "hierarchy": [
            "1) Promessa grande",
            "2) Selo ‘SEM DESCONTO’",
            "3) Timeline ‘Semana 1… Semana 8’",
            "4) 3 bullets do que vem",
            "5) CTA"
          ]
        },
        "onImageCopy": {
          "headline": "Agenda cheia em até 8 semanas",
          "badge": "SEM DESCONTO",
          "subhead": "com alunos chegando entendendo seu valor",
          "bullets": [
            "Mensagens prontas (Whats/DM/e-mail)",
            "Roteiro de pré-venda",
            "Marcos + check-ins (início)"
          ],
          "cta": "Desbloquear a prévia",
          "microcopyOptional": "Sem call. Sem consultoria."
        },
        "visualDirections": {
          "imagery": [
            "Timeline vertical (feed) com 8 marcadores e checks verdes",
            "Ícones pequenos ao lado dos bullets (chat/documento/check)"
          ],
          "background": "Branco ou cinza muito claro; blocos em azul escuro; destaque verde para checks/CTA."
        },
        "productionNotes": {
          "readability": "Manter o subhead em 1 linha (ou 2 curtas). Bullets com 4–6 palavras.",
          "export": [
            "PNG 1080x1350",
            "PNG 1080x1920 (timeline centralizada no terço do meio)"
          ]
        }
      },
      {
        "id": "V2",
        "name": "Quebra de objeção (Sem briga de preço)",
        "concept": {
          "idea": "Contraste direto ‘Preço vs Valor’ para mostrar que o aluno chega mais preparado (sem parecer técnico).",
          "primaryPainToVisualize": "Conversa cai em ‘quanto custa?’ antes do diferencial.",
          "visualMetaphor": "Dois balões de chat: ‘Quanto custa?’ (apagado) vs ‘Entendi como funciona’ (ativo)."
        },
        "layout": {
          "structure": "Topo (promessa) → Meio (visual de chats comparativos) → Rodapé (o que vem + CTA)",
          "hierarchy": [
            "1) Promessa grande",
            "2) Prova visual (chats)",
            "3) ‘SEM DESCONTO’ como selo menor",
            "4) 2–3 bullets",
            "5) CTA"
          ]
        },
        "onImageCopy": {
          "headline": "Mais alunos certos. Menos briga de preço.",
          "subhead": "em até 8 semanas — sem desconto",
          "bullets": [
            "Roteiro pronto de WhatsApp",
            "Mensagens + follow-up",
            "Check-ins do começo"
          ],
          "cta": "Desbloquear a prévia",
          "microcopyOptional": "Ativos digitais gerados por IA"
        },
        "visualDirections": {
          "imagery": [
            "Balão de chat cinza: ‘Quanto custa?’ com ícone de ‘pause’",
            "Balão de chat azul: ‘Entendi o valor e o plano’ com ícone de check",
            "Selo ‘SEM DESCONTO’ em verde"
          ],
          "background": "Gradiente suave do branco para cinza claro; foco nos chats no centro."
        },
        "productionNotes": {
          "readability": "Headline até 38 caracteres. Evitar excesso de texto dentro dos balões.",
          "export": [
            "PNG 1080x1350",
            "PNG 1080x1920 (chats no centro; CTA acima da área do botão nativo)"
          ]
        }
      },
      {
        "id": "V3",
        "name": "Onboarding que segura (Primeiras semanas)",
        "concept": {
          "idea": "Foco em retenção: aluno não some porque sente cuidado + progresso rápido nas primeiras semanas.",
          "primaryPainToVisualize": "Aluno empolga na semana 1 e some na semana 2/3.",
          "visualMetaphor": "Calendário ‘Semana 1–4’ com checks e marcos; seta indicando continuidade até 8 semanas."
        },
        "layout": {
          "structure": "Topo (promessa) → Meio (calendário + marcos) → Rodapé (kit + CTA)",
          "hierarchy": [
            "1) Promessa grande",
            "2) Subhead ‘primeiras semanas’",
            "3) Calendário/marcos",
            "4) 3 itens do kit",
            "5) CTA"
          ]
        },
        "onImageCopy": {
          "headline": "Agenda cheia em até 8 semanas",
          "subhead": "com começo que mantém o aluno engajado",
          "badge": "SEM DESCONTO",
          "bullets": [
            "Marcos de progresso",
            "Check-ins prontos",
            "Mensagens de onboarding"
          ],
          "cta": "Desbloquear a prévia",
          "microcopyOptional": "Sem call. Sem consultoria."
        },
        "visualDirections": {
          "imagery": [
            "Mini calendário (4 semanas) com checks verdes",
            "2–3 ‘marcos’ em tags curtas (ex.: ‘Primeira vitória’, ‘Rotina firme’, ‘Feedback rápido’)"
          ],
          "background": "Branco; blocos em azul escuro; tags em cinza claro; checks em verde."
        },
        "productionNotes": {
          "readability": "Tags de marco com no máximo 2 palavras. Não prometer resultado físico/financeiro.",
          "export": [
            "PNG 1080x1350",
            "PNG 1080x1920"
          ]
        }
      },
      {
        "id": "V4",
        "name": "O que vem na prévia (Kit em 1 tela)",
        "concept": {
          "idea": "Reduz ceticismo mostrando ‘produto’ concreto: lista curta do que a pessoa recebe na prévia.",
          "primaryPainToVisualize": "Dúvida: ‘ok, mas o que eu vou receber?’",
          "visualMetaphor": "Card de checklist ‘Prévia do Kit (IA)’ com 4 itens e ícones."
        },
        "layout": {
          "structure": "Topo (promessa) → Meio (card checklist do kit) → Rodapé (CTA + microcopy)",
          "hierarchy": [
            "1) Promessa grande",
            "2) Card ‘Prévia do Kit’",
            "3) 4 itens (com ícones)",
            "4) CTA",
            "5) Microcopy ‘sem call/consultoria’"
          ]
        },
        "onImageCopy": {
          "headline": "Encha sua agenda em até 8 semanas",
          "badge": "SEM DESCONTO",
          "subhead": "Desbloqueie a prévia do Kit (IA)",
          "bullets": [
            "Mensagens (Whats/DM/e-mail)",
            "Roteiro de pré-venda",
            "Marcos de progresso",
            "Check-ins do início"
          ],
          "cta": "Desbloquear a prévia",
          "microcopyOptional": "Ativos digitais gerados por IA"
        },
        "visualDirections": {
          "imagery": [
            "Card central com borda sutil",
            "Ícones por item (chat/documento/flag/check)"
          ],
          "background": "Azul escuro bem leve (ou branco) com card branco para máximo contraste."
        },
        "productionNotes": {
          "readability": "Manter 4 bullets (não mais). Se necessário, reduzir para 3 no Stories.",
          "export": [
            "PNG 1080x1350",
            "PNG 1080x1920"
          ]
        }
      }
    ]
  }
}

Textos da landing:
{
  "landing-page-copy": {
    "version": "Experiment #10 — Agenda Cheia Sem Desconto (8 Semanas)",
    "primaryPromise": "Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e permanecendo engajados nas primeiras semanas.",
    "ctaPrimary": "Desbloquear o Kit (receber a prévia gerada por IA)",
    "ctaSecondary": "Ver o que vem na prévia",
    "sections": [
      {
        "id": "hero",
        "type": "hero",
        "preheader": "Agenda cheia com constância — sem virar briga de preço",
        "headline": "Encha sua agenda em até 8 semanas sem dar desconto",
        "subheadline": "Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA — para atrair alunos que entendem seu valor e manter engajamento forte nas primeiras semanas.",
        "ctaPrimary": "Desbloquear o Kit (receber a prévia gerada por IA)",
        "ctaSecondary": "Ver o que vem na prévia",
        "microcopy": "Sem call. Sem consultoria. Você recebe ativos digitais prontos (gerados por IA).",
        "trustBullets": [
          "Mensagens prontas (WhatsApp/DM/e-mail)",
          "Roteiro curto para não cair no preço cedo", 
          "Onboarding com marcos + check-ins (8 semanas)"
        ]
      },
      {
        "id": "problem",
        "type": "problemSection",
        "title": "Se todo mês parece uma loteria, não é falta de esforço — é falta de sequência",
        "bullets": [
          "A captação depende de indicação e oscila.",
          "Quando aparece alguém, a conversa cai rápido em ‘quanto custa?’ — antes de você mostrar seu diferencial.",
          "Nas primeiras semanas o aluno ainda não tem hábito: se não sente progresso e cuidado, desengaja e some.",
          "Renovação vira tentativa e erro: alguns ficam, outros somem sem você entender o porquê."
        ],
        "transition": "A saída é um processo simples e repetível: posicionar valor antes do preço + criar vitórias rápidas logo no começo."
      },
      {
        "id": "promise",
        "type": "promiseSection",
        "title": "A promessa central",
        "content": "Em até 8 semanas, você passa a ter um ciclo claro para encher a agenda sem desconto — com alunos chegando mais preparados (entendendo seu valor) e permanecendo engajados nas primeiras semanas por sentirem progresso e cuidado desde o início."
      },
      {
        "id": "mechanism",
        "type": "mechanismSection",
        "title": "Como funciona (sem complicar)",
        "steps": [
          {
            "step": "1",
            "title": "Você responde um briefing rápido",
            "text": "Algumas perguntas simples sobre seu acompanhamento, público e como você trabalha."
          },
          {
            "step": "2",
            "title": "A IA gera sua prévia do Kit de 8 semanas",
            "text": "Você recebe mensagens e roteiros prontos para usar e adaptar (WhatsApp/DM/e-mail), além do plano de começo com marcos e check-ins."
          },
          {
            "step": "3",
            "title": "Você aplica a sequência no seu dia a dia",
            "text": "Usa o roteiro para conduzir conversas sem cair no preço cedo demais e usa o onboarding para criar progresso rápido nas primeiras semanas."
          }
        ],
        "note": "O Kit é o ‘mapa’ e os textos prontos. A execução depende de você e do seu contexto."
      },
      {
        "id": "whats-inside",
        "type": "whatsInsideSection",
        "title": "O que vem na prévia do Kit (gerada por IA)",
        "items": [
          "Mensagens prontas para WhatsApp/DM/e-mail (captação + follow-up)",
          "Roteiro curto de pré-venda para posicionar valor antes do preço",
          "Sequência de onboarding das primeiras semanas (8 semanas)",
          "Marcos de progresso (o que o aluno precisa perceber e quando)",
          "Check-ins prontos (para aumentar percepção de cuidado e reduzir desistência)"
        ],
        "formatNote": "Você recebe em formato copiável (texto), pronto para colar e ajustar."
      },
      {
        "id": "benefits",
        "type": "benefitsSection",
        "title": "O que isso ajuda a destravar",
        "columns": [
          {
            "title": "Antes",
            "bullets": [
              "Conversa vira preço cedo",
              "Aluno entra e desengaja no início",
              "Agenda instável",
              "Renovação imprevisível"
            ]
          },
          {
            "title": "Com o Kit (aplicando a sequência)",
            "bullets": [
              "Mais clareza do seu diferencial antes do preço",
              "Primeiras semanas com ‘vitórias rápidas’",
              "Mais constância no fim do mês",
              "Mais alunos certos e menos desistência"
            ]
          }
        ]
      },
      {
        "id": "proof",
        "type": "proofSection",
        "title": "Por que focar nas primeiras semanas funciona",
        "content": "Os primeiros 30 dias costumam ser decisivos para o aluno criar hábito e perceber valor. Boas práticas de onboarding e retenção indicam que clareza do caminho + vitórias rápidas aumentam engajamento e permanência. Este Kit foi estruturado com esse foco: deixar o valor claro e acelerar a percepção de progresso desde o começo.",
        "source": "Referência: boas práticas de onboarding e retenção (ex.: Glofox)."
      },
      {
        "id": "objections",
        "type": "objectionsSection",
        "title": "Perguntas comuns (sem enrolação)",
        "faqs": [
          {
            "q": "Isso é consultoria ou acompanhamento?",
            "a": "Não. Você recebe ativos digitais gerados por IA (mensagens, roteiros, marcos e check-ins). Não inclui call, gestão humana ou acompanhamento manual."
          },
          {
            "q": "Funciona para qualquer tipo de acompanhamento?",
            "a": "Funciona melhor para serviços recorrentes com alunos/cliente que precisam de orientação e rotina (ex.: treinos, aulas, mentoria prática, acompanhamento profissional). A prévia já ajuda você a ver o encaixe."
          },
          {
            "q": "Vou precisar ‘entender marketing’?",
            "a": "Não. O material é escrito em linguagem de rotina (agenda, alunos, preço, começo). Você só aplica a sequência e adapta termos ao seu jeito."
          },
          {
            "q": "Em quanto tempo vou ver resultado?",
            "a": "A proposta é um ciclo de até 8 semanas. Resultados variam conforme sua execução, oferta e contexto."
          },
          {
            "q": "O que significa ‘gerado por IA’?",
            "a": "Significa que os textos e a estrutura do Kit são gerados automaticamente a partir do seu briefing, para você ganhar velocidade e consistência."
          }
        ]
      },
      {
        "id": "cta",
        "type": "ctaSection",
        "title": "Desbloqueie a prévia do Kit agora",
        "subtitle": "Veja como fica um ciclo de 8 semanas pronto para captar melhor e segurar o aluno no começo — sem depender de indicação e sem desconto.",
        "ctaPrimary": "Desbloquear o Kit (receber a prévia gerada por IA)",
        "microcopy": "*Resultados variam conforme execução e contexto."
      },
      {
        "id": "footer",
        "type": "footer",
        "compliance": [
          "Sem garantias de resultado.",
          "Sem consultoria/call/gestão humana.",
          "Entrega de ativos digitais gerados por IA."
        ]
      }
    ],
    "recommendedQuantity": 4
  }
}

Instruções extras do usuário:
Quantidade sugerida: 2

Responda exclusivamente em JSON válido no formato {"content":"..."}.
```

**Resultado**
```text
{"output_text":null,"id":"resp_08a88c25707934120069cd7bd463d48196b7c7938f07b9811f","output":[{"tool_call":null,"tool_calls":null,"id":"msg_08a88c25707934120069cd7bd4ee3481969b6a432c60e14190","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"content\":\"{\\n  \\\"landingPageWireframe\\\": {\\n    \\\"experiment\\\": \\\"#10 — Agenda Cheia Sem Desconto (8 Semanas)\\\",\\n    \\\"primaryPromise\\\": \\\"Encha sua agenda em até 8 semanas sem dar desconto — com alunos chegando já entendendo o valor do seu acompanhamento e permanecendo engajados nas primeiras semanas.\\\",\\n    \\\"primaryCTA\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\",\\n    \\\"secondaryCTA\\\": \\\"Ver o que vem na prévia\\\",\\n    \\\"pageGoal\\\": \\\"Gerar conversão para receber a prévia do Kit (ativos digitais gerados por IA), mantendo consistência com o anúncio e reduzindo ceticismo.\\\",\\n    \\\"globalNotes\\\": {\\n      \\\"promiseAdConsistency\\\": \\\"Hero repete a promessa central do anúncio e mantém ‘em até 8 semanas’ + ‘sem desconto’.\\\",\\n      \\\"ctaConsistency\\\": \\\"CTA principal igual ao anúncio: ‘Desbloquear o Kit (receber a prévia gerada por IA)’.\\\",\\n      \\\"compliance\\\": [\\n        \\\"Não prometer consultoria, call, gestão humana ou acompanhamento manual.\\\",\\n        \\\"Sem garantias absolutas; usar linguagem aspiracional (‘em até 8 semanas’).\\\",\\n        \\\"Deixar claro: entrega = ativos digitais gerados por IA.\\\"\\n      ]\\n    },\\n    \\\"wireframe\\\": [\\n      {\\n        \\\"id\\\": \\\"header\\\",\\n        \\\"type\\\": \\\"header\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 2,\\n          \\\"left\\\": \\\"Logo\\\",\\n          \\\"right\\\": \\\"CTA mini + âncoras\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"logo\\\": \\\"Marca\\\",\\n          \\\"navLinks\\\": [\\n            {\\n              \\\"label\\\": \\\"Como funciona\\\",\\n              \\\"anchor\\\": \\\"#como-funciona\\\"\\n            },\\n            {\\n              \\\"label\\\": \\\"O que vem\\\",\\n              \\\"anchor\\\": \\\"#o-que-voce-recebe\\\"\\n            },\\n            {\\n              \\\"label\\\": \\\"Perguntas\\\",\\n              \\\"anchor\\\": \\\"#faq\\\"\\n            }\\n          ],\\n          \\\"ctaMini\\\": {\\n            \\\"label\\\": \\\"Desbloquear a prévia\\\",\\n            \\\"action\\\": \\\"scrollToForm\\\",\\n            \\\"target\\\": \\\"#form\\\"\\n          }\\n        },\\n        \\\"notes\\\": \\\"Header simples, sem distrações. CTA mini fixo no mobile (opcional).\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"hero\\\",\\n        \\\"type\\\": \\\"hero\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 2,\\n          \\\"left\\\": \\\"Copy + CTAs + bullets\\\",\\n          \\\"right\\\": \\\"Mockup do Kit + Form (acima da dobra no desktop)\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"preheader\\\": \\\"Agenda cheia com constância — sem virar briga de preço\\\",\\n          \\\"headline\\\": \\\"Encha sua agenda em até 8 semanas sem dar desconto\\\",\\n          \\\"subheadline\\\": \\\"Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA — para atrair alunos que entendem seu valor e manter engajamento forte nas primeiras semanas.\\\",\\n          \\\"ctaPrimary\\\": {\\n            \\\"label\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\",\\n            \\\"action\\\": \\\"submitForm\\\",\\n            \\\"target\\\": \\\"#form\\\"\\n          },\\n          \\\"ctaSecondary\\\": {\\n            \\\"label\\\": \\\"Ver o que vem na prévia\\\",\\n            \\\"action\\\": \\\"scroll\\\",\\n            \\\"target\\\": \\\"#o-que-voce-recebe\\\"\\n          },\\n          \\\"microcopy\\\": \\\"Sem call. Sem consultoria. Você recebe ativos digitais prontos (gerados por IA).\\\",\\n          \\\"trustBullets\\\": [\\n            \\\"Mensagens prontas (WhatsApp/DM/e-mail)\\\",\\n            \\\"Roteiro curto para não cair no preço cedo\\\",\\n            \\\"Onboarding com marcos + check-ins (8 semanas)\\\"\\n          ],\\n          \\\"visual\\\": {\\n            \\\"type\\\": \\\"mockup\\\",\\n            \\\"description\\\": \\\"Mockup (imagem) de um documento/kit com: ‘Mensagens’, ‘Roteiro’, ‘Marcos’, ‘Check-ins’.\\\"\\n          },\\n          \\\"form\\\": {\\n            \\\"id\\\": \\\"form\\\",\\n            \\\"title\\\": \\\"Receber a prévia agora\\\",\\n            \\\"fields\\\": [\\n              {\\n                \\\"name\\\": \\\"firstName\\\",\\n                \\\"label\\\": \\\"Seu nome\\\",\\n                \\\"type\\\": \\\"text\\\",\\n                \\\"required\\\": true\\n              },\\n              {\\n                \\\"name\\\": \\\"email\\\",\\n                \\\"label\\\": \\\"Seu e-mail\\\",\\n                \\\"type\\\": \\\"email\\\",\\n                \\\"required\\\": true\\n              },\\n              {\\n                \\\"name\\\": \\\"segment\\\",\\n                \\\"label\\\": \\\"Seu tipo de acompanhamento\\\",\\n                \\\"type\\\": \\\"select\\\",\\n                \\\"required\\\": true,\\n                \\\"options\\\": [\\n                  \\\"Personal/treino\\\",\\n                  \\\"Nutrição\\\",\\n                  \\\"Aulas/estúdio\\\",\\n                  \\\"Beleza/estética\\\",\\n                  \\\"Consultoria prática recorrente\\\",\\n                  \\\"Outro\\\"\\n                ]\\n              }\\n            ],\\n            \\\"submitButton\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\",\\n            \\\"privacyNote\\\": \\\"Ao enviar, você concorda em receber a prévia por e-mail. Você pode sair quando quiser.\\\",\\n            \\\"reassuranceLine\\\": \\\"Entrega digital. Sem call.\\\"\\n          }\\n        },\\n        \\\"notes\\\": \\\"No mobile: headline + CTA + 3 bullets primeiro; form logo abaixo.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"problem\\\",\\n        \\\"type\\\": \\\"problemSection\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 1,\\n          \\\"style\\\": \\\"Bloco com ícones simples (alerta/chat/calendário)\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"title\\\": \\\"Se todo mês parece uma loteria, não é falta de esforço — é falta de sequência\\\",\\n          \\\"bullets\\\": [\\n            \\\"A captação depende de indicação e oscila.\\\",\\n            \\\"Quando aparece alguém, a conversa cai rápido em ‘quanto custa?’ — antes de você mostrar seu diferencial.\\\",\\n            \\\"Nas primeiras semanas o aluno ainda não tem hábito: se não sente progresso e cuidado, desengaja e some.\\\",\\n            \\\"Renovação vira tentativa e erro: alguns ficam, outros somem sem você entender o porquê.\\\"\\n          ],\\n          \\\"transition\\\": \\\"A saída é um processo simples e repetível: posicionar valor antes do preço + criar vitórias rápidas logo no começo.\\\"\\n        }\\n      },\\n      {\\n        \\\"id\\\": \\\"promise\\\",\\n        \\\"type\\\": \\\"promiseSection\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 1,\\n          \\\"style\\\": \\\"Faixa destacada (fundo leve)\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"title\\\": \\\"A promessa central\\\",\\n          \\\"content\\\": \\\"Em até 8 semanas, você passa a ter um ciclo claro para encher a agenda sem desconto — com alunos chegando mais preparados (entendendo seu valor) e permanecendo engajados nas primeiras semanas por sentirem progresso e cuidado desde o início.\\\",\\n          \\\"supportingPoints\\\": [\\n            \\\"Menos conversa travada em preço\\\",\\n            \\\"Mais alunos certos\\\",\\n            \\\"Começo com percepção rápida de valor\\\"\\n          ]\\n        },\\n        \\\"notes\\\": \\\"Manter a promessa como destaque (não ‘IA’). IA aparece como ‘como’ mais abaixo.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"como-funciona\\\",\\n        \\\"type\\\": \\\"mechanismSection\\\",\\n        \\\"anchor\\\": \\\"#como-funciona\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 3,\\n          \\\"style\\\": \\\"Cards numerados 1–3\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"title\\\": \\\"Como funciona (sem complicar)\\\",\\n          \\\"steps\\\": [\\n            {\\n              \\\"step\\\": \\\"1\\\",\\n              \\\"title\\\": \\\"Você responde um briefing rápido\\\",\\n              \\\"text\\\": \\\"Algumas perguntas simples sobre seu acompanhamento, público e como você trabalha.\\\"\\n            },\\n            {\\n              \\\"step\\\": \\\"2\\\",\\n              \\\"title\\\": \\\"A IA gera sua prévia do Kit de 8 semanas\\\",\\n              \\\"text\\\": \\\"Mensagens e roteiros prontos (WhatsApp/DM/e-mail) + plano de começo com marcos e check-ins.\\\"\\n            },\\n            {\\n              \\\"step\\\": \\\"3\\\",\\n              \\\"title\\\": \\\"Você aplica a sequência no dia a dia\\\",\\n              \\\"text\\\": \\\"Conduz conversas sem cair no preço cedo demais e cria progresso rápido nas primeiras semanas.\\\"\\n            }\\n          ],\\n          \\\"note\\\": \\\"O Kit é o ‘mapa’ e os textos prontos. A execução depende de você e do seu contexto.\\\"\\n        }\\n      },\\n      {\\n        \\\"id\\\": \\\"o-que-voce-recebe\\\",\\n        \\\"type\\\": \\\"whatsInsideSection\\\",\\n        \\\"anchor\\\": \\\"#o-que-voce-recebe\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 2,\\n          \\\"left\\\": \\\"Lista do que vem\\\",\\n          \\\"right\\\": \\\"Preview visual (checklist)\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"title\\\": \\\"O que vem na prévia do Kit (gerada por IA)\\\",\\n          \\\"items\\\": [\\n            \\\"Mensagens prontas para WhatsApp/DM/e-mail (captação + follow-up)\\\",\\n            \\\"Roteiro curto de pré-venda para posicionar valor antes do preço\\\",\\n            \\\"Sequência de onboarding das primeiras semanas (8 semanas)\\\",\\n            \\\"Marcos de progresso (o que o aluno precisa perceber e quando)\\\",\\n            \\\"Check-ins prontos (para aumentar percepção de cuidado e reduzir desistência)\\\"\\n          ],\\n          \\\"formatNote\\\": \\\"Você recebe em formato copiável (texto), pronto para colar e ajustar.\\\",\\n          \\\"ctaInline\\\": {\\n            \\\"label\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\",\\n            \\\"action\\\": \\\"scrollToForm\\\",\\n            \\\"target\\\": \\\"#form\\\"\\n          }\\n        },\\n        \\\"notes\\\": \\\"Aqui entra a concretude do ‘produto’ para reduzir ceticismo.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"benefits\\\",\\n        \\\"type\\\": \\\"benefitsSection\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 2,\\n          \\\"style\\\": \\\"Tabela/colunas ‘Antes vs Com o Kit’\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"title\\\": \\\"O que isso ajuda a destravar\\\",\\n          \\\"columns\\\": [\\n            {\\n              \\\"title\\\": \\\"Antes\\\",\\n              \\\"bullets\\\": [\\n                \\\"Conversa vira preço cedo\\\",\\n                \\\"Aluno entra e desengaja no início\\\",\\n                \\\"Agenda instável\\\",\\n                \\\"Renovação imprevisível\\\"\\n              ]\\n            },\\n            {\\n              \\\"title\\\": \\\"Com o Kit (aplicando a sequência)\\\",\\n              \\\"bullets\\\": [\\n                \\\"Mais clareza do seu diferencial antes do preço\\\",\\n                \\\"Primeiras semanas com ‘vitórias rápidas’\\\",\\n                \\\"Mais constância no fim do mês\\\",\\n                \\\"Mais alunos certos e menos desistência\\\"\\n              ]\\n            }\\n          ]\\n        }\\n      },\\n      {\\n        \\\"id\\\": \\\"proof\\\",\\n        \\\"type\\\": \\\"proofSection\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 1,\\n          \\\"style\\\": \\\"Bloco com citação + fonte\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"title\\\": \\\"Por que focar nas primeiras semanas funciona\\\",\\n          \\\"content\\\": \\\"Os primeiros 30 dias costumam ser decisivos para o aluno criar hábito e perceber valor. Boas práticas de onboarding e retenção indicam que clareza do caminho + vitórias rápidas aumentam engajamento e permanência. Este Kit foi estruturado com esse foco: deixar o valor claro e acelerar a percepção de progresso desde o começo.\\\",\\n          \\\"source\\\": \\\"Referência: boas práticas de onboarding e retenção (ex.: Glofox).\\\"\\n        },\\n        \\\"notes\\\": \\\"Prova leve (plausibilidade), sem números inventados.\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"faq\\\",\\n        \\\"type\\\": \\\"objectionsSection\\\",\\n        \\\"anchor\\\": \\\"#faq\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 1,\\n          \\\"style\\\": \\\"Accordion\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"title\\\": \\\"Perguntas comuns (sem enrolação)\\\",\\n          \\\"faqs\\\": [\\n            {\\n              \\\"q\\\": \\\"Isso é consultoria ou acompanhamento?\\\",\\n              \\\"a\\\": \\\"Não. Você recebe ativos digitais gerados por IA (mensagens, roteiros, marcos e check-ins). Não inclui call, gestão humana ou acompanhamento manual.\\\"\\n            },\\n            {\\n              \\\"q\\\": \\\"Funciona para qualquer tipo de acompanhamento?\\\",\\n              \\\"a\\\": \\\"Funciona melhor para serviços recorrentes com alunos/cliente que precisam de orientação e rotina (ex.: treinos, aulas, mentoria prática, acompanhamento profissional). A prévia já ajuda você a ver o encaixe.\\\"\\n            },\\n            {\\n              \\\"q\\\": \\\"Vou precisar ‘entender marketing’?\\\",\\n              \\\"a\\\": \\\"Não. O material é escrito em linguagem de rotina (agenda, alunos, preço, começo). Você só aplica a sequência e adapta termos ao seu jeito.\\\"\\n            },\\n            {\\n              \\\"q\\\": \\\"Em quanto tempo vou ver resultado?\\\",\\n              \\\"a\\\": \\\"A proposta é um ciclo de até 8 semanas. Resultados variam conforme sua execução, oferta e contexto.\\\"\\n            },\\n            {\\n              \\\"q\\\": \\\"O que significa ‘gerado por IA’?\\\",\\n              \\\"a\\\": \\\"Significa que os textos e a estrutura do Kit são gerados automaticamente a partir do seu briefing, para você ganhar velocidade e consistência.\\\"\\n            }\\n          ]\\n        }\\n      },\\n      {\\n        \\\"id\\\": \\\"cta-final\\\",\\n        \\\"type\\\": \\\"ctaSection\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 2,\\n          \\\"left\\\": \\\"Reforço da promessa + bullets\\\",\\n          \\\"right\\\": \\\"CTA grande (scroll/repete form)\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"title\\\": \\\"Desbloqueie a prévia do Kit agora\\\",\\n          \\\"subtitle\\\": \\\"Veja como fica um ciclo de 8 semanas pronto para captar melhor e segurar o aluno no começo — sem depender de indicação e sem desconto.\\\",\\n          \\\"bullets\\\": [\\n            \\\"Roteiro para posicionar valor antes do preço\\\",\\n            \\\"Mensagens prontas para o dia a dia\\\",\\n            \\\"Marcos + check-ins para o começo\\\"\\n          ],\\n          \\\"ctaPrimary\\\": {\\n            \\\"label\\\": \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\",\\n            \\\"action\\\": \\\"scrollToForm\\\",\\n            \\\"target\\\": \\\"#form\\\"\\n          },\\n          \\\"microcopy\\\": \\\"*Resultados variam conforme execução e contexto.\\\"\\n        }\\n      },\\n      {\\n        \\\"id\\\": \\\"footer\\\",\\n        \\\"type\\\": \\\"footer\\\",\\n        \\\"layout\\\": {\\n          \\\"columns\\\": 1,\\n          \\\"style\\\": \\\"Minimal\\\"\\n        },\\n        \\\"elements\\\": {\\n          \\\"compliance\\\": [\\n            \\\"Sem garantias de resultado.\\\",\\n            \\\"Sem consultoria/call/gestão humana.\\\",\\n            \\\"Entrega de ativos digitais gerados por IA.\\\"\\n          ],\\n          \\\"links\\\": [\\n            \\\"Política de privacidade\\\",\\n            \\\"Termos\\\"\\n          ]\\n        }\\n      }\\n    ],\\n    \\\"conversionSupport\\\": {\\n      \\\"aboveTheFold\\\": {\\n        \\\"mustHave\\\": [\\n          \\\"Promessa (8 semanas, sem desconto)\\\",\\n          \\\"Especificação do que é (prévia do Kit gerado por IA)\\\",\\n          \\\"Microcopy de compliance (sem call/consultoria)\\\",\\n          \\\"3 bullets de valor\\\",\\n          \\\"Form simples\\\"\\n        ]\\n      },\\n      \\\"formStrategy\\\": {\\n        \\\"fieldsMax\\\": 3,\\n        \\\"frictionNotes\\\": \\\"Evitar pedir telefone no primeiro passo para manter escala e reduzir atrito.\\\"\\n      }\\n    }\\n  },\\n  \\\"quantitySuggested\\\": 2,\\n  \\\"variants\\\": [\\n    {\\n      \\\"id\\\": \\\"LP-A\\\",\\n      \\\"name\\\": \\\"Form no Hero + Prova ‘30 dias’ cedo\\\",\\n      \\\"keyDifference\\\": \\\"Form acima da dobra + seção de prova logo após ‘Como funciona’ (reduz ceticismo cedo).\\\",\\n      \\\"recommendedFor\\\": \\\"Tráfego frio com objeção alta (medo de ‘curso/consultoria’).\\\"\\n    },\\n    {\\n      \\\"id\\\": \\\"LP-B\\\",\\n      \\\"name\\\": \\\"Hero curto + ‘O que vem na prévia’ cedo\\\",\\n      \\\"keyDifference\\\": \\\"Após o hero, entra direto ‘O que vem na prévia’ (produto concreto primeiro), depois problema/mecanismo.\\\",\\n      \\\"recommendedFor\\\": \\\"Público que pergunta ‘o que eu vou receber?’ e precisa ver o tangível antes de ler a história.\\\"\\n    }\\n  ]\\n}\"}"}]}],"usage":{"input_tokens":5721,"output_tokens":3980,"prompt_tokens":null,"completion_tokens":null,"total_tokens":9701},"error":null,"status":"completed"}
```

### 9. Layout da Landing
- Data: 02/04, 15:55
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Persona: teste
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))
Metadados obrigatórios do experimento:
- primary_variable: Dor vs Resultado
- variant_id: variant-10
- stage: AD
- control_or_treatment: treatment
- asset_role: landing-page-wireframe

Tarefa alvo: landing-page-wireframe

Ângulo da campanha:
{"experimentMetadata":{"variant_id":"variant-10","asset_role":"campaign-angle","stage":"AD","primary_variable":"Dor vs Resultado","control_or_treatment":"treatment"},"campaignAngle":{"primaryPain":"Você até consegue alunos, mas não tem um processo simples e repetível: a conversa cai rápido em “quanto custa?”, o aluno não percebe valor nas primeiras semanas e o engajamento despenca — aí a agenda vira loteria de indicação e de preço.","singleMindedPromise":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","proofSummary":"Estrutura baseada em boas práticas de onboarding e primeiros 30 dias para aumentar percepção de valor e retenção, com marcos iniciais claros e mensagens que antecipam e reforçam o “porquê” do seu acompanhamento.","funnelStage":"Frio → Morno (Topo/Meio)","landingMatchLine":"Agenda cheia sem desconto em 8 semanas: sequência pronta para o aluno entender seu valor e engajar nas primeiras semanas.","primaryCTA":"Desbloquear o kit","mechanismSummary":"Você recebe um kit de ativos digitais gerados por IA (sequência de mensagens + marcos das primeiras semanas + check-ins) para aplicar um processo de entrada e primeiras vitórias que reduz a briga por preço e aumenta engajamento.","tone":"Direto, claro e orientado a resultado, sem prometer “milagre” e sem jargão.","cta":"Desbloquear o kit","primaryPromise":"Agenda cheia sem desconto em até 8 semanas, com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas."}}

Texto do anúncio:
{"adCopy":{"primaryTextVariants":[{"label":"dor","openingHookType":"dor","headline":"Agenda cheia sem desconto (8 semanas)","placementHint":"feed","lengthVariants":{"curta":"A conversa com aluno vira “quanto custa?” e some na 2ª semana?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos entendendo seu valor e ficando engajados no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"A conversa vira “quanto custa?” cedo demais. O aluno entra empolgado… e some nas primeiras semanas. Aí sua agenda vira loteria de indicação e de preço.\n\nA ideia aqui é ter constância: em até 8 semanas, encher sua agenda sem dar desconto — com alunos chegando entendendo o valor do seu acompanhamento e permanecendo engajados no começo.\n\nPara isso, você desbloqueia uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto pra conduzir a conversa sem cair no preço cedo\n• marcos de progresso + check-ins das primeiras semanas\n\nSem call. Sem consultoria. Só ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"A conversa cai em “quanto custa?” e o aluno desengaja no começo?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e sentindo progresso logo nas primeiras semanas.\n\nDesbloqueie a prévia do Kit gerado por IA com mensagens prontas, roteiro de conversa e check-ins de onboarding.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA): mensagens + roteiro + check-ins.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"resultado","openingHookType":"resultado","headline":"Mais alunos certos. Menos preço.","placementHint":"stories/reels","lengthVariants":{"curta":"Agenda cheia em até 8 semanas — sem dar desconto.\n\nAlunos chegam entendendo seu valor e ficam engajados nas primeiras semanas.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Agenda cheia em até 8 semanas, sem dar desconto.\n\nA diferença não é “fazer mais”. É ter um ciclo simples para:\n1) o aluno entender seu valor antes de falar de preço\n2) o aluno sentir cuidado + progresso rápido nas primeiras semanas (quando mais desiste)\n\nDesbloqueie uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• convite e mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto de conversa (sem cair no preço cedo)\n• marcos iniciais + check-ins para segurar o engajamento\n\nSem call. Sem consultoria. Ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Em até 8 semanas, encha sua agenda sem dar desconto.\n\nCom uma sequência pronta, o aluno chega mais preparado (entende seu valor) e o começo já tem marcos + check-ins pra manter engajamento.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA) para captar e reter melhor.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"prova","openingHookType":"prova","headline":"Primeiros 30 dias: faz diferença","placementHint":"feed","lengthVariants":{"curta":"Os primeiros 30 dias costumam decidir se o aluno engaja ou some.\n\nEm até 8 semanas, encha sua agenda sem desconto com uma sequência que deixa seu valor claro + cria vitórias rápidas no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Os primeiros 30 dias costumam ser decisivos: quando o aluno tem clareza do caminho + pequenas vitórias logo no começo, a percepção de valor sobe e o engajamento tende a aumentar (boas práticas de onboarding e retenção).\n\nCom isso em mente, a promessa é simples: em até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas.\n\nDesbloqueie a prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto para conduzir a conversa antes do preço\n• marcos iniciais + check-ins para o aluno sentir progresso e cuidado\n\nSem call. Sem consultoria. Apenas ativos digitais.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Boas práticas de onboarding mostram: clareza + vitórias rápidas no começo aumentam a percepção de valor.\n\nEm até 8 semanas, encha sua agenda sem dar desconto com uma sequência pronta de comunicação + marcos + check-ins.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Estrutura com foco nos 30 primeiros dias (prévia).","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-copy"}}

Briefing da imagem:
{"adImageBriefing":{"briefings":[{"safeMargins":"Feed 1080x1350: manter 10% de margem de segurança (texto e elementos dentro da área central). Stories 1080x1920: evitar topo (UI) e base (CTA nativo); texto principal no terço central.","formatByPlacement":"Priorizar Feed (1080x1350) com leitura instantânea; adaptar para Stories/Reels (1080x1920) centralizando a metáfora visual e movendo CTA para acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação DOR do anúncio: dor = conversa vira “quanto custa?” cedo + aluno some nas primeiras semanas. Ainda assim, manter a promessa central igual à landing: agenda cheia sem desconto em até 8 semanas. CTA igual à landing: “Desbloquear a prévia/kit”.","imageTextMaxWords":9,"assetType":"estatico","hierarchy":"1) Dor (gatilho rápido) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy de compliance (se couber)","mustMatchAdVariant":"dor","visualAngle":"dor","complianceNotes":"Evitar garantias absolutas e resultados individuais. Não mencionar consultoria/call/gestão humana como entrega. “Gerado por IA” como característica, não milagre. Se usar “em até 8 semanas”, tratar como objetivo/estrutura (aspiracional). Microcopy opcional: “Sem call. Sem consultoria. Ativos digitais (IA).”","visualBriefing":"Conceito: ‘A conversa cai no preço + sumiço na semana 2’. Visual principal em split/contraste com dois balões de chat grandes no centro.\n\nComposição (Feed):\n- Fundo branco/cinza muito claro.\n- Metade esquerda (apagada/cinza): balão de chat com texto curto ‘Quanto custa?’ + ícone de pausa (||) ou ponto de interrogação.\n- Metade direita (ativa/azul escuro #0B1F3B): balão de chat com texto curto ‘Entendi como funciona’ + ícone de check verde #22C55E.\n- No topo, headline curta (máx. 1 linha): ‘Agenda cheia sem desconto’ (ou ‘Chega de briga de preço’ se couber sem perder promessa; manter promessa explícita em selo/linha 2).\n- Selo pequeno em verde: ‘8 SEMANAS’.\n- Rodapé com botão/box contrastante: ‘Desbloquear a prévia’.\n- Microcopy pequeno (se couber, 1 linha): ‘Ativos digitais gerados por IA’.\n\nTexto sugerido (<=9 palavras totais sobre imagem, priorizando):\n- Linha 1: ‘Agenda cheia sem desconto’ (4)\n- Selo: ‘em até 8 semanas’ (4) — se faltar espaço, usar apenas ‘8 semanas’ (2)\n- Botão: ‘Desbloquear a prévia’ (3)\n(Obs.: escolher combinações para respeitar o limite de palavras; preferir ‘Agenda cheia sem desconto’ + ‘8 semanas’ + ‘Desbloquear a prévia’).\n\nÍcones: chat, pause, check.\n\nAdaptação Stories: chats no terço central; headline acima; botão ‘Desbloquear a prévia’ logo abaixo dos chats (antes da área do CTA nativo)."},{"safeMargins":"Stories/Reels 1080x1920: texto e elementos críticos no terço central; evitar topo (UI) e base (CTA nativo). Feed 1080x1350: 10% de margem de segurança em todos os lados.","formatByPlacement":"Priorizar Stories/Reels (1080x1920) com layout vertical e timeline central; adaptar para Feed (1080x1350) mantendo a timeline com 3 marcos visuais e um selo ‘8 semanas’.","messageMatchNotes":"Deve bater com a variação RESULTADO do anúncio: ‘Agenda cheia em até 8 semanas — sem dar desconto’ + ‘alunos chegam entendendo seu valor e ficam engajados no começo’. Manter a mesma promessa do hero da landing e CTA de desbloqueio da prévia do kit.","imageTextMaxWords":10,"assetType":"story-vertical","hierarchy":"1) Resultado (agenda cheia sem desconto) 2) Prazo (8 semanas) 3) Benefício (alunos entendem valor + engajam) 4) CTA 5) Microcopy (IA / sem consultoria)","mustMatchAdVariant":"resultado","visualAngle":"resultado","complianceNotes":"Não prometer ‘agenda cheia garantida’. Usar linguagem aspiracional e de processo (‘com uma sequência pronta’). Não usar termos técnicos (funil/lead). Sem consultoria/call. Incluir ‘Ativos digitais (IA)’ quando possível.","visualBriefing":"Conceito: ‘8 semanas sob controle’ (timeline simples) para passar constância e previsibilidade.\n\nComposição (Stories):\n- Fundo branco com blocos/linhas finas em azul escuro (#0B1F3B) como moldura.\n- Centro: timeline vertical com 8 pontos (Sem 1 a Sem 8) e checks verdes (#22C55E) nos primeiros 3–4 para indicar progresso.\n- Ao lado de 3 pontos, mini-ícones:\n  - Sem 1: ícone chat (mensagens)\n  - Sem 2: ícone documento (roteiro)\n  - Sem 3: ícone check (check-ins)\n- Topo (texto grande): ‘Agenda cheia sem desconto’.\n- Logo abaixo (texto menor): ‘em até 8 semanas’.\n- Linha curta de benefício (se couber): ‘alunos entendem seu valor’.\n- Botão/box no terço inferior (mas acima da área do CTA nativo): ‘Desbloquear a prévia’.\n- Microcopy pequeno: ‘Ativos digitais (IA)’.\n\nTexto sugerido (<=10 palavras totais sobre imagem):\n- ‘Agenda cheia sem desconto’ (4)\n- ‘em até 8 semanas’ (4)\n- ‘Desbloquear a prévia’ (3)\n(Escolher 2 das 3 linhas conforme espaço para respeitar o limite; manter sempre promessa + CTA; o ‘8 semanas’ pode ir no selo).\n\nAdaptação Feed: timeline horizontal/compacta (8 marcadores) no meio; selo ‘8 semanas’; CTA no rodapé."},{"safeMargins":"Feed 1080x1350: 10% de margem de segurança. Stories 1080x1920: manter conteúdo principal no terço central e deixar respiro no topo/base para UI e CTA nativo.","formatByPlacement":"Priorizar Feed (1080x1350) com ‘card de prova’ + referência aos primeiros 30 dias; adaptar para Stories (1080x1920) mantendo o card central grande e CTA acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação PROVA do anúncio: ‘Primeiros 30 dias fazem diferença’ (boas práticas de onboarding) e conectar com a promessa de ‘agenda cheia sem desconto em até 8 semanas’. CTA deve ser ‘Desbloquear a prévia’ (equivalente ao ‘Desbloquear o Kit’ da landing).","imageTextMaxWords":11,"assetType":"carrossel","hierarchy":"1) Prova (primeiros 30 dias) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy (sem consultoria)","mustMatchAdVariant":"prova","visualAngle":"prova","complianceNotes":"Evitar ‘comprovado’/‘garantido’. Tratar como boas práticas/referência. Não usar logos de terceiros. Não prometer acompanhamento humano. Incluir ‘Ativos digitais gerados por IA’ como característica, não como promessa.","visualBriefing":"Formato: Carrossel 3 cards (1080x1350) — cada card com pouquíssimo texto e ícones claros.\n\nCard 1 (PROVA):\n- Headline: ‘Primeiros 30 dias’\n- Sub (curto): ‘decidem o engajamento’\n- Visual: mini-calendário 30 dias com 4 checks verdes (semanas) e uma seta apontando ‘continuidade’.\n\nCard 2 (CONEXÃO COM PROMESSA):\n- Headline: ‘8 semanas’\n- Badge: ‘SEM DESCONTO’\n- Sub (curto): ‘sequência pronta’\n- Visual: trilho/linha do tempo com 8 pontos + 2 ícones (chat + check).\n\nCard 3 (AÇÃO/PRODUTO):\n- Headline: ‘Desbloqueie a prévia’\n- Sub (curto): ‘mensagens + check-ins’\n- Micro (pequeno): ‘Ativos digitais (IA). Sem consultoria.’\n- Botão/box: ‘Desbloquear a prévia’.\n\nPaleta e estilo:\n- Fundo branco/cinza claro; textos em azul escuro (#0B1F3B); destaques em verde (#22C55E).\n- Tipografia sans bold para headline.\n\nControle de texto (<=11 palavras por card):\n- Card 1 exemplo: ‘Primeiros 30 dias decidem o engajamento’ (6)\n- Card 2 exemplo: ‘8 semanas. Sem desconto. Sequência pronta.’ (6)\n- Card 3 exemplo: ‘Desbloquear a prévia’ (3) + ‘Ativos digitais (IA)’ (3) (ajustar para não estourar).\n\nObservação: manter a promessa central explícita no Card 2 para garantir consistência com a landing."}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-image-briefing"}}

Textos da landing:
{"experimentMetadata":{"control_or_treatment":"treatment","variant_id":"variant-10","asset_role":"landing-page-copy","stage":"AD","primary_variable":"Dor vs Resultado"},"landingPageCopy":{}}

Instruções extras do usuário:
Quantidade sugerida: 2

Responda exclusivamente em JSON válido e siga estritamente o schema da seção.
```

**Resultado**
```text
{"output_text":null,"id":"resp_05f70d88063f41ca0069cebb898af0819584e7d93368017237","output":[{"tool_call":null,"tool_calls":null,"id":"msg_05f70d88063f41ca0069cebb8a2db8819583d26f7557852550","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"landingPageWireframe\":{},\"experimentMetadata\":{\"control_or_treatment\":\"treatment\",\"variant_id\":\"variant-10\",\"asset_role\":\"landing-page-wireframe\",\"stage\":\"AD\",\"primary_variable\":\"Dor vs Resultado\"}}"}]}],"usage":{"input_tokens":4652,"output_tokens":58,"prompt_tokens":null,"completion_tokens":null,"total_tokens":4710},"error":null,"status":"completed"}
```

### 10. Layout da Landing
- Data: 02/04, 18:57
- Modelo: gpt-5.2

**Prompt**
```text
Experimento #10
Hipótese resumida: Agenda Cheia Sem Desconto (8 Semanas)
Título da hipótese: Agenda Cheia Sem Desconto (8 Semanas)
Problema: Falta um processo simples e repetível que funcione todo mês (sem depender de indicação) para: (1) deixar claro, antes da conversa virar ‘quanto custa?’, por que seu acompanhamento é diferente e vale o investimento; e (2) fazer o aluno sentir progresso e cuidado logo nas primeiras semanas, quando ele ainda não tem hábito e é mais fácil desistir. Sem uma sequência clara de comunicação + primeiros marcos de evolução + check-ins, o aluno não percebe valor rápido, a conversa vira briga de preço e a renovação vira loteria.
Promessa: Ter uma agenda cheia com constância e tranquilidade no fim do mês, porque os alunos certos chegam entendendo o valor do seu acompanhamento (não só perguntando preço) e, principalmente, continuam engajados nas primeiras semanas por sentirem cuidado e progresso rápido — deixando de ser uma loteria de indicação e virando um negócio sob controle. ([glofox.com](https://www.glofox.com/blog/member-onboarding-first-30-days/?utm_source=openai))
Metadados obrigatórios do experimento:
- primary_variable: Dor vs Resultado
- variant_id: variant-10
- stage: AD
- control_or_treatment: treatment
- asset_role: landing-page-wireframe

Tarefa alvo: landing-page-wireframe

Ângulo da campanha:
{"experimentMetadata":{"variant_id":"variant-10","asset_role":"campaign-angle","stage":"AD","primary_variable":"Dor vs Resultado","control_or_treatment":"treatment"},"campaignAngle":{"primaryPain":"Você até consegue alunos, mas não tem um processo simples e repetível: a conversa cai rápido em “quanto custa?”, o aluno não percebe valor nas primeiras semanas e o engajamento despenca — aí a agenda vira loteria de indicação e de preço.","singleMindedPromise":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","proofSummary":"Estrutura baseada em boas práticas de onboarding e primeiros 30 dias para aumentar percepção de valor e retenção, com marcos iniciais claros e mensagens que antecipam e reforçam o “porquê” do seu acompanhamento.","funnelStage":"Frio → Morno (Topo/Meio)","landingMatchLine":"Agenda cheia sem desconto em 8 semanas: sequência pronta para o aluno entender seu valor e engajar nas primeiras semanas.","primaryCTA":"Desbloquear o kit","mechanismSummary":"Você recebe um kit de ativos digitais gerados por IA (sequência de mensagens + marcos das primeiras semanas + check-ins) para aplicar um processo de entrada e primeiras vitórias que reduz a briga por preço e aumenta engajamento.","tone":"Direto, claro e orientado a resultado, sem prometer “milagre” e sem jargão.","cta":"Desbloquear o kit","primaryPromise":"Agenda cheia sem desconto em até 8 semanas, com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas."}}

Texto do anúncio:
{"adCopy":{"primaryTextVariants":[{"label":"dor","openingHookType":"dor","headline":"Agenda cheia sem desconto (8 semanas)","placementHint":"feed","lengthVariants":{"curta":"A conversa com aluno vira “quanto custa?” e some na 2ª semana?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos entendendo seu valor e ficando engajados no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"A conversa vira “quanto custa?” cedo demais. O aluno entra empolgado… e some nas primeiras semanas. Aí sua agenda vira loteria de indicação e de preço.\n\nA ideia aqui é ter constância: em até 8 semanas, encher sua agenda sem dar desconto — com alunos chegando entendendo o valor do seu acompanhamento e permanecendo engajados no começo.\n\nPara isso, você desbloqueia uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto pra conduzir a conversa sem cair no preço cedo\n• marcos de progresso + check-ins das primeiras semanas\n\nSem call. Sem consultoria. Só ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"A conversa cai em “quanto custa?” e o aluno desengaja no começo?\n\nEm até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e sentindo progresso logo nas primeiras semanas.\n\nDesbloqueie a prévia do Kit gerado por IA com mensagens prontas, roteiro de conversa e check-ins de onboarding.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA): mensagens + roteiro + check-ins.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"resultado","openingHookType":"resultado","headline":"Mais alunos certos. Menos preço.","placementHint":"stories/reels","lengthVariants":{"curta":"Agenda cheia em até 8 semanas — sem dar desconto.\n\nAlunos chegam entendendo seu valor e ficam engajados nas primeiras semanas.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Agenda cheia em até 8 semanas, sem dar desconto.\n\nA diferença não é “fazer mais”. É ter um ciclo simples para:\n1) o aluno entender seu valor antes de falar de preço\n2) o aluno sentir cuidado + progresso rápido nas primeiras semanas (quando mais desiste)\n\nDesbloqueie uma prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• convite e mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto de conversa (sem cair no preço cedo)\n• marcos iniciais + check-ins para segurar o engajamento\n\nSem call. Sem consultoria. Ativos digitais prontos.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Em até 8 semanas, encha sua agenda sem dar desconto.\n\nCom uma sequência pronta, o aluno chega mais preparado (entende seu valor) e o começo já tem marcos + check-ins pra manter engajamento.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Prévia do Kit (IA) para captar e reter melhor.","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"},{"label":"prova","openingHookType":"prova","headline":"Primeiros 30 dias: faz diferença","placementHint":"feed","lengthVariants":{"curta":"Os primeiros 30 dias costumam decidir se o aluno engaja ou some.\n\nEm até 8 semanas, encha sua agenda sem desconto com uma sequência que deixa seu valor claro + cria vitórias rápidas no começo.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","longa":"Os primeiros 30 dias costumam ser decisivos: quando o aluno tem clareza do caminho + pequenas vitórias logo no começo, a percepção de valor sobe e o engajamento tende a aumentar (boas práticas de onboarding e retenção).\n\nCom isso em mente, a promessa é simples: em até 8 semanas, encha sua agenda sem dar desconto — com alunos chegando entendendo seu valor e permanecendo engajados nas primeiras semanas.\n\nDesbloqueie a prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA:\n• mensagens prontas (WhatsApp/DM/e-mail)\n• roteiro curto para conduzir a conversa antes do preço\n• marcos iniciais + check-ins para o aluno sentir progresso e cuidado\n\nSem call. Sem consultoria. Apenas ativos digitais.\n\nDesbloquear o Kit (receber a prévia gerada por IA).","media":"Boas práticas de onboarding mostram: clareza + vitórias rápidas no começo aumentam a percepção de valor.\n\nEm até 8 semanas, encha sua agenda sem dar desconto com uma sequência pronta de comunicação + marcos + check-ins.\n\nDesbloqueie a prévia do Kit gerado por IA.\n\nDesbloquear o Kit (receber a prévia gerada por IA)."},"compliance":{"semPromessaIndividual":true,"semGarantiaAbsoluta":true,"semLinguagemDeConsultoria":true},"description":"Estrutura com foco nos 30 primeiros dias (prévia).","primaryText":"","ctaText":"Desbloquear o Kit (receber a prévia gerada por IA)"}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-copy"}}

Briefing da imagem:
{"adImageBriefing":{"briefings":[{"safeMargins":"Feed 1080x1350: manter 10% de margem de segurança (texto e elementos dentro da área central). Stories 1080x1920: evitar topo (UI) e base (CTA nativo); texto principal no terço central.","formatByPlacement":"Priorizar Feed (1080x1350) com leitura instantânea; adaptar para Stories/Reels (1080x1920) centralizando a metáfora visual e movendo CTA para acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação DOR do anúncio: dor = conversa vira “quanto custa?” cedo + aluno some nas primeiras semanas. Ainda assim, manter a promessa central igual à landing: agenda cheia sem desconto em até 8 semanas. CTA igual à landing: “Desbloquear a prévia/kit”.","imageTextMaxWords":9,"assetType":"estatico","hierarchy":"1) Dor (gatilho rápido) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy de compliance (se couber)","mustMatchAdVariant":"dor","visualAngle":"dor","complianceNotes":"Evitar garantias absolutas e resultados individuais. Não mencionar consultoria/call/gestão humana como entrega. “Gerado por IA” como característica, não milagre. Se usar “em até 8 semanas”, tratar como objetivo/estrutura (aspiracional). Microcopy opcional: “Sem call. Sem consultoria. Ativos digitais (IA).”","visualBriefing":"Conceito: ‘A conversa cai no preço + sumiço na semana 2’. Visual principal em split/contraste com dois balões de chat grandes no centro.\n\nComposição (Feed):\n- Fundo branco/cinza muito claro.\n- Metade esquerda (apagada/cinza): balão de chat com texto curto ‘Quanto custa?’ + ícone de pausa (||) ou ponto de interrogação.\n- Metade direita (ativa/azul escuro #0B1F3B): balão de chat com texto curto ‘Entendi como funciona’ + ícone de check verde #22C55E.\n- No topo, headline curta (máx. 1 linha): ‘Agenda cheia sem desconto’ (ou ‘Chega de briga de preço’ se couber sem perder promessa; manter promessa explícita em selo/linha 2).\n- Selo pequeno em verde: ‘8 SEMANAS’.\n- Rodapé com botão/box contrastante: ‘Desbloquear a prévia’.\n- Microcopy pequeno (se couber, 1 linha): ‘Ativos digitais gerados por IA’.\n\nTexto sugerido (<=9 palavras totais sobre imagem, priorizando):\n- Linha 1: ‘Agenda cheia sem desconto’ (4)\n- Selo: ‘em até 8 semanas’ (4) — se faltar espaço, usar apenas ‘8 semanas’ (2)\n- Botão: ‘Desbloquear a prévia’ (3)\n(Obs.: escolher combinações para respeitar o limite de palavras; preferir ‘Agenda cheia sem desconto’ + ‘8 semanas’ + ‘Desbloquear a prévia’).\n\nÍcones: chat, pause, check.\n\nAdaptação Stories: chats no terço central; headline acima; botão ‘Desbloquear a prévia’ logo abaixo dos chats (antes da área do CTA nativo)."},{"safeMargins":"Stories/Reels 1080x1920: texto e elementos críticos no terço central; evitar topo (UI) e base (CTA nativo). Feed 1080x1350: 10% de margem de segurança em todos os lados.","formatByPlacement":"Priorizar Stories/Reels (1080x1920) com layout vertical e timeline central; adaptar para Feed (1080x1350) mantendo a timeline com 3 marcos visuais e um selo ‘8 semanas’.","messageMatchNotes":"Deve bater com a variação RESULTADO do anúncio: ‘Agenda cheia em até 8 semanas — sem dar desconto’ + ‘alunos chegam entendendo seu valor e ficam engajados no começo’. Manter a mesma promessa do hero da landing e CTA de desbloqueio da prévia do kit.","imageTextMaxWords":10,"assetType":"story-vertical","hierarchy":"1) Resultado (agenda cheia sem desconto) 2) Prazo (8 semanas) 3) Benefício (alunos entendem valor + engajam) 4) CTA 5) Microcopy (IA / sem consultoria)","mustMatchAdVariant":"resultado","visualAngle":"resultado","complianceNotes":"Não prometer ‘agenda cheia garantida’. Usar linguagem aspiracional e de processo (‘com uma sequência pronta’). Não usar termos técnicos (funil/lead). Sem consultoria/call. Incluir ‘Ativos digitais (IA)’ quando possível.","visualBriefing":"Conceito: ‘8 semanas sob controle’ (timeline simples) para passar constância e previsibilidade.\n\nComposição (Stories):\n- Fundo branco com blocos/linhas finas em azul escuro (#0B1F3B) como moldura.\n- Centro: timeline vertical com 8 pontos (Sem 1 a Sem 8) e checks verdes (#22C55E) nos primeiros 3–4 para indicar progresso.\n- Ao lado de 3 pontos, mini-ícones:\n  - Sem 1: ícone chat (mensagens)\n  - Sem 2: ícone documento (roteiro)\n  - Sem 3: ícone check (check-ins)\n- Topo (texto grande): ‘Agenda cheia sem desconto’.\n- Logo abaixo (texto menor): ‘em até 8 semanas’.\n- Linha curta de benefício (se couber): ‘alunos entendem seu valor’.\n- Botão/box no terço inferior (mas acima da área do CTA nativo): ‘Desbloquear a prévia’.\n- Microcopy pequeno: ‘Ativos digitais (IA)’.\n\nTexto sugerido (<=10 palavras totais sobre imagem):\n- ‘Agenda cheia sem desconto’ (4)\n- ‘em até 8 semanas’ (4)\n- ‘Desbloquear a prévia’ (3)\n(Escolher 2 das 3 linhas conforme espaço para respeitar o limite; manter sempre promessa + CTA; o ‘8 semanas’ pode ir no selo).\n\nAdaptação Feed: timeline horizontal/compacta (8 marcadores) no meio; selo ‘8 semanas’; CTA no rodapé."},{"safeMargins":"Feed 1080x1350: 10% de margem de segurança. Stories 1080x1920: manter conteúdo principal no terço central e deixar respiro no topo/base para UI e CTA nativo.","formatByPlacement":"Priorizar Feed (1080x1350) com ‘card de prova’ + referência aos primeiros 30 dias; adaptar para Stories (1080x1920) mantendo o card central grande e CTA acima da área do botão nativo.","messageMatchNotes":"Deve bater com a variação PROVA do anúncio: ‘Primeiros 30 dias fazem diferença’ (boas práticas de onboarding) e conectar com a promessa de ‘agenda cheia sem desconto em até 8 semanas’. CTA deve ser ‘Desbloquear a prévia’ (equivalente ao ‘Desbloquear o Kit’ da landing).","imageTextMaxWords":11,"assetType":"carrossel","hierarchy":"1) Prova (primeiros 30 dias) 2) Promessa (8 semanas, sem desconto) 3) Mecanismo (sequência pronta) 4) CTA 5) Microcopy (sem consultoria)","mustMatchAdVariant":"prova","visualAngle":"prova","complianceNotes":"Evitar ‘comprovado’/‘garantido’. Tratar como boas práticas/referência. Não usar logos de terceiros. Não prometer acompanhamento humano. Incluir ‘Ativos digitais gerados por IA’ como característica, não como promessa.","visualBriefing":"Formato: Carrossel 3 cards (1080x1350) — cada card com pouquíssimo texto e ícones claros.\n\nCard 1 (PROVA):\n- Headline: ‘Primeiros 30 dias’\n- Sub (curto): ‘decidem o engajamento’\n- Visual: mini-calendário 30 dias com 4 checks verdes (semanas) e uma seta apontando ‘continuidade’.\n\nCard 2 (CONEXÃO COM PROMESSA):\n- Headline: ‘8 semanas’\n- Badge: ‘SEM DESCONTO’\n- Sub (curto): ‘sequência pronta’\n- Visual: trilho/linha do tempo com 8 pontos + 2 ícones (chat + check).\n\nCard 3 (AÇÃO/PRODUTO):\n- Headline: ‘Desbloqueie a prévia’\n- Sub (curto): ‘mensagens + check-ins’\n- Micro (pequeno): ‘Ativos digitais (IA). Sem consultoria.’\n- Botão/box: ‘Desbloquear a prévia’.\n\nPaleta e estilo:\n- Fundo branco/cinza claro; textos em azul escuro (#0B1F3B); destaques em verde (#22C55E).\n- Tipografia sans bold para headline.\n\nControle de texto (<=11 palavras por card):\n- Card 1 exemplo: ‘Primeiros 30 dias decidem o engajamento’ (6)\n- Card 2 exemplo: ‘8 semanas. Sem desconto. Sequência pronta.’ (6)\n- Card 3 exemplo: ‘Desbloquear a prévia’ (3) + ‘Ativos digitais (IA)’ (3) (ajustar para não estourar).\n\nObservação: manter a promessa central explícita no Card 2 para garantir consistência com a landing."}]},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"ad-image-briefing"}}

Textos da landing:
{"landingPageCopy":{"hero":{"headline":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","supportingCopy":"Receba uma prévia do Kit de Comunicação + Onboarding (8 semanas) com mensagens prontas, roteiro curto de conversa e check-ins iniciais para reduzir a briga por preço e aumentar engajamento no começo — sem call e sem consultoria.","eyebrow":"Agenda Cheia Sem Desconto (8 semanas)","ctaMatchNotes":"O CTA é idêntico ao do anúncio e leva para o desbloqueio da prévia do kit (ativos digitais gerados por IA).","microcopy":"Entrega 100% digital. Sem consultoria, sem call, sem gestão humana. Ativos gerados por IA para você adaptar e aplicar.","promise":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","subheadline":"Pare de depender de indicação e de “briga de preço”: tenha um processo simples, repetível e aplicável todo mês.","ctaUrl":"#desbloquear","proofBadge":"Baseado em boas práticas de onboarding e primeiros 30 dias (retenção e percepção de valor)."},"messageMatchNotes":"A promessa central e o CTA repetem literalmente o anúncio: agenda cheia sem desconto em até 8 semanas + desbloquear a prévia do kit gerada por IA.","ctaBlocks":[{"ctaSupport":"Desbloqueie a prévia e veja como ficam as mensagens, os marcos e os check-ins das primeiras semanas.","messageMatchNotes":"CTA no hero repete o mesmo texto do anúncio para continuidade imediata após o clique.","ctaVariant":"primary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"hero","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Se a conversa cai no preço cedo, use a sequência para deixar o valor claro antes do “quanto custa?”.","messageMatchNotes":"CTA no meio aparece logo após explicar mecanismo (sequência) e como aplicar sem depender de consultoria.","ctaVariant":"secondary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"mid","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Leve a prévia agora e adapte em minutos para sua rotina (WhatsApp/DM/e-mail).","messageMatchNotes":"CTA final reforça ação única (desbloquear a prévia) após prova + oferta para reduzir ceticismo.","ctaVariant":"primary","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"final","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"},{"ctaSupport":"Prévia do kit (IA) para usar hoje: mensagens + roteiro + check-ins.","messageMatchNotes":"CTA sticky mantém a mesma ação do anúncio durante toda a leitura, sem mudar a intenção.","ctaVariant":"sticky","matchAdCta":"Desbloquear o Kit (receber a prévia gerada por IA)","placement":"sticky","ctaLabel":"Desbloquear o Kit (receber a prévia gerada por IA)","ctaUrl":"#desbloquear"}],"consistencyChecks":[{"details":"CTA na página (hero/meio/final/sticky) usa exatamente ‘Desbloquear o Kit (receber a prévia gerada por IA)’, igual ao anúncio.","check":"CTA_MATCH","status":"PASS"},{"details":"Hero.headline, hero.promise e pageGoal repetem literalmente a single-minded promise fornecida no ângulo da campanha (sem variações criativas).","check":"PROMISE_MATCH","status":"PASS"},{"details":"Página com message match forte, hierarquia clara (dor→resultado→mecanismo→prova→ação), CTAs repetidos, microcopy de entrega e FAQs para reduzir objeções.","check":"GOOGLE_LANDING_BEST_PRACTICES","status":"PASS"}],"bodySections":[{"messageMatchNotes":"Conecta diretamente com a dor citada no anúncio (conversa vira ‘quanto custa?’ e aluno some no começo) e prepara o terreno para a promessa sem trocar o foco.","bullets":["A conversa cai no preço cedo demais — e você perde margem (ou perde o aluno).","O aluno entra animado… e some na 2ª semana porque não enxerga progresso rápido.","Sem um ‘primeiro mês’ bem guiado, o valor do acompanhamento fica invisível.","Resultado: agenda vira loteria de indicação, de mês a mês."],"ctaSupport":"Quer ver a sequência pronta que organiza isso em 8 semanas?","summary":"Se você não deixa claro o seu valor antes do preço e não cria vitórias rápidas no começo, o aluno desengaja — e sua agenda vira loteria.","sectionDependsOn":"primaryPromise","sectionId":"s1-pain-preco-engajamento","sectionType":"pain","copy":"Quando não existe um processo simples e repetível, duas coisas acontecem quase sempre:\n\n1) a conversa vira ‘quanto custa?’ cedo demais (antes do aluno entender o diferente do seu acompanhamento)\n2) nas primeiras semanas, o aluno não sente cuidado/progresso e começa a faltar, enrolar ou desistir\n\nAí o que deveria ser previsível vira estressante: você vive de indicação, de promoção e de negociação.","title":"O que te trava não é falta de aluno. É falta de processo."},{"messageMatchNotes":"Explica o mecanismo como suporte (sequência pronta de comunicação + marcos + check-ins), sem transformá-lo na promessa principal; mantém o match com o anúncio.","bullets":["Mensagens prontas para conduzir a conversa sem cair no preço cedo (WhatsApp/DM/e-mail).","Roteiro curto para apresentar seu ‘porquê’ e o que muda no acompanhamento.","Marcos das primeiras semanas para o aluno perceber evolução rápido.","Check-ins simples (prontos) para reforçar cuidado e consistência no início."],"ctaSupport":"Desbloqueie a prévia e veja exemplos prontos para adaptar ao seu serviço.","summary":"Você aplica uma sequência pronta para o aluno entender seu valor antes do preço e sentir progresso logo no começo.","sectionDependsOn":"mechanismSummary","sectionId":"s2-mechanism-sequencia-pronta","sectionType":"mechanism","copy":"A proposta é colocar sob controle o que normalmente fica “no improviso”:\n\n• Comunicação de entrada: mensagens e roteiro para o aluno entender o valor do seu acompanhamento antes do preço dominar a conversa.\n• Primeiras vitórias: marcos claros nas semanas iniciais para o aluno sentir progresso rápido.\n• Check-ins: pontos de contato prontos para manter engajamento no começo (quando a desistência é mais comum).\n\nTudo em formato de ativos digitais gerados por IA — para você copiar, ajustar e aplicar.","title":"O que muda: uma sequência pronta (com começo, meio e continuidade)"},{"messageMatchNotes":"Apoia a promessa com ‘prova’ em forma de boas práticas (onboarding/primeiros 30 dias) sem alegações absolutas; alinhado ao anúncio de prova e ao briefing.","bullets":["Clareza do caminho reduz ansiedade e aumenta a percepção de valor.","Vitórias rápidas no começo aumentam a chance de continuidade.","Check-ins consistentes reduzem ‘sumiço’ e melhoram engajamento."],"ctaSupport":"Quer ver como isso vira mensagens e check-ins prontos?","summary":"Os primeiros 30 dias são decisivos: clareza + vitórias rápidas tendem a aumentar percepção de valor e engajamento.","sectionDependsOn":"proofSummary","sectionId":"s3-proof-30-dias","sectionType":"proof","copy":"Em boas práticas de onboarding e retenção, o início costuma ser decisivo:\n\nQuando o aluno entende o caminho (o que vai acontecer e por quê) e percebe pequenos avanços logo nas primeiras semanas, a percepção de valor sobe — e o engajamento tende a acompanhar.\n\nPor isso o kit foca em organizar comunicação + marcos + check-ins justamente onde mais dá errado: no começo.","title":"Por que funciona: o começo (primeiros 30 dias) define o resto"},{"messageMatchNotes":"Oferta descrita como prévia do kit (ativos digitais gerados por IA), sem prometer consultoria/call; mantém a promessa central como headline/hero, e a oferta como meio para chegar lá.","bullets":["Prévia do Kit de Comunicação + Onboarding (8 semanas) gerado por IA.","Mensagens prontas (WhatsApp/DM/e-mail) para antes e depois do ‘primeiro sim’.","Roteiro curto para conduzir a conversa e posicionar valor antes do preço.","Marcos das primeiras semanas + check-ins prontos para manter engajamento.","Modelos editáveis para adaptar ao seu estilo (copiar/colar/ajustar)."],"ctaSupport":"Clique para desbloquear a prévia e receber os exemplos gerados por IA.","summary":"Você recebe uma prévia do kit com os ativos essenciais para aplicar a sequência de 8 semanas na prática.","sectionDependsOn":"primaryCTA","sectionId":"s4-offer-previa-kit","sectionType":"offer","copy":"Ao desbloquear, você recebe uma prévia do kit com exemplos prontos para usar como base.\n\nImportante: é entrega digital (ativos), feita para escala — sem call, sem consultoria e sem acompanhamento manual.\n\nVocê adapta ao seu serviço e aplica no seu ritmo.","title":"O que você recebe na prévia (entrega digital)"}],"messageMatchSource":"Agenda cheia sem desconto (8 semanas)","pageGoal":"Encha sua agenda sem baixar preço em até 8 semanas, com uma sequência pronta que faz o aluno entender seu valor antes de falar de preço e sentir progresso logo no começo.","primaryCTA":"Desbloquear o Kit (receber a prévia gerada por IA)","faq":[{"question":"Isso é consultoria, mentoria ou tem call?","answer":"Não. A entrega é 100% digital: ativos gerados por IA (mensagens, roteiro, marcos e check-ins) para você adaptar e aplicar. Não inclui call, consultoria, gestão humana ou acompanhamento manual.","objectionTag":"entrega_sem_consultoria"},{"question":"Serve para qualquer tipo de acompanhamento (treino, nutrição, estética, aulas, etc.)?","answer":"Sim, porque a lógica é de comunicação e onboarding: deixar o valor claro antes do preço e criar marcos iniciais + check-ins. Você só ajusta a linguagem e os marcos para sua realidade.","objectionTag":"serve_para_meu_nicho"},{"question":"E se eu já tenho alunos, mas eles desengajam nas primeiras semanas?","answer":"O kit foi pensado exatamente para esse ponto: organizar os primeiros marcos e os check-ins para o aluno sentir cuidado e progresso rápido no começo, reduzindo o ‘sumiço’ e aumentando a continuidade.","objectionTag":"retencao_engajamento"},{"question":"Isso garante agenda cheia em 8 semanas?","answer":"Não existe garantia de resultado individual. A promessa é um objetivo com base em um processo repetível (sequência de comunicação + marcos + check-ins) que costuma melhorar percepção de valor e engajamento quando aplicado de forma consistente.","objectionTag":"sem_garantia_absoluta"}],"complianceNotes":"A oferta é uma prévia de um kit de ativos digitais gerados por IA (mensagens, roteiro, marcos e check-ins). Não há promessa de consultoria, call, gestão humana ou acompanhamento manual. Evita garantias absolutas e resultados individuais; ‘em até 8 semanas’ é tratado como objetivo/estrutura baseada em processo."},"experimentMetadata":{"variant_id":"variant-10","control_or_treatment":"treatment","primary_variable":"Dor vs Resultado","stage":"AD","asset_role":"landing-page-copy"}}

Diretriz específica para wireframe da landing:
Você cria ativos de campanha para o Marketing Hub.

Regras globais:
1. O anúncio e a landing devem ter a mesma promessa central.
2. O CTA do anúncio deve combinar com a ação principal da landing.
3. O material precisa caber no envelope real do produto:
   - pode entregar ativos digitais gerados por IA
   - não pode prometer consultoria, call, gestão humana ou acompanhamento manual
4. Priorize clareza comercial:
   DOR → RESULTADO → MECANISMO → PROVA → AÇÃO
5. Não transforme mecanismo em promessa principal.
6. Não use jargão técnico desnecessário.
7. O público é geral dentro do nicho, com baixa a moderada maturidade em marketing.
8. Sempre escreva pensando em alta escala e geração automatizada.
9. O anúncio deve ser rápido de entender.
10. A landing deve aprofundar a promessa e reduzir ceticismo.

Hero/headline atual: Agenda cheia sem desconto (8 semanas)
CTA obrigatório: Desbloquear o Kit (receber a prévia gerada por IA)
Linha de match com landing: Agenda cheia sem desconto em 8 semanas: sequência pronta para o aluno entender seu valor e engajar nas primeiras semanas.

Objetivo:
Transformar o copy da landing em um wireframe testável, mobile-first e com message match obrigatório.

Regras:
1. pageGoal deve deixar explícito o resultado da página (ex.: gerar pedido da prévia).
2. variantLayoutId deve ser form-first, proof-first ou story-first.
3. sectionOrder precisa listar cada bloco com sectionId, sectionName, objective, contentType (hero, form, split, proof, timeline, faq, cta), copySource e uiNotes.
4. Cada sectionOrder[i] deve preencher mobilePriorityScore (1-10), dropOffRisk (baixo, medio ou alto) e sectionDependsOn amarrando com primaryPromise, mechanismSummary, proofSummary ou primaryCTA.
5. Se a seção tiver CTA, preencher ctaSlot com hasCta=true, ctaLabel, ctaVariant (hero, mid, final, sticky ou inline) e matchAdCta.
6. formPlacementNotes precisa informar quantos scrolls são necessários para ver o formulário e se existe versão sticky.
7. ctaPlacementNotes deve garantir repetição literal do CTA principal em toda a página.
8. consistencyChecks deve incluir CTA_MATCH e EXPERIENCE_CONTINUITY (status PASS/WARN/FAIL) descrevendo se o anúncio e a landing estão alinhados.
9. mobilePriorityNotes deve destacar o que precisa aparecer antes da rolagem.

Formato obrigatório:
- Preencher sectionOrder respeitando a ordem real da landing e referenciando sectionId de bodySections quando existir.
- Preencher ctaSlot dentro das seções com CTA.
- Preencher consistencyChecks e observações finais (mobilePriorityNotes, ctaPlacementNotes, formPlacementNotes).
- Reforçar message match usando primaryAdHeadline, landingMatchLine e Desbloquear o Kit (receber a prévia gerada por IA) como referência fixa.

Instruções extras do usuário:
Quantidade sugerida: 2

Responda exclusivamente em JSON válido e siga estritamente o schema da seção.
```

**Resultado**
```text
{"output_text":null,"id":"resp_0e192d5bd34f91ea0069cee62cfc4c8193845d20b062fadcf5","output":[{"tool_call":null,"tool_calls":null,"id":"msg_0e192d5bd34f91ea0069cee62d89a08193b62c0695891f6f29","type":"message","role":"assistant","content":[{"type":"output_text","text":"{\"landingPageWireframe\":{\"messageMatchSummary\":\"Message match obrigatório mantido: promessa central e CTA repetem literalmente o anúncio. Referências fixas: primaryAdHeadline = \\\"Agenda cheia sem desconto (8 semanas)\\\", landingMatchLine = \\\"Agenda cheia sem desconto em 8 semanas: sequência pronta para o aluno entender seu valor e engajar nas primeiras semanas.\\\", CTA = \\\"Desbloquear o Kit (receber a prévia gerada por IA)\\\".\",\"sectionOrder\":[{\"contentType\":\"hero\",\"uiNotes\":\"Mobile-first: bloco compacto, headline inteira visível acima da dobra (sem quebrar em muitas linhas). Incluir eyebrow + proofBadge pequeno. Botão CTA primário logo abaixo do subheadline. Microcopy de compliance em fonte menor (1–2 linhas).\",\"ctaSlot\":{\"notes\":\"CTA principal acima da dobra, idêntico ao anúncio. Ideal ter também um sticky CTA no mobile após 20–30% de scroll.\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"hero\"},\"objective\":\"Garantir continuidade do anúncio (promessa + prazo + sem desconto) e direcionar para o desbloqueio da prévia imediatamente.\",\"dropOffRisk\":\"baixo\",\"sectionName\":\"Hero (Promessa + CTA)\",\"mobilePriorityScore\":10,\"messageMatchDependency\":\"primaryPromise + primaryCTA + landingMatchLine\",\"sectionId\":\"hero\",\"sectionDependsOn\":\"primaryPromise\",\"copySource\":\"landingPageCopy.hero\"},{\"contentType\":\"form\",\"uiNotes\":\"Form curto (2–4 campos). Título do form repete a ação: “Desbloquear a prévia do Kit”. Após envio, expectativa clara: “Você recebe a prévia por e-mail/WhatsApp” (definir canal). Inserir nota: “Entrega 100% digital”.\",\"ctaSlot\":{\"notes\":\"Botão do formulário usa exatamente o CTA obrigatório. Evitar ‘Enviar’.\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"inline\"},\"objective\":\"Converter rapidamente (pedido da prévia) antes de leitura longa; reduzir fricção no mobile.\",\"dropOffRisk\":\"medio\",\"sectionName\":\"Formulário (Desbloqueio da prévia)\",\"mobilePriorityScore\":9,\"messageMatchDependency\":\"primaryCTA\",\"sectionId\":\"s0-form-desbloquear\",\"sectionDependsOn\":\"primaryCTA\",\"copySource\":\"landingPageCopy.hero.ctaLabel + landingPageCopy.hero.microcopy\"},{\"contentType\":\"split\",\"uiNotes\":\"Formato lista curta com bullets grandes e escaneáveis. Evitar parágrafos longos. Pode usar ícones simples (R$ / chat / calendário) para leitura rápida. Inserir mini-CTA em texto (link âncora) abaixo dos bullets.\",\"ctaSlot\":{\"notes\":\"Mini-CTA secundário, repetindo o CTA literal e levando ao form (#desbloquear).\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"mid\"},\"objective\":\"Ancorar a dor do anúncio (preço cedo + sumiço nas primeiras semanas) e preparar a aceitação do processo.\",\"dropOffRisk\":\"medio\",\"sectionName\":\"Dor (Preço cedo + desengajamento no começo)\",\"mobilePriorityScore\":8,\"messageMatchDependency\":\"primaryPromise (contexto)\",\"sectionId\":\"s1-pain-preco-engajamento\",\"sectionDependsOn\":\"primaryPromise\",\"copySource\":\"landingPageCopy.bodySections[0]\"},{\"contentType\":\"split\",\"uiNotes\":\"Bloco ‘o que muda’ em 4 bullets com títulos curtos (Mensagens / Roteiro / Marcos / Check-ins). Inserir callout pequeno: “Ativos digitais gerados por IA (editáveis)”.\",\"ctaSlot\":{\"notes\":\"CTA após explicar o mecanismo, para quem precisa entender “como” antes de pedir a prévia.\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"mid\"},\"objective\":\"Explicar o mecanismo como suporte (sequência pronta) sem virar promessa principal; reduzir objeção “não sei o que fazer”.\",\"dropOffRisk\":\"medio\",\"sectionName\":\"Mecanismo (Sequência pronta)\",\"mobilePriorityScore\":7,\"messageMatchDependency\":\"mechanismSummary\",\"sectionId\":\"s2-mechanism-sequencia-pronta\",\"sectionDependsOn\":\"mechanismSummary\",\"copySource\":\"landingPageCopy.bodySections[1]\"},{\"contentType\":\"proof\",\"uiNotes\":\"Proof em formato de ‘boas práticas’ (sem números absolutos). Usar 3 bullets + 1 parágrafo curto. Selo pequeno: “Primeiros 30 dias”.\",\"ctaSlot\":{\"notes\":\"CTA logo após a prova para capturar quem precisava de ‘por que funciona’.\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"mid\"},\"objective\":\"Reduzir ceticismo com racional simples: começo decide percepção de valor e engajamento.\",\"dropOffRisk\":\"baixo\",\"sectionName\":\"Prova/Princípio (Primeiros 30 dias)\",\"mobilePriorityScore\":6,\"messageMatchDependency\":\"proofSummary\",\"sectionId\":\"s3-proof-30-dias\",\"sectionDependsOn\":\"proofSummary\",\"copySource\":\"landingPageCopy.bodySections[2]\"},{\"contentType\":\"timeline\",\"uiNotes\":\"Timeline vertical (mobile) com 8 passos (Semana 1–8) em 1 linha cada: ‘mensagem’, ‘roteiro’, ‘marco’, ‘check-in’… (alto nível). Evitar prometer resultados garantidos; linguagem de processo: “aplica”, “organiza”, “reforça”.\",\"ctaSlot\":{\"notes\":\"CTA inline no fim da timeline (ancora no form).\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"inline\"},\"objective\":\"Materializar o ‘8 semanas’ como estrutura simples e previsível (reforça a promessa sem parecer mágica).\",\"dropOffRisk\":\"medio\",\"sectionName\":\"Timeline (8 semanas sob controle)\",\"mobilePriorityScore\":6,\"messageMatchDependency\":\"primaryPromise (8 semanas) + mechanismSummary\",\"sectionId\":\"s2b-timeline-8-semanas\",\"sectionDependsOn\":\"primaryPromise\",\"copySource\":\"Derivado de landingPageCopy.hero (8 semanas) + landingPageCopy.bodySections[1] (sequência)\"},{\"contentType\":\"split\",\"uiNotes\":\"Card de oferta com checklist do que vem na prévia. Inserir bloco de ‘o que NÃO é’ (sem call/consultoria/gestão). Reforçar editável/copy-colar.\",\"ctaSlot\":{\"notes\":\"CTA primário antes do FAQ, para capturar quem já decidiu após ver a entrega.\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"mid\"},\"objective\":\"Deixar a oferta tangível e compatível com o envelope do produto (entrega digital gerada por IA).\",\"dropOffRisk\":\"baixo\",\"sectionName\":\"Oferta (O que você recebe na prévia)\",\"mobilePriorityScore\":7,\"messageMatchDependency\":\"primaryCTA\",\"sectionId\":\"s4-offer-previa-kit\",\"sectionDependsOn\":\"primaryCTA\",\"copySource\":\"landingPageCopy.bodySections[3]\"},{\"contentType\":\"faq\",\"uiNotes\":\"Accordion com 4 perguntas. A primeira precisa ser ‘Isso é consultoria/tem call?’. Incluir resposta de ‘sem garantia individual’ de forma simples. Última linha com link/âncora para o form.\",\"ctaSlot\":{\"notes\":\"CTA textual após o FAQ (ou dentro do último accordion) para reduzir fricção final.\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"inline\"},\"objective\":\"Remover objeções principais (entrega, aplicabilidade, garantia) e fechar a decisão.\",\"dropOffRisk\":\"medio\",\"sectionName\":\"FAQ (Objeções)\",\"mobilePriorityScore\":5,\"messageMatchDependency\":\"primaryCTA + complianceNotes\",\"sectionId\":\"faq\",\"sectionDependsOn\":\"primaryCTA\",\"copySource\":\"landingPageCopy.faq\"},{\"contentType\":\"cta\",\"uiNotes\":\"Faixa final com headline curta repetindo a promessa (sem inventar nova). Botão grande. Repetir microcopy: “Entrega 100% digital. Ativos gerados por IA.”\",\"ctaSlot\":{\"notes\":\"Último empurrão: CTA final idêntico ao anúncio, alto contraste, largura total no mobile.\",\"ctaLabel\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"hasCta\":true,\"matchAdCta\":\"Desbloquear o Kit (receber a prévia gerada por IA)\",\"ctaVariant\":\"final\"},\"objective\":\"Fechamento e recuperação de quem rolou até o fim sem converter.\",\"dropOffRisk\":\"baixo\",\"sectionName\":\"CTA Final (Desbloquear a prévia)\",\"mobilePriorityScore\":6,\"messageMatchDependency\":\"primaryCTA + primaryPromise\",\"sectionId\":\"s-final-cta\",\"sectionDependsOn\":\"primaryCTA\",\"copySource\":\"landingPageCopy.hero.promise + landingPageCopy.ctaBlocks[2]\"}],\"variantLayoutId\":\"form-first\",\"mobilePriorityNotes\":\"Antes da rolagem (primeira dobra) precisa aparecer: 1) eyebrow “Agenda Cheia Sem Desconto (8 semanas)”, 2) headline completa com ‘em até 8 semanas’ + ‘sem baixar preço’, 3) botão CTA literal, 4) microcopy “Entrega 100% digital. Sem consultoria/sem call. Ativos gerados por IA”. Ideal: form visível ainda no primeiro scroll (logo abaixo do hero) e sticky CTA no rodapé após pequena rolagem.\",\"ctaPlacementNotes\":\"Repetir literalmente “Desbloquear o Kit (receber a prévia gerada por IA)” no: hero, botão do form, pós-dor (mid), pós-mecanismo (mid), pós-prova (mid), inline na timeline, pós-oferta (mid), inline no FAQ e CTA final. Evitar variações como “baixar”, “receber”, “acessar” para não quebrar o match do anúncio.\",\"formPlacementNotes\":\"Form aparece no 1º scroll (logo após o hero). No mobile, o usuário vê o CTA acima da dobra e alcança o form com ~0,5–1 scroll. Recomendar versão sticky de CTA no rodapé (não necessariamente o form inteiro) durante a leitura; ao clicar, scroll para o form (#desbloquear).\",\"pageGoal\":\"Gerar pedido de desbloqueio da prévia do Kit de Comunicação + Onboarding (8 semanas) — ativos digitais gerados por IA — via formulário (conversão principal).\",\"consistencyChecks\":[{\"details\":\"CTA repetido literalmente em todos os pontos: “Desbloquear o Kit (receber a prévia gerada por IA)”, igual ao anúncio.\",\"check\":\"CTA_MATCH\",\"status\":\"PASS\"},{\"details\":\"Promessa e linha de match mantidas do anúncio para a landing (agenda cheia sem desconto em até 8 semanas + sequência pronta para valor antes do preço e engajamento inicial). Nenhuma promessa de consultoria/call/gestão humana. Estrutura DOR→RESULTADO→MECANISMO→PROVA→AÇÃO preservada.\",\"check\":\"EXPERIENCE_CONTINUITY\",\"status\":\"PASS\"}]},\"experimentMetadata\":{\"variant_id\":\"variant-10\",\"control_or_treatment\":\"treatment\",\"primary_variable\":\"Dor vs Resultado\",\"stage\":\"AD\",\"asset_role\":\"landing-page-wireframe\"}}"}]}],"usage":{"input_tokens":8146,"output_tokens":2673,"prompt_tokens":null,"completion_tokens":null,"total_tokens":10819},"error":null,"status":"completed"}
```
