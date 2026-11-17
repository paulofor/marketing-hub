# Fonte revisada — PromptPoseFX: edição reversível de efeitos associados ao movimento

- **Estudo:** Zheng, K.; Ye, H.; Fu, H. *PromptPoseFX: Authoring Pose-Driven Visual Effects through Augmented Prompting and Interactive Refinement*. arXiv:2611.20485v1, submetido em 16/11/2026.
- **Fonte primária:** https://arxiv.org/html/2611.20485v1
- **Tipo:** preprint; estudo comparativo intraindivíduo e estudo exploratório. A publicação não representa evidência de eficácia comercial.

## Evidência encontrada

O PromptPoseFX associa comandos em linguagem natural a controles espaciais, temporais e de parâmetros, e gera efeitos 2D procedurais em camadas independentes sobre vídeo com rastreamento de pose. A comparação manteve o pipeline de geração no baseline, mas retirou os controles gráficos, permitindo comparar interface híbrida e interação somente textual.

No estudo comparativo com **12 participantes**, todos concluíram duas tarefas de reprodução de efeitos com o PromptPoseFX dentro do limite de 30 minutos; no baseline textual, **9 de 12** concluíram. A precisão espacial percebida foi em média **4,58** versus **2,33**; facilidade de controle espacial, **4,92** versus **1,92**; precisão temporal, **4,67** versus **3,25**; simplicidade do ajuste de parâmetros, **4,58** versus **1,83**, com diferenças estatisticamente significativas nas medidas reportadas. Houve também avaliações favoráveis de edição independente de camadas e menor carga de trabalho autorrelatada.

## Resultado realmente observado

O benefício observado refere-se a tarefas curtas de **edição de VFX 2D vinculados a movimentos corporais**, em ambiente experimental, e a avaliações dos participantes. O estudo **não mediu** retenção de vídeo, preferência de audiência, vendas, campanhas comerciais, retorno financeiro ou economia em produção.

## Hipótese interpretativa — não demonstrada como mecanismo causal

Controles visuais situados no próprio vídeo podem reduzir a ambiguidade de instruções apenas textuais: o criador indica diretamente *onde*, *quando* e *qual parâmetro* alterar. Camadas reversíveis podem reduzir risco de perder edições anteriores. A atribuição a menor carga cognitiva é plausível e compatível com autorrelatos, mas não equivale a prova neuropsicológica.

## Aplicação possível ao Marketing Hub

Avaliar, no pipeline do agente Apolo, uma interface de edição para efeitos simples em vídeos reais com: referência espacial clicável, timeline com marcadores, sliders gerados a partir de parâmetros e camadas independentes e reversíveis. A aplicação seria inicialmente **controle de qualidade criativa e retrabalho**, não promessa de aumento de conversão.

## Hipótese testável

Em peças comparáveis de vídeo UGC, editar efeitos de movimento com controles híbridos produzirá mais efeitos corretamente alinhados, menor tempo de revisão e menos regressões de outras camadas que editar com prompts de texto. Medir por auditoria visual cega, tempo, custo por ativo aprovado; avaliar eventos de retenção/CTA/vendas separadamente, apenas quando houver campanha real.

## Riscos e limites

Amostra de 12 pessoas recrutadas em redes pessoais/universitárias; tarefas guiadas com vídeos e efeitos específicos; backend e interface experimentais; componentes atuais baseados em p5.js e MediaPipe, insuficientes para alguns VFX complexos, contornos, profundidade e referências contextuais. Rastreamento de pose pode errar, romper continuidade ou deslocar elementos. Não extrapolar para vídeo generativo geral, sucesso comercial ou todos os criadores. Garantir preservação do original, reversibilidade, consentimento de imagem e revisão humana.
