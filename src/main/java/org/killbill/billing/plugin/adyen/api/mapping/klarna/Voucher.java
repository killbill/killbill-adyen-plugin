package org.killbill.billing.plugin.adyen.api.mapping.klarna;

public class Voucher {
    private String voucher_name;
    private String voucher_company;

    public String getVoucher_name() {
        return voucher_name;
    }
    public void setVoucher_name(String voucher_name) {
        this.voucher_name = voucher_name;
    }

    public String getVoucher_company() {
        return voucher_company;
    }
    public void setVoucher_company(String voucher_company) {
        this.voucher_company = voucher_company;
    }
}

