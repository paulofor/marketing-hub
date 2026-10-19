# Conexão do Worker IA pelo painel — v1

## Caso observado e escolha

Em 19/10/2026, o Worker IA já estava publicado com Codex App Server, mas dependia
de um código temporário entregue na conversa. O operador não conseguiu conectar.
Os agentes já possuem reconexão administrativa; o Worker não pertence ao cadastro
de agentes e não deve ganhar uma identidade fictícia para autenticar.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Repetir código pela conversa | Nenhuma mudança | Expira e depende de intervenção manual | Descartada |
| Cadastrar Worker como agente | Reaproveita a fila existente | Confunde responsabilidades, tarefas e identidade | Descartada |
| Comando administrativo próprio, usando o bridge existente | Sessão individual, acompanhamento e renovação no painel | Contrato pequeno de persistência e callbacks | Escolhida |

O backend persiste comandos e provas; o Worker consulta `pending`, executa o mesmo
`account/login/start` oficial e confirma `account/read`. O frontend mostra apenas
URL oficial, código temporário e estado. Tokens, conta e senha permanecem fora da API.
Não há nova inferência, nova autorização comercial ou fallback pago na conexão.

## Matriz definida antes da execução local

| Área | Aceite |
| --- | --- |
| Caminho feliz | Painel solicita; Worker reserva; código aparece; confirmação ChatGPT chega ao backend e aparece como conectado |
| Validação | URL não oficial e código inválido são recusados; sessão API key não conta como ChatGPT |
| Concorrência e expiração | Cliques repetidos reutilizam comando ativo; apenas um claim; expiração elimina código e permite renovar; callback antigo não conecta |
| Recuperação | Falha transitória de callback reenvia o resultado preservado, sem novo login ou inferência; reinício preserva o recibo |
| Observabilidade | Ausência de heartbeat indica estado desconhecido; falha mostra orientação; nenhum segredo aparece em resposta/log |
| Isolamento | Sessão exclusiva do Worker; filas dos agentes, recibos de geração e limites dos produtos preservados |
| Navegação | Chromium desktop, iPhone e Pixel: iniciar, abrir link oficial, copiar/selecionar código e atualizar estado sem recarregar |
| Comercial | Login não é geração aceita nem experimento RUNNING; sem compras ou margem novas atribuídas aos testes |

Validação usa backend/MySQL efêmeros e App Server simulado, sem credenciais ou dados
comerciais. A confirmação da conta real pertence ao operador na página da OpenAI.

## Fonte do protocolo

[Documentação oficial do App Server](https://learn.chatgpt.com/docs/app-server#auth-endpoints),
consultada pelo MCP oficial em 19/10/2026: `chatgptDeviceCode`, notificação
`account/login/completed` e confirmação com `account/read`. A interface é administrativa
interna da conta operacional existente; não disponibiliza sessão ChatGPT a clientes.

## Aprendizado e adoção

Hipótese verificável: permitir gerar e renovar o código na mesma tela reduz repasses
manuais e elimina a dependência de códigos vencidos enviados em chat. Critério técnico:
matriz acima aceita; critério operacional posterior: tempo para conectar e recorrência
de bloqueios. Sem medida comercial, nenhum ganho de conversão ou vendas é declarado.
Reversão mantém sessão e recibos; apenas desabilita o consumo dos comandos administrativos.

## Resultado local

- Backend: 105 testes de fluxo HTTP/JPA, monitor dos agentes e arquitetura; sem falhas.
- MySQL 5.7.44: Liquibase aplicado duas vezes, tipos temporais conferidos, rollback e reaplicação aceitos.
- Worker Java: 291 testes, sem falhas; três testes já condicionais não aplicáveis ao ambiente.
- Bridge Node: 27 testes aprovados, incluindo recuperação de callback após interrupção e outro identificador.
- Frontend: 15 testes aprovados, verificação TypeScript e build de produção aceitos.
- Chromium: fluxo completo com backend real sintético em desktop, iPhone 15 Pro e Pixel 7;
  confirmação preservada após recarregar, sem rolagem horizontal. Expiração e falha de rede verificadas.
- Contratos existentes dos agentes, shell (`bash -n` e ShellCheck) e validação estática Liquibase aceitos.

Capturas e resultado sintético ficam em `artifacts/worker-connection/` na sandbox.
O harness é `scripts/test-ai-worker-connection-browser.mjs` com o backend restrito
`AiWorkerConnectionFlowTest.main`; a configuração de teste é excluída dos contextos
produtivos. Nenhuma chamada de inferência ou login real foi usada nessa homologação.
