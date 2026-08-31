# Matriz de homologação — descoberta autônoma até produto PDE v1

## Objetivo e critério comercial

Validar localmente a cadeia `Tema amplo + Instagram → Argos → dossiês → Atena → Plutus →
Dédalo → produto planejado`, sem campanha, publicação, gasto ou registro artificial de venda.

- **Gargalo:** candidatas factuais terminavam no ciclo técnico de Argos e ficavam invisíveis para os
  agentes seguintes.
- **Métrica esperada:** 100% das candidatas `DOSSIER_READY` criam um único dossiê e uma única cadeia
  auditável; somente a aprovação sequencial de Atena, Plutus e Dédalo cria um produto `PLANNED`.
- **Continuar:** cadeia conclui com evidências, custos, decisões e produto vinculados.
- **Ajustar:** qualquer gate retorna `ADJUST`, evidência insuficiente ou falha técnica recuperável.
- **Parar:** qualquer gate retorna `REJECT`, o contrato perde rastreabilidade ou o fluxo tenta
  publicar, gastar ou contabilizar receita.

## Alternativas avaliadas

| Alternativa | Benefício | Risco | Esforço | Aderência |
|---|---|---|---|---|
| Transferência manual | Mudança pequena | Dossiês esquecidos, perda de contexto e operação dependente de pessoa | Baixo | Baixa |
| Novo pipeline paralelo | Isolamento total | Duplica catálogo, filas, auditoria e responsabilidade dos agentes | Alto | Média |
| Conectar contratos BPM existentes | Reutiliza gates e agentes canônicos com uma única fonte de verdade | Exige materialização e relatório integrados | Médio | Alta |

A terceira alternativa é a escolhida. O backend materializa e avança a cadeia; workers apenas
consomem suas filas oficiais e reportam resultados.

## Cenários obrigatórios

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Informar somente um tema amplo | Backend completa B2C, Brasil, pt-BR, Instagram, modo descoberta e acervo `/pesquisas` |
| Caminho feliz | Argos devolve obrigatoriamente 2–3 candidatas factuais | Todas aparecem na tela com score, maturidade, evidências e lacunas |
| Contrato | Tema amplo com mais de 191 caracteres | Interface e backend recusam antes de persistir um ciclo incompatível com o banco |
| Contrato | Nome ou público de candidata com mais de 191 caracteres | Schema do modelo e validação do callback recusam o payload; resposta bruta continua auditável |
| Handoff | Candidata `DOSSIER_READY` | Um dossiê vinculado e tarefas únicas de Atena, Plutus e Dédalo |
| Gate | Candidata `RESEARCHABLE` ou `SIGNAL` | Permanece visível para nova pesquisa, sem avançar nem criar produto |
| Gate | Risco factual exige revisão humana | Argos não mascara o risco como pesquisa adicional nem permite `DOSSIER_READY`; o backend recusa combinações inconsistentes |
| Gate | Atena aprova | Plutus fica apto a reservar a próxima atividade |
| Gate | Plutus aprova | Dédalo fica apto a projetar o harness PDE |
| Gate | Atena, Plutus ou Dédalo ajusta/rejeita | Cadeia bloqueada com causa e próxima ação persistidas |
| Materialização | Dédalo aprova arquitetura e harness | Um plano e um produto `PLANNED`, nunca publicado, são vinculados ao dossiê |
| Idempotência | Callback de Argos ou Dédalo repetido | Nenhum dossiê, tarefa, plano ou produto duplicado |
| Integrações | Web, Meta ou `/pesquisas` indisponível | Fonte aparece como lacuna/bloqueio; ausência não vira mercado aprovado |
| Observabilidade | Modelo executa ou falha | Request, resposta bruta, modelo, tokens, custo, erro e evidências permanecem auditáveis |
| Relatório | Consultar execução pela API e pela tela | Cobertura Web, Meta, `/pesquisas`, ofertas, 2–3 candidatas, dossiês, decisões, custos e produto final aparecem sem leitura de JSON técnico |
| Métricas | Produto planejado é criado | Não altera visitantes, cliques, checkout, vendas, receita, campanha ou orçamento |
| Segregação | Dados de homologação | Identidade de teste não se mistura com produto ou execução produtiva |
| Recuperação | DDL aplicado sem registro do Liquibase | Contratos existentes são conferidos e a migração retoma sem duplicar coluna, índice ou chave |
| Compatibilidade | Rollback e reaplicação no MySQL 5.7 | Linhagem e maturidade são removidas e recriadas sem corromper os dados de origem |
| Desktop | Chromium em viewport desktop | Formulário e as sete etapas até **Produto planejado**, candidatas, gates e links ficam legíveis e operáveis |
| Mobile | iPhone 15 Pro e Pixel 7 | As mesmas sete etapas e informações aparecem sem corte, sobreposição ou rolagem horizontal |

## Política de rodadas

Uma rodada local completa sem defeitos conclui a homologação. Se qualquer defeito for encontrado e
corrigido, a contagem é reiniciada e a solução exige duas rodadas completas consecutivas sem falha.
