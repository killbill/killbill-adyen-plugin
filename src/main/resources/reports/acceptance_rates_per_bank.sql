select
  auth_rates.Merchant_Account
, date_format(Creation_Date, '%Y-%m-%d') day
, auth_rates.Issuer_Name
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
  , Creation_Date
  , Issuer_Name
  , case when Merchant_Reference REGEXP '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}' then 'kb' else 'other' end system
  , case when Acquirer_Response = 'APPROVED' then 1 else 0 end 'success'
  , case when Acquirer_Response = 'FRAUD' then 1 else 0 end 'fraud'
  , case when Acquirer_Response NOT IN ('APPROVED', 'FRAUD') then 1 else 0 end 'rejected'
  from received_payments_report
  where 1 = 1
) auth_rates
join (
  select
    Merchant_Account
  , Issuer_Name
  , @curRank := @curRank + 1 as rank
  from (
    select
      Merchant_Account
    , Issuer_Name
    , count(*) count
    from received_payments_report
    group by 1,2
    order by 3 desc
  ) banks, (select @curRank := 0) r
  order by 1 asc, 3 desc
) ordered_banks using (Merchant_Account, Issuer_Name)
group by 1,2,3,4
order by 1,rank,3
;
