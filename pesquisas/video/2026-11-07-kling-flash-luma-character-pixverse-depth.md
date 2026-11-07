# Radar IA para Vídeo — 07/11/2026

**Escopo:** pesquisa até 07/11/2026, comparando EUA, China e sistemas de outros mercados; novidades ainda não descritas nos relatórios até 06/11. Não foi confirmada uma novidade com data de anúncio 07/11, mas três mudanças dos dias 04 e 05 de novembro merecem registro, principalmente a abertura do Kling 4.0 Flash.

## 1. Kling 4.0 Flash / Kuaishou, China — abertura de acesso (04/11; API Runway 05/11)
**Status: ATIVO**; pesos proprietários. Antes restrito ao plano Ultra Yearly (desde 28/09), passou a acesso amplo na interface e API, conforme anúncio descrito por fontes especializadas. Runway Dev confirma em seu changelog oficial de 05/11 a integração `kling_4_flash`.

- Texto, imagem e vídeo para vídeo; vídeo-para-vídeo de referência/edição; áudio nativo.
- **720p**, **3–20s**, até 10 imagens e 5 vídeos de referência combinando no máximo 20s; sem confirmação de suporte a middle keyframes, 1080p ou 4K na variante Flash.
- **Runway:** US$0,094/s sem vídeo de entrada (9,4 créditos/s). Vídeo de 10s: US$0,94; 20s: US$1,88, antes de refações/tributos. Com entrada de vídeo: 6,3 créditos/s × (saída + max(entrada,saída)), e não US$0,063/s puro.
- **Comparação mesmo canal:** Kling 4.0 completo 720p em promoção = US$0,1256/s até 30/11; Flash ~25% menos. Completo chega a 30s e 1080p; preço de lista completo de US$0,157/s, Flash ~40% menos, sem inferir qualidade igual.
- **Marketing Hub:** testar produção de muitas variantes de 5–15s, medindo custo por vídeo aprovado, lip-sync em PT-BR, estabilidade visual, tempo de geração e licenças comerciais.

Fontes: https://docs.dev.runwayml.com/api-details/api_changelog/ ; https://docs.dev.runwayml.com/guides/pricing/ ; https://www.llms.blog/posts/kling-4-0-flash-open-to-all-api-price-limits ; https://help.artlist.io/hc/en-us/articles/40075326862237-Kling-4-0

## 2. Luma Character Sheet / EUA — consistência entre ângulos e cenas
**Status: ATIVO** via workflow Luma MCP no Claude e Claude Code, sem REST dedicado ou preço por execução confirmados; anúncio datado 04/11 segundo cobertura de terceiros. A documentação oficial apresenta `/luma:character-sheet` e `get_workflow(name="character-sheet")`, criando uma referência-âncora e novas vistas, com IDs reaproveitáveis em workflows de curtas. **Não é novo modelo fundacional de vídeo.** Acesso com créditos Luma; não há benchmark independente de manutenção da identidade entre tomadas. Comparar com fluxos de referência no Kling, Seedance e Ray3.2, sem supor compatibilidade direta.

Fontes: https://luma.ai/mcp/character-sheet ; https://www.adgraam.com/blog/luma-character-sheet-features-access

## 3. PixVerse Depth Map Control / Singapura — transferência de movimento
**Status: ATIVO** como mini-app web público; **acesso API específico e preço NÃO CONFIRMADOS**. O anúncio de 04/11 está registrado em fonte secundária; o site oficial já apresenta funcionalidade. Recebe um vídeo de referência MP4/MOV de no máximo 100 MB e 60 segundos, e pretende transferir estrutura de profundidade/movimento a novo assunto ou cenário. **O limite de upload não é duração máxima do vídeo de saída**. Não há benchmarks comparativos nem confirmação de licença comercial específica para essa mini-app. Potencial para reutilizar movimentos de câmera, personagens e objetos em novos criativos; fazer teste somente com material autorizado.

Fontes: https://app.pixverse.ai/mini-apps/depth-map-control ; https://www.adgraam.com/blog/pixverse-depth-map-control-features-access

## Quadro comparativo de ciclo de vida — verificação em 07/11
| Sistema / origem | Status | Condição |
|---|---|---|
| Kling 4.0 Flash / China | **ATIVO** | API Runway confirmada em 05/11; 720p 3–20s; proprietário |
| Kling 4.0 completo / China | **ATIVO** | API Runway; 720p/1080p 3–30s; desconto até 30/11 |
| Gemini Omni 1.2 Flash / EUA | **ATIVO** | GA Google Cloud; também Runway Dev desde 05/11; até 15s |
| Wan 3.0 / China | **ATIVO** | Runway Dev, vídeo áudio até 30s |
| Seedance 2.5 / China | **ATIVO** | Runway Dev, referências, edição, até 30s |
| Luma Character Sheet / EUA | **ATIVO** | Workflow MCP, não endpoint REST confirmado |
| Luma Ray3.2 / EUA | **ATIVO** | Modelo de vídeo distinto do Character Sheet |
| PixVerse Depth Map Control / Singapura | **ATIVO web** | Mini-app, API e preço não verificados |
| Synthesia / Reino Unido | **ATIVO** | Plataforma de avatares; integração MCP beta |
| Kandinsky S2V / Rússia | **ATIVO** | Pesos próprios, execução local; GPU robusta |
| ElevenLabs Ads Engine / EUA | **LIMITADO alpha** | Serviço de QA, sem API pública |
| Atmee V2 / EUA | **LIMITADO** | Acesso por plano/convite |
| Google Veo 3.1 GA / EUA | **ATIVO, transição futura** | Aposentadoria não antes 04/05/2027 segundo Google Cloud |
| ElevenLabs Seedance 1.5 Pro | **ENCERRAMENTO ANUNCIADO** | Desligamento em 11/11/2026 no provedor ElevenLabs |
| OpenAI Sora 2 / EUA | **DESCONTINUADO** | App/web encerrados 26/04, API 24/09/2026 |

## Julgamento
**Alertar:** Kling 4.0 Flash tornou uma opção 720p com áudio e API mais barata e acessível para produção em volume; informação ficou fora do radar de 06/11. Luma e PixVerse são avanços úteis em continuidade e transferência de movimento, com API/preço ainda parcialmente indefinidos. Não confundir novas rotas de acesso com aumento demonstrado de qualidade visual.

## Notas de auditoria
Sem login nem chamadas pagas; sem testes objetivos de qualidade; preços são anúncios públicos, não faturas. Para o Marketing Hub, criar **2 cards**: Flash e Character Sheet; PixVerse fica apenas no relatório até ter API, preço e parâmetros de saída suficientemente claros. Fontes dos cards têm hash SHA-256 do Markdown UTF-8 exato.
