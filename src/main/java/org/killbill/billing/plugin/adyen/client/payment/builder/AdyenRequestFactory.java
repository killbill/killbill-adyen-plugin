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

import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;

public class AdyenRequestFactory {

    private final PaymentInfoConverterManagement paymentInfoConverterManagement;
    private final AdyenConfigProperties adyenConfigProperties;
    private final Signer signer;

    public AdyenRequestFactory(final PaymentInfoConverterManagement paymentInfoConverterManagement,
                               final AdyenConfigProperties adyenConfigProperties,
                               final Signer signer) {
        this.paymentInfoConverterManagement = paymentInfoConverterManagement;
        this.adyenConfigProperties = adyenConfigProperties;
        this.signer = signer;
    }

    public PaymentRequest createPaymentRequest(final String merchantAccount, final PaymentData paymentData, final UserData userData, @Nullable final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
        final PaymentRequestBuilder paymentRequestBuilder = new PaymentRequestBuilder(merchantAccount, paymentData, userData, splitSettlementData, additionalData, paymentInfoConverterManagement);
        return paymentRequestBuilder.build();
    }

    public PaymentRequest3D paymentRequest3d(final String merchantAccount, final PaymentData paymentData, final UserData userData, @Nullable final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
        final PaymentRequest3DBuilder paymentRequest3DBuilder = new PaymentRequest3DBuilder(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        return paymentRequest3DBuilder.build();
    }

    public ModificationRequest createModificationRequest(final String merchantAccount, final PaymentData paymentData, final String pspReference, @Nullable final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
        final ModificationRequestBuilder modificationRequestBuilder = new ModificationRequestBuilder(merchantAccount, paymentData, pspReference, splitSettlementData, additionalData);
        return modificationRequestBuilder.build();
    }

    public Map<String, String> createHppRequest(final String merchantAccount, final PaymentData paymentData, final UserData userData, @Nullable final SplitSettlementData splitSettlementData) throws SignatureGenerationException {
        final HPPRequestBuilder builder = new HPPRequestBuilder(merchantAccount,
                                                                paymentData,
                                                                userData,
                                                                splitSettlementData,
                                                                adyenConfigProperties,
                                                                signer);
        return builder.build();
    }
}
