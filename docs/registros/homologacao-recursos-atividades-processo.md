# Matriz de homologação — recursos especializados por atividade

| Área | Cenários obrigatórios | Critério de aceite |
| --- | --- | --- |
| Caminho feliz | atividade comum; atividade com `themis-image-studio` | versão salva e recurso oficial visível no diagrama |
| Validações | código inexistente; recurso em START/GATEWAY/END; agente divergente | backend rejeita antes da execução |
| Falhas | recurso inativo; executor comum tenta reservar atividade especializada | fila permanece protegida e sem consumo |
| Integrações | tela → API → MySQL → tarefa → `pending` | contrato entrega código, nome, executor e instruções |
| Observabilidade | resultado e erro continuam correlacionados pela tarefa | nenhum avanço ou custo é inferido pela tela |
| Segregação | dados locais; recurso exato por agente/container | outro agente ou recurso não recebe a atividade |
| Interfaces | desktop Chromium, iPhone 15 Pro e Pixel 7 | seleção e detalhe sem overflow e com rótulos legíveis |

Se a primeira rodada completa revelar defeito, a correção reinicia a validação e exige duas rodadas
completas consecutivas sem falhas após o último ajuste.

## Resultado de 2026-08-20

- suíte completa do backend: 1.659 testes sem falhas;
- suíte completa do frontend: 117 arquivos e 343 testes sem falhas;
- duas rodadas finais consecutivas com volume MySQL 5.7 novo, migração física, API, tarefa e
  executores em containers;
- nas duas rodadas, o executor genérico não reservou a atividade e o executor
  `themis-image-studio` recebeu código, nome, executor e instruções oficiais;
- desktop Chromium, iPhone 15 Pro e Pixel 7 exibiram o recurso no diagrama e no editor sem
  overflow horizontal;
- os dados e containers temporários foram removidos ao final.
