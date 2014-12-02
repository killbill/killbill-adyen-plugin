/*
 * Copyright 2014 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.joda.time.DateTime;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.CreditCard;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.WebPaymentFrontend;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestHPPRequestBuilder {

    public static final String COUNTRY_CODE = "DE";
    public static final String MERCHANT_ACCOUNT = "ShoppyShop" + COUNTRY_CODE;
    public static final String SKIN_CODE = "skinCode" + COUNTRY_CODE;
    public static final String CURRENCY_CODE = "EUR";
    public static final String SHOPPER_EMAIL = "test@killbill.io";
    public static final String RES_URL = "http://killbill.io";
    public static final String MERCHANT_SIG = "signature";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm";
    public static final String VARIANT_OVERRIDE = "variantOverride";
    public static final String MERCHANT_REFERENCE = "merchantReference";

    private final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(new Properties());

    @Test(groups = "fast")
    public void testWithAll() throws Exception {
        final PaymentInfo paymentInfo = buildCCPaymentInfo();
        final PaymentType paymentType = paymentInfo.getPaymentProvider().getPaymentType();
        final Map<String, String> splitSettlementParameters = new HashMap<String, String>();
        splitSettlementParameters.put("splitKey", "splitValue");
        final Map<String, String> params = new HPPRequestBuilder().withCountryCode(COUNTRY_CODE)
                                                                  .withMerchantReference(MERCHANT_REFERENCE)
                                                                  .withPaymentAmount(100L)
                                                                  .withCurrencyCode(CURRENCY_CODE)
                                                                  .withShopperEmail(SHOPPER_EMAIL)
                                                                  .withShopperReference(123)
                                                                  .withRecurringContract(paymentInfo.getPaymentProvider())
                                                                  .withResURL(RES_URL)
                                                                  .withMerchantSig(MERCHANT_SIG)
                                                                  .withShipBeforeDate(DateTime.now().toString(DATE_TIME_PATTERN))
                                                                  .withSessionValidity(DateTime.now().plusMinutes(15).toString(DATE_TIME_PATTERN))
                                                                  .withSplitSettlementParameters(splitSettlementParameters)
                                                                  .withMerchantAccount(MERCHANT_ACCOUNT)
                                                                  .withSkinCode(SKIN_CODE)
                                                                  .withBrandCodeAndOrAllowedMethods(paymentInfo)
                                                                  .withShopperLocale(paymentType, Locale.GERMANY)
                                                                  .build();

        Assert.assertFalse(params.isEmpty(), "HPP Params map should not be empty");
        Assert.assertEquals(params.get("countryCode"), COUNTRY_CODE, "Wrong value for 'countryCode'");
        Assert.assertEquals(params.get("merchantAccount"), MERCHANT_ACCOUNT, "Wrong value for 'merchantAccount'");
        Assert.assertEquals(params.get("skinCode"), SKIN_CODE, "Wrong value for 'skinCode'");
        Assert.assertEquals(params.get("brandCode"), paymentType.getName(), "Wrong value for 'brandCode'");
        Assert.assertEquals(params.get("allowedMethods"), paymentType.getName(), "Wrong value for 'allowedMethods'");
        Assert.assertEquals(params.get("merchantReference"), MERCHANT_REFERENCE, "Wrong value for 'merchantReference'");
        Assert.assertEquals(params.get("paymentAmount"), "100", "Wrong value for 'paymentAmount'");
        Assert.assertEquals(params.get("currencyCode"), CURRENCY_CODE, "Wrong value for 'currencyCode'");
        Assert.assertEquals(params.get("shopperEmail"), SHOPPER_EMAIL, "Wrong value for 'shopperEmail'");
        Assert.assertEquals(params.get("shopperReference"), "123", "Wrong value for 'shopperReference'");
        Assert.assertEquals(params.get("resURL"), RES_URL, "Wrong value for 'resURL'");
        Assert.assertEquals(params.get("merchantSig"), MERCHANT_SIG, "Wrong value for 'merchantSig'");
        Assert.assertEquals(params.get("shipBeforeDate"), DateTime.now().toString(DATE_TIME_PATTERN), "Wrong value for 'shipBeforeDate'");
        Assert.assertEquals(params.get("sessionValidity"), DateTime.now().plusMinutes(15).toString(DATE_TIME_PATTERN), "Wrong value for 'sessionValidity'");
        Assert.assertEquals(params.get("splitKey"), "splitValue", "Wrong value for 'splitKey'");
        Assert.assertEquals(params.get("shopperLocale"), Locale.GERMANY.toString(), "Wrong value for 'shopperLocale'");
    }

    @Test(groups = "fast")
    public void testWithPaymentTypeDOTPAY() throws Exception {
        final Map<String, String> params = new HPPRequestBuilder().withCountryCode(COUNTRY_CODE)
                                                                  .withResURL(RES_URL)
                                                                  .build();

        Assert.assertTrue(params.containsKey("resURL"), "HPP Params map should contain a 'resURL'");
    }

    @Test(groups = "fast")
    public void testWithPaymentTypeDEBITCARDSHPP() throws Exception {
        final Map<String, String> params = new HPPRequestBuilder().withCountryCode(COUNTRY_CODE)
                                                                  .build();

        Assert.assertFalse(params.containsKey("brandCode"), "HPP Params map should not containing a 'brandCode'");
    }

    @Test(groups = "fast")
    public void testWithHppVariantOverride() throws Exception {
        final PaymentInfo paymentInfo = buildCardPaymentInfo(PaymentType.DEBITCARDS_HPP);
        paymentInfo.getPaymentProvider().setHppVariantOverride(VARIANT_OVERRIDE);
        final Map<String, String> params = new HPPRequestBuilder().withCountryCode(COUNTRY_CODE)
                                                                  .withBrandCodeAndOrAllowedMethods(paymentInfo)
                                                                  .build();

        Assert.assertEquals(params.get("brandCode"), params.get("brandCode"), "Wrong value for 'brandCode'");
        Assert.assertFalse(params.containsKey("allowedMethods"), "HPP Params map should not containing a 'allowedMethods'");
    }

    @Test(groups = "fast")
    public void testWithNonPayPalLocale() throws Exception {
        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setPaymentType(PaymentType.PAYPAL);
        final PaymentInfo paymentInfo = new WebPaymentFrontend(paymentProvider);
        final Map<String, String> params = new HPPRequestBuilder().withShopperLocale(paymentInfo.getPaymentProvider().getPaymentType(), Locale.JAPAN)
                                                                  .withCountryCode("JP")
                                                                  .build();

        Assert.assertEquals(params.get("shopperLocale"), Locale.US.toString(), "Wrong value for 'shopperLocale'");
    }

    private PaymentInfo buildCardPaymentInfo(final PaymentType paymentType) {
        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setPaymentType(paymentType);
        return new CreditCard(paymentProvider);
    }

    private PaymentInfo buildCCPaymentInfo() {
        return buildCardPaymentInfo(PaymentType.CREDITCARD);
    }
}
