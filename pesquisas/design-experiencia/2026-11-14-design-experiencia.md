# Radar de Design de Experiência — 14/11/2026

**Experience Engine v75 · Marketing Hub**  
**Rodada:** sábado, 14/11/2026 (America/Sao_Paulo)  
**Situação editorial:** relatório e **1 candidato a DRAFT**; nenhuma submissão para revisão, ativação ou arquivamento.

## Recorte, método e critério de novidade

A última listagem diária regular de Human-Computer Interaction do arXiv consultável em 14/11 é a de **sexta-feira, 13/11/2026**, que mostra 29 submissões novas e 70 entradas incluindo cross-lists e revisões: https://arxiv.org/list/cs.HC/new . Foram revisados achados dessa listagem **não tratados no relatório de 13/11**. As datas individuais de depósito das pesquisas selecionadas são predominantemente 11–12/11, não 14/11. Para obter um experimento diretamente aplicável ao funil de compra e pouco explorado pelo radar, incorporou-se **um artigo revisado por pares de 04/02/2026** sobre haptics em comércio móvel, claramente rotulado como evidência complementar anterior, não notícia nova. Não se confundem resultados de pré-print, revisão bibliográfica, demonstração técnica, survey, experimento e resultado comercial.

**Síntese da rodada:** (1) acessibilidade semântica de interfaces também afeta agentes capazes de operar telas; (2) IA pode gerar *ilusão de entendimento* em usuários fora de seu domínio; (3) autorização de skills depende da autoridade conferida ao agente, não apenas de execução de código; (4) personalização multimodal deve respeitar limites de dados e atenção; (5) o feedback háptico em compras por AR tem efeitos diferentes conforme a necessidade de toque do consumidor. O quinto achado foi o único convertido em card, com coleção aderente `momentos-de-compra-b2c`.

## 1. Dívida de acessibilidade em dados usados para treinar agentes de interface

**Fonte:** Kraishan e Jitkajornwanich, *Trained on What Screen Readers Cannot Read*, submetido 11/11/2026, aceito para AISSA 2026 (evento previsto para dezembro). https://arxiv.org/abs/2611.16107

- **Descoberta/evidência:** em **17.373.614 elementos de interface** dos corpora Android Rico (capturas de 2017) e AndroidControl (capturas de 2024), entre elementos elegíveis para descrição acessível, a cobertura foi de **17,74%** e **57,27%**, respectivamente. Controles de entrada foram especialmente subdescritos: 2,20% e 27,21% (ou 30,99% ao contar hints como nomes no segundo corpus). Os autores examinaram também 26.205 ações e relataram viés de seleção para elementos com descrições entre gráficos tocáveis sem texto.
- **Mecanismo:** agentes de UI e leitores de tela dependem da semântica dos componentes; um botão sem nome pode ser visualmente óbvio, mas pouco interpretável por tecnologias assistivas ou automação estruturada.
- **Aplicação:** em produtos React/Android e harness de agentes, auditar rótulos acessíveis, nomes de controles, hierarquia e estados; priorizar a mesma árvore semântica para pessoas e automação.
- **Hipótese experimental:** telas com nomes de ações e estados completos diminuirão erros de agentes e leitores de tela, com redução de passos de correção.
- **Riscos/limites:** Rico e AndroidControl diferem em seleção, método e plataforma; **não** representam evolução longitudinal de acessibilidade. Cobertura semântica isolada não mede usabilidade real. **Sem card:** falta coleção de acessibilidade ou UI agents.

## 2. IA ajuda a explorar novas áreas, mas pode criar ilusão de conhecimento

**Fonte:** Panigrahi e August, *I don't know what I don't know*, submetido 11/11/2026, preprint. https://arxiv.org/abs/2611.15121

- **Descoberta/evidência:** survey com **62** pesquisadores interdisciplinares e **23 entrevistas**. Participantes descreveram ganhos para orientação inicial em domínios desconhecidos, mas também risco de entender superficialmente conceitos que não tinham expertise para contestar.
- **Mecanismo interpretativo:** fluência e síntese podem mascarar lacunas de conhecimento; a falta de repertório externo dificulta detectar falsas conexões.
- **Produto:** no AI Hub e nos relatórios do Marketing Hub, distinguir fonte primária, interpretação, incerteza e competência necessária para revisão; permitir triangulação.
- **Hipótese:** um agente que exige fontes independentes e perguntas de verificação reduzirá decisões mal fundamentadas em tarefas fora da especialidade do operador.
- **Riscos/limites:** percepções qualitativas não quantificam incidência de alucinação nem provam eficácia de uma intervenção. **Sem card:** não existe coleção para integridade de pesquisa e agentes.

