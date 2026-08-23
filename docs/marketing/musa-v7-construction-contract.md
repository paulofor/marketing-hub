# Contrato de construção — MUSA v7 orientado ao desejo

## Decisão do produto

O MUSA v7 é uma experiência digital guiada de sete dias. Vídeos são apoio opcional e não constituem
a entrega central. A cliente compra acesso por pagamento único de **R$ 67**, sem renovação automática,
e pode consultar a experiência e os materiais durante 90 dias.

Foram comparadas três materializações:

| Alternativa | Benefício | Risco | Esforço | Aderência comercial |
| --- | --- | --- | --- | --- |
| Aprovar a v7 atual baseada em vídeo e IA | Reaproveita ativos | Custo sem ledger, promessa confusa e dados livres | Baixo | Baixa |
| Reconstruir como serviço de vídeos personalizados | Alto valor percebido potencial | Contradiz R$ 67 e a margem aprovada | Alto | Baixa |
| Evoluir a plataforma PDE guiada | Preserva sete dias, progresso e ativos existentes | Exige fechar acesso e linguagem | Médio | **Alta; escolhida** |

## Jornada funcional

1. A visitante entende que o produto organiza escolhas possíveis, sem garantir reação de terceiros.
2. A degustação solicita quatro escolhas categoriais e devolve um primeiro ajuste por regras locais.
3. A visitante pode escolher o percurso neutro ou não agir; nenhuma foto ou texto livre é exigido.
4. O pagamento único libera os sete dias e materiais por 90 dias.
5. Cada dia apresenta princípio em linguagem controlada, escolha categorial, microação e evidência de conclusão.
6. O progresso fica persistido na versão `musa-pde-entry-v7-espelho-antes-de-sair` e pode ser retomado pelo mesmo e-mail.
7. Expiração, erro, suporte e recuperação são estados explícitos; nenhuma falha é apresentada como sucesso.
8. Depois do Dia 7, a jornada termina explicitamente e não retorna silenciosamente ao primeiro dia.

## Fronteiras de produto e dados

- Não enviar respostas da v7 à OpenAI: o motor é `MUSA_LOCAL_RULES_V1`, com custo e tokens zero.
- Aceitar somente chaves e valores categoriais documentados no backend em qualquer rota que persista
  interação; um valor válido para outra pergunta também deve ser rejeitado.
- Não solicitar foto, medidas, saúde, emoção, personalidade, raça, religião ou texto livre nas sete
  missões. Mensagem voluntária de suporte deve declarar finalidade, retenção e alerta contra dados
  sensíveis ou desnecessários.
- Ao expirar, reconhecer a compra anterior e direcionar a suporte e direitos de dados, sem iniciar
  renovação ou nova cobrança automática.
- Não diagnosticar autoestima nem inferir o que terceiros pensarão.
- Não contar teste `mh_test`, degustação ou missão concluída como venda.
- Não gerar vídeos novos antes de existir atribuição por produto e ledger completo de custo.
- Exibir o aviso de dados antes do e-mail e dentro do acesso, seguindo o inventário, retenção e direitos
  definidos em `docs/marketing/musa-v7-data-governance.md`.
- Liberar acesso pago somente após consulta autenticada à Pepper confirmar exatamente oferta,
  **R$ 67**, moeda `BRL` e status pago; o webhook público isolado não é prova de compra.
- Persistir transação, oferta, valor, moeda, status e vínculo idempotente ao acesso em auditoria
  financeira própria, sem e-mail ou token reutilizável em texto claro.
- Retomar acesso somente por link entregue ao e-mail ou identidade Google verificada; produto e
  e-mail informados em uma requisição pública nunca podem devolver bearer token.
- Manter acesso integral de QA em rota interna autenticada, desabilitada em produção e segregada de
  compra, receita e eventos humanos.
- Servir missões e materiais pagos somente no workspace autenticado; arquivo protegido exige acesso
  `ACTIVE`.
- Manter um único JSON canônico para a v7 no Marketing Hub, no fallback do backend PDE e no
  changelog; divergência entre essas fontes bloqueia o gate.
- Executar retenção em worker externo, com saúde do backend, retry curto e correlação auditável;
  falha transitória não pode adiar a política por um dia inteiro.

## Entregáveis

- experiência pública de quatro escolhas e primeiro ajuste;
- sete missões guiadas com progressão e retomada;
- três materiais de consulta versionados e coerentes com os sete sinais;
- acesso com versão, pagamento e expiração auditáveis;
- trilha financeira idempotente e minimizada, separada do progresso da cliente;
- estados `TRIAL`, `ACTIVE` e `EXPIRED`;
- eventos de degustação, paywall, checkout, compra, acesso, missão, primeiro uso e reembolso;
- cartão de decisão coerente: R$ 67, pagamento único, acesso de 90 dias, limites e suporte.

## Gate

A construção só avança quando o caminho público e pago passa em desktop e mobile, a degustação não
entra na fila de IA, o dia 2 fica bloqueado sem compra, a versão e expiração persistem, não há texto
livre na v7, o conteúdo não usa alegações científicas como garantia, pagamento e conteúdo pago não
podem ser forjados por rota pública, direitos de dados são exercíveis e Dédalo, Têmis e Psique aprovam
as mesmas evidências. Venda, satisfação e transformação real continuam não comprovadas até clientes reais.
