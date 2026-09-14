# Radar de Design de Experiência — 2026-09-14

## Resumo executivo

A rodada de 14 de setembro de 2026 reforça uma ideia central: **experiências adaptativas melhores não maximizam intervenção, naturalidade ou automação; elas calibram timing, contexto compartilhado, explicação e preservação da agência humana**.

Os achados mais úteis de hoje foram:

1. um ensaio microrandomizado com 160 participantes mostra que prompts adaptativos podem produzir mudança comportamental quando enviados em pontos de decisão elegíveis;
2. um novo preprint com cerca de 101 mil ações propõe medir “convenções implícitas” e sugere que compatibilidade com convenções humanas pode importar mais que desempenho IA–IA;
3. um estudo de usabilidade clínica publicado na Nature Medicine mostra que XAI melhorou parte do desempenho humano, mas também fez pessoas seguirem sugestões erradas — explicabilidade não elimina automation bias;
4. um artigo publicado hoje sobre autoria com GenAI argumenta, por meio de três casos ilustrativos, que voz e identidade precisam ser reintroduzidas ativamente pelo humano durante a co-criação;
5. uma interface conversacional de XAI teve excelente aceitação entre três especialistas, mas o próprio estudo sugere preservar visão gráfica global: conversa e dashboard parecem complementares;
6. um estudo peer-reviewed de affective design em robótica mostra uma abordagem para mapear descritores emocionais em padrões de movimento — uma inspiração para microinterações, embora ainda distante de UX digital comum;
7. o benchmark JarvisGUI mostra que agentes de GUI quebram principalmente quando precisam preservar estado e dependências entre dispositivos, chegando perto de zero em tarefas com quatro ou mais subtarefas.

---

## 1. Proatividade funciona melhor quando existe um ponto de decisão elegível

**Descoberta.** O artigo *Unlocking action and well-being: how just-in-time adaptive exercise prompts shape happiness through reduced sedentary time and greater behavioral consistency*, publicado em 13 de setembro, oferece uma evidência experimental útil para adaptive UX e behavioral design.

**Evidência.** O estudo usou um desenho microrandomizado com 160 mulheres adultas em uma intervenção de smartphone de oito semanas, após sete dias de run-in. Em pontos de decisão considerados elegíveis, a pessoa era randomizada para receber um prompt adaptativo de exercício ou nenhum prompt. Os prompts aumentaram significativamente os passos nos 30 minutos seguintes e a probabilidade de quebrar um período sedentário. Ao longo da intervenção, aumentaram passos e consistência comportamental e caiu o tempo sedentário; satisfação com a vida, bem-estar e autoeficácia também melhoraram entre baseline e pós-teste, com grande parte dos ganhos mantida em follow-up. Os próprios autores alertam que os modelos de mediação dependem de pressupostos temporais e de ausência de confundimento não medido.

**Mecanismo psicológico/comportamental.** O valor da intervenção não parece vir apenas de “lembrar” a pessoa, mas de fazer isso em uma janela na qual a ação é viável. Isso sugere tratar proatividade como uma política de elegibilidade: `há oportunidade real de ação agora?`.

**Implicação para produto.** Em um agente, o equivalente seria distinguir `descoberta relevante` de `momento adequado para interromper`. Um **Eligibility Engine** poderia considerar bloqueio detectado, próxima ação clara, custo da interrupção, urgência e possibilidade de recusa.

**Hipótese/experimento.** Comparar três políticas: proatividade contínua, proatividade apenas em pontos elegíveis e assistência somente sob demanda. Medir conclusão de tarefa, abandono, dismissals, tempo até ação e satisfação.

**Riscos/limites.** O domínio é atividade física e a amostra incluiu apenas mulheres adultas. O efeito causal sobre passos não prova efeito sobre conversão, retenção ou CTA em produto comercial.

Fonte: https://link.springer.com/article/10.1186/s41043-026-01433-4

