# Fonte revisada — Genie 4 passa de pesquisa a API paga de mundos interativos

**Data da revisão:** 2026-11-19
**Janela do acontecimento:** 2026-11-18 a 2026-11-19
**Sistema:** Google DeepMind Genie 4, identificador `models/genie-4`
**Classificação de ciclo de vida:** ATIVO na Gemini API paga (Interactions API v1beta) e Google AI Studio; acesso no Project Genie RESTRITO a assinantes elegíveis AI Ultra; pesos FECHADOS. Acesso autenticado não testado nesta revisão.

## Mudança efetiva
Em 18/11/2026, o Google abriu Genie 4 no Project Genie para assinantes elegíveis de Google AI Ultra. Em 19/11/2026, anunciou a disponibilização para desenvolvedores pela Gemini API e AI Studio, com documentação e precificação oficiais. A novidade para integração comercial e prototipagem não é a primeira divulgação técnica do modelo (27/10), mas o acesso programático pago, com ID e streaming documentados.

Genie 4 cria um ambiente audiovisual navegável por texto e/ou uma imagem 16:9 inicial. Durante o streaming o usuário controla movimento/camera por teclado e altera ambiente com novos prompts: meteorologia, personagens, objetos e eventos. O modelo gera video 1280x720, 16:9, 24 fps e audio estéreo 48 kHz sincronizado; Google usa modelos Gemini/Omni como fundamentacao e raciocinio. Não é um modelo generico de edicao de video gravado.

## Preco e limites confirmados
- **Tarifa de saida:** US$ 17,50 por 1M tokens de video, audio incluso. Faturamento de 240 tokens por frame, 24 fps = 5.760 tokens/s = US$ 0,1008/s, cerca de US$ 6,05/min. Texto de entrada/referencia: US$ 1,50 por 1M tokens. Texto de saida: US$ 9 por 1M. Nao ha tier gratuito para Genie 4.
- **Duracao por sessao da API:** hard cap de 120 segundos; a documentacao alerta que a estabilidade visual pode piorar apos 60-90 segundos. A pagina geral de modelo menciona cerca de um minuto de interacao tipica; nao confundir isso com limite duro da API.
- **Quota:** Tier 1 = 2 sessoes simultaneas e 50 requests/dia; Tier 2 = 5/250; Tier 3 = 10/1000. Os limites reais dependem da conta.
- **Infraestrutura:** servicos nos EUA apenas; latencia de rede acima de 150 ms pode prejudicar direcao interativa, especialmente de outras regioes.
- **API/SDK:** Interactions API v1beta, WebRTC em navegador e WebSocket em Python/Node; exemplos oficiais em JavaScript/TypeScript e Python. Nao ha Batch, Flex, Priority.
- **Cobranca:** fechar sessao com `complete()` para interromper geracao/faturamento; apenas fechar a aba ou desconectar transport nao garante parada.
- **Restricoes:** nao suporta formato vertical 9:16, microfone/webcam ao vivo, chamadas de ferramenta, prompts com imagens de pessoas reais identificaveis ou marcas protegidas podem ser bloqueados. Continuidade espacial e persistencia de objetos em longo prazo ainda falham.
- **Licenca:** servico proprietario com API paga, sem pesos disponibilizados; avaliar termos do Google para uso comercial e direitos do material final. Saidas com SynthID invisivel. Nao inferir direito irrestrito sobre rostos ou marcas.

## Comparacao contextual e aplicacoes
- **Gemini Omni 1.2 Flash:** ATIVO via API paga, clips gerados offline ate 15 s e extensao/edicao ate 30 s. Em 720p com audio, aproximadamente US$ 0,07/s; oferece melhor encaixe para anuncios finais e redes sociais, mas nao mundo interativo continuamente controlavel.
- **Genie 3:** ATIVO de maneira limitada dentro do Project Genie; atualizacao de 18/11 permite sessoes de ate tres minutos, imagens panoramicas 360 e eventos por prompt. Menor fidelidade visual/audio em relacao ao Genie 4 segundo Google; nao atribuir a Genie 3 a nova API Genie 4.
- **Pika Enact:** PREVIEW de pesquisa, sem API publica verificada; controle de cena em tempo real conceitualmente proximo, mas nao uma alternativa contratavel.
- **Apple FastFrames:** PESQUISA com pesos publicos sob licenca nao comercial; reduz latencia de quadros, mas sem servico comercial.
- **Seedance 2.5 e Kling 4.0:** ATIVOS em canais de API documentados, focados em clips e edicao de video, nao navegacao de mundos ao vivo.
- **Sora 2:** DESCONTINUADO, app fechado em abril e API em setembro de 2026; nao considerar como opcao de implementacao.

**Impacto:** abre a possibilidade de prototipos de cenario interativo, previz cinematografica, experiencia audiovisual responsiva e simulacao para agentes. Nao substitui Kling/Seedance/Omni como gerador de anuncio acabado. Para produtos brasileiros, medir latencia a partir de Sao Paulo e custo do fluxo antes de escalar.

## Fontes primarias, consultadas em 19/11/2026
1. Google, anuncio API de 19/11: https://blog.google/innovation-and-ai/technology/developers-tools/genie-4-in-gemini-api/
2. Google, abertura Project Genie de 18/11: https://blog.google/innovation-and-ai/models-and-research/google-deepmind/genie-4-now-available-project-genie/
3. Google AI Studio, guia oficial Genie API: https://aistudio.google.com/docs/genie
4. Google AI Studio, precos: https://aistudio.google.com/docs/pricing#genie-4
5. Google AI Studio, limites: https://aistudio.google.com/docs/rate-limits#genie-4
6. Google DeepMind, model card: https://deepmind.google/models/model-cards/genie-4/
7. Google DeepMind, vista geral e limitacoes: https://deepmind.google/models/genie/

**Limites desta verificacao:** documentacao oficial consultada e contrastada com anuncio datado; nenhuma chamada autenticada, teste regional de latencia ou comparacao visual independente foi executada.
