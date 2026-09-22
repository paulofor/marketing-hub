# Fonte revisada — Xiaomi MiMo-V2.6 como revisor audiovisual open-weight

Data da revisão: 2026-09-22

## Achado

A Xiaomi lançou e abriu os pesos de MiMo-V2.6-Pro-RL e MiMo-V2.6-Flash-RL sob licença MIT. Os dois modelos são nativamente multimodais para texto, imagem, vídeo e áudio, têm contexto de 1 milhão de tokens e suportam tool calling. A Xiaomi também publicou código e ambientes de reinforcement learning e descreve mini-harnesses composáveis para separar system prompts, ferramentas e gerenciamento de contexto.

Os modelos produzem texto, não vídeo. Portanto, o uso relevante no videomaker é como camada de compreensão, revisão e decisão sobre material audiovisual, chamando ferramentas externas para corrigir ou renderizar.

## Status e disponibilidade

- MiMo-V2.6-Pro-RL: ATIVO; pesos públicos no Hugging Face sob MIT; API oficial e OpenRouter.
- MiMo-V2.6-Flash-RL: ATIVO; pesos públicos no Hugging Face sob MIT; API oficial e OpenRouter.
- Saída nativa: texto; não é renderer de vídeo.
- Pro: 1,02T parâmetros totais / 42B ativados.
- Flash: 309B totais / 15B ativados.
- Contexto: 1M tokens.
- Modalidades de entrada: texto, imagem, vídeo e áudio.

## Preço oficial da API

MiMo-V2.6-Flash:
- cache hit: US$ 0,0028 / 1M tokens;
- input cache miss: US$ 0,14 / 1M;
- output: US$ 0,28 / 1M.

MiMo-V2.6-Pro:
- cache hit: US$ 0,0036 / 1M;
- input cache miss: US$ 0,435 / 1M;
- output: US$ 0,87 / 1M.

## Implicação

A novidade reforça o princípio de manter o revisor audiovisual separado do renderer. Agora existe uma alternativa open-weight permissiva que pode assistir e ouvir material, raciocinar, usar ferramentas e, em ambientes com infraestrutura suficiente, ser auto-hospedada. Isso reduz dependência de um fornecedor único e permite versionar modelo, harness e critérios de revisão de forma mais controlável.

Para o Marketing Hub, o uso mais direto é `rough cut -> revisão audiovisual -> decisão -> ferramenta de correção -> nova versão`, preservando gates humanos para claims, preço, CTA e fidelidade do produto.

## Limitações

Os checkpoints são muito grandes para a VPS atual do Marketing Hub e exigem infraestrutura de GPU substancial para self-hosting. A licença MIT facilita uso comercial, mas não elimina a necessidade de validar direitos sobre os vídeos, vozes, marcas e demais materiais processados. Claims de benchmark da Xiaomi não devem ser tratados como prova independente.

## Fontes

- Xiaomi MiMo — anúncio MiMo-V2.6: https://mimo.mi.com/docs/en-US/news/latest/v2-6
- Hugging Face — MiMo-V2.6-Pro-RL: https://huggingface.co/XiaomiMiMo/MiMo-V2.6-Pro-RL
- Hugging Face — MiMo-V2.6-Flash-RL: https://huggingface.co/XiaomiMiMo/MiMo-V2.6-Flash-RL
- Xiaomi MiMo — MiMo-V2.6-Pro: https://mimo.mi.com/models/en-US/mimo-v2.6-pro
- Xiaomi MiMo — MiMo-V2.6-Flash: https://mimo.mi.com/models/en-US/mimo-v2.6-flash
