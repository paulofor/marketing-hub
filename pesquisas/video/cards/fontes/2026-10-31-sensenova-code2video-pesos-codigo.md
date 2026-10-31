# Fonte revisada — SenseNova 6.8 Code2Video (31/10/2026)

## Fatos verificaveis
- Fabricante: SenseNova/SenseTime, China. Repositorios de pesos e codigo publicados em torno de 24–28 de outubro; anuncio publico em 28/10/2026 segundo fontes secundarias.
- Produto: SenseNova 6.8 Code2Video, fine-tune de Qwen3.8-27B com cerca de 27 bilhoes de parametros. Contexto recomendado 512K (a configuracao nao foi testada por nos).
- Status: pesos publicos e baixaveis em Hugging Face; skill code-video, scripts, templates e demonstracoes em GitHub. Sem API SaaS oficial com preco documentado. Implantacao experimental pela propria equipe.
- Licenca: model card do Hugging Face declara Apache 2.0 para os pesos. Isso nao atribui automaticamente essa mesma licenca a todo recurso e dependencia do workflow.
- Metodo: um agente escreve codigo que define cada quadro como funcao do tempo; renderiza, analisa quadros, corrige e edita cenas em iteracoes. Inclui cenas 2D/3D estilizadas, motion graphics, explicadores, tipografia, dados, simulacao e audio procedural.
- Limite declarado pela equipe: nao gera videos fotorrealistas como um modelo pixel-based. A equipe nao publicou benchmark sistematico. Runs completos podem consumir horas de tempo de agente; exemplos de deploy do fornecedor usam tensor parallelism 4.
- Diferencial: objetos, texto, precos, graficos, movimentos e duracoes podem ser alterados no codigo, mantendo outras partes identicas. Potencial para variantes publicitarias deterministicas e material didatico com formulas corretas.
- Risco: o codigo gerado precisa executar em sandbox; medir horas de GPU, custo real, falhas de render, qualidade artistica e licencas de assets antes de uso comercial.

## Comparacao e aplicacao
- Kling 4.0 (China) via Runway Dev (EUA): API ativa para video audiovisual fotorrealista em 720p/1080p, ate 30s; pesos fechados. Tarefa diferente de gerar motion graphics por codigo.
- Runway Gen-4.5, Wan 3.0 e Google Veo 3.1 GA: modelos de geracao de video por inferencia; diferentes do controle programatico do SenseNova.
- Marketing Hub: gerar uma animacao base de oferta e derivar 10 variantes com precos, textos, cores e timing sem refazer a cena toda.
- Hipotese para testar: o tempo e custo de revisoes incrementais caiem frente a regeneracoes de clips inteiros; ainda sem prova.

## Fontes
1. https://github.com/OpenSenseNova/SenseNova-6.8-Code2Video
2. https://huggingface.co/sensenova/SenseNova-6.8-Code2Video
3. https://github.com/OpenSenseNova/SenseNova-6.8-Code2Video/tree/main/skills/code-video
4. https://neuralbrew.ai/en/articles/2026/10/sensenova-6-8-code2video-sensetime-video-through-code-open-source/
5. https://aiwiki.ai/wiki/sensenova_6_8_code2video

Verificacao: 31/10/2026. Qualidade final e custo total nao testados independentemente.
