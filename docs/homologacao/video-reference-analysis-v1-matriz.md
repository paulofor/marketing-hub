# Matriz de homologacao — video-reference-analysis-v1

Evidência das referências inspecionadas: `docs/registros/video-reference-examples-2026-08-25.md`.

## Objetivo e segregacao

Comprovar localmente que uma referencia vira evidencia, aprendizado e receita importavel para um
projeto sem chamada paga de video, publicacao, contato ou evento comercial. Dados da rodada usam
tenant e IDs de teste; metricas humanas, gasto e vendas devem permanecer em zero.

| Bloco | Caminho feliz | Validacoes e falhas obrigatorias |
|---|---|---|
| Ingestao | upload cria referencia e tentativa `QUEUED` | arquivo vazio, extensao invalida, URL nao HTTP(S), HTTP de origem nao 2xx e tamanho acima do limite |
| Fila | `pending` entrega uma execucao e marca referencia `ANALYZING` | polling sobreposto, lease abandonado e callback UUID antigo |
| Midia | ffprobe mede streams; ffmpeg detecta cenas, loudness e cria 24 frames | arquivo invalido, duracao ausente e falha de ferramenta |
| IA | request Flex usa prompt/schema versionados e dois contact sheets | credencial ausente, HTTP externo falho, response sem output e JSON invalido |
| Contrato | saida possui sequencia, aprendizados, direitos e receita | menos de quatro blocos/cenas, direitos ausentes e sugestao injustificada de novo agente |
| Auditoria | backend persiste input/output, artefatos, request/response, modelo e tokens | custo desconhecido permanece nulo e falha preserva erro/artefatos disponiveis |
| Tela | resultado acompanha fila, mostra evidencia e oferece retry/importacao | loading explicito, falha visivel e nenhuma verdade recomputada no frontend |
| Projeto | importacao preenche receita sem trocar produto, oferta ou CTA | produto obrigatorio, duracao/categoria coerentes e render bloqueado antes de salvar |
| Apolo | storyboard aceita ate 48 beats e preserva cenas persistidas | texto no video, retrocesso narrativo, duplicacao, custo acima do teto e provider nao homologado |
| Direitos | somente mecanismos abstratos sao reutilizados | pessoa publica, marca, voz, musica, letra ou gravacao copiada bloqueiam producao |
| Observabilidade | logs correlacionam URL, executionId, request e response | nenhuma credencial, token ou PII desnecessaria em log |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | sem overflow, CTA tocavel, listas e receita legiveis |

## Criterio de encerramento

A primeira rodada completa passa se todos os itens forem aprovados. Como a implementacao nasceu da
fila travada, qualquer defeito encontrado reinicia a contagem: depois da ultima correcao sao exigidas
duas rodadas completas e consecutivas sem falhas. Nao fazem parte desta homologacao chamadas pagas,
publicacao, campanha, envio de contato ou registro de venda.
