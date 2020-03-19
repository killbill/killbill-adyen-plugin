package org.killbill.billing.plugin.adyen.api.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.tools.StringUtils;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Seller;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Voucher;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.api.PluginProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.*;

public abstract class KlarnaPaymentMappingService {
    private static final Logger logger = LoggerFactory.getLogger(KlarnaPaymentMappingService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String KLARNA_PAYMENT_TYPE = "klarna";
    public static final String KLARNA_PAY_LATER = "klarna";
    public static final String KLARNA_PAY_NOW = "klarna_paynow";
    public static final String KLARNA_PAY_INSTALLMENT = "klarna_account";

    public static PaymentInfo toPaymentInfo(String merchantAccount, final String countryCode, Iterable<PluginProperty> properties) {
        final KlarnaPaymentInfo paymentInfo = new KlarnaPaymentInfo();
        paymentInfo.setProperties(properties);
        paymentInfo.setCountryCode(countryCode);
        paymentInfo.setMerchantAccount(merchantAccount);
        paymentInfo.setPaymentType(PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_TYPE, properties));
        paymentInfo.setPaymentMethod(PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_METHOD, properties));
        paymentInfo.setReturnUrl(PluginProperties.findPluginPropertyValue(PROPERTY_RETURN_URL, properties));
        paymentInfo.setOrderReference(PluginProperties.findPluginPropertyValue(PROPERTY_ORDER_REFERENCE, properties));
        setAccountInfo(properties, paymentInfo);

        List<KlarnaPaymentInfo.LineItem> lineItems = extractLineItems(properties);
        if(lineItems != null) {
            paymentInfo.setItems(lineItems);
            setVoucherInfo(lineItems, paymentInfo);
            setSellerInfo(lineItems, paymentInfo);
        }


        return paymentInfo;
    }

    private static List<KlarnaPaymentInfo.LineItem> extractLineItems(Iterable<PluginProperty> properties) {
        List<KlarnaPaymentInfo.LineItem> lineItems = null;
        final String itemsJson = PluginProperties.findPluginPropertyValue(PROPERTY_LINE_ITEMS, properties);
        if(itemsJson != null) {
            try {
                KlarnaPaymentInfo.LineItem[] items = mapper.readValue(itemsJson, KlarnaPaymentInfo.LineItem[].class);
                lineItems = Arrays.asList(items);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.warn("No line items found in plugin property:" + PROPERTY_LINE_ITEMS);
        }

        return lineItems;
    }

    private static void setVoucherInfo(List<KlarnaPaymentInfo.LineItem> lineItems, KlarnaPaymentInfo paymentInfo) {
        List<Voucher> vouchers = new ArrayList<>();
        for(KlarnaPaymentInfo.LineItem item: lineItems) {
            if(item.isVoucher()) {
                if (item.getDescription() != null ||
                        item.getMerchantName() != null) {
                    Voucher voucher = new Voucher();
                    voucher.setVoucher_name(item.getDescription());
                    voucher.setVoucher_company(item.getMerchantName());
                    vouchers.add(voucher);
                }
            }
        }

        paymentInfo.setVouchers(vouchers);
    }

    private static void setSellerInfo(List<KlarnaPaymentInfo.LineItem> lineItems, KlarnaPaymentInfo paymentInfo) {
        List<Seller> sellers = new ArrayList<>();
        for(KlarnaPaymentInfo.LineItem item: lineItems) {
            if(item.getProductName() != null ||
                    item.getProductCategory() != null ||
                    item.getMerchantId() != null) {
                Seller seller = new Seller();
                seller.setProduct_name(item.getProductName());
                seller.setProduct_category(item.getProductCategory());
                seller.setSub_merchant_id(item.getMerchantId());
                sellers.add(seller);
            }
        }

        paymentInfo.setSellers(sellers);
    }

    private static void setAccountInfo(Iterable<PluginProperty> properties, KlarnaPaymentInfo paymentInfo) {
        List<Account> accountList = new ArrayList<>();
        String accountInfoValue = PluginProperties.findPluginPropertyValue(PROPERTY_CUSTOMER_ACCOUNT, properties);
        if(!StringUtils.isEmpty(accountInfoValue)) {
            try {
                KlarnaPaymentInfo.CustomerAccount customerAccount = mapper.readValue(accountInfoValue, KlarnaPaymentInfo.CustomerAccount.class);

                Account account = new Account();
                account.setUnique_account_identifier(customerAccount.getAccountId());
                account.setAccount_registration_date(customerAccount.getRegistrationDate());
                account.setAccount_last_modified(customerAccount.getLastModifiedDate());
                accountList.add(account);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        paymentInfo.setAccounts(accountList);
    }
}
