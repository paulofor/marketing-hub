#!/usr/bin/env python3
"""Double local do modelo; não acessa rede e usa apenas o snapshot entregue pelo worker real."""
import json,sys,pathlib
args=sys.argv[1:]
assert args[:2]==['exec','-']
assert 'read-only' in args and 'features.shell_tool=false' in args
assert '--ignore-user-config' in args and '--ephemeral' in args and 'features.hooks=false' in args
prompt=sys.stdin.read()
context=json.loads(prompt.split('## Contexto oficial da execução\n\n')[-1])
assert context['contractVersion']=='LEARNING_CYCLE_DECISION_PROPOSAL_V1'
assert context['productId'] in (91001,91002)
assert context['cycleId'] > 0 and context['cycleRevision'] > 0
schema=json.loads(pathlib.Path(args[args.index('--output-schema')+1]).read_text())
assert schema['additionalProperties'] is False
commands=[c['action'] for c in context['commands'] if c['available']]
action=next(a for a in ['ADJUST','INCONCLUSIVE','FIX_MEASUREMENT','STOP'] if a in commands)
target=next((t for t in context['returnTargets'] if t['activityId']=='rework'),None)
result=dict(contractVersion=context['contractVersion'],action=action,summary='Proposta simulada de Atena: preservar a referência e testar uma melhoria útil.',rootCause='Hipótese: o primeiro resultado pode exigir esforço; a amostra não comprova a causa.',learning='As sessões observadas não demonstram demanda nem rejeição; comparar explicação concorrente de tráfego.',nextHypothesis='Reduzir o esforço da primeira ação e medir continuidade até compra em um novo experimento.',evidenceLimits='Modelo simulado na sandbox. Poucas sessões e nenhuma compra não comprovam causa de abandono.',correctionPlan='Validar a fonte canônica e a segregação antes de decidir.',scaleHypothesis='Somente reavaliar expansão após vendas líquidas, contribuição e entrega comprovadas.',returnProcessId=target['processDefinitionId'] if action=='ADJUST' else None,returnActivityId=target['activityId'] if action=='ADJUST' else None,evidenceEventIds=[context['measurementEventId']],selectedAlternative=0,alternatives=[dict(option=o,benefit='Preserva aprendizado verificável.',risk='Amostra limitada; não inferir causalidade.',effort='Baixo a médio, sujeito aos gates.',salesImpact='Hipótese de melhora na continuidade; sem estimativa de receita.') for o in ['Melhorar o primeiro resultado','Rever clareza da comunicação','Coletar mais evidências dentro dos limites']])
assert set(result)==set(schema['required'])
pathlib.Path(args[args.index('--output-last-message')+1]).write_text(json.dumps(result,ensure_ascii=False))
print(json.dumps({'type':'turn.completed','usage':{'input_tokens':500,'output_tokens':300}}))
