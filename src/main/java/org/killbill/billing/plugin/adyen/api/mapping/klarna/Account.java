package org.killbill.billing.plugin.adyen.api.mapping.klarna;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Account {
    @JsonProperty("unique_account_identifier")
    private String identifier;

    @JsonProperty("account_registration_date")
    private String registrationDate;

    @JsonProperty("account_last_modified")
    private String lastModifiedDate;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
