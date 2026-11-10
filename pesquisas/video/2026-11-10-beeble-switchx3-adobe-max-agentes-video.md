# Radar IA para Vídeo — 10/11/2026

**Janela:** novidades de 09–10/11/2026; comparação com registro de 09/11. **Mudanças que justificam alerta:** Beeble SwitchX 3.0 e Adobe MAX 2026 (edição + fluxos audiovisuais). **Observação complementar:** aliança Adobe/Disney de 09/11 e verificação do ciclo de vida. Fontes consultadas nesta data; demonstrações não foram testadas diretamente.

## 1. Beeble SwitchX 3.0 — mudança de cenário sem regenerar o ator (10/11)

**Estado ATIVO no Beeble Cloud.** Anúncio oficial de 10/11. Beeble treinou/ajustou ferramenta V2V sobre o MiniMax H3 de pesos abertos, mas não liberou pesos da variante SwitchX. O método proprietário Source-Locked Rendering afirma preservar, quadro a quadro, a performance mascarada de origem ao alterar o mundo e a iluminação externos. A versão 3.0 adiciona até quatro referências visuais, cenários animados, tracking de câmera sem marcadores e melhor controle temporal de eventos por prompt.

A saída Cloud Professional permite até 2160p, 10 bits e 360 quadros por geração (aprox. 12s a 30fps), preservando número de quadros e aspect ratio. **Cloud:** Creator US$19/mês (US$16/mês anual), 540 créditos; Professional US$75/mês (US$60/mês anual), 2.400 créditos e 4K/10-bit. Consumo 720p 6 créditos/s, 1080p 20, 2160p 80. **API SwitchX geral em beta público**: 720p US$0,10 por 30 quadros; 1080p US$0,30 por 30 quadros, com pré-pagamento mínimo US$50, atribuição visível em apps públicos e 5 jobs concorrentes. **Não confirmado que a API já expõe especificamente a versão 3.0, nem 4K.** Assinatura Cloud não inclui API.

**Importância:** publicidade, VFX, curtas e filmes podem explorar locações virtuais mantendo a atuação humana captada; comparar com geração de novo vídeo (Kling/Seedance) não equivale a benchmark de mesma tarefa. Teste recomendado para Marketing Hub: 1 apresentador autorizado, 3 ambientes, 720p, 10s, mede preservação do rosto, tempo de preparação, refações e custo total.

Fontes: https://beeble.ai/research/switchx-3-0 ; https://beeble.ai/pricing-cloud ; https://developer.beeble.ai/pricing ; https://developer.beeble.ai/

## 2. Adobe MAX 2026 — agentes e edição na linha do tempo (10/11)

**Estado Adobe Premiere/Firefly ATIVOS; novas ferramentas com graus distintos de acesso.**
- **Premiere Generative Remove: BETA**, remove objetos com máscara/tracking e recomposição gerativa da área, preservando o restante; processamento na nuvem e internet exigida. Bom para retirar logos/objetos/fundo indesejado de cenas já filmadas.
- **Premiere Generative Media Tool: GA**: geração de vídeo, efeitos, ambiência e música na timeline. Melhorias de separação de fontes de áudio / crosstalk e correção de cor.
- **Adobe Creative Agent / AI Assistants: BETA** em apps desktop, inclusive Premiere e After Effects. Organiza efeitos em sequência, tracking de máscaras, exposição e ajustes de cor em lote.
- **Firefly Graph: BETA**: pipeline reutilizável que liga modelos e ferramentas, com assistente, estudos de personagem, shot lists e padrões de marca. Firefly Video reúne storyboard, cortes, legendas, transições e b-roll.
- **MCP local para editar arquivos Premiere/After Effects de agentes externos: ANUNCIADO, NÃO DISPONÍVEL HOJE**; beta privada prevista para **início de 2027**. Não construir integração imaginando endpoint pronto.
- Novidades são de workflow e edição, não prova de superioridade de realismo sobre os modelos generativos chineses. Sem preço por segundo comum para os novos recursos confirmado nesta pesquisa.

**Importância:** Adobe se aproxima do pipeline audiovisual completo, incluindo texto para edição, efeitos, áudio e agente operando projeto nativo. Para Marketing Hub, comparar edição manual versus Adobe Graph / agente em 10 anúncios com identidade visual controlada, quando acessíveis.

Fontes: https://news.adobe.com/news/2026/11/adobe-transforms-creative-cloud-tools ; https://news.adobe.com/news/2026/11/adobe-firefly-expands ; https://helpx.adobe.com/premiere/desktop/edit-projects/edit-with-generative-ai/generative-remove.html

