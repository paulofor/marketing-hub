# Identificador do aplicativo do Facebook

Este guia explica como localizar o campo obrigatório **ID do aplicativo (App ID)** na tela **Contas do Facebook**.

## ID do aplicativo (App ID)

O App ID é o identificador público do aplicativo registrado no [Facebook for Developers](https://developers.facebook.com/).

1. Acesse `https://developers.facebook.com/apps/` e selecione o aplicativo utilizado para gerar tokens.
2. No painel **Configurações → Básico**, copie o valor exibido logo abaixo do nome do aplicativo. O mesmo número aparece no cabeçalho da página, ao lado do título "ID do aplicativo" — é o código destacado no print `786514...` do exemplo acima.
3. Preencha esse valor no campo **ID do aplicativo (App ID)** da tela do Marketing Hub.

> O App ID também é usado em chamadas à Graph API, Login com Facebook e na renovação de tokens pelo worker.
>
> Atualização 2025-05-20: experimento sem `defaultPageId` agora exige uma página
> associada no backend. Certifique-se de que o aplicativo selecionado tenha
> acesso à página vinculada ao experimento antes de atualizar as credenciais.

## Checklist rápido

| Campo na UI | Onde localizar | Observações |
|-------------|----------------|-------------|
| **ID do aplicativo (App ID)** | Painel do app em developers.facebook.com → Configurações → Básico | Mesmo valor exibido no cabeçalho do portal de desenvolvedores. |
| **Formulário de leads padrão (opcional)** | Gerenciador de Anúncios → Biblioteca de formulários | Copie o `ID` mostrado na tabela de formulários caso deseje habilitar Lead Ads automáticos. |

Confirme que o número informado corresponde ao aplicativo autorizado a gerar tokens para a conta do Business Manager. Isso evita erros de autorização durante o disparo de campanhas e a renovação automática de tokens. Quando utilizar formulários instantâneos, aproveite para registrar também o novo campo **Formulário de leads padrão**.
