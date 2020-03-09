package org.killbill.billing.plugin.adyen.api.mapping.klarna;

import java.util.List;

public class MerchantData {
    private List<Voucher> voucher;
    private List<Account> customer_account_info;
    private List<Seller> marketplace_seller_info;

    public List<Voucher> getVoucher() {
        return voucher;
    }

    public void setVoucher(List<Voucher> voucher) {
        this.voucher = voucher;
    }

    public List<Account> getCustomer_account_info() {
        return customer_account_info;
    }

    public void setCustomer_account_info(List<Account> customer_account_info) {
        this.customer_account_info = customer_account_info;
    }

    public List<Seller> getMarketplace_seller_info() {
        return marketplace_seller_info;
    }

    public void setMarketplace_seller_info(List<Seller> marketplace_seller_info) {
        this.marketplace_seller_info = marketplace_seller_info;
    }
}