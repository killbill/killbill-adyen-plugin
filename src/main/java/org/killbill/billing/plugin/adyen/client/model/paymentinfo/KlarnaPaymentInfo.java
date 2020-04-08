package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.jooq.tools.StringUtils;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.MerchantData;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Seller;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Voucher;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class KlarnaPaymentInfo extends PaymentInfo {
    private static final Logger logger = LoggerFactory.getLogger(KlarnaPaymentInfo.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private String merchantAccount;
    private String paymentMethod;
    private String orderReference;
    private String countryCode;
    private String returnUrl;
    private List<Voucher> vouchers;
    private List<Account> accounts;
    private List<Seller> sellers;
    private List<PropertyMapper.LineItem> items = new ArrayList<>();
    private PropertyMapper.Address shippingAddress;
    private boolean usingShippingAddress = false;

    //data for payment details check
    private Iterable<PluginProperty> properties;

    public Iterable<PluginProperty> getProperties() {
        return properties;
    }
    public void setProperties(Iterable<PluginProperty> properties) {
        this.properties = properties;
    }

    public boolean usingShippingAddress() { return usingShippingAddress; }
    public PropertyMapper.Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(final PropertyMapper.Address shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getMerchantAccount() {
        return merchantAccount;
    }
    public void setMerchantAccount(String merchantAccount) {
        this.merchantAccount = merchantAccount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getOrderReference() { return orderReference; }
    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public String getCountryCode() {
        return countryCode;
    }
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getReturnUrl() {
        return returnUrl;
    }
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public List<PropertyMapper.LineItem> getItems() {
        return items;
    }
    public void setItems(List<PropertyMapper.LineItem> items) {
        this.items = items;

        for(PropertyMapper.LineItem item: items) {
            final String inventoryType = item.getInventoryService();
            if(!StringUtils.isEmpty(inventoryType) && inventoryType.equals("goods")) {
                this.usingShippingAddress = true;
                break;
            }
        }
    }

    public List<Account> getAccounts() {
        return accounts;
    }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }

    public List<Seller> getSellers() {
        return sellers;
    }
    public void setSellers(List<Seller> sellers) {
        this.sellers = sellers;
    }

    public List<Voucher> getVouchers() {
        return vouchers;
    }
    public void setVouchers(List<Voucher> vouchers) { this.vouchers = vouchers; }

    public String getAdditionalData() {
        String additionalData = null;
        MerchantData merchantData = new MerchantData();
        merchantData.setCustomerInfo(accounts);
        merchantData.setSellerInfo(sellers);
        merchantData.setVoucherInfo(vouchers);

        try {
            additionalData = mapper.writeValueAsString(merchantData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return additionalData;
    }
}
