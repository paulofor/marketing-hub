# ElevenLabs Video-to-Music — análise de fonte revisada em 06/11/2026

**Fato previamente não registrado no radar:** API `POST /v1/music/video-to-music` publicada no changelog de **19/10/2026**, distinta do recurso ElevenCreative Instant Soundtracks de 15/10. Não se trata de um lançamento realizado em 06/11.

**Status:** API ATIVA para usuários habilitados de planos pagos do Eleven Music; não foi executado teste autenticado. O endpoint recebe até 10 vídeos, 200 MB no conjunto e no máximo 600 s de duração combinada. Permite descrição e até 10 tags, `add_sfx=true` para efeitos visuais sincronizados, `use_audio_as_reference=true` e `separate_tracks=true` para música e SFX separados em ZIP. Modelos music_v1/v2/v2_5 disponíveis na referência.

**Licença e preço:** direitos comerciais dependem do plano. A página oficial diz que planos de autosserviço pagos dão direitos comerciais amplos, MAS filmes, TV e jogos de estúdio requerem Enterprise. Não encontramos tarifa única pública específica para video-to-music; confirmar créditos da API e restrições de uso antes da produção.

**Evolução correlata (02/11):** SFX v3 ficou ativo para todos os workspaces e é usado para efeitos com `add_sfx` no Video-to-Music. SFX v2 descontinuará em 01/03/2027, conforme documentação oficial.

**Fontes originais:**
- https://elevenlabs.io/docs/changelog (seção 19/10/2026 e 02/11/2026)
- https://elevenlabs.io/docs/api-reference/music/video-to-music
- https://elevenlabs.io/blog/introducing-instant-soundtracks (15/10/2026)
- https://elevenlabs.io/eleven-music-api
- https://elevenlabs.io/docs/eleven-creative/products/music

**Oportunidade Marketing Hub:** automatizar trilha de anúncios/vídeos existentes após a geração e exportar efeitos separados para revisão e mixagem, medindo custo de áudio aprovado e garantindo licença correta. 
