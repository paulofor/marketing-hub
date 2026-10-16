# Mensagem para o suporte da OpenAI

Copie uma das versões abaixo e anexe `capella-diagnostico-escuta-74.zip`.
Não é necessário enviar chaves de API. A organização/projeto pode ser informada pelo
titular diretamente no atendimento autenticado, caso seja solicitada.

## Português

**Assunto: GPT-Audio-1.5 contabiliza áudio de entrada, mas não devolve a avaliação sonora**

Olá. Usamos `gpt-audio-1.5` no endpoint `POST /v1/chat/completions` para avaliar voz,
música e ritmo de um anúncio. O pedido inclui um WAV em base64 no campo `input_audio`
e solicita somente texto (`modalities: ["text"]`).

Em duas chamadas com o mesmo corpo, o modelo não entregou a avaliação solicitada:

- `chatcmpl-EZVhVItMG6MajhDRlwJFw23S3tZym`, `created` 1792132045
  (16/10/2026, 06:27:25 UTC): respondeu uma recusa genérica em inglês.
- `chatcmpl-EZWarUXvRz0k7yk0UWsXtnGx1TUGA`, `created` 1792135477
  (16/10/2026, 07:24:37 UTC): afirmou, em português, não ter acesso ao áudio real.

Em ambas, `usage.prompt_tokens_details.audio_tokens` foi 350. O áudio anexado tem
35 segundos, PCM de 16 bits, mono, 24 kHz, 840.000 amostras e decodifica sem erros.
Seu SHA-256 é `a2bcfc8d81eace67e3e1942d99c6d7ad7c1a1f54abd5ee37cf83bbcf9fc18071`.
Os pedidos das duas tentativas são idênticos byte a byte e também estão no ZIP.

Como comparação, o mesmo contrato e modelo avaliaram uma demonstração de 55 segundos:
`chatcmpl-EZVhEHa3Jp8EdGnTuqq4qnyNbkOr9`, `created` 1792132028
(16/10/2026, 06:27:08 UTC), com 550 tokens de áudio e avaliação estruturada recebida.
O pedido, o áudio e a resposta desse caso também estão anexados.

Nas duas respostas problemáticas, `message.refusal` é nulo e `finish_reason` é `stop`;
o texto está em `message.content`. Não tratamos essas respostas como aprovação nem
estamos repetindo automaticamente as chamadas. O cabeçalho `x-request-id` não foi
preservado; os identificadores acima são os IDs dos objetos de resposta, não esse header.

Podem investigar por que não houve avaliação apesar da entrada de áudio contabilizada,
informar se existe alguma incompatibilidade comprovada neste pedido e orientar uma
recuperação segura antes de novas chamadas cobradas? Se houver restrição aplicável,
precisamos entendê-la; não pretendemos contorná-la. Não afirmamos que os tokens de áudio
comprovem a causa ou um defeito específico no provedor.

O pacote contém os corpos originais, os WAVs extraídos dos próprios pedidos, os hashes
e um manifesto. Ele não contém credenciais nem dados pessoais de clientes. Os custos
registrados pelo nosso sistema são estimativas, não valores faturados conciliados.

## English

**Subject: GPT-Audio-1.5 reports input audio tokens but does not return the audio assessment**

Hello. We use `gpt-audio-1.5` with `POST /v1/chat/completions` to assess voice, music and
pacing in an advertisement. The request includes a base64-encoded WAV in an `input_audio`
content part and asks for a text-only response (`modalities: ["text"]`).

Two calls with byte-identical request bodies did not return the requested assessment:

- `chatcmpl-EZVhVItMG6MajhDRlwJFw23S3tZym`, `created` 1792132045
  (2026-10-16 06:27:25 UTC): a generic refusal in English.
- `chatcmpl-EZWarUXvRz0k7yk0UWsXtnGx1TUGA`, `created` 1792135477
  (2026-10-16 07:24:37 UTC): the response stated, in Portuguese, that the model did not
  have access to the actual audio.

Both responses report 350 `usage.prompt_tokens_details.audio_tokens`. The attached audio
is a valid 35-second, 16-bit PCM, mono, 24 kHz WAV containing 840,000 samples. It decodes
without errors. SHA-256: `a2bcfc8d81eace67e3e1942d99c6d7ad7c1a1f54abd5ee37cf83bbcf9fc18071`.
The two byte-identical request bodies are included in the archive.

For comparison, the same contract and model returned a structured assessment for a
55-second demonstration: `chatcmpl-EZVhEHa3Jp8EdGnTuqq4qnyNbkOr9`, `created` 1792132028
(2026-10-16 06:27:08 UTC), reporting 550 input audio tokens. Its original request,
audio and response are also included.

In both problematic responses, `message.refusal` is null and `finish_reason` is `stop`;
the refusal text is in `message.content`. We have not treated these responses as approval
and are not automatically retrying them. The `x-request-id` header was not retained;
the identifiers above are completion object IDs, not HTTP request IDs.

Could you investigate why the assessment was not produced despite reported audio input,
identify any confirmed incompatibility in this request, and advise on safe recovery before
further paid calls? If a restriction applies, we would like to understand and respect it.
We do not assume that token accounting establishes a specific provider-side defect.

The archive contains the original request and response bodies, WAVs extracted from the
requests, checksums and a manifest. It contains no credentials or customer personal data.
Our recorded costs are estimates, not reconciled invoice amounts.
