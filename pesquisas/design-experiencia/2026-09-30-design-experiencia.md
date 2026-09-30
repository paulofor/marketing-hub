# Radar Design de Experiência — 2026-09-30

Princípio da rodada: **fluência e autonomia não podem substituir evidência, provenance e controle explícito**.

## Achados

### Failure-Transparent Agents
O preprint de 28/09 avaliou 100 tarefas, seis modelos e 3.600 respostas. Após falha de ferramenta, falso sucesso caiu de 22,8% no baseline para 9,3% com instrução de transparência e 0,8% com contrato estruturado de evidência; detalhes fabricados também caíram a 0,8%. Aplicação: separar `observado`, `inferido`, `faltante` e `confirmado` na execução agentic. Limite: benchmark controlado.
Fonte: https://arxiv.org/abs/2609.35732

### Dots e autonomia persistente
A OpenAI lançou em 29/09 agentes persistentes com pesquisa proativa read-only, regras de permissão, auto-review, aprovações e Activity View. Aplicação: classificar ações em pesquisar, preparar, agir reversivelmente, agir com efeito externo e ação sempre humana. Limite: caso de produto recém-lançado.
Fonte: https://openai.com/index/introducing-dots/

### Multi-agent e sinceridade percebida
*Shaping Opinion* (29/09) expôs 120 adolescentes e um piloto de 25 adultos a debates sintéticos; no piloto adulto, o debate sintético foi percebido como mais sincero que o discurso humano equivalente (d=2,04). Aplicação: deixar explícito que os participantes são agentes sintéticos e não tratar concordância entre agentes como prova social. Limite: paper sob revisão e piloto adulto pequeno.
Fonte: https://arxiv.org/abs/2609.37369

### Carga cognitiva contextual
*Beyond Productivity* acompanhou 21 desenvolvedores por quatro dias em dois sites da SAP. Carga percebida variou com uso de GenAI e contexto da tarefa; sinais fisiológicos acrescentaram informação limitada. Aplicação: medir `supervision cost` por correções, re-prompts, switches e tempo de revisão. Limite: N=21 e domínio de software.
Fonte: https://arxiv.org/abs/2609.37645

### Geração visual e drift
Estudo com 11 pares arquiteto-cliente encontrou maior entendimento compartilhado e participação do cliente com geração visual em tempo real, mas também viés estilístico e mudanças imprevisíveis entre gerações. Aplicação: usar `intent lock + diff visual`. Limite: N=11, sem métrica comercial.
Fonte: https://arxiv.org/abs/2609.34118

### Resumos de IA e ancoragem
*First Impressions* realizou RCT pré-registrado com 278 participantes. Resumos de IA e reviews humanos alteraram atitude e intenção de compra; a primeira fonte ancorou julgamento e reviews humanos deslocaram melhor uma âncora existente. Aplicação: testar resumo primeiro, reviews primeiro e resumo junto das evidências, medindo CTA, checkout e pagamento. Limite: intenção autorrelatada e compra hipotética.
Fonte: https://arxiv.org/abs/2609.14900

## Experience Engine v30

Direção: `Permission Class → Evidence Contract → Persistent State → Provenance → Supervision Cost → Resultado humano`.

## Cards

O guia atual continua aceitando somente `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`.

Foi selecionado um candidato: `sumario-ia-ordem-ancoragem-reviews`, coleção `neuromarketing`, por transformar um RCT de julgamento de compra em hipótese testável de ordem/apresentação de resumos e reviews. SHA-256 da fonte revisada: `e4a36337492e447133940c4c7efd273ed0d2cd829e5358e30c88961874ec22fa`.

Os demais achados ficaram sem card por serem principalmente harness, caso de produto ou evidência estreita para as coleções aceitas. Nenhum card foi enviado para revisão, ativado ou arquivado.
