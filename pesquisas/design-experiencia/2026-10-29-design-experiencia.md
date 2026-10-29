# Radar de Design de Experiência — 29/10/2026

**Experience Engine v59 | Marketing Hub**  
**Escopo:** UX, Emotional Design, Behavioral Design, Adaptive UX, Generative UI, Human–AI Interaction, atenção, multimodalidade, voz, haptics e agentes.  
**Recorte de verificação:** 29/10/2026, 08h (horário de Brasília). A listagem regular mais recente de `cs.HC` no arXiv nesse momento ainda era **28/10/2026** (https://arxiv.org/list/cs.HC/new). Foram revisados estudos de 26–27/10 ainda não cobertos no relatório anterior e uma publicação **revisada por pares de 10/10**, descoberta agora, que traz evidência de publicidade mais diretamente aplicável que o estudo anterior de pinturas.

## Síntese analítica

**Descoberta principal:** mais fixações visuais em anúncios não representam automaticamente maior preferência ou melhor resultado comercial. Um experimento publicado em *Sensors* (10/10) comparou anúncios humanos, gerados inteiramente por IA e humanos refinados por IA. Ambas as modalidades de IA apresentaram padrões de olhar diferentes dos materiais humanos, mas apenas a modalidade totalmente gerada por IA apresentou preferência binária superior ao comparador. Diferenças de composição visual impedem atribuir o efeito exclusivamente à origem criativa.

**Outros avanços:** usuários de IA combinam necessidades funcionais e afetivas; descrições textuais corretas podem ser insuficientes para edição acessível; agentes com memória contextual precisam de evidências e controle; a divulgação integral de ataques pode facilitar sua reutilização; avaliação de trabalho com IA exige mais que verificar o artefato final.

**Decisão editorial:** um candidato a DRAFT na coleção `neuromarketing`, baseado no experimento direto com anúncios. Não foram criados cards para estudos de arquitetura, segurança, acessibilidade ou protótipos de experiência que não se encaixam nas quatro coleções válidas.

## 1. Publicidade feita com IA: atenção visual e preferência são medidas distintas

**Fonte:** Gonçalves, A., Afonso, M., Martins, P. e Teixeira, A. (2026), *Eye-Tracking Analysis of Visual Attention and Preference in Human-Created, AI-Generated, and AI-Refined Human Advertisements*, *Sensors*, 26(20), 6411, **publicado em 10/10/2026**. https://doi.org/10.3390/s26206411

**Descoberta e evidência.** Em um estudo com 39 adultos de 18 a 26 anos, com alta familiaridade com IA, foram realizados 858 julgamentos envolvendo 22 pares de anúncios com rastreamento ocular a 60 Hz. Imagens geradas inteiramente por IA foram preferidas em 58,97% das comparações com anúncios humanos (OR=1,44; IC95% 1,15–1,80; p=0,0015). Imagens humanas refinadas com IA tiveram 49,88% das escolhas frente às humanas (p=0,978). Anúncios criados/refinados com IA receberam mais fixações e movimentos oculares de menor amplitude, mas o tempo até a primeira fixação não diferiu de forma detectável. A diferença entre os dois efeitos de escolha não foi formalmente estabelecida; resultados devem ser interpretados com essa cautela.

**Mecanismo interpretativo.** Maior exploração localizada pode estar ligada à organização e complexidade da imagem, não necessariamente a preferência. A densidade de bordas era maior nas imagens processadas por IA, uma fonte de confundimento; o estudo não isolou causalidade entre autoria, olhar e escolha.

**Produto e experimento.** Para Íris, Psique e Têmis, testar criativos humanos, IA integral e refinamento por IA, mantendo oferta, legenda, preço e hierarquia visual comparáveis. Separar métricas de fixação, entendimento de benefício, escolha declarada, CTA, checkout, pagamento e custo por criativo aprovado. Verificar legibilidade e transparência sobre o uso de IA.

**Riscos e limites.** Amostra pequena/jovem, estímulos sem divulgação da origem, diferença estrutural dos anúncios e preferências hipotéticas em tela, sem compra real. Não afirmar que IA vende mais. Este achado justifica **um card `neuromarketing`**, distinto do card de 25/10 sobre rostos em pinturas: aqui a evidência é sobre peças publicitárias e diferença entre atenção e preferência.

## 2. AI-Support: pessoas podem buscar ajuda emocional e funcional ao mesmo tempo

**Fonte:** Wang-Sun et al., submetido em **26/10/2026**, *Affective and Functional Needs in AI Use*, https://arxiv.org/abs/2610.34732 ; texto integral https://arxiv.org/html/2610.34732v1.

**Descoberta e evidência.** Survey de 1.090 participantes na China (dados colhidos em julho–agosto de 2025). O instrumento AI-Support identifica quatro dimensões de apoio percebido: emocional/autoestima, companhia, informação e ajuda instrumental. Uma análise exploratória de agrupamentos indicou cinco configurações de uso; as necessidades podem coexistir em vez de constituir dois perfis excludentes.

**Mecanismo.** Uma pessoa pode querer instruções objetivas e, em momentos diferentes, reconhecimento ou companhia. Um único “tipo psicológico” fixo pode representar mal essa variação. A relação com bem-estar é **correlacional**, não prova de que a IA cause melhora ou piora.

**Produto e experimento.** Em agentes conversacionais, oferecer escolha explícita entre resposta objetiva, explicação detalhada e acolhimento contextual sem inferir emoções ocultas. Comparar satisfação, adequação do tom, solução da tarefa e pedidos para mudar o estilo.

**Riscos e limites.** Dados autorrelatados, amostra chinesa, perfis exploratórios e sem experimento causal; usar apoio afetivo para estimular vínculo ou dependência comercial seria inadequado. **Sem card:** Human–AI Interaction afetiva não é coleção aceita.

## 3. Descrições acessíveis precisam revelar o que o usuário não sabia perguntar

**Fonte:** Kim et al., **26/10/2026**, *Beyond Being Accurate*, https://arxiv.org/abs/2610.35562.

**Descoberta e evidência.** Um piloto autoetnográfico com o primeiro autor cego e 12 participantes videntes, seguido de estudo Wizard-of-Oz com dez pessoas cegas ou com baixa visão, investigou descrições de objetos 3D. Os autores identificaram uma diferença entre compreender parcialmente um objeto e conseguir **editá-lo**, mesmo com descrições corretas. Categorizaram falhas de precisão, completude, expressão e navegação.

**Mecanismo.** A edição depende de características não percebidas e de referências espaciais que o usuário não pode solicitar quando desconhece sua existência.

**Produto e experimento.** Testar interfaces generativas com descrição de estado, hierarquia, componentes invisíveis e comandos de navegação, além de texto alternativo. Comparar taxa de alterações corretas e erros de edição com/sem descrições estruturadas.

**Riscos e limites.** Contexto de modelagem 3D, grupos pequenos, avaliação com respostas simuladas por humanos; não inferir eficácia em landing pages sem estudo específico. **Sem card:** acessibilidade editorial não corresponde às coleções comerciais.

## 4. ECHOES: personalização contextual baseada no motivo da pergunta

**Fonte:** Xiao et al., **26/10/2026**, *ECHOES: An Adaptive LLM-Powered AR Glasses Docent*, https://arxiv.org/abs/2610.34865.

**Descoberta e evidência.** Protótipo funcional de guia de patrimônio arquitetônico em óculos AR. Adapta a estrutura da resposta ao tipo de pergunta, mantém preferências de idioma e sinais de interesse ao longo da sessão e recupera referências documentais. Apresentação demonstrativa em Xangai; o resumo não fornece ensaio humano controlado de ganhos de UX.

**Mecanismo hipotético.** Adaptar a resposta à tarefa imediata pode reduzir explicações irrelevantes, enquanto contexto acumulado oferece continuidade sem exigir repetição.

**Produto e experimento.** Para demonstrações orientadas por IA, distinguir intenções “o que é?”, “como funciona?”, “qual evidência?” e “qual o próximo passo?”. Testar respostas estruturadas por intenção versus script fixo; medir acerto factual, compreensão e interrupções.

**Riscos e limites.** Protótipo sem demonstração causal de benefício; risco de inferir interesse indevidamente e de citações incorretas. **Sem card:** Adaptive UX e AR fora do catálogo.

## 5. Ataques divulgados podem ensinar tanto defesa quanto reutilização ofensiva

**Fonte:** Chen, Zheng e Zhu, submetido em **27/10/2026**, *What Victims Take Away*, https://arxiv.org/abs/2610.36422.

**Descoberta e evidência.** Análise observacional de 563.349 registros de ataques do jogo Tensor Trust. Ataques bem-sucedidos que vítimas haviam recebido foram reutilizados por elas em cerca de 10% dos casos na semana seguinte, contra 0,03% dos ataques comparáveis não recebidos. Alterações posteriores na defesa se associaram a 24,5 pontos percentuais menos sucesso de ataques da mesma família em comparação aos controles.

**Mecanismo.** Aprendizagem social orientada por resultados: observar uma técnica que funcionou facilita reprodução, ao mesmo tempo que uma falha sofrida pode motivar defesa.

**Produto e experimento.** Para o AI Hub, relatórios de red-team devem fornecer classe da vulnerabilidade, impacto, teste de regressão e correção, controlando a exposição irrestrita de payloads reutilizáveis. Testar capacidade de corrigir falhas sem aumentar exposição.

**Riscos e limites.** Ambiente de jogo e comparação observacional, sem causalidade generalizável para sistemas corporativos; preservação da capacidade de auditoria é necessária. **Sem card:** segurança de agentes não é coleção aceita.

## 6. Avaliar a entrega da IA requer múltiplas evidências, não somente o código produzido

**Fonte:** Shakiba, **26/10/2026**, *When AI Can Build It, What Should We Assess?*, https://arxiv.org/abs/2610.36413.

**Descoberta e evidência.** Redesenho de curso de engenharia de software em 13 semanas, com 87 estudantes e 17 equipes até a semana 11. O método distingue seis canais: produto, origem/processo, verificação, explicação, reflexão e garantia individual. Foram acompanhados 957 repositórios semanais individuais e 34 de equipes.

**Mecanismo interpretativo.** Um artefato excelente pode ser gerado por IA sem evidenciar que o responsável compreendeu o problema e sabe verificar a solução.

**Produto e experimento.** Para Têmis e AI Hub, avaliar entregas com checklist de origem, testes executados, critérios, justificativas e validação posterior, não apenas diffs e testes verdes. Medir defeitos não detectados e tempo de auditoria.

**Riscos e limites.** Estudo de implantação educacional sem comparação randomizada ou demonstração quantitativa de melhoria; não transferir taxas para produção empresarial. **Sem card:** governança de engenharia.

## 7. Transformações visuais precisam preservar correspondências compreensíveis

**Fonte:** Salako e Tominski, **27/10/2026**, *Science-Fiction Transformation as a Design Lens for Visualization Transformation*, https://arxiv.org/abs/2610.37131.

**Descoberta e evidência.** Ensaio de design com quatro transições hipotéticas entre representações visuais. Os autores distinguem propósito da transformação e correspondências 1:1, 1:N e N:1 entre elementos, destacando possível esforço interpretativo maior nas relações múltiplas.

**Mecanismo proposto.** Mudar de gráfico sem preservar referências pode dificultar rastrear que item anterior se tornou qual item posterior.

**Produto e experimento.** Em dashboards adaptativos, transições animadas com persistência de IDs, legenda e seleção mantida. Comparar navegação entre gráficos com/sem correspondência, medindo erros de interpretação e tempo.

**Riscos e limites.** Proposta conceitual, sem experimento humano que quantifique o benefício. **Sem card:** UX de visualização não integra o catálogo.

## 8. Colaboração homem–máquina: representar, prever e integrar ações

**Fonte:** Zhang et al., **27/10/2026**, *Understanding Human-Robot Collaboration through Joint Action*, https://arxiv.org/abs/2610.37298.

**Descoberta e evidência.** Revisão sistemática de 83 artigos empíricos de 2021 a 2026, organizados em nove conceitos e três capacidades: representação, previsão e integração das contribuições humanas e robóticas.

**Mecanismo proposto.** A coordenação depende de um modelo compartilhado do estado e dos papéis, de antecipação de ações e da integração das atividades.

**Produto e experimento.** Testar no AI Hub uma visualização de quem está responsável por cada etapa, qual evento o agente aguarda e como uma intervenção humana altera a execução. Medir conflitos, duplicidade e tarefas perdidas.

**Riscos e limites.** A revisão é sobre robótica; a aplicação a agentes de software é analogia arquitetural, não efeito experimental demonstrado. **Sem card.**

## 9. Emoção textual transformada em movimento visual: ideia experimental, não prazer demonstrado

**Fonte:** Nayak, **27/10/2026**, *Translating Poetic Emotion into Dynamic Mathematical Visuals*, https://arxiv.org/abs/2610.36698.

**Descoberta e evidência.** Proposta técnica que classifica emoções expressas em poemas com RoBERTa e as transforma em parâmetros de atratores matemáticos para imagens dinâmicas. O resumo não apresenta estudo com espectadores comprovando emoção percebida ou prazer.

**Mecanismo hipotético.** Mapear categorias semânticas a características formais de movimento pode produzir uma linguagem visual expressiva, mas classificar texto não equivale a captar a emoção da pessoa.

**Produto e experimento.** Para Apolo, prototipar fundos abstratos ajustáveis por ritmo/movimento e comparar com fundos neutros em teste cego de coerência perceptiva, distração e compreensão.

**Riscos e limites.** Sem validação humana de experiência, risco de estereótipos emocionais e de distração. **Sem card:** não há evidência suficiente de prazer audiovisual ou efeito comercial.

## Experience Engine v59

**Estado e intenção verificáveis → Apresentação adaptativa/acessível → Exploração visual mensurada → Preferência explícita → Ação autorizada → Verificação independente → Resultado comercial real.**

Prioridade comercial: separar o que chama atenção do que o consumidor escolhe e efetivamente compra. Prioridade de produto: personalização baseada na intenção atual e possibilidade de correção. Prioridade técnica: múltiplas evidências e rastreabilidade dos agentes.

## Decisão editorial e card

Guia vigente consultado na `main`: `harness-library-api/docs/guia-uso-api-cards.md` (SHA Git `2d38df09cdb01ad604c3edc88a57a22011c092d1`). Coleções aceitas: `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`.

Um novo candidato a DRAFT: **`atencao-vs-preferencia-anuncios-gerados-ia`**, coleção **`neuromarketing`**. A escolha é legítima porque o estudo é experimental, com anúncios e medições oculares/comportamentais. O card distingue a preferência de imagens em testes de laboratório de conversões comerciais não observadas.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-10-29-anuncios-ia-atencao-preferencia.md`. SHA-256 exato dos bytes UTF-8: `724ae1a127662ec9360db8ee7106e8e5b83e5c5112248e480e16939a1e70076b`.

JSON: `pesquisas/design-experiencia/cards/2026-10-29-anuncios-ia-atencao-preferencia.json`. Campos estritos do guia, `sourceKind=MARKDOWN`, `publishedOn=2026-10-29`, `validUntil=2026-12-13`. Arquivos representam candidatos a **DRAFT**. Não submeter para revisão, ativar ou arquivar.

Achados fortes sem card: AI-Support, Beyond Being Accurate, ECHOES, What Victims Take Away e as demais pesquisas de arquitetura, governança e UX exploratória; nenhuma coleção `design-experiencia` foi inventada.

## Fontes principais

- arXiv, listagem `cs.HC` mais recente às 08h de 29/10: https://arxiv.org/list/cs.HC/new
- Gonçalves et al., *Sensors*, 10/10/2026: https://doi.org/10.3390/s26206411
- Wang-Sun et al.: https://arxiv.org/abs/2610.34732
- Kim et al.: https://arxiv.org/abs/2610.35562
- Xiao et al.: https://arxiv.org/abs/2610.34865
- Chen et al.: https://arxiv.org/abs/2610.36422
- Shakiba: https://arxiv.org/abs/2610.36413
- Salako e Tominski: https://arxiv.org/abs/2610.37131
- Zhang et al.: https://arxiv.org/abs/2610.37298
- Nayak: https://arxiv.org/abs/2610.36698
