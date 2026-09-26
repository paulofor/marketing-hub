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
| Midia | ffprobe mede streams; ffmpeg detecta cenas, loudness, extrai MP3 mono e cria 24 frames | arquivo invalido, duracao ausente, video sem audio e falha de ferramenta |
| Transcricao | faixa falada usa prompt versionado, hash, request/response e custo por duracao | binario acima de 25 MB, audio sem fala, HTTP externo falho e video sem audio sem chamada externa |
| IA | request Flex usa prompt/schema versionados, transcricao, catalogo real de capacidades, dois contact sheets e no maximo 4.000 tokens de saida | credencial ausente, HTTP externo falho, response sem output, usage ausente e JSON invalido |
| Contrato | saida possui sequencia, aprendizados, direitos, capacidade do Estudio e receita | menos de quatro blocos/cenas, direitos ausentes, capacidade sem evidencia e sugestao injustificada de novo agente |
| Auditoria | backend persiste input/output, artefatos, request/response, modelo, tokens e custo conservador | nenhuma conclusao aceita custo nulo; cache/Flex nao reduzem o valor reservado |
| Tela | resultado acompanha fila, diferencia falha de reprovacao, mostra evidencia e oferece retry/importacao | loading explicito, falha visivel e nenhuma verdade recomputada no frontend |
| Projeto | importacao preenche receita sem trocar produto, oferta ou CTA | produto obrigatorio, duracao/categoria coerentes e render bloqueado antes de salvar |
| Apolo | storyboard aceita ate 48 beats e preserva cenas persistidas | texto no video, retrocesso narrativo, duplicacao, custo acima do teto e provider nao homologado |
| Direitos | somente mecanismos abstratos sao reutilizados | pessoa publica, marca, voz, musica, letra ou gravacao copiada bloqueiam producao |
| Pos-producao | audio fonte e preservado e reduzido sob locucao; montagem cruza audio junto do video | fonte sem audio usa silencio explicito; nenhum tom sintetico simula musica; direitos ausentes bloqueiam uso comercial |
| Observabilidade | logs correlacionam URL, executionId, request e response | nenhuma credencial, token ou PII desnecessaria em log |
| Orcamento | as tres analises consomem no maximo o envelope de US$ 0,75, com reserva de US$ 0,25 por execucao | backend bloqueia antes da chamada quando custo conhecido mais reservas exceder o teto |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | sem overflow, CTA tocavel, listas e receita legiveis |

## Criterio de encerramento

A rodada local passa se todos os itens aplicáveis forem aprovados e os cenários alterados forem
reexecutados depois de qualquer correção. Não fazem parte desta homologação publicação, campanha,
envio de contato ou registro de venda. A reanálise produtiva das três referências é uma etapa
posterior ao deploy, limitada ao envelope já definido e sem render automático.

O primeiro render QA usa um ciclo separado de US$ 2,00, aprovado por Plutus. O envelope total desta
homologacao e de US$ 2,75: US$ 0,75 para analise e US$ 2,00 para um unico render original de ate dez
segundos, sem publicacao ou campanha.

## Evidencia local de 26/09/2026

| Validacao | Resultado |
|---|---|
| Backend completo | 3.605 testes executados; a unica falha apontou os dois novos artefatos ausentes do catalogo do Agent Harness. O catalogo foi corrigido e `AgentHarnessCatalogTest` mais `VideoReferenceAnalysisServiceTest` passaram na reexecucao focalizada. |
| Executor audiovisual completo | 242 testes, zero falhas, zero erros e um ignorado; inclui transcricao, ausencia de fala, falha externa, inspecao, montagem e pos-producao. |
| Frontend completo | 784 testes, typecheck e build aprovados; os testes novos distinguem falha tecnica de rejeicao editorial e mostram os limites reais do Estudio. |
| Navegacao local | Chromium desktop, iPhone 15 Pro e Pixel 7 exibiram transcricao, parecer de capacidade, adaptacao segura e CTA sem overflow da pagina; a correcao final de rotulos e resumo foi revalidada no iPhone. |
| Midia deterministica | FFmpeg homologado com clipe sonoro, clipe silencioso, transicao de audio e ducking sob voz; saida H.264 vertical com AAC. |
| Container local | Imagem do `video-management-service` construida pelo Compose versionado e iniciada com `/actuator/health` em `UP`; FFmpeg 8.0.1 disponivel. |
| Pacotes de aplicacao | JAR do backend verificado com 4.168 classes, 661 recursos e 437 cartoes do catalogo; o JAR dentro da imagem teve hash identico. Imagem do frontend iniciou e respondeu `/healthz` dentro da topologia isolada. |
| Segregacao comercial | Nenhuma campanha, publicacao, contato, venda ou render pago foi iniciado na rodada local. |

A navegacao final em desktop, iPhone 15 Pro e Pixel 7 e a reanalise das referencias #1, #2 e #3
permanecem como validacao pos-deploy. Elas devem confirmar o contrato publicado e nao substituem os
testes locais acima.
