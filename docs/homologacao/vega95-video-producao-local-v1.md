# Vega #95 — produção local com o harness de Apolo

Registro de 02/10/2026. Escopo: produzir o vídeo candidato do projeto #8, perfil #63,
produto Vega #4, marca Método MUSA, sem ativar campanha ou consumir APIs pagas.

## Decisão e fidelidade

Foram comparados: novas cenas pagas (maior liberdade, orçamento ainda ausente),
reutilização de vídeo já finalizado (econômica, mas preservaria mensagem antiga) e
montagem local com material auditado, capturas reais e voz offline. A terceira opção
preserva a oferta e produz uma candidata sem novo consumo de providers.

O harness utilizado é a missão e o planejador versionados de Apolo, com execução
local. Isso não representa execução do worker produtivo nem aprovação independente
de Apolo, Psique ou Têmis. A candidata deve permanecer pendente de revisão.

Fonte: mídia #2810, montagem de origem do vídeo aprovado #42 do experimento #92.
Somente o intervalo 10,25–14,85 s preserva a personagem com camisa clara e calça
escura; os trechos de outra roupa são descartados. A personagem é ilustrativa,
sem depoimento ou prova de satisfação. Não inventar filmagem de bolsa ou saída.

A versão pública comprovada por capturas deve ser
`musa-pde-entry-v12-primeiro-ajuste-aplicavel`. Sua microação é categorial:
“Use calça e camisa ou blusa em trabalho ou reunião.” A montagem identifica o
ajuste da personagem como ilustração; não atribui dobra de manga, gola ou outra
instrução específica ao aplicativo. Preservar esta limitação para revisão.

## Matriz definida antes da validação

| Critério | Comprovação necessária |
|---|---|
| Identidade | Vega #4, experimento #95, projeto #8, perfil #63; nenhuma mistura com Capella/Mira ou orçamento histórico. |
| Fontes e versão | Hashes dos bytes da filmagem, capturas, modelo e voz; rejeitar hash ou versão divergente antes da síntese. |
| Caminho feliz | Sete blocos contíguos, 45 s, 1080×1920, H.264/AAC, locução feminina em português brasileiro. |
| Legendas | Mesmas palavras da fala, no máximo duas linhas, tempos medidos dos trechos de voz; impedir corte, aceleração e sobreposição. |
| Prova e oferta | Quatro escolhas e resultado reais; e-mail apenas para salvar; R$ 67, pagamento único, 90 dias, sem renovação. |
| Continuidade | Mesma personagem, roupa e ambiente; aplicação identificada como ilustrativa. |
| Falhas | Fonte alterada, versão divergente, voz maior que bloco, legenda adulterada e salto temporal devem bloquear. |
| Integração local | Importação existente também acessível a PDE; produto correto no link; bloqueio de edição e revisão preservados. |
| Reprodução | MP4 reproduzido com áudio e sem áudio em Chromium desktop e emulações iPhone 15 Pro/Pixel 7; duração e controles verificados. |
| Isolamento | Capturas com flags de QA, sem e-mail, checkout ou eventos comerciais; zero chamadas pagas. |
| Persistência | Importar pela tela, preservar origem aprovada, hash e referência versionada; candidata pendente, sem aprovação ou mídia automáticas. |
| Economia | Consumo incremental de APIs pago zero; custo total computacional/histórico desconhecido, nunca presumido zero. |

## Lacuna concreta do harness

A tela ocultava a importação de MP4 pronto para experimentos PDE, embora o backend
já possuísse o contrato governado. Isso obrigava o operador a procurar uma geração
nova ou contornar a tela. A correção expõe o contrato existente, mantendo fonte
aprovada, áudio, referência versionada e revisão. A consulta de vídeos PDE e o link
de produção também passam a usar o produto do experimento, em vez de fixar Vega
para todos os produtos; ausência de produto não autoriza consultar outra identidade.

A montagem acrescenta validação de fontes, identidade textual e duração física.
Essas verificações detectam uma candidata inválida antes da importação; não
substituem a avaliação perceptual independente ou comprovam vendas.

