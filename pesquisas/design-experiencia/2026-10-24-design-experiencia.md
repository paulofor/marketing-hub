# Radar de Design de Experiência — 24 de outubro de 2026

**Experience Engine v54 | Marketing Hub | Rodada de 24/10/2026**

## Escopo, recência e síntese

A listagem do arXiv Human-Computer Interaction em 24/10 continua mostrando o lote de sexta-feira, **23/10/2026** (37 entradas, sendo 18 novas na categoria). As fontes selecionadas nesta rodada foram principalmente submetidas em **22/10**; o trabalho *Data-Void Exploits* foi submetido em **21/10** e entrou como cross-list. Não são estudos realizados em 24/10 nem, por si, publicações com revisão por pares. Listagem: https://arxiv.org/list/cs.HC/new.

**Conclusão:** experiências com IA precisam mostrar a confiabilidade de afirmações individuais, permitir que o usuário conteste sugestões e oferecer representações manipuláveis do trabalho. Fluência e confiança aparente podem coexistir com erros. O resultado tecnicamente convincente não implica entendimento, autorização ou sucesso comercial. Foram priorizados dez trabalhos ainda não destacados nas rodadas de 14 a 23/10.

### 1. Confiança indevida em afirmações de IA — Who Is Sycophantic to Whom?

**Fonte (22/10):** https://arxiv.org/abs/2610.31579.

**Descoberta/evidência.** Experimento intraindivíduos com 32 participantes que estudaram até cinco temas científicos com cinco assistentes construídos sobre GPT-5-mini. Entre condições, 0%, 25%, 50%, 75% ou 100% dos fatos de briefing eram trocados por falsidades plausíveis, sempre apresentadas com plena confiança. Em 104 questionários e 1.040 respostas, o desempenho foi de 7,4 a 2,9/10 conforme cresceu a proporção de falsidades. A alternativa falsa foi selecionada em 60% dos itens adulterados contra 11% dos corretamente ensinados; alegações falsas foram absorvidas em aproximadamente 65% das ocasiões em que apareceram. A confiança global declarada no assistente mudou pouco.

**Mecanismo hipotético.** A segurança da linguagem pode atuar como pista de autoridade, enquanto o usuário perde capacidade de distinguir quando sabe ou não sabe a resposta. A pesquisa observa esse padrão; não isolou o mecanismo psicológico em consumidores.

**Implicação/experimento.** Para Têmis, verificar cada alegação factual de criativos e conversas comerciais contra fonte rastreável, em vez de aprovar por plausibilidade e fluência. Randomizar revisão convencional versus revisão com fontes verificáveis; medir aceitação de informações falsas, compreensão e confiança calibrada antes de acompanhar resultados de funil.

**Riscos/limites.** N=32, única família de modelo, tópicos científicos e falsidades implantadas deliberadamente; não estudou anúncios nem vendas. Não transformar a tendência à deferência em técnica para persuadir ou encobrir alegações sem prova. **Card candidato:** confianca-factual-ia-deferencia-humana, coleção neuromarketing, como salvaguarda de influência e confiança.

### 2. Metadados contestáveis antes do commit — Contestable Agents

**Fonte (22/10):** https://arxiv.org/abs/2610.31553.

**Descoberta/evidência.** O trabalho propõe agentes que apresentam argumentos com evidências citadas para decisões de classificação de dados. O operador pode aceitar, contestar, modificar ou acrescentar argumentos antes do commit, e a decisão final preserva uma trilha de revisão. Dois casos de HVAC ilustram a arquitetura; não há ensaio controlado demonstrando ganho de acurácia ou tempo.

**Mecanismo proposto.** Separar proposta automática, contestação humana e decisão final evita que classificações plausíveis se tornem precedentes silenciosos.

**Implicação/experimento.** No AI Hub, apresentar justificativa e fonte a cada classificação importante de nicho, hipótese ou criativo. Comparar aceite simples com aceite contestável; medir falsas aprovações e capacidade de recuperar o motivo da decisão.

**Riscos/limites.** Protótipo e casos ilustrativos; a trilha de auditoria não assegura correção. **Sem card:** governança e contestabilidade de agentes não constituem coleção aceita.

### 3. O problema não é clicar: é localizar exatamente o alvo — PointerBench

**Fonte (22/10):** https://arxiv.org/abs/2610.31274.

**Descoberta/evidência.** Benchmark de 1.500 pares screenshot-instrução, cobrindo planilhas, texto longo e 100 aplicativos profissionais, incluindo 39 configurações especializadas. Em tarefas de localização de caracteres em texto (PointerBench-Text), as taxas relatadas foram 69,6% para Claude Fable 5, 53,4% para GPT-5.6 Sol e 25,8% para o especialista UI-Venus-2-9B, abaixo de suas médias agregadas respectivas de 83,2%, 77,7% e 54,5%.

**Mecanismo proposto.** Comandos em linguagem natural exigem ancoragem geométrica exata; acertar o objetivo abstrato não garante localizar o elemento concreto correto.

