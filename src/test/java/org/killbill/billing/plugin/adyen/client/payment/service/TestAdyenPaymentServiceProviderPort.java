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

import org.killbill.adyen.payment.Card;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.adyen.payment.ServiceException;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.CreditCard;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAdyenPaymentServiceProviderPort {

    private PaymentData<CreditCard> paymentData;
    private AdyenPaymentServiceProviderPort adyenPaymentServiceProviderPort;
    private PaymentRequest paymentRequest;

    @BeforeMethod(groups = "fast")
    public void setup() throws ServiceException {
        final PaymentInfoConverterManagement paymentInfoConverterManagement = Mockito.mock(PaymentInfoConverterManagement.class);
        final PaymentProvider paymentProvider = Mockito.mock(PaymentProvider.class);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = Mockito.mock(AdyenPaymentRequestSender.class);

        paymentRequest = new PaymentRequest();
        paymentRequest.setReference("12345");
        final Card card = new Card();
        paymentRequest.setCard(card);

        final CreditCard paymentInfo = new CreditCard(paymentProvider);
        this.paymentData = new PaymentData<CreditCard>();
        this.paymentData.setPaymentInfo(paymentInfo);

        this.adyenPaymentServiceProviderPort = Mockito.spy(new AdyenPaymentServiceProviderPort(paymentInfoConverterManagement,
                                                                                               mock(AdyenRequestFactory.class),
                                                                                               adyenPaymentRequestSender));
        final PaymentResult paymentResult = new PaymentResult();
        paymentResult.setResultCode(PaymentServiceProviderResult.REDIRECT_SHOPPER.getId());
        AdyenCallResult adyenCallResult = mock(AdyenCallResult.class);
        when(adyenCallResult.receivedWellFormedResponse()).thenReturn(true);
        when(adyenCallResult.getResult()).thenReturn(Optional.of(paymentResult));
        when(adyenPaymentRequestSender.authorise(anyString(), any(PaymentRequest.class))).thenReturn(adyenCallResult);
    }

    @Test(groups = "fast")
    public void testAuthorizePaymentWith3DSTermUrl() {
        final String termUrl = "termUrl";
        final PurchaseResult result = adyenPaymentServiceProviderPort.authorise(paymentRequest, "DE", paymentData, termUrl);
        final String returnUrl = result.getFormParameter().get("TermUrl");
        Assert.assertEquals(returnUrl, termUrl, "Expected termUrl to be set, but wasn't");
    }

    @Test(groups = "fast")
    public void testAuthorizePaymentWithNullTermUrl() {
        final PurchaseResult result = adyenPaymentServiceProviderPort.authorise(paymentRequest, "DE", paymentData, null);
        Assert.assertNull(result.getFormParameter().get("TermUrl"));
    }
}

