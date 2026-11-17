# Radar de Design de Experiência — 17 de novembro de 2026

**Rodada:** 17/11/2026, manhã (America/Sao_Paulo)  
**Escopo:** UX, generative UI, emotional/behavioral design, interação humano–IA, personalização, multimodalidade, voz, vídeo e agentes.  
**Base de descoberta:** listagem *Human-Computer Interaction* do arXiv exibida em 17/11/2026: **79 entradas, 31 novas submissões**. Os artigos centrais foram submetidos em 15–16/11/2026 e divulgados na listagem de 17/11. Fonte da listagem: https://arxiv.org/list/cs.HC/new.

## Síntese analítica

Três sinais novos se destacam: (1) **interfaces gráficas de ajuste podem eliminar ambiguidades de prompts**, especialmente ao editar vídeo ligado a movimento; (2) **personalizar não é o mesmo que dar infinitas oportunidades de revisão** — o ganho psicológico observado na personalização não equivale a mudança de comportamento; (3) **agentes confiáveis precisam verificar o alvo e o resultado de cada ação**, em vez de somente gerar cliques ou respostas convincentes.

Foram examinados trabalhos recentes de 17/11 sem repetir as conclusões do radar de 07–16/11. Em particular, o card de vídeo abaixo é distinto do card anterior sobre preservação da intenção criativa do briefing ao vídeo: agora o tema é **controle fino de VFX em vídeo existente**, com dados de conclusão e precisão da edição.

## 1. PromptPoseFX — controle direto e reversibilidade na edição de vídeos

- **Descoberta:** geração por IA combinada com cliques espaciais, marcadores temporais e controles de parâmetros favoreceu a edição de efeitos visuais 2D acionados por gestos, em comparação com uma interface somente textual.
- **Evidência:** estudo intraindivíduo com **12 participantes**. As duas tarefas foram concluídas por **12/12** com PromptPoseFX e por **9/12** com o baseline textual, dentro de 30 minutos por condição. Precisão espacial percebida: **4,58 vs 2,33**; precisão temporal: **4,67 vs 3,25**; simplicidade do ajuste de parâmetros: **4,58 vs 1,83**. O baseline manteve o mesmo pipeline de geração; a diferença principal eram os controles da interface.
- **Mecanismo interpretativo:** apontar *onde*, *quando* e *o quê* alterar reduz a ambiguidade linguística; camadas independentes podem prevenir corrupção de efeitos anteriores. Não é uma comprovação isolada de causa psicológica.
- **Produto:** testar no Apolo edição de elementos vinculados à pose com timeline, referência espacial clicável, sliders e camadas não destrutivas, preservando o vídeo original.
- **Hipótese experimental:** mais efeitos corretos por auditoria visual cega, menor retrabalho e custo por ativo aprovado que prompts apenas textuais; métricas reais de audiência e venda devem ser avaliadas separadamente.
- **Riscos/limites:** apenas 12 criadores, efeitos 2D, tarefas guiadas e tecnologia p5.js/MediaPipe limitada a certos contextos; não há prova de melhora de engajamento ou conversão.
- **Fonte:** Zheng, Ye e Fu, *PromptPoseFX*, arXiv:2611.20485v1 (submetido em 16/11/2026): https://arxiv.org/html/2611.20485v1.

**Decisão editorial:** **card novo** na coleção `video`: `controles-hibridos-edicao-vfx-pose-video`. Oportunidade de protótipo e teste, não uma afirmação de ROI.

## 2. Vibe Interventions — personalização percebida pode valer mais que revisar incessantemente

