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

package org.killbill.billing.plugin.adyen.client.payment.service.checkout;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.jooq.tools.StringUtils;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCheckoutApiClient;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;

import com.adyen.model.ApiError;
import com.adyen.model.checkout.PaymentsRequest;
import com.adyen.model.checkout.PaymentsResponse;
import com.adyen.service.exception.ApiException;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAdyenCheckoutApiRequest {

    @Test(groups = "fast")
    public void testAuthoriseKlarnaPayment() throws Exception {
        PaymentsRequest request = new PaymentsRequest();
        final AdyenCheckoutApiClient checkoutApi = mock(AdyenCheckoutApiClient.class);
        PaymentsResponse authoriseResponse = CheckoutApiTestHelper.getRedirectShopperResponse();
        when(checkoutApi.createPayment(request)).thenReturn(authoriseResponse);

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
    public void testAuthoriseErrorOnKlarnaPayment() throws Exception {
        PaymentsRequest request = new PaymentsRequest();
        final AdyenCheckoutApiClient checkoutApi = mock(AdyenCheckoutApiClient.class);
        when(checkoutApi.createPayment(request)).thenThrow(new ApiException("API exception", 411));

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
        assertTrue(result.getResult().isPresent());
        assertNull(result.getResultCode());
        assertEquals(result.getResult().get().getResponses()[0], "Error");
        assertEquals(result.getReason(), "API exception");
    }
}
