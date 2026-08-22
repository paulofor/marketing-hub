# Matriz de homologação — PDE decisão assistida v4

## Escopo

Validar localmente a versão 4 da cadeia `pde-value-creation-delivery`, incluindo as seis versões de
processo, o Cartão de Decisão, a Prontidão para Decisão por IA, fricção agregada e controle da
personalização. Nenhum tráfego, venda, campanha ou dado produtivo é criado nesta homologação.

## Matriz

| Área | Cenário | Evidência esperada |
| --- | --- | --- |
| Caminho feliz | Aplicar os changelogs em ordem sobre schema compatível com MySQL 5.7 | Cadeia v4 `PUBLISHED`, seis itens na ordem correta e processos anteriores `RETIRED` |
| Idempotência | Reexecutar a atualização Liquibase | Nenhuma versão ou item duplicado; execução sem erro |
| Contrato | Ler cada `diagram_json` | JSON válido, nós e fluxos íntegros, score 80 e fatos críticos explicitados |
| Validação | Verificar include mestre, `dbms`, statements e datas | Include relativo presente; nenhum `TIMESTAMP NOT NULL` ou SQL 1093 |
| Falha | Ausência de campo crítico no Cartão de Decisão | Gate permanece bloqueado mesmo com score agregado aparente |
| Integrações | Abrir a cadeia pela API/tela administrativa existente | Seis processos legíveis, sem mudança de endpoint ou recomputação no frontend |
| Observabilidade | Revisar o contrato de execução | Fórmula, amostra, denominadores, origem, versão e custos de revisão por IA são auditáveis |
| Métricas | Simular leitura das regras | Score e fricção permanecem diagnósticos; compra, receita e entrega decidem escala |
| Segregação | Revisar origem e dados comportamentais | Somente origem comprovável; agregados consentidos; sem perfil psicológico individual |
| Desktop | Renderizar a tela de cadeia em viewport desktop | Títulos, score e descrições permanecem navegáveis sem sobreposição |
| Mobile | Renderizar em iPhone 15 Pro e Pixel 7 | Fluxo legível, sem overflow horizontal nem perda de ações |

## Regra de encerramento

Se a primeira rodada completa não revelar defeito, a homologação termina. Se revelar, corrigir a
causa-raiz e executar duas rodadas completas consecutivas sem falha após a última correção.