## 3. Disney e Adobe: modelos proprietários ajustados a IP (09/11)

**Status PARCERIA ATIVA; modelos internos da Disney, restritos.** As duas empresas anunciaram aliança plurianual para adaptar modelos Adobe Firefly Foundry ao conhecimento de personagens, ambientes e estilos protegidos da Disney, com uso em Imagineering, StudioLAB e ESPN Edge. Já há aplicação de esboço para imagem e criação de artes de marca; não foi anunciada API pública ou geração de filmes completos com esses pesos. Sinal importante de especialização corporativa e respeito a identidade de franquias, sem virar opção de compra pública.

Fonte: https://news.adobe.com/news/2026/11/adobe-and-the-walt-disney-company-alliance

## 4. DeepBrain AI Video Agent — disponibilidade verificada, data de lançamento não confirmada

**Status ATIVO SaaS; não é modelo de vídeo fundacional novo.** O AI Studios documenta o Video Agent por conversa, com documentos/URLs e roteiro editável, cenas, avatar, narração, uso de Seedance e outros modelos. Plano Free: 3 conversas de prompt/mês, até 5 versões por conversa, clipes finais até 1 minuto; geração avançada exige créditos pagos. Há documentação atualizada em 09/11, mas não há prova suficiente de lançamento inaugural ontem; por isso não incluído como novo lançamento desta rodada nem gerado card. É diferente do agente de avatar interativo para atendimento.

Fontes: https://help.aistudios.com/en/articles/17836885-video-agent-overview-what-you-can-make ; https://help.aistudios.com/en/articles/17742020-create-a-video-with-video-agent

## Disponibilidade e ciclo de vida em 10/11/2026

| Produto/origem | Status confirmado | Observação prática |
|---|---|---|
| Beeble SwitchX 3.0 / EUA | **ATIVO SaaS; API geral BETA** | Cloud 3.0 ativo; API v3.0 especificamente não confirmada |
| Adobe Creative Agent / EUA | **BETA em editores** | MCP local apenas beta privada planejada em 2027 |
| Adobe Premiere Generative Remove / EUA | **BETA** | Edição de footage, requer nuvem |
| Adobe Firefly Graph / EUA | **BETA** | Workflows reutilizáveis de criação |
| Adobe/Disney Foundry / EUA | **RESTRITO INTERNO** | Modelos sob IP da Disney, não API pública |
| AI Studios Video Agent / Coreia do Sul | **ATIVO SaaS** | Agente de produção, não gerador fundacional próprio novo |
| Kling 4.0 / China | **ATIVO via API Runway** | 3–30 s, 720p/1080p, áudio; promoção até 30/11 |
| Kling 4.0 Flash / China | **ATIVO via API Runway** | 3–20 s 720p, áudio, menor custo |
| Seedance 2.5 / China | **ATIVO via API Runway** | Até 30 s; entrada de referências pode custar adicional |
| Gemini Omni 1.2 Flash / EUA | **ATIVO GA/API** | Até 15 s, 360p–4K, áudio |
| Veo 3.1 GA / EUA | **ATIVO** | Previsão de aposentadoria não antes de 04/05/2027 no Google Cloud; Preview antigo encerrado |
| Seedance 1.5 Pro / canal ElevenLabs | **ENCERRAMENTO ANUNCIADO para 11/11/2026** | Não iniciar integrações neste modelo |
| OpenAI Sora 2 API | **DESCONTINUADO em 24/09/2026** | Não indicar como serviço disponível |

Fontes do ciclo de vida: https://docs.dev.runwayml.com/api-details/api_changelog/ ; https://elevenlabs.io/docs/changelog ; https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate

## Seleção de cards / conclusões para Marketing Hub
- Criar card **Beeble SwitchX 3.0**: caso mensurável de criação de variantes preservando atuação real.
- Criar card **Adobe MAX 2026**: pipeline criativo com agentes, Graph e controle de edição e limites reais de beta.
- Disney e Video Agent registrados só no relatório, sem cards: um é restrito e o outro não possui data de lançamento inaugural suficientemente comprovada.
- Não há prova de redução de preço geral dos modelos neste dia. **Nenhum teste pago ou benchmark independente realizado.**

## Integridade das fontes dos cards
- `pesquisas/video/cards/fontes/2026-11-10-beeble-switchx3-v2v.md` — SHA-256: `0f72bcc79003552cad0e7517813f5413e43f3738f0fdf25f564ca51fa2dfafd5`
- `pesquisas/video/cards/fontes/2026-11-10-adobe-max-agentes-video.md` — SHA-256: `0c13de4161148b77741776a25f00789f32c6e15e086a55e6cb91d63c06460068`
