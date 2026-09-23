# Fonte revisada — Tesseract torna edição local operável por agente

Data da revisão: 2026-09-23
Coleção: video
CardKey: video-projeto-estruturado-editavel-agente

## Achado

A Mirage lançou publicamente o Tesseract, uma suíte de criação audiovisual desenhada para ser operada por agentes. Em 23/09/2026, a versão 0.2.0 passou a oferecer Linux x86_64, além de macOS e Windows, e adicionou exportação em 4K e 60 fps. O fluxo usa um projeto editável `.tsrct` com conceitos nativos de vídeo, incluindo camadas, composições, keyframes, máscaras, adjustment layers, timing e áudio.

O Tesseract não é um modelo de geração de footage ou avatares. Ele recebe mídia existente — gravada ou gerada por outros modelos — e permite que o agente corte, componha, anime tipografia/gráficos, ajuste som, faça preview, revise uma parte específica e renderize o resultado localmente.

## Status e disponibilidade

- Tesseract Runtime/CLI: ATIVO.
- Versão verificada: 0.2.0, publicada em 23/09/2026.
- Plataformas: macOS, Windows e Linux x86_64, incluindo ambientes de agente compatíveis em nuvem.
- Plugin/skills para ChatGPT/Codex: disponíveis pelo repositório oficial; o README aponta instalação direta e plugin de ChatGPT/Codex.
- Renderização: local.
- Preço do engine: gratuito; custos do modelo/agente usado para orquestrar são separados.
- Licença: proprietária, não open source. Uso profissional/comercial é permitido pelos termos para negócios com receita anual abaixo de US$ 1 milhão; negócios cobertos a partir desse limiar precisam de acordo separado com a Mirage.
- Direitos de output: os termos declaram que o usuário mantém os direitos sobre Input e Output, sujeitos aos direitos de terceiros.

## Por que importa

O avanço não está em gerar pixels melhores, e sim em dar ao agente uma representação de projeto audiovisual persistente e granular. Em vez de “prompt -> MP4”, o fluxo passa a ser:

`briefing -> projeto editável -> preview -> revisão localizada -> render`

Isso permite alterar somente um título, keyframe, faixa de áudio, enquadramento ou composição sem destruir partes já aprovadas.

Para o Marketing Hub, o princípio é manter o estado de edição separado do renderer generativo. Um modelo pode gerar a tomada, outro pode avaliar, e Tesseract ou ferramenta equivalente pode fazer a montagem e acabamento mantendo o projeto reeditável.

## Comparação

- Runway Workflows: forte em grafo cloud multimodelo e integração entre geração e pós-produção.
- DaVinci Resolve + MCP: mais completo como NLE profissional, com ferramentas tradicionais de edição e finishing.
- Fotor Agent: referência de timeline multifaixa editável dentro de um produto web.
- Tesseract: diferencia-se por ser agent-native, local e por expor edição, motion graphics e som diretamente a agentes sem exigir automação de interface.

## Riscos e limites

- Não gera footage nem avatar por conta própria.
- O engine é proprietário, apesar de o repositório e skills serem públicos.
- A licença comercial tem limiar de receita anual e restrições contra construir produto concorrente.
- Linux foi adicionado agora; estabilidade e desempenho em ambientes de nuvem/VPS ainda precisam ser testados no workload real.
- A automação deve manter checkpoints e aprovação humana para mudanças comerciais sensíveis.

## Fontes revisadas

- Mirage — Tesseract: https://mirage.app/tesseract
- GitHub oficial — mirage-hq/Tesseract: https://github.com/mirage-hq/Tesseract
- GitHub Releases — Tesseract 0.2.0: https://github.com/mirage-hq/Tesseract/releases
- Mirage — Tesseract Terms: https://mirage.app/legal/tesseract-terms
- Fortune / comunicado Mirage, 22/09/2026: https://fortune.com/press-releases/mirage-tesseract-video-creation-ai-agents-2026-09-22/
