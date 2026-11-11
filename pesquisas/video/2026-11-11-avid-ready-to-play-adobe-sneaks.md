# Radar IA para Vídeo — 11/11/2026

**Janela avaliada:** anúncios de 10–11/11/2026 não incluídos no relatório de 10/11, com verificação de ciclo de vida em 11/11. **Novidade operacional relevante:** extensão Avid Ready to Play, lançada em 10/11. **Novidade de pesquisa importante:** Adobe MAX Sneaks, anunciados em 11/11. Os protótipos Adobe não estão disponíveis para produção. Fontes primárias verificadas; nenhum serviço pago foi testado.

## 1. Avid Ready to Play: IA diretamente no Media Composer (10/11, novo nesta rodada)

**Status ATIVO, mas LIMITADO aos planos Media Composer Ultimate/Enterprise.** Extensão disponível para Windows e macOS. Integra modelos Google Gemini/Veo ao editor com três fluxos:
- **Create Video:** criar takes por texto ou imagem de referência, com saída para bins, útil para B-roll, storyboard e imagens provisórias.
- **Modify Video:** produzir versão alternativa de clipe com mudanças por prompt (por exemplo, retirar microfone), preservando original.
- **Analyze Video:** acrescentar descrição por IA aos metadados de clipes para localizar material audiovisual não catalogado.

**Preço real:** sem taxa adicional pela extensão para assinantes elegíveis, mas os modelos Google usados por Vertex AI/Gemini exigem projeto Google Cloud e cobrança separada de inferência. Não equivale a produção gratuita. Nenhum preço universal por minuto foi anunciado. A extensão não é API autônoma pública. **Ready to Edit**, agente de preparação de projetos, segue apenas apresentado/previsto, não lançado.

**Comparação:** o Premiere/Firefly da Adobe também insere IA na timeline, enquanto Runway Aleph 2.0 e Beeble SwitchX 3.0 editam vídeos existentes. Avid diferencia-se por combinar geração, modificação e compreensão semântica de acervo no fluxo de trabalho de pós-produção, com integração direta a bins e metadados. Não há prova independente de superioridade de qualidade.

Fontes: https://www.avid.com/press-room/2026/11/avid-launches-ready-to-play-putting-new-ai-creative-capabilities-into-editors-hands ; https://www.avid.com/media-composer/extensions/ready-to-play

## 2. Adobe MAX Sneaks: controlar o tempo e a consistência das cenas (11/11)

**Status: PESQUISA/PRÉVIA, sem API ou serviço comercial ativo confirmado.** Esta divulgação de 11/11 é **distinta** das funcionalidades Firefly Graph/Creative Agent/Generative Remove anunciadas ontem, que são betas ou produtos disponíveis.

Projetos audiovisuais de pesquisa:
- **Scene Sweep:** transmitir uma mesma edição por várias tomadas e ângulos, com compreensão da continuidade.
- **Screen Test:** criar personalidade, aparência e voz de personagens e conversar com eles durante a preparação de cenas animadas.
- **Live Set:** modificar cenário, objeto ou câmera de forma interativa enquanto se vê a cena; promessa de controle contínuo, sem números oficiais de FPS/latência comercial.
- **On Cue:** sincronizar ações de vídeo com um sinal temporal desenhado ou extraído de áudio/vídeo.
- **Sound Bender:** separar/criar/variar som e efeitos com controle interativo sincronizado a vídeo.
- **Sight Span:** gerar ambiente virtual a partir de imagem para explorar novos ângulos, potencialmente útil à pré-produção.

**Disponibilidade:** Adobe informa que apenas alguns protótipos de outras áreas estão disponíveis para ensaio no Labs Playground. Não há SDK, API, preço, limites de duração ou licença comercial dos projetos audiovisuais listados. Não são concorrentes compráveis de Kling, Gemini Omni, Runway ou ElevenLabs no dia 11. Nenhuma comparação objetiva de qualidade é possível. O encontro de Sneaks da agenda ocorre mais tarde no dia 11 (17:30 EST), mas o artigo oficial já foi publicado.

Fontes: https://blog.adobe.com/en/publish/2026/11/11/adobe-max-sneaks ; https://www.theverge.com/tech/1031505/adobe-max-2026-sneaks-experiments-ai

## 3. Ciclo de vida e comparação EUA / China / outros