## 3. Instalar uma skill é conceder autoridade ao agente

**Fonte:** Wen, Chinn e Bai, *An Experience Report on Teaching Students to Evaluate AI Agent Skills as Authorization Decisions*, submetido 11/11/2026, preprint para SIGCSE TS 2027. https://arxiv.org/abs/2611.15170

- **Descoberta/evidência:** alunos avaliaram três skills contendo fluxos úteis e permissões específicas (incluindo agir sem consentimento por operação). Em questionários pré/pós de **11 voluntários**, houve aumento de confiança autorrelatada para avaliação de skills; discussão qualitativa passou a considerar a autoridade que as instruções conferiam.
- **Mecanismo:** instruções em linguagem natural alteram comportamento e escopo de atuação sem parecerem código perigoso; revisão somente estática deixa riscos de autorização de fora.
- **Produto:** exibir permissões declarativas na instalação de skills (arquivos, rede, comandos, escrita, confirmação prévia), com *least privilege*, aprovação humana e histórico.
- **Hipótese:** interface de permissão por capacidade evitará concessões excessivas em comparação com simples leitura de README.
- **Riscos/limites:** exercício educacional, amostra de survey muito pequena, sem incidente real evitado. **Sem card:** governança de agentes fora das quatro coleções.

## 4. Projetar interações entre headset, computador e celular a partir da atenção

**Fonte:** Zhan et al., *MRxD: A Usage-Pattern Framework for Mixed-Reality Cross-Device Interaction*, submetido 12/11/2026, preprint. https://arxiv.org/abs/2611.16171

- **Descoberta/evidência:** revisão sistemática de **238 artigos** produziu seis padrões de uso distribuídos por atenção, relação entre tarefas e simetria cognitiva; workshop exploratório com **seis** pessoas avaliou utilidade do framework no design.
- **Mecanismo interpretativo:** transferir informação entre dispositivos pode reduzir ou ampliar alternância de atenção dependendo de como responsabilidades e estados são distribuídos.
- **Produto:** em experiências multimodais, definir explicitamente o papel de cada superfície, continuidade de sessão e estado compartilhado.
- **Hipótese:** handoff que preserve contexto e ação pendente reduzirá erros e retomadas em fluxos cruzados.
- **Riscos/limites:** framework de design, não experimento de ganho de produtividade. **Sem card:** cross-device UX sem coleção.

## 5. Interagir diretamente com representações internas de um modelo

**Fonte:** Huang et al., *Neural Representations as New Modes of Interactivity*, submetido 12/11/2026, trabalho conceitual. https://arxiv.org/abs/2611.17058

- **Descoberta/evidência:** propõe *Neural Representation Interaction* para inspeção e ajuste de conceitos internos, com espaço de design organizado por interação, alvo, referente e finalidade. Os próprios autores tratam aplicações em educação e modelagem pessoal como especulativas.
- **Mecanismo proposto:** a interface poderia expor abstrações manipuláveis em vez de obrigar o usuário a controlar todo comportamento por prompts.
- **Produto:** estudar visualizações experimentais e auditáveis de preferências/objetivos de agentes; evitar prometer controle direto confiável sobre todas as representações do LLM.
- **Hipótese:** edições explícitas de representações de preferências reduzirão desvios em tarefas repetidas se as alterações forem reais e reversíveis.
- **Riscos/limites:** não há teste clínico, de UX nem resultado comercial. Risco de falsa explicabilidade. **Sem card:** trabalho conceitual sem coleção válida.

## 6. Dados de comunidades exigem governança além do consentimento genérico

**Fonte:** Choksi et al., *Terms of Care: Designing for Participation in Data Governance by Disability Communities*, submetido 11/11/2026, **em revisão**. https://arxiv.org/abs/2611.15720

