# Fluxo de Pull Request de Teste

Este documento descreve um fluxo enxuto para criar um Pull Request apenas para fins de teste. Use-o quando quiser validar integrações de CI ou permissões do repositório sem introduzir mudanças funcionais.

## Passo a passo sugerido

1. **Criar uma branch temporária**
   ```bash
   git checkout -b chore/test-pr-$(date +%Y%m%d)
   ```
2. **Adicionar uma alteração neutra**
   - Prefira atualizar documentação ou adicionar um arquivo de nota, mantendo o escopo mínimo.
3. **Executar validações rápidas**
   - Rode linters ou testes obrigatórios para garantir que o PR passe pelos mesmos estágios da pipeline oficial.
4. **Comitar com mensagem clara**
   ```bash
   git commit -am "chore: add placeholder change for test PR"
   ```
5. **Abrir o Pull Request**
   - Descreva que o objetivo é apenas testar o fluxo.
   - Marque revisores responsáveis pelo ambiente que deseja exercer.
6. **Encerrar o teste**
   - Após validar, feche ou faça merge apenas se a alteração for realmente desejada.

## Boas práticas

- Documente o motivo do teste no corpo do PR.
- Evite tocar em código crítico para não gerar deploys acidentais.
- Remova a branch temporária ao finalizar (`git branch -D <branch>`).

## Histórico

- Criado para validar rotinas de teste de PR em ambiente isolado.
