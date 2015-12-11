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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.plugin.adyen.TestRemoteBase;
import org.killbill.billing.plugin.adyen.client.model.OrderData;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.WebPaymentFrontend;
import org.killbill.billing.plugin.util.http.QueryComputer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRemoteAdyenPaymentServiceProviderHostedPaymentPagePort extends TestRemoteBase {

    @Test(groups = "slow")
    public void testRedirectHPP() throws Exception {
        final BigDecimal authAmount = BigDecimal.TEN;

        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setCurrency(Currency.getInstance(DEFAULT_CURRENCY.name()));
        paymentProvider.setCountryIsoCode(DEFAULT_COUNTRY);
        paymentProvider.setPaymentType(PaymentType.CREDITCARD);
        final WebPaymentFrontend paymentInfo = new WebPaymentFrontend(paymentProvider);

        final PaymentData<WebPaymentFrontend> paymentData = new PaymentData<WebPaymentFrontend>();
        paymentData.setPaymentInternalRef(UUID.randomUUID().toString());
        paymentData.setPaymentInfo(paymentInfo);

        final OrderData orderData = new OrderData();
        orderData.setShipBeforeDate(new DateTime(DateTimeZone.UTC));

        final UserData userData = new UserData();
        userData.setCustomerLocale(new Locale("en", DEFAULT_COUNTRY));

        final String serverUrl = "http://killbill.io";
        final String resultUrl = "?q=test+adyen+redirect+success";

        final Map<String, String> formParameter = adyenPaymentServiceProviderHostedPaymentPagePort.getFormParameter(authAmount, paymentData, orderData, userData, serverUrl, resultUrl);
        final String formUrl = adyenPaymentServiceProviderHostedPaymentPagePort.getFormUrl(paymentData);

        Assert.assertNotNull(formParameter.get("merchantReference"));
        Assert.assertNotNull(formParameter.get("paymentAmount"));
        Assert.assertNotNull(formParameter.get("currencyCode"));
        Assert.assertNotNull(formParameter.get("shipBeforeDate"));
        Assert.assertNotNull(formParameter.get("skinCode"));
        Assert.assertNotNull(formParameter.get("merchantAccount"));
        Assert.assertNotNull(formParameter.get("sessionValidity"));
        Assert.assertNotNull(formUrl);

        // For manual testing
        final String fullQueryString = QueryComputer.URL_ENCODING_ENABLED_QUERY_COMPUTER.computeFullQueryString(null, formParameter);
        System.out.println("Redirect to " + formUrl + "?" + fullQueryString);
        System.out.flush();
    }
}
