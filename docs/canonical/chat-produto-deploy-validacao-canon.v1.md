# Cânone v1 — Deploy e validação de chats de produto

## Objetivo

Este cânone define o procedimento operacional para criar, publicar e diagnosticar chats de produto do Marketing Hub, usando o caso do Chat Moda como referência.

Chats de produto devem funcionar como experiência vendável e confiável. Portanto, a validação não termina quando o container sobe: ela termina quando a tela pública envia mensagem, o backend encaminha corretamente, o serviço executor responde com IA real e os logs confirmam o caminho ponta a ponta.

## Arquitetura canônica

Todo chat de produto deve seguir este fluxo:

1. Frontend chama o backend principal por `/api/...`.
2. Backend principal valida e encaminha a solicitação ao serviço de chat.
3. Serviço de chat conversa com o provedor de IA ou app server autorizado.
4. Serviço de chat devolve resposta estruturada ao backend.
5. Backend devolve resposta para a tela.

O frontend não deve chamar diretamente o serviço de chat quando estiver publicado. O serviço de chat também não deve acessar banco diretamente para substituir responsabilidade do backend.

## Regra de proxy do frontend

Todo frontend publicado em container Nginx que chame endpoints do backend precisa ter proxy explícito para `/api/`.

Falha típica:

- a tela parece estar conectada;
- health checks funcionam;
- chamadas diretas ao backend funcionam;
- mas a tela retorna `HTTP 405` ao enviar mensagem.

Causa-raiz provável:

- Nginx do frontend está servindo apenas arquivos estáticos e tratando `/api/...` como rota local, sem proxy para o backend.

Correção canônica:

- configurar `location /api/` no Nginx do frontend;
- encaminhar para o backend principal;
- preservar headers essenciais como `Host`, `X-Real-IP` e `X-Forwarded-For`;
- reconstruir e republicar o container do frontend.

## Regra de contrato com app server de IA

Ao integrar um chat com app server ou provedor externo, a implementação deve aceitar somente contratos reais observados e validados por teste.

No caso do Chat Moda, a causa-raiz secundária foi divergência de contrato:

- o serviço esperava `threadId` no topo da resposta;
- o app server retornava `thread.id`.

Correção canônica:

- mapear o contrato real da resposta;
- aceitar o campo canônico retornado pelo provedor;
- manter compatibilidade defensiva apenas quando ela não esconder erro de contrato;
- registrar logs suficientes para diferenciar resposta real de fallback local.

## Regra de modelo e autorização

Não considerar integração funcional apenas porque a conexão foi autenticada.

Também é obrigatório validar:

- modelo aceito pela conta usada;
- formato de input esperado pelo provedor;
- resposta efetiva gerada por IA;
- ausência de fallback silencioso.

No caso do Chat Moda, o serviço estava autenticado, mas o modelo configurado inicialmente era recusado pela conta. A correção foi usar modelo aceito pelo ambiente e validar resposta real pelo backend.

## Procedimento obrigatório de diagnóstico

Quando um chat publicado apresentar erro na tela, seguir esta ordem:

1. Identificar o endpoint chamado pela tela.
2. Testar o mesmo endpoint pela URL pública do frontend.
3. Testar o endpoint equivalente diretamente no backend.
4. Verificar se o erro muda entre frontend e backend.
5. Se o erro existir apenas pelo frontend, investigar proxy, Nginx, cache ou build publicado.
6. Se o erro existir no backend, investigar controller, service, contrato com o serviço de chat e logs.
7. Acessar o host do container por SSH quando necessário.
8. Ver logs do container durante envio real de mensagem.
9. Confirmar se a resposta veio de IA real ou de fallback local.
10. Corrigir a causa-raiz e repetir o teste pela tela publicada.

## Critério de conclusão

Um chat de produto só pode ser considerado ajustado quando todos os itens abaixo forem verdadeiros:

- container do serviço está saudável;
- backend principal consegue chamar o serviço;
- frontend publicado consegue chamar `/api/...` sem `405`;
- envio real de mensagem pela tela retorna `200`;
- resposta indica modo real de integração, não fallback local;
- logs do container confirmam recebimento, processamento e resposta;
- testes automatizados relevantes passam no módulo alterado.

## Caso de referência: Chat Moda

Em 2026-07-14, o Chat Moda foi diagnosticado com duas causas principais:

1. Nginx do frontend publicado não fazia proxy de `/api/`, gerando `HTTP 405` na tela.
2. Serviço de chat interpretava incorretamente a resposta do app server e também usava modelo não aceito pelo ambiente.

A correção aplicada foi:

- adicionar proxy `/api/` no Nginx do frontend;
- ajustar o serviço para ler `thread.id`;
- usar formato de input compatível com o app server;
- configurar modelo aceito pelo ambiente;
- publicar o serviço no host do container;
- validar chamada real pelo backend.

## Regra para próximos chats

Todo novo chat de produto deve nascer com:

- endpoint backend canônico;
- proxy `/api/` validado no frontend publicado;
- logs de request, resposta, modo de execução e erro;
- teste automatizado do serviço de chat;
- teste manual ponta a ponta pela tela;
- validação explícita de que a resposta veio da integração real.

Não basta o chat estar conectado. Ele precisa responder pela tela publicada usando o fluxo real do produto.
