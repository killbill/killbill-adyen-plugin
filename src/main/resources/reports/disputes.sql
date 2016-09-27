select
  Merchant_Account
, Payment_Method
, system
, sum(lost) lost
, sum(won) won
, sum(in_progress) in_progress
, sum(possible_chargeback) possible_chargeback
from (
  select
    Merchant_Account
  , Payment_Method
  , case when Merchant_Reference REGEXP '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}' then 'kb' else 'other' end system
  , case when Record_Type = 'Chargeback' then 1 else 0 end 'lost'
  , case when Record_Type = 'ChargebackReversed' then 1 else 0 end 'won'
  , case when Record_Type IN ('InformationSupplied', 'NotificationOfChargeback') then 1 else 0 end 'in_progress'
  , case when Record_Type IN ('RequestForInformation', 'NotificationOfFraud') then 1 else 0 end 'possible_chargeback'
  from dispute_report final_state
  where not exists (
    select *
    from dispute_report high
    where 1 = 1
    and high.Merchant_Reference = final_state.Merchant_Reference
    and high.Record_Date > final_state.Record_Date
  )
) disputes
group by 1,2,3
order by 1,2,3
;
