# Matriz de homologação — versionamento de criativos v1

## Objetivo

Permitir corrigir criativos de experimentos pausados sem sobrescrever evidências nem liberar publicação ou gasto.

| Área | Cenário | Resultado obrigatório |
| --- | --- | --- |
| Caminho feliz | Criar revisão com imagem, mensagem, CTA e destino | Novo ID, origem preservada, versão sequencial e status `DRAFT` |
| Validação | Omitir imagem ou destino na tela | Salvamento bloqueado |
| Falha | Backend rejeitar a criação | Original intacto e erro visível |
| Auditoria | Payload tentar enviar `READY` | Backend persiste `DRAFT` e parecer `PENDING` |
| Integração | Listar criativos após salvar | Original e revisão coexistem no mesmo experimento |
| Observabilidade | Falha no endpoint | Log inclui operação, ID de origem e stack trace |
| Métricas | Criar revisão | Não altera vendas, campanha, gasto ou status da Meta |
| Segregação | Versionar criativo de um experimento | Revisão permanece no experimento da origem |
| Desktop | Chromium desktop | Modal e campos operáveis |
| Mobile | iPhone 15 Pro e Pixel 7 | Modal rolável e ação operável por toque |

Uma rodada completa executa testes funcionais de backend e frontend, TypeScript/build e revisão dos gates. São exigidas cinco rodadas locais completas e consecutivas; qualquer falha reinicia a contagem.