- **Descoberta:** um experimento preregistrado com **300 participantes**, cinco condições de 60 pessoas, separou personalização do conteúdo e capacidade de revisar uma interface generativa.
- **Evidência:** a condição personalizada sem revisão apresentou maior senso de propriedade que a GenUI básica (**d = 0,42, p = 0,022**, contraste descritivo não corrigido), e maior sensação de que a aplicação foi criada especificamente para a pessoa. **Nenhum desfecho da família preregistrada resistiu à correção de Holm**, e nenhuma condição generativa superou a aplicação estática em desfechos preregistrados. Mais revisões escolhidas pelos próprios participantes se associaram a avaliações inferiores: associação observacional, **não efeito causal** das revisões.
- **Mecanismo possível:** reconhecimento de um contexto próprio pode favorecer identificação com o artefato; solicitar revisões em texto exige esforço para articular desejos. Esses mecanismos ainda precisam ser testados diretamente.
- **Produto:** no AI Hub, gerar uma primeira interface de tarefa usando contexto pertinente e poucos controles específicos. Não supor que oferecer edições infinitas melhora a experiência.
- **Hipótese:** comparar UI genérica, personalizada e personalizada com edição, medindo conclusão real, alterações necessárias, erros, esforço e intenção de retorno.
- **Limites:** intervenção de hábitos, autoavaliações no mesmo dia; não houve demonstração de mudança efetiva de hábitos ou compras.
- **Fonte:** Ahmed et al., *Vibe Interventions*, arXiv:2611.20315v1 (15/11/2026): https://arxiv.org/html/2611.20315v1.

**Sem card:** design comportamental aplicado a hábitos pessoais, não evidência sobre mecanismos de compra, prazer audiovisual ou vídeo. Não forçar coleção `neuromarketing` ou `momentos-de-compra-b2c`.

## 3. Facilitação de decisões de grupos por LLM

- **Descoberta/evidência:** experimento randomizado com **138 grupos de três pessoas (N = 414)**, em uma tarefa de escolha com informações privadas distribuídas. A escolha correta ocorreu em **32,6%** dos grupos sem IA e em **56,5%** tanto com facilitador LLM individual quanto com facilitador LLM público ao grupo; **OR 2,69**, p=0,022 em cada comparação.
- **Mecanismo:** intervenções que resumem, convidam pessoas caladas a contribuir e questionam premissas podem ajudar o grupo a compartilhar evidências ocultas. O agente não conhecia a resposta certa.
- **Produto:** usar um facilitador em revisões do AI Hub para solicitar evidências ausentes antes de consolidar decisões.
- **Experimento:** comparação de reuniões com/sem facilitador, decisões verificadas, participação e custo por decisão.
- **Riscos/limites:** cenário artificial de contratação, não implantação empresarial; nudges podem influenciar de forma indevida e aumentar fricção.
- **Fonte:** Córdova et al., arXiv:2611.19652v1 (novembro de 2026): https://arxiv.org/html/2611.19652v1.
- **Card:** nenhum; facilitação decisória não se encaixa nas quatro coleções.

## 4. Tutores com correção assistida: fazer o aluno corrigir pode ensinar mais que explicar

- **Descoberta/evidência:** experimento preregistrado com **400 adultos** comparou solução correta, explicação genérica, explicação personalizada por LLM e tutoria socrática com prática guiada. Participantes na condição socrática resolveram **64% de seus erros** durante o diálogo e pontuaram **0,24 desvio-padrão** acima das condições de explicação no pós-teste alinhado à prática; a sessão levou quase o dobro do tempo.
- **Mecanismo plausível:** prática com feedback e experiência de domínio, em vez de leitura passiva de explicações, pode consolidar o aprendizado.
- **Produto:** no onboarding de aplicativos complexos e treinamento de operadores, testar correção por passos em vez de respostas prontas.
- **Hipótese:** melhor retenção de procedimentos após uma semana, mas maior custo inicial de tempo.
- **Riscos/limites:** tarefa educacional de equações; não demonstra ganho em conversão nem em todas as habilidades.
- **Fonte:** Asher, Cao e Carvalho, arXiv:2611.20061 (listagem de 17/11/2026): https://arxiv.org/abs/2611.20061.
- **Card:** nenhum; educação e onboarding geral não constituem uma coleção válida.

## 5. UIAnchor — olhar para o alvo e verificar o resultado de cada ação de GUI