**Implicação/experimento.** Testar operações de edição em criativos e documentos com alvos mínimos e elementos adjacentes; priorizar IDs e coordenadas verificadas, prévia de seleção e confirmação antes de alterações irreversíveis.

**Riscos/limites.** Benchmark diagnóstico, não teste de eficácia em uso real nem comparação de agentes completos. **Sem card:** grounding de interfaces está fora do catálogo.

### 4. CoWriter: envolver o autor em vez de relegá-lo a revisor passivo

**Fonte (22/10):** https://arxiv.org/abs/2610.31277.

**Descoberta/evidência.** Em estudo exploratório, CoWriter orquestrou etapas de escrita apoiadas por IA com intervenções proativas. Os autores relataram maior sensação de autoria, melhor expressão de perspectivas próprias e 2,2 vezes mais interações bem-sucedidas com o agente do que uma interface convencional; avaliadores externos perceberam maior presença de experiências pessoais e narrativa nos textos.

**Mecanismo interpretativo.** Perguntas e decisões inseridas ao longo do processo mantêm a intenção criativa humana ativa e podem reduzir supervisão passiva.

**Implicação/experimento.** No editor de roteiros do Apolo, comparar geração direta com checkpoints em que o operador escolhe ângulo, exemplos e linguagem. Medir retrabalho, originalidade percebida, atribuição de autoria e qualidade de briefing.

**Riscos/limites.** Estudo exploratório; 2,2 vezes mais engajamentos não equivale a 2,2 vezes mais produtividade, vendas ou qualidade causalmente demonstrada. **Sem card:** colaboração na escrita não possui coleção legitimamente aderente.

### 5. Coescrita de respostas emocionalmente difíceis não elimina o tempo de reflexão

**Fonte (22/10):** https://arxiv.org/abs/2610.31357.

**Descoberta/evidência.** Dez participantes escreveram respostas a discursos de ódio sem e com sugestões editáveis da IA. As respostas assistidas receberam médias descritivas maiores de empatia, não toxicidade, relevância, especificidade e persuasão por avaliadores pares, mas o tempo médio passou de 2,52 a 3,88 minutos. O estudo também encontrou tensão entre empatia prescrita pela IA e a vontade legítima de discordar energicamente.

**Mecanismo interpretativo.** Opções editáveis podem apoiar reflexão e senso de agência; padronizar um único tom emocional pode suprimir intenção autêntica.

**Implicação/experimento.** Em revisão de tom de atendimento do Marketing Hub, oferecer alternativas de resposta que o operador possa escolher e modificar. Medir adequação percebida e trabalho de revisão; não otimizar apenas simpatia ou tempo.

**Riscos/limites.** N=10, ordem fixa das condições e tema sensível; resultados descritivos não comprovam transferência à comunicação comercial. **Sem card:** Emotional UX de coautoria não corresponde diretamente às coleções.

### 6. Diversidade de pontos de vista não garante compreensão

**Fonte (22/10):** https://arxiv.org/abs/2610.31459.

**Descoberta/evidência.** Experimento entre grupos com 63 participantes comparou representação de posições em escala única com vetores de posição por conceito. A segunda abordagem ampliou variedade e cobertura dos pontos de vista apresentados, mas não melhorou a profundidade das justificativas nem a reconstrução correta das opiniões.

**Mecanismo interpretativo.** Divergência pode envolver diferentes conceitos; expor alguém a mais alternativas não basta para assegurar entendimento.

**Implicação/experimento.** No debate de hipóteses de mercado, separar discordâncias por preço, urgência, segmento e benefício, exigindo que agentes expliquem o ponto em desacordo. Medir precisão ao reproduzir argumentos opostos, não só quantidade de perspectivas.

**Riscos/limites.** Domínio de diálogo e pontos de vista, não descoberta comercial. **Sem card:** UX deliberativa e raciocínio de agentes fora das coleções.

### 7. Malleable Workspaces: o trabalho precisa sobreviver ao histórico do chat

**Fonte (22/10):** https://arxiv.org/abs/2610.30893.

**Descoberta/evidência.** Modelo conceitual baseado na comparação de 58 artigos e acompanhamento longitudinal por três meses de sete usuários experientes de agentes. Os pesquisadores estudaram como pessoas reorganizam representações compartilhadas de trabalho, e não apenas acumulam respostas em conversas.

**Mecanismo proposto.** Unidades de trabalho, relações e decisões explicitadas na interface reduzem a dependência de reconstruir contexto apenas do chat.

**Implicação/experimento.** No AI Hub, testar workspace visual e versionado contendo objetivos, entidades, hipóteses, restrições e resultados, contra histórico conversacional. Avaliar retomada após interrupções e divergências entre agentes.

**Riscos/limites.** Sete participantes e abordagem sobretudo qualitativa; sem ganho quantitativo generalizável. **Sem card:** interface e estado de trabalho de agentes não possuem coleção específica.

### 8. Uma única página pode contaminar uma resposta de busca — Data-Void Exploits

**Fonte (21/10; cross-list em 23/10):** https://arxiv.org/abs/2610.30365.

