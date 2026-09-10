# Radar IA para vídeo — 2026-09-10

## Resumo executivo

Três achados passaram o filtro de relevância desta rodada:

1. **Adobe Premiere 26.5 / Generative Media** — a ferramenta saiu do beta do Premiere e está disponível na versão 26.5 para gerar vídeo e efeitos sonoros diretamente na timeline. O editor pode usar frames do próprio projeto como referência e escolher entre Firefly e modelos parceiros. Generate Music e Generate Soundscape continuam beta, e o AI Assistant do After Effects permanece em beta.
2. **Suno v6** — lançado em 9 de setembro, aceita texto, áudio, imagens e **vídeo** como entrada para gerar música, oferece edição localizada de seções e chega a 8 minutos por geração. A Suno também iniciou a transição para aposentar os modelos anteriores e mover a plataforma para a família v6.
3. **Prime Video visual dubbing** — a Amazon colocou em produção um processo de lip-sync que altera visualmente os movimentos da boca para acompanhar áudio dublado por humanos. O rollout é limitado a *Maxton Hall* neste momento e não há API ou produto self-service.

Não encontrei, desde a rodada anterior, um novo Veo, Seedance, Wan, Kling, MiniMax ou Runway de magnitude semelhante que justifique notificação adicional.

---

## 1. Adobe Premiere 26.5: geração contextual passa para a timeline estável

**Status: ATIVO no Premiere 26.5 para geração de vídeo e efeitos sonoros.**  
**Generate Music / Generate Soundscape: BETA.**  
**After Effects AI Assistant: BETA.**

A Adobe anunciou em 8 de setembro e publicou a documentação atualizada em 9 de setembro de 2026. Na versão 26.5 do Premiere, a ferramenta **Generative Media** permite selecionar um intervalo da timeline, digitar um prompt e gerar mídia diretamente naquele ponto da sequência.

### O que mudou

Para geração de vídeo, o editor pode:

- usar texto como instrução;
- escolher o modelo na própria barra generativa;
- usar o **primeiro frame** da cena como referência;
- usar **primeiro e último frames** para criar transições;
- fornecer **múltiplos frames de referência** da própria sequência;
- ajustar parâmetros como resolução, aspect ratio, frame rate, seed e duração, conforme o modelo;
- gerar áudio junto ao vídeo quando o modelo escolhido oferecer suporte;
- receber o resultado diretamente como clipe editável na timeline.

A Adobe cita Firefly e parceiros como **Google Veo, Kling, Runway e Luma**. A geração usa créditos da Adobe; não é necessário manter uma assinatura separada de cada provedor. Alguns modelos parceiros ainda têm disponibilidade diferente para usuários individuais e planos empresariais.

O ponto mais importante é que a geração deixa de receber apenas um briefing textual e passa a aproveitar o **contexto editorial já existente**. Frames adjacentes aprovados podem orientar a próxima tomada exatamente no ponto em que ela precisa encaixar.

### Relação com o plugin da Runway acompanhado ontem

O plugin oficial da Runway e o Generative Media do Premiere são complementares:

- **Runway Plugin:** usa a conta/ecossistema Runway e expõe geração, Aleph 2.0, Ruby/HDR, upscale, remoção de fundo e modelos parceiros dentro do Adobe.
- **Premiere Generative Media:** é uma superfície nativa da Adobe, usa créditos Adobe e coloca o **contexto da própria timeline** no centro do fluxo generativo.

Para um harness audiovisual, isso reforça um padrão importante: **o melhor modelo pode variar por tarefa, mas o contexto do projeto precisa permanecer estável acima dos modelos**.

### Áudio e agente

A mesma família de ferramentas inclui:

- **Generate Sound Effects:** ativo, com modelo de áudio da Adobe e opção de usar a própria voz como guia de ritmo, timing e intensidade;
- **Generate Soundscape:** beta; analisa até 15 segundos do vídeo e cria ambiente/efeitos sonoros contextualizados e temporizados;
- **Generate Music:** beta; cria música instrumental com controle de BPM e loop;
- **After Effects AI Assistant:** beta; recebe comandos em linguagem natural para organizar projetos, escrever ou corrigir expressions, gerar imagens/vídeos e executar tarefas em múltiplas etapas.

A Adobe não publicou métricas demonstrando redução de tempo total, número de regenerações ou impacto comercial. Portanto, o ganho de workflow é real como capacidade do produto, mas seu efeito econômico ainda precisa ser medido.

Fontes primárias:

- https://blog.adobe.com/en/publish/2026/09/08/generate-create-directly-in-your-timeline-with-new-ai-powered-innovations-in-premiere-after-effects
- https://helpx.adobe.com/premiere/desktop/whats-new/whats-new.html
- https://helpx.adobe.com/premiere/desktop/edit-projects/edit-with-generative-ai/generate-media-with-generative-media-tool.html
- https://helpx.adobe.com/premiere/desktop/edit-projects/edit-with-generative-ai/generative-media-tool-faq.html
- https://helpx.adobe.com/after-effects/desktop/what-s-new/after-effects-beta.html

---

## 2. Suno v6: música pode ser criada a partir do próprio vídeo

**Status: ATIVO.**  
**v6 / v6-wild: Pro e Premier.**  
**v6-mini: todos os planos.**  
**Modelos anteriores: EM TRANSIÇÃO PARA APOSENTADORIA.**

A Suno lançou em **9 de setembro de 2026** a família **v6**, desenvolvida com parceiros da indústria musical como Warner Music Group, BMG e Believe.

As três variantes suportam até **8 minutos por geração**:

- **v6:** flagship, mais controlável e previsível;
- **v6-wild:** mais exploratório e imprevisível;
- **v6-mini:** variante mais rápida e disponível também no plano gratuito.

