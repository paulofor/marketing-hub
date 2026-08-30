# Homologação — Argos: descoberta de mercados para Instagram v1

Data: 2026-08-30

## Objetivo

Comprovar localmente que Argos consegue partir de um universo amplo de público e canal, pesquisar
sinais factuais em camadas e devolver de duas a três candidatas de mercado distintas, com fontes e
lacunas auditáveis, sem escolher a estratégia que pertence à Atena e sem fabricar demanda, anúncio,
venda ou receita.

## Métrica de liberação

- 100% das candidatas referenciam apenas evidências realmente coletadas e identificadas;
- toda execução informa modo de pesquisa, cobertura das fontes, modelo, prompt, resposta bruta,
  tokens disponíveis e lacunas;
- nenhuma candidata com menos de dez ofertas comparáveis, cobertura Instagram ausente ou evidência
  insuficiente recebe decisão `APPROVE`;
- o modo degradado aparece como determinístico e nunca é apresentado como síntese de modelo;
- Atena permanece como única autoridade de priorização, posicionamento, oferta e canal.

## Matriz ponta a ponta

| Dimensão | Cenário | Evidência esperada |
| --- | --- | --- |
| Caminho feliz | Descobrir mercados B2C para mulheres em contexto editorial e aquisição Instagram | Plano com lentes distintas, consultas atômicas, candidatas não genéricas e referências válidas |
| Compatibilidade | Validar um mercado já informado sem declarar o novo modo | Ciclo usa `VALIDATE_MARKET` e preserva o contrato anterior |
| Validação | Modo, tipo de mercado ou opção de formulário desconhecidos | Backend responde erro de entrada antes de criar ciclo |
| Validação | Síntese cita uma evidência inexistente | Worker bloqueia a execução e preserva a auditoria da chamada |
| Falha externa | Todas as consultas públicas falham | Ciclo termina `FAILED/BLOCKED`, nunca como pesquisa vazia bem-sucedida |
| Evidência insuficiente | Existem dores, mas menos de dez ofertas comparáveis | Candidatas podem ficar visíveis como `RESEARCH_MORE`; nenhuma é aprovada |
| Canal | Fonte Meta está pendente ou indisponível | Cobertura fica explícita e o gate Instagram permanece aberto |
| Integração | Hotmart, ClickBank e páginas comerciais devolvem duplicatas | Contagem canônica deduplica identidade e não inclui anúncio como oferta |
| Biblioteca interna | Tema encontra artigos em `/pesquisas` | Prompt registra caminho, título e trecho dos documentos selecionados |
| Auditoria | Planejamento e síntese usam modelo | Tarefa agrega prompts e tokens; ciclo preserva as duas respostas brutas |
| Degradação | Codex está desabilitado | Plano e síntese determinísticos ficam identificados, sem custo inventado |
| Observabilidade | Request e response de busca/modelo | Logs têm ciclo/consulta/URL sem segredo; relatório funcional fica persistido |
| Segregação | Dados locais e fontes públicas de teste | Nenhum evento é enviado ao funil, nenhuma venda ou receita é criada |
| Desktop | Chromium em viewport desktop | Modo, tipo de mercado, fontes e ajuda ficam legíveis e acionáveis |
| Mobile | iPhone 15 Pro e Pixel 7 | Selects, textarea, botão com spinner e histórico funcionam sem corte horizontal |

## Critério de continuar, ajustar ou parar

- **Continuar:** a rodada completa passa e as candidatas são factuais, distintas e auditáveis.
- **Ajustar:** há pesquisa útil, mas falta fonte, cobertura ou contrato; a lacuna deve ficar explícita.
- **Parar:** aparece evidência inventada, aprovação sem gate, decisão estratégica atribuída a Argos,
  vazamento de segredo, criação de gasto/publicação ou mistura de dados de teste com o funil real.

## Resultado local

Duas rodadas finais completas e consecutivas passaram sem falhas após a última correção. Cada
rodada cobriu testes do worker e do backend, contrato e build do frontend, validação estática e
física no MySQL 5.7, imagem Docker final e jornadas Chromium em desktop, iPhone 15 Pro e Pixel 7.

Em cada integração real com o modelo e a pesquisa pública, Argos executou 20 consultas, preservou
30 evidências públicas e 7 referências da biblioteca `/pesquisas`, e devolveu candidatas com
evidências rastreáveis. Como a cobertura Meta e os sinais de compra ainda eram insuficientes, todas
as candidatas permaneceram corretamente em `RESEARCH_MORE`. Nenhum produto, campanha, gasto,
evento de funil, venda ou receita foi criado durante a homologação.
