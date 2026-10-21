# Atualização do token Meta pelo painel — 21/10/2026

## Evidência e escolha

A tela `/accounts/facebook` não tinha campo de envio de token. A conta 1 mantém
renovação automática habilitada, mas a credencial venceu em 19/10 e a última tentativa
falhou. O histórico está em `meta-renovacao-automatica-v1.md` e no
`LOOP-META-RENOVACAO-BLOQUEADA-POR-FILA`. Não existe renovação de token expirado.
Uma resposta HTTP 200 de `debug_token` também era apresentada como sucesso mesmo com
`is_valid=false`; o screenshot do usuário demonstrou essa contradição.

| Alternativa | Benefício | Custo/risco | Decisão |
| --- | --- | --- | --- |
| Campo no formulário geral | Menos código | Salva também orçamento/defaults e não valida identidade | Não adotada |
| Comando dedicado com validação anterior à troca | Recuperação simples, sem alterar campanha | Duas consultas de leitura à Meta | Adotada |
| Login OAuth completo | Evita copiar token | Configuração de callback, permissões e produto Meta adicionais | Fora deste pedido |

## Matriz definida antes da execução

| Cobertura | Critério |
| --- | --- |
| Caminho feliz | Tela envia a conta selecionada; controller valida Meta e persiste; listagem confirma presença sem segredo |
| Validações | Campo vazio/malformado, expirado, app/titular/tipo diferentes, escopo ausente e conta de anúncios errada preservam credencial |
| Falhas | 400/401/429/503, resposta incompleta e falha de conexão não provocam retry automático nem substituição |
| Concorrência | Configuração ou credencial alterada durante a consulta impede sobrescrita |
| Sigilo | Campo mascarado e limpo; sem token em DTO de resposta, cache de mutação, erro ou log |
| Integração | Banco local e Graph simulada; somente leituras externas; worker e renovação mantidos |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; foco, cancelamento, envio, erro e confirmação |
| Observabilidade | Conta, horário, resultado e validade auditáveis; nenhum token ou segredo no log |
| Métricas | Contar aceites/recusas locais; redução do tempo parado é hipótese operacional, não resultado comercial |
| Dados | Credenciais sintéticas e banco isolado; nada de campanhas ou dados de teste na produção |

## Limites

A substituição não inicia campanha, não muda orçamento e não decide gates de Hermes.
Somente o titular gera a nova credencial. O endpoint não cria login OAuth.
A interface administrativa atual usa HTTP; mascaramento e ausência de segredo nos
retornos não equivalem a criptografia do transporte. TLS do painel deve ser provido
pelo ingresso do ambiente; esta alteração preserva a topologia existente.

## Aprendizado reutilizável

Capacidade: diagnóstico e recuperação de credencial consumida por Hermes.
Antes: dependência de arquivo no servidor e diagnóstico contraditório.
Depois: comando canônico valida evidências antes da escrita, com proteção contra
alteração concorrente e preservação de automação. Critério de adoção: matriz aprovada;
reversão: retirar o botão se houver falha de sigilo ou alteração de configuração não
solicitada. Aplicável a outras contas Meta, sem exceções por produto ou identificador.

## Resultado

Validação local aprovada em 21/10/2026:

- 144 testes Java: 24 do fluxo de substituição com banco H2 e Graph simulada, 5 do
  diagnóstico, 15 de administração de contas, 8 da renovação já existente e 92 de arquitetura.
- 8 testes do formulário, typecheck e build de produção aprovados.
- Navegação Playwright em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados:
  cancelamento, foco, mascaramento, recusa, reenvio explícito, sucesso, atualização
  da listagem e ausência de token em storage/retorno visível.
- Scripts de formatação verificados por `bash -n` e ShellCheck; Spotless/Prettier
  aplicados aos arquivos alterados e diff revisado.

Os testes nunca usam a Meta real nem modificam a conta produtiva. A conferência da
nova credencial real depende do envio pelo titular após a publicação. A limitação
HTTP do painel foi informada; nenhuma alegação de transporte criptografado é feita.
PR, SHA e workflows são vinculados no registro público da entrega.
