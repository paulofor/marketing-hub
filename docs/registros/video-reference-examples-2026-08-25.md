# Evidência local — três referências de vídeo do Estúdio

Data da inspeção: 2026-08-25.

## Escopo e limite da evidência

Os arquivos dos registros `#1`, `#2` e `#3` do Marketing Hub foram baixados para uma pasta
temporária, inspecionados com `ffprobe`/`ffmpeg` e avaliados visualmente por contact sheets. O
arquivo binário não integra o repositório. Acabamento alto e longevidade percebida não foram
tratados como prova de venda, pois os registros não possuem atribuição comercial verificável.

| Registro | SHA-256 | Duração | Formato | Viradas visuais | Áudio |
|---|---|---:|---|---:|---|
| #1 Tik Tok Flavio | `9a672675e73d536bd2ff5f31da9ae6346ff7699fc110dfbe5a2c1dbcbd86e050` | 131,402 s | 576×1024, H.264, 30 fps | 39 | AAC, -11,8 LUFS, pico +0,1 dBFS |
| #2 Rio Antigo | `9e85a4816f882b2c06e0330daf953b52fe8d39c4be33dea5ab39a7af4ae56edc` | 162,006 s | 576×1024, H.264, 30 fps | 35 | AAC, -17,8 LUFS, pico -0,3 dBFS |
| #3 Madonna | `30b56d019f654a1283e5a026349673c7a9949c64ff16d711e3ad4ea3637b1dd2` | 63,745 s | 576×1024, H.264, 30 fps | 10 | AAC, -14,1 LUFS, pico -0,6 dBFS |

## Mecanismos reutilizáveis

### #1 — tensão, escala e revelação

- abre com imagens de alta escala e conflito visual, alternando arquivo, apresentadores e cortes
  curtos para renovar atenção;
- mantém legenda persistente e conduz a narrativa por tensão, explicação e revelação;
- condição original segura: substituir pessoas públicas, marcas, emissoras, uniformes e material
  militar por um conflito ficcional diretamente ligado à dor e ao mecanismo do produto;
- melhor uso provável: campanha de descoberta ou conteúdo orgânico explicativo. A referência não
  comprova que o formato, sozinho, produz checkout ou venda.

### #2 — apresentador dentro de uma reconstrução

- alterna um apresentador coerente com planos gerais e detalhes de uma reconstrução histórica;
- usa passagem de tempo, mudança de luz e progressão da obra como recompensa visual;
- exige bíblia rígida de personagem, figurino, ambiente, objetos, clima, lente e continuidade;
- melhor uso provável: módulo premium de produto, advertorial educativo ou série orgânica. Em
  campanha, deve ganhar uma versão curta com promessa e CTA ligados à oferta.

### #3 — performance musical cinematográfica

- sustenta poucos blocos longos por performance, coreografia, alternância de escala e luz de palco;
- depende de sincronia corporal/facial, direção de arte e legenda temporalmente alinhada;
- contém risco elevado de semelhança com artista, voz, música, letra, figurino e gravação
  reconhecíveis; o sistema deve aprender a mecânica e bloquear qualquer cópia;
- melhor uso provável: peça de marca ou conteúdo orgânico autoral. Campanha exige música, pessoa e
  performance próprias ou licenciadas e revisão humana antes de qualquer render pago.

## Alternativas avaliadas

1. Criar um agente novo para cada estética: amplia governança e fragmenta a direção criativa sem
   criar uma responsabilidade cognitiva nova.
2. Transformar os estilos em presets manuais do Estúdio: tem baixo custo, mas não converte novas
   referências em aprendizado auditável nem preserva evidência.
3. Manter Apolo como diretor e ampliar seu pipeline com inspeção determinística, leitura multimodal,
   receita importável e providers isolados: reaproveita a responsabilidade existente, protege
   direitos e permite evolução técnica sem acoplar estilo a agente.

Decisão: alternativa 3. Apolo possui capacidade de direção e planejamento; o que faltava era uma
etapa executável de engenharia reversa e capacidades técnicas condicionais no Estúdio. Um novo
agente só deve ser proposto se surgir uma responsabilidade cognitiva independente, não por causa de
um novo estilo visual.

## Condições de produção criadas

- `reference-analysis-v1` consome exclusivamente a fila `pending` do backend;
- `ffprobe`/`ffmpeg` geram duração, codecs, resolução, loudness, viradas e 24 frames-chave;
- o prompt e o schema versionados de Apolo separam observação, inferência, aplicação comercial,
  riscos e uma receita completa de até 48 cenas;
- a receita pode preencher o Estúdio sem substituir produto, oferta ou CTA já definidos pelo
  operador;
- Act-Two fica disponível em homologação para performance autorizada, com bloqueio por consentimento,
  direitos da performance, duração e revisão; não existe ativação automática de provider pago;
- request, response, tokens, custo conhecido, artefatos, decisão, erro e correlação ficam persistidos;
- falha automática habilita contingência manual; execução ativa ou concluída não pode ser
  sobrescrita por ela.

## Critério comercial de continuação

Continuar para um render de teste somente quando produto, oferta, CTA, direitos, provider, preço e
revisão estiverem aprovados. Ajustar a receita quando o gate de continuidade, áudio, legenda ou
clareza comercial falhar. Parar antes de gasto ou publicação quando houver semelhança protegida,
custo desconhecido, ausência de consentimento ou falta de atribuição mensurável.

## Envelope financeiro da homologação

- três leituras multimodais: teto conjunto de US$ 0,75 e reserva de US$ 0,25 por execução;
- cálculo conservador: tarifa padrão integral do GPT-5.6, sem descontar cache ou Flex;
- primeiro render original: ciclo separado de até US$ 2,00, bloqueado por Plutus e limitado a dez segundos;
- teto total: US$ 2,75, sem campanha, publicação, contato ou registro de venda.
