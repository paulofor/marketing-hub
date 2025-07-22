-- fact table aggregated from insights
select campaign_id,
       sum(clicks) as clicks,
       sum(spend) as spend,
       sum(purchases) as purchases
from {{ ref('stg_insights') }}
group by campaign_id;