**Card atualizado:** reutilizado `proatividade-informacional-orcamento-atencao`, coleção `neuromarketing`, combinando esta nova evidência experimental com a evidência anterior sobre custo de excesso informacional.

---

## 2. Humanos cooperam por convenções implícitas — e agentes podem precisar aprender esse “idioma local”

**Descoberta.** O preprint *The Convention Gap: Towards Measuring Implicit Communication in Cooperative AI Evaluation*, submetido em 10 de setembro e revisado em 11 de setembro, tenta medir comunicação que existe além da mensagem literal.

**Evidência.** Os autores reanalisaram aproximadamente 101 mil ações de três datasets de Hanabi. O convention gap — diferença entre a falha esperada apenas pela informação literal e a falha observada — foi de +26,2 pontos percentuais em pares humanos, -0,7 em pares IA–IA e +16,4 em pares humano–IA. Em cartas humanas sem pistas literais, o gap chegou a +46 pontos. Entre três parceiros de IA, a probabilidade de falha prevista pelo conteúdo literal era parecida (38%–41%), mas a falha humana real variou de 14,4% a 34,4%; o parceiro com maior convention gap teve menos falhas humanas. Em uma checagem controlada com Off-Belief Learning, o gap cresceu de +1,6 para +21,7 pontos conforme o conteúdo convencional aumentava.

**Mecanismo psicológico/comportamental.** Colaboração humana comprime comunicação. Depois de um histórico compartilhado, certas expressões, omissões e referências passam a carregar significado sem precisar ser reexplicadas.

**Implicação para produto.** Um customer-agent pode ter uma **Convention Memory** separada da memória factual: abreviações, padrões de referência e preferências operacionais só entram nessa memória quando foram confirmados ou repetidos. Em ações ambíguas ou de maior consequência, a convenção não substitui confirmação.

**Hipótese/experimento.** Comparar agente estritamente literal versus agente que reutiliza convenções confirmadas. Medir reexplicações, correções, tempo da tarefa e interpretações erradas.

**Riscos/limites.** O domínio é um jogo formal e o trabalho é preprint. Em linguagem natural aberta, inferir uma convenção inexistente pode aumentar erros confiantes, falsa familiaridade e problemas de privacidade.

Fonte: https://arxiv.org/abs/2609.11489

**Card criado:** `convencoes-implicitas-compatibilidade-humano-agente`, coleção `neuromarketing`.

---

## 3. Explicabilidade pode melhorar decisão e, ao mesmo tempo, aumentar adoção de erro

**Descoberta.** Um artigo publicado em 13 de setembro na *Nature Medicine* avaliou uma ferramenta de apoio à decisão com XAI em câncer de pulmão e produziu um resultado especialmente relevante para Human-AI Interaction: mostrar explicação pode melhorar o desempenho médio sem imunizar o humano contra sugestão errada.

**Evidência.** Vinte médicos — dez especialistas em câncer de pulmão e dez não especialistas — avaliaram dez casos cada, totalizando 200 avaliações. Primeiro decidiram sem apoio do modelo; depois receberam a saída do modelo junto de explicações globais e locais baseadas em SHAP. Para previsão de resposta ao tratamento, a sensibilidade subiu de 0,72 para 0,87 e a acurácia de 0,57 para 0,65. A concordância entre especialistas e não especialistas subiu de κ=0,11 para κ=0,48. Porém, os médicos frequentemente seguiram sugestões corretas da XAI (74,5%) e também seguiram sugestões incorretas: 72,2% entre especialistas e 63,6% entre não especialistas.

**Mecanismo psicológico/comportamental.** Uma explicação aumenta legibilidade da recomendação, mas legibilidade não é veracidade. Quando a explicação torna uma recomendação mais coerente e fácil de justificar, ela pode facilitar tanto aceitação correta quanto automation bias.

