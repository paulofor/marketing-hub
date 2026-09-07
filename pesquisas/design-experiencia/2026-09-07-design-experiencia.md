# Radar de Design de Experiência — 2026-09-07

## Síntese executiva

A rodada de hoje aponta para uma mudança importante no design de produtos com IA: em vez de permitir que o modelo gere uma interface de forma totalmente livre, os trabalhos mais interessantes começam a convergir para **interfaces geradas dentro de contratos, componentes, regras psicológicas, proveniência e mecanismos explícitos de controle**. Ao mesmo tempo, estudos recentes reforçam que experiência não é apenas aparência: timing da informação, acessibilidade, congruência estética com a função, preservação da autonomia e escolha de canais sensoriais podem alterar desempenho, confiança e engajamento.

O padrão que emerge é: **IA gera melhor quando o espaço de experiência é estruturado; o usuário controla melhor quando consegue inspecionar e corrigir os compromissos assumidos pelo sistema; e a UX melhora quando o sistema adapta não apenas conteúdo, mas também forma, tempo, modalidade e grau de iniciativa.**

## 1. Generative UI pode ficar muito mais barata usando modelos pequenos e componentes declarativos

**Descoberta.** O paper *Toward Frontier-Quality Declarative UI Generation at Small-Model Cost*, submetido em 3 de setembro de 2026, testa geração de interfaces A2UI usando modelos pequenos que selecionam componentes pré-construídos de um catálogo em vez de gerar código de frontend arbitrário.

**Evidência.** Em dois domínios React/TypeScript e quatro checkpoints de duas famílias de modelos, um modelo de 4B ajustado recuperou aproximadamente **98% da qualidade semântica** e **97% da qualidade visual** do modelo professor, com custo mais de uma ordem de grandeza menor que chamadas a APIs frontier. Os autores também observaram que modelos pequenos conseguem aproveitar catálogos relativamente grandes de componentes.

**Mecanismo.** A melhoria vem de restringir o espaço de ação. Em vez de pedir ao modelo para inventar HTML/CSS/JS, o sistema o transforma em um **seletor e configurador de primitives de experiência**. Isso reduz graus de liberdade e aumenta consistência.

**Implicação para produto.** Para um Experience Engine, vale separar o modelo que decide a experiência do renderer. O agente poderia gerar algo como `intent -> component selection -> props -> bindings`, enquanto componentes, acessibilidade e regras visuais permanecem controlados pelo produto.

**Experimento.** Comparar três arquiteturas: geração livre de frontend, UI declarativa por catálogo com modelo frontier e UI declarativa com modelo pequeno especializado. Medir custo, latência, erros funcionais, consistência visual e preferência humana.

**Riscos/limites.** O paper se concentra em dois domínios e qualidade de geração, não em uso longitudinal por usuários finais. Catálogos ruins podem limitar criatividade ou perpetuar padrões de UI inadequados.

Fonte: https://arxiv.org/abs/2609.04184

## 2. Psicologia computacional pode virar uma camada objetiva de geração de layout

**Descoberta.** *Grounding GUI Design in Computational Psychology*, submetido em 3 de setembro de 2026, combina Answer Set Programming com objetivos de layout como alinhamento, agrupamento, harmonia de cores, whitespace e preferências do designer.

**Evidência.** O trabalho foi avaliado em três estudos. Em dois estudos com usuários, layouts produzidos pelo modelo completo foram avaliados melhor que layouts aleatórios e que layouts baseados apenas em heurísticas simples. Designers também relataram utilidade para exploração e sketching inicial.

**Mecanismo.** O sistema codifica princípios de percepção e organização visual como restrições explícitas em vez de depender apenas do conhecimento implícito de um LLM. Isso funciona como uma espécie de **guardrail perceptual**.

**Implicação para produto.** Uma arquitetura híbrida parece promissora: `LLM propõe -> motor de constraints valida/otimiza -> renderer mostra`. Isso permitiria transformar leis de Gestalt, hierarquia, espaçamento, densidade e coerência em regras verificáveis.

**Experimento.** Gerar a mesma tela com LLM puro e com LLM + optimizer psicológico, medindo tempo para localizar ações, erros, carga cognitiva, preferência e número de correções solicitadas pelo usuário.

**Riscos/limites.** Regras explícitas podem otimizar estética ou organização média e ainda errar contexto, cultura, marca ou objetivo emocional. O paper está submetido ao *International Journal of Human-Computer Studies* e ainda é preprint.

Fonte: https://arxiv.org/abs/2609.03918

## 3. Agentes que usam o computador precisam ser desenhados para colaboração, não apenas automação

**Descoberta.** *Are We There Yet? Assessing Computer-Use Agents for Blind Users' Accessible Interaction with Desktop Applications*, submetido em 1º de setembro e aceito para a EMNLP 2026, avaliou agentes de uso do computador em workflows reais de pessoas cegas que usam leitor de tela.

