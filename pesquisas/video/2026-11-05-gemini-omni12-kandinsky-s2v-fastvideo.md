# Radar de IA para Vídeo — 05/11/2026

**Critério editorial:** três mudanças relevantes não registradas na rodada anterior: Gemini Omni 1.2 Flash chega em GA e reduz preço; Kandinsky S2V libera pesos para avatares; FastVideo libera implementação NVFP4 significativamente mais rápida para Pika H3 Flex. Adicionalmente, a documentação do Google **corrige/atualiza o piso de aposentadoria do Veo 3.1** para maio de 2027. Fontes verificadas em 05/11/2026.

## 1. Google Gemini Omni 1.2 Flash — GA em 05/11/2026

- **Status: ATIVO (GA), Google Cloud Agent Platform, região global, API Interactions**. Identificador: `gemini-omni-1.2-flash`; API GenerateContent não suportada por esta ficha do Google Cloud. API Gemini de outro produto não deve ser presumida equivalente.
- Gera e edita vídeo de **3 a 15 segundos**, com áudio, por texto, imagens ou referências. Estende tomadas, modifica cena e faz upscale independente; 360p/720p/1080p/4K, 16:9/9:16, até 3 saídas por prompt. Entrada de vídeo para edição até 15s. O catálogo não marca a função **agentic video understanding** como suportada, apesar de aceitar vídeo de entrada.
- **Preço: US$12/1M tokens de vídeo gerado** contra US$17,50/1M da versão 1.1, redução de ~31,4%. A 720p, 5792 tokens/s => **US$0,0695/s** de saída; 15s de saída em 720p ≈ US$1,04 antes de entradas. 1080p ≈ US$0,1043/s, 4K ≈ US$0,2085/s. Entradas: US$1,50/1M tokens. Sem pesos abertos. Preços oficiais, qualidade ainda não medida independentemente.
- Uso: publicidade, vídeos de produto, animação de campanhas, vídeo-vídeo e continuidade por prompt. Avança frente a Kling 4.0 via Runway em tarifa de 720p (Kling promocional: ~US$0,1256/s até 30/11, com limite de 30s), mas **não** prova maior fidelidade visual. Veo 3.1 é outra opção ativa com pipeline diferente.
- Aposentadoria mínima documentada **05/11/2027 ou posterior**. Fontes: https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/gemini/omni-1-2-flash ; https://cloud.google.com/model-garden/pricing ; https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/model-versions

## 2. Kandinsky S2V 1.0 — pesos abertos para avatares (03/11)

- **Status: ATIVO PARA EXECUÇÃO PRÓPRIA**, sem API pública comercial de autosserviço confirmada. Laboratório Kandinsky / Sber (Rússia).
- Entrada: foto, voz pré-gravada e prompt. Gera avatar falante com corpo inteiro, lábios e gestos; não sintetiza o áudio; 25 fps; SD/HD, vídeos em blocos de 5 s. Lite 2B, Pro 21B, modelos destilados. Pesos e código MIT; dependências e direitos sobre imagem e voz devem ser conferidos.
- H100: Lite destilado 8,7s por segundo gerado (pico 57 GiB), Pro destilado 41,5s/s (pico 46 GiB). Requisito típico >=48 GB VRAM; tiling pode baixar pico para ~28 GB com artefatos. Sem comparações independentes robustas.
- Diferença vs Synthesia/HeyGen: controle e distribuição própria com pesos abertos, porém custo operacional de GPU e engenharia. Adequado a locuções, UGC autorizado e vídeos explicativos se lipsync PT-BR e consistência forem aprovados.
- Fontes: https://github.com/kandinskylab/kandinsky-s2v ; https://huggingface.co/kandinskylab/Kandinsky-S2V-1.0-Lite-distill

## 3. FastVideo + Pika H3 Flex — ganho de eficiência (03/11)

