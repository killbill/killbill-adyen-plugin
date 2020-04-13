package org.killbill.billing.plugin.adyen.api.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper.LineAdjustment;
import org.apache.cxf.common.util.CollectionUtils;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper.LineItem;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Seller;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Voucher;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo.KlarnaPaymentInfoBuilder;
import org.killbill.billing.plugin.api.PluginProperties;
import org.jooq.tools.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.*;

public abstract class KlarnaPaymentMappingService {
    private static final Logger logger = LoggerFactory.getLogger(KlarnaPaymentMappingService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String KLARNA_PAYMENT_TYPE = "klarna";

    public static PaymentInfo toPaymentInfo(final String merchantAccount, final String countryCode, Iterable<PluginProperty> properties) {
        final KlarnaPaymentInfoBuilder builder = new KlarnaPaymentInfoBuilder();
        builder.setProperties(properties);
        builder.setCountryCode(countryCode);
        builder.setMerchantAccount(merchantAccount);
        builder.setPaymentType(PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_TYPE, properties));
        builder.setPaymentMethod(PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_METHOD, properties));
        builder.setReturnUrl(PluginProperties.findPluginPropertyValue(PROPERTY_RETURN_URL, properties));
        builder.setOrderReference(PluginProperties.findPluginPropertyValue(PROPERTY_ORDER_REFERENCE, properties));
        setAccountInfo(properties, builder);
        setShippingAddress(properties, builder);

        List<PropertyMapper.LineItem> lineItems = extractLineItems(properties);
        if(!CollectionUtils.isEmpty(lineItems)) {
            setVoucherInfo(lineItems, builder);
            setSellerInfo(lineItems, builder);
            List<LineItem> adjustments = getLineAdjustments(lineItems);
            if(adjustments != null) {
                lineItems.addAll(adjustments);
            }

            builder.setItems(lineItems);
        }

        return builder.build();
    }

    private static List<LineItem> extractLineItems(Iterable<PluginProperty> properties) {
        List<PropertyMapper.LineItem> lineItems = null;
        final String itemsJson = PluginProperties.findPluginPropertyValue(PROPERTY_LINE_ITEMS, properties);
        if(!StringUtils.isEmpty(itemsJson)) {
            try {
                PropertyMapper.LineItem[] items = mapper.readValue(itemsJson, PropertyMapper.LineItem[].class);
                lineItems = new ArrayList<>(Arrays.asList(items));
            } catch (IOException e) {
                logger.error("Failed to parse lineItems, error={}\n{}", e.getMessage(), e.getStackTrace());
            }
        } else {
            logger.error("No line items found in plugin property:" + PROPERTY_LINE_ITEMS);
        }

        return lineItems;
    }

    private static List<LineItem> getLineAdjustments(final List<LineItem> lineItems) {
        final Map<String, LineAdjustment> adjustmentMap = new HashMap<>();
        final List<LineItem> adjustmentLines = new ArrayList<>();
        if(lineItems!= null && lineItems.size() > 0) {
            for(LineItem lineItem: lineItems) {
                List<LineAdjustment> adjustments = lineItem.getAdjustments();
                if(adjustments != null && adjustments.size() > 0) {
                    for(LineAdjustment adjustment: adjustments) {
                        if(adjustmentMap.containsKey(adjustment.getType())) {
                            //update the amount for existing adjustment record
                            LineAdjustment existingRecord = adjustmentMap.get(adjustment.getType());
                            Long newAmount = adjustment.getAmount() + existingRecord.getAmount();
                            existingRecord.setAmount(newAmount);
                        } else {
                            //add new adjustment record in map
                            LineAdjustment newRecord = new LineAdjustment(adjustment.getAmount(),
                                                                          adjustment.getType(),
                                                                          adjustment.getDescription());
                            adjustmentMap.put(adjustment.getType(), newRecord);
                        }
                    }
                }
            }

            // add the adjustments lines as line items
            final Collection<LineAdjustment> finalAdjustments = adjustmentMap.values();
            for(LineAdjustment adjustment: finalAdjustments) {
                final LineItem item = new LineItem();
                item.setQuantity(1L);
                item.setInventoryType("adjustment");
                item.setId(adjustment.getType());
                item.setAmountIncludingTax(adjustment.getAmount());
                item.setDescription(adjustment.getDescription());
                adjustmentLines.add(item);
            }
        }

        return adjustmentLines.size() > 0 ? adjustmentLines : null;
    }

    private static void setVoucherInfo(List<PropertyMapper.LineItem> lineItems, KlarnaPaymentInfoBuilder builder) {
        List<Voucher> vouchers = new ArrayList<>();
        for(PropertyMapper.LineItem item: lineItems) {
            if(item.isVoucher()) {
                if (!StringUtils.isEmpty(item.getDescription()) ||
                    !StringUtils.isEmpty(item.getMerchantName())) {
                    Voucher voucher = new Voucher();
                    voucher.setName(item.getDescription());
                    voucher.setCompany(item.getMerchantName());
                    vouchers.add(voucher);
                }
            }
        }

        builder.setVouchers(vouchers);
    }

    private static void setSellerInfo(List<PropertyMapper.LineItem> lineItems, KlarnaPaymentInfoBuilder builder) {
        List<Seller> sellers = new ArrayList<>();
        for(PropertyMapper.LineItem item: lineItems) {
            if(!StringUtils.isEmpty(item.getProductName()) ||
               !StringUtils.isEmpty(item.getProductCategory()) ||
               !StringUtils.isEmpty(item.getMerchantId())) {
                Seller seller = new Seller();
                seller.setProductName(item.getProductName());
                seller.setProductCategory(item.getProductCategory());
                seller.setMerchantId(item.getMerchantId());
                sellers.add(seller);
            }
        }

        builder.setSellers(sellers);
    }

    private static void setAccountInfo(Iterable<PluginProperty> properties, KlarnaPaymentInfoBuilder builder) {
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
                logger.error("Failed to parse customerAccount, error={}\n{}", e.getMessage(), e.getStackTrace());
            }
        }

        builder.setAccounts(accountList);
    }

    private static void setShippingAddress(Iterable<PluginProperty> properties, KlarnaPaymentInfoBuilder builder) {
        String shippingAddressJson = PluginProperties.findPluginPropertyValue(PROPERTY_SHIPPING_ADDRESS, properties);
        if(!StringUtils.isEmpty(shippingAddressJson)) {
            try {
                PropertyMapper.Address shippingAddress = mapper.readValue(shippingAddressJson, PropertyMapper.Address.class);
                builder.setShippingAddress(shippingAddress);
            } catch (IOException e) {
                logger.error("Failed to parse shippingAddress, error={}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }
}
