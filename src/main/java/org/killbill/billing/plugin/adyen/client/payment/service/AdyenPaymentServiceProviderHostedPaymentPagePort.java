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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.HppCompletedResult;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class AdyenPaymentServiceProviderHostedPaymentPagePort extends BaseAdyenPaymentServiceProviderPort implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(AdyenPaymentServiceProviderHostedPaymentPagePort.class);

    private final AdyenConfigProperties adyenConfigProperties;
    private final AdyenRequestFactory adyenRequestFactory;

    public AdyenPaymentServiceProviderHostedPaymentPagePort(final AdyenConfigProperties adyenConfigProperties,
                                                            final AdyenRequestFactory adyenRequestFactory) {
        this.adyenConfigProperties = adyenConfigProperties;
        this.adyenRequestFactory = adyenRequestFactory;
    }

    @Override
    public void close() throws IOException {
        // No-op for now
    }

    public Map<String, String> getFormParameter(final String merchantAccount, final PaymentData paymentData, final UserData userData, final SplitSettlementData splitSettlementData) throws SignatureGenerationException {
        logOperation(logger, "createHppRequest", paymentData, userData, null);
        return adyenRequestFactory.createHppRequest(merchantAccount, paymentData, userData, splitSettlementData);
    }

    // Used to verify completion
    public Boolean verifyRequestIntegrity(final Map<String, String> requestParameterMap, final String countryIsoCode) {
        final String authResult = requestParameterMap.get("authResult");
        final String pspReference = requestParameterMap.get("pspReference");
        final String merchantReference = requestParameterMap.get("merchantReference");
        final String skinCode = requestParameterMap.get("skinCode");
        final String merchantSig = requestParameterMap.get("merchantSig");
        final String paymentMethod = requestParameterMap.get("paymentMethod");
        final String shopperLocale = requestParameterMap.get("shopperLocale");
        final String merchantReturnData = requestParameterMap.get("merchantReturnData");

        final StringBuilder signingData = new StringBuilder();
        if (authResult != null) {
            signingData.append(authResult);
        }
        if (pspReference != null) {
            signingData.append(pspReference);
        }
        if (merchantReference != null) {
            signingData.append(merchantReference);
        }
        if (skinCode != null) {
            signingData.append(skinCode);
        }
        if (merchantReturnData != null) {
            signingData.append(merchantReturnData);
        }

        try {
            return new Signer(adyenConfigProperties).verifyBase64EncodedSignature(countryIsoCode, merchantSig, signingData.toString());
        } catch (final SignatureVerificationException e) {
            logger.warn("Could not verify signature", e);
            return false;
        }
    }

    public AdyenConfigProperties getAdyenConfigProperties() {
        return adyenConfigProperties;
    }

    public HppCompletedResult parsePSPResponse(final Map<String, String[]> requestParameterMap) {
        final String pspReference = getPspReference(requestParameterMap);
        final PaymentServiceProviderResult pspResult;
        if (requestParameterMap.get("authResult") != null && requestParameterMap.get("authResult")[0] != null) {
            pspResult = PaymentServiceProviderResult.getPaymentResultForId(requestParameterMap.get("authResult")[0]);
        } else {
            pspResult = PaymentServiceProviderResult.ERROR;
        }
        return new HppCompletedResult(pspReference, pspResult, null);
    }

    private String getPspReference(final Map<String, String[]> requestParameterMap) {
        if (requestParameterMap.get("pspReference") == null) {
            return null;
        }
        final String pspReference = requestParameterMap.get("pspReference")[0];
        if (Strings.isNullOrEmpty(pspReference) || pspReference.equals("\"\"")) {
            return null;
        }
        return pspReference;
    }
}
