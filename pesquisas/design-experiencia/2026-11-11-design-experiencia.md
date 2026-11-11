# Radar de Design de Experiência — 2026-11-11

**Experience Engine v72 | Marketing Hub | data da rodada: 11/11/2026**

## Síntese executiva e critérios

Examinada a listagem do arXiv **Human-Computer Interaction de 11 de novembro de 2026** (34 entradas) e estudos complementares divulgados em 10/11. Prioridade a submissões de **9 e 10/11/2026**, ainda ausentes do radar anterior, sobre vídeos publicitários gerados por IA, intenção criativa, UX de agentes, confiança, emoção, realidade estendida, voz e haptics. A data da listagem não é necessariamente a data de submissão nem de validação por pares; a maioria são preprints.

**Mensagem principal:** na produção de vídeo com IA, conferir apenas beleza ou qualidade técnica não basta: a fidelidade do produto e a intenção criativa precisam ser verificadas por cena e ao longo de todas as etapas. Para experiências com agentes, métricas subjetivas de confiança ou de satisfação não substituem verificação de tarefas.

### 1. Revisão multimodal de anúncios de vídeo: detectar o defeito por cena, com decisão humana

**Descoberta.** Juízes multimodais especializados verificam seis dimensões separadas: fidelidade do produto, qualidade visual da cena, pronúncia, roteiro e alegações, som e legendas; revisores humanos confirmam ou rejeitam o que foi sinalizado e encaminham correções localizadas.

