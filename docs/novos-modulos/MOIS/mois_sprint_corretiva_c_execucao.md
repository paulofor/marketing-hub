# MOIS — Sprint corretiva C (execução)

## Objetivo

Executar a integração backend ↔ MOIS com contrato explícito via HTTP, removendo o comportamento de implementação local do domínio MOIS dentro do backend.

## Entregas realizadas

1. Criação do gateway de integração do backend com o serviço separado MOIS:
   - `MoisModuleGateway`
   - `MoisModuleProperties`
2. Atualização do controller institucional do backend (`/api/v1/mois/*`) para atuar como façade de integração:
   - chamadas HTTP para o módulo `mois` em vez de execução local do domínio.
3. Configuração de integração adicionada no backend:
   - `integrations.mois.module.base-url`
   - `integrations.mois.module.connect-timeout`
   - `integrations.mois.module.read-timeout`
4. Atualização de teste de contrato web do backend para mockar o gateway HTTP, validando a borda de integração.

## Observação de compatibilidade transitória

Nesta sprint corretiva, o backend mantém a URL institucional `/api/v1/mois/*` para clientes já integrados, mas a execução de domínio passa a ser delegada ao serviço separado `mois`.
