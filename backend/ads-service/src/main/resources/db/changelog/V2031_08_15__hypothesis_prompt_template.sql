--liquibase formatted sql

--changeset repo:2031-08-15-hypothesis-prompt-template dbms:mysql
INSERT INTO prompt (name, domain, template, active, created_at, updated_at)
VALUES (
    'Geração de hipóteses de nicho (padrão)',
    'NICHE_HYPOTHESIS',
    CONCAT(
        'Gere ${quantity} hipóteses em formato JSON.\n',
        'Use o seguinte nicho como contexto:\n',
        'Nome: ${niche.name}\n',
        '<#if niche.description?has_content>Descrição: ${niche.description}</#if>\n',
        '<#if niche.baseSegmentation?has_content>Segmentação base: ${niche.baseSegmentation}</#if>\n',
        '<#if niche.interests?has_content>Interesses: ${niche.interests}</#if>\n',
        '<#if niche.demographicFilters?has_content>Filtros demográficos: ${niche.demographicFilters}</#if>\n',
        '<#if niche.extraTips?has_content>Dicas extras: ${niche.extraTips}</#if>\n',
        '<#if attributes?has_content>\n',
        'Cada objeto deve conter as chaves: <#list attributes as attr>"${attr.name}"<#if attr_has_next>, </#if></#list>.\n',
        '<#list attributes as attr>Campo "${attr.name}": ${attr.description}. </#list>\n',
        '<#else>\n',
        'Cada objeto deve conter as chaves: "title", "promise", "problem", "persona", "mechanism", "uniqueMechanism", "entrega", "successRule", "offerType", "price".\n',
        '</#if>\n',
        'O campo "offerType" deve ser "LEAD" ou "TRIPWIRE".\n',
        'O campo "price" deve ser um número.\n',
        'Retorne apenas um array JSON com esses objetos, sem texto adicional.'
    ),
    TRUE,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    template = VALUES(template),
    active = VALUES(active),
    updated_at = VALUES(updated_at);
