#!/usr/bin/env python3
"""Gera a revisão textual da cadeia a partir dos contratos e grafos versionados da fixture."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
FIXTURE = Path(__file__).parent
CHANGES = ROOT / 'backend/ads-service/src/main/resources/db/changelog/changesets'
NAME = '2026-10-02-pde-satisfaction-continuity-v1'
MARKER = 'SATISFACTION_CONTINUITY_V1'
SOURCES = json.loads((FIXTURE / 'sources.json').read_text())
REQUIREMENTS = json.loads((FIXTURE / 'requirements.json').read_text())


def literal(value):
    """Escapa valores textuais como literais SQL sem interpolar comandos externos."""
    return "'" + value.replace("'", "''") + "'"


sql = []
checks = []
targets = []
for source in SOURCES:
    code = source['processCode']
    spec = REQUIREMENTS[code]
    previous, target = spec['sourceVersion'], spec['targetVersion']
    paths = []
    nodes = source['diagram']['nodes']
    positions = {node['id']: position for position, node in enumerate(nodes)}
    checks.append((1, f"SELECT COUNT(*) FROM business_process_definition WHERE process_code={literal(code)} AND version_number={previous} AND status='PUBLISHED' AND JSON_LENGTH(JSON_EXTRACT(diagram_json,'$.nodes'))={len(nodes)}" + ''.join(f" AND JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.nodes[{positions[activity]}].id'))={literal(activity)} AND JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.nodes[{positions[activity]}].type'))='TASK'" for activity in spec['objectives'])))
    for activity, objective in spec['objectives'].items():
        paths.extend([literal(f'$.nodes[{positions[activity]}].description'), literal(objective)])
    paths.extend([literal('$.satisfactionContinuityVersion'), literal(MARKER)])
    path_sql = ',\n  '.join(paths)
    targets.append(f"(process_code={literal(code)} AND version_number={target})")
    sql.append(f"""-- {code}: v{previous} -> v{target}; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,{target},'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,\n  {path_sql}),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number={target}
WHERE source.process_code={literal(code)} AND source.version_number={previous} AND existing.id IS NULL;
""")

selection = '(' + ' OR '.join(targets) + ')'
checks.extend([
 (1, "SELECT COUNT(*) FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=24 AND status='PUBLISHED'"),
 (1, "SELECT IF(COUNT(*)=6 AND COUNT(DISTINCT sequence_number)=6,1,0) FROM business_process_chain_item item JOIN business_process_chain_definition chain ON chain.id=item.chain_definition_id WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=24"),
 (0, "SELECT COUNT(*) FROM business_process_definition WHERE " + selection + f" AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.satisfactionContinuityVersion')),'')<>{literal(MARKER)}"),
 (0, "SELECT COUNT(*) FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=25 AND purpose NOT LIKE '%SATISFACTION_CONTINUITY_V1%'")
])
macros = SOURCES[:6]
for index, source in enumerate(macros, 1):
    checks.append((1, f"SELECT COUNT(*) FROM business_process_chain_item item JOIN business_process_chain_definition chain ON chain.id=item.chain_definition_id JOIN business_process_definition process ON process.id=item.process_definition_id WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=24 AND item.sequence_number={index} AND process.process_code={literal(source['processCode'])} AND process.version_number={source['versionNumber']}"))

numbers = ' UNION ALL '.join('SELECT ' + str(n) + (' AS number' if n == 0 else '') for n in range(max(len(s['diagram']['nodes']) for s in SOURCES)))
sql.append(f"""-- A definição relacional usa exatamente o nó versionado consumido pela tela e pela missão BPM.
INSERT INTO business_process_activity_definition
 (process_definition_id,activity_id,name,objective,owner_name,execution_resource_code,subprocess_code,definition_json,created_at)
SELECT process.id,
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].id'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].label'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].description'))),
 NULLIF(JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].owner'))),'null'),
 NULLIF(JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].executionResourceCode'))),'null'),
 NULLIF(JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].subprocessCode'))),'null'),
 JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,']')),UTC_TIMESTAMP(6)
FROM business_process_definition process
CROSS JOIN ({numbers}) node
LEFT JOIN business_process_activity_definition existing
 ON existing.process_definition_id=process.id
 AND existing.activity_id=JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].id')))
