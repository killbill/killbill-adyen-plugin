package org.killbill.billing.plugin.adyen.api.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Seller;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Voucher;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.api.PluginProperties;
import org.jooq.tools.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.*;

public abstract class KlarnaPaymentMappingService {
    private static final Logger logger = LoggerFactory.getLogger(KlarnaPaymentMappingService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String KLARNA_PAYMENT_TYPE = "klarna";

    public static PaymentInfo toPaymentInfo(final String merchantAccount, final String countryCode, Iterable<PluginProperty> properties) {
        final KlarnaPaymentInfo paymentInfo = new KlarnaPaymentInfo();
        paymentInfo.setProperties(properties);
        paymentInfo.setCountryCode(countryCode);
        paymentInfo.setMerchantAccount(merchantAccount);
        paymentInfo.setPaymentType(PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_TYPE, properties));
        paymentInfo.setPaymentMethod(PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_METHOD, properties));
        paymentInfo.setReturnUrl(PluginProperties.findPluginPropertyValue(PROPERTY_RETURN_URL, properties));
        paymentInfo.setOrderReference(PluginProperties.findPluginPropertyValue(PROPERTY_ORDER_REFERENCE, properties));
        setAccountInfo(properties, paymentInfo);
        setShippingAddress(properties, paymentInfo);

        List<PropertyMapper.LineItem> lineItems = extractLineItems(properties);
        if(lineItems != null) {
            paymentInfo.setItems(lineItems);
            setVoucherInfo(lineItems, paymentInfo);
            setSellerInfo(lineItems, paymentInfo);
        }


        return paymentInfo;
    }

    private static List<PropertyMapper.LineItem> extractLineItems(Iterable<PluginProperty> properties) {
        List<PropertyMapper.LineItem> lineItems = null;
        final String itemsJson = PluginProperties.findPluginPropertyValue(PROPERTY_LINE_ITEMS, properties);
        if(itemsJson != null) {
            try {
                PropertyMapper.LineItem[] items = mapper.readValue(itemsJson, PropertyMapper.LineItem[].class);
                lineItems = Arrays.asList(items);
            } catch (IOException e) {
                logger.error("Failed to parse lineItems from request, error=", e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.error("No line items found in plugin property:" + PROPERTY_LINE_ITEMS);
        }

        return lineItems;
    }

    private static void setVoucherInfo(List<PropertyMapper.LineItem> lineItems, KlarnaPaymentInfo paymentInfo) {
        List<Voucher> vouchers = new ArrayList<>();
        for(PropertyMapper.LineItem item: lineItems) {
            if(item.isVoucher()) {
                if (item.getDescription() != null ||
                        item.getMerchantName() != null) {
                    Voucher voucher = new Voucher();
                    voucher.setName(item.getDescription());
                    voucher.setCompany(item.getMerchantName());
                    vouchers.add(voucher);
                }
            }
        }

        paymentInfo.setVouchers(vouchers);
    }

    private static void setSellerInfo(List<PropertyMapper.LineItem> lineItems, KlarnaPaymentInfo paymentInfo) {
        List<Seller> sellers = new ArrayList<>();
        for(PropertyMapper.LineItem item: lineItems) {
            if(item.getProductName() != null ||
                    item.getProductCategory() != null ||
                    item.getMerchantId() != null) {
                Seller seller = new Seller();
                seller.setProductName(item.getProductName());
                seller.setProductCategory(item.getProductCategory());
                seller.setMerchantId(item.getMerchantId());
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
                PropertyMapper.CustomerAccount customerAccount = mapper.readValue(accountInfoValue, PropertyMapper.CustomerAccount.class);

                Account account = new Account();
                account.setIdentifier(customerAccount.getAccountId());
                account.setRegistrationDate(customerAccount.getRegistrationDate());
                account.setLastModifiedDate(customerAccount.getLastModifiedDate());
                accountList.add(account);
            } catch (IOException e) {
                logger.error("Failed to parse customerAccount from request, error=", e.getMessage());
                e.printStackTrace();
            }
        }

        paymentInfo.setAccounts(accountList);
    }

    private static void setShippingAddress(Iterable<PluginProperty> properties, KlarnaPaymentInfo paymentInfo) {
        String shippingAddressJson = PluginProperties.findPluginPropertyValue(PROPERTY_SHIPPING_ADDRESS, properties);
        if(!StringUtils.isEmpty(shippingAddressJson)) {
            try {
                PropertyMapper.Address shippingAddress = mapper.readValue(shippingAddressJson, PropertyMapper.Address.class);
                paymentInfo.setShippingAddress(shippingAddress);
            } catch (IOException e) {
                logger.error("Failed to parse shippingAddress from request, error=", e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
