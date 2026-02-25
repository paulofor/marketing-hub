# Pipeline para geração das imagens sem imagem de origem

Este fluxo cobre todo o ciclo do novo **formulário simples do Lead Portal**, pensado para profissionais que não possuem uma foto de referência. A entrada agora é 100% textual e o sistema transforma os dados em prompts prontos para gerar materiais de divulgação.

## 1) Coleta estruturada no formulário simples
- **Entrada:** Nome do profissional, formas de contato, nome/descrição do estúdio, local de atuação e serviços prestados (lista de opções e/ou texto livre). O slug do fluxo (`formulario-simples-<atividade>`) informa o tipo de atividade principal.
- **Processo:** O Lead Portal (frontend) usa o Flow configurado para esse slug e envia as respostas via `POST /api/flows/{slug}/submissions`. Nenhuma imagem é solicitada.
- **Saída:** Registro na tabela `flow_submissions` com todas as respostas sanitizadas.

## 2) Enriquecimento e montagem automática dos prompts
- **Processo:**
  - O backend identifica formulários simples pelo prefixo `formulario-simples-` e monta um **SimpleImageBriefing** contendo atividade, serviços, local e contatos.
  - O serviço `FlowImagePromptService` converte o briefing em um prompt rico, descrevendo visual, chamada, CTA e contexto do profissional. Todos os prompts incluem o trecho “entregue um pacote em lote (batch) com pelo menos 6 variações quadradas”.
  - O pacote é registrado em `flow_submission_image_package` com `planned_outputs = 6`, `free_images = 0`, prompt final e modelo padrão `gpt-image-1` (pode ser sobrescrito por fluxo).
- **Saída:** Pacote em status `RECENT`, pronto para ser consumido pelo worker, mesmo sem imagem enviada.

## 3) Geração batch no worker de IA
- **Processo:**
  - O serviço `LeadPortalImagePackageWorkerService` lista os pacotes `RECENT/RECEIVED`, marca como `PROCESSING` e envia o prompt compilado para o worker.
  - O worker utiliza somente o texto (não há imagem base) e gera as variações solicitadas em lote, populando `flow_submission_image_item` e atualizando custos (`image_unit_price_usd`, `image_total_price_usd`).
- **Saída:** Pacote avança para `WATERMARK_PENDING` contendo as imagens e os metadados de geração.

## 4) Marca d’água, ZIP e oferta comercial
1. **Marca d’água:** o serviço de watermark aplica a camada institucional em todas as variações.
2. **Compactação:** o `image-zipper-service` cria o ZIP com as versões de prévia.
3. **Pagamento:** `lead-portal-payments-service` gera o link de checkout e anexa ao pacote.
4. **Envio:** o e-mail de amostra é disparado com o ZIP e o link de pagamento.
5. **Entrega final:** mediante confirmação do pagamento, um novo pacote (sem marca d’água) é disponibilizado ao profissional.

## 5) Monitoramento e suporte
- **Painel de estatísticas (frontend `/monitoramento/imagens` + backend `/api/image-material/dashboard`):**
  - Mostra volume de submissões, pacotes em cada status, imagens planejadas vs. geradas e custo estimado (sempre considerando batch de 6 variações).
  - Exibe os pagamentos consolidados por moeda e lista os últimos pacotes para auditoria rápida.
- **Tela de acompanhamento de casos (`/monitoramento/imagens/casos/:id` + `/api/image-material/submissions/{id}`):**
  - Expõe todas as respostas do formulário, os prompts utilizados, custos e histórico completo de status (`flow_submission_image_package_status_history`).
  - Serve como referência quando o time de suporte precisa investigar falhas ou refazer um lote.

Com este pipeline, qualquer profissional que responder ao formulário simples recebe um pacote batch de imagens promocionais, mesmo sem fornecer arquivos. O time interno tem visibilidade total dos custos e de cada caso específico para agir rapidamente em incidentes.
