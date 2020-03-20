package org.killbill.billing.plugin.adyen.api.mapping.klarna;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Voucher {
    @JsonProperty("voucher_name")
    private String name;

    @JsonProperty("voucher_company")
    private String company;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}

