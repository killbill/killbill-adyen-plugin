/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
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

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.joda.time.DateTime;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.WebPaymentFrontend;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TestHPPRequestBuilder extends BaseTestPaymentRequestBuilder {

    public static final String COUNTRY_CODE = "DE";
    public static final String MERCHANT_ACCOUNT = "ShoppyShop" + COUNTRY_CODE;
    public static final String SKIN_CODE = "skinCode" + COUNTRY_CODE;
    public static final String CURRENCY_CODE = "EUR";
    public static final String SHOPPER_EMAIL = "test@killbill.io";
    public static final String RES_URL = "http://killbill.io";
    public static final String MERCHANT_SIG = "signature";
    public static final String MERCHANT_REFERENCE = "merchantReference";
    public static final String BRAND_CODE = "brandCode";
    public static final String ALLOWED_METHODS = "allowedMethods";

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm";

    @Test(groups = "fast")
    public void testBuilder() throws Exception {
        final String merchantAccount = MERCHANT_ACCOUNT;
        final String paymentTransactionExternalKey = MERCHANT_REFERENCE;

        final WebPaymentFrontend webPaymentFrontend = new WebPaymentFrontend();
        webPaymentFrontend.setCountry(COUNTRY_CODE);
        webPaymentFrontend.setSkinCode(SKIN_CODE);
        webPaymentFrontend.setShipBeforeDate(DateTime.now().toString(DATE_TIME_PATTERN));
        webPaymentFrontend.setSessionValidity(DateTime.now().plusMinutes(15).toString(DATE_TIME_PATTERN));
        webPaymentFrontend.setResURL(RES_URL);
        webPaymentFrontend.setBrandCode(BRAND_CODE);
        webPaymentFrontend.setAllowedMethods(ALLOWED_METHODS);
        final PaymentData paymentData = new PaymentData(new BigDecimal("1"), Currency.EUR, paymentTransactionExternalKey, webPaymentFrontend);

        final UserData userData = new UserData();
        userData.setShopperEmail(SHOPPER_EMAIL);
        userData.setShopperReference("123");
        userData.setShopperLocale(Locale.GERMANY);

        final SplitSettlementData splitSettlementData = new SplitSettlementData(1,
                                                                                "EUR",
                                                                                ImmutableList.<SplitSettlementData.Item>of(new SplitSettlementData.Item(500, "deal1", "voucherId", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal1", "voucherId2", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal2", "travelId", "travel")));

        final Signer signer = buildSignerMock();

        final Map<String, String> params = new HPPRequestBuilder(merchantAccount, paymentData, userData, splitSettlementData, new AdyenConfigProperties(new Properties()), signer).build();

        Assert.assertFalse(params.isEmpty(), "HPP Params map should not be empty");
        Assert.assertEquals(params.get("countryCode"), COUNTRY_CODE, "Wrong value for 'countryCode'");
        Assert.assertEquals(params.get("merchantAccount"), MERCHANT_ACCOUNT, "Wrong value for 'merchantAccount'");
        Assert.assertEquals(params.get("skinCode"), SKIN_CODE, "Wrong value for 'skinCode'");
        Assert.assertEquals(params.get("brandCode"), BRAND_CODE, "Wrong value for 'brandCode'");
        Assert.assertEquals(params.get("allowedMethods"), ALLOWED_METHODS, "Wrong value for 'allowedMethods'");
        Assert.assertEquals(params.get("merchantReference"), MERCHANT_REFERENCE, "Wrong value for 'merchantReference'");
        Assert.assertEquals(params.get("paymentAmount"), "100", "Wrong value for 'paymentAmount'");
        Assert.assertEquals(params.get("currencyCode"), CURRENCY_CODE, "Wrong value for 'currencyCode'");
        Assert.assertEquals(params.get("shopperEmail"), SHOPPER_EMAIL, "Wrong value for 'shopperEmail'");
        Assert.assertEquals(params.get("shopperReference"), "123", "Wrong value for 'shopperReference'");
        Assert.assertEquals(params.get("resURL"), RES_URL, "Wrong value for 'resURL'");
        Assert.assertEquals(params.get("merchantSig"), MERCHANT_SIG, "Wrong value for 'merchantSig'");
        Assert.assertEquals(params.get("shipBeforeDate"), DateTime.now().toString(DATE_TIME_PATTERN), "Wrong value for 'shipBeforeDate'");
        Assert.assertEquals(params.get("sessionValidity"), DateTime.now().plusMinutes(15).toString(DATE_TIME_PATTERN), "Wrong value for 'sessionValidity'");
        Assert.assertEquals(params.get("shopperLocale"), Locale.GERMANY.toString(), "Wrong value for 'shopperLocale'");
        Assert.assertEquals(params.get("splitsettlementdata.api"), "1");
        Assert.assertEquals(params.get("splitsettlementdata.nrOfItems"), "3");
        Assert.assertEquals(params.get("splitsettlementdata.totalAmount"), "2000");
        Assert.assertEquals(params.get("splitsettlementdata.currencyCode"), "EUR");
        Assert.assertEquals(params.get("splitsettlementdata.item1.amount"), "500");
        Assert.assertEquals(params.get("splitsettlementdata.item1.currencyCode"), "EUR");
        Assert.assertEquals(params.get("splitsettlementdata.item1.group"), "deal1");
        Assert.assertEquals(params.get("splitsettlementdata.item1.reference"), "voucherId");
        Assert.assertEquals(params.get("splitsettlementdata.item1.type"), "voucher");
        Assert.assertEquals(params.get("splitsettlementdata.item2.amount"), "750");
        Assert.assertEquals(params.get("splitsettlementdata.item2.currencyCode"), "EUR");
        Assert.assertEquals(params.get("splitsettlementdata.item2.group"), "deal1");
        Assert.assertEquals(params.get("splitsettlementdata.item2.reference"), "voucherId2");
        Assert.assertEquals(params.get("splitsettlementdata.item2.type"), "voucher");
        Assert.assertEquals(params.get("splitsettlementdata.item3.amount"), "750");
        Assert.assertEquals(params.get("splitsettlementdata.item3.currencyCode"), "EUR");
        Assert.assertEquals(params.get("splitsettlementdata.item3.group"), "deal2");
        Assert.assertEquals(params.get("splitsettlementdata.item3.reference"), "travelId");
        Assert.assertEquals(params.get("splitsettlementdata.item3.type"), "travel");
    }

    private Signer buildSignerMock() {
        final Signer signer = Mockito.mock(Signer.class);
        Mockito.when(signer.signFormParameters(Mockito.anyLong(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString(),
                                               Mockito.anyString())).thenReturn(MERCHANT_SIG);
        Mockito.when(signer.signFormParameters(Mockito.<String, String>anyMap(),
                                               Mockito.anyString(),
                                               Mockito.anyString())).thenReturn(MERCHANT_SIG);
        return signer;
    }

    @Test(groups = "slow")
    public void testBuilderLocaleGB() throws Exception {
        final String merchantAccount = MERCHANT_ACCOUNT;
        final String paymentTransactionExternalKey = MERCHANT_REFERENCE;

        final WebPaymentFrontend paymentInfo = new WebPaymentFrontend();
        paymentInfo.setCountry("GB");
        paymentInfo.setBrandCode("paypal");

        final PaymentData<WebPaymentFrontend> paymentData = new PaymentData<WebPaymentFrontend>(
                new BigDecimal("1"), Currency.GBP, paymentTransactionExternalKey, paymentInfo);

        final UserData userData = new UserData();
        userData.setShopperLocale(Locale.UK);

        final Signer signer = buildSignerMock();
        final Map<String, String> params = new HPPRequestBuilder(merchantAccount, paymentData, userData, null, new AdyenConfigProperties(new Properties()), signer).build();
        Assert.assertEquals(params.get("countryCode"), "GB");
        Assert.assertEquals(params.get("currencyCode"), "GBP");
        Assert.assertEquals(params.get("shopperLocale"), "en_GB");
    }
}
