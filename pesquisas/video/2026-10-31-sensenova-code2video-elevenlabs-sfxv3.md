# Radar de IA para Video — 31/10/2026

**Resultado:** duas descobertas relevantes nao registradas na rodada anterior: SenseNova Code2Video (pesos abertos) e ElevenLabs SFX v3 (melhora audiovisual). A primeira e o alerta principal. Nao houve lancamento novo confirmado especificamente para 31/10. Pesquisa de fontes ate 31/10/2026.

## 1. SenseNova 6.8 Code2Video — China
**Status: ATIVO com pesos publicos e autoimplantacao experimental; API comercial gerenciada NAO CONFIRMADA.** Repositorios criados no periodo 24-28/10, anuncio publico relatado em 28/10. Fonte primaria no GitHub e Hugging Face.

- Cerca de 27B parametros, derivado de Qwen3.8-27B, com contexto de 512K documentado como alvo de deploy.
- Nao gera diretamente pixels de um clipe cinematografico. O agente cria e renderiza codigo que define quadros ao longo do tempo, avalia os resultados e corrige a cena em novas rodadas.
- Usos: motion graphics, tipografia, graficos e formulas, explicadores, cenas 2D/3D estilizadas, simulacoes e audio procedural. Promete edicoes pontuais sem alterar outras partes do video.
- Pesos marcados Apache-2.0 no Hugging Face, mas a licenca de scripts/dependencias/recursos de terceiros deve ser examinada separadamente. Sem API SaaS e sem tabela de preco. Os exemplos de deploy usam paralelismo de 4 GPUs.
- Os proprios autores dizem que **nao produz filmagem fotorrealista**, que as producoes completas podem consumir horas e que ainda nao publicaram benchmarks sistematicos. Nao afirmar superioridade geral sobre Kling ou Veo.

Fontes: https://github.com/OpenSenseNova/SenseNova-6.8-Code2Video ; https://huggingface.co/sensenova/SenseNova-6.8-Code2Video ; https://neuralbrew.ai/en/articles/2026/10/sensenova-6-8-code2video-sensetime-video-through-code-open-source/

## 2. ElevenLabs SFX v3 — EUA
**Status: LIMITADO por conta/regiao; interface ElevenCreative e ElevenAPI; modelo proprietario.** Lancado em 29/10, confirmado em blog de 29/10 e documentacao atual.

- Segue melhor pedidos que ordenam varios eventos sonoros e promete resultados mais naturais, com melhor tratamento de sons musicais e vocais. Isso e afirmacao do fabricante, sem comparativo independente publicado.
- API via `model_id=eleven_text_to_sound_v3`. Geracoes ate 30 segundos; documentacao indica 40 creditos por segundo com duracao especificada. Acesso restrito: conta sem habilitacao pode receber `Unknown model`.
- O parametro `separate_tracks` de faixas isoladas pertence ao **Video to Music / Instant Soundtracks**, anunciado em 15/10, e nao ao endpoint SFX v3. Instant Soundtracks analisa video e gera musica mais efeitos com alinhamento temporal.
- Som para publicidade, curtas e explicadores. Licenca comercial conforme conta/plano pago e termos.

Fontes: https://elevenlabs.io/blog/sound-effects-v3 ; https://elevenlabs.io/docs/overview/capabilities/sound-effects ; https://elevenlabs.io/blog/introducing-instant-soundtracks ; https://elevenlabs.io/pricing

## 3. Status e comparacao de outras ofertas
| Sistema | Mercado | Status em 31/10 | Observacao |
|---|---|---|---|
| SenseNova Code2Video | China | ATIVO experimental | Pesos abertos; produz video por codigo, nao fotorealismo |
| ElevenLabs SFX v3 | EUA | LIMITADO | API/interface por conta e regiao |
| ElevenLabs Instant Soundtracks | EUA | ATIVO | Disponivel desde 15/10; Video to Music API |
| Kling 4.0 via Runway | China/EUA | API ATIVA | Videos ate 30s, audio, 720p/1080p |
| Wan 3.0 | China | API ATIVA via Runway | Geracao convencional por inferencia |
| Runway Gen-4.5 | EUA | API ATIVA | Geracao convencional por inferencia |
| Google Veo 3.1 GA | EUA | ATIVO em Cloud e parceiros | Preview Gemini API encerrado em 22/10 |
| Vidu Q4 Preview | China | PREVIEW ATIVO | Lancado em 07/10; parceria WaveSpeed de 30/10 nao e nova versao |
| Liquid AI LFM3 | EUA | PESOS DISPONIVEIS | Familia multimodal 2B desde 27/10; nao um gerador comercial de video |
| Sora 2 (OpenAI) | EUA | DESCONTINUADO | App/web encerrados 26/04; API encerrada 24/09 |

Precos Kling 4.0: a pagina oficial de tabela Runway continua mostrando promocao 20% ate 30/11: US$0.1256/s em 720p e US$0.188/s em 1080p. O artigo de tutorial mostra precos regulares (US$0.157 e US$0.235); isso nao prova termino da promocao.

Fontes: https://dev.runway.com/pricing ; https://dev.runway.com/learn/kling-4-0-api ; https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation ; https://ai.google.dev/gemini-api/docs/changelog ; https://www.globenewswire.com/news-release/2026/10/07/3376813/0/en/shengshu-technology-launches-vidu-q4-preview-a-next-generation-ai-video-model-built-for-lifelike-performances.html ; https://www.liquid.ai/blog/lfm3

## 4. Marketing Hub
Piloto prioritario: criar um video explicativo animado de oferta e derivar 10 versoes alterando preco, titulo, paleta e timing. Comparar trabalho total, falhas de render, qualidade visual, custo GPU e manutencao frente a refazer os clipes em Kling 4.0/Wan 3.0. Executar o codigo gerado apenas em sandbox com limite de privilegios, rede e disco.

Piloto secundario: comparar SFX v3 contra v2 em cinco cenas com 3 eventos sonoros sequenciais, desde que a conta tenha acesso. Avaliar obediencia temporal, qualidade auditiva, custo real e direitos de uso.

Um card JSON justificado para SenseNova, com fonte Markdown revisada e hash SHA-256 exato; SFX v3 registrado apenas no relatorio para evitar redundancia. Nenhum acesso pago ou execucao de geracao foi efetuado.
