alter table adyen_hpp_requests add column kb_payment_id char(36) default null after kb_account_id;
alter table adyen_hpp_requests add column kb_payment_transaction_id char(36) default null after kb_payment_id;
