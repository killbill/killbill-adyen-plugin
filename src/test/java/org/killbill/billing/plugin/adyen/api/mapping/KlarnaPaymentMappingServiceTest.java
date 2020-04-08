package org.killbill.billing.plugin.adyen.api.mapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jooq.tools.StringUtils;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Account;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PaymentType;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper.LineAdjustment;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Seller;
import org.killbill.billing.plugin.adyen.api.mapping.klarna.Voucher;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.api.PluginProperties;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

public class KlarnaPaymentMappingServiceTest extends TestKlarnaPaymentInfoBase {
    private final String countryCode = "GB";
    private final String merchantAccount = "MerchantAccount";
    private final String shippingAddress = "{\"address1\":\"Address Line1\",\"address2\":\"Address Line2\",\"city\":\"My City\",\"state\":\"My State\",\"country\":\"My Country\",\"postalCode\":\"AB111CD\"}";
    private final String customerAccount= "{\"accountId\":\"ACCOUNT_ID009\",\"registrationDate\":\"2019-08-08T09:16:15Z\",\"lastModifiedDate\":\"2019-08-08T09:50:15Z\"}";
    private final String lineItems = "[{\"id\":\"Item_ID090909\",\"quantity\":\"2\",\"taxAmount\":\"69\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"331\",\"amountIncludingTax\":\"400\",\"description\":\"Black Shoes\",\"productName\":\"School Shoes\",\"productCategory\":\"Shoes\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Shopee\",\"inventoryType\":\"goods\"},{\"id\":\"Item_ID090910\",\"quantity\":\"1\",\"taxAmount\":\"52\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"248\",\"amountIncludingTax\":\"300\",\"description\":\"Wine Tasting\",\"productName\":\"Winery\",\"productCategory\":\"Experience\",\"merchantId\":\"MERCHANT_ID0910\",\"merchantName\":\"Local Vineyard\",\"inventoryType\":\"vis\"}]";
    private final String paymentDataResponse = "AbcdefghijklmnopqrstuvwxyZ1234567890";
    private String authKeyResponse = "{\"key1\":\"text\",\"key2\":\"blob\"}";
    private Iterable<PluginProperty> authCompleteProperties = PluginProperties.buildPluginProperties(
            ImmutableMap.<String,String>builder()
                    .put("key1", "ResponseData1")
                    .put("key2", "ResponseData2")
                    .build());


    @Test(groups = "fast")
    public void testPaymentKlarnaInfoMapping() throws Exception {
        KlarnaPaymentInfo paymentInfo = getPaymentInfo(merchantAccount,
                countryCode, customerAccount, shippingAddress, lineItems, null);

        //validate payment info mapped from properties
        assertEquals(paymentInfo.getReturnUrl(), returnUrl);
        assertEquals(paymentInfo.getMerchantAccount(), merchantAccount);
        assertEquals(paymentInfo.getCountryCode(), countryCode);
        assertEquals(paymentInfo.getOrderReference(), orderReference);
        assertEquals(paymentInfo.getPaymentType(), KlarnaPaymentMappingService.KLARNA_PAYMENT_TYPE);
        assertEquals(paymentInfo.getPaymentMethod(), PaymentType.PAY_LATER.toString());
        assertTrue(paymentInfo.getAccounts().size() > 0);
        assertTrue(paymentInfo.getVouchers().size() > 0);
        assertTrue(paymentInfo.getSellers().size() > 0);
        assertTrue(paymentInfo.getItems().size() > 0);

        //line items
        PropertyMapper.LineItem lineItem = paymentInfo.getItems().get(0);
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
        assertEquals(lineItem.getInventoryType(), "goods");

        //account info
        Account account = paymentInfo.getAccounts().get(0);
        String test1 = account.toString();
        assertEquals(account.getIdentifier(), "ACCOUNT_ID009");
        assertFalse(StringUtils.isEmpty(account.getRegistrationDate()));
        assertFalse(StringUtils.isEmpty(account.getLastModifiedDate()));

        //voucher info
        Voucher voucher = paymentInfo.getVouchers().get(0);
        String test2 = voucher.toString();
        assertEquals(voucher.getName(), "Wine Tasting");
        assertEquals(voucher.getCompany(), "Local Vineyard");

        //seller info
        Seller seller = paymentInfo.getSellers().get(0);
        String test3 = voucher.toString();
        assertEquals(seller.getProductName(), "School Shoes");
        assertEquals(seller.getProductCategory(), "Shoes");
        assertEquals(seller.getMerchantId(), "MERCHANT_ID0909");

        //shipping address
        PropertyMapper.Address address = paymentInfo.getShippingAddress();
        assertEquals(address.getAddress1(), "Address Line1");
        assertEquals(address.getAddress2(), "Address Line2");
        assertEquals(address.getCity(), "My City");
        assertEquals(address.getState(), "My State");
        assertEquals(address.getCountry(), "My Country");
        assertEquals(address.getPostalCode(), "AB111CD");
    }

