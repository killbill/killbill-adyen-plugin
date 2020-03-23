package org.killbill.billing.plugin.adyen.api.mapping;

import com.google.common.collect.ImmutableMap;

import org.jooq.tools.StringUtils;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.api.PluginProperties;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.*;

public abstract class TestKlarnaPaymentInfoBase {
    private final UserData userData;

    protected final String shopperEmail = "testing@gmail.com";
    protected final String shopperReference = UUID.randomUUID().toString();
    protected final Locale shopperLocale = Locale.UK;
    protected final String firstName = "First";
    protected final String lastName = "Last";
    protected final String gender = "MALE";
    protected final String returnUrl = "https://www.company.com/callback";
    protected final String orderReference = UUID.randomUUID().toString();

    public TestKlarnaPaymentInfoBase() {
        userData = new UserData();
        userData.setShopperEmail(shopperEmail);
        userData.setShopperReference(shopperReference);
        userData.setShopperLocale(shopperLocale);
        userData.setFirstName(firstName);
        userData.setLastName(lastName);
        userData.setGender(gender);
    }

    protected UserData getUserData() {
        return userData;
    }

    protected Iterable<PluginProperty> addKlarnaPaymentData(
            final String customerAccount,
            final String lineItems) {
        Iterable<PluginProperty> klarnaProperties = PluginProperties.buildPluginProperties(ImmutableMap.<String, String>builder()
                .put(PROPERTY_PAYMENT_TYPE, KlarnaPaymentMappingService.KLARNA_PAYMENT_TYPE)
                .put(PROPERTY_PAYMENT_METHOD, KlarnaPaymentMappingService.KLARNA_PAY_LATER)
                .put(PROPERTY_RETURN_URL, returnUrl)
                .put(PROPERTY_ORDER_REFERENCE, orderReference)
                .put(PROPERTY_CUSTOMER_ACCOUNT, customerAccount)
                .put(PROPERTY_LINE_ITEMS, lineItems)
                .build());

        return klarnaProperties;
    }

    protected KlarnaPaymentInfo getPaymentInfo(final String merchantAccount,
                                               final String countryCode,
                                               final String customerAccount,
                                               final String lineItems,
                                               Iterable<PluginProperty> properties) {
        Iterable<PluginProperty> klarnaProperties = addKlarnaPaymentData(customerAccount, lineItems);
        Iterable<PluginProperty> allProperties = PluginProperties.merge(klarnaProperties, properties);
        final KlarnaPaymentInfo paymentInfo = (KlarnaPaymentInfo) KlarnaPaymentMappingService
                .toPaymentInfo(merchantAccount, countryCode, allProperties);
        return paymentInfo;
    }
}