**Implicação para produto.** O design deveria medir separadamente `compreensão`, `confiança`, `aceitação apropriada` e `aceitação indevida`. Quando humano e IA discordarem, o sistema pode ativar um **Disagreement Check** que apresenta evidência contrária ou solicita uma segunda verificação em vez de apenas tornar a recomendação mais persuasiva.

**Hipótese/experimento.** Injetar propositalmente uma fração pequena de recomendações erradas em ambiente de teste e comparar explicação simples versus explicação + contraevidência/checagem. Medir taxa de aceitação do erro e correção humana.

**Riscos/limites.** É um contexto médico altamente específico e de alto risco. O estudo não deve ser usado como evidência direta para marketing; ele serve como alerta de HAI sobre confiança calibrada.

Fonte: https://www.nature.com/articles/s41591-026-04488-2

**Card não criado.** O achado é forte, mas o contexto clínico e a natureza de segurança/decision support não se encaixam de forma limpa nas coleções atuais sem distorção.

---

## 4. Co-criação com GenAI precisa preservar autoria, não apenas “personalizar estilo”

**Descoberta.** O artigo *Authorial voice in human–machine writing with GenAI: plays and counterplays of meaning*, publicado hoje, 14 de setembro, examina como voz autoral é negociada durante escrita com ChatGPT 4.5.

**Evidência.** Trata-se de uma contribuição conceitual com três casos ilustrativos e critical walkthrough, não de um experimento de usuários. Três pesquisadoras iteraram prompts sobre o mesmo tema de escrita e analisaram como a voz mudava segundo quatro princípios: intertextualidade, reflexividade dialógica, multimodalidade e embodiment. O artigo mostra que respostas iniciais tendiam a ser amplas, genéricas e decontextualizadas; novas rodadas precisavam introduzir referências, intenção, audiência, posicionamento e experiência do autor. Os autores argumentam que, mesmo com prompting mais sofisticado, estilo e “flair” continuam dependentes de intervenção humana.

**Mecanismo psicológico/comportamental.** Identidade e autoria não são apenas parâmetros de tom. Elas emergem de escolhas: quais vozes são incluídas, a quem se responde, quais experiências são relevantes e qual posição o autor assume diante do material.

**Implicação para produto.** Em ferramentas criativas, um **Authorial Intent Ledger** pode ser mais útil que um simples slider de “formal/leve”: público, objetivo, posição, referências desejadas, referências recusadas, elementos pessoais e limites. A IA gera dentro desse espaço, mas o humano continua editando o próprio loop.

**Hipótese/experimento.** Comparar geração baseada apenas em prompt de estilo com geração baseada em intent ledger editável. Medir correções, percepção de autoria, sensação de controle e distância entre primeira e última versão.

**Riscos/limites.** É um artigo conceitual e educacional com três casos ilustrativos; não demonstra que essa arquitetura melhora métricas de produto.

Fonte: https://link.springer.com/article/10.1007/s13384-026-01018-4

**Card não criado.** O achado é útil para ferramentas criativas, mas a evidência ainda é conceitual e não justifica uma regra persistente nas quatro coleções atuais.

---

## 5. Conversa e dashboard podem ser modalidades complementares, não rivais

**Descoberta.** O preprint *Explainability Assistant* compara uma interface conversacional para análise de modelos com um dashboard tradicional.

**Evidência.** Na parte técnica, Gemini 2.5 Flash atingiu 94%/93% de exact-match na interpretação de intents em dois conjuntos de 100 exemplos; outros modelos ficaram abaixo. Na validação humana, apenas três especialistas de energia — todos com 10–20 anos de experiência — usaram as duas interfaces. A acurácia das tarefas foi 93% no dashboard e 100% no assistente, diferença não estatisticamente sustentada com n=3. Todos avaliaram o assistente com nota máxima ou próxima disso em facilidade de uso e intenção de reutilização, mas foram mais cautelosos em confiança e compreensão. Os próprios especialistas apontaram que o dashboard ainda era útil para visão global.

