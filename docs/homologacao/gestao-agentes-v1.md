# Matriz de homologação — Gestão de agentes v1

## Objetivo

Validar o cadastro auditável das responsabilidades, entradas, análises, entregáveis e regras de
coordenação dos agentes sem ampliar autoridade operacional.

## Cenários

| Grupo | Cenário | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | Acessar pelo menu, criar contrato completo e editar | Dados persistidos e versão incrementada |
| Validação | Nome, modo ou tema ausente | Salvamento bloqueado pela tela |
| Contexto | Informar múltiplas entradas e saídas ordenadas | Ordem e descrição preservadas |
| Orquestração | Salvar regras, análise, oferta e responsabilidade | API e snapshot versionado preservam os campos |
| Autoridade | Registrar regra de coordenação | Autoridade permanece separada e não é ampliada |
| Falha | Backend rejeitar ou ficar indisponível | Nenhuma falsa confirmação de salvamento |
| Auditoria | Editar contrato existente | Nova versão sem apagar a fotografia anterior |
| Observabilidade | Listar agentes | Status, versão e contratos ficam visíveis |
| Segregação | Editar um agente | Nenhum outro cadastro é alterado |
| Desktop | Chromium desktop | Menu, formulário e listas utilizáveis |
| Mobile | iPhone 15 Pro e Pixel 7 | Campos, botões e navegação utilizáveis com toque |

## Critério de conclusão

Uma rodada local completa sem defeitos conclui a homologação. Se surgir defeito, corrigir a
causa-raiz e executar cinco rodadas completas e consecutivas sem falhas após a última correção.
