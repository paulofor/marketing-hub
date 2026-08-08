# Matriz de homologação — versionamento de hipótese v1

## Objetivo

Validar a criação auditável de uma hipótese comercial corrigida sem alterar a origem nem liberar experimento, publicação ou mídia.

## Matriz ponta a ponta

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Abrir hipótese e criar nova versão com contrato completo | Nova hipótese em `BACKLOG`, origem preservada e redirecionamento para o detalhe |
| Linhagem | Versionar uma hipótese original ou já versionada | `source_hypothesis_id`, `root_hypothesis_id` e `version_number` coerentes |
| Segregação | Criar versão | Produto e nicho são copiados da origem e não podem ser trocados pela tela |
| Contrato | Informar Agenda Cheia por R$ 67 | Entrega e preço corrigidos ficam somente na nova versão |
| Validação | Omitir problema, persona, entrega ou preço positivo | Backend responde 400 e não persiste versão parcial |
| Falha | Origem inexistente | Backend responde 404 e nada é criado |
| Auditoria | Consultar versão criada | DTO expõe origem, raiz e número da versão |
| Segurança comercial | Criar versão | Nenhum experimento, campanha, publicação ou gasto é criado |
| Observabilidade | Falha de validação | Mensagem funcional permite identificar o campo inválido |
| Métricas | Após deploy | Eventos do experimento só serão homologados depois de o usuário criar um experimento separado |
| Dados de teste | Testes locais | Usar nicho, produto e hipótese efêmeros em H2; não misturar eventos operacionais |
| Navegadores | Desktop Chromium | Formulário, validações e navegação funcionam com teclado e mouse |
| Dispositivos | iPhone 15 Pro e Pixel 7 | Campos e ações permanecem utilizáveis em viewport móvel |

## Critérios de decisão

- Continuar: origem intacta, versão em `BACKLOG`, vínculo comercial preservado e testes locais aprovados.
- Ajustar: qualquer campo clonado incorretamente ou falha de usabilidade responsiva.
- Parar: sobrescrita da origem, troca de produto/nicho ou criação automática de experimento, mídia ou gasto.
