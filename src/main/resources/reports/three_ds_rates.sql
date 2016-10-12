select
  Merchant_Account
, date_format(Creation_Date, '%Y-%m-%d') day
, Payment_Method
, system
, count(*) count
, sum(three_ds_triggered) three_ds_triggered
, sum(three_ds_authenticated) three_ds_authenticated
, sum(three_ds_triggered) / count(*) * 100 three_ds_triggered_rate
, ifnull(sum(three_ds_authenticated) / sum(three_ds_triggered) * 100, 0.0000) three_ds_authenticated_rate
from (
  select
    Merchant_Account
  , Creation_Date
  , Payment_Method
  , case when Merchant_Reference REGEXP '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}' then 'kb' else 'other' end system
  , case when 3D_Directory_Response = 'Y' then 1 else 0 end 'three_ds_triggered'
  , case when 3D_Authentication_Response IN ('Y', 'A') then 1 else 0 end 'three_ds_authenticated'
  from received_payments_report
  where 1 = 1
) three_ds_rates
group by 1,2,3,4
order by 1,2,3,4
;
