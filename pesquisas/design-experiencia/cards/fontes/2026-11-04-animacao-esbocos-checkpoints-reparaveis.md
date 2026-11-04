# Fonte revisada — checkpoints reparáveis em animação de esboços (04/11/2026)

## Evidência encontrada
**Fonte primária:** Hien Trinh, Tan-Hiep To e Trung-Nghia Le, *OmniScribble: Mixed-Initiative Authoring of Animated Sketches through Inspectable and Repairable AI Decisions*, arXiv:2611.04284v1, submetido em 03/11/2026. https://arxiv.org/html/2611.04284v1

Protótipo para animar esboços com múltiplos objetos, decomposição de elementos e trajetórias expostas como artefatos editáveis. O usuário pode aceitar, corrigir ou regenerar propostas de IA em pontos de verificação.

Estudo intraindivíduo com **oito participantes** comparou autoria manual e assistida. A mediana dos tempos foi 232,68 s no modo manual e 71,68 s no modo assistido; a **redução mediana calculada por participante** foi 71,44% (p=0,0078 no teste pareado). A carga subjetiva mediana Raw NASA-TLX passou de 15,08 a 5,17 (p≈0,016). Em 32 checkpoints, houve 27 aceites diretos e cinco intervenções; quatro das cinco intervenções incidiram sobre trajetórias.

A métrica de tempo termina ao solicitar renderização: exclui a latência do render, a reprodução e os questionários. Não compara com um gerador end-to-end opaco.

## Hipótese interpretativa
Expor objetos e trajetórias separadamente pode transferir esforço repetitivo para a IA sem eliminar a capacidade de intervenção humana localizada. Não foi isolado o efeito causal dos checkpoints em relação à geração integral de uma só vez.

## Aplicação possível
Testar, no Apolo, representação intermediária editável para cenas animadas simples de produtos: camada de objetos, trajetórias, aprovação por etapa, reparo localizado e regeneração parcial. Avaliar a entrega completa, inclusive renderização, retrabalho e avaliação cega de qualidade.

## Resultados reais observados
O sistema experimental reduziu tempo de autoria de especificações de animação e carga percebida nas tarefas predefinidas. **Não demonstrou** maior conversão, retenção de espectadores, qualidade comercial de vídeos ou desempenho em projetos complexos.

## Limitações e riscos
Amostra N=8, tarefas curtas predefinidas, esboços simples, sem teste longitudinal ou baseline de geração integral. IA pode errar segmentação, geometria e trajetórias; regenerações podem sobrescrever decisões corretas. Não extrapolar 71,44% para toda produção audiovisual.
