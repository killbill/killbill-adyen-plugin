/*! SET storage_engine=INNODB */;


drop table if exists adyen_plugin_gateway_responses;
create table adyen_plugin_gateway_responses (
  record_id int(11) unsigned not null auto_increment
, kb_account_id char(36) not null
, kb_payment_id char(36) not null
, transaction_type varchar(32) not null
, auth_code varchar(32)
, dcc_amount numeric(15,9)
, dcc_currency char(3)
, dcc_signature varchar(64)
, fraud_result int(11)
, issuer_url varchar(1024)
, md varchar(512)
, pa_request varchar(512)
, psp_reference varchar(64)
, refusal_reason varchar(64)
, result_code varchar(64)
, primary key(record_id)
);
