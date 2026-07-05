# Plano — experimentos com Produto IA visual e personalizado

## Objetivo

Preparar experimentos de Produto IA que usem imagem gerada por IA e personalizacao como mecanismos de aumento de valor percebido, sem criar produtos manualmente fora do Marketing Hub.

## Regra principal

Nenhum Produto IA visual ou personalizado deve ser criado como ideia isolada, exemplo manual ou ativo avulso.

Todo teste precisa nascer pelo fluxo:

```text
Nicho/contexto
→ Hipotese
→ Dor
→ Resultado
→ Mecanismo
→ Prova
→ Oferta
→ Subtipo de Produto IA
→ Amostra/produto
→ Experimento
→ Medicao
```

Isso garante que o sistema consiga:

- explicar como o produto foi criado;
- criar o mesmo tipo de produto para outro nicho;
- variar a oferta de forma controlada;
- comparar resultados por subtipo;
- preservar prompts, schemas, custos e aprendizados.

## Subtipos iniciais

| Subtipo | Hipotese de valor | Primeiro uso recomendado |
|---|---|---|
| `AI_VISUAL_PREVIEW` | Ver o resultado aumenta clareza e desejo. | Previa visual do produto ou transformacao. |
| `AI_PERSONALIZED_SAMPLE` | Receber algo exclusivo aumenta valor percebido e reciprocidade. | MVP inicial. |
| `AI_TRANSFORMATION_SIMULATOR` | Antes/depois reduz abstracao da promessa. | Nichos com transformacao visual clara. |
| `AI_VISUAL_ASSET_PACK` | Pacote visual pronto para uso aumenta percepcao de entrega. | Produtos para negocios, criadores e profissionais. |
| `AI_IDENTITY_AVATAR_PRODUCT` | Identidade visual personalizada gera desejo de posse. | Marca pessoal, avatar, estilo e posicionamento. |
| `AI_REPORT_VISUAL_EVIDENCE` | Evidencia visual torna diagnostico mais convincente. | Diagnosticos, auditorias e planos de acao. |

## MVP recomendado

Comecar por `AI_PERSONALIZED_SAMPLE`.

Motivos:

- testa impacto visual e personalizacao no mesmo experimento;
- gera uma entrega concreta antes da compra;
- permite medir custo de IA por lead;
- pode ser comparado contra uma rota sem amostra;
- cria aprendizado reutilizavel para outros nichos.

## Preparo sistêmico antes do experimento

O primeiro experimento não deve ser criado manualmente. A hipótese `AI_PERSONALIZED_SAMPLE` precisa passar pelo preparo sistêmico exposto em `/api/product-ai/experiment-preparations/{hypothesisId}`.

O backend só libera rascunho de experimento quando a hipótese possui:

- nicho/contexto;
- dor principal;
- persona;
- promessa;
- mecanismo;
- preço;
- pacote de oferta;
- entregáveis do pacote;
- descrição da amostra personalizada.

Quando pronta, a tela aplica o rascunho canônico: `LOW_TICKET_PRODUCT`, `AI_PERSONALIZED_SAMPLE`, etapa `SAMPLE`, objetivo `SALES`, variável “Amostra visual personalizada” e métrica “Compra aprovada e custo de IA por compra”.

Depois da criação do experimento, o sistema deve criar ou reaproveitar o funil de coleta pelo endpoint `POST /api/product-ai/experiments/{experimentId}/personalized-sample-funnel`. Esse funil é obrigatório antes de publicar campanha porque a personalização depende dos dados do lead.

A página de venda do GeraSalesPage deve ser incorporada ao mesmo funil de coleta. O anúncio não deve enviar o lead para uma página separada nem para checkout direto: primeiro ele vê a promessa, informa os dados de personalização e só então o sistema segue para amostra/oferta/pagamento conforme o fluxo do Lead Portal.

Campos mínimos do funil:

- nome;
- e-mail;
- WhatsApp;
- negócio/projeto;
- contexto atual;
- objetivo visual;
- dados de personalização;
- preferências visuais.

## Funil de valor para `AI_PERSONALIZED_SAMPLE`

O experimento precisa deixar claro que a personalizacao e parte da entrega inteira. A amostra gratuita e uma prova pequena do mecanismo; o produto pago e a versao completa personalizada. Nao deve haver amostra personalizada seguida de produto final generico.

Fluxo recomendado para o primeiro teste:

```text
Anuncio
→ Lead Portal com formulario curto
→ Amostra personalizada gerada por IA
→ E-mail 1: entrega da amostra + oferta leve
→ E-mail 2: follow-up de conversao
→ Checkout
→ Produto final personalizado
```

O primeiro e-mail deve entregar a amostra, reforcar o principal diagnostico e apresentar a oferta paga sem pressao. O segundo e-mail deve sair depois de aproximadamente 24 horas, reativar a dor, mostrar aplicacao pratica e chamar para o checkout. Quatro ou mais e-mails so fazem sentido em uma otimizacao posterior, quando o funil ja tiver sinais de abertura, clique ou resposta sem compra.

O valor oferecido nao deve ser "usar IA". O valor deve ser reduzir esforco e dor com uma solucao aplicada ao contexto do lead. A entrega paga deve usar uma base padrao para escalar, mas adaptar diagnostico, plano, mensagens, ativos ou recomendacoes aos dados capturados no Lead Portal.

Exemplo de referencia:

- nicho: manicures autonomas que atendem em domicilio;
- dor: faltas, atrasos, cancelamentos e deslocamentos perdidos;
- amostra gratuita: `Agenda Blindada 7D` parcial com diagnostico e 1 ou 2 mensagens personalizadas;
- produto pago: `Kit Personalizado Agenda Blindada 7D`;
- entrega paga: diagnostico completo da agenda, plano dos proximos 7 dias, mensagens para confirmar horario, cobrar sinal, reagendar, lidar com atraso e cliente que some, no tom escolhido pela lead.

## Desenho experimental inicial

Fluxo:

```text
Anuncio
→ Lead Portal com promessa unica e formulario curto
→ Amostra personalizada gerada por IA
→ E-mail de entrega com oferta leve
→ E-mail de follow-up
→ Checkout
→ Entrega paga personalizada
→ Compra/nao compra
```

Metricas:

- custo por lead;
- taxa de conclusao do formulario;
- taxa de visualizacao da amostra;
- clique da amostra para checkout;
- compra;
- custo de IA por lead;
- custo de IA por compra;
- margem por compra;
- aprendizado qualitativo da amostra.

## Cuidados

- A imagem nao deve ser decoracao. Ela precisa provar, tangibilizar, personalizar ou reduzir esforco mental.
- A amostra nao deve entregar valor suficiente para eliminar a compra.
- O custo por geracao deve ter limite antes da escala.
- Cada variacao deve declarar uma unica variavel primaria.
- Se o ativo nao puder ser reconstruido pelo sistema, ele nao deve ser publicado como experimento canônico.