- **Descoberta/evidência:** agente de automação móvel que combina percepção de elementos com verificação antes/depois da ação. No nível L4 (tarefas de 20–30 passos, vários apps, elementos pequenos), sucesso **51,9%**, contra **20,4%** do GPT-4o na configuração publicada e **35,2%** do Mobile-Agent-v3; no nível mais difícil L5, apenas **3,8%** de sucesso para o UIAnchor.
- **Mecanismo técnico:** separação entre reconhecimento de alvos, validação pré-clique, checagem do estado pós-clique e recuperação reduz propagação de erro.
- **Produto:** no AI Hub, acrescentar checkpoints de alvo e estado para ações de interface e logs auditáveis.
- **Hipótese:** reduzir ações no controle errado, navegações perdidas e falhas de tarefas longas, medindo custo e latência junto à taxa de conclusão.
- **Limites:** benchmarks mobile e modelos comparadores específicos, não prova de superioridade geral em Windows/desktop.
- **Fonte:** Zhou et al., arXiv:2611.20753v1 (novembro de 2026): https://arxiv.org/html/2611.20753v1.
- **Card:** nenhum; engenharia de agentes GUI não integra as quatro coleções.

## 6. AdaptType — a multimodalidade precisa adaptar-se à capacidade real da pessoa

- **Descoberta/evidência:** estudo com **10 pessoas com limitações motoras na parte superior do corpo** apresentou entrada de texto VR personalizável por modalidades; o sistema relatou velocidade de **36,26 palavras/min** e **0,36%** de erro. A taxa de falha na correção de fala caiu de **15,1% para 2,0%** no protocolo relatado.
- **Mecanismo possível:** modalidades complementares permitem compensar dificuldades de uma entrada específica.
- **Produto:** oferecer controles alternativos e configuração de interface, em vez de impor voz ou gestos como via única.
- **Hipótese:** menos falhas em tarefas com acessibilidade heterogênea, medindo também esforço e preferência.
- **Limites:** população e contexto VR específicos, sem demonstração direta em e-commerce.
- **Fonte:** Kong et al., arXiv:2611.19096 (listagem 17/11/2026): https://arxiv.org/abs/2611.19096.
- **Card:** nenhum; acessibilidade de VR não corresponde às coleções permitidas.

## 7. Mediação por esquemas na cocriação musical

- **Descoberta/evidência:** Wu et al. propõem traduzir narrativas para música por esquemas intermediários bidirecionais. Dois estudos (**N=188 e N=60**) encontraram melhor clareza de expressão, compreensão da música gerada e sensação de controle, sobretudo em peças menos convencionais.
- **Mecanismo interpretativo:** reduzir a distância associativa entre emoção/descrição e propriedades musicais visíveis ao usuário.
- **Produto:** hipótese de controles semânticos para trilhas de vídeo, com inspeção explícita da tradução intenção→características sonoras.
- **Experimento:** testar alinhamento à intenção e compreensão por ouvintes, não supor indução de prazer.
- **Limites:** o resumo não oferece prova direta de prazer audiovisual, recompensa neural, retenção ou venda.
- **Fonte:** *Decomposing the Associative Chain*, arXiv:2611.19203 (listagem 17/11/2026): https://arxiv.org/abs/2611.19203.
- **Card:** nenhum; a coleção `prazer-audio-visual` não deve ser usada sem evidência de prazer ou resposta afetiva específica à peça.

## 8. Autonomia por função em agentes automotivos

- **Descoberta/evidência:** **126 participantes** avaliaram preferências de autonomia para **14 funções**; um assistente de projeto foi avaliado qualitativamente por nove especialistas. Características da função ajudaram a ordenar níveis de autonomia adequados, mas as previsões para pessoas individualmente foram incertas.
- **Mecanismo:** risco, reversibilidade e consequências da tarefa podem pesar mais que uma preferência geral por automação.
- **Produto/hipótese:** matriz de permissões por tipo de operação no AI Hub; testar confiabilidade e satisfação frente à autonomia uniforme.
- **Risco/limite:** contexto automotivo e preferência declarada, não decisão de produção nem efeito comprovado em outros domínios.
- **Fonte:** Zepf et al., arXiv:2611.19474v1: https://arxiv.org/html/2611.19474v1.
- **Card:** nenhum; governança de autonomia não corresponde às quatro coleções.

## 9. Tom da voz vs direção do conselho em um verificador de sintomas