### O recurso que importa para vídeo

O v6 pode criar música usando **texto, áudio, imagens e vídeo como entrada**. Isso permite que um rough cut, demonstração de produto, trailer ou criativo já montado sirva como contexto para a composição.

O modelo também permite:

- editar uma seção usando linguagem natural sem refazer toda a música;
- alterar uma palavra ou linha da letra preservando o restante;
- combinar elementos de múltiplas fontes em um mashup;
- isolar/samplear trechos e criar novas estruturas a partir deles.

Para produção audiovisual, isso reduz uma etapa comum de tradução de contexto:

`vídeo -> humano/agente descreve clima e ritmo -> modelo musical`

pode virar:

`vídeo/rough cut -> modelo musical + instrução -> trilha -> revisão audiovisual`

Isso **não significa sincronização frame a frame**. A documentação não afirma que cortes, beats ou eventos específicos do vídeo sejam automaticamente alinhados com precisão. Um agente ainda deve revisar timestamps, momentos de tensão, demonstração, fala e CTA.

### Licença e limites

A Suno está movendo toda a geração para v6 e informa que os modelos anteriores serão retirados durante o rollout. Logo, modelos legados não devem ser tratados como a escolha estratégica atual.

Na política atual de download:

- downloads de teste do plano gratuito são apenas para uso pessoal;
- Pro recebe 20 downloads por mês;
- Premier recebe 60 downloads por mês;
- workflows dentro do Suno Studio não estão sujeitos a esses limites de download;
- direitos comerciais dependem do plano e dos termos vigentes.

Não há pesos abertos anunciados. Na documentação oficial consultada nesta rodada, não encontrei um endpoint público específico do v6; a disponibilidade confirmada é no produto Suno.

### Comparação com Lyria 3.5 e Eleven Music v2

- **Suno v6 — ATIVO:** vantagem nova é aceitar vídeo como entrada e oferecer edição musical localizada; integração programática pública específica do v6 não foi identificada.
- **Google Lyria 3.5 — ATIVO na Gemini API, sem shutdown anunciado:** custa US$0,08 por música completa e é muito atraente para geração programática barata, mas não há indicação equivalente de entrada de vídeo no endpoint.
- **Eleven Music v2 — ATIVO com API:** custa atualmente US$0,15/min na API e é mais maduro para integração, streaming, composição por seções, stems e inpainting.

Assim, o Suno avançou especialmente na **ponte vídeo -> música**; ElevenLabs continua mais forte para pipeline via API, e Lyria continua muito competitivo em custo.

Fontes primárias:

- https://blog.suno.com/blog/introducing-v6
- https://help.suno.com/en/articles/13924801
- https://help.suno.com/en/articles/13924929
- https://help.suno.com/en/articles/13876865
- https://ai.google.dev/gemini-api/docs/pricing
- https://elevenlabs.io/pricing/api

---

## 3. Prime Video aplica lip-sync visual sobre dublagem humana

**Status: ATIVO, mas LIMITADO a títulos selecionados.**  
**API/produto self-service: NÃO DISPONÍVEL.**

Em **9 de setembro de 2026**, o Prime Video colocou em produção uma tecnologia de **visual dubbing** que usa IA e VFX para modificar os movimentos da boca e fazê-los acompanhar uma faixa dublada por humanos.

A estreia ocorre globalmente na dublagem em inglês das temporadas 1 e 2 de **Maxton Hall**. A terceira temporada, prevista para 9 de dezembro, também usará o recurso, e a Amazon afirma que pretende expandi-lo para outros títulos.

O aspecto interessante é arquitetural: a Amazon não está substituindo toda a dublagem humana por voz sintética. Ela usa o áudio localizado como fonte de verdade e aplica IA à camada visual para reduzir a discrepância labial.

Isso produz um pipeline diferente:

`performance original -> dublagem humana/localizada -> lip-sync visual por IA/VFX`

Para publicidade e vídeos explicativos, a ideia é potencialmente útil porque permite preservar uma locução localizada de alta qualidade e adaptar apenas a boca do apresentador ou ator. Porém, a Amazon não oferece esse sistema como API, não publicou preço e não apresentou métricas de retenção ou preferência. Portanto, é **prova de adoção em produção**, não uma ferramenta disponível para o Marketing Hub hoje.

Como alternativa programável, plataformas especializadas de lip-sync/dublagem continuam mais operacionais; por exemplo, a Sync Labs documenta um fluxo que combina tradução/dublagem e lip-sync em uma única chamada da API.

Fontes:

- https://www.aboutamazon.com/news/entertainment/prime-video-lip-sync-technology
- https://sync.so/docs/tutorials/dubbing

---

## Conclusão

A mudança mais útil para o Marketing Hub nesta rodada é a consolidação de **contexto acima do modelo**.

O Premiere 26.5 mostra que frames da própria montagem podem ser usados como contexto para gerar a próxima mídia e que o modelo pode ser escolhido conforme a tarefa. O Suno v6 leva a mesma ideia ao áudio, permitindo que **o próprio vídeo seja uma entrada para a composição musical**. O Prime Video demonstra uma terceira forma de composição por camadas: preservar o áudio localizado e modificar apenas a imagem para alinhar os lábios.

Para o videomaker, o padrão que emerge é:

`estado/contexto do projeto -> seleção da ferramenta/modelo -> geração específica -> revisão -> retorno ao mesmo projeto`

Isso favorece um harness que preserve frames, timestamps, roteiro, produto, identidade visual e áudio aprovado independentemente do modelo usado em cada etapa. Nenhuma das novidades prova aumento de conversão; os efeitos comerciais devem ser testados no funil real.
