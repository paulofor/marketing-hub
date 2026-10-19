# Worker IA sobre Codex App Server — homologação v1

## Escopo e decisão

Solicitação de 19/10/2026: usar Codex App Server no Worker IA, como os agentes, removendo
a dependência de créditos da API para as etapas Responses. Capella/experimento 105 tem
`sales-page-offer-brief` em falha por `429 credit_balance_exhausted`; não há geração ativa.
Nenhuma alteração de limite de IA, audiovisual, mídia ou seleção humana está autorizada.

Foram comparados adaptar cada cliente, criar um serviço intermediário e substituir o
transporte dentro do worker. Foi escolhido um filtro compartilhado de WebClient: conserva
as filas, os prompts/schemas, validadores, callbacks e a orquestração do backend, sem novo
serviço nem chamada entre containers. O App Server roda localmente por stdio, em sessão
ChatGPT exclusiva, com lock. Integrações especializadas de imagem continuam explícitas.
O cliente legado de enriquecimento também usa o WebClient compartilhado; pesquisa Google
continua independente. Contratos Responses com ferramentas ou parâmetros sem equivalente
Codex são bloqueados antes da inferência, sem descartar silenciosamente requisitos.

## Matriz definida antes da validação

| Área | Cenário | Aceite |
|---|---|---|
| Caminho feliz | `pending` local → Responses → App Server simulado → schema → callback | Saída utilizável e IDs do job/thread/turno preservados |
| Isolamento | Capella sintética, outro produto e novo job com a mesma fala | Nenhum recibo, contexto ou resultado trocado entre jobs |
| Visão | Texto e screenshot de referência | Imagem transmitida ao turno, sem perda silenciosa |
| Validação | JSON inválido, campo faltante, tipo incorreto, turno incompleto | Falha explícita e recibo bruto preservado |
| Integração | URL do backend e API especializada de imagem | Filtro não intercepta endpoints que não são Responses textuais |
| Autenticação | Sessão ausente ou tipo API key | Bloqueio explícito, sem inferência nem fallback pago |
| Recuperação | Callback falha; mesmo job é reentregue | Replay do mesmo recibo sem nova inferência |
| Despacho incerto | Processo morre depois de `turn/start` | Marcador impede inferência automática duplicada |
| Segurança | Variáveis de API no worker, requisição de ferramenta | Segredos removidos do subprocesso; ferramenta recusada |
| Custo | Flex solicitado; transporte efetivamente default | Auditoria explicita a exceção e estima Standard; ausência de uso continua desconhecida |
| Runtime | Imagem versionada, Node/Codex, CA, entrypoint e persistência | Inicialização local e handshake real sem inferência |
| Compatibilidade | Modo Responses API previamente válido | Continua disponível somente por configuração explícita |
| Observabilidade | Falha, saída e retomada | Request/response crus, erro, tokens quando disponíveis, IDs e motivo do tier registrados |
| Navegadores | Nenhuma interface alterada | Não aplicável ao transporte; retomada operacional conferida no Chromium |
| Dados comerciais | Todos os testes com provedor/identidades locais sintéticos | Nenhuma compra, venda ou métrica comercial simulada como real |

## Evidências e limites

O MCP público apresentou certificado expirado; a mesma tool foi consultada pelo listener
operacional `191.252.210.83:8096`, confirmando banco e logs. O host do Worker não possui
sessão `auth.json` provisionada nos diretórios operacionais consultados. Não copiar a sessão
de outro agente nem seus tokens. Uma autenticação ChatGPT própria é necessária antes da
retomada funcional de Capella. Testes locais não comprovam disponibilidade de conta, saldo
de uso Codex, demanda, receita ou contribuição.

Fonte oficial consultada: [Codex App Server](https://learn.chatgpt.com/docs/app-server).

## Resultado local de 19/10/2026

- Java: 289 casos registrados, 286 executados sem falha e três exclusões preexistentes:
  um replay opcional de navegador e dois testes legados explicitamente desativados.
- Node: 23 casos aprovados; `npm audit` sem vulnerabilidades nas dependências fixadas.
- Integração: adapters produtivos do GeraSalesPage, fila `pending`, subprocesso do App
  Server simulado, schema, custo Standard e callbacks oficiais aprovados para três IDs.
  Replay após callback recusado executa uma única inferência; schema inválido e falta
  de sessão não aprovam a etapa. Ordem diferente das chaves mantém o mesmo recibo.
- Docker: imagem construída pelo Dockerfile versionado; Node 22.23.3 e Codex 0.159.3
  verificados; handshake real sem inferência; sessão ausente bloqueada; aplicação Spring
  com `{"status":"UP"}`, sem chave de API e sem rede externa. Topologia temporária removida.
- `bash -n`, ShellCheck e contrato da imagem/pipeline aprovados. A primeira sondagem
  local usou `curl`, inexistente na imagem-base; foi corrigida para usar o Node já
  instalado. O health produtivo continua consultado pelo host, sem essa dependência.
- Sem novas chamadas pagas, campanha, venda ou alteração de limite nesta validação.
- A proteção operacional revelou que CI e deploy estavam no mesmo workflow pausável.
  Foram comparados adiar a pausa, liberar o publicador durante a revisão e separar a
  validação sem publicação. Foi adotado `ai-worker-validation.yml`, reutilizado pelo
  publicador e disparado no PR. O teste de contrato proíbe publicação nessa validação.
- A regressão adicional de 70 KB falhou antes da correção: a resposta bruta estava
  incorporada ao campo de erro. Agora o recibo integral e a correlação do job continuam
  preservados; a mensagem operacional é curta e o callback de falha consegue ser registrado.
- Turnos incompletos e erros JSON-RPC também conservam entrada, eventos brutos, uso
  disponível e IDs em auditoria durável, sem liberar nova inferência automática.
