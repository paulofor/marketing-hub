# Ordem operacional da landing do experimento

Este guia descreve a sequência obrigatória de ações para gerar, aplicar e aprovar a landing page usada como formulário do experimento (ex.: `exp-10-landing`). A ordem consolidada abaixo evita que o Portal do Lead volte a exibir placeholders como `https://placehold.co/1600x900/png?text=Hero...` e garante que o deploy continue 100% automatizado.

> Referências técnicas:
>
> - Estrutura do pipeline (`CAMPAIGN_ANGLE` → `LANDING_PAGE_HTML`) em `docs/experiment-pipeline-artifacts-visual.md` e na tela **Experimentos › Conteúdo › Geração** (`frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`).
> - Planejamento e geração de imagens do framework em `useFrameworkImageStatuses`/`useGenerateFrameworkImages`.
> - Seleção/aprovação do fluxo do portal em `frontend/src/pages/experiment/LeadPortalFlowTab.tsx`.
> - Service de deploy automático do formulário via `ExperimentLeadPortalFlowScheduler` (AI Worker).

Consulta ao MySQL confirmou o contexto do experimento monitorado:

```
SELECT id, name, status, stage, lead_portal_flow_id FROM experiment WHERE id = 10;
→ id=10, name="Experimento 4", stage="AD", lead_portal_flow_id=20
SELECT id, name, slug, approved FROM lead_portal_flow WHERE id = 20;
→ slug="exp-10-landing", approved=1
```

Os logs em produção reforçam que o fluxo `exp-10-landing` está ativo (`FlowEngagementController` registrando `render-complete`) e que o worker de landing roda a cada 10 minutos (`ExperimentLeadPortalFlowScheduler iniciado/finalizado`).

## Resumo rápido da ordem perfeita

| Ordem | Ação | Onde executar | Depende de | Saída esperada |
| --- | --- | --- | --- | --- |
| 1 | **Solicitar o Texto da Landing** (`LANDING_PAGE_COPY`) | Experimento › Conteúdo › aba Texto da Landing | Ângulo + Copy do anúncio aprovados | Hero, corpo e CTA consistentes | 
| 2 | **Solicitar o Layout da Landing** (`LANDING_PAGE_WIREFRAME`) | Mesma tela, aba Layout | Texto da landing concluído | Hierarquia mobile-first com IDs/slots | 
| 3 | **Solicitar o Planejamento de Imagens** (`LANDING_PAGE_IMAGE_PLANNING`) | aba Planejamento de Imagens | Layout aprovado | Lista `images[]` com hero/prova/CTA e prompts | 
| 4 | **Gerar as imagens** do framework | Painel "Geração das imagens planejadas" (mesma tela) | Planejamento aprovado | Status `WEB_READY` com `webUrl` por item | 
| 5 | **Solicitar o HTML da Landing** (`LANDING_PAGE_HTML`) e revisar | aba HTML da Landing | Steps 1–4 concluídos | HTML final com imagens reais (sem placeholder) | 
| 6 | **Aplicar HTML ao formulário** | Botão "Usar como formulário do experimento" | HTML validado | `customFormHtml` atualizado no fluxo vinculado | 
| 7 | **Selecionar e aprovar o formulário** | Experimento › aba Portal do Lead | HTML aplicado | `lead_portal_flow_id` apontando para o slug correto + `approved=true` |

## Passo a passo detalhado

### 1. Texto da Landing
1. Acesse **Experimentos › [experimento] › Conteúdo › Geração**.
2. Selecione "Texto da Landing" e clique em **Acionar Worker IA**.
3. Aguarde o status `COMPLETED`. O worker valida CTA, hero e `bodySections` (ver regras em `docs/experiment-pipeline-validation-spec.md`).
4. Se precisar regenerar, use "Invalidar" antes de reenviar para garantir que o layout receba a versão atual.

### 2. Layout da Landing
1. Com o texto sincronizado, vá para "Layout da Landing".
2. Reforce nas instruções qualquer restrição de hero/form para o nicho.
3. Só avance quando a visualização mostrar `sectionOrder[]` contendo hero, formulário e CTA com `mobilePriorityScore` > 7 para o topo.

### 3. Planejamento de Imagens
1. A aba "Planejamento de Imagens" consome os artefatos anteriores (Copy + Layout).
2. Dispare o worker e valide se o painel mostra ao menos 4 itens com `placement` (`hero`, `proof`, `cta`, etc.), `imagePrompt`, `altText` e dimensões.
3. Ajuste o prompt manual (campo de instruções) quando o hero precisar de elementos específicos (ex.: preço visível, mockup do produto). Isso evita que o HTML renda placeholders.

