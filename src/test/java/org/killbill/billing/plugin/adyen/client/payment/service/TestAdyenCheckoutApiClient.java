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
import org.jooq.tools.StringUtils;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import com.adyen.model.checkout.PaymentsDetailsRequest;
import com.adyen.model.checkout.PaymentsRequest;
import com.adyen.model.checkout.PaymentsResponse;
import org.killbill.billing.plugin.adyen.client.payment.service.checkout.CheckoutApiTestHelper;
import com.adyen.service.exception.ApiException;
import org.testng.annotations.Test;

import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.RESPONSE_ABOUT_INVALID_REQUEST;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAdyenCheckoutApiClient {

    @Test(groups = "fast")
    public void testAuthoriseKlarnaPayment() throws Exception {
        PaymentsRequest request = new PaymentsRequest();
        final AdyenCheckoutApiClient checkoutApi = mock(AdyenCheckoutApiClient.class);
        PaymentsResponse authResponse = CheckoutApiTestHelper.getRedirectShopperResponse();
        AdyenCallResult<PaymentsResponse> authResult = new SuccessfulAdyenCall<PaymentsResponse>(authResponse,100);
        PaymentsResponse authoriseResponse = CheckoutApiTestHelper.getRedirectShopperResponse();
        when(checkoutApi.createPayment(request)).thenReturn(authResult);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR,null,
                null);

        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.createKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final AdyenPaymentServiceProviderPort adyenServiceProvider = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, checkoutApi);

        PurchaseResult result = adyenServiceProvider.authoriseKlarnaPayment(merchantAccount, paymentData, userData);
        assertEquals(result.getResultCode(), "RedirectShopper");
        assertTrue(result.getAdditionalData().size() > 0);

        Map<String, String> additionalData = result.getAdditionalData();
        assertEquals(additionalData.get("paymentData"), CheckoutApiTestHelper.PAYMENT_DATA);
        assertEquals(additionalData.get("paymentMethod"), "klarna");
        assertEquals(additionalData.get("formUrl"), CheckoutApiTestHelper.URL);
        assertEquals(additionalData.get("formMethod"), "GET");
        assertFalse(StringUtils.isEmpty(additionalData.get("resultKeys")));
    }

    @Test(groups = "fast")
    public void testCompleteAuthoriseKlarna() throws Exception {
        PaymentsDetailsRequest request = new PaymentsDetailsRequest();
        final AdyenCheckoutApiClient checkoutApi = mock(AdyenCheckoutApiClient.class);
        PaymentsResponse authResponse = CheckoutApiTestHelper.getAuthorisedResponse();
        AdyenCallResult<PaymentsResponse> authResult = new SuccessfulAdyenCall<PaymentsResponse>(authResponse, 100);
        when(checkoutApi.paymentDetails(request)).thenReturn(authResult);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR,null, null);

        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.completeKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final AdyenPaymentServiceProviderPort adyenServiceProvider = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, checkoutApi);

        PurchaseResult result = adyenServiceProvider.completeKlarnaPaymentAuth(merchantAccount, paymentData, userData);
        assertEquals(result.getResultCode(), "Authorised");
        assertEquals(result.getPspReference(), CheckoutApiTestHelper.PSP_REFERENCE);
    }

    @Test(groups = "fast", enabled = false)
    public void testAuthoriseErrorOnKlarnaPayment() throws Exception {
        final PaymentsRequest request = new PaymentsRequest();
        final ApiException exception = new ApiException("API exception", 411);
        final FailedCheckoutApiCall callResult = new FailedCheckoutApiCall(
                RESPONSE_ABOUT_INVALID_REQUEST, exception, exception);
        final AdyenCheckoutApiClient checkoutApi = mock(AdyenCheckoutApiClient.class);
        when(checkoutApi.createPayment(request)).thenReturn(callResult);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR,null, null);
        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.createKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final AdyenPaymentServiceProviderPort adyenServiceProvider = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, checkoutApi);
        PurchaseResult result = adyenServiceProvider.authoriseKlarnaPayment(merchantAccount, paymentData, userData);
        assertTrue(result.getResult().isPresent());
        assertNull(result.getResultCode());
        assertEquals(result.getResult().get().getResponses()[0], "Error");
        assertEquals(result.getReason(), "API exception");
    }
}
