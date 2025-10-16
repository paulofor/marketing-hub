# Campos necessários para criar uma campanha no Meta Ads

Este documento descreve todos os campos que precisam ser coletados e preenchidos para configurar uma campanha completa no Meta Ads (Facebook e Instagram). A estrutura segue os três níveis usados pela plataforma — **Campanha**, **Conjunto de Anúncios** e **Anúncio** — e explica o objetivo de cada campo, além de observações úteis para preenchimento. Os valores padrão aplicados pelo `facebook-ads-worker` são parametrizados na tela **Contas do Facebook** e consumidos pelo endpoint `GET /api/accounts/facebook/worker-config`.

> Atualização 2025-05-20: os experimentos agora dependem de uma página associada
> quando a conta não informa `defaultPageId`. Revise os campos de campanha e
> criativo considerando a nova obrigatoriedade.

> Atualização 2025-08-22: os experimentos também precisam informar
> `instagramAccount`. Sem essa conta o worker ignora o experimento para evitar
> criativos sem `instagram_user_id`.

## 1. Configurações no nível de Campanha

### Nome da campanha
Identificador interno usado para organização. Deve seguir um padrão acordado (ex.: objetivo_público_periodo) para facilitar relatórios.

### Objetivo da campanha
Selecione o objetivo principal (Reconhecimento, Tráfego, Engajamento, Leads, App Promotion ou Vendas). O objetivo define quais eventos serão otimizados e quais formatos ficam disponíveis.

### Tipo de compra
Escolha entre **Leilão** (mais flexível) ou **Alcance e frequência** (planejamento fixo). A maioria das integrações usa Leilão.

### Categoria de anúncio especial
Indique se a campanha se enquadra em Habitação, Emprego, Crédito ou Política. Campos adicionais de segmentação ficam limitados quando esta opção está ativa.

### Limite de gasto da campanha (Campaign Spending Limit)
Valor máximo absoluto que a campanha pode gastar ao longo de sua vida útil. Use para controlar orçamentos compartilhados entre conjuntos de anúncios.

### Orçamento da campanha (Campaign Budget Optimization - CBO)
Defina se o orçamento será controlado no nível da campanha. Informe o valor **diário** ou **vitalício**. Se CBO não for utilizado, o orçamento fica por conjunto de anúncios.

### Teste A/B
Opcional. Informe se a campanha fará parte de um experimento A/B e qual variante representa (ex.: Criativo A). Útil para metodologias de teste estruturadas.

### Status inicial
Define se a campanha será criada como **Ativa**, **Em rascunho** ou **Pausada**. Útil para processos que exigem revisão antes da publicação.

### Rastreabilidade com planejamento
Ao automatizar a criação pelo `facebook-ads-worker`, registre o identificador do
experimento aprovado e da conta de Facebook utilizada. Esses valores alimentam
as colunas `facebook_ads_campaign.experiment_id` e
`facebook_ads_campaign.facebook_account_id`, garantindo auditoria de origem.

## 2. Configurações no nível de Conjunto de Anúncios

### Nome do conjunto de anúncios
Identificador interno. Sugere-se padronizar com público + estratégia (ex.: retargeting_30d_cpc).

### Evento de conversão / Meta de otimização
Informe o pixel ou API de conversão e o evento (ex.: Purchase, Lead) ou objetivo equivalente (cliques, alcance). É obrigatório para campanhas de conversão.

### Estratégia de otimização e entrega
Defina como o Meta deve otimizar a entrega: **Conversões**, **Cliques no link**, **Visualizações de página**, **Impressões**, etc. Combine com a janela de atribuição desejada.

### Estratégia de lance
Escolha entre **Custo mais baixo (Lowest Cost)**, **Limite de lance (Bid Cap)**, **Limite de custo (Cost Cap)** ou **Retorno mínimo (Minimum ROAS)**. Informe o valor monetário quando aplicável.

### Orçamento (quando não usa CBO)
Defina valor **diário** ou **vitalício** específico para o conjunto de anúncios. Necessário quando a campanha não utiliza CBO ou quando deseja controle granular.

### Período de veiculação
Informe data e hora de início e término. Para orçamentos vitalícios é possível definir programação por horário (dayparting).

### Públicos personalizados
Liste os públicos personalizados incluídos (site visitors, listas CRM) e excluídos. Informe os identificadores usados na Business Suite.

### Públicos semelhantes (Lookalikes)
Caso use, informe o público de origem, país e porcentagem de semelhança. Permite expandir o alcance com base em dados existentes.

### Segmentação detalhada
Defina interesses, comportamentos ou dados demográficos adicionais. Documente se o recurso Advantage Detailed Targeting está habilitado para permitir expansão automática.

### Localizações
Defina países, estados, cidades ou raio. Especifique se inclui ou exclui determinadas áreas e o tipo de localização (residente, visitante, viajando).

### Faixa etária
Informe idade mínima e máxima dos usuários (13-65+). Alguns objetivos limitam essa configuração.

### Gênero
Selecione **Todos**, **Masculino** ou **Feminino**, conforme a estratégia.

### Idiomas
Especifique idiomas necessários quando o criativo não é adequado para todos os idiomas suportados automaticamente.

