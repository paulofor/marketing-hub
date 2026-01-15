--liquibase formatted sql
--changeset repo:2031-09-10-hypothesis-prompt-detailed-description dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM prompt WHERE domain = 'NICHE_HYPOTHESIS' AND name = 'Geração de hipóteses de nicho (padrão)';
UPDATE prompt
SET template = CONCAT(
    'Gere ${quantity} hipóteses em formato JSON.\n',
    'Use o seguinte nicho como contexto:\n',
    'Nome: ${niche.name}\n',
    '<#if niche.description?has_content>Descrição: ${niche.description}</#if>\n',
    '<#if niche.baseSegmentation?has_content>Segmentação base: ${niche.baseSegmentation}</#if>\n',
    '<#if niche.interests?has_content>Interesses: ${niche.interests}</#if>\n',
    '<#if niche.demographicFilters?has_content>Filtros demográficos: ${niche.demographicFilters}</#if>\n',
    '<#if niche.extraTips?has_content>Dicas extras: ${niche.extraTips}</#if>\n',
    '<#if niche.latestDetailedDescription?has_content>\n',
    'Use a descrição detalhada mais recente (a última por createdAt/id) como referência:\n',
    '<#if niche.latestDetailedDescription.title?has_content>Título: ${niche.latestDetailedDescription.title}</#if>\n',
    '<#if niche.latestDetailedDescription.description?has_content>Descrição: ${niche.latestDetailedDescription.description}</#if>\n',
    '<#if niche.latestDetailedDescription.pains?has_content>Dores: ${niche.latestDetailedDescription.pains}</#if>\n',
    '<#if niche.latestDetailedDescription.desires?has_content>Desejos: ${niche.latestDetailedDescription.desires}</#if>\n',
    '<#if niche.latestDetailedDescription.needs?has_content>Necessidades: ${niche.latestDetailedDescription.needs}</#if>\n',
    '</#if>\n',
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
    updated_at = NOW()
WHERE domain = 'NICHE_HYPOTHESIS'
  AND name = 'Geração de hipóteses de nicho (padrão)';
