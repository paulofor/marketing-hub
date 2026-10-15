# Escuta audiovisual de Psique — v1

## Aceite e alternativas

A solicitação de 15/10/2026 acrescenta escuta real à Psique existente. O histórico MCP
confirma NOT_LISTENED tanto na tarefa bloqueada #731 quanto na aprovada #736 de Capella.
Frames e roteiro não revelam interpretação da voz, mixagem ou o instante realmente falado.

| Alternativa | Benefício | Limite/esforço | Decisão |
| --- | --- | --- | --- |
| Transcrição e medições apenas | Barata, determinística | Não avalia entonação nem relação musical | Apoio, insuficiente |
| Novo agente sonoro separado | Especialização independente | Mais fila, contratos e coordenação | Não necessário |
| Ferramenta auditiva de Psique, combinada com frames | Mesmo MP4, achados temporais e revisão independente | Uma chamada adicional governada por mídia | Adotada |

Não produzir vídeos, reabrir tarefas históricas nem consumir autorização comercial por deploy.
Nenhum teste local chama fornecedor pago. A avaliação perceptual do modelo real permanece uma
validação posterior dentro do teto próprio do produto, distinta do contrato simulado.

## Matriz definida antes da implementação

- Caminho completo: MP4 → hash → áudio decodificado → reserva/auditoria → modelo local →
  resposta preservada → recibo backend → contexto Psique → decisão e correção para Apolo.
- Voz: naturalidade, monotonia, pausas/ênfase; música: equilíbrio, interferência, energia;
  marcações temporais válidas; limitações explícitas, incluindo sincronismo labial não aferido.
- Sincronização: áudio ouvido versus frames com IDs/instantes, legendas e demonstração;
  avaliações insuficientes não podem virar aprovação.
- Falhas: arquivo trocado, silêncio/ausência de áudio, tempo fora do vídeo, JSON inválido,
  timeout, recusa, falta de uso/token, custo desconhecido, saldo insuficiente, pausa.
- Recuperação: callback perdido, reinício após resposta e após envio incerto não repetem chamada.
- Isolamento: duas identidades de produto/experimento, mídia e tarefa distintas; imagem sem vídeo
  mantém fluxo anterior e histórico aprovado não é migrado.
- Observabilidade: request/response integrais, hash, modelo/tier, uso separado por modalidade,
  reserva e estimativa em USD no ledger existente; desconhecido nunca é zero.
- Compatibilidade: Chrome desktop/iPhone/Pixel emulados, sem alterar páginas comerciais.
- Métrica técnica: cobertura de escuta e bloqueios detectados na fixture, replays sem chamadas;
  efeito esperado: menos retrabalho e melhor compreensão. Conversão/margem não medidas.

## Fontes oficiais consultadas em 15/10/2026

- https://developers.openai.com/api/docs/guides/audio-chat-completions
- https://developers.openai.com/api/docs/models/gpt-audio-1.5
- https://developers.openai.com/api/docs/pricing

GPT-Audio-1.5 recebe áudio/texto e devolve texto por Chat Completions. Não recebe imagens;
Psique combina depois os achados auditivos com seus frames. Sem suporte documentado a
Structured Outputs, validar JSON localmente. Não atribuir capacidade labial a áudio/texto.

## Resultado local e limites da evidência

- A rodada ampla do backend executou 4.740 testes e identificou uma falha: os novos
  prompts não estavam no catálogo público do harness. O catálogo foi corrigido e seus
  17 testes passaram, assim como as regressões posteriores de gate, custo e transporte.
- Psique: 221 testes Java sem falhas/erros (11 condicionais não habilitados); Têmis:
  113 sem falhas/erros (um condicional). A escuta usa HTTP local com fornecedor simulado;
  dois produtos preservam a resposta após callback 503 e reinício sem segunda chamada.
- Browser: 44 testes da suíte existente e três verificações de extração/pixels passaram.
  O MP4 H.264/AAC sintético conserva som, duração, hash e frames; silêncio e bytes inválidos
  bloqueiam. Desktop, iPhone e Pixel emulados usam a mesma entrada segregada.
- O Chrome 153.0.8010.36 fixado pelo Dockerfile foi baixado com seu checksum verificado;
  as três verificações de áudio/MP4 passaram também nesse binário, sem inferência paga.
- MySQL 5.7 real: DDL, chave única por tarefa/mídia, isolamento, auditoria e custo NULL
  aprovados. Topologia temporária removida. Changelog mestre usa include relativo e DATETIME.
- Os 38 testes do empacotador/seleção de publicação passaram. A atestação sucessora de
  compatibilidade de Mira preserva a anterior; sua seleção retorna `frontend-version=none`.
- Bash syntax/ShellCheck passaram nos scripts relevantes e no trecho operacional alterado.
  A expansão remota intencional está documentada; argumentos SSH usam array.
- Limitação concreta: o build Docker integral local parou por `No space left on device`
  ao extrair Chrome. Somente caches produzidos por esta execução foram removidos. O Java,
  os contratos, o banco e o navegador exato foram validados separadamente; o build integral
  e a prova dentro da imagem permanecem checks obrigatórios do PR antes do merge.

Não há teste pago de percepção nesta entrega: os casos com voz monótona, música invasiva
ou legenda atrasada são respostas/correspondências simuladas para testar o gate, não uma
comprovação de que o fornecedor já reconhece cada defeito. A calibração real deve comparar
controles bons/ruins sob autorização própria e registrar resultados/custos, sem recrutamento.
A melhoria está apta ao fluxo governado; não autoriza produção de vídeo, campanha ou gasto
novo por si só e não mede conversão, compras ou margem.
