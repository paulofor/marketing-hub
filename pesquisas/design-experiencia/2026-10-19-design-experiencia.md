# Radar de Design de Experiência — 19/10/2026

**Fontes:** publicações disponibilizadas em 16/10, presentes nas listagens científicas de 19/10. Separação estrita entre evidência, mecanismo interpretado, experimento sugerido e resultado comercial não demonstrado.

## Descobertas prioritárias

### 1. SCR-V2A: gerar som a partir de pontos de contato visuais

**Evidência:** artigo arXiv:2610.23021, aceito no ACM Multimedia Asia 2026. Em VGGSound, FAD 1,07 (MMAudio 1,84), mas DeSync temporal 0,680 (MMAudio 0,440; menor é melhor). Em avaliação com 30 pessoas e 20 vídeos, qualidade geral 3,93/5 e alinhamento semântico 4,10/5, versus 3,80 e 3,87 no MMAudio; em alinhamento temporal o MMAudio foi superior (3,87 vs 3,50). **Mecanismo:** usar informação de partes de objetos que produzem som complementa a cena global. **Experimento:** A/B de Foley orientado por eventos visuais em vídeos do Apolo, medindo qualidade, sincronização, custo e retrabalho. **Riscos:** falhas de localização, vídeos curtos; nenhum resultado comercial comprovado. **Card:** video-foley-regioes-visuais-criticas na coleção video.

Fonte: https://arxiv.org/abs/2610.23021

### 2. Conforto em XR depende da configuração dos estímulos

**Evidência:** 20 adultos autistas chineses testaram cinco configurações XR; L3 (som abrupto) recebeu 2,97/5 e L5 (transições graduais com vibração/temperatura) 4,43/5; Friedman χ²(4)=46,39; p<0,001. **Mecanismo:** previsibilidade e possibilidade de interromper estímulos podem afetar conforto. **Experimento:** testar aviso, fade, volume e saída fácil em experiências multimodais. **Limite:** condições mudaram em vários fatores; não se pode atribuir causalidade a um recurso isolado, nem generalizar para anúncios.

Fonte: https://arxiv.org/abs/2610.23833

### 3. PersonaCUABench: obedecer ao usuário difere de completar a tarefa

**Evidência:** 74 tarefas, 44 personas e 3.330 execuções de agentes; queda média de conclusão de 36% nas instruções por personas. Parte das falhas de pontuação era parada explicitamente solicitada pelo usuário (11,2% das células com perda). **Mecanismo:** restrições e consentimento podem conflitar com o objetivo formal. **Experimento:** testar cancelamentos, mudanças de escopo e invariantes no AI Hub. **Limite:** personas e avaliações simuladas.

Fonte: https://arxiv.org/abs/2610.22990

### 4. PresenceBench: adaptação ao estado precisa de controle negativo

**Evidência:** quatro modelos e cenários com estado sintético do usuário: preferências promissoras em algumas condições, mas sem confirmação de especificidade por modelo. **Mecanismo:** instruções adicionais podem melhorar a resposta sem que o estado esteja correto. **Experimento:** estado correto x estado trocado x sem estado em adaptive UX. **Limite:** não mede sinais emocionais reais ou compras.

Fonte: https://arxiv.org/abs/2610.23625

### 5. Wait for It: feedback durante espera de agente em VR

**Evidência:** 18 participantes compararam voz, spinner e gestos corporais; gestos superaram spinners em naturalidade e presença social. **Mecanismo:** coerência entre o feedback e a modalidade de interação. **Experimento:** feedback vocal/contextual versus visual durante espera, medindo compreensão e interrupções. **Risco:** não simular trabalho inexistente; VR não representa todos os produtos.

Fonte: https://arxiv.org/abs/2610.23336

### 6. Entrevistas de pesquisa conduzidas por IA

**Evidência:** N=41, experimento aleatorizado; IA local 7B fez mais que o dobro de perguntas e coletou cerca de 65% mais texto que entrevistadores humanos treinados não especialistas, sem demonstração clara de maior especificidade. **Mecanismo:** volume não equivale a profundidade. **Experimento:** usar entrevista por IA na validação B2C e conferir qualidade de episódios concretos de compra. **Limite:** estudantes e chat textual, sem comportamento de compra observado.

Fonte: https://arxiv.org/abs/2610.23149

### 7. Annotrace: diagnosticar falhas no nível de execução

**Evidência:** seis formatos de traces, 235 unidades de anotação reproduzidas e concordância de dois modelos de 91%, κ=0,82. **Mecanismo:** rótulos por passo permitem associar erro a ferramenta, decisão e handoff. **Experimento:** categorização de falhas do AI Hub e medição de recorrência. **Limite:** concordância entre modelos não valida verdade dos rótulos.

Fonte: https://arxiv.org/abs/2610.23284

### 8. Novos trabalhos possíveis com IA

**Evidência:** levantamento qualitativo de 331 desenvolvedores Microsoft; 45% dos relatos descrevem tarefas conhecidas mais rápidas, 36,9% tarefas antes fora do alcance e 12,4% novas modalidades de trabalho com agentes. **Mecanismo:** habilidade para executar pode crescer mais rápido que julgamento crítico. **Experimento:** medir revisão, tarefas antes inviáveis e bugs de homologação. **Limite:** autorrelatos, não efeito causal.

Fonte: https://arxiv.org/abs/2610.23708

## Tendência observada em evento

No workshop Harness4GenUI do ASE 2026 (Munique, 12/10), o programa incluiu sistemas declarativos, contratos semânticos e rastreabilidade de recursos gerados. É um registro de programação, não evidência experimental. https://conf.researchr.org/program/ase-2026/program-harness4genui-2026/

## Experience Engine v49 e decisão editorial

Fluxo sugerido: intenção → restrições/autorização → contexto correto → representação multimodal → geração → verificação técnica e perceptiva → aprovação → indicadores comerciais reais.

Guia consultado na main: harness-library-api/docs/guia-uso-api-cards.md. Coleções permitidas: video, prazer-audio-visual, neuromarketing, momentos-de-compra-b2c.

**Criado um único candidato DRAFT:** video-foley-regioes-visuais-criticas, coleção video, com fonte revisada e JSON separados. **Sem card apesar de fortes:** PersonaCUABench, PresenceBench, Wait for It, entrevistas, Annotrace e estudo de desenvolvedores, por ausência de coleção aderente. Conforto XR também ficou apenas no relatório pela extrapolação insuficiente para marketing.

Nenhum card foi enviado para revisão, ativado ou arquivado; nenhum resultado comercial foi presumido.
