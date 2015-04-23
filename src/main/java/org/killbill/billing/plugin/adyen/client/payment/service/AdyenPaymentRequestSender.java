/*
 * Copyright 2014-2015 Groupon, Inc
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

import java.io.Closeable;
import java.io.IOException;

import org.killbill.adyen.payment.DirectDebitRequest;
import org.killbill.adyen.payment.DirectDebitResponse;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.ModificationResult;
import org.killbill.adyen.payment.PaymentPortType;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.adyen.payment.ServiceException;
import org.killbill.billing.plugin.adyen.client.PaymentPortRegistry;

public class AdyenPaymentRequestSender implements Closeable {

    private final PaymentPortRegistry adyenPaymentPortRegistry;

    public AdyenPaymentRequestSender(final PaymentPortRegistry adyenPaymentPortRegistry) {
        this.adyenPaymentPortRegistry = adyenPaymentPortRegistry;
    }

    public DirectDebitResponse directdebit(final String countryIsoCode, final DirectDebitRequest request) throws ServiceException {
        return adyenPaymentPortRegistry.getPaymentPort(countryIsoCode).directdebit(request);
    }

    public PaymentResult authorise(final String countryIsoCode, final PaymentRequest request) throws ServiceException {
        return adyenPaymentPortRegistry.getPaymentPort(countryIsoCode).authorise(request);
    }

    public PaymentResult authorise3D(final String countryIsoCode, final PaymentRequest3D request) throws ServiceException {
        return adyenPaymentPortRegistry.getPaymentPort(countryIsoCode).authorise3D(request);
    }

    public ModificationResult refund(final String countryIsoCode, final ModificationRequest modificationRequest) throws ServiceException {
        final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
        return paymentPort.refund(modificationRequest);
    }

    public ModificationResult cancel(final String countryIsoCode, final ModificationRequest modificationRequest) throws ServiceException {
        final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
        return paymentPort.cancel(modificationRequest);
    }

    public ModificationResult cancelOrRefund(final String countryIsoCode, final ModificationRequest modificationRequest) throws ServiceException {
        final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
        return paymentPort.cancelOrRefund(modificationRequest);
    }

    public ModificationResult capture(final String countryIsoCode, final ModificationRequest modificationRequest) throws ServiceException {
        final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
        return paymentPort.capture(modificationRequest);
    }

    @Override
    public void close() throws IOException {
        adyenPaymentPortRegistry.close();
    }
}