WHERE JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,'$.satisfactionContinuityVersion'))='{MARKER}'
 AND {selection}
 AND JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].type')))='TASK'
 AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
 (chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at,published_at)
SELECT source.chain_code,source.name,
 CONCAT(source.purpose,' SATISFACTION_CONTINUITY_V1: situação reconhecível → benefício desejado → experiência real → produto identificável → motivo legítimo para voltar; sem entrevistas ou opinião solicitada.'),
 source.outcome_description,'Compras líquidas, entrega útil, contribuição e retorno voluntário atribuível',25,'PUBLISHED',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
LEFT JOIN business_process_chain_definition existing ON existing.chain_code=source.chain_code AND existing.version_number=25
WHERE source.chain_code='pde-value-creation-delivery' AND source.version_number=24 AND existing.id IS NULL;
""")
contributions = [
 'Argos reúne situações, linguagem pública, satisfação possível, fontes e contrapontos, sem escolher estratégia nem provar demanda.',
 'Atena escolhe uma satisfação como hipótese; Plutus limita custos, inclusive amostras sem compra; Dédalo liga ação, resultado, identidade e ocasião de retorno.',
 'Dédalo constrói resultado fiel, aplicável e recuperável; Psique e Têmis avaliam a mesma versão sem alegar satisfação humana.',
 'Íris e Apolo mostram situação, participação do produto e satisfação possível com identidade contínua; o backend integra prova e continuidade paga.',
 'Preparação, revisões e preflight comprovam promessa, versão, compra/entrega, eventos e limites; autorização humana permanece separada.',
 'Hermes, entrega e Plutus conciliam compras, uso, reembolso e contribuição; o ciclo preserva hipóteses, contrapontos e retorno à atividade da causa.'
]
for index, source in enumerate(macros, 1):
    target = REQUIREMENTS[source['processCode']]['targetVersion']
    sql.append(f"""INSERT INTO business_process_chain_item
 (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT chain.id,process.id,{index},{literal(contributions[index - 1])},UTC_TIMESTAMP(6)
FROM business_process_chain_definition chain
JOIN business_process_definition process ON process.process_code={literal(source['processCode'])} AND process.version_number={target}
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=chain.id AND existing.sequence_number={index}
WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=25 AND existing.id IS NULL;
""")
sql.append(f"""-- Reaplicação após rollback preserva linhas, custos, aprovações e disponibilidade das versões anteriores.
UPDATE business_process_definition SET status='PUBLISHED',published_at=COALESCE(published_at,UTC_TIMESTAMP(6))
WHERE {selection} AND JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.satisfactionContinuityVersion'))='{MARKER}';
UPDATE business_process_chain_definition SET status='PUBLISHED',published_at=COALESCE(published_at,UTC_TIMESTAMP(6))
WHERE chain_code='pde-value-creation-delivery' AND version_number=25 AND purpose LIKE '%SATISFACTION_CONTINUITY_V1%';
""")
(CHANGES / (NAME + '.sql')).write_text('\n'.join(sql))
yaml = 'databaseChangeLog:\n  - changeSet:\n      id: ' + NAME + '\n      author: codex\n      preConditions:\n        - onFail: HALT\n        - onError: HALT\n        - dbms:\n            type: mysql\n'
for expected, query in checks:
    yaml += f'        - sqlCheck:\n            expectedResult: {expected}\n            sql: >-\n              {query}\n'
yaml += f'''      changes:
        - sqlFile:
            path: {NAME}.sql
            relativeToChangelogFile: true
            splitStatements: true
            stripComments: true
      rollback:
        - sql:
            splitStatements: true
            stripComments: true
            sql: |
              UPDATE business_process_definition SET status='RETIRED' WHERE {selection} AND JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.satisfactionContinuityVersion'))='{MARKER}';
              UPDATE business_process_chain_definition SET status='RETIRED' WHERE chain_code='pde-value-creation-delivery' AND version_number=25 AND purpose LIKE '%SATISFACTION_CONTINUITY_V1%';
'''
(CHANGES / (NAME + '.yaml')).write_text(yaml)
print(f'{len(SOURCES)} definições e {sum(len(r["objectives"]) for r in REQUIREMENTS.values())} objetivos gerados.')
