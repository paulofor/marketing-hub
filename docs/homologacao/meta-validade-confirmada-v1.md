# Validade Meta confirmada por inspeção — 21/10/2026

## Evidência e alternativas

Capella, ciclo 12/experimento 105, conserva dez gates aprovados e a credencial como
único impedimento de distribuição. Em 21/10 a troca oficial retornou HTTP 200,
`access_token` e `token_type`, sem `expires_in`. A inspeção das duas credenciais
confirmou mesmo aplicativo, titular, tipo USER e permissões. A candidata retornou
`is_valid=true`, `expires_at=0` e `data_access_expires_at` positivo futuro. Nenhuma
credencial integra esta evidência. O worker descartava a candidata exclusivamente
pela ausência da duração; a credencial anterior vencia no mesmo dia.

Alternativas comparadas: usar a credencial curta mantém a interrupção; presumir
sessenta dias inventa validade; inspecionar a candidata e conservar o menor limite
positivo oficial permite recuperar o acesso sem inventar prazo. Adotada a terceira.
Zero explícito de expiração só pode ser admitido com inspeção válida e um horizonte
positivo confirmado de acesso a dados. Ausência, tipo incorreto e negativo não
significam prazo ilimitado. Revogação continua possível e exige validação operacional.

## Matriz local definida antes da alteração

| Percurso | Aceite |
| --- | --- |
| Troca sem duração, expiração zero e dados futuros | Mesma identidade, escopos e callback com horizonte oficial exato. |
| Outra conta e duração antes válida | Isolamento e prazo positivo anterior preservados. |
| Expiração e dados com prazos diferentes | Menor prazo positivo prevalece. |
| Dados vencidos, metadados ausentes, tipo errado ou identidade divergente | Preservar credencial anterior e registrar falha; nunca presumir sessenta dias. |
| Falha HTTP/transporte/callback | Sem repetir troca, sem mudar token de campanha em curso e sem expor segredos. |
| Comando de tela → controller → Meta simulada → banco | Aceitar apenas candidata conferida, preservar mídia e automação. |
| Observabilidade e segregação | Identidades e segredos sintéticos, sem Meta, cobrança, SMTP ou eventos comerciais reais. |
| Desktop, iPhone, Pixel | Reutilizar tela existente para salvar e conferir o resultado após a entrega. Nenhuma mudança de layout. |

## Fontes oficiais

- https://developers.facebook.com/docs/facebook-login/guides/access-tokens/get-long-lived/
- https://developers.facebook.com/docs/graph-api/reference/debug_token/

Consultadas pelo `meta_docs_get` do MCP com TLS validado; o navegador de pesquisa
recebeu limitação HTTP 429. A inspeção comprova capacidade de acesso e validade
operacional naquele instante, não entrega de anúncio nem venda.

## Resultado

Os 206 testes do Facebook Ads Worker passaram. No backend, 152 testes passaram,
incluindo H2, controllers, troca administrativa, recuperação e arquitetura. A regressão
antes da correção recusava a duração omitida e não fazia a inspeção; agora o callback
persiste o horizonte absoluto correto. A primeira rodada de arquitetura rejeitou uma
dependência do pacote legado para o canônico: o cliente comum foi posicionado em
`com.marketinghub.ads`, sem ampliar permissões da regra. As primeiras execuções amplas
foram interrompidas após limite de processos da sandbox; fechar navegadores e limitar
somente os processos Maven a quatro CPUs permitiu concluir a rodada. Há pouco espaço
livre no filesystem; nenhuma informação de produto ou recurso externo foi removido.
Não houve chamadas pagas, campanha, SMTP ou evento comercial nos testes.

Publicação e retomada ainda precisam de conferência. A retomada deve preservar
R$ 100 totais/5 dias, meta de 30% para continuidade/escala, autorizações separadas de
IA/audiovisual e todos os pareceres aceitos. Não promover experimento por atualização
direta de status nem fabricar gate de exposição.
