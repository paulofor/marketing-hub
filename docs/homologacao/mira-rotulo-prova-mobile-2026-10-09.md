# Mira — rótulo e prova visual em celular — 09/10/2026

## Contexto e causa confirmada

Autorização da pendência #3287: US$15 acumulados na mesma preparação de Mira, incluindo consumo anterior, sem mídia ou vídeo pago. Ciclo #9, experimento #102; nenhum sucessor foi criado nesta retomada.

O parecer Psique #651 recebeu o PNG #567 de Íris #650 e solicitou o rótulo de prévia e melhor leitura a 393 pixels. A especificação continha `eyebrow: Prévia da aplicação`, mas o renderizador privado substituía esse texto por `APLICAÇÃO WEB PRIVADA`. A área fixa de 550 pixels de altura limitava o detalhe. O leitor de Psique anexava somente o original de 1080 pixels, sem uma redução real de celular. O código e o arquivo persistido confirmam as três condições; não são inferência de rejeição comercial.

Íris #652 produziu outra especificação válida, com o aviso de prévia também no corpo. O processo foi pausado pela UI, preservando a saída e o consumo antes de novas revisões. A tentativa original, fontes, PNGs e pareceres permanecem no histórico.

## Alternativas comparadas

| Alternativa | Benefício | Risco/esforço | Escolha |
|---|---|---|---|
| Pedir textos e recortes menores ao modelo | Mudança rápida | Mantém a substituição do rótulo e pode remover limites da fonte; gera retrabalho pago | Não resolve a causa compartilhada |
| Corrigir o template privado e anexar redução fiel na revisão | Preserva formato, fonte, contexto e hash; entrega legível e avaliação rastreável | Ajuste proporcional em dois executores; requer regressões e publicação | Escolhida |
| Criar outro template e outro fluxo de produção | Maior liberdade | Novo contrato e migração sem necessidade comprovada | Fora do ajuste necessário |

## Matriz local de aceite

| Critério | Verificação |
|---|---|
| Rótulo solicitado e identidade privada | PNG muda com o rótulo; identificação privada e aviso sintético continuam presentes |
| Prova ampliada e íntegra | Recorte proporcional dos mesmos pixels; sem reconstruir texto ou remover ressalvas |
| Caminho antes válido | Composição comercial e capturas não criativas preservadas |
| Outras identidades | Duas referências sintéticas independentes, sem IDs do produto em regras ou fixtures |
| Antes da chamada paga | Falta de prévia ou mudança do hash impede o comando |
| Integração da revisão | Comando recebe original e PNG real de 393×491; auditoria conserva origem e hash da redução |
| Observabilidade | Transformação WIDTH_393_BICUBIC_V1 e hashes na entrada e evidência da tarefa; auditoria usa o artefato original |
| Desktop e celular | Inspeção da peça real e das prévias com Chromium desktop, iPhone e Pixel |
| Empacotamento | Testes completos dos dois executores, Spotless, JAR e prompts versionados |
| Segregação | Fixtures e previews locais não são artefatos comerciais nem compras; nenhum provedor é chamado nos testes |

## Referência técnica

[OpenAI — limitações de visão](https://developers.openai.com/api/docs/guides/images-vision#limitations), consultada em 09/10/2026: textos pequenos e redimensionamento exigem cuidado na avaliação. A prévia enviada comprova os pixels reduzidos, sem tratar um parecer do modelo como teste com pessoas.

## Resultado

- Íris: suíte completa com 74 casos, 72 executados e dois replays opcionais dispensados;
  zero falhas. Replay adicional da especificação preservada sobre a fonte conferida:
  sete casos executados, zero falhas.
- Psique: suíte completa com 173 casos, 163 executados e dez cenários/replays opcionais
  dispensados; zero falhas. Dois replays locais da peça persistida e do template corrigido
  exercitam a redução e o comando, cinco casos por execução, sem provedor.
- Download HTTP local → conferência de hash → redução → argumentos do modelo → validação
  da auditoria conserva o original. Fixtures independentes e fluxo não criativo passaram.
- Spotless dos arquivos Java alterados, `bash -n`, ShellCheck e contrato do Dockerfile
  de Psique passaram. JARs contêm classes e prompts idênticos aos arquivos validados.
- Chromium desktop, iPhone 15 Pro e Pixel 7: ambas as prévias mostram 393 × 491 pixels,
  sem erro de página ou transbordamento. O template corrigido amplia o recorte e conserva
  instrução, referência e ressalvas. A redução não foi enviada como artefato comercial.
- Fonte aprovada SHA-256 `45ec50db5899063f59b2d788ee24d9f7f326e18bb3c9194dd32fe63184132fa7`;
  peça persistida #568 `b7c40c5386cd5bc36c6ab8630ad9c4b491418725ed4e306ee06fb00e76d9b3cf`;
  composição local do template corrigido
  `5ec9708e179d4a5e11700be35e79b57a6825db8cad7aecb1f90b697702e7cb1e`.
- Banco consultado às 01:23 UTC: US$11,0794024 conhecidos estimados, teto acumulado US$15,
  nenhuma tarefa ativa, pai pausado e filho aguardando o pai. Estimativa não é fatura.

Publicação e aceite dos pareceres serão vinculados no PR. Retomar pela UI somente após
comprovar os executores compatíveis. Reutilizar primeiro a peça #568, sem alterar seu callback;
se uma revisão funcional nova exigir correção, o fluxo existente produzirá a nova versão
com o template corrigido e o saldo conferido. Não há autorização de mídia ou vídeo pago.