- **Descoberta/evidência:** pesquisa estudantil com **81 participantes e 648 tentativas**: decisões foram revistas em **45,7%** das tentativas quando a IA contrariou a hipótese inicial, contra **1,2%** quando confirmou. A diferença entre voz parecida com a humana e voz claramente artificial **não apresentou efeito estatisticamente significativo** no experimento.
- **Mecanismo plausível:** o sentido do conselho em relação à crença prévia pode importar mais que o estilo vocal.
- **Produto/hipótese:** avaliar factualidade, transparência e revisão de recomendações antes de otimizar antropomorfização vocal.
- **Limites:** autodiagnóstico simulado, não uso clínico, campanha de vendas nem prova de que uma voz é sempre equivalente à outra.
- **Fonte:** Roethlingshoefer et al., arXiv:2611.21379 (listagem 17/11/2026): https://arxiv.org/abs/2611.21379.
- **Card:** nenhum; domínio de saúde e sobreposição com achados anteriores sobre confiança e voz.

## 10. Copiloto de mensagens em serviço real de aconselhamento

- **Descoberta/evidência:** estudo misto de uso por **108 conselheiros** em canal de crise na Índia, analisando edições de **27.778 mensagens** sugeridas pelo copiloto, entrevistas, observações e surveys. Entre respondentes, **98,2%** disseram achar o copiloto útil; **80,4%** relataram ganho de eficiência.
- **Mecanismo possível:** sugestões estruturam texto e reduzem esforço de redação, mas respostas padronizadas podem enfraquecer autenticidade e prática do profissional.
- **Produto/hipótese:** permitir revisão, ajustes de tom e correções antes de enviar sugestões automatizadas; medir erros e qualidade independente, não só aceitação.
- **Limites:** os percentuais são percepções autorrelatadas e não taxas de melhora clínica, segurança ou produtividade objetivamente demonstrada; é contexto sensível.
- **Fonte:** Pham et al., arXiv:2611.21558 (listagem 17/11/2026): https://arxiv.org/abs/2611.21558.
- **Card:** nenhum; contexto clínico e colaboração humano–IA sem coleção legítima.

## Priorização e experimentos

**Prioridade A — Marketing Hub:** protótipo simples da edição híbrida para efeitos vinculados a movimento; comparar com prompts textuais mantendo modelo, roteiro e vídeo-base constantes. Critério: custo por vídeo aprovado sem regressão visual. Não confundir fidelidade técnica com conversão comercial.

**Prioridade B — AI Hub:** instrumentar agentes com detecção de alvo, checagem pré-ação/pós-ação e recuperação; executar testes em interfaces de dificuldade crescente.

**Prioridade C — design de experiências:** comparar personalização objetiva e revisão aberta de interfaces, medir tarefa concluída, esforço e uso posterior, mantendo explícitos os limites da evidência de hábitos.

## Decisão sobre cards (contrato vigente)

Antes de criar cards, o guia atual foi consultado na `main`: `harness-library-api/docs/guia-uso-api-cards.md`. As coleções válidas são somente `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`.

**Um card novo:** `controles-hibridos-edicao-vfx-pose-video` em **`video`**, por aderir diretamente a edição e controle de vídeo. Fonte revisada criada primeiro: `pesquisas/design-experiencia/cards/fontes/2026-11-17-controles-hibridos-vfx-movimento.md`. JSON: `pesquisas/design-experiencia/cards/2026-11-17-controles-hibridos-vfx-movimento.json`. `sourceKind=MARKDOWN`; `publishedOn=2026-11-17`, `validUntil=2027-01-01`; estado pretendido **candidato a DRAFT**.

**Achados fortes sem card por falta de coleção válida:** personalização de intervenções de hábitos, decisão em grupos, tutoria de erros, automação GUI verificável, entrada multimodal acessível e calibração da autonomia. Música foi mantida no relatório por ausência de medida específica de prazer; conselho por voz em autodiagnóstico e copilotagem clínica foram excluídos para não extrapolar domínios nem duplicar cards.

Nenhum card foi enviado para revisão, ativado ou arquivado. O cadastro como DRAFT, caso o workflow de publicação execute após o merge, depende de confirmação da API.