- **Status: ATIVO PARA EXECUÇÃO PRÓPRIA (pesos NVFP4 + código)**, otimização externa de modelo que já havia sido lançado em 29/10. Hao AI Lab / UC San Diego (EUA), modelo Pika baseado no MiniMax H3 (China).
- FastVideo BF16 ~2,4x em GB200 vs código oficial na mesma GPU. Em NVFP4/FP4 attention, 5s/768p ~15,2s vs 59,7s oficial. Resultado anunciado ~7x é comparação 4 GPUs vs 1 GPU, não redução de custo de 7x. Caso 5s/480p em 5,8s é em **GB200**, não placa doméstica.
- Pesos Pika derivados da base MiniMax H3 mantêm **MiniMax H3 Community License**, com restrições territoriais e comerciais. Nem pesos abertos nem aceleração equivalem a direito de uso irrestrito.
- Uso: reduzir custo de iteração/rascunhos quando houver GPU compatível (Blackwell), medindo qualidade e custo por tomada aprovada.
- Fonte: https://haoailab.com/blogs/pika-h3-flex/ ; https://huggingface.co/Pika-Labs/Pika-H3-Flex

## 4. Atualização crítica: vida útil do Veo 3.1

- Documentação oficial Google Cloud consultada em 05/11 indica que os endpoints **Veo 3.1 GA e Veo 3.1 Fast GA** estão **ATIVOS** e mostram aposentadoria **04/05/2027 ou depois**. O relatório de 04/11 indicava prazo mínimo de 17/11/2026; a informação atual do fornecedor substitui a anterior, **não** há fechamento confirmado para 17/11.
- Isso não reativa a **Gemini API Veo 3.1 Preview, DESCONTINUADA** anteriormente em 22/10. **Sora 2 permanece DESCONTINUADO**: app/web 26/04 e API 24/09/2026.
- Fontes: https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate ; https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/model-versions ; https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation

## Panorama operacional comparável

| Sistema / país | Status em 05/11 | Papel real |
| --- | --- | --- |
| Gemini Omni 1.2 Flash / EUA | **ATIVO GA** na Google Cloud Interactions API | vídeo+áudio com edição até 15s; 720p ≈ US$0,0695/s |
| Veo 3.1 GA / EUA | **ATIVO** no Google Cloud, retirada >=04/05/2027 | vídeo cinematográfico com som, 4/6/8s |
| Kling 4.0 / China | **ATIVO** via Runway API; API direta segundo comunicado, autenticação não testada | vídeo 3–30s, referência e edição |
| Pika H3 Flex / EUA e China | **ATIVO**: Pika API e pesos; licença limitada para pesos | audiovisual com referências e stems hospedados |
| FastVideo H3 Flex / EUA | **ATIVO para inferência própria** | aceleração NVFP4 sob licença H3 |
| Kandinsky S2V / Rússia | **ATIVO para inferência própria** | avatar falante sem sintetizar voz |
| Synthesia / Reino Unido | **ATIVO no SaaS**, MCP beta | apresentadores, vídeos de documentos |
| Genie 4 / EUA | **LIMITADO** em pesquisa | simulação interativa, não alternativa SaaS geral |
| Sora 2 / EUA | **DESCONTINUADO** | não recomendar para novos projetos |

## Consequência para Marketing Hub

Prioridade 1: benchmark no Google Cloud de **Omni 1.2 Flash** em 720p e 1080p contra Kling/Seedance/Wan, verificando fidelidade de rótulos de produto, lip-sync em PT-BR, continuidade, custo total por vídeo aprovado e edição por prompt. Prioridade 2: testar **Kandinsky Lite destilado** para avatares com licença e consentimento, simulando custos de H100 por minuto aprovado. Prioridade 3: estudar FastVideo NVFP4 no laboratório de vídeo se houver GPU Blackwell compatível e autorização territorial/contratual.

**Cards:** três achados úteis, cada um com fonte revisada e SHA-256 calculado sobre o conteúdo exato da fonte Markdown publicado neste commit. Não criar card isolado para Veo 3.1, pois a retificação já está documentada no relatório.

**Método:** verificação de documentação Google Cloud e preços, repositórios/model cards Hugging Face, pesquisa técnica UCSD e checagem de descontinuação OpenAI. Não houve chamadas autenticadas aos endpoints nem comparação de qualidade própria.
