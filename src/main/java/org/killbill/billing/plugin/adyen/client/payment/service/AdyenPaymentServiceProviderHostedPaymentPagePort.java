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

import java.util.Map;

import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.HppCompletedResult;
import org.killbill.billing.plugin.adyen.client.model.OrderData;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class AdyenPaymentServiceProviderHostedPaymentPagePort {

    private static final Logger logger = LoggerFactory.getLogger("adyen");

    private final AdyenConfigProperties adyenConfigProperties;
    private final AdyenRequestFactory adyenRequestFactory;

    public AdyenPaymentServiceProviderHostedPaymentPagePort(final AdyenConfigProperties adyenConfigProperties,
                                                            final AdyenRequestFactory adyenRequestFactory) {
        this.adyenConfigProperties = adyenConfigProperties;
        this.adyenRequestFactory = adyenRequestFactory;
    }

    public Map<String, String> getFormParameter(final Long amount, final PaymentData paymentData, final OrderData orderData, final UserData userData, final String serverUrl, final String resultUrl) throws SignatureGenerationException {
        Preconditions.checkNotNull(amount, "amount");
        Preconditions.checkNotNull(paymentData.getPaymentInternalRef(), "paymentData#paymentInternalRef");
        Preconditions.checkNotNull(paymentData.getPaymentInfo(), "paymentData#paymentInfo");
        Preconditions.checkNotNull(paymentData.getPaymentInfo().getPaymentProvider(), "paymentInfo#paymentProvider");
        Preconditions.checkNotNull(paymentData.getPaymentInfo().getPaymentProvider().getCurrency(), "paymentProvider#currency");
        Preconditions.checkNotNull(orderData.getShipBeforeDate(), "orderData#shipBeforeDate");
        Preconditions.checkNotNull(userData.getCustomerLocale(), "userData#customerLocale");

        return adyenRequestFactory.createHppRequest(amount, paymentData, orderData, userData, serverUrl, resultUrl, null);
    }

    public String getFormUrl(final PaymentData paymentData) {
        return getFormUrl(paymentData.getPaymentInfo().getPaymentProvider());
    }

    private String getFormUrl(final PaymentProvider paymentProvider) {
        final String hppTarget = adyenConfigProperties.getHppTarget();
        final String hppTargetOverride = paymentProvider.getHppTargetOverride();
        final String formUrl;
        if (!Strings.isNullOrEmpty(hppTargetOverride) && hppTarget.contains("/")) {
            formUrl = hppTarget.substring(0, hppTarget.lastIndexOf('/') + 1) + hppTargetOverride;
        } else {
            formUrl = hppTarget;
        }
        return formUrl;
    }

    public Boolean verifyRequestIntegrity(final Map<String, String[]> requestParameterMap, final String countryIsoCode, final Long paymentId) {
        ensureMatch(toPaymentRef(paymentId), extractFirst(requestParameterMap.get("merchantReference")));
        try {
            final String authResult = requestParameterMap.get("authResult") != null && requestParameterMap.get("authResult")[0] != null ? requestParameterMap.get("authResult")[0] : "ERROR";
            final String pspReference = requestParameterMap.get("pspReference") != null && requestParameterMap.get("pspReference")[0] != null ? requestParameterMap.get("pspReference")[0] : "";
            final String merchantReference = requestParameterMap.get("merchantReference") != null && requestParameterMap.get("merchantReference")[0] != null ? requestParameterMap.get("merchantReference")[0] : "";
            final String skinCode = requestParameterMap.get("skinCode") != null && requestParameterMap.get("skinCode")[0] != null ? requestParameterMap.get("skinCode")[0] : "";
            final String merchantSig = requestParameterMap.get("merchantSig") != null && requestParameterMap.get("merchantSig")[0] != null ? requestParameterMap.get("merchantSig")[0] : "";
            final StringBuilder signingData = new StringBuilder();
            signingData.append(authResult);
            if (pspReference != null) {
                signingData.append(pspReference);
            }
            signingData.append(merchantReference);
            signingData.append(skinCode);
            // TODO: add back in when paypal sends merchant return data in a cancelled transaction
            //signingData.append(merchantReturnData);
            return new Signer(adyenConfigProperties).verifyBase64EncodedSignature(countryIsoCode, merchantSig, signingData.toString());
        } catch (final SignatureVerificationException e) {
            logger.error("Could not verify signature, exception was: ", e);
            return false;
        }
    }

    private String toPaymentRef(final Long paymentId) {
        return "P" + paymentId;
    }

    public HppCompletedResult parsePSPResponse(final long billingId, final Map<String, String[]> requestParameterMap) {
        final String pspReference = getPspReference(requestParameterMap);
        final PaymentServiceProviderResult pspResult = PaymentServiceProviderResult.getPaymentResultForId(requestParameterMap.get("authResult") != null && requestParameterMap.get("authResult")[0] != null ? requestParameterMap.get("authResult")[0] : "ERROR");
        return new HppCompletedResult(billingId, pspReference, pspResult, null);
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

    private void ensureMatch(final String paymentOrBillingRef, final String merchantRef) {
        if (paymentOrBillingRef == null || merchantRef == null || !paymentOrBillingRef.equals(merchantRef)) {
            logger.error("paymentOrBillingRef " + paymentOrBillingRef + " and merchantRef " + merchantRef + " do not match");
            throw new IllegalArgumentException("invalid foreignRef");
        }
    }

    private String extractFirst(final String[] array) {
        if (array != null && array.length > 0) {
            return array[0];
        } else {
            return null;
        }
    }
}
