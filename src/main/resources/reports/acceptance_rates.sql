select
  Merchant_Account
, Payment_Method
, system
, count(*) count
, sum(success) success
, sum(fraud) fraud
, sum(rejected) rejected
, sum(success) / count(*) * 100 success_rate
, sum(fraud) / count(*) * 100 fraud_rate
, sum(rejected) / count(*) * 100 rejected_rate
from (
  select
    Merchant_Account
  , Payment_Method
  , case when Merchant_Reference REGEXP '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}' then 'kb' else 'other' end system
  , case when Acquirer_Response = 'APPROVED' then 1 else 0 end 'success'
  , case when Acquirer_Response = 'FRAUD' then 1 else 0 end 'fraud'
  , case when Acquirer_Response NOT IN ('APPROVED', 'FRAUD') then 1 else 0 end 'rejected'
  from received_payments_report
  where 1 = 1
) auth_rates
group by 1,2,3
order by 1,2,3
;