| Sistema e origem | Status verificado em 11/11 | Recomendação operacional |
|---|---|---|
| Avid Ready to Play (EUA, modelos Google) | **ATIVO, acesso por licença Ultimate/Enterprise + billing Google** | Avaliar para produtoras já usuárias de Avid; não API genérica |
| Adobe Scene Sweep, Screen Test, Live Set, On Cue, Sound Bender, Sight Span (EUA) | **PESQUISA/PRÉVIA; sem produto comercial confirmado** | Monitorar, não integrar |
| Adobe Creative Agent / Firefly Graph (EUA) | **BETA** | Diferente dos Sneaks |
| Adobe Generative Remove (EUA) | **BETA** | Edição de vídeo disponível para elegíveis |
| Beeble SwitchX 3.0 (EUA) | **ATIVO no Cloud; API v3 específica não comprovada** | Alteração de cenários preservando ator |
| Runway Aleph 2.0 (EUA) | **ATIVO** | Edição localizada de vídeo |
| Gemini Omni 1.2 Flash (Google, EUA) | **ATIVO GA/API** | Até 15s por geração, áudio; ~US$0,07/s 720p |
| Google Veo 3.1 GA (EUA) | **ATIVO; aposentadoria prevista não antes de 04/05/2027 conforme consulta anterior** | Confirmar endpoint e contrato antes de integrar |
| Kling 4.0 / Flash (Kuaishou, China) | **ATIVOS via Runway API** | Geração/edição, 30s vs 20s no Flash, custos variam |
| Seedance 2.5 (ByteDance, China) | **ATIVO via parceiros/API** | Refs multimodais e continuidade |
| Seedance 1.5 Pro na ElevenLabs (China) | **ENCERRAMENTO ANUNCIADO PARA 11/11** | Não iniciar integrações; migração para 2.0. Desligamento ao vivo não testado |
| AI Studios 5.0 / DeepBrain (Coreia do Sul) | **ATIVO SaaS** | Produção com avatar/dublagem/edição por créditos |
| ElevenLabs Video-to-Music (EUA/Europa) | **ATIVO API** | Música e SFX sincronizados; diferente do Sound Bender pesquisado |
| OpenAI Sora 2 API (EUA) | **DESCONTINUADA em 24/09/2026** | Não oferecer como serviço disponível |

Fontes de ciclo de vida: https://elevenlabs.io/docs/changelog ; https://dev.runwayml.com/learn/kling-4-0-api ; https://cloud.google.com/blog/products/ai-machine-learning/gemini-omni-1-2-flash-generally-available ; https://runway.com/news/introducing-aleph-2-and-edit-studio

**Não detectada nesta rodada** redução de preço relevante para modelos chineses ou lançamento de novo gerador fundacional com pesos abertos. Lyra 2.0 da NVIDIA apareceu em reportagens de 09/11, mas os pesos/código eram públicos desde abril, e GUI e treino desde julho de 2026, conforme repositório oficial — **não é lançamento desta semana**: https://github.com/nv-tlabs/lyra

## 4. Implicações para Marketing Hub

Avid fornece referência de arquitetura já operável: ingestão de clipes, descrição semântica, seleção de tomadas, revisão por prompt, retenção do original e controle de custos por Google Cloud. Os Sneaks da Adobe apontam uma próxima etapa: controle temporal e manutenção automática de personagem/cena/efeito sonoro, mas **não** há componente pronto para implementar. Métricas prioritárias: tempo por vídeo aprovado, taxa de refação, custo completo de API e preservação visual/sonora em cortes.

## 5. Cards e integridade

- Card Avid Ready to Play (operacional): `pesquisas/video/cards/2026-11-11-avid-ready-to-play-media-composer.json`
- Card Adobe Sneaks (monitoramento, **não API atual**): `pesquisas/video/cards/2026-11-11-adobe-sneaks-controle-audiovisual-pesquisa.json`
- Fonte Avid: `pesquisas/video/cards/fontes/2026-11-11-avid-ready-to-play-google-ai.md`; SHA-256 `0178ea9e3825ef11fc2581ee016919124dcd8999e3df824bf74ff2b68ba46a60`
- Fonte Adobe: `pesquisas/video/cards/fontes/2026-11-11-adobe-max-sneaks-video-audio.md`; SHA-256 `1fe753e83f56e4ffd1d6c6f4eb681523a345a36c30ba115be5adfa1e6ebb6201`

Foram mantidos os cards existentes; nenhum card foi criado para Lyra por não constituir lançamento recente. Nenhum benchmark ou aquisição de produto foi executado.