**Evidência.** O estudo acompanhou **8 participantes por três semanas**, reunindo **1.258 comandos em 12 aplicações**. GPT-5 teve a maior taxa de sucesso entre os modelos avaliados, mas ainda apenas **52,5%**. A análise encontrou falhas de grounding, planejamento, acompanhamento de restrições e detecção de término da tarefa.

**Mecanismo.** A experiência falha quando o agente assume que concluir a tarefa é suficiente. Para um usuário sem acesso visual, é necessário também tornar observáveis o estado, as ações, as opções disponíveis e as consequências.

**Implicação para produto.** Agentes deveriam ter um **Accessible Action Narrator**: antes/durante/depois de ações relevantes, informar onde estão, o que pretendem fazer, o que mudou e como desfazer ou assumir manualmente.

**Experimento.** Comparar agente silencioso, agente com log técnico e agente com narrativa acessível de estado + confirmação seletiva. Medir sucesso, confiança calibrada, capacidade de recuperar erros e dependência de assistência externa.

**Riscos/limites.** Amostra pequena, somente adultos cegos, Windows, inglês e desktop. Os resultados não generalizam automaticamente para baixa visão, mobile ou outros sistemas operacionais.

Fonte: https://arxiv.org/abs/2609.00524

## 4. Generative UI fica mais controlável quando o processo é dividido em compromissos inspecionáveis

**Descoberta.** O framework **LEGOUI**, publicado como preprint em 4 de agosto, substitui a geração one-shot por etapas sequenciais registradas em uma UI-DSL com proveniência.

**Evidência.** Em 40 prompts reais, o estágio de análise de requisitos capturou requisitos explícitos com **mais de 95% de acurácia**, cobertura quase completa e zero redundância. Em estudos com usuários, o sistema produziu maior transparência, controlabilidade e alinhamento com intenção que ferramentas one-shot.

**Mecanismo.** Em vez de o usuário descobrir no final que o modelo entendeu algo errado, cada decisão intermediária vira um compromisso que pode ser inspecionado e corrigido.

**Implicação para produto.** O Experience Engine pode adotar um **Design Commitment Ledger**: objetivo, público, hierarquia, componentes, interações, estados, acessibilidade e estilo são persistidos antes da renderização final.

**Experimento.** Comparar `prompt -> UI` com `prompt -> plano inspecionável -> UI`. Medir tempo total, número de tentativas, correções tardias, sensação de controle e regressões entre versões.

**Riscos/limites.** Mais transparência pode aumentar carga cognitiva. O estudo sugere que decisões abstratas e relações entre componentes exigem suporte adicional para não virar microgerenciamento.

Fonte: https://arxiv.org/abs/2608.04293

## 5. Em experiências de bem-estar, congruência estética com a função pode importar mais do que “fofura” ou humanização

**Descoberta.** *Calm Beats Cute*, aceito em 19 de agosto de 2026, comparou quatro estilos visuais de um robô-companheiro de respiração: humanoide cartoon, animal, mecânico e humanoide meditativo.

**Evidência.** **126 participantes** avaliaram calor, competência, prazer, intenção de uso, presença social, confiança e preferência. O design animal teve maior warmth, o mecânico ficou pior em todas as medidas, mas o humanoide meditativo obteve as maiores avaliações de competência, confiança e preferência como guia de respiração, superando o humanoide cartoon apesar de níveis semelhantes de human-likeness.

**Mecanismo.** A percepção depende de **aesthetic-functional congruence**: a estética precisa combinar com a finalidade psicológica da experiência. Para relaxamento, sinais de serenidade podem importar mais do que sinais de simpatia ou fofura.

**Implicação para produto.** O estilo do agente deveria ser derivado do objetivo da experiência: `acalmar -> serenidade`, `agir -> energia`, `analisar -> precisão`, `explorar -> curiosidade`, em vez de uma persona visual universal.

**Experimento.** Manter conteúdo e capacidades iguais e variar apenas estética, ritmo, voz e microanimações conforme o objetivo. Medir confiança, intenção, desempenho e estado emocional pós-interação.

**Riscos/limites.** Estudo online baseado em vídeo, não uso prolongado real do robô. Preferências podem mudar por cultura, idade e domínio.

Fonte: https://www.frontiersin.org/journals/cognition/articles/10.3389/fcogn.2026.1886795/abstract

## 6. Timing da informação pode importar mais do que animação para administrar atenção

**Descoberta.** Um estudo publicado em 20 de agosto de 2026 avaliou interfaces AR-HUD para retomada de controle em direção automatizada, comparando apresentação estática/dinâmica e simultânea/progressiva.

