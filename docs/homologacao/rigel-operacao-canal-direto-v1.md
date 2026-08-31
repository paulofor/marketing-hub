# Matriz local — operação do Rigel no canal direto v1

## Objetivo

Comprovar que Hermes conclui a integridade técnica do experimento 89 com o preflight auditado, sem
exigir Meta, fabricar tráfego humano ou alterar a amostra estratégica de 15 contatos.

## Gargalo, métrica e decisão

- Gargalo: contrato operacional genérico de mídia paga aplicado a `DIRECT_ONE_TO_ONE`.
- Métrica esperada: `task-1` concluída com os três gates diretos em `PASS`; `task-2` aguarda 15
  contatos consentidos reais.
- Continuar: run produtivo ativo, preflight sem bloqueador e placares consistentes.
- Ajustar: gate ausente, divergência de eventos, mistura de QA ou requisito de outro canal.
- Parar: falha de privacidade, checkout, entrega, contribuição ou evidência oficial.

## Cenários

| Dimensão | Cenário | Evidência esperada |
| --- | --- | --- |
| Caminho feliz | Run 9 direto, ativo e sem bloqueadores | Hermes usa `consultar_preflight` e reconhece os três gates em `PASS` |
| Histórico | Run 8 falhou e run 9 está vigente | a ferramenta seleciona somente o maior `runNumber` produtivo |
| Canal | `DIRECT_ONE_TO_ONE`, sem campanha e orçamento diário | ausência de Meta não bloqueia nem aparece como próxima ação |
| Amostra | contrato e experimento definem 15 contatos | nenhum piso fixo de cem visitas substitui a amostra |
| Segregação | seis eventos `INTERNAL_QA` | comprovam instrumentação, mas métricas humanas, compras e vendas continuam zero |
| Validação | preflight ausente, bloqueado ou com gate não aprovado | Hermes retorna `BLOCKED` com causa e evidência faltante |
| Regressão paga | experimento `FACEBOOK` | campanha, impressão, gasto e trava de R$ 25 continuam exigidos |
| Integração | MCP chama runs e preflight oficiais | somente GET, escopo do experimento e fontes auditadas |
| Observabilidade | tarefa terminal | prompt, ferramentas, gates, tokens, custo e causa ficam persistidos |
| Interface | página de atividades do processo 66 | tentativa #289 preservada e nova tentativa rastreável pela tela |
| Navegadores | desktop, iPhone 15 Pro e Pixel 7 | situação, tarefa e próximo passo continuam legíveis e acionáveis |
| Segurança | execução local e publicação | sem alteração produtiva por SSH; imagem somente pelo Dockerfile e workflow versionados |

Quando qualquer rodada revelar defeito, a contagem de homologação reinicia. Depois da última
correção, são exigidas duas rodadas locais completas e consecutivas sem falha.

## Resultado de 2026-08-31

A primeira tentativa revelou que o catálogo central não declarava o novo prompt de Hermes; o
manifesto foi corrigido e a contagem reiniciada. Depois disso, duas rodadas completas e consecutivas
passaram sem falhas, cada uma com:

- 2.158 testes do backend, incluindo cockpit direto, canal pago e cobertura do harness;
- 31 testes Java do Hermes e cinco testes do contrato MCP;
- 436 testes do frontend, TypeScript e build de produção;
- navegação Chromium em desktop, iPhone 15 Pro e Pixel 7;
- Spotless, empacotamento, conteúdo do JAR e imagens Docker de backend e Hermes.

A tentativa produtiva #289 foi preservada. Não se criou nova tarefa enquanto a produção ainda usa o
contrato v2, evitando repetir o bloqueio e o consumo do modelo antes do deploy oficial.
