# Matriz de homologação — Descoberta PDE com Meta Ads Library no Instagram v1

## Objetivo comercial

Medir a presença real de uma categoria em anúncios do Instagram antes de comparar uma oportunidade
B2C com o Rigel. Anúncio, atividade, longevidade ou quantidade de anunciantes são sinais de mercado;
somente pagamentos reconciliados do Marketing Hub são vendas.

## Alternativas avaliadas

| Alternativa                                                   | Benefício                                                         | Risco                                                                          | Esforço | Decisão   |
| ------------------------------------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------ | ------- | --------- |
| Entregar o token Meta diretamente a Argos                     | Integração curta                                                  | Duplica integração, expõe credencial ao agente e rompe o isolamento do coletor | Baixo   | Rejeitada |
| Automatizar a interface pública                               | Cobertura comercial brasileira imediata                           | Raspagem frágil e incompatível com o contrato da Meta                          | Médio   | Rejeitada |
| Coletor dedicado + evidência persistida + modo supervisionado | Segurança, auditoria e evolução automática quando a API autorizar | Depende de autorização externa ou observação humana no Brasil                  | Médio   | Escolhida |

## Critérios e cenários

| Área               | Cenário                                                         | Resultado esperado                                                                                                              |
| ------------------ | --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| Caminho feliz API  | Aplicação autorizada, território coberto e consulta `INSTAGRAM` | Preflight aprovado, pendência reservada, payload bruto persistido e evidência entregue a Argos                                  |
| Caminho brasileiro | Categoria comercial no Brasil sem cobertura automática          | Acompanhamento supervisionado criado uma única vez, busca oficial disponível na tela e ciclo marcado como aguardando observação |
| Reuso              | A mesma categoria é solicitada novamente                        | Investigação existente é reutilizada sem duplicação operacional                                                                 |
| Plataforma         | Há anúncios Facebook e Instagram para os mesmos termos          | Somente os anúncios observados no Instagram entram na cobertura do ciclo                                                        |
| Relevância         | Anúncio coincide apenas com termos genéricos                    | Evidência é descartada e não melhora o gate B2C/Instagram                                                                       |
| Autorização        | Token existe, mas `ads_archive` devolve permissão negada        | Coletor não reserva trabalho e expõe diagnóstico sanitizado; ausência não vira ausência de mercado                              |
| Credencial         | Token não existe                                                | Preflight bloqueado sem chamada à Meta e sem evidência fabricada                                                                |
| Atualidade         | Evidência tem mais de 30 dias sem reobservação                  | Cobertura marcada como desatualizada e oportunidade permanece em `RESEARCH_MORE`                                                |
| Integração         | Backend ou Meta responde erro                                   | URL, operação, investigação/ciclo e stack trace ficam nos logs; ciclo preserva a lacuna funcional                               |
| Observabilidade    | Consulta é concluída                                            | Relatório registra status da fonte, modo, anúncios, anunciantes, última observação e interpretação                              |
| Métricas           | Categoria possui anúncios aderentes                             | Contagens de anúncios ativos, anunciantes e longevidade são auditáveis e não alteram vendas                                     |
| Segregação         | Homologação local                                               | Workspace e ciclos de QA ficam na topologia efêmera, sem escrita em produção                                                    |
| Desktop            | Chromium                                                        | Link oficial abre em nova aba e formulário registra plataforma observada                                                        |
| Mobile             | iPhone 15 Pro e Pixel 7                                         | Formulário, link e estados de cobertura permanecem legíveis e operáveis                                                         |

## Regra de decisão

- **Continuar:** cobertura atual do Instagram, anúncio aderente e ao menos um anunciante real,
  combinados com as demais provas de dor, compra e mecanismo.
- **Ajustar:** anúncios existem, mas a consulta é ampla, a aderência é baixa ou a evidência está
  desatualizada.
- **Parar o avanço:** fonte indisponível, autorização inválida, ausência de plataforma comprovada ou
  tentativa de tratar anúncio como venda.

## Resultado da homologação local em 2026-08-26

- O preflight real confirmou `ads_read=granted`, mas o endpoint `ads_archive` respondeu com código
  `10` e subcódigo `2332002`. A coleta automática ficou corretamente em `UNAUTHORIZED`, sem reservar
  pendência e sem converter indisponibilidade da fonte em ausência de mercado.
- A migração foi aplicada fisicamente no MySQL 5.7.44 sobre um registro legado, preencheu
  `publisher_platforms` com `INSTAGRAM` e permaneceu idempotente na segunda execução.
- Duas rodadas locais completas e consecutivas foram concluídas sem falha: 1.873 testes do backend,
  5 testes do coletor, 55 testes de Argos e 403 testes do frontend por rodada, além de typecheck,
  build, validação estática Liquibase/MySQL 5.7 e revisão de formatação.
- Em cada rodada, o fluxo da tela foi homologado em Chromium desktop, iPhone 15 Pro e Pixel 7, com
  criação de acompanhamento, registro da plataforma observada, link oficial em nova aba, ausência
  de erro JavaScript e ausência de overflow horizontal.
- A homologação usou somente dados e dependências locais. Nenhuma campanha, publicação, gasto,
  produto, experimento, evento humano ou venda foi criado.