- **Descoberta/evidência:** pesquisa participativa com **três organizações** de defesa de pessoas com deficiência, usando entrevistas, webinar e atividade de licenças modulares; participantes valorizaram restrições de finalidade, contexto, salvaguardas e representação digna.
- **Mecanismo comportamental:** possibilidade de definir condições específicas de uso pode dar agência a comunidades em vez de forçar escolhas binárias de compartilhamento.
- **Produto:** para conjuntos de mídia de IA, documentar consentimento, público, finalidade, exclusões e responsável por revisão; permitir revogação onde aplicável.
- **Hipótese:** controles modulares aumentam compreensão de usos permitidos e reduzem incidentes de reutilização indevida.
- **Riscos/limites:** não foi demonstrada redução real de danos e monitorar licenças segue difícil. **Sem card:** governança de datasets não pertence legitimamente a `video` só por envolver mídia.

## 7. Áudio captado no ouvido como sensor contextual

**Fonte:** Han et al., *Hear What You Eat*, submetido 11/11/2026, preprint. https://arxiv.org/abs/2611.14878

- **Descoberta/evidência:** com **32 participantes**, o EARDIET reconhece episódios e texturas de mastigação usando microfone voltado para dentro do ouvido; relatou desempenho de detecção de episódios cerca de 10% superior a comparadores e F1 médio **85,97%** em identificação de distúrbio mastigatório durante validação clínica.
- **Mecanismo técnico:** sinais acústicos corporais podem indicar estado sem câmera; não equivalem a inferência confiável de emoção nem de intenção de compra.
- **Produto:** prototipar interação hands-free e feedback consentido em contextos assistivos, evitando vigilância contínua desnecessária.
- **Hipótese:** comandos contextuais sob ativação explícita poderão diminuir digitação sem perder privacidade.
- **Riscos/limites:** amostra pequena, sensibilidade biométrica e nenhuma prova de benefício para publicidade. **Sem card:** não cabe em coleção comercial/audiovisual atual.

## 8. Aparência de rigor em gráficos pode produzir credibilidade enganosa

**Fonte:** Dhawka e Lutz, *Data Visualization Influencers*, submetido 11/11/2026, preprint exploratório. https://arxiv.org/abs/2611.15042

- **Descoberta/evidência:** investigação de **17 criadores** de visualizações propagandísticas em discussões políticas no X. Os autores descrevem o uso da estética de autoridade estatística para favorecer narrativas seletivas.
- **Mecanismo interpretativo:** forma visual e credenciais aparentes podem servir como heurísticas de credibilidade, mesmo quando escolhas de dados e enquadramentos são parciais.
- **Produto:** exigir origem de séries, definições, datas, denominadores e advertências antes de gerar gráficos para conteúdo comercial ou relatórios.
- **Hipótese:** revisão separada de estética e veracidade aumentará a detecção de gráficos enganosos.
- **Riscos/limites:** estudo qualitativo e não experimental em política; não demonstrou efeito causal em consumidores. **Sem card:** integridade de dados/visualização não pertence automaticamente a `neuromarketing`.

## 9. Haptics em compras móveis: estímulo tátil ajuda alguns consumidores e incomoda outros

**Fonte complementar, anterior (não nova):** Racat, Reynolds e Obal, *Haptic Feedback and the Role of Need for Touch on Augmented Reality Mobile Shopping Apps*, *International Journal of Consumer Studies*. **Publicado 04/02/2026**, revisado por pares. https://doi.org/10.1111/ijcs.70189