### 4. Geração das imagens
1. No painel "Geração das imagens planejadas", clique em **Gerar imagens**.
2. Monitore a tabela de status. Aguarde todos os itens chegarem em `WEB_READY` (coluna "Web" preenchida).
3. *Somente* quando todos os heróis/CTA estiverem com `webUrl` o HTML poderá substituir os placeholders automaticamente (`mergeGeneratedImagesIntoLandingHtml`).

### 5. HTML da Landing
1. Solicite o HTML na aba correspondente.
2. Assim que o iframe carregar, confirme que nenhum `<img>` continua apontando para `placehold.co`. O botão "Ver HTML" ajuda a buscar por `placehold`.
3. Use o diff do preview (lado esquerdo `HTML`, lado direito `Visão final: landing + imagens`). Esse bloco já injeta as URLs reais vindas dos itens `WEB_READY`.

### 6. Aplicar ao formulário do experimento
1. Ainda no preview, clique em **Usar como formulário do experimento**. A chamada `POST /api/experiments/:id/pipeline/landing-page-html/apply-to-form` atualiza o `customFormHtml` do fluxo vinculado.
2. Volte para o card "Portal do Lead" da página do experimento e confirme que o slug correto aparece em "Fluxo atribuído".

### 7. Selecionar e aprovar o formulário
1. Na aba **Portal do Lead**, use o seletor para vincular o fluxo desejado ao experimento (campo `lead_portal_flow_id`).
2. Ative a aprovação com o toggle **Aprovar/Revogar** (chamada `PATCH /api/lead-portal-flows/:id/approval`).
3. Se já estava aprovado e o HTML foi substituído, clique em **Revogar** e depois **Aprovar** para propagar o novo artefato.
4. Esta etapa dispara automaticamente o deploy para `https://oportunidadebrasil.shop/flows/{slug}`; não há passos manuais adicionais.

## Evitando o placeholder do hero
- **Causa raiz:** HTML gerado antes das imagens chegarem em `WEB_READY`. O worker injeta `https://placehold.co/...` apenas para manter layout válido.
- **Contramedida operacional:** Nunca acione o HTML final sem ter verificado o painel "Geração das imagens". Aplique o HTML somente após conferir que os campos "Origem" ou "Web" exibem URLs reais.
- **Checagem rápida:** No preview, use o atalho do navegador (Ctrl/Cmd+F) e procure por `placehold`. Resultado vazio = pronto para publicar.

## Monitoramento e deploy automático
1. **Logs do Portal do Lead** (`https://oportunidadebrasil.shop/api/ops-lp-observability-v2/logfile`): confirme entradas `Render-complete` para o slug após aplicar/aprovar.
2. **Logs do Worker AI** (`http://191.252.120.96:4567/worker-observability/logfile`): monitore o ciclo do `ExperimentLeadPortalFlowScheduler` e o status das imagens (`WEB_READY`).
3. **Banco** (`lead_portal_flow.approved`): use consultas rápidas para confirmar que o flag mudou após a aprovação.
4. Não faça ajustes manuais no servidor. O deploy do formulário ocorre automaticamente quando: (a) o HTML é aplicado; (b) o fluxo está associado ao experimento; (c) o fluxo está aprovado. O worker copia os assets para o CDN e publica o slug sem ação humana.

## Checklist final antes de liberar tráfego
- [ ] `LANDING_PAGE_COPY`, `LANDING_PAGE_WIREFRAME`, `LANDING_PAGE_IMAGE_PLANNING` e `LANDING_PAGE_HTML` em `COMPLETED` no histórico do pipeline.
- [ ] Painel de imagens sem registros `PENDING/FAILED` e todas com `webUrl` válido.
- [ ] HTML aplicado ao experimento (`Última aplicação` exibida no card do preview).
- [ ] Fluxo correto selecionado no experimento e aprovado novamente após ajustes.
- [ ] Acesso público (`/flows/{slug}`) renderizando hero real (sem `placehold`).
- [ ] Logs do Portal confirmando `render-complete` após a última aprovação.

Seguindo esta ordem, cada etapa alimenta a próxima com a versão mais recente dos artefatos, eliminando inconsistências visuais e mantendo o deploy 100% automatizado.