# Radar de Design de Experiência — 21/11/2026

**Escopo:** UX, emotional/behavioral design, interfaces generativas e adaptativas, atenção, multimodalidade, voz, experiências audiovisuais e interação com agentes de IA.

**Seleção:** oito pesquisas ainda não tratadas nos relatórios de 11–20/11. A atualização regular mais recente do arXiv é a de **sexta-feira, 20/11**, pois esta rodada ocorre no sábado. Artigos arXiv depositados em 18–19/11 aparecem na listagem de 20/11. Incluí dois trabalhos anteriores de 2026 pela contribuição empírica específica, identificando as datas originais. Preprint não equivale a revisão por pares. O [guia vigente da API de cards](https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md) foi consultado antes da geração de cards.

## 1. Sonic Palette: associar paletas de cor à evolução da música

**Fonte:** Wei, Yang e Lin. *Sonic Palette: Predicting Dynamic Color Themes from Music*. [arXiv 2611.25781](https://arxiv.org/html/2611.25781v1). Submetido em **19/11/2026**.

- **Descoberta/evidência:** modelo Transformer autorregressivo prevê cinco cores dominantes por quadro usando características musicais e cores anteriores. Dados de **265 videoclipes (18,5 h, 1,7 milhão de quadros)**, com 225 vídeos para treino, 20 validação e 20 teste. Em 20 vídeos de teste, discrepância média de bins cromáticos **ACBD 3,5161**, versus **3,8853** do Transformer não autorregressivo e **4,6757** da MLP de paleta fixa (menor é melhor). Para variação temporal ATBD, **0,8671** versus **1,0068** e **0,9959**, respectivamente. **26 avaliadores** atribuíram melhor naturalidade das transições e correspondência percebida entre música e atmosfera cromática ao método dinâmico; os autores não publicam no corpo textual percentuais de vitória para cada comparação.
- **Mecanismo interpretativo:** uma continuidade cromática ajustada à evolução sonora pode aumentar a *congruência percebida* entre modalidades; pesquisas anteriores sugerem mediação por emoção, mas este trabalho não isolou causalmente um mecanismo psicológico, tampouco mediu dopamina ou prazer.
- **Implicação para Marketing Hub:** experimentar animação de cor e iluminação de vídeos criativos sincronizada a trilhas com licença, preservando identidade visual, legibilidade e acessibilidade. Critério de produção: suavidade e adequação, não estímulos a qualquer custo.
- **Hipótese/experimento:** comparar vídeo com paleta estática, transições aleatórias e transições dinâmicas guiadas pela música; cegamente avaliar correspondência subjetiva, desconforto, retenção, reconhecimento da marca e, separadamente, CTA/checkout.
- **Riscos/limites:** treinamento em videoclipes com escolhas estéticas deliberadas, possível viés de estilo e questões de direitos; teste com somente 20 vídeos e 26 avaliadores; sem demonstração de eficácia publicitária, prazer fisiológico ou aumento de vendas.
- **Card candidato:** paletas-cromaticas-dinamicas-alinhamento-musical → **prazer-audio-visual**.

## 2. Epira: regras explícitas aumentam a integridade de entrevistas feitas por LLM

**Fonte:** Asnani et al. *Design and Evaluation of Epira*. [arXiv 2611.25577](https://arxiv.org/html/2611.25577v1). Submetido em **19/11/2026**.

- **Evidência:** o sistema combina diálogo com LLM, registro estruturado editável e controlador determinístico do questionário. **108 casos simulados**, expandidos a **864 sessões**, e piloto com **72 voluntários** em entrevistas de texto. Com modelo local, recuperou **88,9% dos campos obrigatórios** e **60,1% dos contatos verdadeiros** dos cenários; entrevistas abertas com os mesmos modelos capturaram **31,1 a 41,9 pontos percentuais menos campos**. No piloto humano: **100% de conclusão** e SUS adaptado médio **82,95/100**.
- **Mecanismo técnico/comportamental:** conversação ajuda a esclarecer respostas, mas o controlador delimita a iniciativa e conserva os campos que devem ser preenchidos; mostrar o registro ao lado do chat torna correções visíveis.
- **Aplicação:** entrevistas de briefing comercial e coleta de requisitos do AI Hub: conversa livre só dentro de etapas e campos obrigatórios, controle de avanço, confirmação editável de informações e passagem a humano para exceções.
- **Hipótese/experimento:** comparar chat livre, formulário fixo e entrevista guiada; medir campos completos e corretos, taxa de retrabalho, abandono e SUS.
- **Limites:** casos clínicos fictícios, não validação clínica; o estudo humano **não testou voz**, apesar de a interface oferecer esse recurso. Não transferir métricas de saúde diretamente para vendas. **Sem card:** coleção de UX para entrevistas/agentes não existe no catálogo.

## 3. Ground truth errado pode criar falsos ganhos atribuídos à IA

**Fonte:** Baltay et al. *Ground Truth Bias in AI-Assisted Medical Annotation*. [arXiv 2611.25924](https://arxiv.org/abs/2611.25924). Submetido em **19/11/2026**.

- **Evidência:** **51 estudantes**, **50 radiografias**, desenho cruzado. Frente a etiquetas do repositório, assistência elevou acurácia de **60,4% para 65,3%** (p=0,002); frente ao painel independente, nos 27 casos classificados como determinados, foi de **78,4% para 80,8%** (p=0,178). O painel considerou **23 de 50 imagens indeterminadas**.
- **Mecanismo:** a noção de sucesso herdada de uma referência imperfeita pode premiar a conformidade com um rótulo errado, não uma decisão verdadeira.
- **Aplicação:** medir qualidade de revisões dos agentes contra auditoria independente, permitir categoria **indeterminado** e não premiar simplesmente concordância com o primeiro avaliador.
- **Hipótese/experimento:** auditar amostra cega de correções de Têmis/AI Hub com dois revisores e consenso; comparar métricas antes e depois da adjudicação.
- **Limites:** imagens médicas e anotadores sem experiência clínica; sem prova de melhoria em QA de produtos. **Sem card:** avaliação de ground truth e revisão técnica não são nenhuma das quatro coleções.

## 4. Interfaces e nudges podem desviar a escolha de agentes de compras

**Fontes:** Halimeh et al., *A Dual-Process Perspective on Nudge Susceptibility in LLM-Based GUI Agents*, [arXiv 2609.19843](https://arxiv.org/abs/2609.19843), depositado em **17/09/2026**; Tang et al., *Dark Patterns Meet GUI Agents*, [CHI 2026](https://doi.org/10.1145/3772318.3791568), publicado em **13/04/2026**. São estudos anteriores identificados agora como complementares ao foco B2C; não se confundem com o estudo de 18/11 sobre confiança verbal e acurácia.

- **Evidência:** Halimeh et al. descrevem experimento randomizado de compras simuladas com **3.600 agentes** e **21.600 simulações**, seis modelos: maior esforço de raciocínio reduziu suscetibilidade a padrões automáticos de opções padrão, mas elevou suscetibilidade a influência social reflexiva. Tang et al. examinaram seis agentes em **16 tipos de dark patterns**, além de experimento com **22 pessoas** com/sem supervisão; supervisão reduziu várias falhas, mas introduziu visão estreita e carga cognitiva.
- **Mecanismo interpretativo:** agentes perseguem finalização da tarefa e podem tratar sinal de popularidade, opção marcada ou urgência como informação legítima, mesmo quando conflita com requisitos de compra. Mais raciocínio não é defesa universal.
- **Aplicação:** ao automatizar comparações e compras, verificar preço total, assinatura recorrente, consentimentos, atributos e fontes de alegações de escassez/popularidade **fora da hierarquia visual da página**; exigir confirmação humana focada nas consequências.
- **Hipótese/experimento:** simular lojas com/sem defaults enganosos e prova social falsa; comparar agentes com checklist de compra verificado externamente contra agentes de raciocínio alto sem checklist; medir compras indevidas e rejeições corretas.
- **Limites:** navegadores, páginas e modelos em cenários controlados, sem mensuração generalizada de compras reais ou consumidores; viés de modelo e regras. A aplicação é defensiva, nunca uma recomendação para explorar fragilidades do comprador.
- **Card candidato:** nudges-interface-desvio-agentes-compra-ia → **momentos-de-compra-b2c**.

## 5. Open Response Explorer: comparação por unidade de ideia

**Fonte:** Gu, Chen e Glassman. *Open Response Explorer*. [arXiv 2611.25169](https://arxiv.org/abs/2611.25169). Submetido em **18/11/2026**.

- **Evidência:** estudo intraindivíduo com **12 participantes** sobre uma interface que estrutura textos abertos em unidades de ideia e permite comparar respostas de colegas com destaques de função textual.
- **Mecanismo:** separar argumentos por propósito pode facilitar comparação e reduzir esforço de encontrar diferenças em parágrafos livres.
- **Aplicação:** organizar propostas de agentes e resultados de pesquisa do AI Hub em alegação, evidência, alternativa e decisão, mantendo link para texto integral.
- **Teste:** comparar relatórios livres versus relatórios estruturados para detectar divergências e fontes ausentes; medir acertos e custo da revisão.
- **Limite:** escala pequena em ambiente educacional, sem prova de melhoria da qualidade comercial. **Sem card:** UX de escrita colaborativa fora das coleções.

## 6. Atenção visual em materiais educativos: uma média não descreve cada pessoa

**Fonte:** Weng et al. *Modeling Learner-Technology Interaction Through Multilevel Gaze Transition Dynamics*. [arXiv 2611.25294](https://arxiv.org/abs/2611.25294). Submetido em **18/11/2026**.

- **Evidência:** somente **sete gravações de rastreamento ocular**, com **três grupos focais**; diferenças individuais em alternância de olhar e navegação por slides, além de relatos de confusão sobre avaliação e instruções.
- **Mecanismo interpretativo:** esforço de localização e retomada da leitura pode variar, mesmo quando a sequência geral de navegação é compartilhada.
- **Aplicação/teste:** avaliar tutoriais pelo sucesso de encontrar elementos e executar tarefas, não inferir microemoção de uma fixação. Testar instruções visuais, progresso e âncoras.
- **Limites:** exploratório, amostra muito pequena, sem benefício comprovado de personalização. **Sem card** pela ausência de evidência aplicável às coleções aceitas.

## 7. Visualização com privacidade diferencial precisa preservar a leitura relevante

**Fonte:** Yang et al. *Rethinking Utility in Differentially Private Visualization*. [arXiv 2611.26396](https://arxiv.org/abs/2611.26396). Submetido em **19/11/2026**; trabalho associado à IEEE VIS 2026.

- **Descoberta/evidência:** síntese conceitual de pesquisas sobre como ruído de privacidade interage com o canal visual (posição, comprimento, cor) e a operação que a pessoa tenta realizar. Os autores propõem uma hipótese canal–leitura–mecanismo; **não** reportam novo ensaio com consumidores.
- **Mecanismo:** com dados perturbados para proteção, uma codificação visual pode amplificar ou atenuar percepção equivocada sobre padrões e diferenças.
- **Aplicação/teste:** dashboards de marketing com dados agregados devem avaliar se comparações reais continuam interpretáveis sob ruído; testar leitura objetiva por diferentes gráficos e informar intervalos e restrições.
- **Limites:** hipótese de framework a validar, não evidência de aumento de satisfação nem vendas. **Sem card** por tratar de visualização e privacidade.

## 8. Adaptar interfaces/sinais apenas quando há ganho justificável

**Fonte:** Musienko. *When Should Deployed BCI Decoders Adapt?* [arXiv 2611.25315](https://arxiv.org/abs/2611.25315). Submetido em **19/11/2026**.

- **Evidência:** trabalho de decodificadores de EEG mostra que a política de adaptação automática pode gastar recursos sem necessidade: um mecanismo referenciado a dados limpos reduziu entradas de adaptação **de 0,56 para 0,11** em sessões do tipo campo; sob estresse sintético, cabeças leves foram competitivas com ajuste completo usando cerca de **23 vezes menos tempo de treinamento**.
- **Mecanismo técnico:** a decisão de personalizar deve depender de desvio real e custo, não só de capacidade técnica.
- **Aplicação/teste:** em UI adaptativa do AI Hub, registrar motivo de cada alteração automática e permitir retorno; testar ganho real por mudança versus estabilidade padrão.
- **Limites:** extrapolação puramente inspiracional de BCI/EEG para UX; sem teste em interfaces de produto. **Sem card** pela ausência de coleção e aderência empírica.

## Síntese editorial e rastreabilidade

**Cards candidatos a DRAFT (sem submissão editorial/ativação):**
1. **Paletas cromáticas dinâmicas guiadas pela música**, coleção **prazer-audio-visual**. Útil para experimentar congruência audiovisual, não para afirmar prazer ou conversão.
2. **Defesa de agentes compradores contra nudges de interface**, coleção **momentos-de-compra-b2c**. Útil para proteger requisitos e consentimento, não para maximizar manipulação.

**Sem cards apesar da força dos achados:** Epira (entrevistas e workflow de agentes) e Ground Truth Bias (verificação/qualidade) não se encaixam nas coleções atuais. Os estudos de estruturação textual, olhar, privacidade gráfica e adaptação de BCI também permanecem apenas no relatório.

**Política:** somente quatro coleções permitidas pelo guia atual, fontes curtas e SHA-256 dos bytes exatos; os contratos versionados são candidatos a DRAFT, sujeitos à validação e aprovação humana. Não foi feita autorização de revisão, ativação ou arquivamento.
