# Guia de gerenciamento dos estilos do formulário simples do Lead Portal

Este guia explica, em linguagem operacional, como criar e manter os estilos visuais reutilizáveis aplicados aos formulários simples do Lead Portal. Toda a interface descrita abaixo está disponível no Marketing Hub em **Campanhas › Estilos do formulário simples** (`/lead-portal/simple-form-styles`).

## 1. Por que os estilos são importantes?

* Cada fluxo simples do Lead Portal precisa de um estilo para definir paleta, botões e hero da página pública (`/flows/:slug`).
* O estilo é enviado junto com o fluxo no payload de publicação (`LeadPortalFlowPublicationRequest`) e passa a controlar as variáveis CSS consumidas pelo site público (`FlowPage.tsx` + `styles.css`).
* Sem pelo menos um estilo cadastrado não é possível gerar novos formulários no card **“Criar formulário simples (sem imagem)”** presente na página do nicho.

## 2. Pré-requisitos e acesso

1. Entrar no Marketing Hub com um usuário que possua acesso ao menu **Campanhas**.
2. Abrir **Campanhas › Estilos do formulário simples**. A tela é dividida em:
   * **Coluna esquerda (formulário):** criação/edição de estilos.
   * **Coluna direita (cards):** listagem dos estilos existentes, com prévia de cor, slug, data da última atualização e botão **Editar estilo**.
3. O botão **Limpar formulário / Cancelar edição** (canto superior direito) restaura o formulário para o estado vazio (`EMPTY_STATE`).

## 3. Estrutura do formulário de cadastro

### 3.1 Identificação do estilo

| Campo | Obrigatório? | O que faz |
| --- | --- | --- |
| **Nome** | Sim | Nome amigável mostrado no selector de estilos (ex.: “Neon Fitness 2024”).
| **Slug** | Sim | Identificador usado nas APIs e enviado para o Lead Portal. Deve conter apenas minúsculas, números e hífens (`^[a-z0-9]+(?:-[a-z0-9]+)*$`). O campo aplica `toSlug()` automaticamente enquanto você digita.
| **Descrição** | Opcional | Texto curto para lembrar onde o estilo será usado.

### 3.2 Tokens visuais

Os campos abaixo populam as variáveis de estilo aplicadas em `FlowPage.tsx` (`buildStyleVariables`). Use códigos hexadecimais, `rgb()/rgba()` ou gradientes CSS válidos.

| Campo na UI | Variável CSS aplicada | Onde aparece |
| --- | --- | --- |
| **Background principal** | `--flow-background` | Plano de fundo da página inteira. Aceita gradientes (`linear-gradient(135deg, #eef2ff, #fdf2f8)`) ou uma cor sólida.
| **Cor do cartão** | `--flow-card-background` | Cartões que envolvem perguntas e cards de confirmação.
| **Cor do texto** | `--flow-text-color` | Títulos das perguntas, labels e valores principais.
| **Texto auxiliar** | `--flow-muted-text-color` | Descrições e mensagens secundárias.
| **Cor principal** | `--flow-primary-color` | Destaques, bordas de foco e gradiente padrão do botão.
| **Cor de destaque** | `--flow-accent-color` | Usada em etiquetas auxiliares e chips.
| **Botão (fundo)** | `--flow-button-background` | Gradiente do CTA “Enviar”.
| **Botão (texto)** | `--flow-button-text` | Cor da tipografia do CTA.
| **Imagem destaque (URL)** | `heroImageUrl` | Arte exibida no hero da página pública. Use URLs HTTPS acessíveis.
| **Layout do hero** | `data-hero-layout` | Define a posição do bloco de imagem (`image-right`, `image-left` ou `stacked`).

> Observação: outros tokens (`cardBorderColor`, `buttonShadow`, `inputBackground`, etc.) continuam com os valores definidos no tema padrão (`styles.css`). Caso precise alterá-los, abra um ticket para a equipe de plataforma ou ajuste via API.

### 3.3 Conteúdo e parâmetros para automação

Esses campos não mudam o CSS diretamente, mas documentam as referências criativas usadas em experimentos e integrações.

| Campo | Uso típico |
| --- | --- |
| **Prompt textual** | Briefing aplicado aos modelos de texto (`textModel`).
| **Prompt de imagens** | Diretriz visual aplicada ao modelo de imagem (`imageModel`).
| **Modelo de texto / Modelo de imagem** | Nome do modelo (ex.: `gpt-4o-mini`, `gpt-image-1.5`).
| **Batch de imagens** | Quantidade padrão de variações que o worker deve solicitar (validado para ser > 0).
| **Aspect ratio** | Segue o padrão `1:1`, `3:4`, etc., para os renders.
| **Imagem de prévia (URL)** | URL usada em integrações externas que consomem o payload de publicação (fica disponível em `simpleFormStyle.previewImageUrl`).

## 4. Criar um novo estilo

