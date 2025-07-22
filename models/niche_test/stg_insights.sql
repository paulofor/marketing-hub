-- staging table for facebook insights
select *
from raw_insights
where date(timestamp) = current_date;
