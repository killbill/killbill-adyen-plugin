package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.MerchantData;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper.Address;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper.LineItem;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Seller;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Voucher;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.api.PluginProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    private List<PropertyMapper.LineItem> items;
    private PropertyMapper.Address shippingAddress;

    //data for payment details check
    private String paymentsData;
    Map<String, String> detailsData = new HashMap<>();
    private Iterable<PluginProperty> properties;

    private KlarnaPaymentInfo(KlarnaPaymentInfoBuilder builder) {
        this.merchantAccount = builder.merchantAccount;
        this.paymentMethod = builder.paymentMethod;
        this.orderReference = builder.orderReference;
        this.countryCode = builder.countryCode;
        this.returnUrl = builder.returnUrl;
        this.vouchers = builder.vouchers;
        this.accounts = builder.accounts;
        this.sellers = builder.sellers;
        this.items = builder.items;
        this.shippingAddress = builder.shippingAddress;
        this.setPaymentType(builder.paymentType);
    }

    public boolean completeKlarnaAuthorisation() {
        return detailsData.size() > 0;
    }

    private void setAuthResultKeys(Map<String, String> additionalData) {
        List<String> authCompleteKeys = new ArrayList<>();
        String authResultKeys = additionalData.get("resultKeys");
        if(authResultKeys != null) {
            try {
                Map<String, String> authKeyMap = mapper.readValue(authResultKeys, Map.class);
                authCompleteKeys.addAll(authKeyMap.keySet());
            } catch (IOException ex) {
                logger.warn("Failed to retrieve details keys: " + ex.getMessage());
            }
        }

        if(authCompleteKeys.size() > 0) {
            for (String authKey: authCompleteKeys) {
                String value = PluginProperties.findPluginPropertyValue(authKey, properties);
                detailsData.put(authKey, value);
            }
        }
    }

    @Override
    protected void updateAuthResponse() {
        Map<String, String> additionalDataResponse = getAuthResponseData();
        if (additionalDataResponse != null) {
            this.paymentsData = additionalDataResponse.get("paymentData");
            setAuthResultKeys(additionalDataResponse);
        }
    }

    public Iterable<PluginProperty> getProperties() {
        return properties;
    }
    public void setProperties(Iterable<PluginProperty> properties) {
        this.properties = properties;
    }

    public Map<String, String> getDetailsData() { return detailsData; }
    public void setDetailsData(Map<String, String> detailsData) { this.detailsData = detailsData; }

    public String getPaymentsData() { return paymentsData; }
    public void setPaymentsData(String paymentsData) { this.paymentsData = paymentsData; }

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
            logger.error("Failed to generate marchant_data, error{}\n{}",
                         e.getMessage(), e.getStackTrace());
        }

        return additionalData;
    }

    public static class KlarnaPaymentInfoBuilder {
        private String merchantAccount;
        private String paymentType;
        private String paymentMethod;
        private String orderReference;
        private String countryCode;
        private String returnUrl;
        private List<Voucher> vouchers;
        private List<Account> accounts;
        private List<Seller> sellers;
        private List<PropertyMapper.LineItem> items;
        private PropertyMapper.Address shippingAddress;
        private Iterable<PluginProperty> properties;

        public KlarnaPaymentInfo build() {
            return new KlarnaPaymentInfo(this);
        }

        public KlarnaPaymentInfoBuilder setMerchantAccount(final String merchantAccount) {
            this.merchantAccount = merchantAccount;
            return this;
        }

        public KlarnaPaymentInfoBuilder setPaymentType(final String paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public KlarnaPaymentInfoBuilder setPaymentMethod(final String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public KlarnaPaymentInfoBuilder setOrderReference(final String orderReference) {
            this.orderReference = orderReference;
            return this;
        }

        public KlarnaPaymentInfoBuilder setCountryCode(final String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public KlarnaPaymentInfoBuilder setReturnUrl(final String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public KlarnaPaymentInfoBuilder setVouchers(final List<Voucher> vouchers) {
            this.vouchers = vouchers;
            return this;
        }

        public KlarnaPaymentInfoBuilder setAccounts(final List<Account> accounts) {
            this.accounts = accounts;
            return this;
        }

        public KlarnaPaymentInfoBuilder setSellers(final List<Seller> sellers) {
            this.sellers = sellers;
            return this;
        }

        public KlarnaPaymentInfoBuilder setItems(final List<LineItem> items) {
            this.items = items;
            return this;
        }

        public KlarnaPaymentInfoBuilder setShippingAddress(final Address shippingAddress) {
            this.shippingAddress = shippingAddress;
            return this;
        }

        public KlarnaPaymentInfoBuilder setProperties(final Iterable<PluginProperty> properties) {
            this.properties = properties;
            return this;
        }
    }
}