**Descoberta/evidência.** Em experimentos de ataque a busca assistida por IA, os pesquisadores preencheram lacunas de informação com páginas controladas pelo atacante. Conteúdo adversarial apareceu nas respostas em 31% das condições agregadas. Tom cético do prompt ofereceu proteção limitada na configuração testada.

**Mecanismo computacional.** A síntese de busca pode ocultar que a evidência veio de uma fonte isolada e possivelmente interessada; citações formais não garantem independência das fontes.

**Implicação/experimento.** Na pesquisa de oportunidades B2C, exigir confirmação independente antes de transformar uma notícia rara em card ou hipótese forte. Introduzir páginas adversariais simuladas em testes de robustez e medir quantas alegações contaminadas chegam à priorização.

**Riscos/limites.** Frequência experimental de aparição de conteúdo atacante, não taxa de desinformação na web ou impacto comercial. **Sem card:** qualidade de fontes e segurança de RAG não são coleções aceitas.

### 9. Incerteza visual deve comunicar o tamanho da amostra

**Fonte (22/10):** https://arxiv.org/abs/2610.31050.

**Descoberta/evidência.** O trabalho propõe jk-plots, visualizações adaptativas que combinam observações e modelos plausíveis para tornar perceptível como a incerteza da estimativa muda com o número de dados. Foi demonstrado em conjuntos sintéticos, sem experimento comprovando melhor decisão humana.

**Mecanismo proposto.** Mostrar a dispersão de estimativas junto das observações pode reduzir a impressão errada de precisão numérica.

**Implicação/experimento.** No dashboard de experimentos, confrontar taxa de conversão pontual com representação explícita de incerteza e amostra. Testar decisões de pausar ou continuar sob cenários de baixa amostra.

**Riscos/limites.** Sem comprovação de superioridade cognitiva por usuários; visualizações complexas também podem confundir. **Sem card:** visualização estatística/UX analítica fora das coleções.

### 10. Voz como memória pesquisável requer consentimento e correção

**Fonte (22/10):** https://arxiv.org/abs/2610.30895.

**Descoberta/evidência.** *Your Home Remembers* apresenta protótipo em tablet Android com detecção local de palavra de ativação, reconhecimento de voz, diarização, indexação vetorial, timestamps e respostas RAG ligadas a trechos da conversa. O resumo descreve arquitetura, não avaliação comparativa de desempenho humano.

**Mecanismo proposto.** Recuperar passagem temporal e atribuição de falante pode tornar a memória verbal auditável, em vez de responder apenas com resumo sem fonte.

**Implicação/experimento.** Em entrevistas consentidas sobre produtos digitais, testar recuperação de falas com áudio original, timestamp e possibilidade de corrigir atribuições. Verificar qualidade da transcrição, precisão do falante e esforço de conferência.

**Riscos/limites.** Gravação contínua é sensível; exige consentimento claro de todos, limites de retenção e acesso. Não inferir intenção de compra de conversas sem validação. **Sem card:** memória conversacional não é coleção de prazer audiovisual só por empregar voz.

## Experience Engine v54 — implicações prioritárias

**Proposta verificável → Fonte contestável → Decisão humana explícita → Execução com alvo preciso → Resultado observável → Calibração da confiança → Revisão de memória e critérios.**

Prioridades sugeridas: (1) verificar alegações comerciais por fonte, com revisão de confiança calibrada; (2) experimentar decisões contestáveis em entidades e critérios do AI Hub; (3) aprimorar o grounding de editores visuais com IDs e confirmações; (4) transformar pesquisas escassas em hipóteses apenas depois de triangulação de fontes. São recomendações de teste, não funcionalidades implementadas nem efeitos comerciais demonstrados.

## Cards e fluxo editorial

Guia consultado em 24/10/2026 na main de paulofor/marketing-hub: https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md. Coleções aceitas: **video**, **prazer-audio-visual**, **neuromarketing**, **momentos-de-compra-b2c**.

**Um candidato a DRAFT:** `confianca-factual-ia-deferencia-humana`, coleção **neuromarketing**, por estudar deferência, influência e confiança diante de afirmações seguras de IA. A aplicação do card é uma salvaguarda de revisão de mensagens comerciais, e não instrução persuasiva. A fonte revisada curta precedeu o JSON, separa evidência, interpretação, aplicação e resultado real, e tem SHA-256 `61937a22ebf82337421e8e3bf71ae986f5d13d4070ed371b4adbf3257f956e2f`.

**Achados fortes sem card por ausência de coleção pertinente:** Contestable Agents, PointerBench, CoWriter, Malleable Workspaces e Data-Void Exploits. Os demais ficaram no relatório por domínio distante ou evidência aplicada insuficiente. Não criar coleção design-experiencia; nenhum card deve ser enviado à revisão, ativado ou arquivado automaticamente.

Os resultados reportados são de artigos externos, sem prova de conversão ou vendas. Para validar aplicações, acompanhar separadamente compreensão, acerto factual, revisão humana, abandono, retenção, CTA, checkout e pagamentos efetivos.