    @Test(groups = "fast")
    public void testAdditionalData() throws Exception {
        KlarnaPaymentInfo paymentInfo = getPaymentInfo(merchantAccount,
                                                       countryCode,
                                                       customerAccount,
                                                       shippingAddress,
                                                       lineItems,
                                                       null);

        assertFalse(paymentInfo.isIdentifierHashed());
        assertTrue(paymentInfo.getAdditionalData().length() > 0);
        assertTrue(paymentInfo.isIdentifierHashed());
        assertEquals(paymentInfo.getIdentifierMap().size(), 2);
    }

    @Test(groups = "fast")
    public void testAuthResponseData() throws Exception {
        KlarnaPaymentInfo paymentInfo = getPaymentInfo(merchantAccount,
                                                       countryCode,
                                                       "dummy",
                                                       "dummy",
                                                       "dummy",
                                                       authCompleteProperties);

        //update the saved auth response data
        Map<String, String> responseData = ImmutableMap.<String,String>builder()
                .put("paymentData", paymentDataResponse)
                .put("resultKeys", authKeyResponse)
                .build();
        paymentInfo.setAuthResponseData(responseData);

        assertTrue(paymentInfo.completeKlarnaAuthorisation());
        assertEquals(paymentInfo.getPaymentsData(), paymentDataResponse);
        assertEquals(paymentInfo.getDetailsData().get("key1"), "ResponseData1");
        assertEquals(paymentInfo.getDetailsData().get("key2"), "ResponseData2");
    }

    @Test(groups = "fast")
    public void testLineItemAdjustments() throws Exception {
        final String lineItemWithAdjustment = "[{\"id\":\"Item_ID090909\",\"quantity\":\"1\",\"taxAmount\":\"69\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"331\",\"amountIncludingTax\":\"500\",\"description\":\"Black Shoes\",\"productName\":\"School Shoes\",\"productCategory\":\"Shoes\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Shopee\",\"inventoryType\":\"goods\",\"adjustments\":[{\"amount\":199,\"description\":\"Shipping & Handling\",\"type\":\"shipping_cost\"},{\"amount\":-100,\"description\":\"Promotional Discount\",\"type\":\"promo_discount\"}]},{\"id\":\"Item_ID090909\",\"quantity\":\"1\",\"taxAmount\":\"69\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"331\",\"amountIncludingTax\":\"500\",\"description\":\"Black Shoes\",\"productName\":\"School Shoes\",\"productCategory\":\"Shoes\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Shopee\",\"inventoryType\":\"goods\",\"adjustments\":[{\"amount\":199,\"description\":\"Shipping & Handling\",\"type\":\"shipping_cost\"},{\"amount\":-100,\"description\":\"Promotional Discount\",\"type\":\"promo_discount\"}]}]";
        KlarnaPaymentInfo paymentInfo = getPaymentInfo(merchantAccount,
                                                       countryCode,
                                                       customerAccount,
                                                       shippingAddress,
                                                       lineItemWithAdjustment,
                                                       null);

        List<PropertyMapper.LineItem> lineItems = paymentInfo.getItems();
        assertTrue(lineItems.size() == 4); //2 line items, 2 adjustments
        assertTrue(lineItems.get(0).getAdjustments().size() == 2);
        LineAdjustment adjustment = lineItems.get(0).getAdjustments().get(0);
        assertEquals(adjustment.getAmount(), Long.valueOf(199L));
        assertEquals(adjustment.getType(), "shipping_cost");
        assertEquals(adjustment.getDescription(), "Shipping & Handling");
        LineAdjustment adjustment2 = lineItems.get(0).getAdjustments().get(1);
        assertEquals(adjustment2.getAmount(), Long.valueOf(-100L));
        assertEquals(adjustment2.getType(), "promo_discount");
        assertEquals(adjustment2.getDescription(), "Promotional Discount");

        List<PropertyMapper.LineItem> adjustmentList = lineItems.stream().
                filter(item -> item.getInventoryType().equals("adjustment")
                      ).collect(Collectors.toList());

        assertTrue(adjustmentList.size() == 2);
        PropertyMapper.LineItem shippingAdjustment = adjustmentList.get(0);
        assertEquals(shippingAdjustment.getQuantity(), Long.valueOf(1L));
        assertEquals(shippingAdjustment.getId(), "shipping_cost");
        assertEquals(shippingAdjustment.getAmountIncludingTax(), Long.valueOf(398));
        assertEquals(shippingAdjustment.getDescription(), "Shipping & Handling");

        PropertyMapper.LineItem discountAdjustment = adjustmentList.get(1);
        assertEquals(discountAdjustment.getQuantity(), Long.valueOf(1L));
        assertEquals(discountAdjustment.getId(), "promo_discount");
        assertEquals(discountAdjustment.getAmountIncludingTax(), Long.valueOf(-200));
        assertEquals(discountAdjustment.getDescription(), "Promotional Discount");
    }
}
