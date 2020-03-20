package org.killbill.billing.plugin.adyen.api.mapping;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jooq.tools.StringUtils;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Seller;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Voucher;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.api.PluginProperties;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

public class KlarnaPaymentMappingServiceTest {
    private final String countryCode = "GB";
    private final String merchantAccount = "MerchantAccount";
    private final String returnUrl = "https://www.company.com/callback";
    private final String orderReference = UUID.randomUUID().toString();

    @Test(groups = "fast")
    public void testPaymentKlarnaInfoMapping() throws Exception {
        final Iterable<PluginProperty> properties = propertiesForKlarnaPayment();
        KlarnaPaymentInfo paymentInfo = (KlarnaPaymentInfo) KlarnaPaymentMappingService.toPaymentInfo(
                merchantAccount, countryCode, properties);

        //validate payment info mapped from properties
        assertEquals(paymentInfo.getReturnUrl(), returnUrl);
        assertEquals(paymentInfo.getMerchantAccount(), merchantAccount);
        assertEquals(paymentInfo.getCountryCode(), countryCode);
        assertEquals(paymentInfo.getOrderReference(), orderReference);
        assertEquals(paymentInfo.getPaymentType(), KlarnaPaymentMappingService.KLARNA_PAYMENT_TYPE);
        assertEquals(paymentInfo.getPaymentMethod(), KlarnaPaymentMappingService.KLARNA_PAY_LATER);
        assertTrue(paymentInfo.getAccounts().size() > 0);
        assertTrue(paymentInfo.getVouchers().size() > 0);
        assertTrue(paymentInfo.getSellers().size() > 0);
        assertTrue(paymentInfo.getItems().size() > 0);

        //line items
        KlarnaPaymentInfo.LineItem lineItem = paymentInfo.getItems().get(0);
        assertEquals(lineItem.getId(), "Item_ID090909");
        assertEquals(lineItem.getQuantity(), Long.valueOf(2));
        assertEquals(lineItem.getDescription(), "Black Shoes");
        assertEquals(lineItem.getMerchantId(), "MERCHANT_ID0909");
        assertEquals(lineItem.getMerchantName(), "Local Shopee");
        assertEquals(lineItem.getProductName(), "School Shoes");
        assertEquals(lineItem.getProductCategory(), "Shoes");
        assertEquals(lineItem.getAmountExcludingTax(), Long.valueOf(331));
        assertEquals(lineItem.getAmountIncludingTax(), Long.valueOf(400));
        assertEquals(lineItem.getTaxAmount(), Long.valueOf(69));
        assertEquals(lineItem.getTaxPercentage(), Long.valueOf(2100));
        assertEquals(lineItem.getInventoryService(), "goods");

        //account info
        Account account = paymentInfo.getAccounts().get(0);
        assertEquals(account.getIdentifier(), "ACCOUNT_ID009");
        assertFalse(StringUtils.isEmpty(account.getRegistrationDate()));
        assertFalse(StringUtils.isEmpty(account.getLastModifiedDate()));

        //voucher info
        Voucher voucher = paymentInfo.getVouchers().get(0);
        assertEquals(voucher.getName(), "Wine Tasting");
        assertEquals(voucher.getCompany(), "Local Vineyard");

        //seller info
        Seller seller = paymentInfo.getSellers().get(0);
        assertEquals(seller.getProductName(), "School Shoes");
        assertEquals(seller.getProductCategory(), "Shoes");
        assertEquals(seller.getMerchantId(), "MERCHANT_ID0909");
    }

    private Iterable<PluginProperty> propertiesForKlarnaPayment() {
        String customerAccount= "{\"accountId\":\"ACCOUNT_ID009\",\"registrationDate\":\"2019-08-08T09:16:15Z\",\"lastModifiedDate\":\"2019-08-08T09:50:15Z\"}";
        String lineItems = "[{\"id\":\"Item_ID090909\",\"quantity\":\"2\",\"taxAmount\":\"69\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"331\",\"amountIncludingTax\":\"400\",\"description\":\"Black Shoes\",\"productName\":\"School Shoes\",\"productCategory\":\"Shoes\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Shopee\",\"inventoryService\":\"goods\"},{\"id\":\"Item_ID090910\",\"quantity\":\"1\",\"taxAmount\":\"52\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"248\",\"amountIncludingTax\":\"300\",\"description\":\"Wine Tasting\",\"productName\":\"Winery\",\"productCategory\":\"Experience\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Vineyard\",\"inventoryService\":\"vis\"}]";
        Iterable<PluginProperty> klarnaProperties = PluginProperties.buildPluginProperties(ImmutableMap.<String, String>builder()
                .put(PROPERTY_PAYMENT_TYPE, KlarnaPaymentMappingService.KLARNA_PAYMENT_TYPE)
                .put(PROPERTY_PAYMENT_METHOD, KlarnaPaymentMappingService.KLARNA_PAY_LATER)
                .put(PROPERTY_RETURN_URL, returnUrl)
                .put(AdyenPaymentPluginApi.PROPERTY_COUNTRY, countryCode)
                .put(PROPERTY_ORDER_REFERENCE, orderReference)
                .put(PROPERTY_CUSTOMER_ACCOUNT, customerAccount)
                .put(PROPERTY_LINE_ITEMS, lineItems)
                .build());

        return klarnaProperties;
    }
}
