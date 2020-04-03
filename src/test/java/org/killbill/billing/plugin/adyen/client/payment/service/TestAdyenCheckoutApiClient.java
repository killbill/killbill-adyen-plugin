/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;

import org.jooq.tools.StringUtils;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallResult;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCheckoutApiClient;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;

import com.adyen.model.checkout.PaymentsDetailsRequest;
import com.adyen.model.checkout.PaymentsRequest;
import com.adyen.model.checkout.PaymentsResponse;

import org.killbill.billing.plugin.adyen.client.payment.service.SuccessfulAdyenCall;
import org.killbill.billing.plugin.adyen.client.payment.service.checkout.CheckoutApiTestHelper;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAdyenCheckoutApiClient {

    @Test(groups = "fast", enabled = false)
    public void testAuthoriseKlarnaPayment() throws Exception {
        final AdyenConfigProperties adyenConfigProperties = getAdyenConfigForCheckout();
        PaymentsRequest request = new PaymentsRequest();
        final AdyenCheckoutApiClient checkoutApi = mock(AdyenCheckoutApiClient.class);
        PaymentsResponse authResponse = CheckoutApiTestHelper.getRedirectShopperResponse();
        AdyenCallResult<PaymentsResponse> authResult = new SuccessfulAdyenCall<PaymentsResponse>(authResponse,100);
        PaymentsResponse authoriseResponse = CheckoutApiTestHelper.getRedirectShopperResponse();
        when(checkoutApi.createPayment(request)).thenReturn(authResult);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR,null, new KlarnaPaymentInfo());

        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.createKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final AdyenPaymentServiceProviderPort adyenPort = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, adyenConfigProperties);

        PurchaseResult result = adyenPort.authoriseKlarnaPayment(merchantAccount, paymentData, userData);
        assertEquals(result.getResultCode(), "RedirectShopper");
        assertTrue(result.getAdditionalData().size() > 0);

        Map<String, String> additionalData = result.getAdditionalData();
        assertEquals(additionalData.get("paymentData"), CheckoutApiTestHelper.PAYMENT_DATA);
        assertEquals(additionalData.get("paymentMethod"), "klarna");
        assertEquals(additionalData.get("formUrl"), CheckoutApiTestHelper.URL);
        assertEquals(additionalData.get("formMethod"), "GET");
        assertFalse(StringUtils.isEmpty(additionalData.get("resultKeys")));
    }

    @Test(groups = "fast", enabled = false)
    public void testCompleteAuthoriseKlarna() throws Exception {
        final AdyenConfigProperties adyenConfigProperties = getAdyenConfigForCheckout();
        PaymentsDetailsRequest request = new PaymentsDetailsRequest();
        final AdyenCheckoutApiClient checkoutApi = mock(AdyenCheckoutApiClient.class);
        PaymentsResponse authResponse = CheckoutApiTestHelper.getAuthorisedResponse();
        AdyenCallResult<PaymentsResponse> authResult = new SuccessfulAdyenCall<PaymentsResponse>(authResponse, 100);
        when(checkoutApi.paymentDetails(request)).thenReturn(authResult);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR,null, new KlarnaPaymentInfo());

        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.completeKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final AdyenPaymentServiceProviderPort adyenServiceProvider = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, adyenConfigProperties);

        PurchaseResult result = adyenServiceProvider.completeKlarnaPaymentAuth(merchantAccount, paymentData, userData);
        assertEquals(result.getResultCode(), "Authorised");
        assertEquals(result.getPspReference(), CheckoutApiTestHelper.PSP_REFERENCE);
    }

    private AdyenConfigProperties getAdyenConfigForCheckout() {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.checkout.environment", "TEST");
        properties.put("org.killbill.billing.plugin.adyen.checkout.country", "UK");
        properties.put("org.killbill.billing.plugin.adyen.checkout.apiKey.UK", "API_KEY_UK");
        return new AdyenConfigProperties(properties);
    }
}
