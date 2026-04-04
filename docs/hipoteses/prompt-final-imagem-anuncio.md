**Como o prompt foi gerado (visão banco de dados)**  
- A tabela `experiment` mantém o resultado final da etapa `AD_IMAGE_BRIEFING`. Para o experimento `id=10`, o campo `ad_image_briefing` contém o JSON `{"adImageBriefing":{"briefings":[...]}}`. Extraindo `$.adImageBriefing.briefings[0].visualBriefing` encontramos exatamente o texto que você compartilhou (“Variação DOR (estático): foco na fricção ‘quanto custa?’...” etc.), confirmando que o prompt foi persistido ali após a geração.  
- O histórico da geração fica em `experiment_pipeline_generation_job`. O job mais recente para esse experimento/seção tem `id=8C2B25ABDED1459D9E27D47B18DFBDD3`, `section='AD_IMAGE_BRIEFING'`, `status='COMPLETED'`, `model='gpt-5.2'`, `input_tokens=8412` e `output_tokens=2213`. O campo `prompt` registra o texto completo enviado ao modelo (começando por “Experimento #10...” e incluindo todas as regras do ângulo da campanha), e `response_content` guarda o JSON com os três briefings (dor, resultado, prova).  
- Cada processamento bem-sucedido também gera um log em `ai_worker_generation`. Existe um registro (`id=228`) com `domain='experiment.pipeline.ad-image-briefing'`, `reference_id='10'`, `model='gpt-5.2'`, `input_tokens=8412`, `output_tokens=2213` e custo estimado em `US$ 0,0075`, preservando o mesmo `prompt` e o `raw_response` enviado pela OpenAI.

**Fluxo no código**  
1. **Disparo do pipeline** – Quando o endpoint `POST /api/experiments/{id}/pipeline/ads` é chamado (`ExperimentController.requestPipelineAds`), o serviço `ExperimentPipelineGenerationService` (arquivo `backend/ads-service/.../ExperimentPipelineGenerationService.java`) agenda as seções necessárias.  
2. **Construção do prompt** – `buildUserPrompt(...)` monta o texto em múltiplas partes: metadados do experimento, outputs anteriores (ângulo, copy etc.) e as instruções específicas da seção. Para `AD_IMAGE_BRIEFING`, o bloco iniciado em `appendSectionPrompt` (mesmo arquivo, linhas ~460-520) injeta o briefing padrão: contexto do nicho, dor/promessa/mecanismo/prova e a lista de regras (“Gerar exatamente 3 briefings...”, “definir assetType...”, “safe margins”, etc.).  
3. **Estrutura do request** – `enqueueJob(...)` grava o job com o `prompt` produzido, o modelo solicitado (default `gpt-5.2`) e `requestBodyJson`, que inclui:  
   - Mensagem de sistema criada por `buildSystemPrompt` (linhas ~327-338) — define o papel “especialista em marketing direto”.  
   - Mensagem de usuário com o `prompt` acima.  
   - `response_format` contendo o schema JSON específico da seção (definido no `switch` que começa em ~803). Isso obriga o modelo a devolver o objeto `adImageBriefing`.  
4. **Execução pelo worker** – O worker Java em `ai-worker` roda `ExperimentPipelineGenerationWorkerService`:  
   - Usa `ExperimentPipelineBackendClient` para listar jobs (`/api/internal/experiment-pipeline/jobs/pending`), reclamar (`/claim`) e, após gerar, finalizar (`/complete`).  
   - Ao receber o job, `ExperimentPipelineOpenAiClient.generate` lê `requestBodyJson`, reforça o prefixo comum `PIPELINE_PROMPT_PREFIX`, força o uso de `gpt-5.2` e envia o payload para `/responses`. O conteúdo retornado (já em JSON) é enviado de volta ao backend como `response_content`, junto com métricas de tokens/custo.  
5. **Persistência** – No backend, `completeJob(...)` chama `applySectionContent(...)`, que grava `experiment.setAdImageBriefing(responseContent)`. Os dados tornam-se acessíveis para outras rotinas, como `ExperimentPipelineAdExtractor`, que cruza cada variação de copy com o briefing correspondente ao gerar planos criativos.  
6. **Auditoria** – `AiWorkerGenerationService.recordGeneration` grava o prompt original e o raw response na tabela `ai_worker_generation`, permitindo recuperar os detalhes do chamado à OpenAI.

Portanto, o prompt mostrado é a saída `response_content` do job `AD_IMAGE_BRIEFING` do experimento 10, gerado pelo backend `ExperimentPipelineGenerationService` e produzido via worker OpenAI com modelo `gpt-5.2`. Esse texto serve como briefing para o modelo de imagem (`gpt-imagem-1.5`) na etapa seguinte de criação visual.

**Testes / validações**  
- Não havia testes automatizados associados à análise de dados; apenas consultas SQL para comprovar os registros mencionados.