**Evidência.** Agarwaal et al. (submetido 09/11): 120 anúncios de aproximadamente 15 s, 562 cenas e 20 produtos; com os alertas apresentados, a maioria dos revisores marcou produto visualmente errado em 60% das cenas. Em 60 anúncios, a proporção marcada foi 58% com alertas de IA versus 26% quando ocultos. Em 120 cenas rotuladas independentemente, detector de fidelidade do produto obteve AUC 0,84, recall 26/29 e precisão 26/56; portanto, muitos alertas eram falsos. Trata-se de avaliação de um pipeline e não de experimento randomizado independente. [Artigo](https://arxiv.org/html/2611.13178v1).

**Mecanismo interpretativo.** Alertas associados a cenas e evidências podem dirigir a atenção a defeitos pouco salientes, mas também ancorar o julgamento e produzir aceitação indevida de achados automáticos.

**Produto.** Apolo/Têmis: comparar o produto mostrado às fotos e especificações reais; validar cada alegação, legenda e áudio separadamente; mostrar alerta, evidência e localização; corrigir somente trechos confirmados. Separar falso positivo de erro real.

**Experimento.** A/B entre revisão humana comum e revisão com alertas visíveis, com auditoria cega independente da verdade-terreno. Medir recall/precisão de defeitos, minutos de revisão e custo por anúncio realmente correto; acompanhar CTA e vendas em experimentos posteriores.

**Riscos/limites.** Amostra interna, alta taxa de falsos positivos, risco de viés de automação e nenhum resultado de conversão ou ROI.

**Decisão:** card em `video`.

### 2. Intenção criativa como estado persistente entre roteiro, cenas e vídeo

**Descoberta.** VideoMaker transforma intenções do criador em unidades editáveis e confirmadas (*seeds*) que atravessam planejamento, roteiro, storyboard e material audiovisual.

**Evidência.** Jin et al. (submetido 09/11) realizaram 12 entrevistas e estudo intraindivíduo com 30 participantes. Preservação de intenção recebeu 5,33 versus 3,90 (p=0,046) e tradução narrativa 5,87 versus 4,47 (p=0,028) na comparação com uma interface de referência. A iniciativa negociável de IA não obteve significância após correção (p=0,056). [Artigo](https://arxiv.org/html/2611.12605v1).

**Mecanismo interpretativo.** Externalizar decisões criativas confirmadas evita que uma preferência seja esquecida quando o conteúdo muda de representação. Isso não garante coerência global automática: os próprios participantes notaram fragmentação de cenas.

**Produto.** No Apolo, versionar objetivo, público, tom, promessa factual, marca e restrições e exigir rastreabilidade dos trechos gerados à intenção confirmada.

**Experimento.** Comparar prompts soltos versus estado de intenção versionado, usando revisores cegos para medir divergências de marca e narrativa, retrabalho, variedade de opções e custo final.

**Riscos/limites.** Amostra pequena de 18–26 anos, medidas de percepção, contexto de vídeos curtos, sem teste de vendas e sem demonstração causal de fidelidade de anúncios reais.

**Decisão:** card em `video`.

### 3. Os padrões de avaliação de IA mudam mesmo quando o modelo não muda

**Descoberta/evidência.** Chen, Zheng e Zhu (submetido 10/11) analisaram 1.260.967 votos cegos na Chatbot Arena, de maio de 2023 a agosto de 2024, comparando pares de versões fixas. A fração que julgava *ambos ruins* aumentou 1,71 ponto percentual por mês, enquanto a preferência relativa entre modelos permaneceu estável. [Artigo](https://arxiv.org/abs/2611.14333).

**Mecanismo.** Expectativas e padrão de referência do usuário podem mudar com a evolução da categoria; isso é inferência a partir de dados observacionais, não experimento que manipula expectativas.

**Aplicação/experimento.** No AI Hub, acompanhar tarefas padronizadas com versão fixa e satisfação longitudinal, separando queda real de capacidade, mudança do perfil de usuários e elevação do padrão esperado.

**Limite.** Dados de julgamentos públicos anteriores a 2026, sem medir diretamente uso do Marketing Hub. **Sem card:** avaliação de agentes não se enquadra nas coleções disponíveis.

### 4. Explicações de segurança ligadas a código melhoram possibilidade de ação, não necessariamente confiança

**Descoberta/evidência.** Johnson et al. (submetido 09/11) mantiveram a detecção com o analisador estático e usaram agente de IA apenas para explicar achados ancorados no código e na regra. Estudo cruzado com 24 participantes: SUS 77,3→85,0 e odds de correção correta 2,6 vezes maiores; a confiança não apresentou melhora clara. [Artigo](https://arxiv.org/abs/2611.13041).

**Mecanismo.** Explicações auditáveis e próximas da evidência tornam a ação mais compreensível sem precisar aumentar a crença na IA.

**Aplicação/experimento.** No AI Hub, ligar alertas a arquivo, linha, regra e teste reprodutível, medindo correções eficazes contra explicações livres. **Limite:** domínio SAST, avaliação pequena; sem card por falta de coleção para engenharia de agentes.

### 5. Designers usam prompt-to-design mais como motor de ideias do que como produto pronto

**Descoberta/evidência.** Ye, Li e Wang (submetido 09/11) entrevistaram 17 profissionais de UI. A geração por prompts acelerou ideias e prototipação, mas houve preocupações com qualidade, criatividade individual e integração ao trabalho. [Artigo](https://arxiv.org/abs/2611.13211).

**Mecanismo possível.** Propostas rápidas expandem opções, mas transferência de decisões de design pode enfraquecer controle e critério.

**Aplicação/experimento.** Gerar variantes e submetê-las a testes de usabilidade e acessibilidade humanos; comparar qualidade final, originalidade e tempo total, não só tempo de geração. **Limite:** entrevistas, não um ensaio causal. **Sem card:** generative UI não possui coleção permitida.

### 6. Música colaborativa em XR: envolvimento maior em VR não significou melhoria geral de afeto ou flow

**Descoberta/evidência.** Schlagowski et al. (submetido 10/11) compararam VR e AR com 28 músicos. Houve maior envolvimento reportado em VR; comparações de flow, afeto e outras dimensões não mostraram diferenças estatisticamente confiáveis. Afeto positivo, flow e suporte à criatividade foram correlacionados nas duas condições. [Artigo](https://arxiv.org/abs/2611.14029).

**Mecanismo possível.** O ambiente circundante pode alterar sensação de envolvimento e presença, de forma dependente de preferência individual.

**Aplicação/experimento.** Testar escolha entre ambientes musicais, acompanhando conforto e participação ativa, sem inferir que VR produz mais prazer. **Limite:** pequena amostra e sem comércio. **Sem card:** a evidência de prazer audiovisual comercial é insuficiente.

### 7. Personalizar interpretação afetiva pode tornar negociação mais persuasiva, sem benefício demonstrado ao usuário

**Descoberta/evidência.** Keskin et al. (submetido 10/11), em estudo cruzado com 66 participantes negociando com robô, observaram maior utilidade individual do agente e concessões mais frequentes ao usar reconhecimento facial adaptado continuamente. Utilidade do participante e distância de Nash não diferiram significativamente. [Artigo](https://arxiv.org/abs/2611.14247).

**Mecanismo.** O sistema pode ajustar estratégia de negociação a sinais afetivos estimados, mas expressão facial não equivale a emoção comprovada nem justifica inferência sensível.

**Aplicação/experimento.** Auditoria de personalização com consentimento, verificando equilíbrio de benefícios e possibilidade de desligar a adaptação; não aplicar técnicas opacas para extrair concessões de consumidores.

**Limite.** Robô em laboratório; sem compras reais. **Sem card:** o radar de 09/11 já cobre riscos de persuasão com sinais fisiológicos; evitar duplicação, sobretudo sem resultado que autorize exploração comercial.

### 8. Painel de progresso de projetos Codex/Claude Code vincula andamento a evidências

**Descoberta/evidência.** *Project Progress Desk* (submetido 09/11) reúne tarefas, bloqueios, decisões e progresso de múltiplos projetos. Os autores descrevem uso diário em 36 projetos e diferenciam progresso sustentado por resultados verificáveis de estimativas datadas. [Artigo](https://arxiv.org/abs/2611.13287).

**Mecanismo.** Progresso verificável e decisões pendentes reduzem o esforço de reconstruir contexto entre sessões.

**Aplicação/experimento.** Criar visão unificada no AI Hub com estados do orquestrador, artefatos, critérios de aceitação e filas; comparar tempo de retomada e bloqueios esquecidos. **Limite:** demonstração e relato de uso, sem controle experimental. **Sem card:** gestão de agentes fora das coleções.

### 9. Voz e comunicação tátil: novos benchmarks, sem demonstração comercial

**Voz.** Qi e Skantze publicaram corpus de 31 horas e benchmark de previsão de turnos de fala, com 865 eventos de manutenção e 865 de troca de falante em partição de teste. [AVCC](https://arxiv.org/abs/2611.13957). **Mecanismo/aplicação:** pistas audiovisuais podem orientar quando um assistente intervém; testar interrupções e turnos sem sobreposição. **Limite:** benchmark de previsão, não comprovação de melhor UX.

**Haptics.** Harnett e Friesen compararam mapas táteis de altura e camadas binárias para representar formas 3D em displays planos. Houve alta precisão de montagem; diferenças de tempo e experiência não se traduziram em superioridade significativa de sucesso final. [Artigo](https://arxiv.org/abs/2611.14389). **Aplicação:** usar sinalização tátil para orientação espacial acessível, validando tarefa e conforto. **Limite:** hardware específico; não extrapolar para smartphone comercial. **Sem cards:** coleções atuais não abrangem voz operacional ou acessibilidade háptica.

### 10. Aparência de raciocínio pode induzir deferência cognitiva

**Descoberta/evidência.** Lack et al. (submetido 10/11) argumentam que justificativas verbais exibidas por LLMs podem levar usuários a atribuir compreensão e autoridade epistêmica excessivas à IA. É um **trabalho conceitual**, não um experimento que quantificou o efeito. [Artigo](https://arxiv.org/abs/2611.13456).

**Mecanismo possível.** Linguagem explicativa fluente funciona como pista social de competência, mesmo sem ligação auditável com a causa real da resposta.

**Aplicação/experimento.** No AI Hub, preferir evidência rastreável e testes executáveis a narrativas confiantes; medir se usuários identificam respostas erradas com e sem evidência externa. **Limite:** argumento teórico com exemplos, sem estimativa causal; **sem card** por ausência de coleção.

### 11. Sensores multimodais para inferir estados afetivos continuam muito incertos

**Descoberta/evidência.** *BODYinTRANSIT-Lab* (submetido 10/11) apresenta dados multimodais de 101 pessoas em tarefas de caminhada com áudio dos próprios passos alterado. Um baseline de classificação intra-sessão reportou acurácia balanceada média de 45,9% para valência e 44,5% para peso percebido. [Artigo](https://arxiv.org/abs/2611.14093).

**Mecanismo possível.** Áudio e feedback corporal podem alterar percepção subjetiva, mas os sinais observáveis não decodificam emoção de maneira universal.

**Aplicação/experimento.** Evitar decisões comerciais automatizadas baseadas em inferência fisiológica sem validação individual; oferecer controles explícitos e consentidos. **Limite:** dataset experimental, não evidência de prazer audiovisual ou melhoria comercial; **sem card**.

## Síntese para produto

- **Apolo/Têmis:** criar contrato de seis eixos para revisão de anúncios, checagem por cena contra material real e correção localizada com confirmação humana.
- **Apolo/Íris:** transportar intenções confirmadas do briefing para roteiro, storyboard e render, preservando liberdade criativa apenas nos elementos não fixados.
- **AI Hub:** unir evidências e decisões em um painel; distinguir fato validado, estimativa e sugestão; medir êxito da tarefa e qualidade da revisão, além de satisfação autorrelatada.
- **Ética:** não usar sinais fisiológicos ou inferência emocional para persuadir sem consentimento e não tratar aparência de raciocínio do modelo como prova.

## Seleção editorial de cards

Guia consultado na branch `main`: [Guia de uso da API de cards](https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md). Coleções aceitas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

Foram criados **dois candidatos a DRAFT**, ambos legitimamente na coleção `video`:

1. `revisao-multimodal-defeitos-produto-video-ia`: auditar anúncios e detectar produtos inconsistentes com referências reais, com confirmação humana. Fonte: `cards/fontes/2026-11-11-revisao-multimodal-anuncios-video.md`.
2. `intencao-criativa-compartilhada-producao-video`: preservar a intenção confirmada do criador entre estágios de produção. Fonte: `cards/fontes/2026-11-11-intencao-criativa-video-estado-compartilhado.md`.

Ambos com `publishedOn=2026-11-11`, `validUntil=2026-12-26`, `sourceKind=MARKDOWN`, e exatamente os 16 campos requeridos. Os valores SHA-256 são calculados sobre bytes exatos das fontes, separadas de achado, interpretação, aplicação e resultados reais. **Nenhum card foi enviado para revisão, ativado ou arquivado.** O merge pode acionar registro automático como DRAFT, cujo estado deve ser verificado separadamente.

**Achados fortes sem card por falta de coleção aderente:** avaliação longitudinal de modelos, explicações de segurança ancoradas em código, generative UI, painel de progresso de agentes, turn-taking em voz e haptics. Música em XR e personalização afetiva também permaneceram no relatório por limitações de evidência e risco de duplicação.

## Fontes e referências

- [Listagem cs.HC de 11/11/2026](https://arxiv.org/list/cs.HC/recent).
- Os 12 trabalhos individuais estão ligados diretamente nas seções acima, com datas de submissão, métodos e limites.