**Evidência.** Em um desenho 2x2 com **21 motoristas licenciados** e eye tracking, a apresentação progressiva reduziu todas as medidas principais de busca visual e aumentou usabilidade percebida. A usabilidade média foi aproximadamente **78,3** no modo progressivo contra **69,9** no simultâneo. A condição estática-progressiva apresentou o melhor padrão geral.

**Mecanismo.** O sistema diminui competição atencional ao liberar informação em sequência: `alerta -> causa -> ação`, em vez de apresentar todos os elementos ao mesmo tempo.

**Implicação para produto.** Isso sugere um **Presentation Scheduler** separado do conteúdo. Mesmo que o agente saiba tudo, não precisa mostrar tudo imediatamente. O sistema pode ordenar a revelação conforme prioridade cognitiva.

**Experimento.** Para uma tarefa complexa de produto digital, comparar painel completo, UI animada completa e revelação progressiva orientada à decisão. Medir tempo até primeira ação correta, saccades/mouse scanning, erros e compreensão.

**Riscos/limites.** Estudo pequeno, cenários gravados e domínio automotivo. Menos fixações também podem significar menor exposição à informação; portanto, sequência precisa preservar conteúdo crítico.

Fonte: https://www.mdpi.com/1995-8692/19/4/92

## 7. Personalização adaptativa pode enfraquecer autorregulação por vias emocionais

**Descoberta.** Um estudo longitudinal recente sobre ambientes de aprendizagem adaptativos por IA fornece uma nova evidência do “paradoxo da personalização”.

**Evidência.** O desenho acompanhou **486 universitários em quatro universidades da China, em três ondas ao longo de um semestre**. A percepção de ambientes adaptativos por IA foi negativamente associada à aprendizagem autorregulada (`β = -0,19; p < 0,01`). Efeitos indiretos por prazer, ansiedade e tédio foram significativos e, em conjunto, responderam por **54,8% do efeito total**. Maior AI literacy atenuou parte dos efeitos emocionais negativos. O modelo explicou 28,4% da variância em autorregulação.

**Mecanismo.** Quando o sistema escolhe demais pelo usuário, ele pode reduzir sinais de controle, escolha e esforço produtivo necessários à autorregulação. A personalização altera não apenas relevância, mas também a experiência emocional de agência.

**Implicação para produto.** O Experience Engine precisa de um **Agency Preservation Guard**: personalizar conteúdo e ritmo sem remover decisões essenciais ao desenvolvimento de capacidade. Em certas etapas, o melhor comportamento do agente pode ser oferecer opções, pedir previsão ou exigir uma escolha antes de sugerir.

**Experimento.** Comparar personalização automática, personalização transparente/editável e personalização com “espaços de decisão” preservados. Medir desempenho imediato, autonomia percebida, autorregulação e capacidade sem IA depois.

**Riscos/limites.** O estudo é educacional, observacional-longitudinal e baseado em medidas autorrelatadas; não prova que toda personalização cause redução de autorregulação em outros domínios.

Fonte: https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1915839/abstract

## Experience Engine v7

```text
Usuário
   ↓
intenção + contexto + estado + capacidades de acesso
   ↓
AGENCY PRESERVATION GUARD
   ↓
DESIGN COMMITMENT LEDGER
   ↓
EXPERIENCE POLICY
   ├── objetivo psicológico
   ├── nível de controle
   ├── requisitos de acessibilidade
   ├── modalidade
   └── ritmo de apresentação
   ↓
DECLARATIVE UI CONTRACT
   ↓
component catalog + bindings
   ↓
COMPUTATIONAL-PSYCHOLOGY CONSTRAINTS
   ↓
PRESENTATION SCHEDULER
   ↓
AESTHETIC-FUNCTIONAL CONGRUENCE
   ↓
multimodal output
   ├── visual
   ├── texto
   ├── voz
   ├── haptics
   └── outros canais sensoriais
   ↓
ACCESSIBLE ACTION NARRATOR
   ↓
experiência
   ↓
resultado + compreensão + controle + capacidade posterior
```

## Insight principal da rodada

O achado que mais conecta os trabalhos de hoje é que **Generative UI provavelmente ficará mais robusta quando deixar de ser “IA desenhando livremente” e se tornar “IA escolhendo e combinando experiências dentro de um espaço estruturado de políticas, componentes, restrições psicológicas e mecanismos de controle”**.

Isso é importante porque resolve vários problemas ao mesmo tempo: reduz custo e latência, melhora verificabilidade, facilita acessibilidade, diminui regressões, preserva identidade visual e permite que o usuário intervenha antes que erros se propaguem.

Para um produto real, a arquitetura mais promissora parece ser híbrida: **LLM para intenção e composição + DSL para estado + catálogo para segurança + constraints para UX + métricas comportamentais para adaptação**.
