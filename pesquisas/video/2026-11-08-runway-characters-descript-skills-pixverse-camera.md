# Radar IA para Vídeo — 08/11/2026

**Janela principal:** 05 a 08/11/2026, com fontes oficiais rechecadas em 08/11. O filtro considera novidades importantes ainda não cobertas em 07/11, distinguindo publicação de pesquisa, lançamento e disponibilidade comercial. **Não foi localizado novo lançamento importante anunciado especificamente em 08/11**; os acontecimentos relevantes abaixo são de 05 e 06/11.

## 1. Runway Characters — inferência de avatar em tempo real com metade das H100

**Status: ATIVO**, web e API; pesos **proprietários**. Publicação técnica em **05/11/2026**, artigo no arXiv submetido em 03/11. A Runway documentou avanços de inferência no Characters (aproximadamente 57B parâmetros, 704×1088, 24 quadros/s), com redução da infraestrutura por sessão de **8 para 4 GPUs H100**. Na mesma configuração de 4 H100, o passo foi reduzido de 240,9 para 159,3 ms (e a 147,4 ms com kernel fundido), permitindo manter quatro quadros em 166,7 ms. A manutenção do cache caiu de 69,7 para 6,2 ms por passo. Mediana de uma resposta: 2,13 s, sendo 1,42 s no agente de voz e 0,49 s no vídeo. Medições internas, sem auditoria independente.

**Natureza do avanço:** engenharia de inferência já implantada, descrita publicamente em 05/11; não é um novo modelo, abertura de pesos, novo preço ou novo endpoint. Dados internos sobre qualidade e lip-sync devem ser tratados com cautela.

**Disponibilidade comercial:** ajuda oficial confirma demonstração web de até 2 min e API customizada (tabela menciona até 5 min), com 2 créditos a cada 6 s (US$ 0,20/min na página consultada), sujeitos a regras e canal de contratação. Português é suportado, mas a documentação afirma que os testes focaram inglês. A queda de 50% em GPUs necessárias **não implica 50% de redução da cobrança comercial**.

**Comparação:** Atmee V2 (LIMITADO por plano/convite), Meta Muse Avatar (LIMITADO a implantação nos EUA), Google Live Avatar (LIMITADO a Enterprise) e Kandinsky S2V (ATIVO para execução com pesos disponíveis, mas pré-renderizado e não equivalente a conversa em tempo real). Testar no Marketing Hub a viabilidade de atendente ou apresentador interativo, mensurando latência, qualidade em PT-BR, custo por minuto, consentimento e resultado comercial.

Fontes:
- https://runway.com/news/research/real-time-video-at-batch-size-one
- https://arxiv.org/abs/2611.05642
- https://help.runwayml.com/hc/en-us/articles/49557780326163-Runway-Characters

## 2. Descript Underlord — skills reutilizáveis, pacotes de marca e busca visual

**Status: ATIVO no editor.** Changelog oficial de **06/11/2026**, que resume atualizações de outubro/começo de novembro (não presume-se lançamento de todas elas no dia 06).

**Underlord Skills:** salva um procedimento de edição para reutilização, podendo chamar skills via comando de barra no chat. **Brand Layout Packs:** gera um pacote de layouts editáveis a partir de manual de marca, logo e cores; recurso de planos **pagos** que consome créditos. **Busca semântica de cenas:** localizar um trecho pela imagem e não apenas pelo título, inclusive via MCP e API; **restrita a drives Enterprise**. O Descript também documenta upload de arquivos para seus projetos a partir do ChatGPT.

**Transição com prazo:** templates antigos de prompts do Underlord deixam de funcionar **30/11/2026**; templates não se convertem automaticamente, exigindo salvamento manual como skills. Não é encerramento do produto. Não foi confirmada API específica para cadastrar skills por código nem preço unitário de Brand Layout Packs.

**Comparação:** Descript é uma camada de edição e montagem, não gerador fundacional de pixels como Kling ou Gemini Omni; se compara melhor com Genspark Media Studio (ATIVO SaaS), Runway Workflows (ATIVO) e Synthesia Studio (ATIVO, integração MCP beta) em padronização e orquestração. No Marketing Hub, preparar pacote visual da marca e criar variações publicitárias com a mesma skill, avaliando revisão humana, custos e consistência.

Fonte: https://feedback.descript.com/changelog/release-roundupnovember-6-2026

## 3. PixVerse Draw to Fly — caminho de câmera guiado por traçado

**Status: ATIVO como miniapp web; API específica, preço e licença ainda não confirmados.** Post de **06/11/2026** mostra a câmera seguindo uma linha desenhada sobre imagem, criando um deslocamento no estilo FPV. A página oficial solicita JPG/PNG/WebP de até 10 MB e exibe **15 s** como duração configurada. Não foi executado um teste autenticado nem comprovada precisão de câmera.

Relevância para anúncios, cenas de arquitetura, clipes, vídeos explicativos e curtas, mas não foi criado card por falta de documentação pública da API e preço específicos.

Fontes: https://app.pixverse.ai/mini-apps/draw-to-fly ; https://teapawt.org/pixverse

## Comparação operacional e ciclo de vida — 08/11

| Sistema / origem | Status | Condições |
|---|---|---|
| Runway Characters / EUA | ATIVO web/API | Personagem conversacional, pesos proprietários; infraestrutura otimizada |
| Descript Underlord / EUA | ATIVO | Layouts pagos; busca visual Enterprise; templates antigos encerram 30/11 |
| PixVerse Draw to Fly / Singapura | ATIVO web | API específica e preço não verificados |
| Kling 4.0 Flash / China | ATIVO via Runway API | 720p 3–20s, áudio; US$0,094/s sem vídeo de entrada, divulgado 05/11 |
| Gemini Omni 1.2 Flash / EUA | ATIVO GA e Runway API | 3–15s, edição e referências, 360p a 4K; lançado 05/11 |
| ElevenLabs SFX v3 / EUA | ATIVO | Disponível em todos workspaces desde 02/11; SFX v2 encerrará 01/03/2027 |
| Seedance 1.5 Pro / provedor ElevenLabs | ENCERRAMENTO ANUNCIADO | Parará em 11/11/2026; não iniciar fluxos novos |
| Sora 2 / EUA | DESCONTINUADO | API encerrada 24/09/2026 |

Referências comparativas: https://docs.dev.runwayml.com/api-details/api_changelog/ ; https://elevenlabs.io/docs/changelog

## Decisões para o Marketing Hub

Dois cards JSON foram preparados, cada um vinculado a uma fonte Markdown revisada por SHA-256: **Runway Characters** (economia de hardware na experiência audiovisual ao vivo) e **Descript Skills / Brand Layout Packs** (edição repetível, consistência visual e migração de templates). PixVerse fica somente neste relatório até confirmação de API, preço e licença. Não repetir notificações já publicadas sobre Kling 4.0 Flash e Gemini Omni 1.2. Não houve teste pago, autenticação ou benchmark independente.
