# Radar de Design de Experiência — 30/10/2026
**Experience Engine v60 | Marketing Hub**
**Recorte:** artigos submetidos principalmente de 27 a 29/10/2026, listados em Human-Computer Interaction no arXiv em 30/10/2026. Contraste com o relatório de 29/10 para evitar repetição. Fontes primárias e links abaixo.

## Síntese
Duas descobertas se destacam: pequenas alterações no tempo e no ponto de vista de um vídeo podem melhorar sua naturalidade percebida sem reduzir necessariamente o tempo de edição; e preferir uma imagem está associado a considerá-la de autoria humana, inclusive quando foi produzida por IA. Uma terceira evidência experimental aponta que uma breve autorreflexão guiada por IA antes de deliberar pode elevar a qualidade das justificativas e do diálogo. Não confundir esses resultados com prova de vendas.

## 1. Micro-Edits: ritmo, silêncio, sincronismo e ponto de vista
**Fonte e data:** Tran et al., *Micro-Edits*, submetido em 29/10/2026, artigo UIST 2026. https://arxiv.org/html/2610.40653v1

**Descoberta/evidência:** seis editores em estudo formativo; duas avaliações com 6 e 10 editores, e avaliação perceptiva de 47 espectadores (quatro excluídos). Os editores diferenciam microedição de ritmo (pausas, duração) de ponto de vista (transições de áudio/vídeo, olhares e reações). Nos nove logs válidos da segunda avaliação houve 1,4 operações/min assistidas versus 4,9 manuais (p=0,004); porém o tempo total médio foi 13,1 min com assistência e 11,4 min no modo manual. No teste de sincronismo, cortes otimizados alcançaram naturalidade média 5,27/7 contra 3,10/7 em cortes fora da janela, e 32,4% versus 85,1% de violações percebidas.

**Mecanismo:** escolhas de timing e atenção visual podem ser manipuladas separadamente; sugestões de cortes reduzem manipulação quadro a quadro e possibilitam examinar alternativas de reação.

**Produto/experimento:** Apolo pode gerar montagem inicial e oferecer refinamento humano de J/L-cuts, pausas e planos de reação. Comparar taxa de erros de sincronismo, tempo de edição, retenção, compreensão e custo por vídeo aprovado.

**Risco/limite:** amostras pequenas e cenas dialogadas; não foi observado ganho em conversões nem redução média de duração do trabalho. Erros de análise audiovisual exigem conferência. **Card: video.**

## 2. Beauty Is Human: gostar de uma imagem e atribuir autoria humana
**Fonte e data:** Chen, Zhu e Zheng, preprint de 28/10/2026. https://arxiv.org/html/2610.38113v1

**Descoberta/evidência:** 11.123 pessoas avaliaram 50 imagens (26 IA); 9.152 também indicaram a favorita. Com efeitos fixos por participante e imagem, a favorita foi 22,1 pontos percentuais mais associada a ser julgada de autoria humana. O padrão apareceu tanto em imagens de IA quanto humanas, e foi observado também em reanálise de avaliação de poemas por 230 pessoas.

**Mecanismo interpretativo:** preferência estética pode funcionar como pista subjetiva de autoria humana; a crença sobre a origem também pode influenciar o gosto. A direção causal não foi identificada.

**Produto/experimento:** Íris pode comparar apresentações verdadeiras e claras da procedência de um criativo, medindo entendimento, confiança calibrada, preferência e eventos reais de funil. Manter o visual constante e randomizar somente uma forma ética de apresentação da informação, sem ocultar autoria quando a divulgação for exigida.

**Riscos/limites:** amostra autoselecionada, imagens curadas para desafio de detecção, possíveis respostas repetidas; sem compras e sem prova de eficácia publicitária. **Card: neuromarketing.** A ideia é diferente do card de 29/10 (atenção ocular versus preferência em anúncios).

## 3. Autorreflexão guiada por IA antes de uma decisão coletiva
**Fonte:** Li et al., submetido em 27/10, https://arxiv.org/abs/2610.37862

