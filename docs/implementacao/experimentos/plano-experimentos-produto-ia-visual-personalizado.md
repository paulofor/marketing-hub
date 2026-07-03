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

## Desenho experimental inicial

Fluxo:

```text
Anuncio
→ Pagina/formulario com promessa unica
→ Entrada minima do lead
→ Amostra personalizada gerada por IA
→ Oferta paga para pacote completo
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
