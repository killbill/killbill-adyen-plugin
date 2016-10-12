select
  Merchant_Account
, date_format(Creation_Date, '%Y-%m-%d') day
, Payment_Method
, system
, count(*) count
, sum(APPROVED) / count(*) * 100 APPROVED_rate
, sum(FRAUD) / count(*) * 100 FRAUD_rate
, sum(DECLINED) / count(*) * 100 DECLINED_rate
, sum(NOT_ENOUGH_BALANCE) / count(*) * 100 NOT_ENOUGH_BALANCE_rate
, sum(BLOCK_CARD) / count(*) * 100 BLOCK_CARD_rate
, sum(CVC_DECLINED) / count(*) * 100 CVC_DECLINED_rate
, sum(CARD_EXPIRED) / count(*) * 100 CARD_EXPIRED_rate
/*, sum(APPROVED) APPROVED */
/*, sum(BLOCK_CARD) BLOCK_CARD */
/*, sum(CARD_EXPIRED) CARD_EXPIRED */
/*, sum(CVC_DECLINED) CVC_DECLINED */
/*, sum(DECLINED) DECLINED */
/*, sum(DECLINED_NON_GENERIC) DECLINED_NON_GENERIC */
/*, sum(ERROR) ERROR */
/*, sum(FRAUD) FRAUD */
/*, sum(INVALID_CARD) INVALID_CARD */
/*, sum(ISSUER_UNAVAILABLE) ISSUER_UNAVAILABLE */
/*, sum(NOT_3D_AUTHENTICATED) NOT_3D_AUTHENTICATED */
/*, sum(NOT_ENOUGH_BALANCE) NOT_ENOUGH_BALANCE */
/*, sum(NOT_SUPPORTED) NOT_SUPPORTED */
/*, sum(PIN_TRIES_EXCEEDED) PIN_TRIES_EXCEEDED */
/*, sum(REFERRAL) REFERRAL */
/*, sum(RESTRICTED_CARD) RESTRICTED_CARD */
/*, sum(REVOCATION_OF_AUTH) REVOCATION_OF_AUTH */
/*, sum(TRANSACTION_NOT_PERMITTED) TRANSACTION_NOT_PERMITTED */
/*, sum(WITHDRAWAL_COUNT_EXCEEDED) WITHDRAWAL_COUNT_EXCEEDED */
/*, sum(DECLINED_NON_GENERIC) / count(*) * 100 DECLINED_NON_GENERIC_rate */
/*, sum(ERROR) / count(*) * 100 ERROR_rate */
/*, sum(INVALID_CARD) / count(*) * 100 INVALID_CARD_rate */
/*, sum(ISSUER_UNAVAILABLE) / count(*) * 100 ISSUER_UNAVAILABLE_rate */
/*, sum(NOT_3D_AUTHENTICATED) / count(*) * 100 NOT_3D_AUTHENTICATED_rate */
/*, sum(NOT_SUPPORTED) / count(*) * 100 NOT_SUPPORTED_rate */
/*, sum(PIN_TRIES_EXCEEDED) / count(*) * 100 PIN_TRIES_EXCEEDED_rate */
/*, sum(REFERRAL) / count(*) * 100 REFERRAL_rate */
/*, sum(RESTRICTED_CARD) / count(*) * 100 RESTRICTED_CARD_rate */
/*, sum(REVOCATION_OF_AUTH) / count(*) * 100 REVOCATION_OF_AUTH_rate */
/*, sum(TRANSACTION_NOT_PERMITTED) / count(*) * 100 TRANSACTION_NOT_PERMITTED_rate */
/*, sum(WITHDRAWAL_COUNT_EXCEEDED) / count(*) * 100 WITHDRAWAL_COUNT_EXCEEDED_rate */
from (
  select
    Merchant_Account
  , Creation_Date
  , Payment_Method
  , case when Merchant_Reference REGEXP '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}' then 'kb' else 'other' end system
  , case when Acquirer_Response = 'APPROVED' then 1 else 0 end APPROVED
  , case when Acquirer_Response = 'BLOCK_CARD' then 1 else 0 end BLOCK_CARD
  , case when Acquirer_Response = 'CARD_EXPIRED' then 1 else 0 end CARD_EXPIRED
  , case when Acquirer_Response = 'CVC_DECLINED' then 1 else 0 end CVC_DECLINED
  , case when Acquirer_Response = 'DECLINED' then 1 else 0 end DECLINED
  , case when Acquirer_Response = 'DECLINED_NON_GENERIC' then 1 else 0 end DECLINED_NON_GENERIC
  , case when Acquirer_Response = 'ERROR' then 1 else 0 end ERROR
  , case when Acquirer_Response = 'FRAUD' then 1 else 0 end FRAUD
  , case when Acquirer_Response = 'INVALID_CARD' then 1 else 0 end INVALID_CARD
  , case when Acquirer_Response = 'ISSUER_UNAVAILABLE' then 1 else 0 end ISSUER_UNAVAILABLE
  , case when Acquirer_Response = 'NOT_3D_AUTHENTICATED' then 1 else 0 end NOT_3D_AUTHENTICATED
  , case when Acquirer_Response = 'NOT_ENOUGH_BALANCE' then 1 else 0 end NOT_ENOUGH_BALANCE
  , case when Acquirer_Response = 'NOT_SUPPORTED' then 1 else 0 end NOT_SUPPORTED
  , case when Acquirer_Response = 'PIN_TRIES_EXCEEDED' then 1 else 0 end PIN_TRIES_EXCEEDED
  , case when Acquirer_Response = 'REFERRAL' then 1 else 0 end REFERRAL
  , case when Acquirer_Response = 'RESTRICTED_CARD' then 1 else 0 end RESTRICTED_CARD
  , case when Acquirer_Response = 'REVOCATION_OF_AUTH' then 1 else 0 end REVOCATION_OF_AUTH
  , case when Acquirer_Response = 'TRANSACTION_NOT_PERMITTED' then 1 else 0 end TRANSACTION_NOT_PERMITTED
  , case when Acquirer_Response = 'WITHDRAWAL_COUNT_EXCEEDED' then 1 else 0 end WITHDRAWAL_COUNT_EXCEEDED
  from received_payments_report
  where 1 = 1
) error_rates
where 1 = 1
group by 1,2,3,4
order by 1,2,3,4
;
