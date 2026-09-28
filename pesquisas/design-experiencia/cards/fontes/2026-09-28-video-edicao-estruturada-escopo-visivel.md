# Fonte revisada — Estrutura e escopo visível em edição generativa de vídeo

## Evidência encontrada

O preprint *CraftTrace: Unflattening Videos into Malleable, Creation-Inspired Structures for Generative Editing* (arXiv:2609.30623v1, 24/09/2026) apresenta um protótipo que reconstrói um vídeo final em uma estrutura editável de roteiro, cenas, personagens, planos e relações. O agente usa essas dependências para localizar os planos afetados por uma intenção global, adaptar a instrução a cada plano e propagar apenas as mudanças relevantes, mantendo histórico de versões e rollback.

O estudo de usabilidade teve 12 participantes com experiência prévia em ferramentas de vídeo com IA. Os autores não fizeram testes estatísticos inferenciais devido ao tamanho da amostra. Descritivamente, 10/12 consideraram o sistema controlável, 12/12 disseram que ele ajudou a entender o escopo da edição, 12/12 disseram que os workspaces ajudaram a localizar o alvo e 11/12 consideraram intuitivo expressar a intenção.

Uma revisão adicional incluiu cinco profissionais de cinema e mídia com média de 17 anos de experiência. Eles destacaram valor em tornar estrutura, escopo e progresso da edição inspecionáveis; também relataram que maior autonomia pode ser aceitável em ideação, enquanto etapas próximas da produção final exigem supervisão mais estreita e preservação do material que não deveria mudar.

Em benchmark técnico, mantendo os mesmos modelos de geração, a recuperação de estrutura em 20 vídeos acima de 30 segundos levou em média 5,59 minutos no CraftTrace e 29,88 minutos no baseline Codex. Em 50 tarefas de edição, excluindo tempo de geração/filas, o workflow levou 2,99 minutos contra 10,47 minutos no Codex. Essa comparação mede runtime do workflow, não qualidade independente do vídeo final.

## Hipótese interpretativa

Externalizar a estrutura e as dependências do vídeo pode reduzir o esforço de localizar o que uma mudança afeta e, principalmente, tornar a automação inspecionável. O benefício pode vir menos de “gerar mais” e mais de saber o que deve mudar e o que deve permanecer intacto.

## Aplicação possível

No pipeline de vídeo do Marketing Hub, representar roteiro, cenas, personagens, claims, planos, assets e dependências como objetos persistentes. Antes de executar uma alteração global, mostrar o escopo estimado, permitir restringi-lo e registrar versões para rollback. Usar autonomia maior em exploração e controles mais rígidos perto do ativo final.

## Resultado real observado

Os resultados humanos são descritivos e vêm de amostra pequena. O benchmark técnico encontrou menor runtime do workflow, mas não avaliou independentemente a qualidade final nem efeito em retenção, CTA, conversão ou vendas.

Fonte primária: https://arxiv.org/abs/2609.30623