**Mecanismo psicológico/comportamental.** Conversa reduz o custo de formular uma operação analítica específica; visualização persistente ajuda orientação espacial e visão de conjunto. São formas diferentes de externalizar cognição.

**Implicação para produto.** Em vez de substituir dashboards por chat, usar **hybrid analytical UI**: conversa para perguntas locais, what-if e ações; visão gráfica persistente para estado global, tendências, comparação e memória visual.

**Hipótese/experimento.** Comparar chat-only, dashboard-only e híbrido em tarefas de exploração. Medir tempo, erros, navegação perdida, confiança calibrada e número de perguntas necessárias.

**Riscos/limites.** A validação humana tem apenas três participantes e ordem fixa das interfaces; o próprio artigo reconhece possibilidade de learning effect.

Fonte: https://arxiv.org/abs/2609.11860

**Card não criado.** A amostra humana é pequena demais para transformar a preferência observada em orientação persistente.

---

## 6. Movimento pode funcionar como uma camada semântica de emotional design

**Descoberta.** *A semantic–kinematic mapping framework for user-centered transfer-assist nursing robot design*, publicado em 13 de setembro na *Scientific Reports*, trata movimento como algo que também comunica qualidades afetivas.

**Evidência.** O processo construiu um léxico dinâmico de expectativas afetivas, agrupou descritores, relacionou-os a padrões de movimento e extraiu contornos cinemáticos. O caso incluiu nove descritores semânticos dinâmicos e 140 keyframes, seguido de avaliação estrutural e uma avaliação preliminar de usuários geralmente positiva. Os próprios autores classificam o resultado como evidência preliminar de viabilidade, não como validação comparativa do framework.

**Mecanismo psicológico/comportamental.** Pessoas atribuem qualidades como cuidado, estabilidade, firmeza ou suavidade não apenas à aparência estática, mas à trajetória, velocidade, pausa e coordenação do movimento.

**Implicação para produto.** Para microinterações e agentes visuais, motion tokens poderiam deixar de ser puramente técnicos (`duration=200ms`, `ease-out`) e ganhar intenção semântica: `calmo`, `urgente`, `confiante`, `delicado`. Depois, cada semântica seria mapeada para padrões visuais e testada com usuários.

**Hipótese/experimento.** Manter conteúdo e layout constantes e alterar somente parâmetros de movimento associados a duas intenções semânticas. Medir interpretação afetiva, atenção, erro e preferência.

**Riscos/limites.** É robótica assistiva, não UI digital comum; a avaliação é preliminar e não demonstra transferência para animações de tela.

Fonte: https://www.nature.com/articles/s41598-026-71082-4

**Card não criado.** A extrapolação para criativos ou interfaces do Marketing Hub ainda é longa demais.

---

## 7. Para agentes de GUI, continuidade de estado é uma feature de UX — não apenas infraestrutura

**Descoberta.** O benchmark *JarvisGUI*, submetido em 9 de setembro, avalia agentes que precisam completar workflows entre Android, Windows e Ubuntu.

**Evidência.** O benchmark contém 118 tarefas atômicas e 150 tarefas compostas, estas divididas em dependências no mesmo dispositivo, tarefas multi-device independentes e tarefas multi-device dependentes. Nos modelos avaliados, o sucesso total em tarefas multi-device dependentes ficou entre 0% e 2% na maior parte dos casos; tarefas com quatro ou mais subtarefas caíram para sucesso próximo de zero em todos os agentes. O erro típico era tratar uma dependência não executada — transferência de arquivo, rename, login ou estado intermediário — como se tivesse sido concluída, gerando cascata de erros.

**Mecanismo psicológico/comportamental.** Para o usuário, a experiência parece contínua (“continue de onde estava”), mas o agente precisa representar explicitamente estado, dependência e handoff. Sem isso, a interface cria falsa continuidade.