- **Descoberta/evidência:** dois experimentos laboratoriais, **184 + 433 = 617** universitários. Em app móvel de AR para posicionar móveis, vibração se associou a melhores avaliações/intenções de compra entre pessoas com maior *need for touch* (NFT), enquanto níveis baixos de NFT produziram caminhos indiretos negativos. No Estudo 2, o caminho condicional indireto via confiança e utilidade percebida para intenção de compra em alta NFT foi **0,11**, IC 95% **[0,0117; 0,2426]**. Via confiança e entretenimento: **0,07**, IC 95% **[0,0058; 0,1759]**. Mediação via facilidade de uso não foi apoiada.
- **Mecanismo interpretativo:** vibração pode reforçar concretude e valor percebido para pessoas que valorizam toque; para outras, acrescenta distração. Essas mediações estatísticas não comprovam isoladamente causas psicológicas.
- **Produto:** tratar feedback tátil como preferência **opcional** em experiências AR de consideração de produto, com intensidade e desativação acessíveis; não ativar indiscriminadamente.
- **Experimento:** A/B fatorial com preferências declaradas alta/baixa NFT e vibração on/off; medir realização da tarefa, conforto, satisfação e eventos **reais** do funil (CTA, checkout, pagamentos reconciliados).
- **Riscos/limites:** intenção de compra **não é compra observada**; estudantes e móvel em AR não representam população geral nem checkout de produtos digitais comuns. Evitar extrapolar para consumidores sem AR.
- **Card:** `feedback-haptico-necessidade-toque-compras-ar` em **`momentos-de-compra-b2c`** pela aderência ao momento de exploração e avaliação de produto por consumidores. Fonte curta revisada separadamente, hash SHA-256 exato e JSON com 16 campos obrigatórios. Evidência externa complementar, descoberta agora pelo radar, não alegada como publicação de novembro.

## 10. Sensores ambientais também podem criar trabalho adicional para cuidadores

**Fonte:** Green et al., *"We Are the Dirty Hands": Reimagining an Ambient Intelligence Care System with Professional Caregivers*, submetido 12/11/2026, preprint. https://arxiv.org/abs/2611.16198

- **Descoberta/evidência:** dois workshops de especulação coletiva com **24 profissionais** (18 auxiliares de cuidado pessoal, seis auxiliares de enfermagem) identificaram tanto possíveis benefícios para documentar condições de trabalho quanto novos encargos e riscos de interpretação indevida de ações captadas.
- **Mecanismo:** sensores mudam o equilíbrio de visibilidade, autoridade e responsabilidade, não apenas a conveniência técnica do serviço.
- **Produto:** ao implementar ambientes inteligentes, incluir operadores humanos no design, identificar quem responde aos alertas e validar contexto antes de avaliações automatizadas.
- **Experimento:** em piloto autorizado, comparar carga de trabalho, falsos alertas e capacidade de corrigir interpretações com e sem revisão contextual.
- **Riscos/limites:** cenários especulativos, sem sistema implantado nem medição de melhoria. **Sem card:** UX de cuidado/ambient intelligence sem coleção.

## Priorização e experimentos

**Marketing Hub (alto valor para experimento):** testar haptics opcionais quando existir produto/experiência AR real de compra; sem esse caso de uso, não antecipar implantação. Investigar primeiro se o estímulo está facilitando compreensão ou simplesmente adicionando sensação.

**AI Hub (alto valor arquitetural):** fazer auditoria semântica de telas operadas por agentes e permissões declarativas para skills; associar controles e decisões a nomes acessíveis, rastros e confirmação adequada à criticidade. Separar melhoria de UX da simples fluência aparente do agente.

**Critério de conclusão:** taxas de acerto, tempos e satisfação não substituem decisões e resultados comerciais observados. Estudos qualitativos indicam hipóteses; dados de laboratório não garantem conversão.

## Decisões de cards

O guia vigente foi consultado na `main`: https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md . As únicas coleções aceitas continuam `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`.

**1 candidato DRAFT:** `feedback-haptico-necessidade-toque-compras-ar` → `momentos-de-compra-b2c`, pois há evidência experimental diretamente sobre interação sensorial na avaliação de produto e intenção declarada de compra. O card não declara aumento comprovado de vendas.

**Sem card apesar de relevância forte:** acessibilidade dos agentes, avaliação de permissões de skills, prevenção de ilusão de conhecimento, governança de mídia, visualização verificável, padrões cross-device e ambientes assistivos; nenhuma das quatro coleções é aderente sem deturpar o estudo. Nenhuma coleção `design-experiencia` foi criada.

**Fonte do card:** `pesquisas/design-experiencia/cards/fontes/2026-11-14-haptics-necessidade-toque-compras-ar.md`.  
**JSON do card:** `pesquisas/design-experiencia/cards/2026-11-14-haptics-necessidade-toque-compras-ar.json`.  
**Datas editoriais:** `publishedOn=2026-11-14`, `validUntil=2026-12-29`. O campo `publishedOn` registra a rodada editorial; o artigo original é explicitamente datado de 04/02/2026.

Nenhuma submissão a revisão, ativação ou arquivamento foi autorizada.
