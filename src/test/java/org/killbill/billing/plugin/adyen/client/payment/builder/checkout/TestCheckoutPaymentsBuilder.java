package org.killbill.billing.plugin.adyen.client.payment.builder.checkout;

import com.adyen.model.Address;
import com.adyen.model.Name;
import com.adyen.model.checkout.LineItem;
import com.adyen.model.checkout.PaymentsRequest;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.api.mapping.TestKlarnaPaymentInfoBase;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.testng.annotations.Test;
import java.math.BigDecimal;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestCheckoutPaymentsBuilder extends TestKlarnaPaymentInfoBase {
    private final String countryCode = "GB";
    private final String merchantAccount = "MerchantAccount";
    private final String shippingAddress = "{\"address1\":\"Address Line1\",\"address2\":\"Address Line2\",\"city\":\"My City\",\"state\":\"My State\",\"country\":\"My Country\",\"postalCode\":\"AB111CD\"}";
    private final String customerAccount= "{\"accountId\":\"ACCOUNT_ID009\",\"registrationDate\":\"2019-08-08T09:16:15Z\",\"lastModifiedDate\":\"2019-08-08T09:50:15Z\"}";
    private final String lineItems = "[{\"id\":\"Item_ID090909\",\"quantity\":\"2\",\"taxAmount\":\"69\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"331\",\"amountIncludingTax\":\"400\",\"description\":\"Black Shoes\",\"productName\":\"School Shoes\",\"productCategory\":\"Shoes\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Shopee\",\"inventoryService\":\"goods\"},{\"id\":\"Item_ID090910\",\"quantity\":\"1\",\"taxAmount\":\"52\",\"taxPercentage\":\"2100\",\"amountExcludingTax\":\"248\",\"amountIncludingTax\":\"300\",\"description\":\"Wine Tasting\",\"productName\":\"Winery\",\"productCategory\":\"Experience\",\"merchantId\":\"MERCHANT_ID0909\",\"merchantName\":\"Local Vineyard\",\"inventoryService\":\"vis\"}]";


    @Test(groups = "fast")
    public void testPaymentRequest() throws Exception {
        KlarnaPaymentInfo paymentInfo = getPaymentInfo(merchantAccount,
                countryCode, customerAccount, shippingAddress, lineItems);
        PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.valueOf(12), Currency.EUR,
                UUID.randomUUID().toString(), paymentInfo);

        final CheckoutPaymentsBuilder builder = new CheckoutPaymentsBuilder(
                merchantAccount, paymentData, getUserData());

        PaymentsRequest request = builder.build();
        assertEquals(request.getCountryCode(), "GB");
        assertEquals(request.getMerchantAccount(), merchantAccount);
        assertEquals(request.getPaymentMethod().getType(), "klarna");
        assertEquals(request.getAmount().getCurrency(), "EUR");
        assertEquals(request.getAmount().getValue(), Long.valueOf(1200));
        assertEquals(request.getReturnUrl(), returnUrl);
        assertEquals(request.getReference(), orderReference);
        assertNotNull(request.getAdditionalData());

        //shopper data
        assertEquals(request.getShopperEmail(), shopperEmail);
        assertEquals(request.getShopperLocale(), shopperLocale.toString());
        assertEquals(request.getShopperName().getFirstName(), firstName);
        assertEquals(request.getShopperName().getLastName(), lastName);
        assertEquals(request.getShopperName().getGender(), Name.GenderEnum.valueOf(gender));

        //invoice lines
        assertEquals(request.getLineItems().size(), 2);
        LineItem item1 = request.getLineItems().get(0);
        LineItem item2 = request.getLineItems().get(1);
        assertEquals(item1.getId(), "Item_ID090909");
        assertEquals(item1.getDescription(), "Black Shoes");
        assertEquals(item1.getAmountIncludingTax(), Long.valueOf(400));
        assertEquals(item1.getQuantity(), Long.valueOf(2));
        assertEquals(item2.getId(), "Item_ID090910");
        assertEquals(item2.getDescription(), "Wine Tasting");
        assertEquals(item2.getAmountIncludingTax(), Long.valueOf(300));
        assertEquals(item2.getQuantity(), Long.valueOf(1));

        //delivery address
        Address address = request.getDeliveryAddress();
        assertEquals(address.getHouseNumberOrName(), "Address Line1");
        assertEquals(address.getStreet(), "Address Line2");
        assertEquals(address.getCity(), "My City");
        assertEquals(address.getStateOrProvince(), "My State");
        assertEquals(address.getCountry(), "My Country");
        assertEquals(address.getPostalCode(), "AB111CD");
    }
}
