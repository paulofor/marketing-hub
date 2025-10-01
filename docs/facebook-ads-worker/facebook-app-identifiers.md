# Identificadores de aplicativos do Facebook

Este guia explica como localizar os dois campos obrigatórios na tela **Contas do Facebook**: **ID do aplicativo (App ID)** e **ID do aplicativo vinculado ao Business Manager**.

## 1. ID do aplicativo (App ID)

O App ID é o identificador público do aplicativo registrado no [Facebook for Developers](https://developers.facebook.com/).

1. Acesse `https://developers.facebook.com/apps/` e selecione o aplicativo utilizado para gerar tokens.
2. No painel **Configurações → Básico**, copie o valor exibido logo abaixo do nome do aplicativo. O mesmo número aparece no cabeçalho da página, ao lado do título "ID do aplicativo" — é o código destacado no print `786514...` do exemplo acima.
3. Preencha esse valor no campo **ID do aplicativo (App ID)** da tela do Marketing Hub.

> O App ID também é usado em chamadas à Graph API, Login com Facebook e na renovação de tokens pelo worker.

## 2. ID do aplicativo vinculado ao Business Manager

Esse campo registra o identificador do aplicativo **dentro do Business Manager** responsável pelos ativos da conta.

1. Entre em `https://business.facebook.com/` com o perfil que administra o ativo.
2. No menu lateral, abra **Configurações do negócio → Contas → Aplicativos**.
3. Selecione o aplicativo desejado. O painel de detalhes exibe o campo **ID do aplicativo do Business Manager** (ou apenas **ID** quando a interface estiver em português).
4. Copie esse número e preencha o campo **ID do aplicativo vinculado ao Business Manager** no Marketing Hub.

> Esse identificador é diferente do App ID público. Ele comprova que o aplicativo está vinculado ao Business Manager e facilita auditorias internas.

## 3. Checklist rápido

| Campo na UI | Onde localizar | Observações |
|-------------|----------------|-------------|
| **ID do aplicativo (App ID)** | Painel do app em developers.facebook.com → Configurações → Básico | Mesmo valor exibido no cabeçalho do portal de desenvolvedores. |
| **ID do aplicativo vinculado ao Business Manager** | business.facebook.com → Configurações do negócio → Contas → Aplicativos | Disponível somente para usuários com acesso ao BM. |

Sempre valide que ambos os IDs pertencem ao mesmo aplicativo antes de salvar a conta. Isso evita erros de autorização durante o disparo de campanhas e a renovação automática de tokens.
