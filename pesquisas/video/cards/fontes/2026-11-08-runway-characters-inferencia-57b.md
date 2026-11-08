# Runway Characters — eficiência de inferência em vídeo conversacional

**Data da publicação técnica:** 05/11/2026 (artigo Runway Research). **Artigo arXiv:** 03/11/2026 (primeira versão). **Verificação:** 08/11/2026.

## Fonte primária e natureza da novidade

- Runway Research, *Real-Time Video at Batch Size One* (05/11/2026): https://runway.com/news/research/real-time-video-at-batch-size-one
- Artigo técnico, *Runway Characters: Serving a 57B Video Diffusion Transformer for Real-Time Conversation*, arXiv 2611.05642: https://arxiv.org/abs/2611.05642
- Ajuda oficial, *Runway Characters* (interface, API, duração e preços): https://help.runwayml.com/hc/en-us/articles/49557780326163-Runway-Characters

**Status em 08/11: ATIVO**, interface web para demonstração e API de integração documentada. Modelo e pesos **proprietários**. **Não** houve anúncio de novo preço, nova API ou lançamento de um novo modelo em 08/11. Trata-se da **divulgação técnica de uma melhoria já implantada**, identificada nesta rodada e ausente dos relatórios anteriores.

## Resultados divulgados (medições do próprio fabricante)

- Aproximadamente **57 bilhões de parâmetros**, vídeo gerado a **704×1088**, **24 quadros/s**, quatro quadros por bloco; orçamento de **166,7 ms** por bloco.
- A Runway informa ter reduzido a infraestrutura por sessão de **8 GPUs H100 para 4 H100**. No mesmo conjunto de quatro H100, a implementação inicial exigia **240,9 ms** por passo; a otimizada exige **159,3 ms** (1,51×), com kernel de comunicação fundido podendo atingir **147,4 ms**.
- Manutenção do cache de atenção caiu de **69,7 ms para 6,2 ms** por passo, ao aplicar RoPE/FP8 uma vez na escrita e usar armazenamento circular.
- Em sete semanas de telemetria, 99,5% das sessões que transmitiram por mais de um segundo apresentaram média acima de 23,5 fps; métricas ainda são internas.
- Mediana de resposta conversacional **2,13 s**, sendo **1,42 s atribuídos ao agente de voz** e **0,49 s ao pipeline de vídeo**.
- Estudo de lip-sync limitado a 51 clipes analisáveis; diferenças médias de pontuação de até 0,03, sem equivalência absoluta comprovada.

## Disponibilidade e limites comerciais

- Ajuda oficial: interface web para demonstrações de até **2 minutos**; integração pela API e plataforma de desenvolvedor para até **5 minutos** por sessão, conforme tabela atual (conta e regras contratuais devem ser conferidas).
- A página informa **2 créditos por 6 segundos (US$ 0,20/min)**; verificar modalidade e valores da conta antes de contratar. A economia de hardware **não foi anunciada como desconto ao consumidor**.
- Idioma principal testado: inglês; português e outros idiomas são suportados, mas não têm avaliação equivalente divulgada. São necessários imagem autorizada do personagem, controle de consentimento, proteção de identidade, testes de latência e de qualidade em PT-BR.
- Não há pesos abertos nem reivindicação de que quatro H100 tenham custo viável para hospedar independentemente esta mesma implementação.

## Relevância e hipótese de produto

A eficiência de infraestrutura é relevante para atendentes visuais interativos, tutores, apresentações ao vivo, NPCs e experiências audiovisuais personalizadas. No Marketing Hub, testar protótipo de apresentador de produto com interação vocal, comparando atendimento sem avatar, avatar Runway e alternativas de acesso limitado (Atmee V2, Google Live Avatar). Medir conversão, latência, minutos faturados, PT-BR e requisitos legais, sem extrapolar automaticamente a redução de 50% de GPUs para uma redução de 50% no preço da API.