### Posicionamentos
Escolha entre **Automáticos (Advantage+ Placements)** ou **Manuais**. Para manuais, liste cada posicionamento aprovado (Feed, Stories, Reels, Audience Network etc.).

### Dispositivos e sistemas operacionais
Opcional. Defina se o conjunto de anúncios atende apenas Mobile, Desktop, iOS, Android ou versões específicas (relevante para apps).

### Limite de frequência
Opcional. Configure número máximo de impressões por pessoa em determinado período.

### Janela de atribuição
Selecione a janela (ex.: 7d clique / 1d visualização). Impacta relatórios e otimização.

### Rastreadores externos
Informe se haverá integração com parceiros (ex.: Appsflyer, Google Analytics) e quais parâmetros devem ser enviados.

## 3. Configurações no nível de Anúncio

### Nome do anúncio
Identificador interno. Sugestão: formato + ângulo criativo (ex.: video_tutorial_promocao).

### Identidade
Selecione a Página do Facebook e a conta do Instagram que representarão o anúncio. Necessário ter permissões válidas.

### Formato do anúncio
Defina o tipo de criativo: Imagem única, Vídeo, Carrossel, Coleção, Stories, Reels, Advantage+ Creative etc. Alguns formatos exigem proporções específicas.

### Mídia principal
Forneça o arquivo de imagem ou vídeo (URL ou ID do Creative Hub). Inclua especificações: dimensão, proporção, duração máxima, tamanho de arquivo.

### Mídias adicionais (quando carrossel ou coleção)
Liste cada cartão com título, descrição, URL e mídia correspondente. Defina a ordem de exibição.

### Texto primário (Primary Text)
Copy principal exibida acima da mídia. Indique variações se utilizar otimização de texto dinâmico.

### Título (Headline)
Texto em destaque abaixo da mídia (em feeds). Informe versões alternativas se habilitar otimização dinâmica.

### Descrição
Texto complementar opcional (aparece em alguns posicionamentos). Use para benefícios adicionais.

### URL de destino
Link final que o usuário acessará. Deve incluir protocolo (`https://`). Garanta compatibilidade com a política de domínio verificado.

Quando o experimento ou a conta configurada também informam um **formulário de leads** (Lead Ads/Instant Form), o worker passa a tratar o link como opcional: se existir `lead_gen_form_id`, o CTA abrirá o formulário dentro do Facebook/Instagram mesmo que não haja URL.

### Formulário de leads (Lead Ads)
Informe o identificador do formulário publicado no Gerenciador de Anúncios. O campo é aceito tanto nos criativos aprovados quanto como fallback em **Contas do Facebook** (`defaultLeadGenFormId`).

- Se apenas o formulário estiver preenchido, o worker ajusta automaticamente `destination_type=ON_AD` e `optimization_goal=LEAD_GENERATION` no conjunto de anúncios.
- Caso URL e formulário estejam presentes, o CTA prioriza o formulário e mantém o link como destino secundário.

### Display Link
Versão amigável do link exibida no anúncio. Normalmente o domínio raiz.

### Chamada para ação (CTA)
Selecione o botão apropriado (Saiba mais, Comprar agora, Fale conosco etc.). Deve alinhar com o objetivo da campanha. Consulte [call-to-action-types.md](call-to-action-types.md) para a lista completa suportada pela Graph API e replicada no frontend/worker.

### Parâmetros UTM / Template de rastreamento
Inclua UTMs ou parâmetros dinâmicos (ex.: `utm_source=facebook&utm_campaign={{campaign.name}}`). Importante para mensuração em ferramentas externas.

### Pixel e eventos associados
Confirme que o anúncio utiliza o mesmo pixel/API definido no conjunto de anúncios e, se necessário, adicione eventos personalizados.

### Deep link / App link
Para campanhas de app, informe o deep link ou link universal correspondente ao sistema operacional do usuário.

### Texto alternativo (alt text)
Opcional, porém recomendado para acessibilidade. Descreva a mídia para leitores de tela.

### Rastreamento de chamadas
Quando usar anúncios com ligações, forneça o número de telefone e configure o recurso de rastreamento disponível.

## 4. Documentação complementar

- **Regras de revisão e políticas**: registre checklists de compliance (texto, imagens, reivindicações, restrições de público).
- **Responsáveis e aprovações**: identifique quem aprova cada etapa (campanha, orçamento, criativos).
- **Checklist de assets**: mantenha inventário dos arquivos enviados (nomes, local de armazenamento, versões).
- **Histórico de versões**: registre alterações relevantes em objetivos, públicos ou criativos para aprendizado futuro.

## 5. Fluxo recomendado de coleta de dados

1. **Briefing inicial** com objetivo de negócio, público-alvo e oferta.
2. **Definição de orçamento e calendário** (campanha ou conjuntos).
3. **Construção ou seleção de públicos** (customizados, similares, detalhados).
4. **Produção criativa** com mensagens aprovadas e formatos compatíveis.
5. **Configuração técnica** (pixel, eventos, UTMs, parceiros de mensuração).
6. **Revisão de conformidade** e ativação.

Ter todos os campos acima documentados reduz retrabalho, garante consistência entre campanhas e facilita a automação via `facebook-ads-worker`.
