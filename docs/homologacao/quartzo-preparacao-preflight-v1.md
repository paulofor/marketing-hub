# Preparação Quartzo e preflight — 21/09/2026

## Causa e comparação

Psique #471 analisou a publicação correta #28, aprovou oferta, preço, prova,
checkout e economia, mas exigiu compra simulada, entrega e eventos que o processo
pai reserva ao preflight posterior. A definição v8 do pai e o contrato `checkout`
Quartzo já estabelecem essa ordem. O prompt v2 de Psique e v1 de Têmis não
explicitavam a fronteira. Não se altera o BPM, nem se elimina o preflight.

Os prompts candidatos Psique v3/Têmis v2 delimitam preparação, requisitos atuais,
riscos a transferir e falhas comprovadas que não podem ser adiadas. Os dez gates,
schemas, controle do backend e exigência de prova material permanecem. Melhoria
estética sem déficit crítico não vira condição obrigatória em ciclos sucessivos.

As dúvidas concretas sobre briefing, recebedor e reembolso foram esclarecidas no
contrato do produto pela tela, preservando prazo de três dias úteis, preço e entrega.
O nome observado do recebedor não prova vínculo societário; não se inventa CNPJ.

## Matriz antes dos testes

| Caso | Aceite |
| --- | --- |
| Contexto e empacotamento | Workers carregam prompts novos e schemas anteriores, identidades genéricas preservadas |
| Fontes completas, preflight futuro | Instruções exigem parecer sobre preparação e registro explícito de limitações |
| Contradição ou falha observada | Instruções continuam bloqueando preço/prazo divergente, kit ausente, download falho e tráfego misturado |
| Pós-compra local | HTTP real, pagamento simulado, briefing persistido, composição/ZIP real, contrato HTTP de e-mail e download íntegro |
| Falha e retomada | Falha de e-mail não marca entregue; repetir após recuperação reutiliza pagamento/briefing, entrega e não duplica envio concluído |
| Proteção | Pagamento pendente não inicia produção; nenhum pagamento/modelo/SMTP produtivo |
| Visual | Página e formulário publicados coerentes em desktop, iPhone e Pixel |
| Processo | Retomar a execução existente somente após publicação e entradas válidas; backend decide Psique, Têmis e retorno ao pai |

Teste local de e-mail usa um double HTTP, não comprova SMTP produtivo, leitura
humana ou SLA. A prova completa do ambiente comercial permanece no preflight.
Não se alegam vendas, ganho de conversão ou aprendizado automático por alteração de prompt.

Fontes: definição versionada em `2026-09-20-quartzo-commercial-preparation-v1.sql`,
resultado persistido de Psique #471 e [OpenAI Prompt engineering](https://developers.openai.com/api/docs/guides/prompt-engineering#message-formatting-with-markdown-and-xml),
consultada em 21/09/2026: separar instruções, exemplos e contexto. Rollback por
retorno aos caminhos anteriores; não sobrescrever os recursos históricos.

## Resultado local

- Psique: 141 casos Java, zero falhas/erros e dois condicionais ignorados; 22 casos
  de navegador aprovados. Têmis: 103 casos, zero falhas/erros e um condicional ignorado.
- Pagamentos: 49 casos aprovados, incluindo três integrações novas de HTTP/DB/ZIP,
  envio simulado, falha e retomada; pacote Spring construído.
- Builds e Spotless dos workers aprovados; diff sem erros. O contrato de teste
  inicialmente procurava uma formulação diferente da instrução equivalente e foi
  ajustado; as duas regressões foram repetidas, sem remover proteções.
- Pagamento pendente não criou briefing; entrega concluída não reenviou e-mail;
  ZIP baixado correspondeu byte a byte ao arquivo produzido, com 24 entradas.
- Ainda é necessário observar a decisão dos agentes publicados. Os testes de
  composição não medem sozinhos mudança de julgamento do modelo.

## Condições omitidas na geração

Na geração seguinte, o HTML `a9a82535-a863-4224-9fa8-d9885ce3cd7a` conservou o
recebedor, mas omitiu canal do briefing e distinção de reembolso, ambos presentes
na fonte oficial. Não se repetiu Psique. O complemento no executor GeraSalesPage
renderiza três campos explícitos do contrato como texto escapado nas etapas de
HTML e publicação, antes da auditoria. A revisão de qualidade recebe o mesmo bloco.
Não se inventam condições, não se mudam imagens nem se aceitam scripts do contrato.

Matriz adicional: fonte ausente não inventa texto; termos presentes aparecem mesmo
quando o modelo omite; repetição não duplica; mudança legítima substitui termos;
HTML malicioso fica texto; estágios anteriores não mudam; resposta bruta e custos
continuam auditáveis. Teste integrado do processor usa modelo/backend simulados e
confere HTML devolvido nas duas etapas. Conferir layout desktop/iPhone/Pixel local
antes de atualizar o PR. A geração vigente termina normalmente; nenhuma chamada
em curso é cancelada e nenhuma avaliação paga ocorre antes da correção.

O AI Worker passou nos 248 casos da suíte local, com dois condicionais ignorados,
sem falhas/erros. A dependência backend foi instalada localmente após a resolução
remota retornar 401; não foi necessário acesso adicional. O replay do HTML real
passou em desktop/iPhone/Pixel. A inspeção visual identificou a CTA fixa cobrindo
as últimas linhas; o bloco agora reserva espaço final, verificado nos três perfis.
As regressões afetadas e o build foram repetidos após esse ajuste de apresentação.

O CI detectou os novos prompts ausentes do inventário `agent-harness-v2.json`.
O catálogo recebeu as duas versões preservando as anteriores; o teste específico
foi reproduzido localmente e passou. A suíte backend completa foi executada antes
de publicar esse complemento. A configuração efetiva do fornecedor foi consultada
em diagnóstico somente de leitura, limitada aos três campos públicos, e o CNPJ
cadastrado foi acrescentado à oferta pela tela.
