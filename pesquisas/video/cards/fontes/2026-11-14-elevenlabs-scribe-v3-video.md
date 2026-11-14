# ElevenLabs Scribe v3 e Live: transcricao para audiovisual

Verificacao: 14/11/2026. Lancamento: 10/11/2026, artigo atualizado em 13/11.
Fornecedor: ElevenLabs, Estados Unidos. Status: ATIVOS em ElevenAPI e no aplicativo. Pesos proprietarios.

## Melhoria

Scribe v3 transcreve fala de videos e audios com tempos por palavra e identificacao de locutores. A ElevenLabs relata 27% menos erros de palavra em relacao ao v2 no benchmark FLEURS de 102 idiomas e 17% menos erros de diarizacao em gravacoes com tres ou mais pessoas. Oferece contexto de idiomas, formata numeros e aceita ate 1.000 termos-chave conforme documentacao atual.

O Scribe v3 Live adiciona diarizacao em tempo real, suporte a alternancia de idiomas e predicao contextual do fim da fala. Nos testes internos da empresa, ele apresenta 43% menos WER semantico e 31% menos WER tradicional versus v2 Realtime. Sao medidas diferentes; nao ha validacao propria em portugues brasileiro nesta rodada.

## Disponibilidade, custo e limites

Modelo batch `scribe_v3`, ATIVO por API, US$0,20 por hora de audio. Modelo streaming `scribe_v3_live`, ATIVO por API, US$0,22 por hora. Nao houve aumento de preco base em relacao aos v2 correspondentes na pagina atual. Orientacao por termos-chave, edicao e outros recursos podem ter tarifa adicional. Em algumas configuracoes e regioes o modelo padrao ainda e anterior; configurar o ID explicitamente. Servico na nuvem; instalacao privada e residencia de dados dependem de contrato Enterprise.

Uso em legendagem, documentarios, entrevistas, indexacao e preparacao de dublagem, mas nao gera video, audio de voz sintetica ou lipsync. Complementa geradores como Kling 4.0 e Gemini Omni 1.2, sem competir com eles.

Hipotese Marketing Hub: medir WER, identificacao de falantes, timestamps, custo e tempo de revisao em 10 videos PT-BR transcritos com v2 e v3.

## Fontes

- https://elevenlabs.io/blog/scribe-v3
- https://elevenlabs.io/pricing/api
- https://elevenlabs.io/docs/overview/capabilities/speech-to-text
- https://elevenlabs.io/speech-to-text