**Implicação para produto.** Um agente multi-superfície deveria possuir **State Transfer Contract**: artefato produzido, localização, versão, plataforma de origem, plataforma destino, pré-condições e confirmação de chegada. O agente não marca a subtarefa como concluída até o estado final ser verificável.

**Hipótese/experimento.** Em workflows multi-app, comparar memória conversacional simples contra estado estruturado e verificado. Medir subtarefas omitidas, ações redundantes e falhas em cadeia.

**Riscos/limites.** É benchmark de agentes open-source em VMs; não mede experiência subjetiva de usuários e não representa todos os sistemas comerciais.

Fonte: https://arxiv.org/abs/2609.10451

**Card não criado por falta de coleção válida.** É um achado arquitetural de harness/GUI agents e não cabe honestamente em `video`, `prazer-audio-visual`, `neuromarketing` ou `momentos-de-compra-b2c`.

---

## Experience Engine v14

A síntese desta rodada acrescenta quatro controles que aparecem repetidamente nas evidências recentes:

```text
usuário
   ↓
intenção + contexto + estado
   ↓
ATTENTION ELIGIBILITY
   ├── é relevante?
   ├── é acionável agora?
   └── vale interromper?
   ↓
CONVENTION MEMORY
   ├── literal
   ├── padrão confirmado
   └── confirmar quando ambíguo
   ↓
AUTHORIAL / HUMAN INTENT
   ↓
INTERFACE ROUTER
   ├── conversa para consulta/ação local
   └── visual persistente para visão global
   ↓
EXECUÇÃO
   ↓
STATE TRANSFER CONTRACT
   ↓
EXPLANATION + DISAGREEMENT CHECK
   ↓
resultado
   + compreensão
   + autonomia
   + confiança calibrada
```

O princípio mais importante de hoje é: **o agente precisa saber não apenas “o que o usuário quer”, mas quando vale intervir, quais convenções já foram realmente estabelecidas e quais estados precisam ser verificados antes de continuar**.

---

## Cards desta rodada

### 1. `proatividade-informacional-orcamento-atencao` — nova versão

Coleção: `neuromarketing`.

O card existente de 09/09 foi atualizado porque a nova evidência não cria uma ideia diferente; ela resolve uma lacuna importante. Antes tínhamos principalmente o lado negativo — excesso de encontros informacionais associado a burnout. Agora há evidência experimental de que prompts proativos, quando vinculados a pontos de decisão elegíveis, podem mudar comportamento no curto prazo. A regra passa a ser **proatividade seletiva e temporizada**, não simplesmente “falar menos”.

Fonte revisada:
`pesquisas/design-experiencia/cards/fontes/2026-09-14-proatividade-informacional-orcamento-atencao.md`

SHA-256:
`418954521d85381af5cbd3fb81e64cb0a1a3d8a838a0280f612324bd2517734d`

JSON:
`pesquisas/design-experiencia/cards/2026-09-14-proatividade-informacional-orcamento-atencao.json`

### 2. `convencoes-implicitas-compatibilidade-humano-agente` — novo

Coleção: `neuromarketing`.

É importante para o customer-agent porque transforma “memória” em algo mais específico: **convenções compartilhadas que reduzem esforço de comunicação**. O card mantém explicitamente a distinção entre evidência do benchmark, hipótese interpretativa e aplicação comercial ainda não validada.

Fonte revisada:
`pesquisas/design-experiencia/cards/fontes/2026-09-14-convencoes-implicitas-compatibilidade-humano-agente.md`

SHA-256:
`a1b208f6988a022db41ac6df4bf96d05bfd8d2ee2a5dfd101bc0c9a0e19db74e`

JSON:
`pesquisas/design-experiencia/cards/2026-09-14-convencoes-implicitas-compatibilidade-humano-agente.json`

Os dois arquivos permanecem candidatos a `DRAFT`; nenhuma etapa de revisão, ativação ou arquivamento foi executada.
