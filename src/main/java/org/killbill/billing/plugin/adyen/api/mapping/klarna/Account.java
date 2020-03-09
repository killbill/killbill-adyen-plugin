package org.killbill.billing.plugin.adyen.api.mapping.klarna;

public class Account {
    private String unique_account_identifier;
    private String account_registration_date;
    private String account_last_modified;

    public String getUnique_account_identifier() { return unique_account_identifier; }
    public void setUnique_account_identifier(String unique_account_identifier) { this.unique_account_identifier = unique_account_identifier; }

    public String getAccount_registration_date() { return account_registration_date; }
    public void setAccount_registration_date(String account_registration_date) { this.account_registration_date = account_registration_date; }

    public String getAccount_last_modified() { return account_last_modified; }
    public void setAccount_last_modified(String account_last_modified) { this.account_last_modified = account_last_modified; }
}
