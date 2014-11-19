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

package org.killbill.billing.plugin.adyen.client;

import org.killbill.adyen.payment.*;

public class AdyenRequestSender {


    private final PaymentPortRegistry adyenPaymentPortRegistry;

    public AdyenRequestSender(PaymentPortRegistry adyenPaymentPortRegistry) {
        this.adyenPaymentPortRegistry = adyenPaymentPortRegistry;
    }

    public PaymentResult authorize(String countryIsoCode, PaymentRequest request) throws ServiceException {
        final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
        return paymentPort.authorise(request);
    }

    public ModificationResult capture(String countryIsoCode, final ModificationRequest modificationRequest) throws ServiceException {
        final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
        return paymentPort.capture(modificationRequest);
    }


    public PaymentResult authorize3D(String countryIsoCode, final PaymentRequest3D paymentRequest3D) throws ServiceException {
        final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
        return paymentPort.authorise3D(paymentRequest3D);
    }
}
