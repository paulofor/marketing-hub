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
| IA | request Flex usa prompt/schema versionados, dois contact sheets e no maximo 4.000 tokens de saida | credencial ausente, HTTP externo falho, response sem output, usage ausente e JSON invalido |
| Contrato | saida possui sequencia, aprendizados, direitos e receita | menos de quatro blocos/cenas, direitos ausentes e sugestao injustificada de novo agente |
| Auditoria | backend persiste input/output, artefatos, request/response, modelo, tokens e custo conservador | nenhuma conclusao aceita custo nulo; cache/Flex nao reduzem o valor reservado |
| Tela | resultado acompanha fila, mostra evidencia e oferece retry/importacao | loading explicito, falha visivel e nenhuma verdade recomputada no frontend |
| Projeto | importacao preenche receita sem trocar produto, oferta ou CTA | produto obrigatorio, duracao/categoria coerentes e render bloqueado antes de salvar |
| Apolo | storyboard aceita ate 48 beats e preserva cenas persistidas | texto no video, retrocesso narrativo, duplicacao, custo acima do teto e provider nao homologado |
| Direitos | somente mecanismos abstratos sao reutilizados | pessoa publica, marca, voz, musica, letra ou gravacao copiada bloqueiam producao |
| Observabilidade | logs correlacionam URL, executionId, request e response | nenhuma credencial, token ou PII desnecessaria em log |
| Orcamento | as tres analises consomem no maximo o envelope de US$ 0,75, com reserva de US$ 0,25 por execucao | backend bloqueia antes da chamada quando custo conhecido mais reservas exceder o teto |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | sem overflow, CTA tocavel, listas e receita legiveis |

## Criterio de encerramento

A primeira rodada completa passa se todos os itens forem aprovados. Como a implementacao nasceu da
fila travada, qualquer defeito encontrado reinicia a contagem: depois da ultima correcao sao exigidas
duas rodadas completas e consecutivas sem falhas. Nao fazem parte desta homologacao chamadas pagas,
publicacao, campanha, envio de contato ou registro de venda.

O primeiro render QA usa um ciclo separado de US$ 2,00, aprovado por Plutus. O envelope total desta
homologacao e de US$ 2,75: US$ 0,75 para analise e US$ 2,00 para um unico render original de ate dez
segundos, sem publicacao ou campanha.
