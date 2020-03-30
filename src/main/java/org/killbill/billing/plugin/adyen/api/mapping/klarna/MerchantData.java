package org.killbill.billing.plugin.adyen.api.mapping.klarna;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MerchantData {
    @JsonProperty("voucher")
    private List<Voucher> voucherInfo;

    @JsonProperty("customer_account_info")
    private List<Account> customerInfo;

    @JsonProperty("marketplace_seller_info")
    private List<Seller> sellerInfo;

    public List<Voucher> getVoucherInfo() {
        return voucherInfo;
    }

    public void setVoucherInfo(List<Voucher> voucherInfo) {
        this.voucherInfo = voucherInfo;
    }

    public List<Account> getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(List<Account> customerInfo) {
        this.customerInfo = customerInfo;
    }

    public List<Seller> getSellerInfo() {
        return sellerInfo;
    }

    public void setSellerInfo(List<Seller> sellerInfo) {
        this.sellerInfo = sellerInfo;
    }

    @Override
    public String toString() {
        return "MerchantData{" +
               "voucherInfo=" + voucherInfo +
               ", customerInfo=" + customerInfo +
               ", sellerInfo=" + sellerInfo +
               '}';
    }
}
