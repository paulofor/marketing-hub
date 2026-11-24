# 3AGameFactory-Motion: animação 3D programável por agentes

Revisão: 24/11/2026. Artigo submetido em 23/11. China/Singapura e colaboradores. Status: pesquisa com repositório público, framework Apache-2.0, sem API comercial oficial confirmada. Não é um gerador de vídeo direto nem um novo conjunto de pesos.

O agente interpreta geometria e instruções, produz programas compactos de esqueleto e movimento; um executor determinístico calcula cinemática, curvas, deformações e correções geométricas, exportando GLB. DivDynBench: 50 objetos 3D, 850 tarefas em seis morfologias. Na tabela 4, a configuração Claude-ops5.5 com framework tem escore global 50,07/100 versus 48,19 sem framework e custo reportado US$ 0,047 versus 0,065 por tarefa (aprox. 28% menor). Não são preços comerciais nem custo total de filme. Avaliações dos autores, sem replicação independente. Experimento principal de movimento usa rigs de referência, portanto não prova desempenho ponta a ponta em malhas brutas. O escore absoluto mostra margem para melhoria.

O projeto documenta integração com Blender, Unreal, Unity, Godot e three.js, e agentes de código. A licença Apache-2.0 do repositório não substitui as licenças de modelos, assets ou serviços externos. Não foi executado nesta revisão.

Fontes:
- https://arxiv.org/abs/2611.30013
- https://arxiv.org/html/2611.30013v1 (tabelas 4–5 e método)
- https://github.com/OpenDCAI/GameFactory-3A (código e licença)

Aplicação: personagens/objetos reutilizáveis, pré-visualização e animação com restrições temporais e geométricas. Testar um produto mecânico com três movimentos e renderizar em vários ângulos sem regenerar o objeto.