**Descoberta/evidência:** experimento randomizado com 1.173 pessoas em 391 deliberações pequenas. Reflexão não diretiva com chatbot antes da conversa elevou o índice de qualidade discursiva em relação a não refletir (d=0,31) e refletir sem assistência (d=0,27); efeito mais acentuado em grupos com discordância.

**Mecanismo:** organizar previamente justificativas, contrapontos e incertezas pode favorecer argumentação e escuta. Não se pode atribuir cada componente ao efeito isoladamente.

**Produto/experimento:** antes da aprovação de uma hipótese de produto, um agente não diretivo pode solicitar evidências pró e contra; comparar quantidade de objeções relevantes detectadas e qualidade de decisões posteriores.

**Limites:** contexto de deliberação de grupos, não compras ou produtividade corporativa. **Sem card:** colaboração deliberativa não é coleção aceita.

## 4. Intervenção humana no planejamento pode preservar diversidade criativa
**Fonte:** Li et al., *What Is the Role for Humans in Creative Writing?*, 28/10, https://arxiv.org/abs/2610.37916

**Descoberta/evidência:** estudo com 333 participantes e 427 histórias, organizadas em cadeias de planejar, escrever, revisar e reescrever. Cadeias só de IA produziram histórias avaliadas como mais criativas, porém menos diversas que cadeias só humanas. Planejamento humano inicial em cadeia majoritariamente automatizada preservou diversidade e nível de criatividade.

**Mecanismo:** especificar objetivos e escolhas narrativas antes da geração preserva variedade das intenções humanas.

**Produto/experimento:** testar no Apolo briefing humano de ângulo e conflito antes da geração automática versus briefing inferido pela IA; medir diversidade entre peças e adequação ao produto.

**Limites:** narrativas escritas, não vídeos comerciais; não confundir diversidade com vendas. **Sem card:** mecanismo de fluxo criativo não equivale à coleção video.

## 5. Personalidade artificial afeta a experiência, mas não se resume aos Big Five
**Fonte:** Dregger et al., submetido em 28/10, https://arxiv.org/abs/2610.38608

**Descoberta/evidência:** três experimentos, N total 395, avaliaram um questionário de personalidade artificial. Traços humanos não migraram todos para a percepção de IA; manipulações de personalidade apresentaram efeitos significativos em satisfação, intenção de uso continuado e presença social.

**Mecanismo:** estilo do agente modifica expectativas sociais, não apenas a utilidade objetiva.

**Produto/experimento:** oferecer configuração explícita de tom e iniciativa em agentes, medir precisão, satisfação e rejeição sem presumir emoções ocultas.

**Limites:** instrumento psicométrico inicial; intenção de continuidade não é adoção real nem benefício causal de longo prazo. **Sem card:** Human-AI Interaction fora das coleções.

## 6. GazeEval: o olhar oferece sinais sobre realismo percebido de vídeos
**Fonte:** Shi et al., submetido em 28/10, https://arxiv.org/html/2610.38825v1

**Descoberta/evidência:** 90 participantes, 4.286 ensaios de eye tracking e 438 vídeos. Uma abordagem que prediz avaliação de realismo por padrão ocular alcançou 71,3% de acurácia contra 61,4% de comparador de avaliação por estímulo.

**Mecanismo proposto:** na segunda visualização, espectadores podem procurar inconsistências e mudar padrões de exploração ocular.

**Produto/experimento:** para Apolo, testar revisão de artefatos visuais com observações humanas estruturadas em vez de apenas nota global; estudos com eye tracking seriam complementares, não necessários para produzir criativos.

**Limites:** vídeos de cinco segundos sem pessoas/narrativas, vistos duas vezes para avaliar autenticidade; não mostra resposta publicitária ou prazer audiovisual. **Sem card:** operacionalização comercial ainda restrita.

## 7. Voz regional: acolhimento e autoridade percebida podem entrar em conflito
**Fonte:** Serditova, Tang e Steffens, 27/10, https://arxiv.org/abs/2610.38005

**Descoberta/evidência:** estudo com assistente de voz simulado em sotaque Geordie versus inglês padrão do sul da Inglaterra. O sotaque regional foi associado a proximidade e pertencimento, mas também a dúvidas sobre profissionalismo; voz padrão foi descrita como mais neutra e credível. Usuários relataram adaptar sua fala para serem reconhecidos.

