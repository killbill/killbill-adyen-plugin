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

import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.ModificationResult;
import org.killbill.adyen.payment.PaymentPortType;
import org.killbill.adyen.payment.ServiceException;
import org.killbill.billing.plugin.adyen.client.AdyenPaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.PaymentPortRegistry;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.ws.WebServiceException;

// Note: retries have been disabled for now
public class TestAdyenPaymentRequestSender {

    @Test(groups = "fast", enabled = false)
    public void shouldRetryTheSendingOfCancelMessages() throws Exception {
        final int numberOfAttempts = 5;

        final PaymentPortType paymentPort = Mockito.mock(PaymentPortType.class);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = adyenRequestSender(paymentPort);

        final ModificationRequest modificationRequest = new ModificationRequest();
        Mockito.when(paymentPort.cancel(modificationRequest)).thenAnswer(new ThrowExceptionForNTimesBeforeReturningResult(numberOfAttempts - 1, new WebServiceException()));
        AdyenCallResult<ModificationResult> result = adyenPaymentRequestSender.cancel("any", modificationRequest);

        Mockito.verify(paymentPort, Mockito.atLeast(numberOfAttempts)).cancel(modificationRequest);
        Assert.assertNotNull(result);
    }

    @Test(groups = "fast", enabled = false)
    public void shouldAtLeastDoOneAttemptToCancel() throws Exception {
        final int numberOfAttempts = -1;

        final PaymentPortType paymentPort = Mockito.mock(PaymentPortType.class);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = adyenRequestSender(paymentPort);

        final ModificationRequest modificationRequest = new ModificationRequest();
        Mockito.when(paymentPort.cancel(modificationRequest)).thenAnswer(new ThrowExceptionForNTimesBeforeReturningResult(numberOfAttempts - 1, new WebServiceException()));
        AdyenCallResult<ModificationResult> result = adyenPaymentRequestSender.cancel("any", modificationRequest);

        Mockito.verify(paymentPort, Mockito.atLeastOnce()).cancel(modificationRequest);
        Assert.assertNotNull(result);
    }

    @Test(groups = "fast", expectedExceptions = RuntimeException.class, enabled = false)
    public void shouldOnlyRetryTheCancellationIfWebserviceExceptionIsThrown() throws Exception {
        final int numberOfAttempts = 5;

        final PaymentPortType paymentPort = Mockito.mock(PaymentPortType.class);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = adyenRequestSender(paymentPort);

        final ModificationRequest modificationRequest = new ModificationRequest();
        Mockito.when(paymentPort.cancel(modificationRequest)).thenAnswer(new ThrowExceptionForNTimesBeforeReturningResult(numberOfAttempts - 1, new ServiceException()));
        adyenPaymentRequestSender.cancel("any", modificationRequest);
    }

    @Test(groups = "fast", enabled = false)
    public void shouldRetryTheSendingOfCancelOrRefundMessages() throws Exception {
        final int numberOfAttempts = 3;

        final PaymentPortType paymentPort = Mockito.mock(PaymentPortType.class);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = adyenRequestSender(paymentPort);

        final ModificationRequest modificationRequest = new ModificationRequest();
        Mockito.when(paymentPort.cancelOrRefund(modificationRequest)).thenAnswer(new ThrowExceptionForNTimesBeforeReturningResult(numberOfAttempts - 1, new WebServiceException()));
        AdyenCallResult<ModificationResult> result = adyenPaymentRequestSender.cancelOrRefund("any", modificationRequest);

        Mockito.verify(paymentPort, Mockito.atLeast(numberOfAttempts)).cancelOrRefund(modificationRequest);
        Assert.assertNotNull(result);
    }

    @Test(groups = "fast", enabled = false)
    public void shouldRetryTheSendingOfCaptureMessages() throws Exception {
        final int numberOfAttempts = 2;

        final PaymentPortType paymentPort = Mockito.mock(PaymentPortType.class);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = adyenRequestSender(paymentPort);

        final ModificationRequest modificationRequest = new ModificationRequest();
        Mockito.when(paymentPort.capture(modificationRequest)).thenAnswer(new ThrowExceptionForNTimesBeforeReturningResult(numberOfAttempts - 1, new WebServiceException()));
        AdyenCallResult<ModificationResult> result = adyenPaymentRequestSender.capture("any", modificationRequest);

        Mockito.verify(paymentPort, Mockito.atLeast(numberOfAttempts)).capture(modificationRequest);
        Assert.assertNotNull(result);
    }

    @Test(groups = "fast", enabled = false)
    public void shouldRetryTheSendingOfRefundMessages() throws Exception {
        final int numberOfAttempts = 2;

        final PaymentPortType paymentPort = Mockito.mock(PaymentPortType.class);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = adyenRequestSender(paymentPort);

        final ModificationRequest modificationRequest = new ModificationRequest();
        Mockito.when(paymentPort.refund(modificationRequest)).thenAnswer(new ThrowExceptionForNTimesBeforeReturningResult(numberOfAttempts - 1, new WebServiceException()));
        AdyenCallResult<ModificationResult> result = adyenPaymentRequestSender.refund("any", modificationRequest);

        Mockito.verify(paymentPort, Mockito.atLeast(numberOfAttempts)).refund(modificationRequest);
        Assert.assertNotNull(result);
    }

    private AdyenPaymentRequestSender adyenRequestSender(final PaymentPortType paymentPort) {
        final PaymentPortRegistry portRegistry = Mockito.mock(AdyenPaymentPortRegistry.class);
        Mockito.when(portRegistry.getPaymentPort(Mockito.anyString())).thenReturn(paymentPort);
        return Mockito.spy(new AdyenPaymentRequestSender(portRegistry));
    }

    private static class ThrowExceptionForNTimesBeforeReturningResult implements Answer<Object> {

        private final int times;
        private final Exception exception;
        int counter = 1;

        private ThrowExceptionForNTimesBeforeReturningResult(final int times, final Exception exception) {
            this.times = times;
            this.exception = exception;
        }

        @Override
        public Object answer(final InvocationOnMock invocation) throws Throwable {
            if (counter <= times) {
                counter++;
                throw exception;
            }
            return new ModificationResult();
        }
    }
}