1. Preencha **Nome** e **Slug** (os únicos campos obrigatórios). Sem eles o formulário mostra um alerta vermelho (`feedback.variant === "error"`).
2. Configure a paleta visual com base no moodboard desejado. Dica: copie um gradiente direto do Figma usando `linear-gradient`.
3. Informe o hero (URL + layout). Caso não tenha imagem ainda, deixe o campo vazio — o portal omite o bloco visual automaticamente.
4. (Opcional) Documente prompts, modelos e parâmetros para manter o histórico do estilo.
5. Clique em **Criar estilo**. Em caso de sucesso o sistema exibe um alerta verde (“Estilo criado com sucesso.”) e limpa o formulário. Em caso de erro (slug duplicado, payload inválido, etc.), o backend responde com `422/400` e a mensagem é mostrada no alerta vermelho.
6. O novo estilo aparece imediatamente na grade da direita porque o React Query invalida `lead-portal-simple-form-styles` após o POST.

## 5. Editar um estilo existente

1. Localize o card desejado na lista e clique em **Editar estilo**.
2. O formulário carrega os dados atuais (`mapStyleToState`) e o topo da tela passa a mostrar o botão **Cancelar edição**.
3. Faça os ajustes e clique em **Atualizar estilo**. O card será atualizado com a nova data/hora (`style.updatedAt`).
4. Para desistir das alterações use **Cancelar edição**, que restaura o formulário para o estado vazio.

## 6. Usar o estilo ao criar formulários simples

1. Acesse a página do nicho (**Nicho › Formulários simples do nicho**).
2. No card **Criar formulário simples (sem imagem)** escolha o estilo no seletor **“Estilo visual do formulário”**. A lista é preenchida com todos os estilos cadastrados (hook `useLeadPortalSimpleFormStyles`).
3. Caso não exista nenhum estilo, o card mostra a mensagem *“Cadastre um estilo em Campanhas > Estilos do formulário simples antes de gerar novos fluxos.”*
4. Finalize o cadastro do fluxo simples. O `simpleFormStyleId` selecionado é enviado ao backend; quando o fluxo for publicado, o portal público renderizará o formulário com o tema escolhido.

## 7. Validar o resultado no portal público

1. Depois de publicar/aprovar o fluxo, abra **Experimentos › aba Lead Portal** e encontre o fluxo na lista.
2. Use o link **URL pública** mostrado abaixo do slug ou acesse diretamente `https://<domínio-do-portal>/flows/<slug-do-fluxo>`.
3. Confirme:
   * Gradiente de fundo, cartões e botões com as cores definidas.
   * Hero exibido com o layout selecionado (`data-hero-layout` controla os estilos responsivos em `styles.css`).
   * Mensagens e labels usando `--flow-text-color` / `--flow-muted-text-color`.
4. Se necessário, ajuste o estilo e salve novamente. O portal consome sempre a última versão enviada.

## 8. Boas práticas e solução de problemas

| Sintoma | Causa provável | Como resolver |
| --- | --- | --- |
| Erro “Informe o nome do estilo / Defina um slug válido.” | Validação básica do formulário. | Preencha os campos obrigatórios antes de salvar. |
| Erro de API indicando slug duplicado | O slug já existe (checado em `LeadPortalSimpleFormStyleService.ensureUniqueSlug`). | Escolha outro slug ou renomeie o estilo antigo. |
| Hero não aparece | URL vazia ou arquivo sem HTTPS/cabeçalhos CORS. | Use links públicos (S3, CDN, etc.) e teste no navegador separado. |
| Botão perde contraste | Cores iguais em **Botão (fundo)** e **Botão (texto)**. | Ajuste para garantir contraste AA/AAA. |
| Formulário do nicho não lista estilos | Nenhum estilo cadastrado ou erro na API `/api/lead-portal/simple-form-styles`. | Cadastre um estilo e recarregue a página; se persistir, verifique logs do backend `ads-service`. |
| Mudanças não aparecem no portal | Cache do browser ou fluxo ainda não republicado. | Atualize a página pública (Ctrl+F5) ou publique novamente o fluxo para forçar o envio do novo payload. |

### Dicas adicionais

* Prefira cores `rgba()` com alpha para criar luzes e sombras suaves (ex.: `rgba(99,102,241,0.08)` para realces).
* Planeje o hero para proporções retangulares (ex.: 4:3) — o CSS define `background-size: cover`, então imagens quadradas podem cortar conteúdo.
* Use o campo **Imagem de prévia** para guardar a thumb oficial do estilo; ela é enviada junto no payload e pode ser reutilizada em materiais externos.
* Multiplique estilos para testes A/B: crie variações leve (mudando apenas gradiente e hero) e aplique-as em fluxos clones para comparar métricas na página de experimentos.

Com essas etapas você consegue manter um catálogo organizado de estilos e garantir que todos os formulários simples do Lead Portal sigam o mesmo padrão visual definido para cada campanha ou nicho.
