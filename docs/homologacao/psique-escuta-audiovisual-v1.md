# Escuta audiovisual de Psique — v1

## Contrato OpenAI sem Structured Outputs — 16/10/2026

Pedido: pesquisar a documentação oficial e ajustar o uso em Psique. A página do
[GPT-Audio-1.5](https://developers.openai.com/api/docs/models/gpt-audio-1.5) declara
Structured Outputs não suportado. O guia de [áudio em Chat Completions](https://developers.openai.com/api/docs/guides/audio-chat-completions)
documenta `input_audio` em base64 WAV/MP3; `modalities` define a saída. Mantemos saída
textual, sem gerar áudio adicional. Schema no prompt é orientação, não garantia do provedor.
O guia de [saídas estruturadas](https://developers.openai.com/api/docs/guides/structured-outputs)
recomenda validação local quando não há suporte ao recurso. Não habilitar `json_schema`
nem presumir suporte a JSON mode a partir de documentação de outros modelos.

Histórico conferido no MCP: recibo #3 da demonstração #75 válido; #4/#5 do anúncio #74
sem avaliação, com tokens de áudio registrados. O pedido funcionou no outro arquivo;
a limitação de Structured Outputs não demonstra a causa da recusa específica. Os
[originais preservados](../diagnosticos/capella-openai-escuta-74/README.md) serão usados
como regressão. Nenhuma chamada paga integra esta homologação.

Alternativas comparadas:

| Caminho | Benefício | Custo/risco | Decisão |
| --- | --- | --- | --- |
| Mesmo pedido e validação estrita | Conserva recibos, modelo e custo | Não garante percepção nem conformidade futura | Adotado |
| Áudio livre seguido de segundo modelo estruturador | Separa percepção e formato | Mais custo; pode transformar ausência de escuta em parecer aparente | Não necessário |
| Troca para function calling | Outra forma de transportar dados | Suporte a função não garante schema estrito; muda hashes e exige calibração | Não adotado |

Lacuna observada no leitor Jackson: leitura de árvore sem rejeitar chaves duplicadas
ou conteúdo posterior; coerção de números para textos do parecer. Esses comportamentos
podem transformar resposta ambígua em avaliação aceita. Diagnóstico de texto sem JSON
também precisa distinguir contrato inválido de ausência física de áudio. A causa interna
da resposta de Capella continua desconhecida.

Matriz de aceite definida antes dos testes:

| Área | Prova local exigida |
| --- | --- |
| Caminho válido | Recibo #3 e fixture de outra identidade mantêm o aceite |
| Resposta | Recusa explícita, texto livre, truncamento, duplicatas, conteúdo posterior e tipos incorretos bloqueiam |
| Integração | Reserva → HTTP local → resposta bruta → callback → recibo, sem modelo externo |
| Recuperação/custo | Repetir callback não cobra; falha conserva resposta, custo ou reserva e motivo |
| Observabilidade | Identificador do cliente e `x-request-id` preservados sem credenciais |
| Compatibilidade | Pedido/schema originais permanecem idênticos; nenhum recibo histórico é migrado |
| Segregação | Fixtures identificadas como teste; nenhuma campanha, tarefa ou gasto produtivo iniciado |
| Navegação | Consulta das telas da cadeia, agentes e vídeos; sem mudança de interface ou mídia |

Métrica técnica: zero aceite de JSON ambíguo e zero inferência adicional na recuperação.
Benefício esperado: revisões confiáveis e diagnóstico mais rápido. Conversão, compras,
qualidade perceptual e margem não são resultados destes testes. Adoção depende dos testes
e deploy; reversão pelo fluxo de PR se houver regressão de recibos antes válidos.

Resultado local desta revisão:

- Os quatro casos novos reproduziram aceite indevido antes da correção e bloqueio depois:
  JSON posterior, chave duplicada, justificativa numérica e múltiplas alternativas.
- Backend: rodada relacionada de 375 testes sem falhas/erros (três condicionais não
  habilitados); após o ajuste final da validação de modelo/custo, 37 testes do contrato
  passaram novamente. Inclui arquitetura, orçamento, catálogo e recuperação.
- Psique: 228 testes sem falhas/erros (11 condicionais não habilitados); a validação final
  focada passou nos 57 casos de escuta, captura e recuperação. O WAV histórico íntegro foi
  enviado somente ao servidor HTTP local, com respostas simuladas, em duas identidades.
- O recibo válido #3 permanece aceito; #4/#5 permanecem bloqueados. Callback repetido
  não duplica custo/inferência; os pedidos históricos permanecem idênticos.
- Spotless, revisão do diff, 16 testes do empacotador e geração do pacote existente de
  evidências passaram. Os condicionais dependem de fixtures externas de jornada/publicação;
  nenhum teste da ferramenta auditiva ficou sem execução.

Os identificadores de transporte seguem a [referência oficial da API](https://developers.openai.com/api/reference/overview):
UUID em `X-Client-Request-Id`, `x-request-id` retornado quando disponível. A regressão
confirma persistência no reinício e exclui autenticação/cookies. IDs novos não recuperam
retroativamente os cabeçalhos das chamadas antigas. A recusa específica do anúncio #74
continua sem explicação comprovada; esta entrega não declara o experimento desbloqueado.

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

## Ajustes após os checks do primeiro HEAD

O run de contratos `38752076916` identificou um falso bloqueio no validador SSH:
aceitava apenas expansão textual, recusando o array derivado do mesmo preflight.
O teste agora aceita a derivação comprovada e recusa origem arbitrária, reatribuição
ou remoção do array. Os nove publicadores e 92 testes relacionados passaram localmente.

A fixture financeira do run `38752075954` encerrava na inicialização porque instanciava
o controller de tarefas sem declarar sua nova dependência. Foi acrescentado um test
double somente nessa fixture; o serviço financeiro e o MySQL continuam reais.
A rodada local `--persistence-only` passou em API, reserva nativa, concorrência,
reinício e migração (67 segundos), com limpeza do projeto Compose exclusivo.

O mesmo run teve falha isolada de resolução do plugin Maven na imagem das fronteiras;
não houve erro de compilação Java nessa etapa. Os outros builds do HEAD passaram.
A causa externa não foi presumida como resolvida: o check continua obrigatório no
HEAD atualizado. O build e smoke de Psique no primeiro PR passaram, suprindo a
limitação de disco do build integral local sem iniciar uma publicação de produção.

## Pausa final na linha do tempo — 16/10/2026

A tentativa local com os MP4 reais de Capella #72/#73 reproduziu um bloqueio antes da
inferência: o vídeo de 55 s possui faixa AAC de aproximadamente 47,9 s; os segundos finais
são imagem com silêncio. O decoder retorna apenas a faixa sonora. O capturador comparava essa
duração com a do vídeo e rejeitava o arquivo. A fixture anterior terminava áudio e imagem
juntos e não detectava essa classe de falha. O roteiro da revisão continua vindo da narração
final, não do campo histórico do primeiro render.

Foram comparadas três soluções: remuxar todas as entregas, extrair com FFmpeg e completar a
linha do tempo no OfflineAudioContext existente. A última preserva os MP4, os hashes e o
histórico, dispensa nova dependência e representa o mesmo silêncio ouvido na reprodução.
Não alonga fala, normaliza volume ou corta palavras. Áudio mais de um segundo além do vídeo
continua inválido; silêncio integral, arquivo trocado e duração visual divergente continuam
bloqueados. A folga de até um segundo para padding do codec preserva o comportamento anterior.

O manifesto passa a preservar duração decodificada e silêncio final, além de pico/RMS e hash.
A alteração aplica-se ao mix com início em zero entregue pelo produtor atual. Trilhas com
offset inicial externo exigem sua própria prova; não se presume sincronização labial.

Matriz da correção: dois IDs independentes com voz mais curta que o vídeo, alinhamento
anteriormente válido, sinal/volume preservados, pausa silenciosa, faixa excedente e silêncio
integral. O teste falhou antes da correção e usa Chromium real, sem rede ou modelo pago.
A captura real dos mesmos MP4 produziu 24 quadros e WAVs de 35/55 s; as pausas decodificadas
foram aproximadamente 2,34/7,17 s. Isso comprova extração e preservação temporal, não qualidade
persuasiva, naturalidade ou demanda: esses aceites continuam independentes.

Validação desta correção: 44 testes Node e os três smokes do navegador passaram;
221 testes Java do executor terminaram com 0 falhas e 0 erros
(11 condicionais existentes). O empacotamento das 115 experiências vigentes passou.
Os testes de browser foram executados em sequência para respeitar o limite local de processos.
Nenhuma inferência paga foi usada na validação local.
