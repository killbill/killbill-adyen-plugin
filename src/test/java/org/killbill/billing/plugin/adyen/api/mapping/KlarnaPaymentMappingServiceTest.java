package org.killbill.billing.plugin.adyen.api.mapping;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
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
        PaymentInfo paymentInfo = KlarnaPaymentMappingService.toPaymentInfo(merchantAccount, countryCode, properties);
        KlarnaPaymentInfo klarnaPaymentInfo = (KlarnaPaymentInfo) paymentInfo;

        //validate payment info mapped from properties
        assertEquals(klarnaPaymentInfo.getNotificationUrl(), returnUrl);
        assertEquals(klarnaPaymentInfo.getMerchantAccount(), merchantAccount);
        assertEquals(klarnaPaymentInfo.getCountryCode(), countryCode);
        assertEquals(klarnaPaymentInfo.getOrderReference(), orderReference);

        //account info

        //voucher info

        //seller info

        //line items
    }

    private Iterable<PluginProperty> propertiesForKlarnaPayment() {
        String customerAccount= "{\"accountId\":\"ACCOUNT_ID009\",\"registrationDate\":\"2019-08-08T09:16:15Z\",\"lastModifiedDate\":\"2019-08-08T09:50:15Z\"}";
        String lineItems = "[{\"id\":\"Item_ID090909\",\"quantity\":\"1\",\"taxAmount\":\"69\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"331\",\"amountIncludingTax\":\"400\",\"description\":\"Shoes\",\"productName\":\"School Shoes\",\"productCategory\":\"Shoes\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Shopee\",\"inventoryService\":\"vis\"},{\"id\":\"Item_ID090910\",\"quantity\":\"2\",\"taxAmount\":\"52\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"248\",\"amountIncludingTax\":\"300\",\"description\":\"Socks\",\"productName\":\"School Shoes\",\"productCategory\":\"Shoes\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Shopee\",\"inventoryService\":\"goods\"}]";
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
