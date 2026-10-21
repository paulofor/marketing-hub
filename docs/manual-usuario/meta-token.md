# Atualizar o token da Meta

1. Abra <a href="http://191.252.181.168:5173/accounts/facebook" target="_blank" rel="noopener noreferrer">Contas do Facebook</a> e clique em **Atualizar token** na conta desejada.
2. No <a href="https://developers.facebook.com/tools/explorer/" target="_blank" rel="noopener noreferrer">Graph API Explorer</a>, use o aplicativo indicado na tela e autentique o titular cadastrado. Gere um **token de usuário**, com as permissões `ads_read` e `ads_management`.
3. Cole somente o token em **Novo token de usuário** e clique em **Validar e salvar token**.
4. Aguarde a confirmação. O sistema confere o acesso antes de substituir a credencial anterior e mantém a renovação automática conforme a configuração da conta.

O campo é mascarado e fica vazio após o envio. Se a Meta recusar o acesso, a credencial anterior permanece. Se houver perda de conexão, atualize a tela e confira o estado antes de reenviar. Não publique o token em chat, comentários ou anexos.

A recuperação não inicia campanhas nem altera orçamento. O painel existente utiliza HTTP; o mascaramento não substitui a criptografia do transporte por HTTPS.