O histórico confirmou a fonte #42 pronta e aprovada e o upload #48 com vídeo #47
do próprio experimento. Na origem #92 o Instagram é ausente; no #95 é #1. O contrato
anterior recusava a reutilização visual sem adotar a origem inteira, acoplando
pixels à identidade de campanha. Alterar o Instagram do #95 destruiria sua
configuração; gerar novas cenas consumiria recursos sem necessidade. A correção
aceita fonte pronta e aprovada da mesma oferta completa e registra a linhagem,
sem copiar página, checkout, plano, campanha, métricas ou orçamento.

## Reprodução da produção

As fontes são baixadas dos contratos oficiais, fora do Git, com seus hashes no
briefing e manifesto. Não colocar vídeo, pesos de modelo ou dados pessoais no repo.

1. Capturar: `node scripts/marketing/capture-vega95-video-proof-v1.mjs <fontes>`.
2. Instalar em diretório temporário `kokoro-onnx==0.4.9` e `soundfile==0.13.1`.
3. Usar modelo e banco de vozes do release `model-files-v1.0`, com os hashes do
   briefing. Configurar `PYTHONPATH` para as dependências temporárias.
4. Compor: `node scripts/marketing/create-vega95-video-v1.mjs <fontes> <saida> <modelo.onnx> <vozes.bin>`.
5. Conferir manifesto, MP4, legendas e reprodução antes de importar pela tela.

Voz `pf_dora`, brasileira feminina, modelo Kokoro Apache 2.0 e runtime MIT:
[modelo](https://huggingface.co/hexgrad/Kokoro-82M),
[vozes](https://huggingface.co/hexgrad/Kokoro-82M/blob/main/VOICES.md),
[runtime](https://github.com/thewh1teagle/kokoro-onnx).

## Resultados locais

- MP4 H.264/AAC, 1080×1920, 45,000 s, sete blocos e onze legendas com as
  palavras da locução. Hash:
  `49e1c0fe73b0234227b795762b1578017f92ec34039248cdf0639d2676bd53e9`.
- Fontes, capturas, voz, tempos e limites estão no
  [manifesto de produção](../../scripts/marketing/vega95-video-v1/production-evidence.json).
- Primeiro render revelou títulos invadindo a moldura e enquadramento cortando o
  rosto. A composição agora reduz o título dentro de limites de leitura e recusa
  sobreposição; o recorte preserva o rosto. Nova inspeção visual confirmou o ajuste.
- O checklist é uma prévia do material HTML versionado, identificada assim na
  montagem; não é apresentado como captura de uma publicação pública comprovada.
- 821 testes do frontend passaram, além de typecheck e build. Os sete testes do
  contrato audiovisual cobrem palavras, tempos, duração, hashes, versão e QA.
- Os 34 testes do serviço de vídeo passaram, incluindo o multipart do controller
  com serviço real e dependências locais simuladas. Fontes incompatíveis foram
  recusadas antes do armazenamento. A suíte completa do backend terminou sem
  falhas: 3.789 testes, incluindo 23 ignorados já existentes; os testes novos não
  foram ignorados. Empacotamento e Spotless dos arquivos alterados passaram.
- A importação foi exercitada com backend simulado em desktop, iPhone 15 Pro e
  Pixel 7: MP4 real, metadados, fonte aprovada e um único envio pendente de revisão.
- O player nativo reproduziu os trechos conferidos com áudio e sem áudio nos três
  perfis; confirmou 45 s, resolução, decodificação de áudio, controles e término.
  O arquivo inteiro também foi decodificado pelo FFmpeg. Emulação Chromium não
  comprova Safari real; escuta e aprovação perceptual independentes seguem pendentes.
- Consumo incremental de APIs pagas: USD 0. O custo computacional e os custos
  históricos das fontes não foram medidos e permanecem desconhecidos.

A entrega de código deve passar por PR, merge e deploy antes de importar a candidata
pela tela publicada. O upload e o status de revisão serão conferidos no backend;
nenhum orçamento ou campanha será iniciado por esta produção.
