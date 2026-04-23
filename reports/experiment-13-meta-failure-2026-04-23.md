# Diagnóstico — Falha recorrente do experimento 13 na publicação Meta

Data da análise: 2026-04-23 (UTC)

## Resumo executivo

O experimento **13** falhou repetidamente na etapa de criação do criativo (`POST /adcreatives`) no Facebook Ads Worker.
A resposta da Meta retornou erro de parâmetro inválido para download da imagem:
- `code=100`
- `error_subcode=3858258`
- `error_user_title="A imagem não foi baixada"`
- `error_user_msg` pedindo validação de acessibilidade pública/`robots.txt`.

## Evidências dos logs (MCP + banco)

### Timestamp e endpoint da falha
- Falhas em `2026-04-23T04:12:46Z` e novamente em `2026-04-23T13:25:57Z` até `13:26:02Z`.
- Endpoint afetado: `POST https://graph.facebook.com/v23.0/act_939323521124952/adcreatives`.

### Trechos dos payloads rejeitados
- Tentativa 1 (`id=97`):
  - `object_story_spec.link_data.picture = .../2026/04/22/.../a163f0cfaef4-creative-de8a163b-34f7-46bb-9573-7bbd5f4.jpg`
- Tentativas 2-4 (`id=100/101/102`):
  - `object_story_spec.link_data.picture = .../2026/04/23/.../eaeb11a83f53-creative-b44016ca-5179-4505-9f4b-1c59f5a.jpg`

### Resposta da Meta registrada no worker
- `type=OAuthException`
- `code=100`
- `error_subcode=3858258`
- `message=Invalid parameter`
- `error_user_title=A imagem não foi baixada`
- `error_user_msg=Não foi possível baixar sua imagem (...) Verifique se a imagem pode ser acessada pela internet e se não está bloqueada por um arquivo robots.txt`

### Resultado do experimento
- Worker registrou `Unexpected error while processing experiment 13`.
- Worker marcou o experimento 13 como `FAILED` no backend.

## Comparativo solicitado: experimento 10 (publicado) vs experimento 13 (falhou)

### Experimento 10 (referência de sucesso)
- Publicação bem-sucedida em 2026-04-14.
- Fluxo completo com sucesso:
  - `campaigns` (200),
  - `adsets` (200),
  - `adcreatives` (3x 200),
  - `ads` (3x 200).

### Experimento 13 (falha recorrente)
- Mesma conta de anúncios e mesmo endpoint Graph `v23.0`.
- `campaigns` e `adsets` criados com sucesso (200).
- **Apenas `adcreatives` falha** (4x 400 com subcódigo 3858258).
- Falhou em duas janelas temporais diferentes e com duas URLs de imagem diferentes.

### O que isso elimina como causa principal
1. Não parece problema geral de token/permissão da conta (campaign/adset funcionam).
2. Não parece erro estrutural do payload base de criativo (mesmo formato já funcionou no experimento 10).
3. Não é uma única imagem corrompida, pois o erro ocorreu com **duas URLs distintas** no experimento 13.

## Verificações técnicas adicionais feitas nesta investigação

1. Testes `curl -I`/`HEAD` e `GET` (inclusive com user-agent `facebookexternalhit`) para URLs do experimento 10 e 13:
   - todas responderam `HTTP 200`, `content-type: image/jpeg`.
2. Comparação de características dos arquivos (amostra de URLs usadas):
   - dimensões equivalentes (`683x1024`),
   - tamanhos próximos (faixa ~111KB a ~127KB).
3. `/robots.txt` no domínio da mídia retorna `404` (sem política explícita publicada).

## Hipóteses adicionais de causa raiz (priorizadas)

1. **Intermitência/bloqueio no fetch da Meta para o host de imagem (R2/CDN/WAF/edge)**
   - Sintoma compatível: URL acessível para nós, mas não baixável para o crawler da Meta no instante da criação do criativo.
2. **Variabilidade de rota/PoP para bots da Meta**
   - Mesmo arquivo pode responder 200 para uma origem e falhar para outra (rede da Meta), gerando erro 3858258.
3. **Inconsistência de versão de criativo no momento da publicação**
   - A primeira falha usou URL de 2026-04-22 e, após ajuste de imagem, novas tentativas usaram URL de 2026-04-23; ainda assim falhou.
   - Isso sugere possível janela de inconsistência entre atualização de criativo e tentativa de publicação.
4. **Ausência de estratégia de fallback para `image_hash`**
   - Hoje o fluxo depende de `picture` por URL externa; quando a Meta não consegue baixar, o processo quebra.

## Comparação com documentação/contrato da integração Meta

1. O fluxo documentado do worker define criação de criativo via `POST /adcreatives` com `object_story_spec`.
2. A modelagem funcional do anúncio considera mídia principal por URL/ID e depende de acessibilidade válida do asset.
3. O código do worker já trata especificamente `error_subcode=3858258` como erro de download de imagem e executa retry (até 3 tentativas), confirmando que a falha é reconhecida como ingestão de asset pela Meta.

## Ação corretiva recomendada (para parar recorrência)

1. **Trocar o caminho principal para upload em `/adimages` + uso de `image_hash` em `/adcreatives`**.
2. Manter `picture` por URL apenas como fallback secundário (não o inverso).
3. Inserir pré-flight obrigatório antes de publicar:
   - `HEAD/GET`, tipo MIME, tamanho, dimensões,
   - validação de disponibilidade estável por alguns segundos (múltiplas leituras).
4. Implementar retentativa com troca de estratégia:
   - na 1ª falha 3858258 com URL, reenviar o criativo com `image_hash`.
5. Persistir no backend motivo estruturado de falha (`error_subcode`, `fbtrace_id`, URL da imagem) para observabilidade e reprocessamento seletivo.