**Mecanismo:** expectativas sociolinguísticas e pressões de inteligibilidade influenciam a relação com interfaces por voz.

**Produto/experimento:** permitir escolha de voz, velocidade e legendas; medir entendimento e esforço de usuários com diferentes variantes linguísticas, sem estereotipar sotaques.

**Limites:** pesquisa contextual, sem evidência de conversões, e conclusões não transferíveis automaticamente ao português brasileiro. **Sem card.**

## 8. InterA11y: acessibilidade exige interação real e evidência determinística
**Fonte:** Zhong et al., 29/10, https://arxiv.org/abs/2610.39488

**Descoberta/evidência:** 197 casos anotados por especialistas em 54 páginas reais. Agente com rubricas WCAG e ferramentas para observar estados interativos alcançou 79,0% de recall versus 44,4% no melhor comparador baseado em LLM; precisão 93,1% dentro do intervalo 90,3–97,6% dos melhores verificadores baseados em regras.

**Mecanismo técnico:** executar ações e inspecionar foco, erros e estados evita inferência baseada somente em screenshots.

**Produto/experimento:** integrar testes de teclado, foco, mensagens de validação e acessibilidade dinâmica em landing pages produzidas pelo AI Hub; comparar falhas confirmadas e falsos positivos.

**Limites:** apenas 10 critérios WCAG e conjunto amostral restrito; não substitui avaliação com usuários. **Sem card:** acessibilidade não é coleção de roteamento existente.

## 9. StreamStudio: gerar vídeo enquanto se decide a próxima cena
**Fonte:** Li et al., submetido em 28/10, https://arxiv.org/abs/2610.38467

**Descoberta/evidência:** protótipo de autoria que permite orientar vídeo durante a geração, voltar a instantes anteriores e explorar continuações, em vez de esperar renderização integral. O resumo não relata melhoria quantitativa de produtividade ou preferência de espectadores.

**Mecanismo proposto:** tornar a geração reversível e incremental pode reduzir a distância entre intenção criativa e resultado.

**Produto/experimento:** prototipar para Apolo prévias interrompíveis e ramificações de cenas; comparar custo total, tempo até aprovação e retrabalho com renderização convencional.

**Limites:** paradigma de interface em protótipo, dependente de modelos de geração compatíveis; sem prova comercial. **Sem card:** sem evidência suficiente para orientação de uso na coleção video.

## Experience Engine v60
**Intenção criativa explicitada → montagem inicial → microedição reversível → avaliação perceptiva e factual separadas → aprovação humana → resultado comercial real.**

Prioridade Apolo: testar microedição de ritmo e reações sem presumir diminuição de tempo total. Prioridade Íris/Têmis: procedência honesta dos criativos e confiança calibrada. Prioridade AI Hub: reflexões curtas antes de decisões sensíveis e verificação de acessibilidade no fluxo real.

## Decisão editorial e contratos
Guia consultado na `main`: `harness-library-api/docs/guia-uso-api-cards.md`, blob SHA `2d38df09cdb01ad604c3edc88a57a22011c092d1`; OpenAPI em `docs/swagger/harness-library-api-swagger.yaml`. Coleções válidas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`. Nenhuma coleção `design-experiencia` criada.

Cards candidatos a DRAFT:
- `video-microedicao-ritmo-atencao-sincronia`, `video`, fonte `pesquisas/design-experiencia/cards/fontes/2026-10-30-microedicao-ritmo-sincronia.md`, SHA-256 `abb3f42480558060dea5d527a0b2632204a7384894eab3c4f605c9cf46ff056d`.
- `preferencia-estetica-atribuicao-autoria-humana-ia`, `neuromarketing`, fonte `pesquisas/design-experiencia/cards/fontes/2026-10-30-preferencia-estetica-autoria-ia.md`, SHA-256 `c7fe9c9cf4e7e99d9366a74ca42956399d888ad068232f29ba191c49251e26a1`.

Cada JSON possui somente os 16 campos do contrato, datas 2026-10-30 a 2026-12-14 e fontes revisadas previamente. Não submeter para revisão, ativar ou arquivar. Outros achados permanecem apenas no relatório pelas razões editoriais acima.
