# Matriz de homologação local — Descoberta PDE B2C/Instagram

## Objetivo e decisão

Executar localmente a Descoberta com foco comercial B2C para Instagram, usando todos os artigos
vigentes em `pesquisas/ia-aplicada`, o inventário vivo de `pesquisas/gartner`, o histórico Hotmart
persistido e evidências independentes. O objetivo é encontrar uma oportunidade com potencial
auditável **estritamente superior** ao Rigel, 82/100, sem transformar score, temperatura, anúncio ou
parecer em venda.

- **Gargalo:** o processo anterior aceitava oportunidades B2B sem exigir aderência ao canal real.
- **Evidência:** a melhor hipótese anterior foi B2B e estabilizou em 74/100; o Instagram é o canal
  informado e o funil comercial ainda não recebeu eventos humanos.
- **Métrica esperada:** consenso `APPROVE` de Argos, Hermes, Dédalo e Psique, score acima de 82,
  valor percebido mínimo 75 e distribuição Instagram mínima 8/10.
- **Continuar:** candidata acima de 82, com duas vias independentes de recorrência, desatendimento e
  intenção de compra, além de primeiro valor mobile em até dez minutos.
- **Ajustar:** lacuna corrigível de evidência, momento de compra, gancho, simplicidade ou atribuição.
- **Parar:** score até 82, B2B disfarçado, curso genérico, risco sensível, dependência operacional ou
  canal presumido.

## Alternativas de desenho comparadas

| Alternativa | Benefício | Risco | Esforço | Decisão |
| --- | --- | --- | --- | --- |
| Apenas ampliar temas do prompt | mudança pequena | o scoring continuaria premiando B2B bem documentado | baixo | rejeitada |
| Inserir um filtro B2C antes da priorização | corta candidatos empresariais | não prova simplicidade nem Instagram | médio | insuficiente |
| Curadoria e gate B2C/Instagram integrados | governa público, cena, produto, canal e métrica | exige contratos e testes novos | médio | escolhida |

## Matriz ponta a ponta

| Área | Cenário | Evidência esperada | Critério |
| --- | --- | --- | --- |
| Caminho feliz | Três dores pessoais distintas e quatro agentes | decisão, score, fontes, produto e limite | consenso e score acima de 82 |
| Inspirações vivas | Arquivos atuais e futuros das duas coleções | caminho, SHA-256, conteúdo e consulta do ciclo | nenhum arquivo congelado |
| Hotmart | Último snapshot nominal e histórico de destaque | produto, ranking/temperatura, data e limitação | sinal nunca vira venda |
| Falha Hotmart | Snapshot atual vazio ou placeholder | causa e uso do último snapshot nominal | nenhuma oferta inventada |
| B2C | Pessoa, cena, desejo e momento de compra | campos explícitos por candidata | nenhuma operação empresarial |
| Instagram | Gancho, cena demonstrável e rota de eventos | `IMPRESSION` até `CHECKOUT_STARTED` | distribuição mínima 8/10 |
| Produto simples | Primeiro valor no celular | tempo e saída funcional | até dez minutos, sem integração |
| Evidência | Dor, lacuna e compra | duas vias independentes por dimensão | inspiração fora da contagem |
| Integrações | OpenAI Flex, artigos e Hotmart | request, response, modelo, custo e origem | correlação única e erro auditável |
| Falha de agente | Schema, score ou nomes divergentes | execução falha sem correção silenciosa | nenhum avanço parcial |
| Observabilidade | Quatro execuções e gate final | status, tokens, custo, erro e decisão | consumo ausente fica desconhecido |
| Métricas | Homologação sem efeito comercial | zero contatos, compras, vendas, receita e mídia | parecer não conta como venda |
| Dados de teste | Ciclo e artefatos `LOCAL_QA` | diretório isolado e nenhum callback produtivo | zero contaminação do Hub |
| Navegadores | Descoberta ainda sem jornada pública | não aplicável nesta etapa | Chromium desktop, iPhone 15 Pro e Pixel 7 obrigatórios após materialização |

## Política de rodadas

Uma primeira execução completa é suficiente se não revelar defeito. Se qualquer defeito for
encontrado, a causa-raiz deve ser corrigida e a contagem reiniciada; depois da última correção, são
obrigatórias duas rodadas completas e consecutivas sem falha. Nenhuma campanha, publicação, gasto ou
cadastro produtivo faz parte desta matriz.

## Resultado executado em 2026-08-26

A primeira execução revelou que o gate de distribuição era aplicado também às alternativas
perdedoras, encerrando a comparação antes da decisão final. A causa foi corrigida: todas as
alternativas continuam obrigadas a ser B2C, simples e mobile, mas a nota mínima de distribuição é
gate apenas da vencedora quando a decisão for `APPROVE`. Dois testes de contrato cobrem a regra.

Depois da correção, duas rodadas completas e consecutivas terminaram sem falha:

| Rodada | Vencedora | Dédalo | Psique | Decisão |
| --- | --- | ---: | ---: | --- |
| `LOCAL_QA...V5_02` | Entrevista sem Branco | 75 | 74 | `RESEARCH_MORE` |
| `LOCAL_QA...V5_03` | Entrevista sem Branco | 78 | 73 | `RESEARCH_MORE` |

As duas rodadas preservaram zero contatos, compras, vendas, receita, mídia e publicações. A hipótese
foi estável, mas não superou o Rigel, 82, nem o mínimo de valor percebido, 75. Portanto, o caminho
feliz de aprovação não foi atingido e nenhum produto ou experimento foi criado.
