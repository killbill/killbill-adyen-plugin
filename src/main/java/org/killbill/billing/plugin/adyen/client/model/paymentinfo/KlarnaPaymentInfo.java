package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.tools.StringUtils;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.MerchantData;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Seller;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Voucher;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KlarnaPaymentInfo extends PaymentInfo {
    private String merchantAccount;
    private String paymentMethod;
    private String orderReference;
    private String countryCode;
    private String returnUrl;
    private List<Voucher> vouchers;
    private List<Account> accounts;
    private List<Seller> sellers;
    private List<LineItem> items = new ArrayList<>();

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

    public List<LineItem> getItems() {
        return items;
    }
    public void setItems(List<LineItem> items) {
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
        merchantData.setCustomer_account_info(accounts);
        merchantData.setMarketplace_seller_info(sellers);
        merchantData.setVoucher(vouchers);

        try {
            ObjectMapper mapper = new ObjectMapper();
            additionalData = mapper.writeValueAsString(merchantData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return additionalData;
    }

    // Line Item
    public static class LineItem {
        private String id;
        private String description;
        private Long quantity;
        private Long taxAmount;
        private Long taxPercentage;
        private Long amountExcludingTax;
        private Long amountIncludingTax;

        // fields included for voucher, seller
        private String inventoryService;
        private String productName;
        private String productCategory;
        private String merchantId;
        private String merchantName;

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }

        public Long getQuantity() {
            return quantity;
        }
        public void setQuantity(Long quantity) {
            this.quantity = quantity;
        }

        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getProductCategory() { return productCategory; }
        public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

        public Long getTaxAmount() {
            return taxAmount;
        }
        public void setTaxAmount(Long taxAmount) {
            this.taxAmount = taxAmount;
        }

        public Long getTaxPercentage() {
            return taxPercentage;
        }
        public void setTaxPercentage(Long taxPercentage) {
            this.taxPercentage = taxPercentage;
        }

        public Long getAmountExcludingTax() {
            return amountExcludingTax;
        }
        public void setAmountExcludingTax(Long amountExcludingTax) {
            this.amountExcludingTax = amountExcludingTax;
        }

        public Long getAmountIncludingTax() {
            return amountIncludingTax;
        }
        public void setAmountIncludingTax(Long amountIncludingTax) {
            this.amountIncludingTax = amountIncludingTax;
        }

        public String getInventoryService() { return inventoryService; }
        public void setInventoryService(String inventoryService) { this.inventoryService = inventoryService; }

        public boolean isVoucher() {
            boolean itemIsVoucher = true;
            if(!StringUtils.isEmpty(inventoryService) && inventoryService.toLowerCase().equals("goods")) {
                itemIsVoucher = false;
            }

            return itemIsVoucher;
        }
    }

    public static class CustomerAccount {
        private String accountId;
        private String registrationDate;
        private String lastModifiedDate;

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
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
}
