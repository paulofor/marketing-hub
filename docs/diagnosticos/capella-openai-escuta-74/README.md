# Capella: material para o suporte da OpenAI

> STATUS: HISTÓRICO
> FONTE CANÔNICA: [Revisão de Psique](../../canonical/customer-agent-personas-canon.v1.md)
> ÚLTIMA VALIDAÇÃO: 2026-10-16

**Este é o material mencionado nas solicitações #3495 e #3496.** Ele reúne os dados da
avaliação sonora do anúncio #74 de Capella, produto #7, ciclo #12, experimento #105.
A chave administrativa de consumo já funcionava: este incidente trata da resposta da
avaliação do áudio, não do cadastro da chave nem da consulta de custos.

## O que baixar e enviar

1. [Baixe o pacote completo](capella-diagnostico-escuta-74.zip). No GitHub, abra o arquivo
   e use **Download raw file** (ícone de download). O ZIP contém os pedidos completos,
   respostas, áudios WAV, identificadores, custos estimados, hashes e o texto do chamado.
2. Abra [a mensagem pronta para o suporte](mensagem-suporte.md) e copie o conteúdo do
   chamado em português ou inglês.
3. Entre no [Help Center da OpenAI](https://help.openai.com/) com a conta responsável e
   anexe o ZIP ao atendimento. Se já houver um chamado, acrescente o material nele.

Os arquivos são do mesmo caso relatado na conversa. O pacote foi **reconstituído dos
registros persistidos**, porque o ZIP antigo apontava para uma sandbox temporária que
não está disponível neste ambiente. Os pedidos e respostas recuperados do executor são
idênticos aos preservados no banco. Nenhuma avaliação ou produção paga foi repetida
para preparar este material.

## O que aconteceu

| Recibo / tarefa | Mídia | Resultado registrado | Identificador da resposta OpenAI |
| --- | --- | --- | --- |
| #3 / #740 | Demonstração #75, 55 s | `COMPLETED`, avaliação sonora `PASS` | `chatcmpl-EZVhEHa3Jp8EdGnTuqq4qnyNbkOr9` |
| #4 / #740 | Anúncio #74, 35 s | `INVALID`, texto de recusa sem avaliação estruturada | `chatcmpl-EZVhVItMG6MajhDRlwJFw23S3tZym` |
| #5 / #741 | Mesmo anúncio #74 | `INVALID`, resposta afirma não ter acesso ao áudio | `chatcmpl-EZWarUXvRz0k7yk0UWsXtnGx1TUGA` |

Os três pedidos usaram `gpt-audio-1.5`, `POST https://api.openai.com/v1/chat/completions`,
`modalities: ["text"]`, entrada `input_audio` em WAV e `service_tier: "default"`.
Esse é o contrato histórico efetivamente enviado; não foi trocado durante a coleta.

A [documentação do GPT-Audio-1.5](https://developers.openai.com/api/docs/models/gpt-audio-1.5)
confirma que **Structured Outputs não é suportado**. O schema dentro do prompt orienta o
modelo, sem garantir conformidade. Isso exige validação local; não explica por si só a
recusa específica, pois o mesmo contrato funcionou para a demonstração. A
[revisão do consumidor e sua matriz](../../homologacao/psique-escuta-audiovisual-v1.md#contrato-openai-sem-structured-outputs--16102026)
documenta essa distinção e preserva os anexos históricos sem alterações.

O provedor informou **350 tokens de áudio em cada tentativa do anúncio** e 550 na
demonstração. Os dois pedidos do anúncio são idênticos byte a byte. O áudio do anúncio
tem 35 segundos, 840.000 amostras, PCM de 16 bits, mono, 24 kHz, e decodifica sem erros.
Esses fatos afastam arquivo ausente, vazio ou corrompido; **não explicam a causa interna
das respostas do modelo**. Não há evidência para atribuir o caso a uma política específica.

O campo `message.refusal` veio nulo, `finish_reason` foi `stop` e a recusa apareceu no
campo textual `message.content`. O erro de leitura de JSON registrado pelo Hub é uma
consequência desse texto não estruturado. Nenhuma dessas respostas constitui um parecer
sonoro válido nem pode ser convertida em aprovação.

## Conteúdo verificável

- [Manifesto](manifesto.json): origem, correspondência entre recibos/arquivos, horários,
  identificadores, formato de áudio e custos estimados das três chamadas.
- [Pedido do anúncio sem base64](pedido-anuncio-sem-base64.json): leitura do prompt e
  dos parâmetros no GitHub. O marcador do áudio é apenas uma representação para leitura;
  **o pedido original completo está no ZIP**.
- [Pedido da demonstração sem base64](pedido-demonstracao-sem-base64.json): comparação
  com o caso que devolveu uma avaliação válida.
- [Resposta #3](respostas/recibo-3.json), [resposta #4](respostas/recibo-4.json) e
  [resposta #5](respostas/recibo-5.json): corpos originais, sem modificações.
- [Hashes dos arquivos](SHA256SUMS.txt): incluem o ZIP. Dentro do pacote existe outra
  lista para conferir os arquivos extraídos.

Os horários `created` do provedor foram convertidos para UTC. Os horários do banco são
preservados separadamente, sem acrescentar um fuso não registrado: os relógios apresentavam
diferença. `chatcmpl-...` identifica a resposta; não é o cabeçalho `x-request-id`, que não
foi preservado e não pode ser inventado. Se o suporte precisar da organização/projeto,
o titular deve informar esse contexto diretamente no atendimento autenticado.

O pacote não contém chave de API, cabeçalho de autenticação, cookies, URL assinada ou
dados pessoais de clientes. Contém apenas o material operacional deste incidente e
narrações sintéticas do produto. Os custos individuais são estimativas registradas pelo
Hub, não fatura conciliada nem total de toda a preparação de Capella.

## Conferência local realizada

- Correspondência exata entre pedidos/respostas do executor e recibos persistidos.
- Igualdade dos pedidos das duas tentativas do anúncio e dos hashes dos áudios.
- Integridade dos WAVs, duração/amostras e decodificação com FFmpeg/FFprobe.
- Conferência do ZIP, dos hashes, dos arquivos JSON e dos links relativos deste material.
- Inspeção de conteúdo e busca de credenciais nos textos e nos arquivos compactados.

Este material melhora a recuperação e o encaminhamento de incidentes pelos agentes: a
evidência fica acessível pelo repositório, com identidade e integridade verificáveis, em
vez de depender de um caminho temporário da sandbox. Não muda prompts, gates, orçamento,
aprovações ou estado do experimento. O critério desta entrega é permitir ler o relato e
baixar os anexos; não comprova resolução da escuta, lançamento, vendas ou lucro.

## Referências

- [Investigação e recuperação anteriores](../../homologacao/capella-revisao-audio-tempos-v1.md).
- [Conferência operacional do PR #5607](https://github.com/paulofor/marketing-hub/pull/5607#issuecomment-6210489486).
- [Entrada de áudio por Chat Completions — OpenAI](https://developers.openai.com/api/docs/guides/audio-chat-completions).
- [Recusas e saídas estruturadas — OpenAI](https://developers.openai.com/api/docs/guides/structured-outputs).
