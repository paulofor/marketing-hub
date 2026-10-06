# Capella — conciliação do aprendizado no Lead Portal

Contexto de 05/10/2026: produto #7, cadeia #26, experimento #88 encerrado,
sucessor de vídeo #94 preparado e candidata de barbearia #96 sem gasto autorizado.
A execução solicitada começa pelo histórico comercial, sem reativar o #88,
herdar orçamento ou alterar as hipóteses dos outros experimentos.

## Causa e escolha

O coletor do ciclo só lê `PdeExperimentAnalyticsReader`, que exige slot PDE.
Capella usa página GeraSalesPage, eventos normalizados e pagamentos do Lead Portal.
O cockpit e o MySQL confirmam essa fonte; ausência de slot não significa ausência
de visitas ou vendas. A mesma distinção já existe na preparação Quartzo e no cockpit.

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Adaptar o serviço de funil existente ao recorte do ciclo | Reutiliza a projeção do cockpit | Amplia um serviço grande e afeta leitores fora do ciclo; esforço maior | Não escolhida |
| Ler o Lead Portal no coletor conforme o tipo canônico | Preserva formato, auditoria e automação | Adaptador de leitura e regressões localizadas; esforço proporcional | Escolhida |
| Normalizar todas as fontes em um catálogo de provedores | Facilita outros formatos futuros | Acrescenta abstração sem necessidade demonstrada; esforço maior | Não escolhida |

A segunda alternativa resolve a fonte compartilhada sem mudar a oferta ou a operação.
Métricas manuais e slot artificial foram descartados por incompatibilidade com os contratos.

## Matriz de aceite local

| Cenário | Prova esperada |
| --- | --- |
| Capella histórico | Quartzo sem slot concilia visitantes, sessões, checkout, pagamento, mídia e custos da janela |
| Outro produto e outros IDs | Mesmo comportamento sem exceção por nome ou identificador |
| PDE antes válido | Caminho de slot, versão, atribuição e desfechos canônicos permanece válido |
| QA, bots e desconhecidos | Permanecem na auditoria bruta e fora das métricas comerciais |
| Janela e outro experimento | Eventos e compras externos ao recorte não contaminam a leitura |
| Checkout compartilhado | Compra sem atribuição suficiente bloqueia; não é duplicada no sucessor |
| Pagamento incompleto ou duplicado | Valor, moeda, referência ou status inconsistentes bloqueiam a decisão |
| Reembolso e entrega | Reembolso sem conciliação datada bloqueia; entrega exige pagamento e ZIP/envio na janela; uso/satisfação ausentes não viram prova |
| Fonte ausente ou indisponível | Bloqueio persistível, sem números presumidos |
| SQL | Consultas exercitadas em banco local e MySQL 5.7 isolado |
| API e continuidade | Entrada histórica produz medição auditável e retorno ao Processo 6, sem campanha ou cobrança |
| Desktop e mobile | Formulário e leitura pelo Chromium desktop, iPhone e Pixel emulados; sem eventos comerciais sintéticos |

Não exige nova inferência paga, geração de mídia ou aquisição. Os testes simulam
somente integrações externas e preservam registros de custo. A candidata barber-v1
continua sujeita a acervo, economia e autorizações próprios.

## Resultados locais

- O teste do caso original falhou antes da correção com `PDE_ANALYTICS_SLOT_REQUIRED`.
- O MySQL 5.7 isolado reproduziu conflito de collation na conversão textual do ID.
  A consulta passou após usar `CONCAT(id, '')`, cuja coercibilidade permite usar a collation
  da coluna e cuja assinatura também é aceita pelo H2 de testes. Referência:
  [manual oficial MySQL 5.7](https://downloads.mysql.com/docs/refman-5.7-en.a4.pdf).
- SQL, coletor e regras validaram o Capella, outro kit, IDs distintos, janelas,
  atribuição, pagamentos, falhas e dados de teste. A regressão PDE e as regras de
  arquitetura passaram. A suíte completa terminou com 4.020 testes, nenhuma falha ou erro
  e 27 casos condicionais de outros fluxos ignorados: 3.993 executados com sucesso.
  Após o ajuste de compatibilidade, o recorte de SQL, coletor e regras passou também no H2.
  O recorte final teve 59 testes no H2 e 151 no MySQL 5.7, incluindo arquitetura,
  todos aprovados. [Resultado local](evidencias/capella-aprendizado-lead-portal-v1/resultado-local.json).
- O harness existente recebeu a fotografia exportada do teste SQL, com os mesmos IDs,
  valores e ausência explícita de degustação. Backend/BPM persistem adoção, medição,
  decisão, retorno e aprendizado do sucessor em MySQL real; o worker de Atena executa
  com modelo local simulado. Nenhuma chamada comercial ou de produção ocorre nos testes.
- Playwright passou nos três perfis: Chromium desktop, iPhone 15 Pro e Pixel 7.
  A fixture de posição foi corrigida para declarar a cadeia, conforme a segregação
  vigente; o número da atividade e o retorno ao Processo 6 voltaram a ser verificados.
- Evidências transitórias da execução: relatórios Surefire, snapshot SQL e screenshots
  em `/tmp/capella-execution`. Esses dados são sintéticos e não comprovam mercado.
